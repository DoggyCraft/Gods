package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PriestRulesTest {
  private static final long HOUR = 3600000L;
  private static final long NOW = 1000 * HOUR;

  @Test
  void eachPriestNeedsSixMoreBelievers() {
    assertTrue(PriestRules.hasEnoughBelievers(3, 0, 3));
    assertFalse(PriestRules.hasEnoughBelievers(8, 1, 3));
    assertTrue(PriestRules.hasEnoughBelievers(9, 1, 3));
    assertTrue(PriestRules.wantsMorePriests(1, 2));
    assertFalse(PriestRules.wantsMorePriests(2, 2));
  }

  @Test
  void aPriestWhoStoppedPrayingIsDropped() {
    assertFalse(PriestRules.isInactive(NOW - 24 * HOUR, NOW, 24));
    assertFalse(PriestRules.isInactive(NOW - 25 * HOUR + 1, NOW, 24), "whole hours");
    assertTrue(PriestRules.isInactive(NOW - 25 * HOUR, NOW, 24));
    assertTrue(PriestRules.isInactive(0L, NOW, 24), "never prayed");
  }

  @Test
  void priesthoodIsOfferedAtMostEveryThreeMinutes() {
    assertTrue(PriestRules.canOfferAgain(null, NOW));
    assertFalse(PriestRules.canOfferAgain(NOW - 179_999L, NOW));
    assertTrue(PriestRules.canOfferAgain(NOW - 180_000L, NOW));
  }

  @Test
  void onlyBelieversWhoAreNotPriestsOrAskedAlreadyAreCandidates() {
    assertTrue(PriestRules.isCandidate(false, false, false, true));
    assertFalse(PriestRules.isCandidate(true, false, false, true));
    assertFalse(PriestRules.isCandidate(false, true, false, true));
    assertFalse(PriestRules.isCandidate(false, false, true, true));
    assertFalse(PriestRules.isCandidate(false, false, false, false), "needs gods.priest");
  }
}
