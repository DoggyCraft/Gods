package com.dogonfire.gods.service;

/**
 * When a god wants a priest, who can become one, and when a priest is dropped, without Bukkit so it
 * can be tested. The numbers are kept exactly as they were.
 */
public final class PriestRules {
  /** A god offers priesthood at most once in this many minutes */
  public static final long OFFER_INTERVAL_MINUTES = 3L;

  /** Each priest a god has needs this many more believers before it wants another */
  public static final int BELIEVERS_PER_PRIEST = 6;

  private PriestRules() {}

  /**
   * Whether a god has enough believers for its priests (else it neither looks for priests nor
   * reminds them)
   */
  public static boolean hasEnoughBelievers(int believers, int priests, int minBelievers) {
    return believers >= minBelievers + BELIEVERS_PER_PRIEST * priests;
  }

  /** Whether a god may take another priest */
  public static boolean wantsMorePriests(int priests, int maxPerGod) {
    return priests < maxPerGod;
  }

  /**
   * Whether a priest who last prayed at {@code lastPrayerMillis} (0: never) prayed too long ago to
   * stay one
   */
  public static boolean isInactive(
      long lastPrayerMillis, long nowMillis, int maxHoursWithoutPrayer) {
    long diffHours = (nowMillis - lastPrayerMillis) / 3600000L;
    return diffHours > maxHoursWithoutPrayer;
  }

  /**
   * Whether a god may offer priesthood again, {@code lastOfferMillis} (null: never) after its last
   * offer
   */
  public static boolean canOfferAgain(Long lastOfferMillis, long nowMillis) {
    return lastOfferMillis == null
        || (nowMillis - lastOfferMillis) / 60000L >= OFFER_INTERVAL_MINUTES;
  }

  /** Whether an online believer can be offered priesthood */
  public static boolean isCandidate(
      boolean isPriest, boolean isPendingPriest, boolean offeredRecently, boolean hasPermission) {
    return !isPriest && !isPendingPriest && !offeredRecently && hasPermission;
  }
}
