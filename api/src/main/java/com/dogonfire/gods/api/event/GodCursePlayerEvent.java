package com.dogonfire.gods.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * A god is about to curse a player: an angry god punishes a believer, a believer broke a
 * commandment, someone attacks in its holy land, it curses the player a priest asked for, or
 * another plugin had it curse them. Cancelling it does nothing to the player.
 */
public class GodCursePlayerEvent extends GodEvent implements Cancellable {
  private static final HandlerList HANDLERS = new HandlerList();

  /** What the curse is */
  public enum Curse {
    /** A bad potion effect for a while (blindness, nausea, poison, slowness or mining fatigue) */
    EFFECT,
    /** Lightning strikes the player */
    LIGHTNING,
    /** Hostile mobs appear around the player */
    MOBS
  }

  private final Player player;
  private final Curse curse;
  private boolean cancelled;

  public GodCursePlayerEvent(String godName, Player player, Curse curse) {
    super(godName);
    this.player = player;
    this.curse = curse;
  }

  /** The player the god curses */
  public Player getPlayer() {
    return this.player;
  }

  /** What the curse is */
  public Curse getCurse() {
    return this.curse;
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
