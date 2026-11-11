package com.dogonfire.gods.api;

/** The outcome of a {@link GodsApi} action. Each action says which of these it can return. */
public enum ActionResult
{
	/** It was done (an effect or item that takes a moment, such as a dropped item, follows a tick or two later) */
	SUCCESS,
	/** A listener cancelled the event the action fires, so nothing changed */
	CANCELLED,
	/** There is no god of that name */
	NO_SUCH_GOD,
	/** The player doesn't believe in the god */
	NOT_BELIEVER,
	/** The feature is switched off in Gods' config (blessings, curses, holy artifacts, quests) */
	FEATURE_DISABLED,
	/** Not possible right now: a cooldown, the god has (no) quest, the player has one already, and so on */
	NOT_POSSIBLE;

	/** True for {@link #SUCCESS} */
	public boolean isSuccess()
	{
		return this == SUCCESS;
	}
}
