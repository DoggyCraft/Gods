package com.dogonfire.gods.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import com.dogonfire.gods.migration.VersionedYaml;
import com.dogonfire.gods.util.AtomicFiles;

/**
 * One YAML data file (gods.yml, believers.yml, ...). Either a manager reads and changes {@link #yaml()} directly on the
 * main thread, or a {@link Repository} reads it into models on load and hands back a fresh YAML snapshot on every
 * flush ({@link #bind}). Changes are marked with {@link #markDirty()} (written by the next autosave) or {@link #save()}
 * (written now). Writing never blocks the main thread: {@link #flush()} turns the data into YAML text on the main
 * thread and hands the text to the storage's writer thread, which writes a temp file and moves it over the old one.
 * <p>
 * A file that can't be read is copied to {@code <name>.broken-<timestamp>} and is never written over until it loads
 * again (after it is fixed and Gods is reloaded or restarted). A file with a data version ({@link VersionedYaml}) is
 * upgraded on load, after a {@code <name>.v<n>.bak} backup, and written back right away by its repository.
 */
public final class DataFile
{
	/** Fixes the raw text of a file before it is parsed, e.g. YAML tags older versions wrote that can't be loaded. */
	public record Repair(String description, UnaryOperator<String> fix)
	{
	}

	private final Path							file;
	private final Logger						logger;
	private final Executor						writer;
	private final Repair						repair;
	private final VersionedYaml					format;

	private YamlConfiguration					yaml		= new YamlConfiguration();
	// Set by a repository: the data to write, built from its models
	private Supplier<YamlConfiguration>			snapshot;
	private volatile boolean					dirty;
	private volatile boolean					readOnly;
	private boolean								loaded;
	// When the file was last read or written by Gods, to see on /g reload whether someone edited it since
	private volatile FileTime				knownModified;
	// Set by the writer thread when the file was edited on disk since Gods last read or wrote it: it is then not written
	// until it is read again (/g reload), so the edit wins; the changes in memory stay marked dirty until then
	private volatile boolean				editedOnDisk;
	// Set by prepareReload: the next load keeps the data in memory instead of reading the unchanged file again
	private boolean								keepOnNextLoad;
	private boolean								lastLoadKept;

	DataFile(Path file, Logger logger, Executor writer, Repair repair)
	{
		this(file, logger, writer, repair, null);
	}

