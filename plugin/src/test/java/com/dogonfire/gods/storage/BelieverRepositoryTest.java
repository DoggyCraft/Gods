package com.dogonfire.gods.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dogonfire.gods.model.Believer;
import com.dogonfire.gods.model.DevotionLevel;
import com.dogonfire.gods.util.LegacyDates;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BelieverRepositoryTest {
  private static final UUID THOR_BELIEVER = UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7");
  private static final UUID ODIN_BELIEVER = UUID.fromString("0b8d4f7e-3c1a-4bfa-9a3e-6f0f1c2d3e4f");
  private static final UUID GODLESS = UUID.fromString("5d0e2f3a-1b2c-4d5e-8f90-a1b2c3d4e5f6");

  @TempDir Path dir;

  @Test
  void readsAnOldFile() throws IOException {
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
  void savesWhatItReadAndKeepsWhatItDoesNotKnow() throws IOException {
    Fixtures.copy("legacy/believers-0.9.yml", this.dir, "believers.yml");
    Storage storage = Fixtures.storage(this.dir);
    storage.believers().load();
    String before = Fixtures.snapshot(storage.believers());
    storage.believers().markDirty();
    storage.close();

    YamlConfiguration written =
        YamlConfiguration.loadConfiguration(this.dir.resolve("believers.yml").toFile());
    assertEquals("21:15:03 14-03-2019", written.getString(THOR_BELIEVER + ".LastPrayer"));
    assertEquals("Odin", written.getString(THOR_BELIEVER + ".Invitation.God"));
    assertEquals("21:16:00 14-03-2019", written.getString(THOR_BELIEVER + ".Invitation.Time"));
    assertEquals(3, written.getInt(ODIN_BELIEVER + ".SomethingNewer"));
    // An entry keyed by a name the server doesn't know is kept under Unresolved
    assertEquals(99, written.getInt("Unresolved.Notch.Prayers"));

    Storage again = Fixtures.storage(this.dir);
    assertTrue(again.believers().load());
    assertEquals(before, Fixtures.snapshot(again.believers()));
    again.close();
  }

  @Test
  void believersKeptByNameAreMovedToTheirUuidWithABackup() throws IOException {
    UUID notch = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
    Path file = Fixtures.copy("legacy/believers-0.9.yml", this.dir, "believers.yml");
    String original = Files.readString(file);
    Storage storage = Fixtures.storage(this.dir, Map.of("notch", notch));
    assertTrue(storage.believers().load());

    Believer converted = storage.believers().find(notch);
    assertEquals("Thor", converted.godName());
    assertEquals(99, converted.prayers());
    assertEquals(4, storage.believers().size());
    storage.close();

    assertEquals(original, Files.readString(this.dir.resolve("believers.yml.v1.bak")));
    YamlConfiguration written = YamlConfiguration.loadConfiguration(file.toFile());
    assertEquals(2, written.getInt("data-version"));
    assertEquals(99, written.getInt(notch + ".Prayers"));
    assertFalse(written.contains("Notch"));
    assertFalse(written.contains("Unresolved"));
  }

  @Test
  void aNameWhosePlayerAlreadyHasAnEntryIsNotMerged() throws IOException {
    Files.writeString(
        this.dir.resolve("believers.yml"), THOR_BELIEVER + ":\n  God: Thor\nAnna:\n  God: Odin\n");
    Storage storage = Fixtures.storage(this.dir, Map.of("anna", THOR_BELIEVER));
    assertTrue(storage.believers().load());
    assertEquals("Thor", storage.believers().find(THOR_BELIEVER).godName());
    storage.close();

    YamlConfiguration written =
        YamlConfiguration.loadConfiguration(this.dir.resolve("believers.yml").toFile());
    assertEquals("Odin", written.getString("Unresolved.Anna.God"));
  }

  @Test
  void aNewBelieverIsSavedWithOnlyWhatIsSet() throws IOException {
    Storage storage = Fixtures.storage(this.dir);
    storage.believers().load();
    UUID id = UUID.randomUUID();
    Believer believer = storage.believers().getOrCreate(id);
    believer.setGodName("Loki");
    believer.setHunting(false);
    storage.believers().markDirty();
    storage.close();

    YamlConfiguration written =
        YamlConfiguration.loadConfiguration(this.dir.resolve("believers.yml").toFile());
    assertEquals(Set.of("data-version", id.toString()), written.getKeys(false));
    assertEquals("Loki", written.getString(id + ".God"));
    assertFalse(written.contains(id + ".Invitation"));
    assertFalse(written.contains(id + ".Prayers"));
  }

  /**
   * Devotion and who sent an invitation survive a save; a new believer starts with 0 points, an old
   * one has none
   */
  @Test
  void devotionIsSavedAndReadBack() throws IOException {
    Fixtures.copy("legacy/believers-0.9.yml", this.dir, "believers.yml");
    Storage storage = Fixtures.storage(this.dir);
    storage.believers().load();
    Believer thor = storage.believers().find(THOR_BELIEVER);
    assertNull(thor.devotion(), "a believer from before devotion is seeded once");
    thor.setDevotion(640);
    thor.setDevotionLevel(DevotionLevel.ZEALOT);
    thor.setDevotionSince(LegacyDates.SECONDS.parse("10:00:00 01-11-2026"));
    thor.setDevotionFaded(LegacyDates.SECONDS.parse("11:00:00 05-11-2026"));
    thor.setInvitedBy(ODIN_BELIEVER);
    UUID id = UUID.randomUUID();
    assertEquals(0, storage.believers().getOrCreate(id).devotion());
    storage.believers().markDirty();
    storage.close();

    YamlConfiguration written =
        YamlConfiguration.loadConfiguration(this.dir.resolve("believers.yml").toFile());
    assertEquals(640, written.getInt(THOR_BELIEVER + ".Devotion"));
    assertEquals("zealot", written.getString(THOR_BELIEVER + ".DevotionLevel"));
    assertEquals("10:00:00 01-11-2026", written.getString(THOR_BELIEVER + ".DevotionSince"));
    assertEquals(ODIN_BELIEVER.toString(), written.getString(THOR_BELIEVER + ".Invitation.By"));
    assertEquals(0, written.getInt(id + ".Devotion", -1));
    assertFalse(written.contains(id + ".DevotionLevel"), "a follower's level isn't written");
    assertFalse(written.contains(ODIN_BELIEVER + ".Devotion"));

    Storage again = Fixtures.storage(this.dir);
    assertTrue(again.believers().load());
    Believer read = again.believers().find(THOR_BELIEVER);
    assertEquals(640, read.devotion());
    assertEquals(DevotionLevel.ZEALOT, read.devotionLevel());
    assertEquals(thor.devotionSince(), read.devotionSince());
    assertEquals(thor.devotionFaded(), read.devotionFaded());
    assertEquals(ODIN_BELIEVER, read.invitedBy());
    again.close();
  }

  /** A devotion that can't be read is kept as it was */
  @Test
  void anUnreadableDevotionIsKept() throws IOException {
    Files.writeString(
        this.dir.resolve("believers.yml"),
        "data-version: 2\n"
            + THOR_BELIEVER
            + ":\n  God: Thor\n  Devotion: lots\n  DevotionLevel: pope\n");
    Storage storage = Fixtures.storage(this.dir);
    assertTrue(storage.believers().load());
    Believer thor = storage.believers().find(THOR_BELIEVER);
    assertNull(thor.devotion());
    assertNull(thor.devotionLevel());
    storage.believers().markDirty();
    storage.close();

    YamlConfiguration written =
        YamlConfiguration.loadConfiguration(this.dir.resolve("believers.yml").toFile());
    assertEquals("lots", written.getString(THOR_BELIEVER + ".Devotion"));
    assertEquals("pope", written.getString(THOR_BELIEVER + ".DevotionLevel"));
  }
}
