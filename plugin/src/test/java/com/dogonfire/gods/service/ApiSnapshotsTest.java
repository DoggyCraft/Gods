package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dogonfire.gods.api.BelieverInfo;
import com.dogonfire.gods.api.GodInfo;
import com.dogonfire.gods.api.QuestInfo;
import com.dogonfire.gods.model.Altar;
import com.dogonfire.gods.model.AltarKind;
import com.dogonfire.gods.model.Believer;
import com.dogonfire.gods.model.BlockPos;
import com.dogonfire.gods.model.God;
import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.model.GodHome;
import com.dogonfire.gods.model.GodMood;
import com.dogonfire.gods.model.GodRank;
import com.dogonfire.gods.model.GodType;
import com.dogonfire.gods.model.Quest;
import com.dogonfire.gods.model.QuestType;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

class ApiSnapshotsTest {
  private static final UUID PRIEST = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID BELIEVER = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final Instant CREATED = Instant.parse("2026-01-02T03:04:05Z");

  // A world is only needed for its presence in a Location
  private static final World WORLD =
      (World)
          Proxy.newProxyInstance(
              World.class.getClassLoader(),
              new Class<?>[] {World.class},
              (proxy, method, args) ->
                  switch (method.getName()) {
                    case "getName" -> "world";
                    case "hashCode" -> 1;
                    case "equals" -> proxy == args[0];
                    default -> null;
                  });

  @Test
  void everyGodTypeHasTheSameNameInTheApi() {
    assertEquals(names(GodType.values()), names(com.dogonfire.gods.api.GodType.values()));
    for (GodType type : GodType.values()) {
      assertEquals(type.name(), ApiSnapshots.godType(type).name());
    }
    assertNull(ApiSnapshots.godType(null));
  }

  @Test
  void gendersAndMoodsMapToTheApi() {
    assertEquals(com.dogonfire.gods.api.GodGender.MALE, ApiSnapshots.gender(GodGender.Male));
    assertEquals(com.dogonfire.gods.api.GodGender.FEMALE, ApiSnapshots.gender(GodGender.Female));
    assertEquals(com.dogonfire.gods.api.GodGender.NONE, ApiSnapshots.gender(GodGender.None));
    assertEquals(
        com.dogonfire.gods.api.GodGender.NON_BINARY, ApiSnapshots.gender(GodGender.NonBinary));
    assertEquals(com.dogonfire.gods.api.GodGender.NONE, ApiSnapshots.gender((GodGender) null));
    assertEquals(
        names(GodGender.values()).size(), com.dogonfire.gods.api.GodGender.values().length);
    for (GodGender gender : GodGender.values()) {
      assertEquals(gender, ApiSnapshots.modelGender(ApiSnapshots.gender(gender)));
    }

    assertEquals(names(GodMood.values()), names(com.dogonfire.gods.api.GodMood.values()));
    assertEquals(com.dogonfire.gods.api.GodMood.ANGRY, ApiSnapshots.mood(-71.0F));
    assertEquals(com.dogonfire.gods.api.GodMood.NEUTRAL, ApiSnapshots.mood(0.0F));
    assertEquals(com.dogonfire.gods.api.GodMood.EXALTED, ApiSnapshots.mood(100.0F));

    assertEquals(names(GodRank.values()), names(com.dogonfire.gods.api.GodRank.values()));
    for (GodRank rank : GodRank.values()) {
      assertEquals(rank, ApiSnapshots.modelRank(ApiSnapshots.rank(rank)));
    }
    assertEquals(com.dogonfire.gods.api.GodRank.SPIRIT, ApiSnapshots.rank(null));

    assertEquals(
        names(com.dogonfire.gods.model.DevotionLevel.values()),
        names(com.dogonfire.gods.api.DevotionLevel.values()));
    for (com.dogonfire.gods.model.DevotionLevel level :
        com.dogonfire.gods.model.DevotionLevel.values()) {
      assertEquals(level, ApiSnapshots.modelDevotion(ApiSnapshots.devotion(level)));
    }
    assertEquals(com.dogonfire.gods.api.DevotionLevel.FOLLOWER, ApiSnapshots.devotion(null));
  }

  @Test
  void onlyTheQuestsGodsMakesHaveTheirOwnApiType() {
    assertEquals(
        com.dogonfire.gods.api.QuestType.PILGRIMAGE, ApiSnapshots.questType(QuestType.PILGRIMAGE));
    assertEquals(com.dogonfire.gods.api.QuestType.SLAY, ApiSnapshots.questType(QuestType.SLAY));
    assertEquals(
        com.dogonfire.gods.api.QuestType.ITEM_SACRIFICE,
        ApiSnapshots.questType(QuestType.ITEMSACRIFICE));
    assertEquals(
        com.dogonfire.gods.api.QuestType.GIVE_ITEMS, ApiSnapshots.questType(QuestType.GIVEITEMS));
    assertEquals(com.dogonfire.gods.api.QuestType.OTHER, ApiSnapshots.questType(QuestType.CONVERT));
    assertEquals(com.dogonfire.gods.api.QuestType.OTHER, ApiSnapshots.questType(QuestType.NONE));
  }

