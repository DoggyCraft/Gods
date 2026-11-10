package com.dogonfire.gods.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import com.dogonfire.gods.util.AtomicFiles;

/**
 * One YAML data file (gods.yml, believers.yml, ...), held in memory. The manager that owns it reads and changes
 * {@link #yaml()} on the main thread and calls {@link #markDirty()} (written by the next autosave) or {@link #save()}
 * (written now). Writing never blocks the main thread: {@link #flush()} turns the data into YAML text on the main
 * thread and hands the text to the storage's writer thread, which writes a temp file and moves it over the old one.
 * <p>
 * A file that can't be read is copied to {@code <name>.broken-<timestamp>} and is never written over until it loads
 * again (after it is fixed and Gods is reloaded or restarted).
 */
public final class DataFile
{
	/** Fixes the raw text of a file before it is parsed, e.g. YAML tags older versions wrote that can't be loaded. */
	public record Repair(String description, UnaryOperator<String> fix)
	{
	}

	private final Path				file;
	private final Logger			logger;
	private final Executor			writer;
	private final Repair			repair;

	private YamlConfiguration		yaml		= new YamlConfiguration();
	private volatile boolean		dirty;
	private volatile boolean		readOnly;
	private boolean					loaded;

	DataFile(Path file, Logger logger, Executor writer, Repair repair)
	{
		this.file = file;
		this.logger = logger;
		this.writer = writer;
		this.repair = repair;
	}

	/** The file name, e.g. "gods.yml" */
	public String name()
	{
		return this.file.getFileName().toString();
	}

	public Path path()
	{
		return this.file;
	}

	/**
	 * The data. Main thread only. Replaced by {@link #load()}, so fetch it every time instead of keeping it. Empty
	 * before the file is loaded.
	 */
	public YamlConfiguration yaml()
	{
		return this.yaml;
	}

	/** Whether {@link #load()} has been called (successfully or not) */
	/** True while the file on disk could not be read: it is then never written over */
	public boolean isReadOnly()
	{
		return this.readOnly;
	}

	public boolean isDirty()
	{
		return this.dirty;
	}

	/**
	 * Reads the file into memory. A missing file is empty. If it can't be read, a copy is kept as
	 * {@code <name>.broken-<timestamp>} and the file is not saved until a later load works; the first load then starts
	 * empty, a reload keeps the data that was already in memory. Returns whether the file was read.
	 */
	public boolean load()
	{
		boolean firstLoad = !this.loaded;
		this.loaded = true;

		if (!Files.exists(this.file))
		{
			this.yaml = new YamlConfiguration();
			this.dirty = false;
			this.readOnly = false;
			return true;
		}

		YamlConfiguration loadedYaml = new YamlConfiguration();
		boolean repaired = false;
		try
		{
			String text = Files.readString(this.file, StandardCharsets.UTF_8);
			if (this.repair != null)
			{
				String fixed = this.repair.fix().apply(text);
				if (!fixed.equals(text))
				{
					this.logger.info("Repaired " + name() + ": " + this.repair.description());
					text = fixed;
					repaired = true;
				}
			}
			loadedYaml.loadFromString(text);
		}
		catch (IOException | InvalidConfigurationException | RuntimeException ex)
		{
			String copy = keepBrokenCopy();
			this.logger.log(Level.SEVERE, "Could not read " + name() + ": " + ex.getMessage());
			if (firstLoad)
			{
				this.yaml = new YamlConfiguration();
				this.logger.severe("NOTHING from " + name() + " is loaded, and it will NOT be saved until it is fixed, so nothing in it is overwritten."
						+ (copy != null ? " A copy is in " + copy + "." : "") + " Fix the file and restart the server or run /g reload.");
			}
			else
			{
				this.logger.severe("Keeping the data loaded before, but " + name() + " will NOT be saved until it is fixed, so nothing in it is overwritten."
						+ (copy != null ? " A copy is in " + copy + "." : "") + " Fix the file and run /g reload.");
			}
			this.readOnly = true;
			this.dirty = false;
			return false;
		}

		this.yaml = loadedYaml;
		this.readOnly = false;
		this.dirty = repaired;
		return true;
	}

	private String keepBrokenCopy()
	{
		String copyName = name() + ".broken-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
		try
		{
			Files.copy(this.file, this.file.resolveSibling(copyName), StandardCopyOption.REPLACE_EXISTING);
			return copyName;
		}
		catch (IOException ex)
		{
			this.logger.severe("Could not copy " + name() + " to " + copyName + ": " + ex.getMessage());
			return null;
		}
	}

	/** The data changed; the next {@link #flush()} (autosave, reload or disable) writes it */
	public void markDirty()
	{
		this.dirty = true;
	}

	/** The data changed and should be written now (in the background) */
	public void save()
	{
		markDirty();
		flush();
	}

	/**
	 * Main thread: if anything changed, turns the data into YAML text and queues the write. Does nothing for a file
	 * that could not be read.
	 */
	public void flush()
	{
		if (!this.dirty || this.readOnly)
		{
			return;
		}

		String text = this.yaml.saveToString();
		this.dirty = false;

		try
		{
			this.writer.execute(() -> write(text));
		}
		catch (RejectedExecutionException ex)
		{
			this.dirty = true;
			this.logger.warning("Could not save " + name() + ": storage is already closed.");
		}
	}

	private void write(String text)
	{
		try
		{
			AtomicFiles.write(this.file, text);
		}
		catch (IOException ex)
		{
			// Written again by the next flush
			this.dirty = true;
			this.logger.log(Level.SEVERE, "Could not save " + name() + ": " + ex.getMessage());
		}
	}
}
