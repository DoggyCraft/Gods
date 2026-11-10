package com.dogonfire.gods.tasks;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Spawns a holy mob 17 blocks from the player in the direction of the target, which jumps twice and then vanishes.
 */
public class TaskSpawnGuideMob extends Task {
	private EntityType mobType;
	private Location targetLocation;
	private Player player;
	private Entity spawnedMob = null;
	private int runs = 0;

	public TaskSpawnGuideMob(Player p, Location location, EntityType entityType) {
		this.targetLocation = location;
		this.player = p;
		this.mobType = entityType;
	}

	@Override
	public void run() {
		if (this.runs == 0) {
			this.runs++;
			spawn();
			return;
		}

		if (this.spawnedMob == null || !this.spawnedMob.isValid()) {
			return;
		}

		if (this.runs < 3) {
			this.spawnedMob.setVelocity(new Vector(0.0D, 0.5D, 0.0D));
			getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), this, 40L);
		} else {
			this.spawnedMob.remove();
		}
		this.runs++;
	}

	private void spawn() {
		World world = this.player.getWorld();
		if (!this.player.isOnline() || this.targetLocation == null || this.targetLocation.getWorld() != world || !this.mobType.isSpawnable()) {
			return;
		}

		Vector dir = this.targetLocation.toVector().subtract(this.player.getLocation().toVector());
		dir.setY(0);
		if (dir.lengthSquared() < 1.0D) {
			return;
		}
		dir.normalize().multiply(17);

		int x = this.player.getLocation().getBlockX() + dir.getBlockX();
		int z = this.player.getLocation().getBlockZ() + dir.getBlockZ();
		Location location = new Location(world, x + 0.5D, world.getHighestBlockYAt(x, z) + 1, z + 0.5D);

		this.spawnedMob = world.spawnEntity(location, this.mobType);
		if (this.spawnedMob instanceof Mob mob) {
			mob.setTarget(this.player);
		}

		getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), this, 40L);
	}
}
