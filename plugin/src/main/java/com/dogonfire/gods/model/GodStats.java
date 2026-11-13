package com.dogonfire.gods.model;

/**
 * What a god's religion has done in its whole life ({@code Stats.*} in gods.yml), counted towards
 * its ranks: prayers at its altars, sacrifices it accepted, quests completed and believers who
 * joined through an invitation. Immutable.
 *
 * @param prayers prayers at its altars ({@code Stats.Prayers})
 * @param sacrifices sacrifices it was pleased with ({@code Stats.Sacrifices})
 * @param quests quests its believers completed ({@code Stats.Quests})
 * @param converts players who joined it by accepting an invitation ({@code Stats.Converts})
 */
public record GodStats(int prayers, int sacrifices, int quests, int converts) {
  public static final GodStats ZERO = new GodStats(0, 0, 0, 0);

  public GodStats withPrayers(int prayers) {
    return new GodStats(prayers, this.sacrifices, this.quests, this.converts);
  }

  public GodStats withSacrifices(int sacrifices) {
    return new GodStats(this.prayers, sacrifices, this.quests, this.converts);
  }

  public GodStats withQuests(int quests) {
    return new GodStats(this.prayers, this.sacrifices, quests, this.converts);
  }

  public GodStats withConverts(int converts) {
    return new GodStats(this.prayers, this.sacrifices, this.quests, converts);
  }

  /** Each counter at least the other's */
  public GodStats atLeast(GodStats other) {
    return new GodStats(
        Math.max(this.prayers, other.prayers),
        Math.max(this.sacrifices, other.sacrifices),
        Math.max(this.quests, other.quests),
        Math.max(this.converts, other.converts));
  }
}
