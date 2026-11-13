package com.dogonfire.gods.api;

/**
 * How far a god's religion has grown. A god starts as a {@link #SPIRIT} and rises one rank at a
 * time when its religion meets every condition Gods' config sets for the next rank ({@code
 * ranks.<rank>}: believers, power, a priest, its mood, and what the religion has done: prayers,
 * sacrifices, quests, altars, holy land, converts). It falls when it loses its priest or too many
 * believers. Each god type has its own names for the ranks; these constants are the same for all.
 */
public enum GodRank {
  /** A new god */
  SPIRIT,
  /** A religion with a priest and a few believers */
  RECOGNIZED,
  /** The god has descended to its main altar, where it appears as an NPC */
  DESCENDED,
  /** A great religion */
  EXALTED,
  /** The highest rank */
  GREATER
}
