package com.dogonfire.gods.managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Tameable;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.task.BoostKnowledgeTask;
import com.dogonfire.gods.task.CallMoonTask;
import com.dogonfire.gods.task.CallSunTask;
import com.dogonfire.gods.task.DrunkTask;
import com.dogonfire.gods.task.FireworkTask;
import com.dogonfire.gods.task.HealRadiusTask;
import com.dogonfire.gods.task.ThunderStormTask;

public class HolyPowerManager
{
	public static enum HolyPower 
	{
		KNOWLEDGE, CALLMOON, CALLSUN, HEALING, TAME, LIGHTNING, LIGHTNING_STORM, TELEPORT, FREEZE, ICE, FIREBALL, EARTHQUAKE, NATURE, FIREWORK, DRUNK;
	}

	private static final int	MAX_EFFECT_RADIUS	= 30;

	// How long a player must wait between uses of a holy artifact
	private static final long	DEFAULT_COOLDOWN_MILLIS		= 30 * 1000L;
	private static final long	KNOWLEDGE_COOLDOWN_MILLIS	= 10 * 60 * 1000L;

	private Random				random				= new Random();
	// Player UUID -> power -> when it can be used again
	private final Map<UUID, Map<HolyPower, Long>>	nextUseTimes	= new HashMap<>();
	// World UUID -> until when a sun, moon or storm power is changing that world's time or weather
	private final Map<UUID, Long>					worldBusyUntil	= new HashMap<>();

	private final GodsContext ctx;

