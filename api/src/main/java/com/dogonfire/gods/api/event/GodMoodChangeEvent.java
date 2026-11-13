package com.dogonfire.gods.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * A god's mood is about to change: believers pleased or angered it, a quest ended, time passed
 * (moods fall off while believers are online), or another plugin changed it. Moods go from -100 to
 * 100; see {@link com.dogonfire.gods.api.GodMood}. Cancelling it keeps the mood as it is.
 */
public class GodMoodChangeEvent extends GodEvent implements Cancellable {
  private static final HandlerList HANDLERS = new HandlerList();

  private static final float MIN_MOOD = -100.0F;
  private static final float MAX_MOOD = 100.0F;

  private final float oldMood;
  private float newMood;
  private boolean cancelled;

  public GodMoodChangeEvent(String godName, float oldMood, float newMood) {
    super(godName);
    this.oldMood = oldMood;
    this.newMood = newMood;
  }

  /** The mood before the change, -100 to 100 */
  public float getOldMood() {
    return this.oldMood;
  }

  /** The mood after the change, -100 to 100 */
  public float getNewMood() {
    return this.newMood;
  }

  /**
   * Changes the mood the god gets; it is kept within -100 to 100 (so an infinity becomes -100 or
   * 100)
   *
   * @throws IllegalArgumentException if {@code newMood} is NaN
   */
  public void setNewMood(float newMood) {
    if (Float.isNaN(newMood)) {
      throw new IllegalArgumentException("The new mood of " + getGodName() + " is NaN");
    }
    this.newMood = Math.max(MIN_MOOD, Math.min(MAX_MOOD, newMood));
  }

  @Override
  public boolean isCancelled() {
    return this.cancelled;
  }

  @Override
  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }

  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }
}
