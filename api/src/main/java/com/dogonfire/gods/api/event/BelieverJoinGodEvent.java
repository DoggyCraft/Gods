package com.dogonfire.gods.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * A player is about to become a god's believer, leaving the god they believed in (if any; that god then gets a
 * {@link BelieverLeaveGodEvent} with {@link BelieverLeaveGodEvent.Reason#CHANGED_GOD}). Cancelling it keeps them where
 * they are.
 */
public class BelieverJoinGodEvent extends GodEvent implements Cancellable
{
	private static final HandlerList HANDLERS = new HandlerList();

	/** How the player comes to the god */
	public enum Reason
	{
		/** They prayed at the god's altar */
		ALTAR,
		/** They accepted a priest's (or another plugin's) invitation with {@code /g yes} */
		INVITATION
	}

	private final Player player;
	private final String previousGodName;
	private final Reason reason;
	private boolean cancelled;

	/**
	 * @param godName         the god the player joins
	 * @param player          the player
	 * @param previousGodName the god they believe in now, or null
	 * @param reason          how they come to the god
	 */
	public BelieverJoinGodEvent(String godName, Player player, String previousGodName, Reason reason)
	{
		super(godName);
		this.player = player;
		this.previousGodName = previousGodName;
		this.reason = reason;
	}

	/** The player who joins the god */
	public Player getPlayer()
	{
		return this.player;
	}

	/** The god the player leaves for this one, or null if they believe in no god */
	public String getPreviousGodName()
	{
		return this.previousGodName;
	}

	/** How the player comes to the god */
	public Reason getReason()
	{
		return this.reason;
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
