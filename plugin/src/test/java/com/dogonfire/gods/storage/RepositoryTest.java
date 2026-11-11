package com.dogonfire.gods.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.dogonfire.gods.migration.Migration;
import com.dogonfire.gods.migration.VersionedYaml;

class RepositoryTest
{
	private static final Logger	LOG	= Logger.getAnonymousLogger();

	@TempDir
	Path						dir;

	private final List<Runnable>	queued	= new ArrayList<>();

	/** Knows the top-level sections that have a "Power" */
	static class PowerRepository extends Repository
	{
		final Map<String, Integer> powers = new LinkedHashMap<>();

		PowerRepository(DataFile file)
		{
			super(file, LOG);
		}

		@Override
		protected void read(YamlConfiguration yaml)
		{
			for (String key : yaml.getKeys(false))
			{
				if (yaml.isInt(key + ".Power"))
				{
					this.powers.put(key, yaml.getInt(key + ".Power"));
				}
				else
				{
					keepUnknown(key, yaml.get(key));
				}
			}
		}

		@Override
		protected void write(YamlConfiguration yaml)
		{
			this.powers.forEach((key, power) -> yaml.set(key + ".Power", power));
		}

		@Override
		protected void clear()
		{
			this.powers.clear();
		}

		@Override
		public int size()
		{
			return this.powers.size();
		}
	}

	private PowerRepository repository(VersionedYaml format)
	{
		return new PowerRepository(new DataFile(this.dir.resolve("data.yml"), LOG, this.queued::add, null, format));
	}

	private void runQueued()
	{
		List<Runnable> writes = new ArrayList<>(this.queued);
		this.queued.clear();
		writes.forEach(Runnable::run);
	}

	private String read() throws IOException
	{
		return Files.readString(this.dir.resolve("data.yml"));
	}

	@Test
	void writesTheModelsAndKeepsUnknownKeys() throws IOException
	{
		Files.writeString(this.dir.resolve("data.yml"), "Thor:\n  Power: 3\nnotes:\n  a: [1, 2]\n");
		PowerRepository repository = repository(null);
		assertTrue(repository.load());
		assertEquals(Map.of("Thor", 3), repository.powers);

		repository.powers.put("Odin", 5);
		repository.save();
		runQueued();

		YamlConfiguration written = YamlConfiguration.loadConfiguration(this.dir.resolve("data.yml").toFile());
		assertEquals(3, written.getInt("Thor.Power"));
		assertEquals(5, written.getInt("Odin.Power"));
		assertEquals(List.of(1, 2), written.getIntegerList("notes.a"));
	}

	@Test
	void aBrokenReloadKeepsTheModels() throws IOException
	{
		Files.writeString(this.dir.resolve("data.yml"), "Thor:\n  Power: 3\n");
		PowerRepository repository = repository(null);
		assertTrue(repository.load());

		Files.writeString(this.dir.resolve("data.yml"), "Thor: [unclosed\n");
		assertFalse(repository.load());
		assertEquals(Map.of("Thor", 3), repository.powers);
		assertTrue(repository.isReadOnly());
		repository.save();
		assertTrue(this.queued.isEmpty());
	}

	@Test
	void dataTheRepositoryCanNotMakeSenseOfIsUsedButNeverWritten() throws IOException
	{
		Files.writeString(this.dir.resolve("data.yml"), "Thor:\n  Power: 3\nOdin: 4\n");
		PowerRepository repository = new PowerRepository(new DataFile(this.dir.resolve("data.yml"), LOG, this.queued::add, null))
		{
			@Override
			protected void read(YamlConfiguration yaml)
			{
				super.read(yaml);
				// What Bukkit's getters throw on odd YAML
				throw new ClassCastException("a number where a section belongs");
			}
		};
		assertFalse(repository.load());
		assertTrue(repository.isReadOnly());
		assertEquals(Map.of("Thor", 3), repository.powers);
		repository.save();
		assertTrue(this.queued.isEmpty());
		assertEquals("Thor:\n  Power: 3\nOdin: 4\n", read());
	}

