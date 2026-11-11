package com.dogonfire.gods.service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The lights that mark an altar when they stand on top of its block, without Bukkit so it can be tested. Any of them
 * will do; the light no longer decides the god's gender (the sign does). Newer lights are named, not referenced, so
 * Gods still runs on servers without them.
 */
public final class AltarLights
{
	/**
	 * The lights by Material name. Candles (any color, lit or not, so a candle blown out doesn't break the altar) and
	 * candles on cakes come from the server's tags. Copper torches and lanterns (Minecraft 1.21.9) count where the
	 * server has them.
	 */
	public static final List<String> NAMES = List.of(
			"TORCH", "SOUL_TORCH", "REDSTONE_TORCH", "LANTERN", "SOUL_LANTERN",
			"COPPER_TORCH",
			"COPPER_LANTERN", "EXPOSED_COPPER_LANTERN", "WEATHERED_COPPER_LANTERN", "OXIDIZED_COPPER_LANTERN",
			"WAXED_COPPER_LANTERN", "WAXED_EXPOSED_COPPER_LANTERN", "WAXED_WEATHERED_COPPER_LANTERN", "WAXED_OXIDIZED_COPPER_LANTERN");

	private AltarLights()
	{
	}

	/**
	 * The lights this server has: {@link #NAMES} through {@code lookup} (null for a name the server doesn't have, which
	 * is passed to {@code missing}), and the {@code tagged} ones (the candles).
	 */
	public static <T> Set<T> resolve(Function<String, T> lookup, Collection<T> tagged, Consumer<String> missing)
	{
		Set<T> lights = new LinkedHashSet<>();
		for (String name : NAMES)
		{
			T light = lookup.apply(name);
			if (light == null)
			{
				missing.accept(name);
			}
			else
			{
				lights.add(light);
			}
		}
		lights.addAll(tagged);
		return Set.copyOf(lights);
	}
}