  @Test
  void aGodSnapshotHoldsTheModelAndWhatTheServerKnows() {
    God god = new God("Zeus");
    god.setCreated(CREATED);
    god.setType(GodType.THUNDER);
    god.setGender(GodGender.Male);
    god.setMood(42.5F);
    god.setDescription("Lord of the sky");
    god.priests().add(PRIEST);
    god.allies().add("Hera");
    god.enemies().add("Hades");
    god.setDescended(CREATED.plusSeconds(3600));
    god.setRank(GodRank.EXALTED);
    QuestInfo quest =
        new QuestInfo(
            "Zeus", com.dogonfire.gods.api.QuestType.SLAY, "ZOMBIE", 10, 3, null, null, null);
    Location home = new Location(WORLD, 1, 64, 2);

    Location mainAltar = new Location(WORLD, 10, 64, -4);

    GodInfo info =
        ApiSnapshots.god(god, 12.5F, Set.of(PRIEST, BELIEVER), home, 4, quest, mainAltar);

    assertEquals("Zeus", info.name());
    assertEquals(com.dogonfire.gods.api.GodType.THUNDER, info.type());
    assertEquals(com.dogonfire.gods.api.GodGender.MALE, info.gender());
    assertEquals(com.dogonfire.gods.api.GodMood.PLEASED, info.mood());
    assertEquals(42.5F, info.moodValue());
    assertEquals(12.5F, info.power());
    assertEquals(Set.of(PRIEST, BELIEVER), info.believers());
    assertEquals(List.of(PRIEST), info.priests());
    assertTrue(info.isPriest(PRIEST));
    assertFalse(info.isPriest(BELIEVER));
    assertTrue(info.isBeliever(BELIEVER));
    assertEquals(List.of("Hera"), info.allies());
    assertEquals(List.of("Hades"), info.enemies());
    assertEquals("Lord of the sky", info.description());
    assertEquals(home, info.home());
    assertEquals(4, info.holyLandClaims());
    assertEquals(quest, info.quest());
    assertEquals(CREATED, info.created());
    assertEquals(mainAltar, info.mainAltar());
    assertNotSame(info.mainAltar(), info.mainAltar());
    assertEquals(CREATED.plusSeconds(3600), info.descended());
    assertTrue(info.isDescended());
    assertEquals(com.dogonfire.gods.api.GodRank.EXALTED, info.rank());
    // An unranked god is a spirit
    assertEquals(
        com.dogonfire.gods.api.GodRank.SPIRIT,
        ApiSnapshots.god(new God("Hera"), 0F, Set.of(), null, 0, null, null).rank());
  }

  @Test
  void aGodSnapshotDoesNotChangeWithTheGod() {
    God god = new God("Zeus");
    god.priests().add(PRIEST);
    Location home = new Location(WORLD, 1, 64, 2);
    GodInfo info = ApiSnapshots.god(god, 0.0F, Set.of(), home, 0, null, null);

    god.priests().clear();
    god.allies().add("Hera");
    home.setX(100);
    info.home().setY(0);

    assertEquals(List.of(PRIEST), info.priests());
    assertEquals(List.of(), info.allies());
    assertEquals(new Location(WORLD, 1, 64, 2), info.home());
    assertNotSame(info.home(), info.home());
    assertThrows(UnsupportedOperationException.class, () -> info.priests().add(BELIEVER));
  }

  @Test
  void aGodWithoutTypeGenderOrLoadedHomeStillHasASnapshot() {
    GodInfo info =
        ApiSnapshots.god(
            new God("Nobody"),
            0.0F,
            Set.of(),
            new Location(null, 1, 2, 3),
            0,
            null,
            new Location(null, 4, 5, 6));

    assertNull(info.type());
    assertEquals(com.dogonfire.gods.api.GodGender.NONE, info.gender());
    assertEquals(com.dogonfire.gods.api.GodMood.NEUTRAL, info.mood());
    assertNull(info.home());
    assertNull(info.mainAltar());
    assertNull(info.description());
    assertNull(info.created());
    assertNull(info.descended());
    assertFalse(info.isDescended());
  }

