package com.dogonfire.gods.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StorageTest
{
	private static final Logger	LOG		= Logger.getAnonymousLogger();
	private static final String	BROKEN	= "Thor:\n  Priests: [unclosed\n";

	@TempDir
	Path						dir;

	/** Writes queued here run only when the test says so */
	private final List<Runnable>	queued	= new ArrayList<>();

	private DataFile queuedFile(String name)
	{
		return new DataFile(this.dir.resolve(name), LOG, this.queued::add, null);
	}

	private void runQueued()
	{
		List<Runnable> writes = new ArrayList<>(this.queued);
		this.queued.clear();
		writes.forEach(Runnable::run);
	}

	private Storage storage()
	{
		return new Storage(this.dir, LOG, Executors.newSingleThreadExecutor());
	}

	private List<String> filesInDir() throws IOException
	{
		try (Stream<Path> files = Files.list(this.dir))
		{
			return files.map(path -> path.getFileName().toString()).sorted().toList();
		}
	}

	@Test
	void savedDataLoadsBack()
	{
		Storage storage = storage();
		storage.gods().load();
		storage.gods().yaml().set("Thor.Power", 42.5);
		storage.gods().yaml().set("Thor.Priests", List.of("8667ba71-b85a-4004-af54-457a9734eed7"));
		storage.gods().markDirty();
		storage.close();

		Storage again = storage();
		assertTrue(again.gods().load());
		assertEquals(42.5, again.gods().yaml().getDouble("Thor.Power"));
		assertEquals(List.of("8667ba71-b85a-4004-af54-457a9734eed7"), again.gods().yaml().getStringList("Thor.Priests"));
		again.close();
	}

	@Test
	void missingFileIsEmptyAndNotWrittenUntilChanged() throws IOException
	{
		Storage storage = storage();
		assertTrue(storage.believers().load());
		assertTrue(storage.believers().yaml().getKeys(false).isEmpty());
		storage.flush();
		storage.close();

		assertEquals(List.of(), filesInDir());
	}

	@Test
	void flushWritesWhatTheDataWasAtTheTimeThroughATempFile() throws IOException
	{
		DataFile file = queuedFile("quests.yml");
		file.load();
		file.yaml().set("Thor.Amount", 1);
		file.save();

		// Changed after the snapshot was taken: belongs to the next write
		file.yaml().set("Thor.Amount", 2);
		assertFalse(file.isDirty());
		assertEquals(1, this.queued.size());
		runQueued();

		assertEquals(1, YamlConfiguration.loadConfiguration(this.dir.resolve("quests.yml").toFile()).getInt("Thor.Amount"));
		assertEquals(List.of("quests.yml"), filesInDir());
	}

	@Test
	void unchangedFileIsNotWritten()
	{
		DataFile file = queuedFile("bibles.yml");
		file.load();
		file.flush();
		assertTrue(this.queued.isEmpty());
	}

	@Test
	void brokenFileIsKeptAndNeverOverwritten() throws IOException
	{
		Path gods = this.dir.resolve("gods.yml");
		Files.writeString(gods, BROKEN);

		Storage storage = storage();
		assertFalse(storage.gods().load());
		assertTrue(storage.gods().isReadOnly());
		assertTrue(storage.gods().yaml().getKeys(false).isEmpty());

		storage.gods().yaml().set("Odin.Power", 1);
		storage.gods().save();
		storage.close();

		assertEquals(BROKEN, Files.readString(gods));
		List<String> files = filesInDir();
		assertEquals(2, files.size());
		assertEquals("gods.yml", files.get(0));
		assertTrue(files.get(1).startsWith("gods.yml.broken-"), files.get(1));
		assertEquals(BROKEN, Files.readString(this.dir.resolve(files.get(1))));
	}

	@Test
	void brokenOnReloadKeepsTheDataInMemoryUntilFixed() throws IOException
	{
		Path believers = this.dir.resolve("believers.yml");
		Files.writeString(believers, "abc:\n  God: Thor\n");
		DataFile file = queuedFile("believers.yml");
		assertTrue(file.load());

		Files.writeString(believers, BROKEN);
		assertFalse(file.load());
		assertTrue(file.isReadOnly());
		assertEquals("Thor", file.yaml().getString("abc.God"));
		file.save();
		assertTrue(this.queued.isEmpty());

		Files.writeString(believers, "abc:\n  God: Odin\n");
		assertTrue(file.load());
		assertFalse(file.isReadOnly());
		assertEquals("Odin", file.yaml().getString("abc.God"));
		file.save();
		assertEquals(1, this.queued.size());
	}

	@Test
	void uuidTagsInGodsYmlAreRepaired() throws IOException
	{
		Files.writeString(this.dir.resolve("gods.yml"), "Thor:\n  BlessedPlayer: !!java.util.UUID '8667ba71-b85a-4004-af54-457a9734eed7'\n");

		Storage storage = storage();
		assertTrue(storage.gods().load());
		assertEquals("8667ba71-b85a-4004-af54-457a9734eed7", storage.gods().yaml().getString("Thor.BlessedPlayer"));
		// Written back without the tags
		assertTrue(storage.gods().isDirty());
		storage.close();

		assertFalse(Files.readString(this.dir.resolve("gods.yml")).contains("!!"));
	}

	@Test
	void awaitWritesWaitsForQueuedWrites()
	{
		Storage storage = storage();
		storage.marriages().load();
		for (int i = 0; i < 50; i++)
		{
			storage.marriages().yaml().set("couple" + i + ".Married.Partner", "p" + i);
			storage.marriages().save();
		}
		assertTrue(storage.awaitWrites(10, TimeUnit.SECONDS));

		YamlConfiguration written = YamlConfiguration.loadConfiguration(this.dir.resolve("marriages.yml").toFile());
		assertEquals("p49", written.getString("couple49.Married.Partner"));
		storage.close();
	}

	@Test
	void nothingIsWrittenAfterClose() throws IOException
	{
		Storage storage = storage();
		storage.holyLands().load();
		storage.close();

		storage.holyLands().yaml().set("Holylands.world.1.GodName", "Thor");
		storage.holyLands().save();
		assertTrue(storage.holyLands().isDirty());
		assertEquals(List.of(), filesInDir());
	}
}
