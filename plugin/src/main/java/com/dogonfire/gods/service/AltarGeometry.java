package com.dogonfire.gods.service;

import java.util.List;
import java.util.function.Predicate;

import org.bukkit.block.BlockFace;

/**
 * Which block an altar sign hangs on, without the server so it can be tested. An altar sign is a wall sign or a wall
 * hanging sign (ceiling hanging signs and standing signs never are):
 * <ul>
 * <li>A wall sign faces away from the block it is attached to: its altar block is behind it.</li>
 * <li>A wall hanging sign's bracket is attached to a block on one of the two sides across its facing (vanilla accepts
 * either: the facing turned clockwise or counter-clockwise). Its altar block is the side that is a lit altar block (a
 * block of an altar type with a light on top); if both are, the one the registry already knows as its altar block,
 * else the clockwise one. With neither lit (the light or the altar block is gone), a side that can still hold it, the
 * known one first, else the clockwise one ({@link #attached}).</li>
 * </ul>
 * Either way the sign stands next to its altar block, so the spot in front of the altar (where the god's figure stands,
 * {@link MainAltarRules#standingSpot}) is the sign moved once more away from the altar block.
 */
public final class AltarGeometry
{
	/** How a sign is mounted */
	public enum Mount
	{
		/** A wall sign: attached to the block behind it */
		WALL,
		/** A wall hanging sign: its bracket is attached to a block on its left or right */
		WALL_HANGING
	}

	private AltarGeometry()
	{
	}

	/** The sides (seen from the sign) it can be attached to, in the order they are preferred */
	public static List<BlockFace> attachSides(Mount mount, BlockFace facing)
	{
		if (mount == Mount.WALL)
		{
			return List.of(facing.getOppositeFace());
		}
		return List.of(clockwise(facing), clockwise(facing).getOppositeFace());
	}

	/**
	 * The side of the sign (seen from the sign) its altar block is on: for a wall sign the block behind it; for a wall
	 * hanging sign a side that is a lit altar block, else one that can hold it; either way the one the registry knows
	 * first, then the clockwise one.
	 *
	 * @param known whether the block on that side is the one the registry has as this sign's altar block
	 * @param holds whether the block on that side can hold the sign (a solid block)
	 * @param lit   whether the block on that side is a lit altar block (a block of an altar type with a light on top)
	 * @return the side; for a wall hanging sign with neither side holding it, the clockwise one
	 */
	public static BlockFace attached(Mount mount, BlockFace facing, Predicate<BlockFace> known, Predicate<BlockFace> holds, Predicate<BlockFace> lit)
	{
		List<BlockFace> sides = attachSides(mount, facing);
		if (sides.size() == 1)
		{
			return sides.get(0);
		}
		BlockFace best = sides.get(0);
		int bestScore = -1;
		for (BlockFace side : sides)
		{
			boolean isLit = lit.test(side);
			boolean canHold = isLit || holds.test(side);
			int score = (isLit ? 4 : 0) + (canHold ? 2 : 0) + (canHold && known.test(side) ? 1 : 0);
			if (score > bestScore)
			{
				best = side;
				bestScore = score;
			}
		}
		return best;
	}

	/**
	 * The side of the sign its altar block is on if that is a lit altar block ({@link #attached}), or null if it is none
	 */
	public static BlockFace altarSide(Mount mount, BlockFace facing, Predicate<BlockFace> known, Predicate<BlockFace> holds, Predicate<BlockFace> lit)
	{
		BlockFace side = attached(mount, facing, known, holds, lit);
		return lit.test(side) ? side : null;
	}

	/**
	 * True if a sign of this mount and facing on this side of a block (seen from the block) can be attached to it: a
	 * wall sign east of the block facing east, a wall hanging sign east of the block facing north or south.
	 */
	public static boolean hangsOn(Mount mount, BlockFace side, BlockFace facing)
	{
		return attachSides(mount, facing).contains(side.getOppositeFace());
	}

	/** The horizontal face a quarter turn clockwise (seen from above): north to east, east to south, ... */
	public static BlockFace clockwise(BlockFace face)
	{
		return switch (face)
		{
			case NORTH -> BlockFace.EAST;
			case EAST -> BlockFace.SOUTH;
			case SOUTH -> BlockFace.WEST;
			case WEST -> BlockFace.NORTH;
			default -> face;
		};
	}
}
