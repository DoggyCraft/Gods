package com.dogonfire.gods.service;

import java.util.Collection;
import java.util.UUID;

import com.dogonfire.gods.model.BlockPos;

/**
 * Claim lands (/g holyclaim): a priest makes their GriefPrevention claim the holy land of their god. Who may, when it
 * is released or refused, and when a claim land goes; without a server so it can be tested. GriefPrevention's claims
 * reach Gods as {@link ClaimInfo}s ({@code integration/GriefPreventionIntegration}).
 */
public final class HolyClaimRules
{
	private HolyClaimRules()
	{
	}

	/**
	 * A GriefPrevention claim as Gods sees it: its id, its owner (null for an admin claim), whether it is an admin claim
	 * and a top-level claim (not a subclaim), its world's name and its area (corners included, any height).
	 */
	public record ClaimInfo(long id, UUID owner, boolean admin, boolean topLevel, String world, int minX, int minZ, int maxX, int maxZ)
	{
		/** Whether the block is in the claim's area (any height) */
		public boolean contains(String worldName, int x, int z)
		{
			return this.world != null && this.world.equals(worldName) && x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
		}

		/** Whether the block is in the claim's area (any height) */
		public boolean contains(BlockPos block)
		{
			return block != null && contains(block.world(), block.x(), block.z());
		}
	}

	/** What /g holyclaim does */
	public enum Answer
	{
		/** The priest stands in no claim */
		NO_CLAIM,
		/** The priest stands in a subclaim: only a whole (top-level) claim can be holy land */
		SUBCLAIM,
		/** An admin claim is nobody's to give */
		ADMIN_CLAIM,
		/** The claim isn't the priest's own */
		NOT_OWNER,
		/** It is another god's holy land */
		OTHER_GOD,
		/** It is the priest's god's holy land already: it is released */
		RELEASE,
		/** No registered praying altar of the god stands in the claim */
		NO_ALTAR,
		/** The god holds as many holy lands as it may */
		AT_LIMIT,
		/** The claim becomes the god's holy land */
		CLAIM,
		/** A plugin cancelled the {@code HolyLandClaimEvent} (the service's answer, never {@link #decide}'s) */
		CANCELLED
	}

	/**
	 * What /g holyclaim does for {@code player}, a priest of {@code godName}, standing in {@code claim}.
	 *
	 * @param claim        the claim the priest stands in (its subclaim if they stand in one), or null
	 * @param claimLandGod the god whose claim land the claim is, or null if it isn't one
	 * @param altarInside  whether a registered praying altar of the god has its altar block in the claim
	 * @param lands        how many holy lands the god holds (of chunks and claims)
	 * @param maxLands     how many it may hold ({@code RankRules.maxHolyLands}); a claim land counts as one, whatever its size
	 */
	public static Answer decide(ClaimInfo claim, UUID player, String godName, String claimLandGod, boolean altarInside, int lands, int maxLands)
	{
		if (claim == null)
		{
			return Answer.NO_CLAIM;
		}
		if (!claim.topLevel())
		{
			return Answer.SUBCLAIM;
		}
		if (claim.admin() || claim.owner() == null)
		{
			return Answer.ADMIN_CLAIM;
		}
		if (!claim.owner().equals(player))
		{
			return Answer.NOT_OWNER;
		}
		if (claimLandGod != null)
		{
			return claimLandGod.equals(godName) ? Answer.RELEASE : Answer.OTHER_GOD;
		}
		if (!altarInside)
		{
			return Answer.NO_ALTAR;
		}
		if (lands >= maxLands)
		{
			return Answer.AT_LIMIT;
		}
		return Answer.CLAIM;
	}

	/** Whether any of the blocks is in the claim's area */
	public static boolean anyInside(ClaimInfo claim, Collection<BlockPos> blocks)
	{
		if (claim == null)
		{
			return false;
		}
		for (BlockPos block : blocks)
		{
			if (claim.contains(block))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Whether a claim land goes with a praying altar of {@code altarGod} that was removed from it (its altar block was in
	 * the claim): only when it is that god's land and no other praying altar of the god has its altar block in the
	 * claim (as {@code HolyLandRules.goesWithAltar} for lands of chunks).
	 *
	 * @param otherAltarBlocks the altar blocks of the god's other registered praying altars
	 */
	public static boolean goesWithAltar(String landGod, String altarGod, ClaimInfo claim, Collection<BlockPos> otherAltarBlocks)
	{
		if (altarGod == null || !altarGod.equals(landGod) || claim == null)
		{
			return false;
		}
		return !anyInside(claim, otherAltarBlocks);
	}

	/** Why a claim land is no longer holy land, or null while it still is */
	public enum Loss
	{
		/** GriefPrevention deleted the claim (or it is gone) */
		CLAIM_GONE,
		/** The claim belongs to someone who is not a priest of the land's god now, or became an admin claim */
		NEW_OWNER,
		/** The claim no longer has a praying altar of the god in it (it was resized) */
		NO_ALTAR
	}

	/**
	 * Whether a claim land's claim still makes it holy land.
	 *
	 * @param claim the claim as GriefPrevention has it now, or null if it has none of that id
	 * @param altarInside whether a registered praying altar of the land's god has its altar block in the claim; null
	 *            when it isn't checked (the god's turn only checks that the claim exists)
	 * @return why it is lost, or null if it is kept
	 */
	public static Loss check(ClaimInfo claim, Boolean altarInside)
	{
		if (claim == null || !claim.topLevel())
		{
			return Loss.CLAIM_GONE;
		}
		if (claim.admin() || claim.owner() == null)
		{
			return Loss.NEW_OWNER;
		}
		if (altarInside != null && !altarInside)
		{
			return Loss.NO_ALTAR;
		}
		return null;
	}

	/**
	 * Whether a claim land is kept when its claim changes owner: only when the new owner (null for an admin claim) is a
	 * priest of the land's god
	 */
	public static boolean keptByNewOwner(UUID newOwner, boolean newOwnerIsPriestOfLandGod)
	{
		return newOwner != null && newOwnerIsPriestOfLandGod;
	}
}
