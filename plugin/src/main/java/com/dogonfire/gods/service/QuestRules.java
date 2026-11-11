package com.dogonfire.gods.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

/**
 * The numbers behind quests, without Bukkit so they can be tested: how big and long a new quest is, what it asks for,
 * when it has expired, which rewards it gives and where a pilgrimage chest may go. Kept exactly as they were, including
 * the order the random numbers are drawn in.
 */
public final class QuestRules
{
	/** Reward items are drawn at most this often, so values that don't fit can't loop forever */
	public static final int				MAX_REWARD_ROLLS	= 100;

	/** What a sacrifice quest asks for (by Material name) */
	public static final List<String>	SACRIFICE_TARGETS	= List.of("GOLDEN_SWORD", "GOLDEN_PICKAXE", "GOLDEN_SHOVEL", "GOLDEN_AXE", "FISHING_ROD", "ANVIL", "OAK_BOAT", "BOOK", "BOW", "APPLE");

	/** What a slay quest asks for besides the god's unholy mob (by EntityType name), drawn from ten */
	public static final List<String>	SLAY_TARGETS		= List.of("SHEEP", "SPIDER", "CHICKEN", "COW", "PIG");

	/** A pilgrimage chest is this many blocks from the believer, at least and at most */
	public static final int				PILGRIMAGE_MIN_DISTANCE	= 500;
	public static final int				PILGRIMAGE_MAX_DISTANCE	= 2000;

	private static final String[]		CONSONANTS			= { "b", "f", "g", "h", "j", "k", "l", "ll", "v", "r", "rr", "kk", "p", "t", "s", "x", "d", "n", "m", "nn" };
	private static final String[]		VOWELS				= { "a", "o", "i", "u", "y", "e" };

	private QuestRules()
	{
	}

	/** Size and length (in minutes) of a new quest */
	public record Size(int amount, int maxDurationMinutes)
	{
	}

	/** Make non-believers read the holy book: about a third of the non-believers online */
	public static Size giveItems(Random random, int nonBelieversOnline)
	{
		int amount = 1 + nonBelieversOnline / (2 + random.nextInt(3));
		return new Size(amount, (1 + random.nextInt(5)) * amount);
	}

	/** Sacrifice items: up to one more than the god's believers online */
	public static Size sacrifice(Random random, int believersOnline)
	{
		int amount = 1 + random.nextInt(1 + believersOnline);
		return new Size(amount, (1 + random.nextInt(5)) * amount);
	}

	/** Slay mobs: one to three more than the god's believers online */
	public static Size slay(Random random, int believersOnline)
	{
		int amount = 1 + random.nextInt(3) + believersOnline;
		return new Size(amount, (5 + random.nextInt(5)) * amount);
	}

	/** How long a pilgrimage lasts, in minutes */
	public static int pilgrimageMinutes(Random random)
	{
		return 15 + random.nextInt(10);
	}

	/** How much the rewards in a pilgrimage chest are worth */
	public static int pilgrimageRewardPower(Random random)
	{
		return 200 + random.nextInt(150);
	}

	/** The Material name a sacrifice quest asks for */
	public static String sacrificeTarget(Random random)
	{
		return SACRIFICE_TARGETS.get(random.nextInt(SACRIFICE_TARGETS.size()));
	}

	/**
	 * The mob a slay quest asks for: one of {@code common} (indices 0 to 4 of ten draws), or else the god's unholy mob
	 * (asked for each time, it may be null). Never the god's holy mob; drawn again until it fits.
	 */
	public static <T> T slayTarget(Random random, List<T> common, Supplier<T> unholyMob, T holyMob)
	{
		T target;
		do
		{
			int roll = random.nextInt(10);
			target = (roll < common.size()) ? common.get(roll) : unholyMob.get();
		}
		while (target == null || target.equals(holyMob));
		return target;
	}

	/** A made-up holy name, like "Ollukka" */
	public static String holyName(Random random)
	{
		int length = 2 + random.nextInt(6);
		StringBuilder name = new StringBuilder();

		boolean wasVowel = false;
		for (int l = 0; l < length; l++)
		{
			String[] letters = wasVowel ? CONSONANTS : VOWELS;
			name.append(letters[random.nextInt(letters.length)]);
			wasVowel = !wasVowel;
		}
		return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1).toLowerCase(Locale.ROOT);
	}

	/** The pilgrimage target's name */
	public static String holyMountainName(Random random)
	{
		return "Holy mountain of " + holyName(random);
	}

	/**
	 * A quest has expired when more whole minutes than its duration passed since it was created. A quest without a
	 * (readable) creation time counts as created long ago.
	 */
	public static boolean isExpired(Instant created, int maxDurationMinutes, Instant now)
	{
		Instant start = (created != null) ? created : Instant.EPOCH;
		return Duration.between(start, now).toMillis() / 60000L > maxDurationMinutes;
	}

	/**
	 * Random reward items worth about {@code power} in total: draws an item at most {@link #MAX_REWARD_ROLLS} times and
	 * keeps it if its value still fits. {@code values} are the items with a positive value.
	 */
	public static <T> List<T> rollRewards(Random random, List<Map.Entry<T, Integer>> values, int power)
	{
		List<T> rewards = new ArrayList<T>();
		if (values.isEmpty())
		{
			return rewards;
		}

		for (int t = 0; power > 0 && t < MAX_REWARD_ROLLS; t++)
		{
			Map.Entry<T, Integer> item = values.get(random.nextInt(values.size()));
			if (item.getValue() <= power)
			{
				rewards.add(item.getKey());
				power -= item.getValue();
			}
		}
		return rewards;
	}

	// Pilgrimage spots

	/** A spot at a random angle and distance from the center: {x, z} */
	public static int[] pilgrimageSpot(Random random, int centerX, int centerZ, int minDistance, int maxDistance)
	{
		double angle = random.nextDouble() * 2.0D * Math.PI;
		double distance = minDistance + random.nextDouble() * (maxDistance - minDistance);

		int x = centerX + (int) (Math.cos(angle) * distance);
		int z = centerZ + (int) (Math.sin(angle) * distance);

		return new int[] { inChunk(random, x), inChunk(random, z) };
	}

	/** Moves a coordinate to 1..14 inside its chunk, so the spot and its four neighbours are in one chunk */
	public static int inChunk(Random random, int coordinate)
	{
		return (coordinate & ~15) | (1 + random.nextInt(14));
	}

	/** The ground of a pilgrimage chest is above the sea and leaves room for the chest */
	public static boolean isFittingHeight(int groundY, int seaLevel, int maxHeight)
	{
		return groundY > seaLevel && groundY < maxHeight - 2;
	}
}
