package com.dogonfire.gods.api.event;

import org.bukkit.event.Event;

/** An event about a god. */
public abstract class GodEvent extends Event
{
	private final String godName;

	protected GodEvent(String godName)
	{
		this.godName = godName;
	}

	/** The god's name, as Gods writes it; {@link com.dogonfire.gods.api.GodsApi#getGod(String)} gives a snapshot */
	public String getGodName()
	{
		return this.godName;
	}
}
