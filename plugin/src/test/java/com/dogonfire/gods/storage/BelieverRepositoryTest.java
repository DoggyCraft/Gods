package com.dogonfire.gods.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.dogonfire.gods.model.Believer;
import com.dogonfire.gods.util.LegacyDates;

class BelieverRepositoryTest
{
	private static final UUID	THOR_BELIEVER	= UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7");
	private static final UUID	ODIN_BELIEVER	= UUID.fromString("0b8d4f7e-3c1a-4bfa-9a3e-6f0f1c2d3e4f");
	private static final UUID	GODLESS			= UUID.fromString("5d0e2f3a-1b2c-4d5e-8f90-a1b2c3d4e5f6");

	@TempDir
	Path						dir;

	@Test
	void readsAnOldFile() throws IOException
	{
		Fixtures.copy("legacy/believers-0.9.yml", this.dir, "believers.yml");
		Storage storage = Fixtures.storage(this.dir);
		assertTrue(storage.believers().load());
		assertEquals(3, storage.believers().size());

		Believer thor = storage.believers().find(THOR_BELIEVER);
		assertEquals("Thor", thor.godName());
		assertEquals(12, thor.prayers());
		assertEquals(LegacyDates.SECONDS.parse("21:15:03 14-03-2019"), thor.lastPrayer());
		assertEquals(LegacyDates.SECONDS.parse("10:00:00 01-01-2019"), thor.joined());
		assertEquals(4, thor.prayerPower());
		assertTrue(thor.religionChat());
		assertFalse(thor.hunting());
		assertEquals("Odin", thor.invitationGod());
		assertEquals(LegacyDates.SECONDS.parse("21:16:00 14-03-2019"), thor.invitationTime());
		assertEquals(LegacyDates.SECONDS.parse("08:30:00 12-03-2019"), thor.lastPriestOffer());
		assertEquals(LegacyDates.SECONDS.parse("21:15:03 14-03-2019"), thor.lastBlessing());
		assertEquals(LegacyDates.SECONDS.parse("20:00:00 14-03-2019"), thor.lastItemBlessing());

		Believer odin = storage.believers().find(ODIN_BELIEVER);
		assertTrue(odin.hunting());
		assertEquals(LegacyDates.SECONDS.parse("07:01:00 02-02-2020"), odin.lastCursing());
		assertEquals(LegacyDates.SECONDS.parse("07:02:00 02-02-2020"), odin.lastHolyArtifactBlessing());
		assertEquals(LegacyDates.SECONDS.parse("07:03:00 02-02-2020"), odin.changingGod());
		assertEquals(3, odin.extras().get("SomethingNewer"));

		Believer godless = storage.believers().find(GODLESS);
		assertNull(godless.godName());
		assertNull(godless.lastPrayer(), "an unreadable time counts as never");
		storage.close();
	}

	@Test
	void savesWhatItReadAndKeepsWhatItDoesNotKnow() throws IOException
	{
		Fixtures.copy("legacy/believers-0.9.yml", this.dir, "believers.yml");
		Storage storage = Fixtures.storage(this.dir);
		storage.believers().load();
		String before = Fixtures.snapshot(storage.believers());
		storage.believers().markDirty();
		storage.close();

		YamlConfiguration written = YamlConfiguration.loadConfiguration(this.dir.resolve("believers.yml").toFile());
		assertEquals("21:15:03 14-03-2019", written.getString(THOR_BELIEVER + ".LastPrayer"));
		assertEquals("Odin", written.getString(THOR_BELIEVER + ".Invitation.God"));
		assertEquals("21:16:00 14-03-2019", written.getString(THOR_BELIEVER + ".Invitation.Time"));
		assertEquals(3, written.getInt(ODIN_BELIEVER + ".SomethingNewer"));
		// Entries keyed by name are not believers, but they stay in the file
		assertEquals(99, written.getInt("Notch.Prayers"));

		Storage again = Fixtures.storage(this.dir);
		assertTrue(again.believers().load());
		assertEquals(before, Fixtures.snapshot(again.believers()));
		again.close();
	}

	@Test
	void aNewBelieverIsSavedWithOnlyWhatIsSet() throws IOException
	{
		Storage storage = Fixtures.storage(this.dir);
		storage.believers().load();
		UUID id = UUID.randomUUID();
		Believer believer = storage.believers().getOrCreate(id);
		believer.setGodName("Loki");
		believer.setHunting(false);
		storage.believers().markDirty();
		storage.close();

		YamlConfiguration written = YamlConfiguration.loadConfiguration(this.dir.resolve("believers.yml").toFile());
		assertEquals(1, written.getKeys(false).size());
		assertEquals("Loki", written.getString(id + ".God"));
		assertFalse(written.contains(id + ".Invitation"));
		assertFalse(written.contains(id + ".Prayers"));
	}
}
