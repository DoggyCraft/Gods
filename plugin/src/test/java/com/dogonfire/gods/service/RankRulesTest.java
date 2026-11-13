package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.config.TestConfigs;
import com.dogonfire.gods.model.GodMood;
import com.dogonfire.gods.model.GodRank;
import com.dogonfire.gods.model.GodStats;
import com.dogonfire.gods.service.RankRules.Change;
import com.dogonfire.gods.service.RankRules.Condition;
import com.dogonfire.gods.service.RankRules.Facts;
import com.dogonfire.gods.service.RankRules.Features;
import com.dogonfire.gods.service.RankRules.Outcome;
import com.dogonfire.gods.service.RankRules.Progress;
import com.dogonfire.gods.service.RankRules.Reason;
import com.dogonfire.gods.service.RankRules.State;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RankRulesTest {
  /** config.yml's defaults */
  private static final Settings.Ranks RANKS = TestConfigs.defaultRanks();

  private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");

  /** Everything the greater rank asks for, and more */
  private static final GodStats LOTS = new GodStats(1000, 1000, 1000, 1000);

  private static Instant hoursAgo(long hours) {
    return NOW.minus(Duration.ofHours(hours));
  }

  /** A god that has held its rank for a long time, with a priest for as long */
  private static State at(GodRank rank) {
    return new State(rank, hoursAgo(1000), hoursAgo(1000), null);
  }

  /** A great religion: it meets every condition of every rank */
  private static Facts great() {
    return new Facts(true, 30, 200F, GodMood.EXALTED, true, 5, 6, LOTS, NOW);
  }

  private static Facts with(
      Facts facts, int believers, GodMood mood, boolean priest, GodStats stats) {
    return new Facts(
        priest,
        believers,
        facts.power(),
        mood,
        facts.hasMainAltar(),
        facts.altars(),
        facts.holyLands(),
        stats,
        facts.now());
  }

  private static Outcome evaluate(State state, Facts facts) {
    return RankRules.evaluate(RANKS, state, facts, Features.ALL);
  }

  @Test
  void itRisesOneRankAtATime() {
    Outcome outcome = evaluate(at(GodRank.SPIRIT), great());
    assertEquals(Change.UP, outcome.change());
    assertEquals(Reason.RANK_UP, outcome.reason());
    assertEquals(GodRank.RECOGNIZED, outcome.state().rank());
    assertEquals(NOW, outcome.state().rankSince());

    assertEquals(GodRank.DESCENDED, evaluate(at(GodRank.RECOGNIZED), great()).state().rank());
    assertEquals(GodRank.EXALTED, evaluate(at(GodRank.DESCENDED), great()).state().rank());
    assertEquals(GodRank.GREATER, evaluate(at(GodRank.EXALTED), great()).state().rank());
    // The highest rank has nowhere to go
    Outcome top = evaluate(at(GodRank.GREATER), great());
    assertEquals(Change.NONE, top.change());
    assertEquals(GodRank.GREATER, top.state().rank());
    assertEquals(hoursAgo(1000), top.state().rankSince());
  }

  @Test
  void anUnrankedGodIsASpirit() {
    assertEquals(
        GodRank.RECOGNIZED, evaluate(new State(null, null, null, null), great()).state().rank());
  }

  /** Every condition of the next rank must hold */
  @Test
  void itWaitsForEachCondition() {
    Facts great = great();
    // Recognized: a priest and three believers
    assertEquals(
        Change.NONE,
        evaluate(at(GodRank.SPIRIT), with(great, 30, GodMood.EXALTED, false, LOTS)).change());
    assertEquals(
        Change.NONE,
        evaluate(at(GodRank.SPIRIT), with(great, 2, GodMood.EXALTED, true, LOTS)).change());
    // Descended: a priest for 24 hours, a main altar, at least a neutral mood
    assertEquals(
        Change.NONE,
        evaluate(new State(GodRank.RECOGNIZED, hoursAgo(100), hoursAgo(23), null), great).change());
    assertEquals(
        Change.UP,
        evaluate(new State(GodRank.RECOGNIZED, hoursAgo(100), hoursAgo(24), null), great).change());
    assertEquals(
        Change.NONE,
        evaluate(
                at(GodRank.RECOGNIZED),
                new Facts(true, 30, 200F, GodMood.EXALTED, false, 5, 6, LOTS, NOW))
            .change());
    assertEquals(
        Change.NONE,
        evaluate(at(GodRank.RECOGNIZED), with(great, 30, GodMood.DISPLEASED, true, LOTS)).change());
    assertEquals(
        Change.UP,
        evaluate(at(GodRank.RECOGNIZED), with(great, 30, GodMood.NEUTRAL, true, LOTS)).change());
    // Exalted: 72 hours as descended, and each counter
    assertEquals(
        Change.NONE,
        evaluate(new State(GodRank.DESCENDED, hoursAgo(71), hoursAgo(1000), null), great).change());
    assertEquals(
        Change.NONE,
        evaluate(
                at(GodRank.DESCENDED),
                with(great, 30, GodMood.EXALTED, true, LOTS.withSacrifices(19)))
            .change());
    assertEquals(
        Change.NONE,
        evaluate(
                at(GodRank.DESCENDED),
                new Facts(true, 30, 49.9F, GodMood.EXALTED, true, 5, 6, LOTS, NOW))
            .change());
    assertEquals(
        Change.NONE,
        evaluate(
                at(GodRank.DESCENDED),
                new Facts(true, 30, 200F, GodMood.EXALTED, true, 1, 6, LOTS, NOW))
            .change());
    // Greater: holy land regions
    assertEquals(
        Change.NONE,
        evaluate(
                at(GodRank.EXALTED),
                new Facts(true, 30, 200F, GodMood.EXALTED, true, 5, 3, LOTS, NOW))
            .change());
  }

  /** A condition left out doesn't count; one of a feature that is off neither */
  @Test
  void conditionsAreOptional() {
    Settings.Ranks none = ranks(Settings.RankConditions.NONE);
    Facts nobody = new Facts(false, 0, 0F, GodMood.ANGRY, false, 0, 0, GodStats.ZERO, NOW);
    assertEquals(
        GodRank.RECOGNIZED,
        RankRules.evaluate(none, at(GodRank.SPIRIT), nobody, Features.ALL).state().rank());
    assertTrue(
        RankRules.progress(none, GodRank.GREATER, at(GodRank.EXALTED), nobody, Features.ALL)
            .isEmpty());

    // Without quests, sacrifices and holy land their conditions don't count
    Facts noActivity =
        new Facts(true, 30, 200F, GodMood.EXALTED, true, 5, 0, new GodStats(1000, 0, 0, 1000), NOW);
    assertEquals(Change.NONE, evaluate(at(GodRank.EXALTED), noActivity).change());
    assertEquals(
        Change.UP,
        RankRules.evaluate(
                RANKS, at(GodRank.EXALTED), noActivity, new Features(false, false, false))
            .change());
  }

  /** An angry god, or one whose religion stopped doing things, stalls but never falls */
  @Test
  void moodAndActivityNeverMakeItFall() {
    Outcome angry =
        evaluate(at(GodRank.GREATER), with(great(), 30, GodMood.ANGRY, true, GodStats.ZERO));
    assertEquals(Change.NONE, angry.change());
    assertEquals(GodRank.GREATER, angry.state().rank());
    // Nor losing its main altar, altars or power
    assertEquals(
        Change.NONE,
        evaluate(
                at(GodRank.GREATER),
                new Facts(true, 30, 0F, GodMood.ANGRY, false, 0, 0, GodStats.ZERO, NOW))
            .change());
    // An angry descended god doesn't rise
    assertEquals(
        Change.NONE,
        evaluate(at(GodRank.DESCENDED), with(great(), 30, GodMood.ANGRY, true, LOTS)).change());
  }

  @Test
  void withoutAPriestItFallsAtOnce() {
    Outcome outcome =
        evaluate(at(GodRank.GREATER), with(great(), 30, GodMood.EXALTED, false, LOTS));
    assertEquals(Change.DOWN, outcome.change());
    assertEquals(Reason.NO_PRIEST, outcome.reason());
    // Below every rank that needs one: recognized needs one, so it is a spirit again
    assertEquals(GodRank.SPIRIT, outcome.state().rank());
    assertNull(outcome.state().priestSince());
    // A spirit has nothing to lose
    assertEquals(
        Change.NONE,
        evaluate(at(GodRank.SPIRIT), with(great(), 30, GodMood.EXALTED, false, LOTS)).change());

    // Where only descended needs a priest, it falls to recognized
    Map<GodRank, Settings.RankConditions> conditions = new EnumMap<>(RANKS.conditions());
    conditions.put(GodRank.RECOGNIZED, Settings.RankConditions.NONE);
    Settings.Ranks ranks = new Settings.Ranks(conditions, 24, RANKS.unlocks(), RANKS.aura());
    assertEquals(
        GodRank.RECOGNIZED,
        RankRules.evaluate(
                ranks,
                at(GodRank.EXALTED),
                with(great(), 30, GodMood.EXALTED, false, LOTS),
                Features.ALL)
            .state()
            .rank());
  }

  @Test
  void withTooFewBelieversItFallsAfterTheGracePeriod() {
    // Greater needs 20, exalted 10, descended and recognized 3
    Facts nine = with(great(), 9, GodMood.EXALTED, true, LOTS);
    Outcome first = evaluate(at(GodRank.GREATER), nine);
    assertEquals(Change.NONE, first.change());
    assertEquals(NOW, first.state().fallingSince());
    // The clock keeps running
    assertEquals(
        hoursAgo(5),
        evaluate(new State(GodRank.GREATER, hoursAgo(1000), hoursAgo(1000), hoursAgo(5)), nine)
            .state()
            .fallingSince());
    // After 24 hours: to the highest rank it has the believers for
    Outcome fallen =
        evaluate(new State(GodRank.GREATER, hoursAgo(1000), hoursAgo(1000), hoursAgo(24)), nine);
    assertEquals(Change.DOWN, fallen.change());
    assertEquals(Reason.TOO_FEW_BELIEVERS, fallen.reason());
    assertEquals(GodRank.DESCENDED, fallen.state().rank());
    assertNull(fallen.state().fallingSince());
    assertEquals(NOW, fallen.state().rankSince());
    // Believers coming back stop the clock
    assertNull(
        evaluate(new State(GodRank.GREATER, hoursAgo(1000), hoursAgo(1000), hoursAgo(20)), great())
            .state()
            .fallingSince());
    // With none left it is a spirit
    assertEquals(
        GodRank.SPIRIT,
        evaluate(
                new State(GodRank.DESCENDED, hoursAgo(1000), hoursAgo(1000), hoursAgo(30)),
                with(great(), 0, GodMood.EXALTED, true, LOTS))
            .state()
            .rank());
  }

  @Test
  void aFirstPriestStartsTheClock() {
    Facts great = great();
    assertEquals(
        NOW,
        evaluate(new State(GodRank.RECOGNIZED, hoursAgo(100), null, null), great)
            .state()
            .priestSince());
    assertEquals(
        hoursAgo(3),
        evaluate(new State(GodRank.RECOGNIZED, hoursAgo(100), hoursAgo(3), null), great)
            .state()
            .priestSince());
  }

  @Test
  void progressShowsEveryConditionOfTheRank() {
    Facts facts =
        new Facts(true, 7, 31.6F, GodMood.NEUTRAL, true, 1, 2, new GodStats(40, 31, 0, 1), NOW);
    List<Progress> progress =
        RankRules.progress(
            RANKS,
            GodRank.EXALTED,
            new State(GodRank.DESCENDED, hoursAgo(10), hoursAgo(100), null),
            facts,
            Features.ALL);
    assertEquals(
        List.of(
            new Progress(Condition.BELIEVERS, 7, 10, false),
            new Progress(Condition.POWER, 31, 50, false),
            // Recognized's priest carries over
            new Progress(Condition.PRIEST, 1, 1, true),
            new Progress(Condition.MOOD, 2, 3, false),
            new Progress(Condition.HOURS_AT_PREVIOUS_RANK, 10, 72, false),
            new Progress(Condition.PRAYERS, 40, 150, false),
            new Progress(Condition.SACRIFICES, 31, 20, true),
            new Progress(Condition.QUESTS, 0, 2, false),
            new Progress(Condition.ALTARS, 1, 2, false),
            new Progress(Condition.CONVERTS, 1, 2, false)),
        progress);
    assertTrue(
        RankRules.progress(RANKS, GodRank.SPIRIT, at(GodRank.SPIRIT), facts, Features.ALL)
            .isEmpty());
    assertEquals(4, RankRules.moodLevel(GodMood.EXALTED));
    assertEquals(0, RankRules.moodLevel(GodMood.ANGRY));
  }

  /**
   * A god from before ranks lands on the highest rank its believers, priest, mood and so on allow
   */
  @Test
  void seedingPlacesAGodFromBeforeRanks() {
    Facts exalted = new Facts(true, 12, 60F, GodMood.PLEASED, true, 2, 0, GodStats.ZERO, NOW);
    RankRules.Seed seed =
        RankRules.seed(RANKS, exalted, Features.ALL, hoursAgo(24 * 30), GodRank.SPIRIT);
    assertEquals(GodRank.EXALTED, seed.rank());
    // Its counters are exactly what exalted (and the ranks below) ask for
    assertEquals(new GodStats(150, 20, 2, 2), seed.stats());
    // ... so it stays there
    assertEquals(
        Change.NONE,
        evaluate(
                new State(seed.rank(), NOW, NOW, null),
                with(exalted, 12, GodMood.PLEASED, true, seed.stats()))
            .change());
    assertTrue(
        RankRules.qualifies(
            RANKS,
            GodRank.EXALTED,
            new State(GodRank.DESCENDED, null, hoursAgo(100), null),
            with(exalted, 12, GodMood.PLEASED, true, seed.stats()),
            Features.ALL));

    // Counted already: kept
    Facts counted =
        new Facts(true, 12, 60F, GodMood.PLEASED, true, 2, 0, new GodStats(400, 0, 0, 0), NOW);
    assertEquals(
        new GodStats(400, 20, 2, 2),
        RankRules.seed(RANKS, counted, Features.ALL, hoursAgo(24 * 30), GodRank.SPIRIT).stats());

    // Too young for exalted's 72 hours: descended (24 hours with a priest are counted from its
    // creation)
    assertEquals(
        GodRank.DESCENDED,
        RankRules.seed(RANKS, exalted, Features.ALL, hoursAgo(30), GodRank.SPIRIT).rank());
    // A day old god with a priest is recognized
    assertEquals(
        GodRank.RECOGNIZED,
        RankRules.seed(RANKS, exalted, Features.ALL, hoursAgo(5), GodRank.SPIRIT).rank());
    // Unknown creation: old
    assertEquals(
        GodRank.EXALTED, RankRules.seed(RANKS, exalted, Features.ALL, null, GodRank.SPIRIT).rank());
    // An angry god stays below descended
    assertEquals(
        GodRank.RECOGNIZED,
        RankRules.seed(
                RANKS,
                with(exalted, 12, GodMood.ANGRY, true, GodStats.ZERO),
                Features.ALL,
                null,
                GodRank.SPIRIT)
            .rank());
    // No priest: a spirit, with nothing seeded
    RankRules.Seed spirit =
        RankRules.seed(
            RANKS,
            with(exalted, 12, GodMood.PLEASED, false, GodStats.ZERO),
            Features.ALL,
            null,
            GodRank.SPIRIT);
    assertEquals(GodRank.SPIRIT, spirit.rank());
    assertEquals(GodStats.ZERO, spirit.stats());
    // A god that had descended stays descended
    RankRules.Seed descended =
        RankRules.seed(
            RANKS,
            with(exalted, 2, GodMood.ANGRY, false, GodStats.ZERO),
            Features.ALL,
            null,
            GodRank.DESCENDED);
    assertEquals(GodRank.DESCENDED, descended.rank());
    // Greater needs holy land, which is off: it doesn't count
    Facts greater = new Facts(true, 25, 160F, GodMood.EXALTED, true, 4, 0, GodStats.ZERO, NOW);
    assertEquals(
        GodRank.EXALTED, RankRules.seed(RANKS, greater, Features.ALL, null, GodRank.SPIRIT).rank());
    assertEquals(
        GodRank.GREATER,
        RankRules.seed(RANKS, greater, new Features(true, true, false), null, GodRank.SPIRIT)
            .rank());
    assertEquals(
        new GodStats(500, 75, 6, 5),
        RankRules.seed(RANKS, greater, new Features(true, true, false), null, GodRank.SPIRIT)
            .stats());
  }

  @Test
  void unlocks() {
    assertTrue(RankRules.unlocked(GodRank.EXALTED, GodRank.RECOGNIZED));
    assertTrue(RankRules.unlocked(GodRank.RECOGNIZED, GodRank.RECOGNIZED));
    assertFalse(RankRules.unlocked(GodRank.SPIRIT, GodRank.RECOGNIZED));
    assertFalse(RankRules.unlocked(null, GodRank.RECOGNIZED));
    assertTrue(RankRules.unlocked(null, GodRank.SPIRIT));

    assertEquals(1, RankRules.maxPriests(1, GodRank.EXALTED, GodRank.GREATER));
    assertEquals(2, RankRules.maxPriests(1, GodRank.GREATER, GodRank.GREATER));

    // power / 10 + 3 holy lands, whatever the rank
    assertEquals(3, RankRules.maxHolyLands(5F));
    assertEquals(8, RankRules.maxHolyLands(50F));
    assertEquals(4, RankRules.maxHolyLands(10F));
    // and each land reaches further with the rank
    assertEquals(0, RANKS.unlocks().holyLandRadiusChunks(GodRank.SPIRIT));
    assertEquals(1, RANKS.unlocks().holyLandRadiusChunks(GodRank.RECOGNIZED));
    assertEquals(4, RANKS.unlocks().holyLandRadiusChunks(GodRank.GREATER));
  }

  @Test
  void requiresPriestAndBelieversCarryOver() {
    assertFalse(RankRules.requiresPriest(RANKS, GodRank.SPIRIT));
    assertTrue(RankRules.requiresPriest(RANKS, GodRank.GREATER));
    assertEquals(0, RankRules.neededBelievers(RANKS, GodRank.SPIRIT));
    assertEquals(3, RankRules.neededBelievers(RANKS, GodRank.DESCENDED));
    assertEquals(20, RankRules.neededBelievers(RANKS, GodRank.GREATER));
  }

  private static Settings.Ranks ranks(Settings.RankConditions all) {
    Map<GodRank, Settings.RankConditions> conditions = new EnumMap<>(GodRank.class);
    for (GodRank rank : GodRank.values()) {
      conditions.put(rank, all);
    }
    return new Settings.Ranks(conditions, 24, RANKS.unlocks(), RANKS.aura());
  }
}
