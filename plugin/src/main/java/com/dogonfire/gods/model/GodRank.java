package com.dogonfire.gods.model;

import java.util.Locale;

/**
 * How far a god's religion has grown, from {@link #SPIRIT} (just created) to {@link #GREATER}; stored as {@code Rank}
 * in gods.yml. A god rises one rank at a time when it meets every condition config.yml's {@code ranks.<rank>} sets,
 * and its rank decides what its religion has (see {@code service/RankRules}). From {@link #DESCENDED} on, the god stands
 * as an NPC at its main altar. The names players see come from themes.yml ({@code ranks:}).
 */
public enum GodRank
{
	SPIRIT, RECOGNIZED, DESCENDED, EXALTED, GREATER;

	/** Its key in config.yml and themes.yml: the name in lower case */
	public String key()
	{
		return name().toLowerCase(Locale.ROOT);
	}

	/** The rank above, or null for the highest */
	public GodRank next()
	{
		return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null;
	}

	/** The rank below, or null for the lowest */
	public GodRank previous()
	{
		return ordinal() > 0 ? values()[ordinal() - 1] : null;
	}

	/** Whether this rank is {@code other} or higher */
	public boolean atLeast(GodRank other)
	{
		return compareTo(other) >= 0;
	}

	/** The rank with this key (any case), or null */
	public static GodRank byKey(String key)
	{
		if (key == null)
		{
			return null;
		}
		for (GodRank rank : values())
		{
			if (rank.name().equalsIgnoreCase(key.trim()))
			{
				return rank;
			}
		}
		return null;
	}
}
