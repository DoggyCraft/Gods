package com.dogonfire.gods.service.power;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;

import com.dogonfire.gods.service.HolyPowerRules;
import com.dogonfire.gods.util.NearbyEntities;

/** The powers over nature: freezing water, growing plants and taming beasts */
public final class NaturePowers
{
	private final Random random = new Random();

	/** FREEZE: water around and below the player turns to ice */
	public void freeze(Player player, int power)
	{
		player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0F, 0.1F);
		int radius = HolyPowerRules.effectRadius(power);
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

	/** NATURE: dirt grows grass, stone grows vines, grass grows flowers and tall grass */
	public void growNature(Player player, int power)
	{
		player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 1.0F, 0.1F);
		int radius = HolyPowerRules.effectRadius(power);
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
						for (BlockFace face : new BlockFace[] { BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH })
						{
							if (block.getRelative(face).getType() == Material.AIR)
							{
								block.getRelative(face).setType(Material.VINE, false);
							}
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

	/** TAME: every tameable creature within 2 blocks per power point becomes the player's */
	public void tame(Player player, int powerStrength)
	{
		for (Entity entity : NearbyEntities.all(player.getLocation(), powerStrength * 2))
		{
			if (entity instanceof Tameable creature)
			{
				creature.setOwner(player);
				creature.setTamed(true);
			}
		}
	}
}
