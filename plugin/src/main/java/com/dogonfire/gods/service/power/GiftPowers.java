package com.dogonfire.gods.service.power;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.task.BoostKnowledgeTask;
import com.dogonfire.gods.task.DrunkTask;
import com.dogonfire.gods.task.FireworkTask;
import com.dogonfire.gods.task.HealRadiusTask;

/** The powers that do something for the player or those around: knowledge, healing, fireworks, drink and teleporting */
public final class GiftPowers
{
	private final GodsContext ctx;

	public GiftPowers(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	/** KNOWLEDGE: powerValue levels of xp */
	public void boostKnowledge(Player player, int powerValue)
	{
		ctx.plugin().logDebug("Boosting " + powerValue + " xp of knowledge");

		ctx.plugin().getServer().getScheduler().scheduleSyncDelayedTask(ctx.plugin(), new BoostKnowledgeTask(ctx, player, powerValue), 1L);
	}

	/** HEALING: heals up to powerValue other players around */
	public void healing(Player player, int powerValue)
	{
		ctx.plugin().logDebug("Healing up to " + powerValue + " creatures");

		ctx.plugin().getServer().getScheduler().scheduleSyncDelayedTask(ctx.plugin(), new HealRadiusTask(ctx, player, powerValue), 1L);
	}

	/** FIREWORK: powerValue rockets, then fewer in later rounds */
	public void shootFirework(Player player, int powerValue)
	{
		ctx.plugin().logDebug("Starting fireworks with " + powerValue + " rockets");

		ctx.plugin().getServer().getScheduler().scheduleSyncDelayedTask(ctx.plugin(), new FireworkTask(ctx, player, powerValue), 1L);
	}

	/** DRUNK: nausea for powerValue seconds */
	public void drunk(Player player, int powerValue)
	{
		ctx.plugin().logDebug("Getting " + player.getName() + " drunk for " + powerValue + " seconds");

		ctx.plugin().getServer().getScheduler().scheduleSyncDelayedTask(ctx.plugin(), new DrunkTask(ctx, player, powerValue), 1L);
	}

	/** TELEPORT: up to 100 blocks per power point where the player looks, stopping before the first solid block */
	public void teleport(Player player, int powerStrength)
	{
		int distance = 100 * powerStrength;

		World world = player.getWorld();
		Location start = player.getLocation();
		start.setY(start.getY() + 1.6D);

		Block lastSafe = world.getBlockAt(start);

		BlockIterator bi = new BlockIterator(player, distance);
		while (bi.hasNext())
		{
			Block block = bi.next();
			if ((block.getType().isSolid()) && (block.getType() != Material.AIR))
			{
				break;
			}
			lastSafe = block;
		}
		Location newLoc = lastSafe.getLocation();
		newLoc.setPitch(start.getPitch());
		newLoc.setYaw(start.getYaw());
		player.teleport(newLoc);
		world.playEffect(newLoc, Effect.ENDER_SIGNAL, 0);
		world.playSound(newLoc, Sound.ENTITY_ENDERMAN_AMBIENT, 1.0F, 0.3F);
	}
}
