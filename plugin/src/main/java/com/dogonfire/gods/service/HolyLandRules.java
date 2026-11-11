package com.dogonfire.gods.service;

import java.util.EnumSet;
import java.util.Set;

import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

/** What holy land and neutral land keep out, without a server so it can be tested */
public final class HolyLandRules
{
	/**
	 * The ways the world itself spawns mobs. Holy land and neutral land only stop these: a mob a player hatches, breeds,
	 * builds, summons by command or that a plugin (Gods' own curses too) spawns is let in.
	 */
	private static final Set<SpawnReason> WORLD_SPAWNS = EnumSet.of(SpawnReason.NATURAL, SpawnReason.CHUNK_GEN, SpawnReason.SPAWNER, SpawnReason.TRIAL_SPAWNER, SpawnReason.JOCKEY, SpawnReason.MOUNT, SpawnReason.PATROL, SpawnReason.RAID, SpawnReason.REINFORCEMENTS, SpawnReason.VILLAGE_INVASION, SpawnReason.TRAP, SpawnReason.SILVERFISH_BLOCK, SpawnReason.NETHER_PORTAL);

	private HolyLandRules()
	{
	}

	/** Whether a mob spawned this way is one the world spawned by itself */
	public static boolean isWorldSpawn(SpawnReason reason)
	{
		return reason != null && WORLD_SPAWNS.contains(reason);
	}
}
