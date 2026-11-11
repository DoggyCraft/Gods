package com.dogonfire.gods.service;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import com.dogonfire.gods.model.Altar;
import com.dogonfire.gods.model.AltarKind;
import com.dogonfire.gods.model.BlockPos;

/**
 * Main altars (pure, tested): which altar /g setaltar means (the priest looks at a registered altar's sign, its altar
 * block or the light on top of it, and it must be a praying altar of their own god), which altar a god without one gets
 * on its own ({@link #automatic}), and where the god stands in front of it ({@link #standingSpot}).
 */
public final class MainAltarRules
{
	/** How far a priest may stand from the altar they look at, in blocks */
	public static final int REACH_BLOCKS = 5;

	private MainAltarRules()
	{
	}

	public enum Verdict
	{
		/** A praying altar of the priest's god */
		OK,
		/** Not a registered altar (nor its block or light) */
		NO_ALTAR,
		/** Only curse or blessing signs there */
		NOT_PRAYING,
		/** A praying altar, but of another god */
		OTHER_GOD
	}

	public record Target(Verdict verdict, Altar altar)
	{
	}

	/**
	 * The registered altars the looked-at block belongs to: the altar whose sign it is; else the altars on it (an altar
	 * block); else the altars on the block below (it is the light on top)
	 *
	 * @param atSign  the altar with this sign, or null
	 * @param onBlock the altars whose signs hang on this block
	 */
	public static List<Altar> candidates(BlockPos looked, Function<BlockPos, Altar> atSign, Function<BlockPos, List<Altar>> onBlock)
	{
		Altar sign = atSign.apply(looked);
		if (sign != null)
		{
			return List.of(sign);
		}
		List<Altar> block = onBlock.apply(looked);
		if (!block.isEmpty())
		{
			return block;
		}
		return onBlock.apply(looked.up(-1));
	}

	/**
	 * Where a god appears at its altar as an NPC ({@link GodNpcService}): the block in front of the sign, facing the altar,
	 * i.e. the sign moved once more away from its altar block. Whether a figure fits there is the caller's to check.
	 */
	public static BlockPos standingSpot(Altar altar)
	{
		BlockPos sign = altar.sign();
		BlockPos block = altar.block();
		return new BlockPos(sign.world(), 2 * sign.x() - block.x(), sign.y(), 2 * sign.z() - block.z());
	}

	/** The praying altar of {@code godName} among the candidates (the first), or why there is none */
	public static Target choose(List<Altar> candidates, String godName)
	{
		if (candidates.isEmpty())
		{
			return new Target(Verdict.NO_ALTAR, null);
		}
		boolean praying = false;
		for (Altar altar : candidates)
		{
			if (altar.kind() == AltarKind.PRAYING)
			{
				praying = true;
				if (altar.god() != null && altar.god().equals(godName))
				{
					return new Target(Verdict.OK, altar);
				}
			}
		}
		return new Target(praying ? Verdict.OTHER_GOD : Verdict.NOT_PRAYING, null);
	}

	/**
	 * The altar a god without a main altar gets on its own, among its registered altars (in the order the registry
	 * found them): of the praying ones, the oldest whose creation time is known, else the first found; null if there is
	 * none
	 */
	public static Altar automatic(Collection<Altar> altars)
	{
		Altar first = null;
		Altar oldest = null;
		for (Altar altar : altars)
		{
			if (altar.kind() != AltarKind.PRAYING)
			{
				continue;
			}
			if (first == null)
			{
				first = altar;
			}
			if (altar.created() != null && (oldest == null || altar.created().isBefore(oldest.created())))
			{
				oldest = altar;
			}
		}
		return oldest != null ? oldest : first;
	}
}
