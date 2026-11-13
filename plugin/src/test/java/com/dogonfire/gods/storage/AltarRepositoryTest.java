package com.dogonfire.gods.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dogonfire.gods.model.Altar;
import com.dogonfire.gods.model.AltarKind;
import com.dogonfire.gods.model.BlockPos;
import com.dogonfire.gods.util.LegacyDates;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AltarRepositoryTest {
  private static final UUID STEVE = UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7");

  @TempDir Path dir;

  private static final String FILE =
      """
      data-version: 1
      Altars:
      - World: world
        Sign: 10,64,-3
        Block: 10,64,-4
        Kind: praying
        God: Zeus
        Created: 12:00:00 01-01-2026
        Creator: 8667ba71-b85a-4004-af54-457a9734eed7
        Note: built for the opening
      - World: world.of.gods
        Sign: 15,70,16
        Block: 16,70,16
        Kind: curses
        God: Hades
      - World: world
        Sign: here
        Kind: praying
      - World: world
        Sign: 1,2,3
        Block: 1,2,4
        Kind: praying
        God: Thor
        Created: yesterday
      Comment: kept
      """;

  @Test
  void readsTheFileAndIndexesTheAltars() throws IOException {
    Files.writeString(this.dir.resolve("altars.yml"), FILE);
    Storage storage = Fixtures.storage(this.dir);
    assertTrue(storage.altars().load());
    assertEquals(3, storage.altars().size());

    Altar zeus = storage.altars().atSign(new BlockPos("world", 10, 64, -3));
    assertEquals("Zeus", zeus.god());
    assertEquals(AltarKind.PRAYING, zeus.kind());
    assertEquals(new BlockPos("world", 10, 64, -4), zeus.block());
    assertEquals(LegacyDates.SECONDS.parse("12:00:00 01-01-2026"), zeus.created());
    assertEquals(STEVE, zeus.creator());
    assertEquals(List.of(zeus), storage.altars().onBlock(new BlockPos("world", 10, 64, -4)));
    assertEquals(List.of(zeus), storage.altars().forGod("Zeus"));

    // A world name with dots is a value, not a YAML path
    Altar hades = storage.altars().atSign(new BlockPos("world.of.gods", 15, 70, 16));
    assertEquals(AltarKind.CURSES, hades.kind());
    assertNull(hades.created());
    assertNull(hades.creator());

    // A time that can't be read is known as never, but kept
    assertNull(storage.altars().atSign(new BlockPos("world", 1, 2, 3)).created());
    storage.close();
  }

  @Test
  void savesWhatItReadAndKeepsWhatItDoesNotKnow() throws IOException {
    Files.writeString(this.dir.resolve("altars.yml"), FILE);
    Storage storage = Fixtures.storage(this.dir);
    storage.altars().load();
    String before = Fixtures.snapshot(storage.altars());
    storage.altars().markDirty();
    storage.close();

    YamlConfiguration written =
        YamlConfiguration.loadConfiguration(this.dir.resolve("altars.yml").toFile());
    assertEquals(AltarRepository.DATA_VERSION, written.getInt("data-version"));
    assertEquals("kept", written.getString("Comment"));
    List<Map<?, ?>> altars = written.getMapList("Altars");
    assertEquals(4, altars.size());
    assertEquals("built for the opening", altars.get(0).get("Note"));
    assertEquals("12:00:00 01-01-2026", altars.get(0).get("Created"));
    // The entry that couldn't be read is written back as it was
    assertTrue(altars.contains(Map.of("World", "world", "Sign", "here", "Kind", "praying")));
    assertTrue(altars.stream().anyMatch(altar -> "yesterday".equals(altar.get("Created"))));
    // A version 1 file needs no upgrade, so no backup
    assertFalse(Files.exists(this.dir.resolve("altars.yml.v1.bak")));

    Storage again = Fixtures.storage(this.dir);
    assertTrue(again.altars().load());
    assertEquals(before, Fixtures.snapshot(again.altars()));
    again.close();
  }

  @Test
  void aNewFileGetsTheDataVersion() throws IOException {
    Storage storage = Fixtures.storage(this.dir);
    storage.altars().load();
    storage
        .altars()
        .add(
            new Altar(
                new BlockPos("world", 0, 64, 1),
                new BlockPos("world", 0, 64, 0),
                AltarKind.PRAYING,
                "Freya",
                LegacyDates.SECONDS.parse("10:00:00 01-01-2026"),
                STEVE));
    storage.altars().markDirty();
    storage.close();

    YamlConfiguration written =
        YamlConfiguration.loadConfiguration(this.dir.resolve("altars.yml").toFile());
    assertEquals(1, written.getInt("data-version"));
    Map<?, ?> altar = written.getMapList("Altars").getFirst();
    assertEquals("world", altar.get("World"));
    assertEquals("0,64,1", altar.get("Sign"));
    assertEquals("0,64,0", altar.get("Block"));
    assertEquals("praying", altar.get("Kind"));
    assertEquals("Freya", altar.get("God"));
    assertEquals(STEVE.toString(), altar.get("Creator"));
    assertFalse(Files.readString(this.dir.resolve("altars.yml")).contains("!!"));
  }

  @Test
  void aFileFromANewerGodsIsNotWritten() throws IOException {
    Files.writeString(this.dir.resolve("altars.yml"), "data-version: 2\nAltars: []\n");
    Storage storage = Fixtures.storage(this.dir);
    storage.altars().load();
    assertTrue(storage.altars().isReadOnly());
    storage.close();
    assertEquals("data-version: 2\nAltars: []\n", Files.readString(this.dir.resolve("altars.yml")));
  }

  /**
   * Lookups by sign, altar block and chunk follow adding and removing, also across a chunk border
   */
  @Test
  void theIndexesFollowChanges() {
    Storage storage = Fixtures.storage(this.dir);
    AltarRepository altars = storage.altars();
    altars.load();
    // The sign in chunk (0, 0), its altar block in chunk (-1, 0)
    Altar border = altar("world", 0, 64, 5, -1, 64, 5, "Zeus");
    Altar inside = altar("world", 3, 64, 5, 4, 64, 5, "Zeus");
    Altar sameBlock = altar("world", 4, 64, 6, 4, 64, 5, "Zeus");
    Altar elsewhere = altar("nether", 3, 64, 5, 4, 64, 5, "Hades");
    altars.add(border);
    altars.add(inside);
    altars.add(sameBlock);
    altars.add(elsewhere);

    BlockPos.ChunkPos chunk = new BlockPos.ChunkPos("world", 0, 0);
    assertEquals(3, altars.inChunk(chunk).size());
    assertEquals(List.of(border), altars.inChunk(new BlockPos.ChunkPos("world", -1, 0)));
    assertEquals(List.of(border, inside, sameBlock), altars.signsInChunk(chunk));
    assertEquals(List.of(), altars.signsInChunk(new BlockPos.ChunkPos("world", -1, 0)));
    assertEquals(List.of(inside, sameBlock), altars.onBlock(new BlockPos("world", 4, 64, 5)));
    assertTrue(altars.isAltarPart(new BlockPos("world", -1, 64, 5)));
    assertTrue(altars.isAltarPart(new BlockPos("world", 0, 64, 5)));
    assertFalse(altars.isAltarPart(new BlockPos("world", 0, 65, 5)));
    assertEquals(List.of(elsewhere), altars.forGod("Hades"));

    assertEquals(border, altars.remove(border.sign()));
    assertNull(altars.remove(border.sign()));
    assertEquals(List.of(), altars.inChunk(new BlockPos.ChunkPos("world", -1, 0)));
    assertFalse(altars.isAltarPart(new BlockPos("world", -1, 64, 5)));

    // The same sign again replaces the altar
    Altar moved = altar("world", 3, 64, 5, 3, 64, 4, "Hera");
    altars.add(moved);
    assertEquals(moved, altars.atSign(inside.sign()));
    assertEquals(List.of(sameBlock), altars.onBlock(new BlockPos("world", 4, 64, 5)));
    assertEquals(3, altars.size());
    storage.close();
  }

  private static Altar altar(
      String world, int x, int y, int z, int bx, int by, int bz, String god) {
    return new Altar(
        new BlockPos(world, x, y, z),
        new BlockPos(world, bx, by, bz),
        AltarKind.PRAYING,
        god,
        Instant.EPOCH,
        null);
  }
}
