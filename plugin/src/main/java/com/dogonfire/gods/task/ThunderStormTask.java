package com.dogonfire.gods.task;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.util.Lightning;
import com.dogonfire.gods.util.NearbyEntities;

/**
 * LIGHTNING_STORM: a storm in the player's world until stopTime (System.currentTimeMillis()) or until they leave that
 * world, with a bolt now and then on a creature near them. The bolts set no fires and change no creatures
 * ({@link Lightning}).
 */
public class ThunderStormTask extends Task
{
	private long	stopTime;
	private Player	player;
	private World	world;
	private Random	random	= new Random();

	public ThunderStormTask(GodsContext ctx, Player player, long stopTime)
	{
		super(ctx);
		this.stopTime = stopTime;
		this.player = player;
		this.world = player.getWorld();
	}

	@Override
	public void run()
	{
		// The storm is in the world the player called it in; it ends when they leave it
		if (!this.player.isOnline() || !this.world.equals(this.player.getWorld()) || System.currentTimeMillis() >= this.stopTime)
		{
			this.world.setStorm(false);
			return;
		}

		this.world.setStorm(true);

		Entity[] entities = NearbyEntities.living(this.player.getLocation(), 20.0D);

		if (entities.length > 0)
		{
			Entity targetEntity = entities[this.random.nextInt(entities.length)];
			if (targetEntity != this.player)
			{
				// The bolt comes down on the highest block; it hits the creature only if that is where it stands
				Location strikeLocation = targetEntity.getWorld().getHighestBlockAt(targetEntity.getLocation()).getLocation().add(0.5D, 1.0D, 0.5D);
				LightningStrike bolt = Lightning.strike(strikeLocation);
				if (targetEntity.getLocation().getY() >= strikeLocation.getY() - 1.0D)
				{
					Lightning.hurt((LivingEntity) targetEntity, bolt, Lightning.LIGHTNING_DAMAGE);
				}
			}
		}

		getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), this, 20 + this.random.nextInt(100));
	}
}
