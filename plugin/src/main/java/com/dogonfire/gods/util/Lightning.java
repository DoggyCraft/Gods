package com.dogonfire.gods.util;

import org.bukkit.Location;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;

/**
 * Lightning that only hurts whom the gods mean to hurt. Real lightning sets fires (in holy land too) and turns villagers
 * into witches, pigs into zombified piglins and creepers into charged creepers; this is lightning that only looks and
 * sounds like it, plus lightning damage to the target. The damage comes from the bolt, as real lightning's does, so
 * holy land and neutral land protect against it as they did.
 */
public final class Lightning
{
	/** What a bolt of real lightning does to a creature it hits */
	public static final double LIGHTNING_DAMAGE = 5.0D;

	private Lightning()
	{
	}

	/** A bolt at {@code location} that only looks like lightning; returns it for {@link #hurt} */
	public static LightningStrike strike(Location location)
	{
		return location.getWorld().strikeLightningEffect(location);
	}

	/** Lightning damage from the bolt to {@code target} */
	public static void hurt(LivingEntity target, LightningStrike bolt, double damage)
	{
		target.damage(damage, DamageSource.builder(DamageType.LIGHTNING_BOLT).withDirectEntity(bolt).withDamageLocation(bolt.getLocation()).build());
	}

	/** A bolt that hits {@code target} with {@code damage} */
	public static void strike(LivingEntity target, double damage)
	{
		hurt(target, strike(target.getLocation()), damage);
	}
}
