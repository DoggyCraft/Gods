package com.dogonfire.gods.api;

import java.time.Instant;
import org.bukkit.Location;

/**
 * A snapshot of a god's current quest, as it was when it was fetched.
 *
 * @param god the god that gave the quest
 * @param type what kind of quest it is
 * @param target what the believers have to slay, sacrifice or find (a mob or item type name, e.g.
 *     {@code ZOMBIE}, or the name of a pilgrimage's holy place), or null if the quest has none
 * @param amount how many it takes in all
 * @param progress how many the believers have done
 * @param started when the god gave the quest, or null if that isn't known
 * @param expires when the quest fails if it isn't done, or null if that isn't known
 * @param location where a pilgrimage leads, or null for other quests and while its world isn't
 *     loaded
 */
public record QuestInfo(
    String god,
    QuestType type,
    String target,
    int amount,
    int progress,
    Instant started,
    Instant expires,
    Location location) {
  public QuestInfo {
    location = location != null ? location.clone() : null;
  }

  /** A copy, so changing it doesn't change the snapshot */
  @Override
  public Location location() {
    return this.location != null ? this.location.clone() : null;
  }
}
