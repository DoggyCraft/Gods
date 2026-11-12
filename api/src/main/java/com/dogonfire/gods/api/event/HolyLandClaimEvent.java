package com.dogonfire.gods.api.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * A god is about to claim a new holy land, because a believer built an altar or prayed at one whose chunk is free.
 * Each altar claims its own holy land: the chunks of the altar block, its sign and the spot in front of the sign, and
 * the square of chunks around the altar's chunk that the god's rank gives it (it grows as the god rises, without this
 * event). Cancelling it leaves the land free.
 */
public class HolyLandClaimEvent extends GodEvent implements Cancellable
{
	private static final HandlerList HANDLERS = new HandlerList();

	private final Player player;
	private final Location location;
	private boolean cancelled;

	/**
	 * @param godName  the god that claims the land
	 * @param player   the believer whose altar or prayer claims it
	 * @param location the altar block
	 */
	public HolyLandClaimEvent(String godName, Player player, Location location)
	{
		super(godName);
		this.player = player;
		this.location = location.clone();
	}

	/** The believer whose altar or prayer claims the land */
	public Player getPlayer()
	{
		return this.player;
	}

	/** The altar block, in the center chunk of the land that is claimed (a copy) */
	public Location getLocation()
	{
		return this.location.clone();
	}

	@Override
	public boolean isCancelled()
	{
		return this.cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled)
	{
		this.cancelled = cancelled;
	}

	@Override
	public HandlerList getHandlers()
	{
		return HANDLERS;
	}

	public static HandlerList getHandlerList()
	{
		return HANDLERS;
	}
}
