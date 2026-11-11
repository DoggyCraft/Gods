package com.dogonfire.gods.task;

import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

/**
 * Speeds time up until day breaks, then keeps it day until stopTime (System.currentTimeMillis()).
 */
public class CallSunTask extends Task
{
	private static final long	DAY_END	= 12000L;

	private long				stopTime;
	private Player				player;
	private World				world;

	public CallSunTask(GodsContext ctx, Player player, long stopTime)
	{
		super(ctx);
		this.stopTime = stopTime;
		this.player = player;
		this.world = player.getWorld();
	}

	@Override
	public void run()
	{
		if (!this.player.isOnline() || System.currentTimeMillis() >= this.stopTime)
		{
			return;
		}

		long time = this.world.getFullTime() % 24000L;

		if (time >= DAY_END)
		{
			this.player.playSound(this.player.getLocation(), Sound.BLOCK_STONE_STEP, 1.0F, 0.1F);
			this.world.setFullTime(this.world.getFullTime() + Math.min(1000L, 24000L - time));
		}
		else if (time >= DAY_END - 1000L)
		{
			// Hold the day: turn the clock back before sunset
			this.world.setFullTime(this.world.getFullTime() - 1000L);
		}

		getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), this, 20L);
	}
}
