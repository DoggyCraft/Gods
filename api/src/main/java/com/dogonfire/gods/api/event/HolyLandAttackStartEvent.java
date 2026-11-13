package com.dogonfire.gods.api.event;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * A god is about to attack another god's holy land, which a priest started with {@code /g
 * startattack}. The gods are at war; the side whose believers kill more there wins, and if the
 * attackers win the land becomes theirs. {@link #getGodName()} is the attacking god. Cancelling it
 * starts no attack.
 */
public class HolyLandAttackStartEvent extends GodEvent implements Cancellable {
  private static final HandlerList HANDLERS = new HandlerList();

  private final String defendingGodName;
  private final Location location;
  private boolean cancelled;

  /**
   * @param godName the attacking god
   * @param defendingGodName the god whose holy land it is
   * @param location where the attack starts
   */
  public HolyLandAttackStartEvent(String godName, String defendingGodName, Location location) {
    super(godName);
    this.defendingGodName = defendingGodName;
    this.location = location.clone();
  }

  /** The god whose holy land is attacked */
  public String getDefendingGodName() {
    return this.defendingGodName;
  }

  /** Where the attack starts (a copy) */
  public Location getLocation() {
    return this.location.clone();
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
