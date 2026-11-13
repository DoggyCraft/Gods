package com.dogonfire.gods.api.event;

import com.dogonfire.gods.api.GodRank;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * A god's rank is about to change. A rise ({@link Cause#RANK_UP}, {@link Cause#API}) can be
 * cancelled: the god keeps its rank, and a rank up is tried again on a later turn. A fall ({@link
 * Cause#NO_PRIEST}, {@link Cause#TOO_FEW_BELIEVERS}) and the placing of a god from before ranks
 * ({@link Cause#SEEDED}) can't: {@link #isCancellable()} is false and {@link #setCancelled} does
 * nothing. Crossing into or out of {@link GodRank#DESCENDED} also fires {@link GodDescendEvent} or
 * {@link GodAscendEvent}.
 */
public class GodRankChangeEvent extends GodEvent implements Cancellable {
  private static final HandlerList HANDLERS = new HandlerList();

  /** Why the rank changes */
  public enum Cause {
    /** The god met every condition of the next rank */
    RANK_UP,
    /** The god has no priest any more, and falls below every rank that needs one */
    NO_PRIEST,
    /** The god had too few believers for its rank for longer than the grace period */
    TOO_FEW_BELIEVERS,
    /** A god from before ranks gets the rank its religion qualifies for (once) */
    SEEDED,
    /** A plugin set it through {@code GodsApi.setRank} */
    API
  }

  private final GodRank oldRank;
  private final GodRank newRank;
  private final Cause cause;
  private boolean cancelled;

  public GodRankChangeEvent(String godName, GodRank oldRank, GodRank newRank, Cause cause) {
    super(godName);
    this.oldRank = oldRank;
    this.newRank = newRank;
    this.cause = cause;
  }

  /** The god's rank before */
  public GodRank getOldRank() {
    return this.oldRank;
  }

  /** The god's rank after */
  public GodRank getNewRank() {
    return this.newRank;
  }

  /** Why it changes */
  public Cause getCause() {
    return this.cause;
  }

  /** Whether the god rises */
  public boolean isRise() {
    return this.newRank.compareTo(this.oldRank) > 0;
  }

  /**
   * Whether cancelling it keeps the god's rank: only for a rank up and a change made through the
   * API
   */
  public boolean isCancellable() {
    return this.cause == Cause.RANK_UP || this.cause == Cause.API;
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
