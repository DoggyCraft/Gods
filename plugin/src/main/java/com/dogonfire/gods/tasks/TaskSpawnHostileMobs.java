package com.dogonfire.gods.tasks;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

public class TaskSpawnHostileMobs extends Task {
	private String godName;
	private EntityType mobType;
	private int numberOfMobs = 0;
	private Player player;

	public TaskSpawnHostileMobs(GodsContext ctx, String god, Player p, EntityType entityType, int n) {
		super(ctx);
		this.godName = god;
		this.numberOfMobs = n;
		this.player = p;
		this.mobType = entityType;
	}

	@Override
	public void run() {
		if (!this.player.isOnline() || this.mobType == null || !this.mobType.isSpawnable()) {
			return;
		}

		getPlugin().logDebug(this.godName + " spawns " + this.numberOfMobs + " " + this.mobType.name() + " at " + this.player.getName());

		for (int i = 0; i < this.numberOfMobs; i++) {
			Entity spawned = this.player.getWorld().spawnEntity(this.player.getLocation(), this.mobType);
			if (spawned instanceof Mob mob) {
				mob.setTarget(this.player);
			}
		}
	}
}
