package com.dogonfire.gods.integration.npc;

import org.bukkit.entity.Entity;

/**
 * Something that can make a god's NPC: the vanilla mannequin ({@link MannequinNpcs}), Citizens or FancyNpcs. Chosen by
 * {@link NpcProviders} from config.yml's {@code npc.provider}, created by {@code GodNpcService.start()} and closed by
 * its {@code stop()}. NPCs are never saved: every one is made again from Gods' data after a restart or reload.
 * Main thread only.
 */
public interface GodNpcProvider
{
	/** For the log: "the built-in mannequin", "Citizens", "FancyNpcs" */
	String name();

	/** Whether NPCs can be made now (a plugin may still be loading its own); the service tries again later */
	default boolean ready()
	{
		return true;
	}

	/**
	 * Removes NPCs of an earlier run that are still in the world (after a crash, or a reload that didn't stop cleanly);
	 * called once when the provider starts
	 */
	default void removeStrays()
	{
	}

	/**
	 * Makes the god's NPC look like the spec says: spawns it, moves it, or changes its name or skin. Does nothing if it
	 * already does. Returns false if it couldn't (the spot was refused, the other plugin failed); the service then
	 * tries again on the god's next refresh.
	 */
	boolean show(NpcSpec spec);

	/** Removes the god's NPC, if it has one */
	void remove(String god);

	/** Removes every NPC and lets go of the other plugin (on reload and disable) */
	void close();

	/** The god whose NPC the entity is, or null if it isn't one of Gods' NPCs */
	String godOf(Entity entity);

	/** The entity of the god's NPC, or null (none, or a provider whose NPCs have no server-side entity) */
	Entity entity(String god);

	/** About once a second: lets NPCs that don't do it themselves turn toward players close by */
	default void tick()
	{
	}
}