  @Test
  void aQuestSnapshotKnowsWhenItExpires() {
    Quest quest = new Quest("Zeus");
    quest.setType(QuestType.PILGRIMAGE);
    quest.setTargetType("Mount Olympus");
    quest.setAmount(1);
    quest.setMaxDuration(90);
    quest.setCreatedTime(CREATED);
    Location chest = new Location(WORLD, 10, 70, -5);

    QuestInfo info = ApiSnapshots.quest(quest, chest);

    assertEquals("Zeus", info.god());
    assertEquals(com.dogonfire.gods.api.QuestType.PILGRIMAGE, info.type());
    assertEquals("Mount Olympus", info.target());
    assertEquals(1, info.amount());
    assertEquals(0, info.progress());
    assertEquals(CREATED, info.started());
    assertEquals(CREATED.plusSeconds(90 * 60), info.expires());
    assertEquals(chest, info.location());
  }

  @Test
  void aQuestWithMissingValuesOrNoTypeReadsAsGodsDoes() {
    Quest quest = new Quest("Zeus");
    quest.setType(QuestType.SLAY);
    QuestInfo info = ApiSnapshots.quest(quest, new Location(null, 0, 0, 0));
    assertEquals(0, info.amount());
    assertNull(info.started());
    assertNull(info.expires());
    assertNull(info.location());

    assertNull(ApiSnapshots.quest(new Quest("Hera"), null));
    assertNull(ApiSnapshots.quest(null, null));
  }

  @Test
  void onlyAPlayerWithAGodIsABeliever() {
    Believer believer = new Believer(BELIEVER);
    assertNull(ApiSnapshots.believer(believer, false));
    assertNull(ApiSnapshots.believer(null, false));

    believer.setGodName("Zeus");
    believer.setPrayers(7);
    believer.setPrayerPower(3);
    believer.setJoined(CREATED);
    believer.setLastPrayer(CREATED.plusSeconds(60));

    BelieverInfo info = ApiSnapshots.believer(believer, true);
    assertEquals(
        new BelieverInfo(BELIEVER, "Zeus", 7, 3, true, CREATED, CREATED.plusSeconds(60)), info);
    assertEquals(com.dogonfire.gods.api.DevotionLevel.FOLLOWER, info.devotion());

    believer.setDevotion(640);
    believer.setDevotionLevel(com.dogonfire.gods.model.DevotionLevel.ZEALOT);
    assertEquals(
        new BelieverInfo(
            BELIEVER,
            "Zeus",
            7,
            3,
            true,
            CREATED,
            CREATED.plusSeconds(60),
            com.dogonfire.gods.api.DevotionLevel.ZEALOT,
            640),
        ApiSnapshots.believer(believer, true));
  }

  /**
   * GodInfo.home: in front of the main altar, facing it; without a main altar the home from before
   * main altars
   */
  @Test
  void theHomeIsTheMainAltar() {
    // The sign hangs south of its altar block
    Altar main =
        new Altar(
            new BlockPos("world", 10, 64, 11),
            new BlockPos("world", 10, 64, 10),
            AltarKind.PRAYING,
            "Zeus",
            null,
            null);
    GodHome legacy = new GodHome("world", 100.5, 70, -3.25);

    Location home = ApiSnapshots.home(HomeRules.home(main, legacy), name -> WORLD);
    assertEquals(new Location(WORLD, 10.5, 64, 12.5, GodNpcRules.facingYaw(main), 0.0F), home);
    // Looking north, at the altar
    assertEquals(180.0F, Math.abs(home.getYaw()), 0.001F);

    Location old = ApiSnapshots.home(HomeRules.home(null, legacy), name -> WORLD);
    assertEquals(new Location(WORLD, 100.5, 70, -3.25), old);

    assertNull(ApiSnapshots.home(HomeRules.home(null, null), name -> WORLD));
    // Its world isn't loaded
    assertNull(ApiSnapshots.home(HomeRules.home(main, legacy), name -> null));
  }

  private static List<String> names(Enum<?>[] values) {
    return Arrays.stream(values).map(Enum::name).toList();
  }

  @Test
  void rankProgressShowsEachCondition() {
    com.dogonfire.gods.api.RankProgress progress =
        ApiSnapshots.rankProgress(
            GodRank.DESCENDED,
            GodRank.EXALTED,
            List.of(
                new RankRules.Progress(RankRules.Condition.BELIEVERS, 7, 10, false),
                new RankRules.Progress(RankRules.Condition.MOOD, 3, 3, true)));
    assertEquals(com.dogonfire.gods.api.GodRank.DESCENDED, progress.rank());
    assertEquals(com.dogonfire.gods.api.GodRank.EXALTED, progress.next());
    assertEquals(
        List.of(
            new com.dogonfire.gods.api.RankProgress.Condition("min-believers", 7, 10, false),
            new com.dogonfire.gods.api.RankProgress.Condition("min-mood", 3, 3, true)),
        progress.conditions());
    assertFalse(progress.isComplete());
    assertTrue(ApiSnapshots.rankProgress(GodRank.GREATER, null, List.of()).isComplete());
  }
}
