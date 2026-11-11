package com.dogonfire.gods.service.power;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.dogonfire.gods.util.NearbyEntities;

/** The powers that hit something: a fireball, an ice spike and an earthquake */
public final class AttackPowers
{
	private final Plugin	plugin;
	private final Random	random	= new Random();

	public AttackPowers(Plugin plugin)
	{
		this.plugin = plugin;
	}

	/** FIREBALL: a small fireball where the player looks */
	public void fireBall(Player player, int power)
	{
		player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1.0F, 1.0F);
		player.launchProjectile(SmallFireball.class);
	}

	/**
	 * EARTHQUAKE: a wave of breaking blocks runs ahead of the player (up to 10 steps) and throws up whatever it reaches
	 */
	public void earthQuake(final Player player, int value)
	{
		final Location location = player.getLocation().add(0.0D, -0.2D, 0.0D);
		final Vector direction = player.getLocation().getDirection();
		direction.setY(0);
		direction.normalize();
		BukkitRunnable task = new BukkitRunnable()
		{
			private int count = 0;

			@Override
			public void run()
			{
				Location above = location.clone().add(0.0D, 1.0D, 0.0D);
				if ((above.getBlock().getType().isSolid()) || (!location.getBlock().getType().isSolid()))
				{
					cancel();
					return;
				}
				Location temp = location.clone();
				for (int x = -2; x <= 2; x++)
				{
					for (int z = -2; z <= 2; z++)
					{
						temp.setX(x + location.getBlockX());
						temp.setZ(z + location.getBlockZ());
						Block block = temp.getBlock();
						playBlockBreakEffect(temp, block);
					}
				}
				Entity[] near = NearbyEntities.all(location, 1.5D);

				boolean hit = false;

				Random random = new Random();
				for (Entity e : near)
				{
					if (e != player)
					{
						hit = true;
						break;
					}
				}
				if (hit)
				{
					location.getWorld().createExplosion(location.getX(), location.getY(), location.getZ(), 10.0F, false, false);
					near = NearbyEntities.all(location, 2.5D);
					for (Entity e : near)
					{
						if (e != player)
						{
							e.setVelocity(new Vector(random.nextGaussian() / 4.0D, 1.0D + random.nextDouble() * 10.0D, random.nextGaussian() / 4.0D));
						}
					}
					cancel();
					return;
				}
				location.add(direction);
				if (this.count >= 10)
				{
					cancel();
				}
				this.count += 1;
			}
		};
		task.runTaskTimer(this.plugin, 0L, 3L);
	}

	/** ICE: an ice block flies where the player looks and encases what it hits in ice for a few seconds */
	public void ice(final Player player, int power)
	{
		player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0F, 0.1F);

		final FallingBlock block = player.getWorld().spawnFallingBlock(player.getLocation().add(0.0D, 1.8D, 0.0D), Material.ICE.createBlockData());
		block.setVelocity(player.getLocation().getDirection().multiply(2.0D));

		block.setDropItem(false);

		BukkitRunnable run = new BukkitRunnable()
		{
			@Override
			public void run()
			{
				boolean hit = false;
				World world = block.getWorld();
				Location bLoc = block.getLocation();
				Location loc;

				for (int x = -1; x < 2; x++)
				{
					for (int y = -1; y < 2; y++)
					{
						for (int z = -1; z < 2; z++)
						{
							loc = block.getLocation().add(x, y, z);
							if (world.getBlockAt(loc).getType() != Material.AIR)
							{
								Block b = world.getBlockAt(loc);
								if ((b.getType().isSolid()) && (overlaps(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), 1.0D, 1.0D, 1.0D, bLoc.getX() - 0.5D, bLoc.getY() - 0.5D, bLoc.getZ() - 0.5D, 1.0D, 1.0D, 1.0D)))
								{
									hit = true;
									break;
								}
							}
						}
					}
				}

				if (!hit)
				{
					List<Entity> entities = block.getNearbyEntities(1.0D, 1.0D, 1.0D);
					for (Entity e : entities)
					{
						if (e != player)
						{
							hit = true;
							break;
						}
					}
				}

				if (block.isDead() || hit)
				{
					block.remove();

					// A landed falling block turns into an ice block; remove only that
					Block landed = block.getLocation().getBlock();
					if (landed.getType() == Material.ICE)
					{
						landed.setType(Material.AIR);
					}

					cancel();

					encaseInIce(landed);
				}
			}
		};
		run.runTaskTimer(this.plugin, 0L, 1L);
	}

	// Encase the target in ice for a few seconds, then put back what was there
	private void encaseInIce(Block landed)
	{
		final List<Block> changedBlocks = new ArrayList<Block>();
		final Map<Block, BlockData> originalData = new HashMap<Block, BlockData>();

		for (int x = -1; x < 2; x++)
		{
			for (int y = -1; y < 3; y++)
			{
				for (int z = -1; z < 2; z++)
				{
					Block b = landed.getRelative(x, y, z);
					Material type = b.getType();

					if (type.isAir() || (Tag.REPLACEABLE.isTagged(type) && type != Material.LAVA))
					{
						originalData.put(b, b.getBlockData());
						changedBlocks.add(b);
						b.setType(Material.ICE, false);
					}
				}
			}
		}

		Collections.shuffle(changedBlocks);

		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				for (int i = 0; i < 4; i++)
				{
					if (changedBlocks.isEmpty())
					{
						cancel();
						return;
					}

					Block c = changedBlocks.remove(changedBlocks.size() - 1);
					if (c.getType() == Material.ICE)
					{
						playBlockBreakEffect(c.getLocation(), c);
						c.setBlockData(originalData.get(c), false);
					}
				}
			}
		}.runTaskTimer(this.plugin, 80 + this.random.nextInt(40), 3L);
	}

	// Whether two boxes (corner and size) touch
	private static boolean overlaps(double x1, double y1, double z1, double w1, double h1, double d1, double x2, double y2, double z2, double w2, double h2, double d2)
	{
		return !(x1 + w1 < x2 || x2 + w2 < x1 || y1 + h1 < y2 || y2 + h2 < y1 || z1 + d1 < z2 || z2 + d2 < z1);
	}

	// Block break particles and sound, as the removed Effect.STEP_SOUND did
	private static void playBlockBreakEffect(Location location, Block block)
	{
		if (block.getType().isAir())
		{
			return;
		}
		BlockData data = block.getBlockData();
		location.getWorld().spawnParticle(Particle.BLOCK, location.clone().add(0.5D, 0.5D, 0.5D), 30, 0.25D, 0.25D, 0.25D, data);
		location.getWorld().playSound(location, data.getSoundGroup().getBreakSound(), 1.0F, 1.0F);
	}
}
