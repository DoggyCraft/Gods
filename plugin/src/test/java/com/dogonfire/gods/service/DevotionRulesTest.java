package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.config.TestConfigs;
import com.dogonfire.gods.model.DevotionLevel;

class DevotionRulesTest
{
	private static final Settings.Devotion	DEVOTION	= TestConfigs.defaultDevotion();
	private static final Instant			NOW			= Instant.parse("2026-11-12T12:00:00Z");

	private static Instant daysAgo(double days)
	{
		return NOW.minus(Duration.ofMinutes((long) (days * 24 * 60)));
	}

	@Test
	void theDefaultsAreAchievable()
	{
		// A devotee after 15 prayers (a few days of regular prayer), a zealot after 60, a saint takes 200 or deeds
		assertEquals(DevotionLevel.FOLLOWER, DevotionRules.levelFor(DEVOTION, 14 * DEVOTION.points().prayer()));
		assertEquals(DevotionLevel.DEVOTEE, DevotionRules.levelFor(DEVOTION, 15 * DEVOTION.points().prayer()));
		assertEquals(DevotionLevel.ZEALOT, DevotionRules.levelFor(DEVOTION, 60 * DEVOTION.points().prayer()));
		assertEquals(DevotionLevel.SAINT, DevotionRules.levelFor(DEVOTION, 2000));
		assertEquals(DevotionLevel.FOLLOWER, DevotionRules.levelFor(DEVOTION, 0));
	}

	@Test
	void aBelieverRisesAsSoonAsTheyHaveThePoints()
	{
		assertEquals(DevotionLevel.DEVOTEE, DevotionRules.evaluate(DEVOTION, DevotionLevel.FOLLOWER, 150));
		assertEquals(DevotionLevel.FOLLOWER, DevotionRules.evaluate(DEVOTION, DevotionLevel.FOLLOWER, 149));
		// Several levels at once when the points allow
		assertEquals(DevotionLevel.SAINT, DevotionRules.evaluate(DEVOTION, DevotionLevel.FOLLOWER, 5000));
		assertEquals(DevotionLevel.ZEALOT, DevotionRules.evaluate(DEVOTION, null, 600));
	}

	@Test
	void aLevelIsKeptUntilRealNeglect()
	{
		// 75 % of 2000
		assertEquals(1500, DevotionRules.keepThreshold(DEVOTION, DevotionLevel.SAINT));
		assertEquals(DevotionLevel.SAINT, DevotionRules.evaluate(DEVOTION, DevotionLevel.SAINT, 1999));
		assertEquals(DevotionLevel.SAINT, DevotionRules.evaluate(DEVOTION, DevotionLevel.SAINT, 1500));
		// Below that, to the level the points reach
		assertEquals(DevotionLevel.ZEALOT, DevotionRules.evaluate(DEVOTION, DevotionLevel.SAINT, 1499));
		assertEquals(DevotionLevel.FOLLOWER, DevotionRules.evaluate(DEVOTION, DevotionLevel.DEVOTEE, 100));
		assertEquals(DevotionLevel.DEVOTEE, DevotionRules.evaluate(DEVOTION, DevotionLevel.DEVOTEE, 113));
		// Followers have nothing to lose
		assertEquals(0, DevotionRules.keepThreshold(DEVOTION, DevotionLevel.FOLLOWER));
	}

	@Test
	void devotionFadesOnlyAfterTheGracePeriod()
	{
		// Three days without prayer: nothing yet
		DevotionRules.Faded faded = DevotionRules.fade(DEVOTION, 1000, daysAgo(3.9), null, NOW);
		assertEquals(1000, faded.points());
		assertNull(faded.fadedUntil());
		// One whole day after that: 5 % less
		faded = DevotionRules.fade(DEVOTION, 1000, daysAgo(4.5), null, NOW);
		assertEquals(950, faded.points());
		assertEquals(daysAgo(4.5).plus(Duration.ofDays(4)), faded.fadedUntil());
		// Counted only once: until a whole day after that, nothing more fades
		DevotionRules.Faded again = DevotionRules.fade(DEVOTION, faded.points(), daysAgo(4.5), faded.fadedUntil(), NOW.plus(Duration.ofHours(11)));
		assertEquals(950, again.points());
		// Compounded per day
		assertEquals((int) Math.floor(1000 * Math.pow(0.95, 5)), DevotionRules.fade(DEVOTION, 1000, daysAgo(8.2), null, NOW).points());
	}

	@Test
	void aSaintFallsAboutNineDaysAfterTheirLastPrayer()
	{
		DevotionLevel level = DevotionLevel.SAINT;
		int points = 2000;
		Instant fadedUntil = null;
		Instant lastPrayer = NOW;
		int day = 0;
		while (level == DevotionLevel.SAINT && day < 30)
		{
			day++;
			DevotionRules.Faded faded = DevotionRules.fade(DEVOTION, points, lastPrayer, fadedUntil, NOW.plus(Duration.ofDays(day)));
			points = faded.points();
			fadedUntil = faded.fadedUntil();
			level = DevotionRules.evaluate(DEVOTION, level, points);
		}
		assertEquals(9, day);
		assertEquals(DevotionLevel.ZEALOT, level);
	}

	@Test
	void nothingFadesWithoutAKnownPrayerOrWithFadingOff()
	{
		assertEquals(1000, DevotionRules.fade(DEVOTION, 1000, null, null, NOW).points());
		assertEquals(0, DevotionRules.fade(DEVOTION, 0, daysAgo(30), null, NOW).points());
		Settings.Devotion never = new Settings.Devotion(true, DEVOTION.points(), DEVOTION.levels(), 10, 3, 0, 75, 0, DEVOTION.unlocks(), 25, 2, true);
		assertEquals(1000, DevotionRules.fade(never, 1000, daysAgo(30), null, NOW).points());
	}

