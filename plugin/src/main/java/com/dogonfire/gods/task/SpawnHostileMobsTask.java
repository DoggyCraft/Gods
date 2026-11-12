package com.dogonfire.gods.task;

import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

import com.dogonfire.gods.GodsContext;

public class SpawnHostileMobsTask extends Task {
	/** How long a curse's bees stay angry: a minute */
	private static final int ANGER_TICKS = 20 * 60;

	private String godName;
	private EntityType mobType;
	private int numberOfMobs = 0;
	private Player player;

	public SpawnHostileMobsTask(GodsContext ctx, String god, Player p, EntityType entityType, int n) {
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
			// Mobs that are only angry when provoked (wolves, bees, zombified piglins) are provoked
			if (spawned instanceof Wolf wolf) {
				wolf.setAngry(true);
			} else if (spawned instanceof Bee bee) {
				bee.setAnger(ANGER_TICKS);
			} else if (spawned instanceof PigZombie pigZombie) {
				pigZombie.setAngry(true);
			}
			if (spawned instanceof Mob mob) {
				mob.setTarget(this.player);
			}
		}
	}
}
