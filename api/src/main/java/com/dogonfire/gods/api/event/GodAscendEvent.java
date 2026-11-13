package com.dogonfire.gods.api.event;

import org.bukkit.event.HandlerList;

/**
 * A god that had descended to its main altar ascended again: it has no priest any more, or too few
 * believers for too long (Gods' config, {@code ranks}): it fell below the descended rank. Its NPC
 * is gone. It reports what happened and can't be cancelled; the god descends again when its
 * religion reaches the milestone once more ({@link GodDescendEvent}).
 */
public class GodAscendEvent extends GodEvent {
  private static final HandlerList HANDLERS = new HandlerList();

  public GodAscendEvent(String godName) {
    super(godName);
  }

  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }
}
