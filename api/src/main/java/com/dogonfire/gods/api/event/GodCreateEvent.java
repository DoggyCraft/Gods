package com.dogonfire.gods.api.event;

import com.dogonfire.gods.api.GodGender;
import com.dogonfire.gods.api.GodType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * A player is about to create a god by praying at an altar for a god nobody prayed to before. Its
 * type and gender come from the altar. Cancelling it creates no god (and the player doesn't pray).
 */
public class GodCreateEvent extends GodEvent implements Cancellable {
  private static final HandlerList HANDLERS = new HandlerList();

  private final Player creator;
  private final GodType type;
  private final GodGender gender;
  private final Location location;
  private boolean cancelled;

  /**
   * @param godName the new god's name
   * @param creator the player who prays to it first
   * @param type its divine force, from the altar block
   * @param gender its gender, from the altar
   * @param location where it is created (where the creator stands at its altar, which becomes its
   *     main altar and home)
   */
  public GodCreateEvent(
      String godName, Player creator, GodType type, GodGender gender, Location location) {
    super(godName);
    this.creator = creator;
    this.type = type;
    this.gender = gender;
    this.location = location.clone();
  }

  /** The player who prays to the god first */
  public Player getCreator() {
    return this.creator;
  }

  /** The god's divine force, from the altar block */
  public GodType getType() {
    return this.type;
  }

  /** The god's gender, from the altar */
  public GodGender getGender() {
    return this.gender;
  }

  /**
   * Where the god is created: where the creator stands at its altar, which becomes its main altar
   * and home (a copy)
   */
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
