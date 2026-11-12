package com.dogonfire.gods.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.dogonfire.gods.model.ChunkKey;
import com.dogonfire.gods.model.HolyLand;
import com.dogonfire.gods.model.NeutralLand;
import com.dogonfire.gods.util.LegacyDates;

class HolyLandRepositoryTest
{
	@TempDir
	Path dir;

	private YamlConfiguration written()
	{
		return YamlConfiguration.loadConfiguration(this.dir.resolve("holyland.yml").toFile());
	}

	private static ChunkKey chunk(String world, int x, int z)
	{
		return new ChunkKey(world, x, z);
	}

	@Test
	void theOldFormatIsDroppedWithABackup() throws IOException
	{
		Fixtures.copy("legacy/holyland-0.9.yml", this.dir, "holyland.yml");
		String original = Files.readString(this.dir.resolve("holyland.yml"));

		Storage storage = Fixtures.storage(this.dir);
		assertTrue(storage.holyLands().load());
		assertEquals(0, storage.holyLands().size());
		assertTrue(storage.holyLands().allNeutral().isEmpty());
		assertTrue(storage.awaitWrites(10, TimeUnit.SECONDS));

		// Written back at once, without the old sections
		YamlConfiguration written = written();
		assertEquals(HolyLandRepository.DATA_VERSION, written.getInt("data-version"));
		assertFalse(written.contains("Holyland"));
		assertFalse(written.contains("NeutralLand"));
		assertEquals(original, Files.readString(this.dir.resolve("holyland.yml.v1.bak")));
		storage.close();
	}

	/** Each region of version 2 becomes one land of its 64 chunks, centered on its middle chunk; each neutral region 64 neutral chunks */
	private static void assertRegionsBecameChunks(Storage storage)
	{
		assertEquals(3, storage.holyLands().size());

		// Region 1, 1: chunks 8..15
		HolyLand thor = storage.holyLands().find(chunk("world", 12, 12));
		assertEquals("12,12", thor.id());
		assertEquals(chunk("world", 12, 12), thor.center());
		assertEquals(64, thor.chunks().size());
		assertSame(thor, storage.holyLands().find(chunk("world", 8, 8)));
		assertSame(thor, storage.holyLands().find(chunk("world", 15, 15)));
		assertNull(storage.holyLands().find(chunk("world", 16, 15)));
		assertNull(storage.holyLands().find(chunk("world", 7, 8)));
		assertEquals("Thor", thor.godName());
		assertEquals("Upper Flowerfield", thor.name());
		assertEquals(LegacyDates.MINUTES.parse("10:00 01-01-2025"), thor.firstPrayerTime());
		assertEquals(LegacyDates.MINUTES.parse("21:15 14-03-2025"), thor.lastPrayerTime());
		assertEquals("world", thor.worldName());
		assertNull(thor.attackingGodName());

		// Region -1, -1: chunks -8..-1, the attack on it kept
		HolyLand odin = storage.holyLands().find(ChunkKey.of("world", -1, -1));
		assertEquals(chunk("world", -4, -4), odin.center());
		assertSame(odin, storage.holyLands().find(chunk("world", -8, -8)));
		assertNull(storage.holyLands().find(chunk("world", 0, -1)));
		assertEquals("Odin", odin.godName());
		assertEquals("Thor", odin.attackingGodName());
		assertEquals(LegacyDates.MINUTES.parse("21:00 14-03-2025"), odin.contestedTime());
		assertEquals(3, odin.attackerKills());
		assertEquals(1, odin.defenderKills());
		assertEquals(7, odin.desecration());
		assertEquals("red", odin.extras().get("Banner"));

		// The world key of a world with a '.' in its name
		HolyLand loki = storage.holyLands().find(ChunkKey.of("my.world", 5, 5));
		assertEquals("Loki", loki.godName());
		assertEquals(chunk("my_world", 4, 4), loki.center());
		assertNull(storage.holyLands().find(ChunkKey.of("world", 5, 5)));

		// Region 2, 2: chunks 16..23
		assertEquals(64, storage.holyLands().allNeutral().size());
		NeutralLand neutral = storage.holyLands().findNeutral(chunk("world", 16, 23));
		assertEquals(LegacyDates.MINUTES.parse("12:00 01-01-2025"), neutral.since());
		assertNotNull(storage.holyLands().findNeutral(ChunkKey.of("world", 2 * 128, 2 * 128)));
		assertNull(storage.holyLands().findNeutral(chunk("world", 24, 16)));
	}

