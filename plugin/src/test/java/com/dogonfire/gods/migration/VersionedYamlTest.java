package com.dogonfire.gods.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VersionedYamlTest
{
	private static final Logger				LOG			= Logger.getAnonymousLogger();

	/** Renames "a" to "b" (v1 to v2) and "b" to "c" (v2 to v3). */
	private static final List<Migration>	MIGRATIONS	= List.of(rename(1, "a", "b"), rename(2, "b", "c"));

	@TempDir
	Path									dir;

	private static Migration rename(int from, String oldKey, String newKey)
	{
		return new Migration()
		{
			@Override
			public int fromVersion()
			{
				return from;
			}

			@Override
			public YamlConfiguration migrate(YamlConfiguration old)
			{
				old.set(newKey, old.get(oldKey));
				old.set(oldKey, null);
				return old;
			}
		};
	}

	private File write(String content) throws IOException
	{
		Path file = this.dir.resolve("test.yml");
		Files.writeString(file, content);
		return file.toFile();
	}

	@Test
	void runsMigrationsInOrderAndBacksUpTheOriginal() throws Exception
	{
		File file = write("a: 42\n");
		YamlConfiguration yaml = new VersionedYaml("version", 3, 1, MIGRATIONS).load(file, LOG);

		assertEquals(42, yaml.getInt("c"));
		assertEquals(3, yaml.getInt("version"));
		// Saved to disk, with the original kept next to it
		assertEquals(42, YamlConfiguration.loadConfiguration(file).getInt("c"));
		assertEquals("a: 42\n", Files.readString(this.dir.resolve("test.yml.v1.bak")));
		assertTrue(Files.notExists(this.dir.resolve("test.yml.tmp")), "the temporary file is moved into place");
	}

	@Test
	void startsFromTheVersionInTheFile() throws Exception
	{
		File file = write("version: 2\nb: 7\n");
		YamlConfiguration yaml = new VersionedYaml("version", 3, 1, MIGRATIONS).load(file, LOG);
		assertEquals(7, yaml.getInt("c"));
		assertTrue(Files.exists(this.dir.resolve("test.yml.v2.bak")));
	}

	@Test
	void leavesCurrentAndNewerFilesAlone() throws Exception
	{
		for (String content : List.of("version: 3\nz: 1\n", "version: 4\nz: 1\n"))
		{
			File file = write(content);
			YamlConfiguration yaml = new VersionedYaml("version", 3, 1, MIGRATIONS).load(file, LOG);
			assertEquals(1, yaml.getInt("z"));
			assertEquals(content, Files.readString(file.toPath()));
		}
		try (var files = Files.list(this.dir))
		{
			assertEquals(List.of("test.yml"), files.map(path -> path.getFileName().toString()).toList(), "no backups");
		}
	}

	@Test
	void missingFileGivesNull() throws Exception
	{
		assertNull(new VersionedYaml("version", 3, 1, MIGRATIONS).load(this.dir.resolve("nope.yml").toFile(), LOG));
	}

	@Test
	void failsWhenAMigrationIsMissing() throws Exception
	{
		File file = write("a: 1\n");
		VersionedYaml format = new VersionedYaml("version", 3, 1, List.of(rename(2, "b", "c")));
		assertThrows(VersionedYaml.LoadException.class, () -> format.load(file, LOG));
		// The file itself is not touched, and no backup is made (it would be made again on every start)
		assertEquals("a: 1\n", Files.readString(file.toPath()));
		try (var files = Files.list(this.dir))
		{
			assertEquals(List.of("test.yml"), files.map(path -> path.getFileName().toString()).toList(), "no backups");
		}
	}

	@Test
	void aVersionWithoutMigrationsMakesNoBackup() throws Exception
	{
		// Like a messages file with a wrong messages-version: no migration chain at all
		File file = write("version: 1\nz: 1\n");
		for (int i = 0; i < 2; i++)
		{
			assertThrows(VersionedYaml.LoadException.class, () -> new VersionedYaml("version", 2, 2, List.of()).load(file, LOG));
		}
		try (var files = Files.list(this.dir))
		{
			assertEquals(List.of("test.yml"), files.map(path -> path.getFileName().toString()).toList(), "no backups");
		}
	}

	@Test
	void aQuotedVersionIsANumber() throws Exception
	{
		File file = write("version: '3'\nz: 1\n");
		YamlConfiguration yaml = new VersionedYaml("version", 3, 1, MIGRATIONS).load(file, LOG);
		assertEquals(1, yaml.getInt("z"));
		assertEquals("version: '3'\nz: 1\n", Files.readString(file.toPath()));

		File unreadable = write("version: soon\nz: 1\n");
		assertThrows(VersionedYaml.LoadException.class, () -> new VersionedYaml("version", 3, 1, MIGRATIONS).load(unreadable, LOG));
	}

	@Test
	void newerDataIsMarked() throws Exception
	{
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.set("version", 4);
		VersionedYaml.Upgraded upgraded = new VersionedYaml("version", 3, 1, MIGRATIONS).upgrade(yaml, this.dir.resolve("test.yml"), LOG);
		assertTrue(upgraded.newer());
		assertFalse(upgraded.changed());
	}

	@Test
	void aFileInTheCurrentLayoutWithoutItsVersionGetsTheVersion() throws Exception
	{
		File file = write("c: 5\n");
		YamlConfiguration yaml = new VersionedYaml("version", 3, 1, MIGRATIONS, data -> data.contains("c")).load(file, LOG);
		assertEquals(5, yaml.getInt("c"));
		assertEquals(3, YamlConfiguration.loadConfiguration(file).getInt("version"));
		assertEquals(5, YamlConfiguration.loadConfiguration(file).getInt("c"));
	}

	@Test
	void failsOnBrokenYamlWithoutTouchingIt() throws Exception
	{
		File file = write("a: [unclosed\n");
		assertThrows(VersionedYaml.LoadException.class, () -> new VersionedYaml("version", 3, 1, MIGRATIONS).load(file, LOG));
		assertEquals("a: [unclosed\n", Files.readString(file.toPath()));
		assertTrue(Files.notExists(this.dir.resolve("test.yml.v1.bak")));
	}

	@Test
	void neverOverwritesAnOlderBackup() throws Exception
	{
		Files.writeString(this.dir.resolve("test.yml.v1.bak"), "older: true\n");
		File file = write("a: 42\n");
		new VersionedYaml("version", 3, 1, MIGRATIONS).load(file, LOG);

		assertEquals("older: true\n", Files.readString(this.dir.resolve("test.yml.v1.bak")));
		try (var files = Files.list(this.dir))
		{
			List<String> backups = files.map(path -> path.getFileName().toString()).filter(name -> name.startsWith("test.yml.v1.") && !name.equals("test.yml.v1.bak")).toList();
			assertEquals(1, backups.size());
			assertEquals("a: 42\n", Files.readString(this.dir.resolve(backups.get(0))));
		}
	}
}
