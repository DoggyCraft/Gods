package com.dogonfire.gods.util;

import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/** Entities around a spot, for the holy powers and their tasks */
public final class NearbyEntities
{
	private NearbyEntities()
	{
	}

	/** Entities within radius of l, except those standing in the block at l itself */
	public static Entity[] all(Location l, double radius)
	{
		return find(l, radius, e -> true);
	}

	/** Living entities within radius of l, except those standing in the block at l itself */
	public static Entity[] living(Location l, double radius)
	{
		return find(l, radius, e -> e instanceof LivingEntity);
	}

	private static Entity[] find(Location center, double radius, Predicate<Entity> filter)
	{
		World world = center.getWorld();
		if (world == null)
		{
			return new Entity[0];
		}
		double radiusSquared = radius * radius;
		return world.getNearbyEntities(center, radius, radius, radius, e -> filter.test(e) && e.getLocation().distanceSquared(center) <= radiusSquared && !isSameBlock(e.getLocation(), center)).toArray(new Entity[0]);
	}

	private static boolean isSameBlock(Location a, Location b)
	{
		return a.getBlockX() == b.getBlockX() && a.getBlockY() == b.getBlockY() && a.getBlockZ() == b.getBlockZ();
	}
}