	public HolyPowerManager(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	/**
	 * Uses a power, unless the player used it too recently or (for the sun, moon and storm powers) another one is
	 * still changing the world. Tells the player why not and returns false in that case.
	 */
	public boolean activatePower(Player player, HolyPower powerType, int powerStrength)
	{
		long now = System.currentTimeMillis();

		Map<HolyPower, Long> playerTimes = this.nextUseTimes.computeIfAbsent(player.getUniqueId(), id -> new HashMap<>());
		long nextUse = playerTimes.getOrDefault(powerType, 0L);
		if (now < nextUse)
		{
			player.sendMessage(ChatColor.RED + "Your holy artifact needs " + ((nextUse - now) / 1000L + 1) + " more seconds to regain its power.");
			return false;
		}

		boolean changesWorld = powerType == HolyPower.CALLMOON || powerType == HolyPower.CALLSUN || powerType == HolyPower.LIGHTNING_STORM;
		UUID worldId = player.getWorld().getUID();
		if (changesWorld && now < this.worldBusyUntil.getOrDefault(worldId, 0L))
		{
			player.sendMessage(ChatColor.RED + "The heavens of this world are already being moved by a holy artifact.");
			return false;
		}

		long cooldown = switch (powerType)
		{
		case KNOWLEDGE -> KNOWLEDGE_COOLDOWN_MILLIS;
		case FIREBALL -> Math.max(1, powerStrength) * 1000L;
		case CALLMOON, CALLSUN, LIGHTNING_STORM -> Math.max(DEFAULT_COOLDOWN_MILLIS, powerStrength * 60000L);
		default -> DEFAULT_COOLDOWN_MILLIS;
		};
		playerTimes.put(powerType, now + cooldown);
		if (changesWorld)
		{
			this.worldBusyUntil.put(worldId, now + powerStrength * 60000L);
		}

		switch (powerType)
		{
		case KNOWLEDGE -> boostKnowledge(player, powerStrength);
		case CALLMOON -> callMoon(player, powerStrength);
		case CALLSUN -> callSun(player, powerStrength);
		case HEALING -> healing(player, powerStrength);
		case TAME -> tame(player, powerStrength);
		case LIGHTNING -> lightning(player, powerStrength);
		case LIGHTNING_STORM -> lightningStorm(player, powerStrength);
		case TELEPORT -> teleport(player, powerStrength);
		case FREEZE -> freeze(player, powerStrength);
		case ICE -> ice(player, powerStrength);
		case FIREBALL -> fireBall(player, powerStrength);
		case EARTHQUAKE -> earthQuake(player, powerStrength);
		case NATURE -> growNature(player, powerStrength);
		case FIREWORK -> shootFirework(player, powerStrength);
		case DRUNK -> drunk(player, powerStrength);
		}
		return true;
	}

	public void boostKnowledge(Player player, int powerValue)
	{
		ctx.plugin().logDebug("Boosting " + powerValue + " xp of knowledge");

		ctx.plugin().getServer().getScheduler().scheduleSyncDelayedTask(ctx.plugin(), new BoostKnowledgeTask(ctx, player, powerValue), 1L);
	}

	public void callMoon(Player player, int powerValue)
	{
		ctx.plugin().logDebug("Calling the moon for " + powerValue + " minutes");

		ctx.plugin().getServer().getScheduler().scheduleSyncDelayedTask(ctx.plugin(), new CallMoonTask(ctx, player, System.currentTimeMillis() + powerValue * 60000L), 1L);
	}

	public void callSun(Player player, int powerValue)
	{
		ctx.plugin().logDebug("Calling the sun for " + powerValue + " minutes");

		ctx.plugin().getServer().getScheduler().scheduleSyncDelayedTask(ctx.plugin(), new CallSunTask(ctx, player, System.currentTimeMillis() + powerValue * 60000L), 1L);
	}

	private boolean checkBlock(double x1, double y1, double z1, double w1, double h1, double d1, double x2, double y2, double z2, double w2, double h2, double d2)
	{
		if (x1 + w1 < x2)
		{
			return false;
		}
		if (x2 + w2 < x1)
		{
			return false;
		}
		if (y1 + h1 < y2)
		{
			return false;
		}
		if (y2 + h2 < y1)
		{
			return false;
		}
		if (z1 + d1 < z2)
		{
			return false;
		}
		if (z2 + d2 < z1)
		{
			return false;
		}
		return true;
	}

	public String describe(HolyPower power, int value)
	{
		return switch (power)
		{
		case KNOWLEDGE -> "Boosts " + value + " levels of xp";
		case CALLMOON -> "Calls the moon for " + value + " min";
		case CALLSUN -> "Calls the sun for " + value + " min";
		case HEALING -> "Heals up to " + value + " other players";
		case TAME -> "Tame all beasts within " + 2 * value + " blocks";
		case LIGHTNING -> "Lightning strikes up to " + value + " enemies";
		case LIGHTNING_STORM -> "Creates a thunderstorm lasting " + value + " minutes";
		case TELEPORT -> "Teleport up to " + value + " blocks";
		case FREEZE -> "Freezes all water within a " + value + " block radius";
		case ICE -> "Shoots ice spike which freezes a target on hit";
		case FIREBALL -> "Fires a fireball with a " + value + " seconds cooldown";
		case EARTHQUAKE -> "Earthquake in " + value + " block radius";
		case NATURE -> "Grows nature in a radius of " + value + " blocks";
		case FIREWORK -> "Shoots " + value + " firework rockets";
		case DRUNK -> "Gets you drunk for " + value + " seconds!";
		};
	}

	public void drunk(Player player, int powerValue)
	{
		ctx.plugin().logDebug("Getting " + player.getName() + " drunk for " + powerValue + " seconds");

		ctx.plugin().getServer().getScheduler().scheduleSyncDelayedTask(ctx.plugin(), new DrunkTask(ctx, player, powerValue), 1L);
	}

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
				Entity[] near = HolyPowerManager.this.getNearbyEntities(location, 1.5D);

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
					near = HolyPowerManager.this.getNearbyEntities(location, 2.5D);
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
		task.runTaskTimer(ctx.plugin(), 0L, 3L);
	}

