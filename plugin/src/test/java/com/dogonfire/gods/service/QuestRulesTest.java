package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

class QuestRulesTest
{
	private static final Instant NOW = Instant.parse("2024-05-01T12:00:00Z");

	@Test
	void questsExpireAfterMoreWholeMinutesThanTheirDuration()
	{
		assertFalse(QuestRules.isExpired(NOW.minus(Duration.ofMinutes(10)), 10, NOW));
		assertFalse(QuestRules.isExpired(NOW.minus(Duration.ofSeconds(10 * 60 + 59)), 10, NOW));
		assertTrue(QuestRules.isExpired(NOW.minus(Duration.ofMinutes(11)), 10, NOW));
		// A quest without a creation time was made long ago
		assertTrue(QuestRules.isExpired(null, 1000, NOW));
		// A quest made in the future (clock changed) has not expired
		assertFalse(QuestRules.isExpired(NOW.plus(Duration.ofHours(1)), 0, NOW));
	}

	@Test
	void questSizesStayInTheirRanges()
	{
		Random random = new Random(1);
		for (int i = 0; i < 1000; i++)
		{
			QuestRules.Size slay = QuestRules.slay(random, 2);
			assertTrue(slay.amount() >= 3 && slay.amount() <= 5, "slay " + slay);
			assertEquals(0, slay.maxDurationMinutes() % slay.amount());
			int perKill = slay.maxDurationMinutes() / slay.amount();
			assertTrue(perKill >= 5 && perKill <= 9);

			QuestRules.Size sacrifice = QuestRules.sacrifice(random, 2);
			assertTrue(sacrifice.amount() >= 1 && sacrifice.amount() <= 3, "sacrifice " + sacrifice);
			int perItem = sacrifice.maxDurationMinutes() / sacrifice.amount();
			assertTrue(perItem >= 1 && perItem <= 5);

			QuestRules.Size giveItems = QuestRules.giveItems(random, 12);
			assertTrue(giveItems.amount() >= 1 + 12 / 4 && giveItems.amount() <= 1 + 12 / 2, "give " + giveItems);

			int pilgrimage = QuestRules.pilgrimageMinutes(random);
			assertTrue(pilgrimage >= 15 && pilgrimage <= 24);
			int power = QuestRules.pilgrimageRewardPower(random);
			assertTrue(power >= 200 && power <= 349);
		}
	}

	@Test
	void everySacrificeTargetCanBeAskedFor()
	{
		Random random = new Random(2);
		List<String> seen = new ArrayList<String>();
		for (int i = 0; i < 1000; i++)
		{
			String target = QuestRules.sacrificeTarget(random);
			assertTrue(QuestRules.SACRIFICE_TARGETS.contains(target));
			if (!seen.contains(target))
			{
				seen.add(target);
			}
		}
		assertEquals(QuestRules.SACRIFICE_TARGETS.size(), seen.size());
	}

	@Test
	void slayTargetsAreNeverTheHolyMob()
	{
		Random random = new Random(3);
		List<String> common = List.of("SHEEP", "SPIDER", "CHICKEN", "COW", "PIG");
		boolean unholySeen = false;
		for (int i = 0; i < 500; i++)
		{
			String target = QuestRules.slayTarget(random, common, () -> "ZOMBIE", "COW");
			assertNotEquals("COW", target);
			unholySeen |= target.equals("ZOMBIE");
		}
		assertTrue(unholySeen);

		// Without an unholy mob, only the common ones
		for (int i = 0; i < 100; i++)
		{
			assertTrue(common.contains(QuestRules.slayTarget(random, common, () -> null, "PIG")));
		}
	}

	/** Five targets are drawn from ten, as Gods always did */
	@Test
	void slayTargetsDrawLikeBefore()
	{
		List<String> common = List.of("SHEEP", "SPIDER", "CHICKEN", "COW", "PIG");
		Random before = new Random(11);
		Random now = new Random(11);
		for (int i = 0; i < 200; i++)
		{
			int roll;
			String expected;
			do
			{
				roll = before.nextInt(10);
				expected = roll < common.size() ? common.get(roll) : "ZOMBIE";
			}
			while (expected.equals("COW"));
			assertEquals(expected, QuestRules.slayTarget(now, common, () -> "ZOMBIE", "COW"));
		}
	}

