package com.dogonfire.gods.api.event;

import java.util.UUID;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * A believer is about to become one of their god's priests: they accepted the god's offer, an admin
 * used {@code /g setpriest}, or another plugin made them one. Cancelling it keeps them a plain
 * believer.
 */
public class PriestAssignEvent extends GodEvent implements Cancellable {
  private static final HandlerList HANDLERS = new HandlerList();

  private final UUID playerId;
  private boolean cancelled;

  public PriestAssignEvent(String godName, UUID playerId) {
    super(godName);
    this.playerId = playerId;
  }

  /** The believer who becomes a priest */
  public UUID getPlayerId() {
    return this.playerId;
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
