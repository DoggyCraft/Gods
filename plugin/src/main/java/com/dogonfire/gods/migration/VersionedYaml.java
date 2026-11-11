package com.dogonfire.gods.migration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import com.dogonfire.gods.util.AtomicFiles;

/**
 * Loads a YAML file that carries a version number, upgrading it with {@link Migration}s when it is older than the
 * current version. The original file is copied to {@code <name>.v<old version>.bak} before it is upgraded.
 *
 * @param versionKey            the top-level key holding the version
 * @param currentVersion        the version this build of the plugin writes
 * @param legacyVersion         the version to assume when an existing file has no version key
 * @param currentWithoutVersion null, or tells a file without a version key that is already in the current layout (it
 *                              lost its version key, e.g. by an edit): it is taken as the current version and gets the
 *                              key, instead of being "upgraded" from {@code legacyVersion}
 */
public record VersionedYaml(String versionKey, int currentVersion, int legacyVersion, List<Migration> migrations, Predicate<YamlConfiguration> currentWithoutVersion)
{
	public VersionedYaml(String versionKey, int currentVersion, int legacyVersion, List<Migration> migrations)
	{
		this(versionKey, currentVersion, legacyVersion, migrations, null);
	}

	public static final class LoadException extends Exception
	{
		public LoadException(String message, Throwable cause)
		{
			super(message, cause);
		}
	}

	/**
	 * The result of {@link #upgrade}: the (possibly new) data, whether it changed (and should be written), and whether
	 * it is from a newer version of Gods (it is then as it was, and must not be written by this version)
	 */
	public record Upgraded(YamlConfiguration yaml, boolean changed, boolean newer)
	{
	}

	/**
	 * Loads and, if needed, upgrades the file (and writes the upgraded file). Returns {@code null} when the file does
	 * not exist. A file from a newer version of the plugin is loaded as-is, with a warning, and not written; see
	 * {@link #isNewer}.
	 */
	public YamlConfiguration load(File file, Logger log) throws LoadException
	{
		if (!file.exists())
		{
			return null;
		}

		YamlConfiguration yaml = new YamlConfiguration();
		try
		{
			yaml.load(file);
		}
		catch (IOException | InvalidConfigurationException e)
		{
			throw new LoadException("Could not read " + file.getName() + ": " + e.getMessage(), e);
		}

		Upgraded upgraded;
		try
		{
			upgraded = upgrade(yaml, file.toPath(), log);
		}
		catch (RuntimeException e)
		{
			throw new LoadException("Could not upgrade " + file.getName() + ": " + e, e);
		}
		if (upgraded.changed())
		{
			try
			{
				AtomicFiles.write(file.toPath(), upgraded.yaml().saveToString());
			}
			catch (IOException e)
			{
				throw new LoadException("Could not save the upgraded " + file.getName(), e);
			}
		}
		return upgraded.yaml();
	}

	/**
	 * The version of the data: its version key (a number, or a number in quotes), else {@code legacyVersion} (or the
	 * current version when {@code currentWithoutVersion} says so).
	 */
	public int version(YamlConfiguration yaml) throws LoadException
	{
		if (!yaml.contains(versionKey))
		{
			return currentWithoutVersion != null && currentWithoutVersion.test(yaml) ? currentVersion : legacyVersion;
		}
		Object value = yaml.get(versionKey);
		if (value instanceof Number number)
		{
			return number.intValue();
		}
		if (value instanceof String text)
		{
			try
			{
				return Integer.parseInt(text.trim());
			}
			catch (NumberFormatException e)
			{
				// below
			}
		}
		throw new LoadException("The " + versionKey + " '" + value + "' is not a number", null);
	}

	/** Whether the data is from a newer version of Gods than this one */
	public boolean isNewer(YamlConfiguration yaml) throws LoadException
	{
		return version(yaml) > currentVersion;
	}

	/**
	 * Upgrades data already read from {@code file} to the current version, after copying the file to
	 * {@code <name>.v<old version>.bak} (an existing backup is never overwritten: the new one then gets a timestamp).
	 * Does not write the upgraded data; the caller does. Data from a newer version is returned as-is, with a warning
	 * and {@link Upgraded#newer()}. A version no migration chain leads from is an error, before any backup is made.
	 */
	public Upgraded upgrade(YamlConfiguration yaml, Path file, Logger log) throws LoadException
	{
		String name = file.getFileName().toString();
		int version = version(yaml);
		if (version > currentVersion)
		{
			log.severe(name + " is version " + version + ", from a newer version of Gods; this one only knows up to version " + currentVersion
					+ ". It is used as it is but NOT saved, so nothing in it is lost. Update Gods.");
			return new Upgraded(yaml, false, true);
		}
		if (version == currentVersion)
		{
			if (!yaml.contains(versionKey) && currentWithoutVersion != null && legacyVersion != currentVersion)
			{
				// Already in the current layout; it only lost its version key
				log.info(name + " has no " + versionKey + " but is already in the version " + currentVersion + " layout; adding " + versionKey + ": " + currentVersion + ".");
				yaml.set(versionKey, currentVersion);
				return new Upgraded(yaml, true, false);
			}
			return new Upgraded(yaml, false, false);
		}

		Map<Integer, Migration> byVersion = new HashMap<>();
		for (Migration migration : migrations)
		{
			byVersion.put(migration.fromVersion(), migration);
		}
		// Every step must be known before anything is touched (and a backup made)
		for (int step = version; step < currentVersion; step++)
		{
			if (!byVersion.containsKey(step))
			{
				throw new LoadException("Don't know how to upgrade " + name + " from version " + step, null);
			}
		}

		Path backup = backupPath(file, version);
		try
		{
			Files.copy(file, backup);
		}
		catch (IOException e)
		{
			throw new LoadException("Could not back up " + name + " before upgrading it", e);
		}

		int startVersion = version;
		while (version < currentVersion)
		{
			Migration migration = byVersion.get(version);
			try
			{
				yaml = migration.migrate(yaml);
			}
			catch (Migration.MigrationException e)
			{
				throw new LoadException("Could not upgrade " + name + " from version " + version + ": " + e.getMessage(), e);
			}
			version++;
			yaml.set(versionKey, version);
		}

		log.info("Upgraded " + name + " from version " + startVersion + " to " + currentVersion + ". The old file was kept as " + backup.getFileName() + ".");
		return new Upgraded(yaml, true, false);
	}

	/** {@code <name>.v<version>.bak}, or the same with a timestamp if that exists already (it may hold older data) */
	static Path backupPath(Path file, int version)
	{
		Path backup = file.resolveSibling(file.getFileName() + ".v" + version + ".bak");
		if (Files.exists(backup))
		{
			backup = file.resolveSibling(file.getFileName() + ".v" + version + "." + System.currentTimeMillis() + ".bak");
		}
		return backup;
	}
}
