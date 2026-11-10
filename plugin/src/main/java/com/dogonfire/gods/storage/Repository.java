package com.dogonfire.gods.storage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * The models of one data file, held in memory. {@link #load()} reads the {@link DataFile} into models
 * ({@link #read}); after that the models are the truth. A change calls {@link #markDirty()} (written by the next
 * autosave) or {@link #save()} (written now); the flush asks {@link #write} for a fresh YAML snapshot on the main thread
 * and writes it on the storage thread. Main thread only.
 * <p>
 * Keys a repository doesn't know are kept and written back unchanged: top-level ones through {@link #keepUnknown},
 * those inside a record through {@link #extraKeys}/{@link #writeExtras}.
 */
public abstract class Repository
{
	private final DataFile				file;
	private final Logger				logger;
	// Top-level keys the subclass did not understand, written back unchanged
	private final Map<String, Object>	unknownKeys	= new LinkedHashMap<String, Object>();
	private boolean						loaded;

	protected Repository(DataFile file, Logger logger)
	{
		this.file = file;
		this.logger = logger;
		file.bind(this::snapshot);
	}

	/** Parses the data into models. Pass top-level keys it doesn't know to {@link #keepUnknown}. */
	protected abstract void read(YamlConfiguration yaml);

	/** Writes every model into an empty YamlConfiguration, with the keys {@link #read} understands. */
	protected abstract void write(YamlConfiguration yaml);

	/** Forgets every model (before a load) */
	protected abstract void clear();

	/** The number of records, for the log */
	public abstract int size();

	public DataFile file()
	{
		return this.file;
	}

	protected Logger logger()
	{
		return this.logger;
	}

	/**
	 * Reads the file. If it can't be read, the first load starts empty and a reload keeps the models in memory (see
	 * {@link DataFile#load()}); either way the file is then not saved until it loads. A file that was repaired or
	 * upgraded is written back right away. Returns whether the file was read.
	 */
	public boolean load()
	{
		boolean firstLoad = !this.loaded;
		this.loaded = true;

		boolean read = this.file.load();
		if (read || firstLoad)
		{
			clear();
			this.unknownKeys.clear();
			read(this.file.yaml());
		}
		if (read && this.file.isDirty())
		{
			this.file.flush();
		}
		return read;
	}

	public boolean isReadOnly()
	{
		return this.file.isReadOnly();
	}

	/** The data changed; the next autosave writes it */
	public void markDirty()
	{
		this.file.markDirty();
	}

	/** The data changed and is written now (in the background) */
	public void save()
	{
		this.file.save();
	}

	protected void keepUnknown(String key, Object value)
	{
		this.unknownKeys.put(key, toPlain(value));
	}

	/** Main thread: the YAML to write */
	YamlConfiguration snapshot()
	{
		YamlConfiguration yaml = new YamlConfiguration();
		write(yaml);
		for (Map.Entry<String, Object> entry : this.unknownKeys.entrySet())
		{
			if (!yaml.contains(entry.getKey()))
			{
				yaml.set(entry.getKey(), entry.getValue());
			}
		}
		if (this.file.format() != null && !yaml.contains(this.file.format().versionKey()))
		{
			yaml.set(this.file.format().versionKey(), this.file.format().currentVersion());
		}
		return yaml;
	}

	/** Turns configuration sections into plain maps, so values can be kept after the YamlConfiguration is gone */
	protected static Object toPlain(Object value)
	{
		if (value instanceof ConfigurationSection section)
		{
			Map<String, Object> map = new LinkedHashMap<String, Object>();
			for (String key : section.getKeys(false))
			{
				map.put(key, toPlain(section.get(key)));
			}
			return map;
		}
		return value;
	}

	/** The keys of {@code section} other than {@code known}, as plain values */
	protected static Map<String, Object> extraKeys(ConfigurationSection section, Set<String> known)
	{
		Map<String, Object> extras = new LinkedHashMap<String, Object>();
		for (String key : section.getKeys(false))
		{
			if (!known.contains(key))
			{
				extras.put(key, toPlain(section.get(key)));
			}
		}
		return extras;
	}

	protected static void writeExtras(ConfigurationSection section, Map<String, Object> extras)
	{
		for (Map.Entry<String, Object> entry : extras.entrySet())
		{
			if (!section.contains(entry.getKey()))
			{
				section.set(entry.getKey(), entry.getValue());
			}
		}
	}
}
