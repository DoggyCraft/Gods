package com.dogonfire.gods.model;

import java.util.Locale;

/**
 * How devoted a believer is to their god, from {@link #FOLLOWER} (every believer) to {@link #SAINT}; stored as
 * {@code DevotionLevel} in believers.yml. A believer rises when their devotion points reach the next level's threshold
 * (config.yml's {@code devotion.levels}) and each level unlocks something ({@code devotion.unlocks}; see
 * {@code service/DevotionRules}). The names players see come from themes.yml ({@code devotion:}).
 */
public enum DevotionLevel
{
	FOLLOWER, DEVOTEE, ZEALOT, SAINT;

	/** Its key in config.yml and themes.yml: the name in lower case */
	public String key()
	{
		return name().toLowerCase(Locale.ROOT);
	}

	/** The level above, or null for the highest */
	public DevotionLevel next()
	{
		return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null;
	}

	/** The level below, or null for the lowest */
	public DevotionLevel previous()
	{
		return ordinal() > 0 ? values()[ordinal() - 1] : null;
	}

	/** Whether this level is {@code other} or higher */
	public boolean atLeast(DevotionLevel other)
	{
		return compareTo(other) >= 0;
	}

	/** The level with this key (any case), or null */
	public static DevotionLevel byKey(String key)
	{
		if (key == null)
		{
			return null;
		}
		for (DevotionLevel level : values())
		{
			if (level.name().equalsIgnoreCase(key.trim()))
			{
				return level;
			}
		}
		return null;
	}
}
