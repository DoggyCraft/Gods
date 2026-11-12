package com.dogonfire.gods.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import com.dogonfire.gods.model.ChunkKey;

/** What holy land and neutral land keep out, and which chunks an altar's land takes, without a server so it can be tested */
public final class HolyLandRules
{
	/**
	 * The ways the world itself spawns mobs. Holy land and neutral land only stop these: a mob a player hatches, breeds,
	 * builds, summons by command or that a plugin (Gods' own curses too) spawns is let in.
	 */
	private static final Set<SpawnReason> WORLD_SPAWNS = EnumSet.of(SpawnReason.NATURAL, SpawnReason.CHUNK_GEN, SpawnReason.SPAWNER, SpawnReason.TRIAL_SPAWNER, SpawnReason.JOCKEY, SpawnReason.MOUNT, SpawnReason.PATROL, SpawnReason.RAID, SpawnReason.REINFORCEMENTS, SpawnReason.VILLAGE_INVASION, SpawnReason.TRAP, SpawnReason.SILVERFISH_BLOCK, SpawnReason.NETHER_PORTAL);

	/** The radius of chunks /g setneutralland sets or clears around the player: 1 is 3x3 chunks */
	public static final int NEUTRAL_RADIUS_CHUNKS = 1;

	private HolyLandRules()
	{
	}

	/** Whether a mob spawned this way is one the world spawned by itself */
	public static boolean isWorldSpawn(SpawnReason reason)
	{
		return reason != null && WORLD_SPAWNS.contains(reason);
	}

	/**
	 * The chunks an altar's land starts with, without duplicates: the altar block's (first: the land's center), its
	 * sign's and that of the spot in front of the sign where the god's figure stands ({@code MainAltarRules.standingSpot}).
	 * One chunk, or up to four at a chunk corner.
	 *
	 * @param world the world key
	 */
	public static List<ChunkKey> footprint(String world, int blockX, int blockZ, int signX, int signZ)
	{
		Set<ChunkKey> chunks = new LinkedHashSet<ChunkKey>();
		chunks.add(chunk(world, blockX, blockZ));
		chunks.add(chunk(world, signX, signZ));
		chunks.add(chunk(world, 2 * signX - blockX, 2 * signZ - blockZ));
		return new ArrayList<ChunkKey>(chunks);
	}

	/** The square of chunks {@code radius} chunks around {@code center} ((2r+1)^2 of them), the center first; 0 or less is the center alone */
	public static List<ChunkKey> square(ChunkKey center, int radius)
	{
		List<ChunkKey> chunks = new ArrayList<ChunkKey>();
		chunks.add(center);
		for (int dx = -radius; dx <= radius; dx++)
		{
			for (int dz = -radius; dz <= radius; dz++)
			{
				if (dx != 0 || dz != 0)
				{
					chunks.add(center.offset(dx, dz));
				}
			}
		}
		return chunks;
	}

	/** The chunks of {@code wanted} that are free, in order and without duplicates */
	public static List<ChunkKey> claimable(Collection<ChunkKey> wanted, Predicate<ChunkKey> free)
	{
		Set<ChunkKey> chunks = new LinkedHashSet<ChunkKey>();
		for (ChunkKey chunk : wanted)
		{
			if (free.test(chunk))
			{
				chunks.add(chunk);
			}
		}
		return new ArrayList<ChunkKey>(chunks);
	}

	/**
	 * Whether a holy land goes with a praying altar of {@code altarGod} that was removed from it (its altar block was in
	 * the land): only when it is that god's land and no other praying altar of the god has its altar block in the
	 * land's chunks. A land belongs to its altars; one without a known altar is only removed when it is abandoned.
	 *
	 * @param otherAltarBlocks the chunks of the altar blocks of the god's other registered praying altars
	 */
	public static boolean goesWithAltar(String landGod, String altarGod, Set<ChunkKey> landChunks, Collection<ChunkKey> otherAltarBlocks)
	{
		if (altarGod == null || !altarGod.equals(landGod))
		{
			return false;
		}
		for (ChunkKey chunk : otherAltarBlocks)
		{
			if (landChunks.contains(chunk))
			{
				return false;
			}
		}
		return true;
	}

	private static ChunkKey chunk(String world, int blockX, int blockZ)
	{
		return new ChunkKey(world, blockX >> ChunkKey.CHUNK_SHIFT, blockZ >> ChunkKey.CHUNK_SHIFT);
	}
}
