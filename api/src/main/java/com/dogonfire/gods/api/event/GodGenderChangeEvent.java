package com.dogonfire.gods.api.event;

import com.dogonfire.gods.api.GodGender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * A god's gender is about to change: a priest used {@code /g gender}, or another plugin called
 * {@link com.dogonfire.gods.api.GodsApi#setGender(String, GodGender)}. Cancelling it keeps the
 * gender as it is (and starts no cooldown).
 */
public class GodGenderChangeEvent extends GodEvent implements Cancellable {
  private static final HandlerList HANDLERS = new HandlerList();

  private final GodGender oldGender;
  private final GodGender newGender;
  private boolean cancelled;

  public GodGenderChangeEvent(String godName, GodGender oldGender, GodGender newGender) {
    super(godName);
    this.oldGender = oldGender;
    this.newGender = newGender;
  }

  /** The god's gender now ({@link GodGender#NONE} for a god from before genders could be chosen) */
  public GodGender getOldGender() {
    return this.oldGender;
  }

  /** The gender the god is getting */
  public GodGender getNewGender() {
    return this.newGender;
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
