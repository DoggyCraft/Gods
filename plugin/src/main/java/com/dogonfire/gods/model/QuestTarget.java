package com.dogonfire.gods.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A block that belongs to a god's quest (the pilgrimage chest), found by its position. In
 * quests.yml it is {@code QuestTargets.<hash>.God} and {@code .Location.World/X/Y/Z}, keyed by a
 * hash of the block position ({@link #keyFor}). A plain data object: {@code QuestService} changes
 * it.
 */
public final class QuestTarget {
  private final String key;
  private final String godName;
  private final QuestLocation location;

  /**
   * Keys of the entry this version doesn't know (0.9.x kept LockedBy and LockedTime), written back
   * unchanged
   */
  private Map<String, Object> extras = new LinkedHashMap<String, Object>();

  /**
   * @param key the hash it is stored under, as in the file
   * @param location null if the file has none that can be read
   */
  public QuestTarget(String key, String godName, QuestLocation location) {
    this.key = key;
    this.godName = godName;
    this.location = location;
  }

  /**
   * The key of the target at this block in quests.yml: the hash Gods has always used. Several
   * blocks can share one (the last one set wins, as before), so a lookup also checks the location.
   */
  public static String keyFor(QuestLocation location) {
    return keyFor(location.x(), location.y(), location.z());
  }

  public static String keyFor(int x, int y, int z) {
    return String.valueOf(x * 73856093 ^ y * 19349663 ^ z * 83492791);
  }

  public String key() {
    return this.key;
  }

  public String godName() {
    return this.godName;
  }

  public QuestLocation location() {
    return this.location;
  }

  public Map<String, Object> extras() {
    return this.extras;
  }

  public void setExtras(Map<String, Object> extras) {
    this.extras = extras;
  }
}
