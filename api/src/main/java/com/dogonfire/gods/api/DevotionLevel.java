package com.dogonfire.gods.api;

/**
 * How devoted a believer is to their god. Every believer starts as a {@link #FOLLOWER} and rises
 * when the devotion points they earn serving their god (prayers, sacrifices it wanted, quests,
 * players they converted, its commandments) reach the next level's threshold in Gods' config
 * ({@code devotion.levels}). Devotion fades while they don't pray, and they lose a level only after
 * real neglect. Each level unlocks something ({@code devotion.unlocks}). Each god type has its own
 * names for the levels; these constants are the same for all.
 */
public enum DevotionLevel {
  /** Every believer */
  FOLLOWER,
  /** A believer who prays regularly */
  DEVOTEE,
  /** A believer who serves their god in many ways */
  ZEALOT,
  /** The highest level */
  SAINT
}
