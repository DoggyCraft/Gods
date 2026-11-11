package com.dogonfire.gods.command;

import java.util.UUID;

import com.dogonfire.gods.GodsContext;

/** What the router needs to know about a player to check believer-only and priest-only commands */
public interface CommandChecks
{
	/** The god the player believes in, or null */
	String godOf(UUID playerId);

	boolean isPriest(UUID playerId);

	static CommandChecks of(GodsContext ctx)
	{
		return new CommandChecks()
		{
			@Override
			public String godOf(UUID playerId)
			{
				return ctx.believerService().getGodForBeliever(playerId);
			}

			@Override
			public boolean isPriest(UUID playerId)
			{
				return ctx.priestService().isPriest(playerId);
			}
		};
	}
}
