package com.dogonfire.gods.api.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * A god is about to claim free land as its holy land, because a believer built an altar or prayed there. Holy land is
 * claimed per 128x128 block region. Cancelling it leaves the land free.
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
	 * @param location the altar
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

	/** The altar, in the region that is claimed (a copy) */
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
