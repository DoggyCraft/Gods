package com.dogonfire.gods.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.dogonfire.gods.model.Quest;
import com.dogonfire.gods.model.QuestLocation;
import com.dogonfire.gods.model.QuestTarget;
import com.dogonfire.gods.model.QuestType;
import com.dogonfire.gods.util.LegacyDates;

class QuestRepositoryTest
{
	@TempDir
	Path					dir;

	private ExecutorService	writer;

	private QuestRepository open()
	{
		this.writer = Executors.newSingleThreadExecutor();
		return new QuestRepository(new DataFile(this.dir.resolve("quests.yml"), Fixtures.LOG, this.writer, null), Fixtures.LOG);
	}

	private void close(QuestRepository repository) throws InterruptedException
	{
		repository.file().flush();
		this.writer.shutdown();
		assertTrue(this.writer.awaitTermination(10, TimeUnit.SECONDS));
	}

	@AfterEach
	void stopWriter()
	{
		if (this.writer != null)
		{
			this.writer.shutdownNow();
		}
	}

	private YamlConfiguration written()
	{
		return YamlConfiguration.loadConfiguration(this.dir.resolve("quests.yml").toFile());
	}

	@Test
	void readsAnOldFileAndDropsWhatCanNoLongerBeUsed() throws Exception
	{
		Fixtures.copy("legacy/quests-0.9.yml", this.dir, "quests.yml");
		QuestRepository quests = open();
		assertTrue(quests.load());

		// Global and the holy wars of Loki and Thor (with their target) are gone
		assertEquals(4, quests.size());
		assertNull(quests.find("Loki"));
		assertNull(quests.find("Thor"));
		assertNull(quests.target(QuestTarget.keyFor(100, 64, 200)));

		Quest odin = quests.find("Odin");
		assertEquals(QuestType.PILGRIMAGE, odin.type());
		assertEquals("Holy mountain of Ollukka", odin.targetType());
		assertEquals(new QuestLocation("world", 812, 74, -1520, null), odin.location());
		assertEquals(21, odin.maxDuration());
		assertNull(odin.amount());
		assertEquals(0, odin.amountOrZero());
		assertEquals(LegacyDates.MINUTES.parse("20:15 14-04-2019"), odin.createdTime());

		QuestTarget target = quests.target(QuestTarget.keyFor(odin.location()));
		assertEquals("Odin", target.godName());
		assertTrue(target.location().isAt("world", 812, 74, -1520));

		Quest freya = quests.find("Freya");
		assertEquals(QuestType.SLAY, freya.type());
		assertEquals(4, freya.amount());
		assertEquals(1, freya.progress());
		assertEquals("SPIDER", freya.targetType());

		Quest baldur = quests.find("Baldur");
		assertEquals(QuestType.CONVERT, baldur.type());
		assertEquals(List.of("8667ba71-b85a-4004-af54-457a9734eed7", "0b8d4f7e-3c1a-4bfa-9a3e-6f0f1c2d3e4f"), baldur.players());

		// A type that can't be read is still a quest (it expires), and a bad date reads as never
		Quest hel = quests.find("Hel");
		assertEquals("7", hel.typeName());
		assertEquals(QuestType.NONE, hel.type());
		assertNull(hel.createdTime());

		// The cleanup was written back at once
		close(quests);
		YamlConfiguration written = written();
		assertFalse(written.contains("Global"));
		assertFalse(written.contains("Loki"));
		assertFalse(written.contains("QuestTargets.313038188"));
		assertEquals("Odin", written.getString("QuestTargets.-797570438.God"));
	}

	@Test
	void savesWhatItReadAndKeepsWhatItDoesNotKnow() throws Exception
	{
		Fixtures.copy("legacy/quests-0.9.yml", this.dir, "quests.yml");
		QuestRepository quests = open();
		quests.load();
		String before = Fixtures.snapshot(quests);
		quests.markDirty();
		close(quests);

		YamlConfiguration written = written();
		assertEquals("Pilgrimage", written.getString("Odin.Type"));
		assertFalse(written.contains("Odin.Amount"));
		assertFalse(written.contains("Odin.Progress"));
		assertEquals(812, written.getInt("Odin.Location.X"));
		assertEquals("20:15 14-04-2019", written.getString("Odin.CreatedTime"));
		assertEquals(50, written.getInt("Baldur.Reward"));
		assertEquals("Odin", written.getString("QuestTargets.-797570438.LockedBy"));
		assertEquals("20:40:12 14-04-2019", written.getString("QuestTargets.-797570438.LockedTime"));

		QuestRepository again = open();
		assertTrue(again.load());
		assertEquals(before, Fixtures.snapshot(again));
		close(again);
	}