	/** Themes may have any number of targets, or none that fit */
	@Test
	void slayTargetsOfAnyThemeEnd()
	{
		Random random = new Random(5);
		assertEquals("ZOMBIE", QuestRules.slayTarget(random, List.of(), () -> "ZOMBIE", "COW"));
		assertNull(QuestRules.slayTarget(random, List.of("COW"), () -> null, "COW"));
		assertNull(QuestRules.slayTarget(random, List.of(), () -> "COW", "COW"));
		List<String> many = List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L");
		boolean unholySeen = false;
		for (int i = 0; i < 200; i++)
		{
			unholySeen |= "ZOMBIE".equals(QuestRules.slayTarget(random, many, () -> "ZOMBIE", "A"));
		}
		assertTrue(unholySeen, "the unholy mob is still asked for with many targets");
	}

	@Test
	void rewardsAddUpToAtMostThePower()
	{
		List<Map.Entry<String, Integer>> values = List.of(new SimpleEntry<String, Integer>("DIAMOND", 100), new SimpleEntry<String, Integer>("COAL", 5), new SimpleEntry<String, Integer>("POPPY", 1));
		Map<String, Integer> byName = Map.of("DIAMOND", 100, "COAL", 5, "POPPY", 1);
		Random random = new Random(4);
		for (int i = 0; i < 200; i++)
		{
			int power = random.nextInt(400);
			int total = QuestRules.rollRewards(random, values, power).stream().mapToInt(byName::get).sum();
			assertTrue(total <= power, total + " > " + power);
		}

		assertTrue(QuestRules.rollRewards(random, List.of(), 500).isEmpty());
		assertTrue(QuestRules.rollRewards(random, values, 0).isEmpty());
	}

	@Test
	void rewardsStopAfterTheMaximumRolls()
	{
		// Nothing ever fits: the rolls end anyway
		List<Map.Entry<String, Integer>> values = List.of(new SimpleEntry<String, Integer>("DIAMOND", 1000));
		assertTrue(QuestRules.rollRewards(new Random(5), values, 999).isEmpty());

		// Only 1-point items: at most MAX_REWARD_ROLLS of them
		List<Map.Entry<String, Integer>> cheap = List.of(new SimpleEntry<String, Integer>("POPPY", 1));
		assertEquals(QuestRules.MAX_REWARD_ROLLS, QuestRules.rollRewards(new Random(6), cheap, 10_000).size());
	}

	@Test
	void holyNamesAlternateVowelsAndConsonants()
	{
		Random random = new Random(7);
		for (int i = 0; i < 200; i++)
		{
			String name = QuestRules.holyName(random);
			assertTrue(name.length() >= 2, name);
			assertTrue(Character.isUpperCase(name.charAt(0)), name);
			assertTrue("AOIUYE".indexOf(name.charAt(0)) >= 0, name);
			assertEquals(name.substring(1), name.substring(1).toLowerCase(java.util.Locale.ROOT));
		}
		assertTrue(QuestRules.holyMountainName(random).startsWith("Holy mountain of "));
	}

	@Test
	void pilgrimageSpotsLeaveRoomForTheNeighboursInTheirChunk()
	{
		Random random = new Random(8);
		for (int i = 0; i < 1000; i++)
		{
			int[] spot = QuestRules.pilgrimageSpot(random, -300, 4000, QuestRules.PILGRIMAGE_MIN_DISTANCE, QuestRules.PILGRIMAGE_MAX_DISTANCE);
			int inChunkX = spot[0] & 15;
			int inChunkZ = spot[1] & 15;
			assertTrue(inChunkX >= 1 && inChunkX <= 14);
			assertTrue(inChunkZ >= 1 && inChunkZ <= 14);
			double distance = Math.hypot(spot[0] + 300, spot[1] - 4000);
			assertTrue(distance >= QuestRules.PILGRIMAGE_MIN_DISTANCE - 32 && distance <= QuestRules.PILGRIMAGE_MAX_DISTANCE + 32, "distance " + distance);
		}
		assertEquals(-16 | 3, QuestRules.inChunk(new Random(0) { @Override public int nextInt(int bound) { return 2; } }, -5));
	}

	@Test
	void pilgrimageGroundIsAboveTheSeaAndBelowTheTop()
	{
		assertFalse(QuestRules.isFittingHeight(63, 63, 320));
		assertTrue(QuestRules.isFittingHeight(64, 63, 320));
		assertTrue(QuestRules.isFittingHeight(317, 63, 320));
		assertFalse(QuestRules.isFittingHeight(318, 63, 320));
	}
}
