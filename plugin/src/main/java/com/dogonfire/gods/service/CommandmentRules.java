package com.dogonfire.gods.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * How a god picks its commandments and sacrifice needs from its type's theme, without Bukkit so it
 * can be tested. The lists may be anything an admin wrote in themes.yml, so nothing here loops
 * without a limit.
 */
public final class CommandmentRules {
  /** A sacrifice need is drawn at most this often */
  public static final int MAX_DRAWS = 20;

  /** How many items of its type's sacrifice needs a god's seed picks */
  public static final int NEEDS_PER_GOD = 5;

  private CommandmentRules() {}

  /**
   * A random option that isn't {@code avoid} (e.g. the holy mob, when picking the unholy one);
   * {@code avoid} itself only when it is the only option. Null when there are no options.
   */
  public static <T> T pick(Random random, List<T> options, T avoid) {
    List<T> others = new ArrayList<>(options);
    others.removeIf(option -> option.equals(avoid));
    if (others.isEmpty()) {
      return options.isEmpty() ? null : options.get(0);
    }
    return others.get(random.nextInt(others.size()));
  }

  /**
   * What a god wants sacrificed now: its seed picks {@link #NEEDS_PER_GOD} of its type's needs
   * (plus one more choice, its unholy food, which it may want burnt), and one of those is drawn,
   * never its holy food. Null if none fits.
   *
   * @param seed the god's seed, so a god always wants from the same few items
   * @param needs the god type's sacrifice needs
   * @param unholyFood the god's unholy food, asked for when that choice is drawn (may return null)
   * @param holyFood the god's holy food, never asked for
   */
  public static <T> T sacrificeNeed(
      Random random, long seed, List<T> needs, Supplier<T> unholyFood, T holyFood) {
    Random seedRandom = new Random(seed);
    List<Integer> picked = new ArrayList<>();
    for (int n = 0; n < NEEDS_PER_GOD; n++) {
      // needs.size() is the unholy food
      picked.add(seedRandom.nextInt(needs.size() + 1));
    }

    for (int draw = 0; draw < MAX_DRAWS; draw++) {
      int index = picked.get(random.nextInt(picked.size()));
      T need = index < needs.size() ? needs.get(index) : unholyFood.get();
      if (need != null && !need.equals(holyFood)) {
        return need;
      }
    }
    return null;
  }
}