	@Test
	void theFormatBeforeDataVersionsBecomesChunks() throws IOException
	{
		Fixtures.copy("legacy/holyland-1.0-pre.yml", this.dir, "holyland.yml");
		Storage storage = Fixtures.storage(this.dir);
		assertTrue(storage.holyLands().load());
		assertRegionsBecameChunks(storage);
		assertTrue(Files.exists(this.dir.resolve("holyland.yml.v1.bak")));
		storage.close();
	}

	@Test
	void regionsBecomeChunks() throws IOException
	{
		Fixtures.copy("legacy/holyland-1.0.yml", this.dir, "holyland.yml");
		String original = Files.readString(this.dir.resolve("holyland.yml"));
		Storage storage = Fixtures.storage(this.dir);
		assertTrue(storage.holyLands().load());
		assertRegionsBecameChunks(storage);
		assertTrue(storage.awaitWrites(10, TimeUnit.SECONDS));
		assertEquals(original, Files.readString(this.dir.resolve("holyland.yml.v2.bak")));

		// Written back at once in the new layout
		YamlConfiguration written = written();
		assertEquals(3, written.getInt("data-version"));
		assertEquals("12,12", written.getString("Holylands.world.12,12.Center"));
		assertEquals(64, written.getStringList("Holylands.world.12,12.Chunks").size());
		assertEquals("12,12", written.getStringList("Holylands.world.12,12.Chunks").get(0), "the center first");
		assertTrue(written.getStringList("Holylands.world.12,12.Chunks").contains("8,8"));
		assertEquals("Thor", written.getString("Holylands.world.-4,-4.AttackingGodName"));
		assertEquals("kept as it is", written.getString("Holylands.world.notes"));
		assertFalse(written.contains("Holylands.world.4294967297"));
		assertEquals("12:00 01-01-2025", written.getString("NeutralLands.world.16,16"));
		assertFalse(written.contains("NeutralLands.world.8589934594"));
		storage.close();
	}

	@Test
	void savesWhatItReadAndKeepsWhatItDoesNotKnow() throws IOException
	{
		Fixtures.copy("legacy/holyland-1.0.yml", this.dir, "holyland.yml");
		Storage storage = Fixtures.storage(this.dir);
		storage.holyLands().load();
		String before = Fixtures.snapshot(storage.holyLands());
		storage.holyLands().markDirty();
		storage.close();

		YamlConfiguration written = written();
		assertEquals("red", written.getString("Holylands.world.-4,-4.Banner"));
		assertEquals("Upper Flowerfield", written.getString("Holylands.world.12,12.Name"));

		Storage again = Fixtures.storage(this.dir);
		assertTrue(again.holyLands().load());
		assertEquals(before, Fixtures.snapshot(again.holyLands()));
		assertRegionsBecameChunks(again);
		again.close();
		// Current now: only the one backup
		try (var files = Files.list(this.dir))
		{
			assertEquals(1, files.filter(path -> path.getFileName().toString().contains(".bak")).count());
		}
	}

	@Test
	void landsGrowAndAreRemovedWithTheirChunks() throws IOException
	{
		Storage storage = Fixtures.storage(this.dir);
		storage.holyLands().load();
		HolyLand land = storage.holyLands().create(chunk("world", 0, 0));
		land.setGodName("Thor");
		assertTrue(storage.holyLands().addChunk(land, chunk("world", 1, 0)));
		HolyLand other = storage.holyLands().create(chunk("world", 5, 5));
		assertFalse(storage.holyLands().addChunk(other, chunk("world", 1, 0)), "another land's chunk");
		assertTrue(storage.holyLands().addChunk(land, chunk("world", 1, 0)), "its own chunk");
		assertSame(land, storage.holyLands().find(chunk("world", 1, 0)));
		assertEquals("0,0", land.id());

		storage.holyLands().remove(land);
		assertNull(storage.holyLands().find(chunk("world", 1, 0)));
		assertNull(storage.holyLands().find(chunk("world", 0, 0)));
		assertSame(other, storage.holyLands().find(chunk("world", 5, 5)));
		storage.close();
	}

	@Test
	void anEndedAttackLeavesNoCounters() throws IOException
	{
		Fixtures.copy("legacy/holyland-1.0.yml", this.dir, "holyland.yml");
		Storage storage = Fixtures.storage(this.dir);
		storage.holyLands().load();
		storage.holyLands().find(chunk("world", -1, -1)).clearContest();
		storage.holyLands().markDirty();
		storage.close();

		YamlConfiguration written = written();
		assertEquals("Odin", written.getString("Holylands.world.-4,-4.GodName"));
		assertFalse(written.contains("Holylands.world.-4,-4.AttackingGodName"));
		assertFalse(written.contains("Holylands.world.-4,-4.AttackerKills"));
	}
}
