package com.dogonfire.gods.service;

/**
 * The rules of holy artifacts, without a server so they can be tested: the rank an artifact earns
 * by being used, and how artifacts from before the tags are recognized by their lore. Kept exactly
 * as they were; the texts are {@code artifact.*} in messages.yml.
 */
public final class HolyArtifactRules {
  private HolyArtifactRules() {}

  /**
   * The rank an artifact has after this many uses (its key under artifact.rank in messages.yml); ""
   * below 5 uses
   */
  public static String rank(int uses) {
    if (uses < 5) {
      return "";
    }
    if (uses < 20) {
      return "shiny";
    }
    if (uses < 50) {
      return "honorable";
    }
    if (uses < 100) {
      return "holy";
    }
    if (uses < 150) {
      return "saintly";
    }
    if (uses < 200) {
      return "amazing";
    }
    if (uses < 300) {
      return "mega";
    }
    if (uses < 500) {
      return "awesome";
    }
    if (uses < 1000) {
      return "epic";
    }
    return "legendary";
  }

  /**
   * The first lore line (without colors) of an artifact made before artifacts were tagged. These
   * were always in English, whatever messages.yml says now.
   */
  public static boolean isArtifactLine(String firstLine) {
    return firstLine.startsWith("A Holy artifact") || firstLine.startsWith("An Unholy artifact");
  }
}
