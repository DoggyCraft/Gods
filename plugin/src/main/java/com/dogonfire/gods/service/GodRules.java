package com.dogonfire.gods.service;

import java.util.Collection;
import java.util.Random;

import org.bukkit.ChatColor;

import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.model.GodType;

/**
 * The rules for a god's identity and power, without Bukkit's server so they can be tested: how its name is written,
 * its color and language file, how strong it is and how talkative. The arithmetic is kept exactly as it was.
 */
public final class GodRules
{
	private GodRules()
	{
	}

	/** "tHOR" becomes "Thor" */
	public static String formatGodName(String godName)
	{
		return godName.substring(0, 1).toUpperCase() + godName.substring(1).toLowerCase();
	}

	/** The chat color of a god type; white for none */
	public static ChatColor color(GodType godType)
	{
		if (godType == null)
		{
			return ChatColor.WHITE;
		}
		return switch (godType)
		{
			case THUNDER -> ChatColor.DARK_GRAY;
			case EVIL -> ChatColor.RED;
			case WISDOM -> ChatColor.DARK_GREEN;
			case FROST -> ChatColor.BLACK;
			case SUN -> ChatColor.DARK_RED;
			case SEA -> ChatColor.DARK_AQUA;
			case LOVE -> ChatColor.BLUE;
			case MOON -> ChatColor.GRAY;
			case WAR -> ChatColor.GREEN;
			case NATURE -> ChatColor.YELLOW;
			case CREATURES -> ChatColor.DARK_BLUE;
			case WEREWOLVES -> ChatColor.GRAY;
			default -> ChatColor.WHITE;
		};
	}

	/**
	 * The language file a god speaks from, e.g. english_sun_male.yml. A god without a gender always gets the same one,
	 * picked from its seed; a god that doesn't exist ({@code type} null) speaks as a male sun god.
	 */
	public static String languageFile(String language, GodType type, GodGender gender, long seed)
	{
		if (type == null)
		{
			type = GodType.SUN;
			gender = GodGender.Male;
		}
		else if (gender == GodGender.None)
		{
			gender = (seed & 1L) == 0L ? GodGender.Female : GodGender.Male;
		}
		return language + "_" + type.name().toLowerCase() + "_" + gender.name().toLowerCase() + ".yml";
	}

	/** A god's power: the power of all its believers, but at least {@code minPower} (the whitelist's) */
	public static float godPower(Collection<Float> believerPowers, int minPower)
	{
		float godPower = 0.0F;
		for (float believerPower : believerPowers)
		{
			godPower += believerPower;
		}
		if (godPower < minPower)
		{
			godPower = minPower;
		}
		return godPower;
	}

	/** Whether a god is dead: nobody believes in it and it has no power left */
	public static boolean isDead(int believers, float godPower)
	{
		return believers == 0 && godPower < 1.0F;
	}

	/**
	 * How talkative a god is now (1 or more): its own verbosity and power, times config.yml's verbosity, varying over
	 * the hours in a way its seed fixes.
	 */
	public static int verbosity(int baseVerbosity, long seed, float godPower, double verbositySetting, long nowMillis)
	{
		Random moodRandom = new Random(seed);

		double variation = 1.0D + 1.0D * Math.sin(moodRandom.nextFloat() + nowMillis / 3600000.0F);

		double godVerbosity = godPower / 100.0F + baseVerbosity;

		return (int) (1.0D + variation * (verbositySetting * godVerbosity));
	}
}
