package com.dogonfire.gods.api.event;

import java.util.UUID;
import org.bukkit.event.HandlerList;

/**
 * A priest is no longer one of the god's priests: they left the god, didn't pray for too long, or
 * another plugin removed them. It can't be cancelled: they are no priest any more.
 */
public class PriestRemoveEvent extends GodEvent {
  private static final HandlerList HANDLERS = new HandlerList();

  private final UUID playerId;

  public PriestRemoveEvent(String godName, UUID playerId) {
    super(godName);
    this.playerId = playerId;
  }

  /** The former priest; they may be offline */
  public UUID getPlayerId() {
    return this.playerId;
  }

  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }
}
