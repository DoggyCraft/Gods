package com.dogonfire.gods.migration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import com.dogonfire.gods.util.AtomicFiles;

/**
 * Loads a YAML file that carries a version number, upgrading it with {@link Migration}s when it is older than the
 * current version. The original file is copied to {@code <name>.v<old version>.bak} before it is upgraded.
 *
 * @param versionKey     the top-level key holding the version
 * @param currentVersion the version this build of the plugin writes
 * @param legacyVersion  the version to assume when an existing file has no version key
 */
public record VersionedYaml(String versionKey, int currentVersion, int legacyVersion, List<Migration> migrations)
{
	public static final class LoadException extends Exception
	{
		public LoadException(String message, Throwable cause)
		{
			super(message, cause);
		}
	}

	/** The result of {@link #upgrade}: the (possibly new) data, and whether any migration ran */
	public record Upgraded(YamlConfiguration yaml, boolean changed)
	{
	}

	/**
	 * Loads and, if needed, upgrades the file (and writes the upgraded file). Returns {@code null} when the file does
	 * not exist. A file from a newer version of the plugin is loaded as-is, with a warning.
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

		Upgraded upgraded = upgrade(yaml, file.toPath(), log);
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
	 * Upgrades data already read from {@code file} to the current version, after copying the file to
	 * {@code <name>.v<old version>.bak} (an existing backup is never overwritten: the new one then gets a timestamp).
	 * Does not write the upgraded data; the caller does. Data from a newer version is returned as-is, with a warning.
	 */
	public Upgraded upgrade(YamlConfiguration yaml, Path file, Logger log) throws LoadException
	{
		String name = file.getFileName().toString();
		int version = yaml.contains(versionKey) ? yaml.getInt(versionKey) : legacyVersion;
		if (version > currentVersion)
		{
			log.warning(name + " is version " + version + ", but this version of Gods only knows up to version " + currentVersion + ". Loading it as-is; consider updating Gods.");
			return new Upgraded(yaml, false);
		}
		if (version == currentVersion)
		{
			return new Upgraded(yaml, false);
		}

		Map<Integer, Migration> byVersion = new HashMap<>();
		for (Migration migration : migrations)
		{
			byVersion.put(migration.fromVersion(), migration);
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
			if (migration == null)
			{
				throw new LoadException("Don't know how to upgrade " + name + " from version " + version, null);
			}
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
		return new Upgraded(yaml, true);
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