	@Test
	void readsAndWritesACurrentFileUnchanged() throws Exception
	{
		Fixtures.copy("legacy/quests-1.0-pre.yml", this.dir, "quests.yml");
		QuestRepository quests = open();
		assertTrue(quests.load());
		assertEquals(3, quests.size());

		Quest ra = quests.find("Ra");
		assertEquals("GRASS_BLOCK", ra.location().ground());
		assertEquals("Ra", quests.target(QuestTarget.keyFor(ra.location())).godName());

		Quest isis = quests.find("Isis");
		assertEquals(QuestType.GIVEITEMS, isis.type());
		assertEquals(List.of("5d0e2f3a-1b2c-4d5e-8f90-a1b2c3d4e5f6"), isis.players());

		Quest anubis = quests.find("Anubis");
		assertEquals(QuestType.ITEMSACRIFICE, anubis.type());
		assertNull(anubis.progress());

		String before = Fixtures.snapshot(quests);
		quests.markDirty();
		close(quests);

		YamlConfiguration original = YamlConfiguration.loadConfiguration(Fixtures.copy("legacy/quests-1.0-pre.yml", this.dir, "original.yml").toFile());
		YamlConfiguration written = written();
		for (String key : original.getKeys(true))
		{
			if (!original.isConfigurationSection(key))
			{
				assertEquals(original.get(key), written.get(key), key);
			}
		}
		assertEquals(original.getKeys(true), written.getKeys(true));

		QuestRepository again = open();
		again.load();
		assertEquals(before, Fixtures.snapshot(again));
		close(again);
	}

	@Test
	void aNewQuestIsWrittenWithTheOldKeys() throws Exception
	{
		QuestRepository quests = open();
		quests.load();

		Quest quest = quests.getOrCreate("Zeus");
		quest.setType(QuestType.SLAY);
		quest.setAmount(3);
		quest.setTargetType("COW");
		quest.setMaxDuration(15);
		quest.setCreatedTime(LegacyDates.MINUTES.parse("12:00 01-11-2026"));
		QuestLocation location = new QuestLocation("world_nether", 1, 2, 3, null);
		quests.putTarget(new QuestTarget(QuestTarget.keyFor(location), "Zeus", location));
		quests.save();
		close(quests);

		YamlConfiguration written = written();
		assertEquals("Slay", written.getString("Zeus.Type"));
		assertEquals(3, written.getInt("Zeus.Amount"));
		assertFalse(written.contains("Zeus.Progress"));
		assertFalse(written.contains("Zeus.Players"));
		assertEquals("12:00 01-11-2026", written.getString("Zeus.CreatedTime"));
		assertEquals("world_nether", written.getString("QuestTargets." + QuestTarget.keyFor(1, 2, 3) + ".Location.World"));
	}

	@Test
	void theTargetKeyIsTheOldHash()
	{
		assertEquals("-797570438", QuestTarget.keyFor(812, 74, -1520));
		assertEquals("313038188", QuestTarget.keyFor(100, 64, 200));
		assertEquals("0", QuestTarget.keyFor(0, 0, 0));
	}

	@Test
	void questTypesReadInAnyCaseAndWriteCapitalized()
	{
		assertEquals(QuestType.ITEMSACRIFICE, QuestType.parse("Itemsacrifice"));
		assertEquals(QuestType.GIVEITEMS, QuestType.parse("GIVEITEMS"));
		assertEquals(QuestType.NONE, QuestType.parse("Dance"));
		assertEquals(QuestType.NONE, QuestType.parse(null));
		assertEquals("Pilgrimage", QuestType.PILGRIMAGE.toString());
	}

	@Test
	void anUnreadableFileIsNotOverwritten() throws IOException, InterruptedException
	{
		Path file = this.dir.resolve("quests.yml");
		java.nio.file.Files.writeString(file, "Odin: [broken\n");
		QuestRepository quests = open();
		assertFalse(quests.load());
		assertTrue(quests.isReadOnly());
		quests.getOrCreate("Odin").setType(QuestType.SLAY);
		quests.save();
		close(quests);
		assertEquals("Odin: [broken\n", java.nio.file.Files.readString(file));
	}
}