	@Test
	void leavingKeepsAFraction()
	{
		assertEquals(0, DevotionRules.afterLeaving(DEVOTION, 1234));
		Settings.Devotion half = new Settings.Devotion(true, DEVOTION.points(), DEVOTION.levels(), 10, 3, 5, 75, 50, DEVOTION.unlocks(), 25, 2, true);
		assertEquals(617, DevotionRules.afterLeaving(half, 1234));
	}

	@Test
	void believersFromBeforeDevotionStartFromTheirPrayers()
	{
		assertEquals(0, DevotionRules.seed(DEVOTION, true, 0));
		assertEquals(0, DevotionRules.seed(DEVOTION, false, 99));
		assertEquals(150, DevotionRules.seed(DEVOTION, true, 15));
		// At most a zealot: saints are earned
		assertEquals(600, DevotionRules.seed(DEVOTION, true, 500));
		assertEquals(DevotionLevel.ZEALOT, DevotionRules.levelFor(DEVOTION, DevotionRules.seed(DEVOTION, true, 10000)));
	}

	@Test
	void pointsToGo()
	{
		assertEquals(50, DevotionRules.pointsToGo(DEVOTION, DevotionLevel.DEVOTEE, 100));
		assertEquals(0, DevotionRules.pointsToGo(DEVOTION, DevotionLevel.DEVOTEE, 200));
		assertEquals(0, DevotionRules.pointsToGo(DEVOTION, null, 200));
	}

	@Test
	void unlocks()
	{
		assertTrue(DevotionRules.unlocked(DevotionLevel.SAINT, DevotionLevel.ZEALOT));
		assertTrue(DevotionRules.unlocked(DevotionLevel.ZEALOT, DevotionLevel.ZEALOT));
		assertFalse(DevotionRules.unlocked(DevotionLevel.DEVOTEE, DevotionLevel.ZEALOT));
		assertFalse(DevotionRules.unlocked(null, DevotionLevel.DEVOTEE));
		assertTrue(DevotionRules.unlocked(null, DevotionLevel.FOLLOWER));
	}

	@Test
	void theDiscountRoundsDown()
	{
		assertEquals(8, DevotionRules.discountedCost(10, 25));
		assertEquals(15, DevotionRules.discountedCost(20, 25));
		assertEquals(38, DevotionRules.discountedCost(50, 25));
		assertEquals(50, DevotionRules.discountedCost(50, 0));
		assertEquals(0, DevotionRules.discountedCost(50, 100));
	}

	@Test
	void blessingsGetLikelier()
	{
		assertEquals(25, DevotionRules.blessingBound(50, 2));
		assertEquals(50, DevotionRules.blessingBound(50, 1));
		assertEquals(1, DevotionRules.blessingBound(1, 2));
		assertEquals(1, DevotionRules.blessingBound(3, 10));
	}

	@Test
	void commandmentsEarnOncePerCooldown()
	{
		assertTrue(DevotionRules.commandmentReady(null, NOW, 10));
		assertFalse(DevotionRules.commandmentReady(NOW.minus(Duration.ofMinutes(9)), NOW, 10));
		assertTrue(DevotionRules.commandmentReady(NOW.minus(Duration.ofMinutes(10)), NOW, 10));
		assertTrue(DevotionRules.commandmentReady(NOW, NOW, 0));
	}

	@Test
	void godsPreferTheMostDevotedPriests()
	{
		List<DevotionRules.PriestCandidate<String>> candidates = List.of(new DevotionRules.PriestCandidate<>("strong follower", DevotionLevel.FOLLOWER, 90F),
				new DevotionRules.PriestCandidate<>("weak zealot", DevotionLevel.ZEALOT, 5F), new DevotionRules.PriestCandidate<>("strong zealot", DevotionLevel.ZEALOT, 50F),
				new DevotionRules.PriestCandidate<>("devotee", DevotionLevel.DEVOTEE, 70F));
		assertEquals(List.of("strong zealot", "weak zealot", "devotee", "strong follower"),
				DevotionRules.priestOrder(candidates).stream().map(DevotionRules.PriestCandidate::id).toList());

		// Without a saint: one of the three first, at random
		for (int seed = 0; seed < 20; seed++)
		{
			String picked = DevotionRules.pickPriest(candidates, DevotionLevel.SAINT, new Random(seed));
			assertTrue(List.of("strong zealot", "weak zealot", "devotee").contains(picked), picked);
		}
		// A saint is always asked first
		List<DevotionRules.PriestCandidate<String>> withSaint = new java.util.ArrayList<>(candidates);
		withSaint.add(new DevotionRules.PriestCandidate<>("saint", DevotionLevel.SAINT, 1F));
		for (int seed = 0; seed < 20; seed++)
		{
			assertEquals("saint", DevotionRules.pickPriest(withSaint, DevotionLevel.SAINT, new Random(seed)));
		}
		// Devotion off: no level is always preferred
		java.util.Set<String> picked = new java.util.HashSet<>();
		for (int seed = 0; seed < 50; seed++)
		{
			picked.add(DevotionRules.pickPriest(withSaint, null, new Random(seed)));
		}
		assertTrue(picked.size() > 1, "picked " + picked);
		assertNull(DevotionRules.pickPriest(List.<DevotionRules.PriestCandidate<String>>of(), DevotionLevel.SAINT, new Random()));
	}
}