	public void fireBall(Player player, int power)
	{
		player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1.0F, 1.0F);
		player.launchProjectile(SmallFireball.class);
	}

	public void freeze(Player player, int power)
	{
		player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0F, 0.1F);
		int radius = effectRadius(power);
		Block center = player.getLocation().getBlock();
		for (int x = -radius; x <= radius; x++)
		{
			for (int y = -10; y < 2; y++)
			{
				for (int z = -radius; z <= radius; z++)
				{
					Block block = center.getRelative(x, y, z);
					if (block.getType() == Material.WATER)
					{
						block.setType(Material.ICE);
					}
				}
			}
		}
	}

	// Freeze and nature work on 10 blocks per power point, capped so one use can't touch hundreds of thousands of blocks
	private static int effectRadius(int power)
	{
		return Math.max(1, Math.min(10 * power, MAX_EFFECT_RADIUS));
	}

	public record PowerDescription(HolyPower power, int value)
	{
	}

	/**
	 * Reads a power and its value back from text made by {@link #describe}, or returns null.
	 */
	public PowerDescription getHolyPowerFromDescription(String description)
	{
		final int marker = 1234567;
		for (HolyPower power : HolyPower.values())
		{
			String template = describe(power, marker);
			String regex = Pattern.quote(template).replace(String.valueOf(2 * marker), "\\E(\\d+)\\Q").replace(String.valueOf(marker), "\\E(\\d+)\\Q");
			Matcher matcher = Pattern.compile(regex).matcher(description);
			if (matcher.matches())
			{
				int value = 1;
				if (matcher.groupCount() > 0)
				{
					value = Integer.parseInt(matcher.group(1));
					if (power == HolyPower.TAME)
					{
						value = Math.max(1, value / 2);
					}
				}
				return new PowerDescription(power, value);
			}
		}
		return null;
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

	/**
	 * Entities within radius of l, except those standing in the block at l itself.
	 */
	public Entity[] getNearbyEntities(Location l, double radius)
	{
		return getNearbyEntities(l, radius, e -> true);
	}

	/**
	 * Living entities within radius of l, except those standing in the block at l itself.
	 */
	public Entity[] getNearbyLivingEntities(Location l, double radius)
	{
		return getNearbyEntities(l, radius, e -> e instanceof LivingEntity);
	}

	private Entity[] getNearbyEntities(Location center, double radius, Predicate<Entity> filter)
	{
		World world = center.getWorld();
		if (world == null)
		{
			return new Entity[0];
		}
		double radiusSquared = radius * radius;
		return world.getNearbyEntities(center, radius, radius, radius, e -> filter.test(e) && e.getLocation().distanceSquared(center) <= radiusSquared && !isSameBlock(e.getLocation(), center)).toArray(new Entity[0]);
	}

	private static boolean isSameBlock(Location a, Location b)
	{
		return a.getBlockX() == b.getBlockX() && a.getBlockY() == b.getBlockY() && a.getBlockZ() == b.getBlockZ();
	}

	public void growNature(Player player, int power)
	{
		player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 1.0F, 0.1F);
		int radius = effectRadius(power);
		Block center = player.getLocation().getBlock();
		for (int x = -radius; x <= radius; x++)
		{
			for (int y = -10; y < 20; y++)
			{
				for (int z = -radius; z <= radius; z++)
				{
					Block block = center.getRelative(x, y, z);
					if (block.getType() == Material.DIRT)
					{
						if (block.getRelative(BlockFace.UP).getType() == Material.AIR)
						{
							block.setType(Material.GRASS_BLOCK);
						}
					}
					if ((block.getType() == Material.STONE) || (block.getType() == Material.COBBLESTONE))
					{
						if (block.getRelative(BlockFace.WEST).getType() == Material.AIR)
						{
							block.getRelative(BlockFace.WEST).setType(Material.VINE, false);
						}
						if (block.getRelative(BlockFace.EAST).getType() == Material.AIR)
						{
							block.getRelative(BlockFace.EAST).setType(Material.VINE, false);
						}
						if (block.getRelative(BlockFace.NORTH).getType() == Material.AIR)
						{
							block.getRelative(BlockFace.NORTH).setType(Material.VINE, false);
						}
						if (block.getRelative(BlockFace.SOUTH).getType() == Material.AIR)
						{
							block.getRelative(BlockFace.SOUTH).setType(Material.VINE, false);
						}
					}
					if ((block.getType() == Material.GRASS_BLOCK) && (block.getRelative(BlockFace.UP).getType() == Material.AIR))
					{
						Block above = block.getRelative(BlockFace.UP);
						switch (this.random.nextInt(5))
						{
						case 0:
							above.setType(Material.POPPY);
							break;
						case 1:
							placeTallPlant(above, Material.TALL_GRASS);
							break;
						case 2:
							placeTallPlant(above, Material.SUNFLOWER);
							break;
						case 3:
							above.setType(Material.SHORT_GRASS);
							break;
						default:
							break;
						}
					}
				}
			}
		}
	}

	// Two-block plants need both halves, or the lower half pops off as an item
	private static void placeTallPlant(Block lower, Material plant)
	{
		Block upper = lower.getRelative(BlockFace.UP);
		if (!upper.getType().isAir())
		{
			lower.setType(Material.SHORT_GRASS);
			return;
		}
		Bisected bottom = (Bisected) plant.createBlockData();
		bottom.setHalf(Bisected.Half.BOTTOM);
		Bisected top = (Bisected) plant.createBlockData();
		top.setHalf(Bisected.Half.TOP);
		lower.setBlockData(bottom, false);
		upper.setBlockData(top, false);
	}

	public void healing(Player player, int powerValue)
	{
		ctx.plugin().logDebug("Healing up to " + powerValue + " creatures");

		ctx.plugin().getServer().getScheduler().scheduleSyncDelayedTask(ctx.plugin(), new HealRadiusTask(ctx, player, powerValue), 1L);
	}

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
								if ((b.getType().isSolid()) && (HolyPowerManager.this.checkBlock(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), 1.0D, 1.0D, 1.0D, bLoc.getX() - 0.5D, bLoc.getY() - 0.5D, bLoc.getZ() - 0.5D, 1.0D, 1.0D, 1.0D)))
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

					// Encase the target in ice for a few seconds, then put back what was there
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
					}.runTaskTimer(ctx.plugin(), 80 + HolyPowerManager.this.random.nextInt(40), 3L);
				}
			}
		};
		run.runTaskTimer(ctx.plugin(), 0L, 1L);
	}

	void lightning(Player player, int powerStrength)
	{
		int strikes = 0;
		for (Entity entity : getNearbyLivingEntities(player.getLocation(), 10.0D))
		{
			if (strikes >= powerStrength)
			{
				break;
			}
			if (entity.getEntityId() != player.getEntityId())
			{
				entity.getWorld().strikeLightning(entity.getLocation());

				strikes++;
			}
		}
	}

	public void lightningStorm(Player player, int powerValue)
	{
		player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0F, 0.1F);

		player.getWorld().setStorm(true);

		ctx.plugin().logDebug("Starting thunderstorm for " + powerValue + " minutes");

		ctx.plugin().getServer().getScheduler().scheduleSyncDelayedTask(ctx.plugin(), new ThunderStormTask(ctx, player, System.currentTimeMillis() + powerValue * 60000), 60L);
	}

	public void shootFirework(Player player, int powerValue)
	{
		ctx.plugin().logDebug("Starting fireworks with " + powerValue + " rockets");

		ctx.plugin().getServer().getScheduler().scheduleSyncDelayedTask(ctx.plugin(), new FireworkTask(ctx, player, powerValue), 1L);
	}

	private void tame(Player player, int powerStrength)
	{
		for (Entity entity : getNearbyEntities(player.getLocation(), powerStrength * 2))
		{
			if ((entity instanceof Tameable))
			{
				Tameable creature = (Tameable) entity;

				creature.setOwner(player);
				creature.setTamed(true);
			}
		}
	}

	public void teleport(Player player, int powerStrength)
	{
		int distance = 100 * powerStrength;

		World world = player.getWorld();
		Location start = player.getLocation();
		start.setY(start.getY() + 1.6D);

		Block lastSafe = world.getBlockAt(start);

		BlockIterator bi = new BlockIterator(player, distance);
		while (bi.hasNext())
		{
			Block block = bi.next();
			if ((block.getType().isSolid()) && (block.getType() != Material.AIR))
			{
				break;
			}
			lastSafe = block;
		}
		Location newLoc = lastSafe.getLocation();
		newLoc.setPitch(start.getPitch());
		newLoc.setYaw(start.getYaw());
		player.teleport(newLoc);
		world.playEffect(newLoc, Effect.ENDER_SIGNAL, 0);
		world.playSound(newLoc, Sound.ENTITY_ENDERMAN_AMBIENT, 1.0F, 0.3F);
	}
}
