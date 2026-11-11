package com.dogonfire.gods.service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Who dropped which item, so it counts as their sacrifice when it burns (in fire or lava, both start with the item
 * catching fire). An item counts once: {@link #take} forgets it, so the fire burning it tick after tick doesn't make
 * more sacrifices of it. An item is forgotten {@value #LIFETIME_MILLIS} ms after it was dropped (when a dropped item
 * despawns) or when it is gone. Without Bukkit so it can be tested; main thread only.
 */
public final class DroppedItems
{
	/** How long a dropped item lies on the ground before it despawns (6000 ticks) */
	public static final long		LIFETIME_MILLIS	= 5L * 60L * 1000L;

	private record Drop(UUID playerId, long droppedAt)
	{
	}

	private final Map<UUID, Drop>	drops			= new HashMap<UUID, Drop>();

	public void add(UUID itemId, UUID playerId, long now)
	{
		this.drops.put(itemId, new Drop(playerId, now));
	}

	/** The player who dropped the item, forgetting it; null if no player did, it already counted or it expired */
	public UUID take(UUID itemId, long now)
	{
		Drop drop = this.drops.remove(itemId);
		if (drop == null || now - drop.droppedAt() > LIFETIME_MILLIS)
		{
			return null;
		}
		return drop.playerId();
	}

	/** Forgets the items that expired or are gone; returns how many */
	public int prune(long now, Predicate<UUID> gone)
	{
		int removed = 0;
		for (Iterator<Map.Entry<UUID, Drop>> it = this.drops.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry<UUID, Drop> entry = it.next();
			if (now - entry.getValue().droppedAt() > LIFETIME_MILLIS || gone.test(entry.getKey()))
			{
				it.remove();
				removed++;
			}
		}
		return removed;
	}

	public int size()
	{
		return this.drops.size();
	}

	public void clear()
	{
		this.drops.clear();
	}
}
