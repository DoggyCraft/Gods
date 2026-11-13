package com.dogonfire.gods.api.event;

import com.dogonfire.gods.api.DevotionLevel;
import java.util.UUID;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * A believer's devotion level is about to change. A rise they earned ({@link Cause#EARNED}) and a
 * change made through the API ({@link Cause#API}) can be cancelled: the believer keeps their level
 * (a believer who earned a rise keeps the points too, and rises when they next earn devotion). A
 * fall ({@link Cause#FADED}, {@link Cause#CHANGED_GOD}) can't: {@link #isCancellable()} is false
 * and {@link #setCancelled} does nothing. Fired for believers who are offline too (their devotion
 * fades on their god's turn).
 */
public class BelieverDevotionChangeEvent extends GodEvent implements Cancellable {
  private static final HandlerList HANDLERS = new HandlerList();

  /** Why the level changes */
  public enum Cause {
    /** The believer earned the points of a higher level */
    EARNED,
    /**
     * The believer didn't pray for so long that their points fell below what their level needs to
     * be kept
     */
    FADED,
    /** The believer left their god or changed to another, and keeps only part of their points */
    CHANGED_GOD,
    /**
     * A plugin changed it through {@code GodsApi.addDevotion} or {@code GodsApi.setDevotionLevel}
     */
    API
  }

  private final UUID playerId;
  private final DevotionLevel oldLevel;
  private final DevotionLevel newLevel;
  private final Cause cause;
  private boolean cancelled;

  /**
   * @param godName the believer's god (the god they leave, for {@link Cause#CHANGED_GOD})
   * @param playerId the believer, who may be offline
   * @param oldLevel their level before
   * @param newLevel their level after
   * @param cause why it changes
   */
  public BelieverDevotionChangeEvent(
      String godName, UUID playerId, DevotionLevel oldLevel, DevotionLevel newLevel, Cause cause) {
    super(godName);
    this.playerId = playerId;
    this.oldLevel = oldLevel;
    this.newLevel = newLevel;
    this.cause = cause;
  }

  /** The believer, who may be offline */
  public UUID getPlayerId() {
    return this.playerId;
  }

  /** Their level before */
  public DevotionLevel getOldLevel() {
    return this.oldLevel;
  }

  /** Their level after */
  public DevotionLevel getNewLevel() {
    return this.newLevel;
  }

  /** Why it changes */
  public Cause getCause() {
    return this.cause;
  }

  /** Whether the believer rises */
  public boolean isRise() {
    return this.newLevel.compareTo(this.oldLevel) > 0;
  }

  /**
   * Whether cancelling it keeps the believer's level: only for {@link Cause#EARNED} and {@link
   * Cause#API}
   */
  public boolean isCancellable() {
    return this.cause == Cause.EARNED || this.cause == Cause.API;
  }

  @Override
  public boolean isCancelled() {
    return this.cancelled;
  }

  /** Does nothing when the change can't be cancelled ({@link #isCancellable()}) */
  @Override
  public void setCancelled(boolean cancelled) {
    if (isCancellable()) {
      this.cancelled = cancelled;
    }
  }

  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }
}
