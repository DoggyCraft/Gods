package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.model.GodType;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

class GodRulesTest {
  @Test
  void godNamesAreCapitalized() {
    assertEquals("Thor", GodRules.formatGodName("tHOR"));
    assertEquals("Odin", GodRules.formatGodName("Odin"));
  }

  @Test
  void everyTypeHasItsColor() {
    assertEquals(ChatColor.DARK_GRAY, GodRules.color(GodType.THUNDER));
    assertEquals(ChatColor.RED, GodRules.color(GodType.EVIL));
    assertEquals(ChatColor.WHITE, GodRules.color(GodType.PARTY));
    assertEquals(ChatColor.WHITE, GodRules.color(null));
  }

  @Test
  void theVoiceComesFromTypeAndGender() {
    assertEquals(List.of("thunder_male"), GodRules.voices(GodType.THUNDER, GodGender.Male, 0L));
    assertEquals(List.of("love_female"), GodRules.voices(GodType.LOVE, GodGender.Female, 1L));
    // No gender: the seed picks one, always the same (as before non-binary gods)
    assertEquals(List.of("sea_female"), GodRules.voices(GodType.SEA, GodGender.None, 42L));
    assertEquals(List.of("sea_male"), GodRules.voices(GodType.SEA, GodGender.None, 43L));
    assertEquals(List.of("sea_female"), GodRules.voices(GodType.SEA, GodGender.None, 44L));
    // Non-binary: both voices of its type, whatever the seed
    assertEquals(
        List.of("sea_male", "sea_female"), GodRules.voices(GodType.SEA, GodGender.NonBinary, 42L));
    // No such god
    assertEquals(List.of("sun_male"), GodRules.voices(null, null, 0L));
  }

  /** A god without a gender word on its sign chooses its gender from its seed, among all three */
  @Test
  void aGodChoosesItsGenderFromItsSeed() {
    assertEquals(GodGender.Female, GodRules.chosenGender(42L));
    assertEquals(GodGender.Male, GodRules.chosenGender(43L));
    assertEquals(GodGender.NonBinary, GodRules.chosenGender(44L));
    assertEquals(GodGender.Male, GodRules.chosenGender(-2L));
    assertEquals(GodRules.chosenGender(7L), GodRules.chosenGender(7L));
    Set<GodGender> chosen = new HashSet<>();
    for (long seed = 1000L; seed < 1010L; seed++) {
      chosen.add(GodRules.chosenGender(seed));
    }
    assertEquals(Set.of(GodGender.Male, GodGender.Female, GodGender.NonBinary), chosen);
  }

  @Test
  void priestsWaitBetweenGenderChanges() {
    Instant now = Instant.parse("2026-11-11T12:00:00Z");
    assertEquals(Duration.ZERO, GodRules.genderChangeWait(null, now, 24));
    assertEquals(
        Duration.ofHours(23), GodRules.genderChangeWait(now.minus(Duration.ofHours(1)), now, 24));
    assertEquals(
        Duration.ZERO, GodRules.genderChangeWait(now.minus(Duration.ofHours(24)), now, 24));
    assertEquals(Duration.ZERO, GodRules.genderChangeWait(now.minus(Duration.ofDays(3)), now, 24));
    assertEquals(Duration.ZERO, GodRules.genderChangeWait(now, now, 0));
    // A change in the future (the clock went back) waits from then
    assertEquals(
        Duration.ofHours(25), GodRules.genderChangeWait(now.plus(Duration.ofHours(1)), now, 24));
  }

  @Test
  void powerIsTheBelieversPowerButAtLeastTheMinimum() {
    assertEquals(3.5F, GodRules.godPower(List.of(1.0F, 2.5F), 0));
    assertEquals(10.0F, GodRules.godPower(List.of(1.0F, 2.5F), 10));
    assertEquals(0.0F, GodRules.godPower(List.of(), 0));
  }

  @Test
  void aGodWithoutBelieversAndPowerIsDead() {
    assertTrue(GodRules.isDead(0, 0.5F));
    assertFalse(GodRules.isDead(0, 1.0F), "the whitelist keeps it alive");
    assertFalse(GodRules.isDead(1, 0.0F));
  }

  @Test
  void verbosityGrowsWithPowerAndTheSetting() {
    long now = 0L;
    int quiet = GodRules.verbosity(1, 7L, 0.0F, 1.0, now);
    int loud = GodRules.verbosity(50, 7L, 1000.0F, 1.0, now);
    assertTrue(quiet >= 1);
    assertTrue(loud > quiet);
    assertEquals(1, GodRules.verbosity(50, 7L, 1000.0F, 0.0, now), "verbosity 0 in config.yml");
    assertEquals(
        GodRules.verbosity(10, 7L, 5.0F, 2.0, now),
        GodRules.verbosity(10, 7L, 5.0F, 2.0, now),
        "the seed fixes it");
  }
}
