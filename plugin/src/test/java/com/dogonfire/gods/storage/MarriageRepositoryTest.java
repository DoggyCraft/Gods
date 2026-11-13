package com.dogonfire.gods.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dogonfire.gods.model.Marriage;
import com.dogonfire.gods.util.LegacyDates;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MarriageRepositoryTest {
  private static final UUID HUSBAND = UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7");
  private static final UUID WIFE = UUID.fromString("0b8d4f7e-3c1a-4bfa-9a3e-6f0f1c2d3e4f");
  private static final UUID BRIDE = UUID.fromString("5d0e2f3a-1b2c-4d5e-8f90-a1b2c3d4e5f6");
  private static final UUID GROOM = UUID.fromString("1a2b3c4d-1b2c-4d5e-8f90-a1b2c3d4e5f6");
  private static final UUID OLD = UUID.fromString("ffffffff-1b2c-4d5e-8f90-a1b2c3d4e5f6");

  @TempDir Path dir;

  private YamlConfiguration written() {
    return YamlConfiguration.loadConfiguration(this.dir.resolve("marriages.yml").toFile());
  }

  @Test
  void readsAnOldFile() throws IOException {
    Fixtures.copy("legacy/marriages-0.9.yml", this.dir, "marriages.yml");
    Storage storage = Fixtures.storage(this.dir);
    assertTrue(storage.marriages().load());
    assertEquals(5, storage.marriages().size());

    Marriage.Wedlock husband = storage.marriages().find(HUSBAND).wedlock();
    assertEquals(WIFE, husband.partner());
    assertEquals(LegacyDates.SECONDS.parse("20:00:00 01-03-2019"), husband.since());
    assertEquals(LegacyDates.SECONDS.parse("21:15:03 14-03-2019"), husband.lastLove());

    Marriage bride = storage.marriages().find(BRIDE);
    assertEquals(GROOM, bride.engagement().partner());
    assertTrue(bride.engagement().tokenPickedUp());
    assertNull(bride.proposal());

    Marriage groom = storage.marriages().find(GROOM);
    assertFalse(groom.engagement().tokenPickedUp());
    assertEquals(BRIDE, groom.proposal().partner());
    assertEquals(LegacyDates.SECONDS.parse("09:59:00 02-02-2020"), groom.proposal().time());

    // A partner kept by name is no partner, but the section stays
    assertNull(storage.marriages().find(OLD).wedlock());
    storage.close();
  }

  @Test
  void savesWhatItReadAndKeepsWhatItDoesNotKnow() throws IOException {
    Fixtures.copy("legacy/marriages-0.9.yml", this.dir, "marriages.yml");
    Storage storage = Fixtures.storage(this.dir);
    storage.marriages().load();
    String before = Fixtures.snapshot(storage.marriages());
    storage.marriages().markDirty();
    storage.close();

    YamlConfiguration written = written();
    assertEquals(WIFE.toString(), written.getString(HUSBAND + ".Married.Partner"));
    assertEquals("21:15:03 14-03-2019", written.getString(HUSBAND + ".Married.LastLove"));
    assertTrue(written.getBoolean(BRIDE + ".GettingMarried.HasPickupWeddingToken"));
    assertFalse(written.contains(GROOM + ".GettingMarried.HasPickupWeddingToken"));
    assertEquals("Sweetie", written.getString(BRIDE + ".Nickname"));
    assertEquals("Notch", written.getString(OLD + ".Married.Partner"));
    assertEquals(OLD.toString(), written.getString("Notch.Married.Partner"));

    Storage again = Fixtures.storage(this.dir);
    assertTrue(again.marriages().load());
    assertEquals(before, Fixtures.snapshot(again.marriages()));
    again.close();
  }

  @Test
  void anUnreadableSectionIsReplacedWhenTheStateChanges() throws IOException {
    Fixtures.copy("legacy/marriages-0.9.yml", this.dir, "marriages.yml");
    Storage storage = Fixtures.storage(this.dir);
    storage.marriages().load();
    Marriage old = storage.marriages().find(OLD);
    old.setWedlock(new Marriage.Wedlock(HUSBAND, null, null));
    storage.marriages().markDirty();
    storage.close();

    assertEquals(HUSBAND.toString(), written().getString(OLD + ".Married.Partner"));
    assertFalse(written().contains(OLD + ".Married.Time"));
  }
}
