package com.dogonfire.gods.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.dogonfire.gods.model.Altar;
import com.dogonfire.gods.model.AltarKind;
import com.dogonfire.gods.model.BlockPos;

/**
 * How a scan of a loaded chunk brings the altar registry in line with the world (pure, tested). A scan only looks at
 * the chunk's tile entities: the wall signs tagged {@code gods:altar}. What the registry has there and the chunk
 * doesn't (the sign is gone, or no longer tagged) is removed; tagged signs it doesn't have are added; ones whose kind,
 * god or altar block differ are updated from the sign.
 */
public final class AltarScan
{
	/** At most this many queued chunks are scanned per tick, so loading a large area (or a reload) never lags */
	public static final int CHUNKS_PER_TICK = 8;

	private AltarScan()
	{
	}

	/** A tagged altar sign found in a chunk */
	public record Found(BlockPos sign, BlockPos block, AltarKind kind, String god)
	{
	}

	/**
	 * What to change
	 *
	 * @param removed the signs of registered altars that are no longer in the world
	 * @param added   tagged signs the registry doesn't have
	 * @param changed tagged signs the registry has with another kind, god or altar block
	 */
	public record Changes(List<BlockPos> removed, List<Found> added, List<Found> changed)
	{
		public boolean isEmpty()
		{
			return this.removed.isEmpty() && this.added.isEmpty() && this.changed.isEmpty();
		}
	}

	/**
	 * @param registered the registered altars whose sign is in the scanned chunk
	 * @param found      the tagged altar signs in the chunk
	 */
	public static Changes reconcile(Collection<Altar> registered, Collection<Found> found)
	{
		Set<BlockPos> foundSigns = new HashSet<>();
		for (Found sign : found)
		{
			foundSigns.add(sign.sign());
		}

		List<BlockPos> removed = new ArrayList<>();
		Map<BlockPos, Altar> bySign = new HashMap<>();
		for (Altar altar : registered)
		{
			bySign.put(altar.sign(), altar);
			if (!foundSigns.contains(altar.sign()))
			{
				removed.add(altar.sign());
			}
		}

		List<Found> added = new ArrayList<>();
		List<Found> changed = new ArrayList<>();
		for (Found sign : found)
		{
			Altar altar = bySign.get(sign.sign());
			if (altar == null)
			{
				added.add(sign);
			}
			else if (!matches(altar, sign))
			{
				changed.add(sign);
			}
		}
		return new Changes(removed, added, changed);
	}

	/** Whether the registered altar is what the sign says */
	public static boolean matches(Altar altar, Found sign)
	{
		return altar.kind() == sign.kind() && Objects.equals(altar.god(), sign.god()) && altar.block().equals(sign.block());
	}
}
