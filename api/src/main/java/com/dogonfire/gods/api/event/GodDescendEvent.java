package com.dogonfire.gods.api.event;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * A god is about to descend to its main altar: its religion rose to the descended rank (Gods' config, {@code ranks.descended}:
 * a priest for long enough, enough believers and power), and the god will appear there as an NPC, with lightning, and
 * tell its believers. Cancelling it keeps the god from descending for now; Gods checks again on the god's next turn.
 */
public class GodDescendEvent extends GodEvent implements Cancellable
{
	private static final HandlerList HANDLERS = new HandlerList();

	private final Location	mainAltar;
	private boolean			cancelled;

	public GodDescendEvent(String godName, Location mainAltar)
	{
		super(godName);
		this.mainAltar = mainAltar;
	}

	/** The altar block of the god's main altar, where it appears; null if its world isn't loaded */
	public Location getMainAltar()
	{
		return this.mainAltar != null ? this.mainAltar.clone() : null;
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
