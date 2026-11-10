package com.dogonfire.gods.service;

/**
 * What a sacrifice is worth to a god, without Bukkit so it can be tested. A god keeps a value per item: above
 * {@link #WANTED} it wants the item (a good sacrifice), down to {@link #MEH} it doesn't care, below that it is
 * insulted. Over time it wants more of some items and forgets the items it didn't want. Kept exactly as it was.
 */
public final class SacrificeRules
{
	/** Above this an item is wanted */
	public static final float	WANTED		= 10.0F;
	/** Down to this a sacrifice is accepted without joy */
	public static final float	MEH			= -5.0F;
	/** Sacrifice values stay within -64 to 64 */
	public static final float	MAX_VALUE	= 64.0F;

	public enum Verdict
	{
		GOOD, MEH, BAD
	}

	private SacrificeRules()
	{
	}

	/** How the god takes a sacrifice of an item with this value */
	public static Verdict verdict(float value)
	{
		if (value > WANTED)
		{
			return Verdict.GOOD;
		}
		if (value >= MEH)
		{
			return Verdict.MEH;
		}
		return Verdict.BAD;
	}

	/** Whether the god tells its believers it wants this item (whole values only, as the file was always read) */
	public static boolean isAskedFor(float value)
	{
		return (int) value > WANTED;
	}

	/** Each sacrifice of an item makes the god want it a little less */
	public static float afterSacrifice(float value)
	{
		return value - 1.0F;
	}

	/** The god wants an item more ({@code increase}: 1 to 3), up to {@link #MAX_VALUE} */
	public static float wantMore(float value, int increase)
	{
		value += increase;
		if (value > MAX_VALUE)
		{
			value = MAX_VALUE;
		}
		else if (value < -MAX_VALUE)
		{
			value = -MAX_VALUE;
		}
		return value;
	}

	/** An unwanted item is slowly forgotten: the value shrinks, and becomes 0 (removed) close to 0 */
	public static float forgetUnwanted(float value)
	{
		value = 0.25F * value;
		if (value > -0.5D)
		{
			value = 0.0F;
		}
		return value;
	}
}
