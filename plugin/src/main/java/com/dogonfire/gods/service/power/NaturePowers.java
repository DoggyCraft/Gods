package com.dogonfire.gods.service.power;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.service.HolyPowerRules;
import com.dogonfire.gods.util.NearbyEntities;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;

/** The powers over nature: freezing water, growing plants and taming beasts */
public final class NaturePowers {
  private final GodsContext ctx;
  private final Random random = new Random();

  public NaturePowers(GodsContext ctx) {
    this.ctx = ctx;
  }

  /** FREEZE: water around and below the player turns to ice */
  public void freeze(Player player, int power) {
    player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0F, 0.1F);
    int radius = HolyPowerRules.effectRadius(power);
    Block center = player.getLocation().getBlock();
    for (int x = -radius; x <= radius; x++) {
      for (int y = -10; y < 2; y++) {
        for (int z = -radius; z <= radius; z++) {
          Block block = center.getRelative(x, y, z);
          if (block.getType() == Material.WATER && mayChange(player, block)) {
            block.setType(Material.ICE);
          }
        }
      }
    }
  }

  /** NATURE: dirt grows grass, stone grows vines, grass grows flowers and tall grass */
  public void growNature(Player player, int power) {
    player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 1.0F, 0.1F);
    int radius = HolyPowerRules.effectRadius(power);
    Block center = player.getLocation().getBlock();
    for (int x = -radius; x <= radius; x++) {
      for (int y = -10; y < 20; y++) {
        for (int z = -radius; z <= radius; z++) {
          Block block = center.getRelative(x, y, z);
          if (block.getType() == Material.DIRT) {
            if (block.getRelative(BlockFace.UP).getType() == Material.AIR
                && mayChange(player, block)) {
              block.setType(Material.GRASS_BLOCK);
            }
          }
          if ((block.getType() == Material.STONE) || (block.getType() == Material.COBBLESTONE)) {
            for (BlockFace face :
                new BlockFace[] {
                  BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH
                }) {
              Block beside = block.getRelative(face);
              if (beside.getType() == Material.AIR && mayChange(player, beside)) {
                // The vine hangs on the stone it grows from
                MultipleFacing vine = (MultipleFacing) Material.VINE.createBlockData();
                vine.setFace(face.getOppositeFace(), true);
                beside.setBlockData(vine, false);
              }
            }
          }
          if ((block.getType() == Material.GRASS_BLOCK)
              && (block.getRelative(BlockFace.UP).getType() == Material.AIR)
              && mayChange(player, block.getRelative(BlockFace.UP))) {
            Block above = block.getRelative(BlockFace.UP);
            switch (this.random.nextInt(5)) {
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

  /**
   * Powers only change blocks outside other gods' holy land, neutral land and WorldGuard regions
   */
  private boolean mayChange(Player player, Block block) {
    if (this.ctx.settings().holyLand().enabled()) {
      Location location = block.getLocation();
      if (this.ctx.holyLandService().isNeutralLandLocation(location)) {
        return false;
      }
      String landGod = this.ctx.holyLandService().getGodAtHolyLandLocation(location);
      if (landGod != null
          && !landGod.equals(this.ctx.believerService().getGodForBeliever(player.getUniqueId()))) {
        return false;
      }
    }
    return this.ctx.worldGuard() == null || !this.ctx.worldGuard().isInRegion(block);
  }

  // Two-block plants need both halves, or the lower half pops off as an item
  private static void placeTallPlant(Block lower, Material plant) {
    Block upper = lower.getRelative(BlockFace.UP);
    if (!upper.getType().isAir()) {
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

  /**
   * TAME: every untamed tameable creature within 2 blocks per power point becomes the player's
   * (never someone's pet)
   */
  public void tame(Player player, int powerStrength) {
    for (Entity entity : NearbyEntities.all(player.getLocation(), powerStrength * 2.0D)) {
      if (entity instanceof Tameable creature
          && !creature.isTamed()
          && creature.getOwner() == null) {
        creature.setOwner(player);
        creature.setTamed(true);
      }
    }
  }
}
