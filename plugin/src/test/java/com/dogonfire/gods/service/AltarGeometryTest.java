package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dogonfire.gods.model.Altar;
import com.dogonfire.gods.model.AltarKind;
import com.dogonfire.gods.model.BlockPos;
import com.dogonfire.gods.service.AltarGeometry.Mount;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;

class AltarGeometryTest {
  private static final Predicate<BlockFace> NONE = side -> false;
  private static final Predicate<BlockFace> ALL = side -> true;

  /** A wall sign hangs on the block behind it, whatever is there */
  @Test
  void aWallSignHangsOnTheBlockBehindIt() {
    assertEquals(List.of(BlockFace.WEST), AltarGeometry.attachSides(Mount.WALL, BlockFace.EAST));
    assertEquals(
        BlockFace.WEST, AltarGeometry.attached(Mount.WALL, BlockFace.EAST, NONE, NONE, NONE));
    assertEquals(
        BlockFace.WEST, AltarGeometry.altarSide(Mount.WALL, BlockFace.EAST, NONE, ALL, ALL));
    // Only if it is a lit altar block, never one beside it
    assertNull(
        AltarGeometry.altarSide(
            Mount.WALL, BlockFace.EAST, NONE, ALL, side -> side != BlockFace.WEST));
  }

  /**
   * A wall hanging sign's bracket holds on to its left or right: the facing turned clockwise or
   * counter-clockwise
   */
  @Test
  void aWallHangingSignHangsOnEitherSideAcrossItsFacing() {
    assertEquals(
        List.of(BlockFace.EAST, BlockFace.WEST),
        AltarGeometry.attachSides(Mount.WALL_HANGING, BlockFace.NORTH));
    assertEquals(
        List.of(BlockFace.SOUTH, BlockFace.NORTH),
        AltarGeometry.attachSides(Mount.WALL_HANGING, BlockFace.EAST));
    assertEquals(
        List.of(BlockFace.WEST, BlockFace.EAST),
        AltarGeometry.attachSides(Mount.WALL_HANGING, BlockFace.SOUTH));
    assertEquals(
        List.of(BlockFace.NORTH, BlockFace.SOUTH),
        AltarGeometry.attachSides(Mount.WALL_HANGING, BlockFace.WEST));
  }

  @Test
  void theLitAltarBlockSideIsTheAltar() {
    Predicate<BlockFace> westLit = side -> side == BlockFace.WEST;
    assertEquals(
        BlockFace.WEST,
        AltarGeometry.altarSide(Mount.WALL_HANGING, BlockFace.NORTH, NONE, ALL, westLit));
    assertEquals(
        BlockFace.WEST,
        AltarGeometry.attached(
            Mount.WALL_HANGING, BlockFace.NORTH, side -> side == BlockFace.EAST, ALL, westLit));
    // Neither side lit: no altar
    assertNull(AltarGeometry.altarSide(Mount.WALL_HANGING, BlockFace.NORTH, NONE, ALL, NONE));
  }

  /** Between two altar blocks: the one the registry knows, else the clockwise one */
  @Test
  void betweenTwoAltarBlocksTheKnownThenTheClockwiseOneWins() {
    assertEquals(
        BlockFace.EAST,
        AltarGeometry.altarSide(Mount.WALL_HANGING, BlockFace.NORTH, NONE, ALL, ALL));
    assertEquals(
        BlockFace.WEST,
        AltarGeometry.altarSide(
            Mount.WALL_HANGING, BlockFace.NORTH, side -> side == BlockFace.WEST, ALL, ALL));
  }

  /**
   * With no altar block lit, the block that may still hold it: the known one first, then the
   * clockwise one
   */
  @Test
  void withoutALitSideTheBlockThatHoldsItIsAttached() {
    Predicate<BlockFace> westHolds = side -> side == BlockFace.WEST;
    // The known altar block is gone (air): the sign hangs on the other side now
    assertEquals(
        BlockFace.WEST,
        AltarGeometry.attached(
            Mount.WALL_HANGING, BlockFace.NORTH, side -> side == BlockFace.EAST, westHolds, NONE));
    assertEquals(
        BlockFace.WEST,
        AltarGeometry.attached(
            Mount.WALL_HANGING, BlockFace.NORTH, side -> side == BlockFace.WEST, ALL, NONE));
    assertEquals(
        BlockFace.EAST,
        AltarGeometry.attached(Mount.WALL_HANGING, BlockFace.NORTH, NONE, ALL, NONE));
    assertEquals(
        BlockFace.EAST,
        AltarGeometry.attached(Mount.WALL_HANGING, BlockFace.NORTH, NONE, NONE, NONE));
  }

  @Test
  void signsOnAnAltarBlocksSide() {
    // Seen from the block: a wall sign east of it faces east
    assertTrue(AltarGeometry.hangsOn(Mount.WALL, BlockFace.EAST, BlockFace.EAST));
    assertFalse(AltarGeometry.hangsOn(Mount.WALL, BlockFace.EAST, BlockFace.NORTH));
    assertFalse(AltarGeometry.hangsOn(Mount.WALL, BlockFace.EAST, BlockFace.WEST));
    // A hanging sign east of it faces north or south
    assertTrue(AltarGeometry.hangsOn(Mount.WALL_HANGING, BlockFace.EAST, BlockFace.NORTH));
    assertTrue(AltarGeometry.hangsOn(Mount.WALL_HANGING, BlockFace.EAST, BlockFace.SOUTH));
    assertFalse(AltarGeometry.hangsOn(Mount.WALL_HANGING, BlockFace.EAST, BlockFace.EAST));
    assertFalse(AltarGeometry.hangsOn(Mount.WALL_HANGING, BlockFace.EAST, BlockFace.WEST));
  }

  /**
   * A hanging sign east of its altar block (facing north): the god stands one block further east,
   * facing west toward the altar, and /g home never lands on the sign
   */
  @Test
  void theGodStandsBeyondAHangingSignFacingTheAltar() {
    BlockPos block = new BlockPos("world", 10, 64, 10);
    BlockPos sign = new BlockPos("world", 11, 64, 10);
    Altar altar = new Altar(sign, block, AltarKind.PRAYING, "Zeus", null, null);
    assertEquals(new BlockPos("world", 12, 64, 10), MainAltarRules.standingSpot(altar));
    assertEquals(90F, GodNpcRules.facingYaw(altar), 0.001F);
    HomeRules.Home home = HomeRules.home(altar, null);
    assertEquals(90F, home.yaw(), 0.001F);
    assertTrue(home.keepClear().containsAll(Set.of(sign, block)));
  }
}
