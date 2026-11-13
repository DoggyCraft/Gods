package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BlessingRulesTest {
  private static final Instant NOW = Instant.parse("2024-05-01T12:00:00Z");

  @Test
  void strongerGodsGiveLongerEffects() {
    assertEquals(300, BlessingRules.blessingTicks(0.0F));
    assertEquals(600, BlessingRules.blessingTicks(100.0F));
    assertEquals(200, BlessingRules.curseTicks(0.0F));
    assertEquals(300, BlessingRules.curseTicks(50.0F));
    assertEquals(1, BlessingRules.curseMobs(9.9F));
    assertEquals(4, BlessingRules.curseMobs(30.0F));
  }

  @Test
  void toolTiersFollowThePowerLevels() {
    assertEquals(0, BlessingRules.toolTier(10.0F, 10, 20, 30));
    assertEquals(1, BlessingRules.toolTier(10.5F, 10, 20, 30));
    assertEquals(2, BlessingRules.toolTier(25.0F, 10, 20, 30));
    assertEquals(3, BlessingRules.toolTier(31.0F, 10, 20, 30));
  }

  @Test
  void disabledKindsAreAvoidedUnlessAllAre() {
    Random random = new Random(3);
    for (int i = 0; i < 100; i++) {
      assertEquals(
          2, BlessingRules.pickKind(random, BlessingRules.BLESSING_KINDS, kind -> kind == 2));
      int curse =
          BlessingRules.pickKind(
              random, BlessingRules.CURSE_KINDS, kind -> kind < BlessingRules.CURSE_LIGHTNING);
      assertTrue(curse < BlessingRules.CURSE_LIGHTNING);
    }
    // Nothing enabled: something is still picked
    int kind = BlessingRules.pickKind(random, BlessingRules.BLESSING_KINDS, k -> false);
    assertTrue(kind >= 0 && kind < BlessingRules.BLESSING_KINDS);
  }

  @Test
  void blessedIsInSecondsAndCursedInMinutes() {
    assertFalse(BlessingRules.blessingOver(NOW.minusSeconds(60), NOW, 60));
    assertTrue(BlessingRules.blessingOver(NOW.minusSeconds(61), NOW, 60));
    assertFalse(BlessingRules.curseOver(NOW.minus(Duration.ofMinutes(10)), NOW, 10));
    assertFalse(
        BlessingRules.curseOver(NOW.minus(Duration.ofMinutes(11)).plusMillis(1), NOW, 10),
        "whole minutes");
    assertTrue(BlessingRules.curseOver(NOW.minus(Duration.ofMinutes(11)), NOW, 10));
  }

  /** A curse sends one of the theme's curse mobs, each of them now and then; none without any */
  @Test
  void curseMobsComeFromTheList() {
    Random random = new Random(5);
    List<String> mobs = List.of("stray", "skeleton", "wolf");
    Set<String> seen = new HashSet<>();
    for (int i = 0; i < 100; i++) {
      seen.add(BlessingRules.curseMob(random, mobs));
    }
    assertEquals(Set.copyOf(mobs), seen);
    assertNull(BlessingRules.curseMob(random, List.of()));
  }
}
