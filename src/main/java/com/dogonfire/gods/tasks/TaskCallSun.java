package com.dogonfire.gods.tasks;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class TaskCallSun extends Task
{
	private long	stopTime;
	private Player	player;

	public TaskCallSun(Player player, long stopTime)
	{
		this.stopTime = stopTime;
		this.player = player;
	}

	@Override
	public void run()
	{
		long time = this.player.getWorld().getFullTime() % 24000L;
		
		if ((time > 13000L) || (time < 1000L))
		{
			this.player.playSound(this.player.getLocation(), Sound.BLOCK_STONE_STEP, 1.0F, 0.1F);
			this.player.getWorld().setFullTime(this.player.getWorld().getFullTime() + 1000L);
			getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new TaskCallSun(this.player, this.stopTime), 20L);
		}
	}
}