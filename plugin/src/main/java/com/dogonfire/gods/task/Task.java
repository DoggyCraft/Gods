package com.dogonfire.gods.task;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.GodsContext;

/** A short task run once (or rescheduling itself until it is done) through the Bukkit scheduler */
public abstract class Task implements Runnable
{
	protected final GodsContext ctx;

	protected Task(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	protected Gods getPlugin()
	{
		return this.ctx.plugin();
	}

	/**
	 * Where a god's gift appears: in the air (cave air too) 4 blocks in front of the player and 2 up, or, where there is
	 * no air (a wall, a low cave ceiling, water), at the player
	 */
	protected static Location giftSpot(Player player)
	{
		Vector dir = player.getLocation().getDirection();
		dir.setY(0);
		Location inFront = player.getLocation().add(dir.multiply(4)).add(0.0D, 2.0D, 0.0D);
		if (inFront.getBlock().getType().isAir())
		{
			return inFront;
		}
		return player.getLocation().add(0.0D, 1.0D, 0.0D);
	}

	@Override
	public abstract void run();
}
