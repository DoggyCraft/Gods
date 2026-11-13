package com.dogonfire.gods.api.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * A player is about to pray at a god's altar. Praying makes them the god's believer (asking first
 * if they believe in another god) and creates the god if it doesn't exist yet ({@link
 * GodCreateEvent}); joining fires {@link BelieverJoinGodEvent}. Cancelling it stops the prayer.
 */
public class BelieverPrayEvent extends GodEvent implements Cancellable {
  private static final HandlerList HANDLERS = new HandlerList();

  private final Player player;
  private final Location altar;
  private boolean cancelled;

  /**
   * @param godName the god the altar is for, which may not exist yet
   * @param player the player who prays
   * @param altar the altar's sign
   */
  public BelieverPrayEvent(String godName, Player player, Location altar) {
    super(godName);
    this.player = player;
    this.altar = altar.clone();
  }

  /** The player who prays */
  public Player getPlayer() {
    return this.player;
  }

  /** Where the altar's sign is (a copy) */
  public Location getAltar() {
    return this.altar.clone();
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
