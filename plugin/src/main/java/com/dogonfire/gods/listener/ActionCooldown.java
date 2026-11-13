package com.dogonfire.gods.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.event.Event;

/**
 * Rate-limits eating (commandments, quests) and reading a holy book (quests): a player's meal or
 * reading counts at most once every {@link #COOLDOWN_MILLIS}, and both share the one timer. Several
 * listeners handle the same event; each asks {@link #ready} and they all get the answer the first
 * one got, so the order they run in doesn't matter. Main thread only.
 */
public final class ActionCooldown {
  public static final long COOLDOWN_MILLIS = 10000L;

  private final Map<UUID, Long> lastTimes = new HashMap<UUID, Long>();
  private Event lastEvent;
  private boolean lastAnswer;

  /**
   * Whether the player's action in this event counts; the first ask for an event starts the
   * cooldown
   */
  public boolean ready(UUID playerId, Event event) {
    if (event == this.lastEvent) {
      return this.lastAnswer;
    }

    long currentTime = System.currentTimeMillis();
    Long lastTime = this.lastTimes.get(playerId);
    boolean ready = lastTime == null || currentTime - lastTime.longValue() > COOLDOWN_MILLIS;
    if (ready) {
      this.lastTimes.put(playerId, currentTime);
    }

    this.lastEvent = event;
    this.lastAnswer = ready;
    return ready;
  }

  /** The player left */
  public void forget(UUID playerId) {
    this.lastTimes.remove(playerId);
    this.lastEvent = null;
  }
}
