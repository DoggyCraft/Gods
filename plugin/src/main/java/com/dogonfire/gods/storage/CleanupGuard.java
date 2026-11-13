package com.dogonfire.gods.storage;

import java.time.Duration;

/**
 * Whether the update loop may remove records because of what another file says: dead gods (no
 * believers, no power) and priests who stopped praying are judged from believers.yml, and removing
 * a god also changes holyland.yml and bibles.yml. A file that could not be read (or is from a newer
 * Gods) holds nothing, so judging from it would remove every god and priest; and right after a
 * start or a reload a hand edit may still be in progress. Records are never removed then. Pure, so
 * it can be tested without a server.
 */
public final class CleanupGuard {
  /** How long after the data was (re)loaded nothing is removed */
  public static final Duration GRACE_PERIOD = Duration.ofMinutes(10);

  public enum Verdict {
    /** Records may be removed */
    ALLOWED,
    /** The data was loaded less than {@link #GRACE_PERIOD} ago */
    GRACE_PERIOD,
    /** A file the removal reads or changes could not be read, or is from a newer Gods */
    FILE_NOT_READABLE,
    /** There are gods but not a single believer: believers.yml is missing or lost its entries */
    NO_BELIEVERS
  }

  private CleanupGuard() {}

  /**
   * @param anyFileReadOnly whether gods.yml, believers.yml, holyland.yml or bibles.yml is read-only
   * @param gods the number of gods loaded
   * @param believers the number of believers loaded
   * @param sinceLoad how long ago the data was (re)loaded, or null if it was not loaded yet
   */
  public static Verdict check(
      boolean anyFileReadOnly, int gods, int believers, Duration sinceLoad) {
    if (anyFileReadOnly) {
      return Verdict.FILE_NOT_READABLE;
    }
    if (gods > 0 && believers == 0) {
      return Verdict.NO_BELIEVERS;
    }
    if (sinceLoad == null || sinceLoad.compareTo(GRACE_PERIOD) < 0) {
      return Verdict.GRACE_PERIOD;
    }
    return Verdict.ALLOWED;
  }
}
