package com.dogonfire.gods.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
		storage.quests().load();
		storage.quests().yaml().set("Thor.Power", 42.5);
		storage.quests().yaml().set("Thor.Priests", List.of("8667ba71-b85a-4004-af54-457a9734eed7"));
		storage.quests().markDirty();
		storage.close();

		Storage again = storage();
		assertTrue(again.quests().load());
		assertEquals(42.5, again.quests().yaml().getDouble("Thor.Power"));
		assertEquals(List.of("8667ba71-b85a-4004-af54-457a9734eed7"), again.quests().yaml().getStringList("Thor.Priests"));
		again.close();
	}

	@Test
	void missingFileIsEmptyAndNotWrittenUntilChanged() throws IOException
	{
		Storage storage = storage();
		assertTrue(storage.bibles().load());
		assertTrue(storage.bibles().yaml().getKeys(false).isEmpty());
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
		Path file = this.dir.resolve("quests.yml");
		Files.writeString(file, BROKEN);

		Storage storage = storage();
		assertFalse(storage.quests().load());
		assertTrue(storage.quests().isReadOnly());
		assertTrue(storage.quests().yaml().getKeys(false).isEmpty());

		storage.quests().yaml().set("Odin.Power", 1);
		storage.quests().save();
		storage.close();

		assertEquals(BROKEN, Files.readString(file));
		List<String> files = filesInDir();
		assertEquals(2, files.size());
		assertEquals("quests.yml", files.get(0));
		assertTrue(files.get(1).startsWith("quests.yml.broken-"), files.get(1));
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
		Files.writeString(this.dir.resolve("gods.yml"), "data-version: 2\nThor:\n  BlessedPlayer: !!java.util.UUID '8667ba71-b85a-4004-af54-457a9734eed7'\n");

		Storage storage = storage();
		assertTrue(storage.gods().load());
		assertEquals(UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7"), storage.gods().find("Thor").blessedPlayer());
		storage.close();

		// Written back without the tags
		String written = Files.readString(this.dir.resolve("gods.yml"));
		assertFalse(written.contains("!!"), written);
		assertTrue(written.contains("8667ba71-b85a-4004-af54-457a9734eed7"), written);
	}

	@Test
	void awaitWritesWaitsForQueuedWrites()
	{
		Storage storage = storage();
		storage.bibles().load();
		for (int i = 0; i < 50; i++)
		{
			storage.bibles().yaml().set("book" + i + ".Title", "p" + i);
			storage.bibles().save();
		}
		assertTrue(storage.awaitWrites(10, TimeUnit.SECONDS));

		YamlConfiguration written = YamlConfiguration.loadConfiguration(this.dir.resolve("bibles.yml").toFile());
		assertEquals("p49", written.getString("book49.Title"));
		storage.close();
	}

	@Test
	void nothingIsWrittenAfterClose() throws IOException
	{
		Storage storage = storage();
		storage.bibles().load();
		storage.close();

		storage.bibles().yaml().set("Thor.Title", "Book");
		storage.bibles().save();
		assertTrue(storage.bibles().isDirty());
		assertEquals(List.of(), filesInDir());
	}
	@Test
	void reloadKeepsUnsavedChangesOfFilesNobodyEdited()
	{
		Storage storage = storage();
		storage.quests().load();
		storage.quests().yaml().set("Thor.Power", 1.0);
		storage.quests().markDirty();

		assertTrue(storage.prepareReload(5, TimeUnit.SECONDS));
		assertTrue(storage.quests().load());
		storage.finishReload();

		assertTrue(storage.quests().lastLoadKept());
		assertEquals(1.0, storage.quests().yaml().getDouble("Thor.Power"));
		storage.close();

		Storage again = storage();
		again.quests().load();
		assertEquals(1.0, again.quests().yaml().getDouble("Thor.Power"));
		again.close();
	}

	@Test
	void reloadReadsFilesEditedOnDiskAndDropsUnsavedChanges() throws IOException
	{
		Storage storage = storage();
		storage.quests().load();
		storage.quests().yaml().set("Thor.Power", 1.0);
		storage.quests().save();
		assertTrue(storage.awaitWrites(5, TimeUnit.SECONDS));

		// A change that isn't saved yet, then an admin edits the file
		storage.quests().yaml().set("Thor.Power", 2.0);
		storage.quests().markDirty();
		Path file = this.dir.resolve("quests.yml");
		Files.writeString(file, "Thor:\n  Power: 5.0\n");
		Files.setLastModifiedTime(file, FileTime.fromMillis(Files.getLastModifiedTime(file).toMillis() + 10_000));

		assertTrue(storage.prepareReload(5, TimeUnit.SECONDS));
		assertTrue(storage.quests().load());
		storage.finishReload();

		assertFalse(storage.quests().lastLoadKept());
		assertEquals(5.0, storage.quests().yaml().getDouble("Thor.Power"));
		storage.close();

		Storage again = storage();
		again.quests().load();
		assertEquals(5.0, again.quests().yaml().getDouble("Thor.Power"));
		again.close();
	}

}
