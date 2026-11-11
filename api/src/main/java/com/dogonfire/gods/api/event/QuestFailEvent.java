package com.dogonfire.gods.api.event;

import org.bukkit.event.HandlerList;

import com.dogonfire.gods.api.QuestInfo;

/**
 * The god's quest failed: its time ran out (or another plugin failed it). The god is angered and the quest is removed
 * right after this event.
 */
public class QuestFailEvent extends QuestEvent
{
	private static final HandlerList HANDLERS = new HandlerList();

	public QuestFailEvent(QuestInfo quest)
	{
		super(quest);
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
