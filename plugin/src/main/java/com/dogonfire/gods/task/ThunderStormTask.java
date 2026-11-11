package com.dogonfire.gods.task;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.util.NearbyEntities;

public class ThunderStormTask extends Task {
	private long stopTime;
	private Player player;
	private Random random = new Random();

	public ThunderStormTask(GodsContext ctx, Player player, long stopTime) {
		super(ctx);
		this.stopTime = stopTime;
		this.player = player;
	}

	@Override
	public void run() {
		if (!this.player.isOnline() || System.currentTimeMillis() >= this.stopTime) {
			this.player.getWorld().setStorm(false);
			return;
		}

		this.player.getWorld().setStorm(true);

		Entity[] entities = NearbyEntities.living(this.player.getLocation(), 20.0D);

		if (entities.length > 0) {
			Entity targetEntity = entities[this.random.nextInt(entities.length)];
			if (targetEntity != this.player) {
				Location strikeLocation = targetEntity.getWorld().getHighestBlockAt(targetEntity.getLocation()).getLocation();
				targetEntity.getWorld().strikeLightning(strikeLocation);
			}
		}

		getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), this, 20 + this.random.nextInt(100));
	}
}
