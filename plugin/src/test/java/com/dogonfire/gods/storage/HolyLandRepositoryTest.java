package com.dogonfire.gods.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.dogonfire.gods.model.HolyLand;
import com.dogonfire.gods.model.RegionKey;
import com.dogonfire.gods.util.LegacyDates;

class HolyLandRepositoryTest
{
	@TempDir
	Path dir;

	private YamlConfiguration written()
	{
		return YamlConfiguration.loadConfiguration(this.dir.resolve("holyland.yml").toFile());
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
		assertTrue(storage.awaitWrites(10, java.util.concurrent.TimeUnit.SECONDS));

		// Written back at once, without the old sections
		YamlConfiguration written = written();
		assertEquals(HolyLandRepository.DATA_VERSION, written.getInt("data-version"));
		assertFalse(written.contains("Holyland"));
		assertFalse(written.contains("NeutralLand"));
		assertEquals(original, Files.readString(this.dir.resolve("holyland.yml.v1.bak")));
		storage.close();
	}

	@Test
	void theFormatBeforeDataVersionsLoadsAsItIs() throws IOException
	{
		Fixtures.copy("legacy/holyland-1.0-pre.yml", this.dir, "holyland.yml");
		Storage storage = Fixtures.storage(this.dir);
		assertTrue(storage.holyLands().load());
		assertEquals(3, storage.holyLands().size());

		HolyLand thor = storage.holyLands().find(new RegionKey("world", 4294967297L));
		assertEquals("Thor", thor.godName());
		assertEquals("Upper Flowerfield", thor.name());
		assertEquals(LegacyDates.MINUTES.parse("10:00 01-01-2025"), thor.firstPrayerTime());
		assertEquals(LegacyDates.MINUTES.parse("21:15 14-03-2025"), thor.lastPrayerTime());
		assertEquals("world", thor.worldName());
		assertNull(thor.attackingGodName());

		HolyLand odin = storage.holyLands().find(new RegionKey("world", -1L));
		assertEquals("Thor", odin.attackingGodName());
		assertEquals(LegacyDates.MINUTES.parse("21:00 14-03-2025"), odin.contestedTime());
		assertEquals(3, odin.attackerKills());
		assertEquals(1, odin.defenderKills());
		assertEquals(7, odin.desecration());

		// The world key of a world with a '.' in its name
		HolyLand loki = storage.holyLands().find(RegionKey.of("my.world", 5, 5));
		assertEquals("Loki", loki.godName());

		assertNotNull(storage.holyLands().findNeutral(RegionKey.of("world", 2 * 128, 2 * 128)));
		assertTrue(Files.exists(this.dir.resolve("holyland.yml.v1.bak")));
		storage.close();
	}

	@Test
	void savesWhatItReadAndKeepsWhatItDoesNotKnow() throws IOException
	{
		Fixtures.copy("legacy/holyland-1.0-pre.yml", this.dir, "holyland.yml");
		Storage storage = Fixtures.storage(this.dir);
		storage.holyLands().load();
		String before = Fixtures.snapshot(storage.holyLands());
		storage.holyLands().markDirty();
		storage.close();

		YamlConfiguration written = written();
		assertEquals("red", written.getString("Holylands.world.-1.Banner"));
		assertEquals("12:00 01-01-2025", written.getString("NeutralLands.world.8589934594"));
		assertEquals("Upper Flowerfield", written.getString("Holylands.world.4294967297.Name"));

		Storage again = Fixtures.storage(this.dir);
		assertTrue(again.holyLands().load());
		assertEquals(before, Fixtures.snapshot(again.holyLands()));
		again.close();
		// Current now: no new backup
		try (var files = Files.list(this.dir))
		{
			assertEquals(1, files.filter(path -> path.getFileName().toString().contains(".bak")).count());
		}
	}

	@Test
	void anEndedAttackLeavesNoCounters() throws IOException
	{
		Fixtures.copy("legacy/holyland-1.0-pre.yml", this.dir, "holyland.yml");
		Storage storage = Fixtures.storage(this.dir);
		storage.holyLands().load();
		storage.holyLands().find(new RegionKey("world", -1L)).clearContest();
		storage.holyLands().markDirty();
		storage.close();

		YamlConfiguration written = written();
		assertEquals("Odin", written.getString("Holylands.world.-1.GodName"));
		assertFalse(written.contains("Holylands.world.-1.AttackingGodName"));
		assertFalse(written.contains("Holylands.world.-1.AttackerKills"));
	}
}
