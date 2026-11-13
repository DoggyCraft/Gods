package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dogonfire.gods.config.Theme.EffectMood;
import com.dogonfire.gods.model.GodMood;
import com.dogonfire.gods.model.GodRank;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class EffectRulesTest {
  @Test
  void theGodsMoodPicksTheStyle() {
    assertEquals(EffectMood.PLEASED, EffectRules.moodOf(GodMood.EXALTED));
    assertEquals(EffectMood.PLEASED, EffectRules.moodOf(GodMood.PLEASED));
    assertEquals(EffectMood.NEUTRAL, EffectRules.moodOf(GodMood.NEUTRAL));
    assertEquals(EffectMood.ANGRY, EffectRules.moodOf(GodMood.DISPLEASED));
    assertEquals(EffectMood.ANGRY, EffectRules.moodOf(GodMood.ANGRY));
    assertEquals(EffectMood.NEUTRAL, EffectRules.moodOf(null));
  }

  @Test
  void wantedSacrificesPleaseUnwantedOnesAnger() {
    assertEquals(EffectMood.PLEASED, EffectRules.sacrificeMood(SacrificeRules.Verdict.GOOD));
    assertEquals(EffectMood.NEUTRAL, EffectRules.sacrificeMood(SacrificeRules.Verdict.MEH));
    assertEquals(EffectMood.ANGRY, EffectRules.sacrificeMood(SacrificeRules.Verdict.BAD));
  }

  /**
   * Brighter and higher when pleased, short and low when angry; always a small, fixed number of
   * particles
   */
  @Test
  void prayersRiseWithTheMood() {
    EffectRules.Waves pleased = EffectRules.prayer(EffectMood.PLEASED);
    EffectRules.Waves neutral = EffectRules.prayer(EffectMood.NEUTRAL);
    EffectRules.Waves angry = EffectRules.prayer(EffectMood.ANGRY);
    assertTrue(pleased.rise() > neutral.rise() && neutral.rise() > angry.rise());
    assertTrue(pleased.pitch() > neutral.pitch() && neutral.pitch() > angry.pitch());
    assertTrue(
        angry.waves() * angry.ticksApart() < pleased.waves() * pleased.ticksApart(),
        "angry is shorter");
    for (EffectMood mood : EffectMood.values()) {
      assertTrue(EffectRules.prayer(mood).total() <= 64, mood.name());
    }
    assertTrue(EffectRules.BLESSING.total() <= 64 && EffectRules.CURSE.total() <= 64);
  }

  /** Blessings glow upwards with a high chime, curses swirl down with a low sound */
  @Test
  void blessingsRiseCursesSink() {
    assertTrue(EffectRules.BLESSING.rise() > 0);
    assertTrue(EffectRules.CURSE.rise() < 0);
    assertTrue(EffectRules.BLESSING.pitch() > 1F && EffectRules.CURSE.pitch() < 1F);
  }

  @Test
  void sacrificesFloatAndEnd() {
    for (EffectMood mood : EffectMood.values()) {
      EffectRules.Rise rise = EffectRules.sacrifice(mood);
      // Under 180 degrees a step, so the client turns it the right way; gone within two seconds
      assertTrue(rise.spinDegrees() < 180F, mood.name());
      assertTrue(rise.ticks() <= 40, mood.name());
    }
    assertTrue(
        EffectRules.sacrifice(EffectMood.PLEASED).rise()
            > EffectRules.sacrifice(EffectMood.ANGRY).rise());
  }

  @Test
  void angryGodsShowNoSymbol() {
    assertTrue(EffectRules.showsSymbol(EffectMood.PLEASED, "❄"));
    assertTrue(EffectRules.showsSymbol(EffectMood.NEUTRAL, "❄"));
    assertFalse(EffectRules.showsSymbol(EffectMood.ANGRY, "❄"));
    assertFalse(EffectRules.showsSymbol(EffectMood.PLEASED, ""));
  }

  @Test
  void onlyNearbyPlayersSeeEffects() {
    assertTrue(EffectRules.inView(32 * 32, 32));
    assertFalse(EffectRules.inView(32 * 32 + 1, 32));
  }

  @Test
  void ringsSpiralUpwards() {
    EffectRules.Waves waves = new EffectRules.Waves(3, 4, 2.0, 1.0, 2, 1F);
    double[] first = EffectRules.ringOffset(waves, 0, 0);
    assertEquals(1.0, first[0], 1e-9);
    assertEquals(0.0, first[1], 1e-9);
    assertEquals(0.0, first[2], 1e-9);
    double[] last = EffectRules.ringOffset(waves, 2, 1);
    assertEquals(2.0, last[1], 1e-9);
    assertEquals(1.0, Math.hypot(last[0], last[2]), 1e-9);
  }

  @Test
  void picksFromTheList() {
    assertNull(EffectRules.pick(List.of(), new Random(1)));
    assertEquals("a", EffectRules.pick(List.of("a"), new Random(1)));
  }

  @Test
  void colorsBecomeLegacyHexCodes() {
    assertEquals("§x§a§e§e§8§f§f", EffectRules.legacyColor(0xAEE8FF));
    assertEquals("§x§0§0§0§0§0§1", EffectRules.legacyColor(1));
  }

  /**
   * A spirit's effects are a modest flicker; they grow with each rank, and the symbol and floating
   * sacrifice come with it
   */
  @Test
  void effectsGrowWithTheRank() {
    EffectRules.Waves previous = null;
    for (GodRank rank : GodRank.values()) {
      EffectRules.Waves waves = EffectRules.prayer(EffectMood.PLEASED, rank);
      assertTrue(waves.waves() >= 2 && waves.particles() >= 4, rank.name());
      // Still a small, fixed number per viewer
      assertTrue(waves.total() <= 150, rank + ": " + waves.total());
      if (previous != null) {
        assertTrue(waves.total() > previous.total(), rank.name());
        assertTrue(waves.rise() > previous.rise(), rank.name());
      }
      previous = waves;
      EffectRules.Waves rankUp = EffectRules.rankUp(rank);
      assertTrue(rankUp.total() > waves.total(), rank.name());
      assertTrue(rankUp.total() <= 250, rank + " rank up: " + rankUp.total());
    }
    assertEquals(
        EffectRules.prayer(EffectMood.NEUTRAL),
        EffectRules.prayer(EffectMood.NEUTRAL, GodRank.DESCENDED));
    assertTrue(
        EffectRules.prayer(EffectMood.PLEASED, GodRank.SPIRIT).total()
            < EffectRules.prayer(EffectMood.PLEASED).total());

    assertFalse(EffectRules.showsSymbol(EffectMood.PLEASED, "✦", GodRank.SPIRIT));
    assertFalse(EffectRules.showsSymbol(EffectMood.PLEASED, "✦", GodRank.RECOGNIZED));
    assertTrue(EffectRules.showsSymbol(EffectMood.PLEASED, "✦", GodRank.DESCENDED));
    assertFalse(EffectRules.showsSymbol(EffectMood.ANGRY, "✦", GodRank.GREATER));
    assertFalse(EffectRules.tier(GodRank.SPIRIT).sacrificeDisplay());
    assertTrue(EffectRules.tier(GodRank.RECOGNIZED).sacrificeDisplay());
    assertEquals(EffectRules.tier(GodRank.SPIRIT), EffectRules.tier(null));
  }
}
