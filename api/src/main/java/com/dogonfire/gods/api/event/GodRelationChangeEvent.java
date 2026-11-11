package com.dogonfire.gods.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Two gods are about to become allies or go to war, or to end their alliance or war: a priest used {@code /g ally} or
 * {@code /g war}, or another plugin changed it. Relations go both ways, so the other god gets no event of its own.
 * Allying ends a war between the two and going to war ends an alliance. Cancelling it keeps things as they are.
 */
public class GodRelationChangeEvent extends GodEvent implements Cancellable
{
	private static final HandlerList HANDLERS = new HandlerList();

	/** A relation between two gods */
	public enum Relation
	{
		ALLIANCE, WAR
	}

	private final String otherGodName;
	private final Relation relation;
	private final boolean starting;
	private boolean cancelled;

	/**
	 * @param godName      the god whose priest (or another plugin) makes the change
	 * @param otherGodName the other god
	 * @param relation     the relation that starts or ends
	 * @param starting     true if it starts, false if it ends
	 */
	public GodRelationChangeEvent(String godName, String otherGodName, Relation relation, boolean starting)
	{
		super(godName);
		this.otherGodName = otherGodName;
		this.relation = relation;
		this.starting = starting;
	}

	/** The other god */
	public String getOtherGodName()
	{
		return this.otherGodName;
	}

	/** The relation that starts or ends */
	public Relation getRelation()
	{
		return this.relation;
	}

	/** True if the relation starts, false if it ends */
	public boolean isStarting()
	{
		return this.starting;
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
