package com.dogonfire.gods.service;

import com.dogonfire.gods.model.HolyLand;
import java.time.Instant;

/**
 * The rules of an attack on a holy land (a priest started it with /g attack), without Bukkit so
 * they can be tested. The attackers win with {@value #ATTACKER_KILLS_TO_WIN} kills in the land, or
 * when outsiders eat the god's unholy food there {@value #DESECRATION_TO_WIN} times (the god's
 * believers eating its holy food undo one each); the defenders win with {@value
 * #DEFENDER_KILLS_TO_WIN} kills, or by holding out {@value #CONTEST_MINUTES} minutes.
 */
public final class HolyLandContest {
  /** How long an attack lasts before the defenders win */
  public static final long CONTEST_MINUTES = 60L;

  public static final int ATTACKER_KILLS_TO_WIN = 10;
  public static final int DEFENDER_KILLS_TO_WIN = 5;
  public static final int DESECRATION_TO_WIN = 30;

  public enum Outcome {
    /** The attack goes on */
    NONE,
    /** The land goes to the attacking god */
    ATTACKERS_WIN,
    /** The land stays with its god */
    DEFENDERS_WIN
  }

  private HolyLandContest() {}

  /**
   * Minutes left of an attack that started at {@code contestedTime}; 0 when the defenders have held
   * out (or unknown)
   */
  public static long minutesLeft(Instant contestedTime, Instant now) {
    if (contestedTime == null) {
      return 0L;
    }
    long elapsedMinutes = (now.toEpochMilli() - contestedTime.toEpochMilli()) / 60000L;
    return Math.max(0L, CONTEST_MINUTES - elapsedMinutes);
  }

  /**
   * A believer of {@code killerGodName} killed a player in the contested land: counts for their
   * side
   */
  public static Outcome recordKill(HolyLand land, String killerGodName) {
    if (killerGodName.equals(land.attackingGodName())) {
      land.setAttackerKills(Math.max(0, land.attackerKills() + 1));
      return land.attackerKills() >= ATTACKER_KILLS_TO_WIN ? Outcome.ATTACKERS_WIN : Outcome.NONE;
    }
    if (killerGodName.equals(land.godName())) {
      land.setDefenderKills(Math.max(0, land.defenderKills() + 1));
      return land.defenderKills() >= DEFENDER_KILLS_TO_WIN ? Outcome.DEFENDERS_WIN : Outcome.NONE;
    }
    return Outcome.NONE;
  }

  /**
   * Someone who believes in {@code eaterGodName} (null: no god) ate in the contested land. Returns
   * null if the meal doesn't count (nothing changed), else the outcome.
   */
  public static Outcome recordMeal(
      HolyLand land, String eaterGodName, boolean unholyFood, boolean holyFood) {
    if (!land.godName().equals(eaterGodName)) {
      if (!unholyFood) {
        return null;
      }
      land.setDesecration(Math.max(0, land.desecration() + 1));
      return land.desecration() >= DESECRATION_TO_WIN ? Outcome.ATTACKERS_WIN : Outcome.NONE;
    }

    if (!holyFood) {
      return null;
    }
    land.setDesecration(Math.max(0, land.desecration() - 1));
    return Outcome.NONE;
  }

  public static int attackerKillsNeeded(HolyLand land) {
    return Math.max(0, ATTACKER_KILLS_TO_WIN - land.attackerKills());
  }

  public static int defenderKillsNeeded(HolyLand land) {
    return Math.max(0, DEFENDER_KILLS_TO_WIN - land.defenderKills());
  }
}
