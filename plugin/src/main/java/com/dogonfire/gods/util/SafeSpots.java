package com.dogonfire.gods.util;

import com.dogonfire.gods.service.HolyPowerRules;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;

/**
 * Where a player can be put safely (TELEPORT, /g home): {@link HolyPowerRules#isSafeSpot} on the
 * world's blocks. Reading a block of an unloaded chunk loads it; callers that must not load chunks
 * check first.
 */
public final class SafeSpots {
  private SafeSpots() {}

  /**
   * Whether a player can stand with their feet in {@code feet}: solid ground that doesn't hurt, two
   * free blocks, not the void
   */
  public static boolean isSafe(World world, Block feet) {
    Block ground = feet.getRelative(0, -1, 0);
    if (ground.getY() < world.getMinHeight() || feet.getY() + 1 >= world.getMaxHeight()) {
      return false;
    }
    Block head = feet.getRelative(0, 1, 0);
    return HolyPowerRules.isSafeSpot(
        ground.getType(),
        ground.getType().isSolid() && !isWet(ground),
        feet.getType(),
        isFree(feet),
        head.getType(),
        isFree(head));
  }

  /** Passable, no liquid and not waterlogged */
  public static boolean isFree(Block block) {
    return block.isPassable() && !block.isLiquid() && !isWet(block);
  }

  private static boolean isWet(Block block) {
    return block.getBlockData() instanceof Waterlogged waterlogged && waterlogged.isWaterlogged();
  }
}
