package com.dogonfire.gods.tasks;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.dogonfire.gods.managers.HolyPowerManager;

public class ThunderStormTask extends Task {
	private long stopTime;
	private Player player;
	private Random random = new Random();

	public ThunderStormTask(Player player, long stopTime) {
		this.stopTime = stopTime;
		this.player = player;
	}

	@Override
	public void run() {
		this.player.getWorld().setStorm(true);

		Entity[] entities = HolyPowerManager.instance().getNearbyLivingEntities(this.player.getLocation(), 20.0D);

		Entity targetEntity = entities[this.random.nextInt(entities.length)];
		if (targetEntity != this.player) {
			Location strikeLocation = targetEntity.getLocation();
			strikeLocation = strikeLocation.getWorld().getHighestBlockAt(strikeLocation).getLocation();
			this.player.getWorld().strikeLightning(strikeLocation);
		}
		if (System.currentTimeMillis() < this.stopTime) {
			getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new ThunderStormTask(this.player, this.stopTime), 20 + this.random.nextInt(100));
		} else {
			this.player.getWorld().setStorm(false);
		}
	}
}

/*
 * Location: C:\temp\Gods.jar
 * 
 * Qualified Name: com.dogonfire.gods.tasks.ThunderStormTask
 * 
 * JD-Core Version: 0.7.0.1
 */