	/** @param format the data version and its migrations, or null for a file without a version */
	DataFile(Path file, Logger logger, Executor writer, Repair repair, VersionedYaml format)
	{
		this.file = file;
		this.logger = logger;
		this.writer = writer;
		this.repair = repair;
		this.format = format;
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

	/** The data version and migrations of this file, or null */
	VersionedYaml format()
	{
		return this.format;
	}

	/** From now on, flushes write what {@code snapshot} returns (called on the main thread) instead of {@link #yaml()} */
	void bind(Supplier<YamlConfiguration> snapshot)
	{
		this.snapshot = snapshot;
	}

	/**
	 * The data as read by the last {@link #load()}. Main thread only. Replaced by {@link #load()}, so fetch it every
	 * time instead of keeping it. Empty before the file is loaded. For a file with a repository, only the repository
	 * reads it.
	 */
	public YamlConfiguration yaml()
	{
		return this.yaml;
	}

	/** True while the file on disk could not be read (or upgraded): it is then never written over */
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
	 * empty, a reload keeps the data that was already in memory. Returns whether the file was read (and upgraded).
	 */
	public boolean load()
	{
		if (this.keepOnNextLoad)
		{
			this.keepOnNextLoad = false;
			this.lastLoadKept = true;
			return !this.readOnly;
		}
		this.lastLoadKept = false;
		this.editedOnDisk = false;

		boolean firstLoad = !this.loaded;
		this.loaded = true;

		if (!Files.exists(this.file))
		{
			this.yaml = new YamlConfiguration();
			this.knownModified = null;
			this.dirty = false;
			this.readOnly = false;
			return true;
		}

		YamlConfiguration loadedYaml = new YamlConfiguration();
		boolean repaired = false;
		try
		{
			this.knownModified = modifiedTime();
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

		if (this.format != null)
		{
			try
			{
				VersionedYaml.Upgraded upgraded = this.format.upgrade(loadedYaml, this.file, this.logger);
				loadedYaml = upgraded.yaml();
				repaired |= upgraded.changed();
			}
			catch (VersionedYaml.LoadException | RuntimeException ex)
			{
				// Use what is there, but never write over the file that could not be upgraded
				this.logger.log(Level.SEVERE, (ex instanceof VersionedYaml.LoadException ? "" : "Could not upgrade " + name() + ": ") + ex.getMessage()
						+ (ex.getCause() != null ? ": " + ex.getCause().getMessage() : ""), ex instanceof RuntimeException ? ex : null);
				this.logger.severe(name() + " is used as it is, but it will NOT be saved until it can be upgraded, so nothing in it is overwritten. Fix the problem and restart the server or run /g reload.");
				this.yaml = loadedYaml;
				this.readOnly = true;
				this.dirty = false;
				return false;
			}
		}

		this.yaml = loadedYaml;
		this.readOnly = false;
		this.dirty = repaired;
		return true;
	}


	/** True if the last {@link #load()} kept the data in memory because the file had not changed (see {@link #prepareReload}) */
	public boolean lastLoadKept()
	{
		return this.lastLoadKept;
	}

	/**
	 * /g reload, after pending writes have finished: decides whether the next {@link #load()} reads the file again. A
	 * file that was edited on disk since Gods last read or wrote it (or could not be read before) is read again, so the
	 * hand edit wins over changes not yet saved; any other file keeps the data in memory (and is saved as usual).
	 */
	void prepareReload()
	{
		this.keepOnNextLoad = false;
		if (!this.loaded)
		{
			return;
		}

		if (!this.readOnly && Objects.equals(modifiedTime(), this.knownModified))
		{
			this.keepOnNextLoad = true;
			return;
		}

		if (this.dirty && !this.readOnly)
		{
			this.logger.warning(name() + " was changed on disk; reading it again (changes not yet saved are discarded if it can be read).");
		}
		// Never write the old data over the edit
		this.dirty = false;
	}

	/** After /g reload: a file that was not loaded again (its feature is off) reads the file the next time it is loaded */
	void finishReload()
	{
		this.keepOnNextLoad = false;
	}

	private FileTime modifiedTime()
	{
		try
		{
			return Files.exists(this.file) ? Files.getLastModifiedTime(this.file) : null;
		}
		catch (IOException ex)
		{
			return null;
		}
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

	/** The repository could not read the data: like a file that can't be parsed, it is not written until it loads again */
	void markUnreadable()
	{
		this.readOnly = true;
		this.dirty = false;
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
	 * True when the file was edited (or deleted) on disk since Gods last read or wrote it: it is not written until it
	 * is read again by /g reload, so the edit wins over the changes in memory
	 */
	public boolean isEditedOnDisk()
	{
		return this.editedOnDisk;
	}

	/**
	 * Main thread: if anything changed, turns the data into YAML text and queues the write. Does nothing for a file
	 * that was never loaded, could not be read, or was edited on disk since Gods last read or wrote it.
	 */
	public void flush()
	{
		if (!this.loaded || !this.dirty || this.readOnly || this.editedOnDisk)
		{
			return;
		}

		String text = (this.snapshot != null ? this.snapshot.get() : this.yaml).saveToString();
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

	/** Writer thread */
	private void write(String text)
	{
		if (this.editedOnDisk || !Objects.equals(modifiedTime(), this.knownModified))
		{
			// Someone edited the file since Gods read or wrote it: never write over that. /g reload reads it.
			this.dirty = true;
			if (!this.editedOnDisk)
			{
				this.editedOnDisk = true;
				this.logger.warning(name() + " was edited on disk; Gods won't save it until /g reload, which reads your edit (changes made in the game since are dropped).");
			}
			return;
		}

		try
		{
			AtomicFiles.write(this.file, text);
			this.knownModified = modifiedTime();
		}
		catch (IOException ex)
		{
			// Written again by the next flush
			this.dirty = true;
			this.logger.log(Level.SEVERE, "Could not save " + name() + ": " + ex.getMessage());
		}
	}
}
