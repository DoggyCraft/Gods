package com.dogonfire.gods.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.dogonfire.gods.model.God;
import com.dogonfire.gods.model.ChunkKey;
import com.dogonfire.gods.util.LegacyDates;

/** A time that can't be parsed reads as never, but is written back as it was instead of being lost or becoming 1970 */
class UnparseableTimesTest
{
	private static final String	BAD		= "sometime last week";
	private static final UUID	PLAYER	= UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7");
	private static final UUID	PARTNER	= UUID.fromString("0b8d4f7e-3c1a-4bfa-9a3e-6f0f1c2d3e4f");

	@TempDir
	Path						dir;

	private YamlConfiguration written(String name)
	{
		return YamlConfiguration.loadConfiguration(this.dir.resolve(name).toFile());
	}

	@Test
	void godTimes() throws IOException
	{
		Files.writeString(this.dir.resolve("gods.yml"), "data-version: 2\nThor:\n  Created: " + BAD + "\n  PendingPriestTime: " + BAD + "\n  BlessedTime: " + BAD
				+ "\n  CursedTime: " + BAD + "\n  LastQuestTime: " + BAD + "\n  Verbosity: -5\nOdin:\n  Created: " + BAD + "\n");
		Storage storage = Fixtures.storage(this.dir);
		storage.gods().load();
		God thor = storage.gods().find("Thor");
		assertEquals(Instant.EPOCH, thor.created(), "a Created that can't be read still counts as created");
		assertNull(thor.blessedTime());
		assertEquals(0, thor.verbosity(), "a negative verbosity is generated again");

		// A new time replaces the unreadable one
		Instant now = LegacyDates.SECONDS.now();
		storage.gods().find("Odin").setCreated(now);
		storage.gods().markDirty();
		storage.close();

		YamlConfiguration written = written("gods.yml");
		for (String key : new String[] { "Created", "PendingPriestTime", "BlessedTime", "CursedTime", "LastQuestTime" })
		{
			assertEquals(BAD, written.getString("Thor." + key), key);
		}
		assertEquals(LegacyDates.SECONDS.format(now), written.getString("Odin.Created"));
	}

	@Test
	void believerTimes() throws IOException
	{
		Files.writeString(this.dir.resolve("believers.yml"), "data-version: 2\n" + PLAYER + ":\n  God: Thor\n  LastPrayer: " + BAD + "\n  Joined: " + BAD
				+ "\n  LastBlessingTime: " + BAD + "\n  Invitation:\n    God: Odin\n    Time: " + BAD + "\n");
		Storage storage = Fixtures.storage(this.dir);
		storage.believers().load();
		assertNull(storage.believers().find(PLAYER).lastPrayer());
		storage.believers().find(PLAYER).setLastBlessing(LegacyDates.SECONDS.now());
		storage.believers().markDirty();
		storage.close();

		YamlConfiguration written = written("believers.yml");
		assertEquals(BAD, written.getString(PLAYER + ".LastPrayer"));
		assertEquals(BAD, written.getString(PLAYER + ".Joined"));
		assertEquals(BAD, written.getString(PLAYER + ".Invitation.Time"));
		assertEquals("Odin", written.getString(PLAYER + ".Invitation.God"));
		assertNotNull(LegacyDates.SECONDS.parse(written.getString(PLAYER + ".LastBlessingTime")), "the new time replaced it");
	}

	@Test
	void holyLandTimes() throws IOException
	{
		Files.writeString(this.dir.resolve("holyland.yml"), "data-version: 3\nHolylands:\n  world:\n    '5,0':\n      GodName: Thor\n      FirstPrayerTime: " + BAD
				+ "\n      LastPrayerTime: " + BAD + "\n      AttackingGodName: Odin\n      ContestedTime: " + BAD + "\nNeutralLands:\n  world:\n    '7,0': " + BAD + "\n");
		Storage storage = Fixtures.storage(this.dir);
		storage.holyLands().load();
		assertNull(storage.holyLands().find(new ChunkKey("world", 5, 0)).firstPrayerTime());
		assertNull(storage.holyLands().findNeutral(new ChunkKey("world", 7, 0)).since());
		storage.holyLands().markDirty();
		storage.close();

		YamlConfiguration written = written("holyland.yml");
		assertEquals(BAD, written.getString("Holylands.world.5,0.FirstPrayerTime"));
		assertEquals(BAD, written.getString("Holylands.world.5,0.LastPrayerTime"));
		assertEquals(BAD, written.getString("Holylands.world.5,0.ContestedTime"));
		assertEquals(BAD, written.getString("NeutralLands.world.7,0"));
	}

	@Test
	void marriageTimes() throws IOException
	{
		Files.writeString(this.dir.resolve("marriages.yml"), "data-version: 2\n" + PLAYER + ":\n  Married:\n    Partner: " + PARTNER + "\n    Time: " + BAD + "\n    LastLove: " + BAD
				+ "\n");
		Storage storage = Fixtures.storage(this.dir);
		storage.marriages().load();
		assertNull(storage.marriages().find(PLAYER).wedlock().since());
		storage.marriages().markDirty();
		storage.close();

		YamlConfiguration written = written("marriages.yml");
		assertEquals(BAD, written.getString(PLAYER + ".Married.Time"));
		assertEquals(BAD, written.getString(PLAYER + ".Married.LastLove"));
		assertEquals(PARTNER.toString(), written.getString(PLAYER + ".Married.Partner"));
	}

	@Test
	void questTimes() throws IOException
	{
		Files.writeString(this.dir.resolve("quests.yml"), "data-version: 2\nThor:\n  Type: Slay\n  Amount: 3\n  CreatedTime: " + BAD + "\n");
		Storage storage = Fixtures.storage(this.dir);
		storage.quests().load();
		assertNull(storage.quests().find("Thor").createdTime());
		storage.quests().markDirty();
		storage.close();

		assertEquals(BAD, written("quests.yml").getString("Thor.CreatedTime"));
	}
}
