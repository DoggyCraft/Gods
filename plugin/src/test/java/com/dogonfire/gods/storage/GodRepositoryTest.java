package com.dogonfire.gods.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dogonfire.gods.model.BlockPos;
import com.dogonfire.gods.model.ContestedLand;
import com.dogonfire.gods.model.God;
import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.model.GodHome;
import com.dogonfire.gods.model.GodRank;
import com.dogonfire.gods.model.GodStats;
import com.dogonfire.gods.model.GodType;
import com.dogonfire.gods.model.NpcSkin;
import com.dogonfire.gods.util.LegacyDates;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GodRepositoryTest {
  private static final UUID STEVE = UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7");
  private static final UUID ALEX = UUID.fromString("0b8d4f7e-3c1a-4bfa-9a3e-6f0f1c2d3e4f");
  private static final UUID NOTCH = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
  private static final UUID JEB = UUID.fromString("853c80ef-3c37-49fd-aa49-938b674adae6");

  /** The players the test server has seen (Herobrine never was) */
  private static final Map<String, UUID> KNOWN = Map.of("notch", NOTCH, "jeb_", JEB);

  @TempDir Path dir;

  private YamlConfiguration written() {
    return YamlConfiguration.loadConfiguration(this.dir.resolve("gods.yml").toFile());
  }

  @Test
  void anOldFileIsUpgradedWithABackup() throws IOException {
    Fixtures.copy("legacy/gods-0.9.yml", this.dir, "gods.yml");
    String original = Files.readString(this.dir.resolve("gods.yml"));

    Storage storage = Fixtures.storage(this.dir, KNOWN);
    assertTrue(storage.gods().load());
    assertEquals(List.of("Thor", "Odin", "Loki"), storage.gods().names());

    God thor = storage.gods().find("Thor");
    // PriestName is added, names become UUIDs, an unknown name is kept aside, "none" is dropped, no
    // duplicates
    assertEquals(List.of(JEB, STEVE, NOTCH), thor.priests());
    assertEquals(STEVE, thor.blessedPlayer());
    assertEquals(LegacyDates.SECONDS.parse("21:15:03 14-03-2019"), thor.blessedTime());
    assertEquals(ALEX, thor.cursedPlayer());
    assertEquals(ALEX, thor.pendingPriest());
    assertEquals(LegacyDates.SECONDS.parse("21:15:03 14-03-2019"), thor.pendingPriestTime());
    assertEquals(GodType.THUNDER, thor.type());
    assertEquals(GodGender.Male, thor.gender());
    assertEquals(LegacyDates.SECONDS.parse("10:00:00 01-01-2019"), thor.created());
    assertEquals(new GodHome("world", 120.5, 64.0, -310.5), thor.home());
    assertEquals(42.5F, thor.mood());
    assertEquals(-4962768465676381896L, thor.seed());
    assertEquals(17, thor.verbosity());
    assertEquals(List.of("Odin"), thor.allies());
    assertEquals(List.of("Loki"), thor.enemies());
    assertEquals(new ContestedLand("world", 130, 70, 140, 4294967297L), thor.contestedLand());
    assertEquals("BREAD", thor.holyFood());
    // Names that no longer exist are kept; the services fall back or pick a new one
    assertEquals("RAW_FISH", thor.unholyFood());
    assertEquals("PIG_ZOMBIE", thor.holyMob());
    assertEquals("ZOMBIE", thor.unholyMob());
    assertEquals(
        List.of("GOLD_INGOT", "POPPY", "IRON_INGOT"), List.copyOf(thor.sacrificeValues().keySet()));
    assertEquals(12.0F, thor.sacrificeValues().get("GOLD_INGOT"));
    assertEquals(-3.5F, thor.sacrificeValues().get("POPPY"));
    assertEquals(4.0F, thor.sacrificeValues().get("IRON_INGOT"));
    assertEquals("The god of thunder", thor.description());
    assertEquals(true, thor.pvp());
    assertNull(thor.mobDamage());
    assertEquals(false, thor.mobSpawning());
    assertEquals(LegacyDates.SECONDS.parse("20:00:00 14-03-2019"), thor.lastQuestTime());
    assertEquals("SLAY", thor.extras().get("QuestType"));
    assertFalse(thor.extras().containsKey("LanguageFileName"));
    assertFalse(thor.extras().containsKey("PriestName"));

    God odin = storage.gods().find("Odin");
    assertTrue(odin.privateAccess());
    assertEquals(-80.0F, odin.mood());
    assertTrue(odin.priests().isEmpty());

    // An unknown type and gender are kept as they are
    God loki = storage.gods().find("Loki");
    assertNull(loki.type());
    assertNull(loki.gender());
    assertEquals("TRICKERY", loki.extras().get("DivineForce"));
    assertEquals(List.of(JEB), loki.priests());

    // Written back at once, upgraded, and the old file kept
    assertTrue(storage.awaitWrites(10, TimeUnit.SECONDS));
    YamlConfiguration written = written();
    assertEquals(GodRepository.DATA_VERSION, written.getInt("data-version"));
    assertEquals(
        List.of(JEB.toString(), STEVE.toString(), NOTCH.toString()),
        written.getStringList("Thor.Priests"));
    assertFalse(written.contains("Thor.PriestName"));
    assertEquals(List.of("Herobrine"), written.getStringList("Thor.UnresolvedPriests"));
    assertFalse(written.contains("Thor.LanguageFileName"));
    assertFalse(written.contains("Odin.LanguageFileName"));
    assertEquals("TRICKERY", written.getString("Loki.DivineForce"));
    assertEquals("Unknown", written.getString("Loki.Gender"));
    assertEquals(3, written.getInt("Thor.ContestedKills"));
    assertEquals(List.of("Dinnerbone"), written.getStringList("Thor.InvitedPlayers"));
    assertFalse(Files.readString(this.dir.resolve("gods.yml")).contains("!!"));
    assertTrue(Files.readString(this.dir.resolve("gods.yml.v1.bak")).contains("PriestName: Notch"));
    assertEquals(original, Files.readString(this.dir.resolve("gods.yml.v1.bak")));
    storage.close();
  }

  @Test
  void theFormatBeforeDataVersionsLoadsAsItIs() throws IOException {
    Fixtures.copy("legacy/gods-1.0-pre.yml", this.dir, "gods.yml");
    Storage storage = Fixtures.storage(this.dir);
    assertTrue(storage.gods().load());
    assertEquals(2, storage.gods().size());

    God thor = storage.gods().find("Thor");
    assertEquals(List.of(STEVE), thor.priests());
    assertEquals(12.25F, thor.mood());
    assertEquals(false, thor.pvp());
    assertEquals(true, thor.mobDamage());
    assertNull(thor.mobSpawning());
    assertEquals(new ContestedLand("world", -5, 64, -5, -1L), thor.contestedLand());

    God loki = storage.gods().find("Loki");
    assertEquals(GodGender.None, loki.gender());
    assertEquals(GodType.EVIL, loki.type());
    assertTrue(loki.priests().isEmpty());
    assertEquals(7, loki.extras().get("SomethingNewer"));
    assertTrue(Files.exists(this.dir.resolve("gods.yml.v1.bak")));
    storage.close();
  }

  @Test
  void savesWhatItReadAndReadsItBackTheSame() throws IOException {
    for (String fixture : List.of("legacy/gods-0.9.yml", "legacy/gods-1.0-pre.yml")) {
      Path folder = Files.createDirectories(this.dir.resolve(fixture.replace('/', '_')));
      Fixtures.copy(fixture, folder, "gods.yml");
      Storage storage = Fixtures.storage(folder, KNOWN);
      storage.gods().load();
      String before = Fixtures.snapshot(storage.gods());
      storage.gods().markDirty();
      storage.close();

      Storage again = Fixtures.storage(folder, KNOWN);
      assertTrue(again.gods().load());
      assertEquals(before, Fixtures.snapshot(again.gods()), fixture);
      // Already upgraded: no second backup
      try (java.util.stream.Stream<java.nio.file.Path> files = Files.list(folder)) {
        assertFalse(
            files.anyMatch(
                file -> file.getFileName().toString().matches("gods\\.yml\\.v1\\..*\\.bak")),
            fixture);
      }
      again.close();
    }
  }

  @Test
  void clearingTheContestedLandDropsWhatOlderVersionsKeptWithIt() throws IOException {
    Fixtures.copy("legacy/gods-0.9.yml", this.dir, "gods.yml");
    Storage storage = Fixtures.storage(this.dir, KNOWN);
    storage.gods().load();
    GodRepository.clearContestedLand(storage.gods().find("Thor"));
    storage.gods().markDirty();
    storage.close();

    YamlConfiguration written = written();
    assertFalse(written.contains("Thor.ContestedLand"));
    assertFalse(written.contains("Thor.ContestedKills"));
  }

  @Test
  void aNewGodIsSavedWithOnlyWhatIsSet() throws IOException {
    Storage storage = Fixtures.storage(this.dir);
    storage.gods().load();
    God god = storage.gods().getOrCreate("Freya");
    god.setCreated(LegacyDates.SECONDS.parse("10:00:00 01-01-2026"));
    god.setType(GodType.LOVE);
    god.setGender(GodGender.Female);
    god.setHome(new GodHome("world", 1.5, 2.0, 3.5));
    god.setBlessed(STEVE, LegacyDates.SECONDS.parse("10:05:00 01-01-2026"));
    storage.gods().markDirty();
    storage.close();

    YamlConfiguration written = written();
    assertEquals(List.of("Freya", "data-version"), List.copyOf(written.getKeys(false)));
    assertEquals(GodRepository.DATA_VERSION, written.getInt("data-version"));
    assertEquals("10:00:00 01-01-2026", written.getString("Freya.Created"));
    assertEquals("LOVE", written.getString("Freya.DivineForce"));
    assertEquals("Female", written.getString("Freya.Gender"));
    assertEquals(1.5, written.getDouble("Freya.Home.X"));
    assertEquals("world", written.getString("Freya.Home.World"));
    // Stored as a string, never as a tagged UUID object
    assertEquals(STEVE.toString(), written.getString("Freya.BlessedPlayer"));
    assertFalse(Files.readString(this.dir.resolve("gods.yml")).contains("!!"));
    assertFalse(written.contains("Freya.Mood"));
    assertFalse(written.contains("Freya.Priests"));
    assertFalse(written.contains("Freya.PvP"));
  }

  /**
   * Non-binary gods are stored as NonBinary and read back, with when the gender last changed; the
   * old genders read as they always did
   */
  @Test
  void gendersRoundTrip() throws IOException {
    Files.writeString(
        this.dir.resolve("gods.yml"),
        """
        data-version: 2
        Loki:
          Gender: NonBinary
          GenderChangedTime: 09:30:00 10-11-2026
        Thor:
          Gender: Male
        Odin:
          Gender: None
        Hel:
          Gender: Unknown
        """);
    Storage storage = Fixtures.storage(this.dir);
    assertTrue(storage.gods().load());
    assertEquals(GodGender.NonBinary, storage.gods().find("Loki").gender());
    assertEquals(
        LegacyDates.SECONDS.parse("09:30:00 10-11-2026"),
        storage.gods().find("Loki").genderChangedTime());
    assertNull(storage.gods().find("Thor").genderChangedTime());
    assertEquals(GodGender.Male, storage.gods().find("Thor").gender());
    assertEquals(GodGender.None, storage.gods().find("Odin").gender());
    assertNull(storage.gods().find("Hel").gender());

    storage.gods().find("Thor").setGender(GodGender.NonBinary);
    storage
        .gods()
        .find("Thor")
        .setGenderChangedTime(LegacyDates.SECONDS.parse("18:00:05 11-11-2026"));
    storage.gods().markDirty();
    storage.close();

    YamlConfiguration written = written();
    assertEquals("NonBinary", written.getString("Loki.Gender"));
    assertEquals("NonBinary", written.getString("Thor.Gender"));
    assertEquals("09:30:00 10-11-2026", written.getString("Loki.GenderChangedTime"));
    assertEquals("18:00:05 11-11-2026", written.getString("Thor.GenderChangedTime"));
    assertFalse(written.contains("Odin.GenderChangedTime"));
    assertEquals("None", written.getString("Odin.Gender"));
    // A gender this version doesn't know is kept as it was
    assertEquals("Unknown", written.getString("Hel.Gender"));
  }

  /**
   * The main altar (/g setaltar) is its sign's block, MainAltar.World/X/Y/Z; one that can't be read
   * is kept as it was
   */
  @Test
  void mainAltarsRoundTrip() throws IOException {
    Files.writeString(
        this.dir.resolve("gods.yml"),
        """
        data-version: 2
        Zeus:
          MainAltar:
            World: world.of.gods
            X: 10
            Y: 64
            Z: -3
        Hera:
          MainAltar: somewhere
        """);
    Storage storage = Fixtures.storage(this.dir);
    assertTrue(storage.gods().load());
    assertEquals(
        new BlockPos("world.of.gods", 10, 64, -3), storage.gods().find("Zeus").mainAltar());
    assertNull(storage.gods().find("Hera").mainAltar());

    God thor = storage.gods().getOrCreate("Thor");
    thor.setMainAltar(new BlockPos("world", -1, 70, 2));
    storage.gods().markDirty();
    storage.close();

    YamlConfiguration written = written();
    assertEquals("world.of.gods", written.getString("Zeus.MainAltar.World"));
    assertEquals(-3, written.getInt("Zeus.MainAltar.Z"));
    assertEquals("somewhere", written.getString("Hera.MainAltar"));
    assertEquals(70, written.getInt("Thor.MainAltar.Y"));

    Storage again = Fixtures.storage(this.dir);
    assertTrue(again.gods().load());
    assertEquals(new BlockPos("world", -1, 70, 2), again.gods().find("Thor").mainAltar());
    again.close();
  }

  /** A god's descent: PriestSince, DescendedTime and AscendingSince, written only while set */
  @Test
  void descentTimesRoundTrip() throws IOException {
    Files.writeString(
        this.dir.resolve("gods.yml"),
        """
        data-version: 2
        Zeus:
          PriestSince: 08:00:00 09-11-2026
          DescendedTime: 09:30:00 10-11-2026
          AscendingSince: 10:00:00 11-11-2026
        """);
    Storage storage = Fixtures.storage(this.dir);
    assertTrue(storage.gods().load());
    God zeus = storage.gods().find("Zeus");
    assertEquals(LegacyDates.SECONDS.parse("08:00:00 09-11-2026"), zeus.priestSince());
    assertEquals(LegacyDates.SECONDS.parse("09:30:00 10-11-2026"), zeus.descended());
    assertEquals(LegacyDates.SECONDS.parse("10:00:00 11-11-2026"), zeus.ascendingSince());
    zeus.setAscendingSince(null);
    storage.gods().markDirty();
    storage.close();

    YamlConfiguration written = written();
    assertEquals("09:30:00 10-11-2026", written.getString("Zeus.DescendedTime"));
    assertEquals("08:00:00 09-11-2026", written.getString("Zeus.PriestSince"));
    assertFalse(written.contains("Zeus.AscendingSince"));
  }

  /**
   * The NPC skin (/g godskin) is NpcSkin.Name/Value/Signature; one that isn't a section is kept as
   * it was
   */
  @Test
  void npcSkinsRoundTrip() throws IOException {
    Files.writeString(
        this.dir.resolve("gods.yml"),
        """
        data-version: 2
        Zeus:
          NpcSkin:
            Name: Notch
            Value: abc
            Signature: def
        Hera:
          NpcSkin: Notch
        Odin:
          NpcSkin:
            Name: Jeb_
        """);
    Storage storage = Fixtures.storage(this.dir);
    assertTrue(storage.gods().load());
    assertEquals(new NpcSkin("Notch", "abc", "def"), storage.gods().find("Zeus").npcSkin());
    assertNull(storage.gods().find("Hera").npcSkin());
    assertEquals(NpcSkin.named("Jeb_"), storage.gods().find("Odin").npcSkin());

    storage.gods().getOrCreate("Thor").setNpcSkin(new NpcSkin(null, "xyz", null));
    storage.gods().find("Odin").setNpcSkin(null);
    storage.gods().markDirty();
    storage.close();

    YamlConfiguration written = written();
    assertEquals("def", written.getString("Zeus.NpcSkin.Signature"));
    assertEquals("Notch", written.getString("Hera.NpcSkin"));
    assertEquals("xyz", written.getString("Thor.NpcSkin.Value"));
    assertFalse(written.contains("Thor.NpcSkin.Signature"));
    assertFalse(written.contains("Odin.NpcSkin"));

    Storage again = Fixtures.storage(this.dir);
    assertTrue(again.gods().load());
    assertEquals(new NpcSkin("Notch", "abc", "def"), again.gods().find("Zeus").npcSkin());
    assertEquals(new NpcSkin(null, "xyz", null), again.gods().find("Thor").npcSkin());
    again.close();
  }

  /**
   * A god's rank and its religion's lifetime stats: Rank, RankSince, Stats.*, StatsSeeded; odd
   * values are kept
   */
  @Test
  void ranksAndStatsRoundTrip() throws IOException {
    Files.writeString(
        this.dir.resolve("gods.yml"),
        """
        data-version: 2
        Zeus:
          Rank: EXALTED
          RankSince: 08:00:00 09-11-2026
          Stats:
            Prayers: 120
            Sacrifices: 31
            Quests: 2
            Converts: 4
            Miracles: 3
          StatsSeeded: true
        Hera:
          Rank: DEMIGOD
          Stats: lots
        Odin:
          Created: 10:00:00 01-01-2019
        """);
    Storage storage = Fixtures.storage(this.dir);
    assertTrue(storage.gods().load());
    God zeus = storage.gods().find("Zeus");
    assertEquals(GodRank.EXALTED, zeus.rank());
    assertEquals(LegacyDates.SECONDS.parse("08:00:00 09-11-2026"), zeus.rankSince());
    assertEquals(new GodStats(120, 31, 2, 4), zeus.stats());
    assertTrue(zeus.statsSeeded());
    God hera = storage.gods().find("Hera");
    assertNull(hera.rank());
    assertEquals(GodStats.ZERO, hera.stats());
    assertFalse(hera.statsSeeded());
    // A god from before ranks has none of it
    God odin = storage.gods().find("Odin");
    assertNull(odin.rank());
    assertNull(odin.rankSince());
    assertEquals(GodStats.ZERO, odin.stats());
    assertFalse(odin.statsSeeded());

    zeus.setStats(zeus.stats().withPrayers(121));
    odin.setRank(GodRank.RECOGNIZED);
    odin.setStatsSeeded(true);
    storage.gods().markDirty();
    storage.close();

    YamlConfiguration written = written();
    assertEquals("EXALTED", written.getString("Zeus.Rank"));
    assertEquals("08:00:00 09-11-2026", written.getString("Zeus.RankSince"));
    assertEquals(121, written.getInt("Zeus.Stats.Prayers"));
    assertEquals(31, written.getInt("Zeus.Stats.Sacrifices"));
    assertEquals(3, written.getInt("Zeus.Stats.Miracles"));
    assertTrue(written.getBoolean("Zeus.StatsSeeded"));
    assertEquals("DEMIGOD", written.getString("Hera.Rank"));
    assertEquals("lots", written.getString("Hera.Stats"));
    assertFalse(written.contains("Hera.StatsSeeded"));
    assertEquals("RECOGNIZED", written.getString("Odin.Rank"));
    assertEquals(0, written.getInt("Odin.Stats.Prayers", -1));
    assertTrue(written.getBoolean("Odin.StatsSeeded"));

    Storage again = Fixtures.storage(this.dir);
    assertTrue(again.gods().load());
    assertEquals(new GodStats(121, 31, 2, 4), again.gods().find("Zeus").stats());
    assertEquals(GodRank.RECOGNIZED, again.gods().find("Odin").rank());
    again.close();
  }
}
