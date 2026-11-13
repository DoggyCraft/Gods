package com.dogonfire.gods.service;

import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Names of holy lands, without a server so it can be tested: the group of words a biome's names
 * come from (matched by the biome's key, so biomes newer than the server's API need no constant), a
 * name picked from a group's words (messages.yml {@code
 * holy-land.names.<group>.prefix/middle/suffix}: prefix + middle + suffix), and the names priests
 * may give their land (/g landname).
 */
public final class HolyLandNames {
  /** The group of biomes no other group covers, and whose words fill a group's empty list */
  public static final String DEFAULT = "default";

  /** Every group, as messages.yml names them */
  public static final List<String> GROUPS =
      List.of(
          DEFAULT,
          "plains",
          "forest",
          "dark-forest",
          "snow",
          "desert",
          "badlands",
          "jungle",
          "swamp",
          "ocean",
          "mountains",
          "cherry",
          "mushroom",
          "savanna",
          "nether",
          "end");

  /** The parts of a name, in the order they are joined */
  public static final List<String> PARTS = List.of("prefix", "middle", "suffix");

  /** The longest name /g landname takes */
  public static final int MAX_LENGTH = 32;

  private HolyLandNames() {}

  /** The messages.yml key of a group's part ({@link #PARTS}) */
  public static String key(String group, String part) {
    return "holy-land.names." + group + "." + part;
  }

  /**
   * The group of a biome by its key ({@code minecraft:} left out or not, any case): the first that
   * matches of end, nether, mushroom, cherry, dark-forest, snow, ocean, swamp, jungle, desert,
   * badlands, savanna, mountains, forest, plains; else {@link #DEFAULT} (caves, the void, biomes of
   * mods)
   */
  public static String group(String biomeKey) {
    if (biomeKey == null) {
      return DEFAULT;
    }
    String key = biomeKey.toLowerCase(Locale.ROOT);
    int colon = key.indexOf(':');
    if (colon >= 0) {
      key = key.substring(colon + 1);
    }

    if (key.equals("the_end") || key.startsWith("end_") || key.endsWith("end_islands")) {
      return "end";
    }
    if (has(key, "nether", "crimson", "warped", "soul_sand", "basalt")) {
      return "nether";
    }
    if (has(key, "mushroom")) {
      return "mushroom";
    }
    if (has(key, "cherry")) {
      return "cherry";
    }
    if (has(key, "dark_forest", "pale_garden")) {
      return "dark-forest";
    }
    if (has(key, "snowy", "frozen") || key.startsWith("ice") || key.equals("grove")) {
      return "snow";
    }
    if (has(key, "ocean", "beach", "river", "shore")) {
      return "ocean";
    }
    if (has(key, "swamp")) {
      return "swamp";
    }
    if (has(key, "jungle")) {
      return "jungle";
    }
    if (has(key, "desert")) {
      return "desert";
    }
    if (has(key, "badlands")) {
      return "badlands";
    }
    if (has(key, "savanna")) {
      return "savanna";
    }
    if (has(key, "peaks", "hills", "slopes", "mountain", "windswept")) {
      return "mountains";
    }
    if (has(key, "forest", "taiga")) {
      return "forest";
    }
    if (has(key, "plains", "meadow")) {
      return "plains";
    }
    return DEFAULT;
  }

  private static boolean has(String key, String... parts) {
    for (String part : parts) {
      if (key.contains(part)) {
        return true;
      }
    }
    return false;
  }

  /**
   * A name: a random prefix, middle and suffix joined as they are (the words carry their own
   * spaces), trimmed, with runs of spaces made one. An empty list adds nothing. Null if that leaves
   * nothing.
   */
  public static String pick(
      List<String> prefixes, List<String> middles, List<String> suffixes, Random random) {
    String name =
        (one(prefixes, random) + one(middles, random) + one(suffixes, random))
            .trim()
            .replaceAll(" {2,}", " ");
    return name.isEmpty() ? null : name;
  }

  private static String one(List<String> words, Random random) {
    return words == null || words.isEmpty() ? "" : words.get(random.nextInt(words.size()));
  }

  /**
   * A name a priest typed for their land: trimmed, with runs of spaces made one; null if it is
   * empty, longer than {@link #MAX_LENGTH} or has color codes (§ or &amp;)
   */
  public static String validName(String typed) {
    if (typed == null) {
      return null;
    }
    String name = typed.trim().replaceAll("\\s+", " ");
    if (name.isEmpty()
        || name.length() > MAX_LENGTH
        || name.indexOf('§') >= 0
        || name.indexOf('&') >= 0) {
      return null;
    }
    return name;
  }
}
