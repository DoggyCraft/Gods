package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.dogonfire.gods.model.ChunkKey;
import com.dogonfire.gods.model.HolyLand;
import com.dogonfire.gods.service.HolyLandContest.Outcome;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class HolyLandContestTest {
  private static HolyLand contested() {
    HolyLand land = new HolyLand("0,0", new ChunkKey("world", 0, 0));
    land.setGodName("Odin");
    land.setAttackingGodName("Thor");
    return land;
  }

  @Test
  void anAttackLastsAnHour() {
    Instant start = Instant.parse("2024-05-01T12:00:00Z");
    assertEquals(60L, HolyLandContest.minutesLeft(start, start));
    assertEquals(1L, HolyLandContest.minutesLeft(start, start.plus(Duration.ofMinutes(59))));
    assertEquals(0L, HolyLandContest.minutesLeft(start, start.plus(Duration.ofMinutes(61))));
    assertEquals(0L, HolyLandContest.minutesLeft(null, start));
  }

  @Test
  void tenAttackerKillsWin() {
    HolyLand land = contested();
    for (int i = 1; i < HolyLandContest.ATTACKER_KILLS_TO_WIN; i++) {
      assertEquals(Outcome.NONE, HolyLandContest.recordKill(land, "Thor"));
    }
    assertEquals(1, HolyLandContest.attackerKillsNeeded(land));
    assertEquals(Outcome.ATTACKERS_WIN, HolyLandContest.recordKill(land, "Thor"));
  }

  @Test
  void fiveDefenderKillsWin() {
    HolyLand land = contested();
    for (int i = 1; i < HolyLandContest.DEFENDER_KILLS_TO_WIN; i++) {
      assertEquals(Outcome.NONE, HolyLandContest.recordKill(land, "Odin"));
    }
    assertEquals(Outcome.DEFENDERS_WIN, HolyLandContest.recordKill(land, "Odin"));
    assertEquals(
        Outcome.NONE,
        HolyLandContest.recordKill(contested(), "Loki"),
        "a third god's kills don't count");
  }

  @Test
  void desecrationByOutsidersAndCleansingByBelievers() {
    HolyLand land = contested();
    assertNull(
        HolyLandContest.recordMeal(land, "Thor", false, true), "only unholy food desecrates");
    assertEquals(Outcome.NONE, HolyLandContest.recordMeal(land, null, true, false));
    assertEquals(1, land.desecration());
    assertNull(
        HolyLandContest.recordMeal(land, "Odin", true, false),
        "believers only cleanse with holy food");
    assertEquals(Outcome.NONE, HolyLandContest.recordMeal(land, "Odin", false, true));
    assertEquals(Outcome.NONE, HolyLandContest.recordMeal(land, "Odin", false, true));
    assertEquals(0, land.desecration(), "never below zero");

    land.setDesecration(HolyLandContest.DESECRATION_TO_WIN - 1);
    assertEquals(Outcome.ATTACKERS_WIN, HolyLandContest.recordMeal(land, "Thor", true, false));
  }
}
