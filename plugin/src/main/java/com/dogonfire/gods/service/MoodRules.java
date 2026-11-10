package com.dogonfire.gods.service;

import java.util.Random;

import com.dogonfire.gods.model.GodMood;

/**
 * How a god's mood works, without Bukkit so it can be tested. The mood is a number from -100 to 100; prayers, meals,
 * kills and sacrifices raise or lower it, and while believers are online it slowly falls off. A god's seed fixes how
 * much a good deed pleases it and how fast its mood falls. The arithmetic is kept exactly as it was.
 */
public final class MoodRules
{
	public static final float	MAX_MOOD		= 100.0F;
	public static final float	MIN_MOOD		= -100.0F;

	/** How much a bad deed angers any god */
	public static final float	ANGRY_MODIFIER	= -1.0F;

	private MoodRules()
	{
	}

	/** The mood after adding {@code change}, kept within -100 to 100 */
	public static float add(float mood, float change)
	{
		float godMood = mood + change;
		if (godMood > MAX_MOOD)
		{
			godMood = MAX_MOOD;
		}
		else if (godMood < MIN_MOOD)
		{
			godMood = MIN_MOOD;
		}
		return godMood;
	}

	/** How a mood value feels */
	public static GodMood moodFor(float godMood)
	{
		if (godMood < -70.0F)
		{
			return GodMood.ANGRY;
		}
		if (godMood < -20.0F)
		{
			return GodMood.DISPLEASED;
		}
		if (godMood < 20.0F)
		{
			return GodMood.NEUTRAL;
		}
		if (godMood < 70.0F)
		{
			return GodMood.PLEASED;
		}
		return GodMood.EXALTED;
	}

	/** How much a good deed pleases the god with this seed (5 to 14) */
	public static float pleasedModifier(long seed)
	{
		Random moodRandom = new Random(seed);

		return 5 + moodRandom.nextInt(10);
	}

	/**
	 * How much the mood changes on one update while believers are online (negative): config.yml's falloff, more with
	 * more believers online (how much more the seed decides), and swinging over the hours.
	 */
	public static float falloff(long seed, double moodFalloffSetting, int onlineBelievers, long nowMillis)
	{
		Random moodRandom = new Random(seed);

		float baseFalloff = (1 + moodRandom.nextInt(40)) / 20.0F;

		double falloffValue = -moodFalloffSetting * (1.0F + baseFalloff * onlineBelievers) * (1.0D + Math.sin(nowMillis / 1500000.0F));

		return (float) falloffValue;
	}
}
