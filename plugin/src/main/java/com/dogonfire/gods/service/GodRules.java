package com.dogonfire.gods.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.bukkit.ChatColor;

import com.dogonfire.gods.message.GodVoices;
import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.model.GodType;

/**
 * The rules for a god's identity and power, without Bukkit's server so they can be tested: how its name is written,
 * its color and voice, how strong it is and how talkative. The arithmetic is kept exactly as it was.
 */
public final class GodRules
{
	private GodRules()
	{
	}

	/** "tHOR" becomes "Thor" */
	public static String formatGodName(String godName)
	{
		return godName.substring(0, 1).toUpperCase(Locale.ROOT) + godName.substring(1).toLowerCase(Locale.ROOT);
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
	 * The voices a god speaks with (see {@link GodVoices}), e.g. [sun_male]. A god without a gender always gets the
	 * same one, picked from its seed; a non-binary god has both of its type's voices and speaks each line with one of
	 * them, picked anew every time; a god that doesn't exist ({@code type} null) speaks as a male sun god.
	 */
	public static List<String> voices(GodType type, GodGender gender, long seed)
	{
		if (type == null)
		{
			return List.of(GodVoices.name(GodType.SUN, GodGender.Male));
		}
		if (gender == GodGender.NonBinary)
		{
			return List.of(GodVoices.name(type, GodGender.Male), GodVoices.name(type, GodGender.Female));
		}
		if (gender == null || gender == GodGender.None)
		{
			gender = (seed & 1L) == 0L ? GodGender.Female : GodGender.Male;
		}
		return List.of(GodVoices.name(type, gender));
	}

	/**
	 * The gender a new god chooses itself (when its altar sign didn't say), fixed by its seed: male, female or
	 * non-binary
	 */
	public static GodGender chosenGender(long seed)
	{
		return switch ((int) Math.floorMod(seed, 3L))
		{
			case 0 -> GodGender.Female;
			case 1 -> GodGender.Male;
			default -> GodGender.NonBinary;
		};
	}

	/**
	 * How long a god's priests must still wait before they may change its gender again ({@link Duration#ZERO}: they
	 * may now)
	 *
	 * @param lastChange    when its gender last changed, or null for never
	 * @param cooldownHours config.yml's priests.gender-change-cooldown-hours
	 */
	public static Duration genderChangeWait(Instant lastChange, Instant now, int cooldownHours)
	{
		if (lastChange == null)
		{
			return Duration.ZERO;
		}
		Duration left = Duration.between(now, lastChange.plus(Duration.ofHours(cooldownHours)));
		return left.isNegative() ? Duration.ZERO : left;
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