	@Test
	void aMigrationThatThrowsLeavesTheFileUsedButNeverWritten() throws IOException
	{
		Files.writeString(this.dir.resolve("data.yml"), "Thor:\n  Power: 3\n");
		Migration throwing = new Migration()
		{
			@Override
			public int fromVersion()
			{
				return 1;
			}

			@Override
			public YamlConfiguration migrate(YamlConfiguration old)
			{
				throw new NullPointerException("odd YAML");
			}
		};
		PowerRepository repository = repository(new VersionedYaml("data-version", 2, 1, List.of(throwing)));
		assertFalse(repository.load());
		assertTrue(repository.isReadOnly());
		assertEquals(Map.of("Thor", 3), repository.powers);
		repository.save();
		assertTrue(this.queued.isEmpty());
		assertEquals("Thor:\n  Power: 3\n", read());
	}

	@Test
	void aBrokenFirstLoadStartsEmptyAndNeverWrites() throws IOException
	{
		Files.writeString(this.dir.resolve("data.yml"), "Thor: [unclosed\n");
		PowerRepository repository = repository(null);
		assertFalse(repository.load());
		assertEquals(0, repository.size());
		repository.powers.put("Odin", 1);
		repository.save();
		assertTrue(this.queued.isEmpty());
		assertEquals("Thor: [unclosed\n", read());
	}

	@Test
	void anOldFileIsUpgradedBackedUpAndWrittenBackAtOnce() throws IOException
	{
		Files.writeString(this.dir.resolve("data.yml"), "Thor:\n  Strength: 3\n");
		Migration strengthToPower = new Migration()
		{
			@Override
			public int fromVersion()
			{
				return 1;
			}

			@Override
			public YamlConfiguration migrate(YamlConfiguration old)
			{
				old.set("Thor.Power", old.getInt("Thor.Strength"));
				old.set("Thor.Strength", null);
				return old;
			}
		};
		PowerRepository repository = repository(new VersionedYaml("data-version", 2, 1, List.of(strengthToPower)));
		assertTrue(repository.load());
		assertEquals(Map.of("Thor", 3), repository.powers);
		assertEquals(1, this.queued.size(), "written back right away");
		runQueued();

		YamlConfiguration written = YamlConfiguration.loadConfiguration(this.dir.resolve("data.yml").toFile());
		assertEquals(2, written.getInt("data-version"));
		assertEquals(3, written.getInt("Thor.Power"));
		assertEquals("Thor:\n  Strength: 3\n", Files.readString(this.dir.resolve("data.yml.v1.bak")));

		// Current now: no second backup, nothing to write
		PowerRepository again = repository(new VersionedYaml("data-version", 2, 1, List.of(strengthToPower)));
		assertTrue(again.load());
		assertTrue(this.queued.isEmpty());
		try (Stream<Path> files = Files.list(this.dir))
		{
			assertEquals(2, files.count());
		}
	}

	@Test
	void aNewFileGetsTheCurrentVersion() throws IOException
	{
		PowerRepository repository = repository(new VersionedYaml("data-version", 2, 1, List.of()));
		assertTrue(repository.load());
		repository.powers.put("Odin", 1);
		repository.save();
		runQueued();
		assertEquals(2, YamlConfiguration.loadConfiguration(this.dir.resolve("data.yml").toFile()).getInt("data-version"));
	}

	@Test
	void aFileThatCanNotBeUpgradedIsUsedButNeverWritten() throws IOException
	{
		Files.writeString(this.dir.resolve("data.yml"), "Thor:\n  Power: 3\n");
		// No migration from version 1
		PowerRepository repository = repository(new VersionedYaml("data-version", 2, 1, List.of()));
		assertFalse(repository.load());
		assertEquals(Map.of("Thor", 3), repository.powers);
		repository.save();
		assertTrue(this.queued.isEmpty());
		assertEquals("Thor:\n  Power: 3\n", read());
	}
}
