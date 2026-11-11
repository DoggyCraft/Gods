package com.dogonfire.gods.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import com.dogonfire.gods.api.QuestInfo;

/**
 * A god has made a new quest and is about to give it to its believers (they haven't been told yet). It comes from the
 * god's own turns, a believer praying for one with {@code /g prayfor quest}, or another plugin. Cancelling it takes
 * the quest back (and its pilgrimage chest), as if the god had found no quest to give.
 */
public class QuestStartEvent extends QuestEvent implements Cancellable
{
	private static final HandlerList HANDLERS = new HandlerList();

	private boolean cancelled;

	public QuestStartEvent(QuestInfo quest)
	{
		super(quest);
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
