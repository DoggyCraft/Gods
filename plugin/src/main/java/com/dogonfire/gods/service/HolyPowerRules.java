package com.dogonfire.gods.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dogonfire.gods.model.HolyPower;

/**
 * The rules of holy powers, without Bukkit so they can be tested: the lore line that describes a power (and reading it
 * back from artifacts made before artifacts were tagged), how long a player waits between uses, and how far the area
 * powers reach. Kept exactly as they were.
 */
public final class HolyPowerRules
{
	/** A power and its strength */
	public record PowerDescription(HolyPower power, int value)
	{
	}

	/** How long a player must wait between uses of a holy artifact */
	public static final long	DEFAULT_COOLDOWN_MILLIS		= 30 * 1000L;
	public static final long	KNOWLEDGE_COOLDOWN_MILLIS	= 10 * 60 * 1000L;

	private static final int	MAX_EFFECT_RADIUS			= 30;

	private HolyPowerRules()
	{
	}

	/**
	 * The lore line that described the power of an artifact made before artifacts were tagged, which {@link #parse} reads
	 * back. Always English; what new artifacts show is artifact.power.&lt;POWER&gt; in messages.yml.
	 */
	public static String describe(HolyPower power, int value)
	{
		return switch (power)
		{
		case KNOWLEDGE -> "Boosts " + value + " levels of xp";
		case CALLMOON -> "Calls the moon for " + value + " min";
		case CALLSUN -> "Calls the sun for " + value + " min";
		case HEALING -> "Heals up to " + value + " other players";
		case TAME -> "Tame all beasts within " + 2 * value + " blocks";
		case LIGHTNING -> "Lightning strikes up to " + value + " enemies";
		case LIGHTNING_STORM -> "Creates a thunderstorm lasting " + value + " minutes";
		case TELEPORT -> "Teleport up to " + value + " blocks";
		case FREEZE -> "Freezes all water within a " + value + " block radius";
		case ICE -> "Shoots ice spike which freezes a target on hit";
		case FIREBALL -> "Fires a fireball with a " + value + " seconds cooldown";
		case EARTHQUAKE -> "Earthquake in " + value + " block radius";
		case NATURE -> "Grows nature in a radius of " + value + " blocks";
		case FIREWORK -> "Shoots " + value + " firework rockets";
		case DRUNK -> "Gets you drunk for " + value + " seconds!";
		};
	}

	/** Reads a power and its value back from text made by {@link #describe} (without colors), or returns null */
	public static PowerDescription parse(String description)
	{
		final int marker = 1234567;
		for (HolyPower power : HolyPower.values())
		{
			String template = describe(power, marker);
			String regex = Pattern.quote(template).replace(String.valueOf(2 * marker), "\\E(\\d+)\\Q").replace(String.valueOf(marker), "\\E(\\d+)\\Q");
			Matcher matcher = Pattern.compile(regex).matcher(description);
			if (matcher.matches())
			{
				int value = 1;
				if (matcher.groupCount() > 0)
				{
					value = Integer.parseInt(matcher.group(1));
					if (power == HolyPower.TAME)
					{
						value = Math.max(1, value / 2);
					}
				}
				return new PowerDescription(power, value);
			}
		}
		return null;
	}

	/** The sun, moon and storm powers change the whole world, so only one of them runs in a world at a time */
	public static boolean changesWorld(HolyPower power)
	{
		return power == HolyPower.CALLMOON || power == HolyPower.CALLSUN || power == HolyPower.LIGHTNING_STORM;
	}

	/** How long the player waits before using this power again */
	public static long cooldownMillis(HolyPower power, int strength)
	{
		return switch (power)
		{
		case KNOWLEDGE -> KNOWLEDGE_COOLDOWN_MILLIS;
		case FIREBALL -> Math.max(1, strength) * 1000L;
		case CALLMOON, CALLSUN, LIGHTNING_STORM -> Math.max(DEFAULT_COOLDOWN_MILLIS, strength * 60000L);
		default -> DEFAULT_COOLDOWN_MILLIS;
		};
	}

	/** How long a world-changing power keeps its world busy */
	public static long worldBusyMillis(int strength)
	{
		return strength * 60000L;
	}

	/** Freeze and nature work on 10 blocks per power point, capped so one use can't touch hundreds of thousands of blocks */
	public static int effectRadius(int strength)
	{
		return Math.max(1, Math.min(10 * strength, MAX_EFFECT_RADIUS));
	}
}
