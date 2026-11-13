package com.dogonfire.gods.service;

import java.time.Instant;
import java.util.Random;
import java.util.function.IntPredicate;

/**
 * The numbers behind blessings and curses, without Bukkit so they can be tested: how strong and
 * long an effect is, which tool tier a god gives, how long a player stays blessed or cursed, and
 * how many mobs a curse sends. Kept exactly as they were.
 */
public final class BlessingRules {
  /** Blessing effects: haste, instant health, regeneration, speed, strength */
  public static final int BLESSING_KINDS = 5;

  /** Curse effects: blindness, nausea, poison, slowness, mining fatigue, lightning, hostile mobs */
  public static final int CURSE_KINDS = 7;

  public static final int CURSE_LIGHTNING = 5;
  public static final int CURSE_MOBS = 6;

  /** Tries before a disabled kind is used anyway (when everything is switched off) */
  private static final int MAX_TRIES = 50;

  private BlessingRules() {}

  /**
   * A random kind of blessing or curse among {@code kinds}, preferring those that are {@code
   * enabled}
   */
  public static int pickKind(Random random, int kinds, IntPredicate enabled) {
    int kind;
    int tries = 0;
    do {
      kind = random.nextInt(kinds);
      tries++;
    } while (tries < MAX_TRIES && !enabled.test(kind));
    return kind;
  }

  /** How much stronger a god's blessings and curses are with its power */
  public static float effectPower(float godPower) {
    return 1.0F + godPower / 100.0F;
  }

  /** How long a blessing effect lasts, in ticks */
  public static int blessingTicks(float godPower) {
    return (int) (300.0F * effectPower(godPower));
  }

  /** How long a curse effect lasts, in ticks */
  public static int curseTicks(float godPower) {
    return (int) (200.0F * effectPower(godPower));
  }

  /**
   * The kind of mob a curse sends: one of the theme's curse mobs, picked at random; null if there
   * are none
   */
  public static <T> T curseMob(Random random, java.util.List<T> curseMobs) {
    return curseMobs.isEmpty() ? null : curseMobs.get(random.nextInt(curseMobs.size()));
  }

  /** How many hostile mobs a curse sends */
  public static int curseMobs(float godPower) {
    return 1 + (int) (godPower / 10.0F);
  }

  /** The tool tier a god of this power gives: 0 wood, 1 stone, 2 iron, 3 diamond */
  public static int toolTier(float godPower, int level1Power, int level2Power, int level3Power) {
    if (godPower > level3Power) {
      return 3;
    }
    if (godPower > level2Power) {
      return 2;
    }
    if (godPower > level1Power) {
      return 1;
    }
    return 0;
  }

  /**
   * Whether a player blessed at {@code blessedTime} is no longer the blessed player (config.yml:
   * seconds)
   */
  public static boolean blessingOver(Instant blessedTime, Instant now, int blessedSeconds) {
    return (now.toEpochMilli() - blessedTime.toEpochMilli()) / 1000L > blessedSeconds;
  }

  /**
   * Whether a player cursed at {@code cursedTime} is no longer the cursed player (config.yml:
   * minutes)
   */
  public static boolean curseOver(Instant cursedTime, Instant now, int cursedMinutes) {
    return (now.toEpochMilli() - cursedTime.toEpochMilli()) / 60000L > cursedMinutes;
  }
}
