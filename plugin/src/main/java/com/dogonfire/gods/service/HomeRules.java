package com.dogonfire.gods.service;

import com.dogonfire.gods.model.Altar;
import com.dogonfire.gods.model.BlockPos;
import com.dogonfire.gods.model.GodHome;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Where a religion's home is (pure, tested): /g home and {@code GodInfo.home}. The home is the main
 * altar: the spot in front of its sign ({@link MainAltarRules#standingSpot}), facing the altar. A
 * god without a main altar keeps the home stored before main altars ({@code Home} in gods.yml), if
 * it has one. /g home lands on the nearest safe spot around it ({@link #safeLanding}).
 */
public final class HomeRules {
  /** How far /g home looks around the home for a safe spot, sideways and up or down, in blocks */
  public static final int SEARCH_RADIUS_BLOCKS = 3;

  /**
   * Offsets from the home, nearest first: the home itself, then outwards, at the same height before
   * above and below
   */
  private static final List<int[]> OFFSETS = offsets();

  private HomeRules() {}

  /**
   * A home: where to stand and which way to look
   *
   * @param altarBlock the main altar's block the player faces, or null for a home from before main
   *     altars
   * @param keepClear blocks no one is put in (the altar's sign, block and the space above it)
   */
  public record Home(
      String world,
      double x,
      double y,
      double z,
      float yaw,
      BlockPos altarBlock,
      Set<BlockPos> keepClear) {
    /** The block the player's feet are in */
    public BlockPos spot() {
      return new BlockPos(
          this.world, (int) Math.floor(this.x), (int) Math.floor(this.y), (int) Math.floor(this.z));
    }
  }

  /**
   * The religion's home: in front of the main altar if the god has one, else the home stored before
   * main altars, else null
   *
   * @param mainAltar the god's main altar, or null
   * @param legacy the home from before main altars ({@code Home} in gods.yml), or null
   */
  public static Home home(Altar mainAltar, GodHome legacy) {
    return home(mainAltar, legacy, false);
  }

  /**
   * The religion's home, as {@link #home(Altar, GodHome)}; while the god's figure stands on the
   * spot in front of the main altar, the home is the block beyond it (one further from the altar,
   * facing it), so no one lands inside the figure
   *
   * @param figureStands whether the god's NPC stands at its main altar now
   */
  public static Home home(Altar mainAltar, GodHome legacy, boolean figureStands) {
    if (mainAltar != null) {
      BlockPos block = mainAltar.block();
      BlockPos figure = MainAltarRules.standingSpot(mainAltar);
      BlockPos spot =
          figureStands
              ? new BlockPos(
                  figure.world(),
                  2 * figure.x() - mainAltar.sign().x(),
                  figure.y(),
                  2 * figure.z() - mainAltar.sign().z())
              : figure;
      Set<BlockPos> keepClear =
          figureStands
              ? Set.of(mainAltar.sign(), block, block.up(1), block.up(2), figure, figure.up(1))
              : Set.of(mainAltar.sign(), block, block.up(1), block.up(2));
      return new Home(
          spot.world(),
          spot.x() + 0.5D,
          spot.y(),
          spot.z() + 0.5D,
          GodNpcRules.facingYaw(mainAltar),
          block,
          keepClear);
    }
    if (legacy != null && legacy.world() != null) {
      return new Home(legacy.world(), legacy.x(), legacy.y(), legacy.z(), 0.0F, null, Set.of());
    }
    return null;
  }

  /**
   * The nearest spot within {@link #SEARCH_RADIUS_BLOCKS} of {@code spot} (itself first) where a
   * player can stand, outside {@code keepClear}, or null if there is none
   *
   * @param safe whether a player can stand with their feet in that block (solid ground below, two
   *     free blocks, no liquid or fire, not the void)
   */
  public static BlockPos safeLanding(
      BlockPos spot, Set<BlockPos> keepClear, Predicate<BlockPos> safe) {
    for (int[] offset : OFFSETS) {
      BlockPos feet =
          new BlockPos(
              spot.world(), spot.x() + offset[0], spot.y() + offset[1], spot.z() + offset[2]);
      if (!keepClear.contains(feet) && safe.test(feet)) {
        return feet;
      }
    }
    return null;
  }

  /** The yaw that looks from the feet at the altar block's middle */
  public static float facing(BlockPos feet, BlockPos altarBlock) {
    return GodNpcRules.yawToward(
        feet.x() + 0.5D, feet.z() + 0.5D, altarBlock.x() + 0.5D, altarBlock.z() + 0.5D);
  }

  private static List<int[]> offsets() {
    List<int[]> offsets = new ArrayList<>();
    int r = SEARCH_RADIUS_BLOCKS;
    for (int dy = -r; dy <= r; dy++) {
      for (int dx = -r; dx <= r; dx++) {
        for (int dz = -r; dz <= r; dz++) {
          offsets.add(new int[] {dx, dy, dz});
        }
      }
    }
    // Nearest first; at the same distance, the same height first, then above before below (a step
    // up, not a fall)
    offsets.sort(
        Comparator.<int[]>comparingInt(o -> o[0] * o[0] + o[1] * o[1] + o[2] * o[2])
            .thenComparingInt(o -> Math.abs(o[1]))
            .thenComparingInt(o -> -o[1])
            .thenComparingInt(o -> o[0])
            .thenComparingInt(o -> o[2]));
    return List.copyOf(offsets);
  }
}
