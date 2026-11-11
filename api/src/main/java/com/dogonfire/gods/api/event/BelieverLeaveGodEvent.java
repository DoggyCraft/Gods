package com.dogonfire.gods.api.event;

import java.util.UUID;

import org.bukkit.event.HandlerList;

/** A player stopped believing in a god. It can't be cancelled: they have already left. */
public class BelieverLeaveGodEvent extends GodEvent
{
	private static final HandlerList HANDLERS = new HandlerList();

	/** Why the player left the god */
	public enum Reason
	{
		/** They left with {@code /g leave} */
		LEFT,
		/** They joined another god ({@link BelieverJoinGodEvent}) */
		CHANGED_GOD,
		/** A priest kicked them out with {@code /g kick} */
		KICKED,
		/** They died, and the server has believers leave their god when they die */
		DIED,
		/** They didn't pray for too long, so the god forgot them */
		INACTIVE
	}

	private final UUID playerId;
	private final Reason reason;

	/**
	 * @param godName  the god the player left
	 * @param playerId the player, who may be offline
	 * @param reason   why they left
	 */
	public BelieverLeaveGodEvent(String godName, UUID playerId, Reason reason)
	{
		super(godName);
		this.playerId = playerId;
		this.reason = reason;
	}

	/** The player who left; they may be offline */
	public UUID getPlayerId()
	{
		return this.playerId;
	}

	/** Why the player left */
	public Reason getReason()
	{
		return this.reason;
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
