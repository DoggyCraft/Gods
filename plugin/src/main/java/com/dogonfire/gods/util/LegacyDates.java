package com.dogonfire.gods.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * The date formats the data files have always used, in the server's time zone: {@link #SECONDS}
 * ({@code "HH:mm:ss dd-MM-yyyy"}, believers.yml and marriages.yml) and {@link #MINUTES} ({@code
 * "HH:mm dd-MM-yyyy"}, holyland.yml). Parsing is as lenient as it always was ({@link
 * SimpleDateFormat}), so every file that loaded before still loads. Models keep {@link Instant}s;
 * repositories convert with these helpers.
 */
public enum LegacyDates {
  SECONDS("HH:mm:ss dd-MM-yyyy", ChronoUnit.SECONDS),
  MINUTES("HH:mm dd-MM-yyyy", ChronoUnit.MINUTES);

  private final String pattern;
  private final ChronoUnit precision;

  LegacyDates(String pattern, ChronoUnit precision) {
    this.pattern = pattern;
    this.precision = precision;
  }

  /** The time in the file, or null if it is missing or can't be read */
  public Instant parse(String text) {
    if (text == null) {
      return null;
    }
    try {
      // SimpleDateFormat is not thread-safe, so one per call
      return new SimpleDateFormat(this.pattern).parse(text).toInstant();
    } catch (ParseException ex) {
      return null;
    }
  }

  /** The text written to the file, or null for null */
  public String format(Instant time) {
    return time != null ? new SimpleDateFormat(this.pattern).format(Date.from(time)) : null;
  }

  /**
   * Now, cut to what the format keeps. The old code compared against the time read back from the
   * file, so keeping this precision in memory gives the same results.
   */
  public Instant now() {
    return Instant.now().truncatedTo(this.precision);
  }
}
