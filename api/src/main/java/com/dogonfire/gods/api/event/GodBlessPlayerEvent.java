package com.dogonfire.gods.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * A god is about to bless a player: a believer pleased it, prayed for it, it blesses the player a
 * priest asked for, or another plugin had it bless them. Cancelling it gives nothing.
 */
public class GodBlessPlayerEvent extends GodEvent implements Cancellable {
  private static final HandlerList HANDLERS = new HandlerList();

  /** What the blessing is */
  public enum Blessing {
    /** A good potion effect for a while (haste, healing, regeneration, speed or strength) */
    EFFECT,
    /** The player is healed */
    HEAL,
    /** An item drops in front of the player (food, a tool, a quest reward, ...) */
    ITEM,
    /** A holy artifact drops in front of the player */
    HOLY_ARTIFACT
  }

  private final Player player;
  private final Blessing blessing;
  private boolean cancelled;

  public GodBlessPlayerEvent(String godName, Player player, Blessing blessing) {
    super(godName);
    this.player = player;
    this.blessing = blessing;
  }

  /** The player the god blesses */
  public Player getPlayer() {
    return this.player;
  }

  /** What the blessing is */
  public Blessing getBlessing() {
    return this.blessing;
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
