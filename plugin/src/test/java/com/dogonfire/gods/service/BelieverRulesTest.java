package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.dogonfire.gods.model.Believer;

class BelieverRulesTest
{
	private static final Instant	NOW	= Instant.parse("2024-05-01T12:00:00Z");

	private static Believer believer(String god, int prayers, Duration sinceLastPrayer)
	{
		Believer believer = new Believer(UUID.randomUUID());
		believer.setGodName(god);
		believer.setPrayers(prayers);
		believer.setLastPrayer(NOW.minus(sinceLastPrayer));
		return believer;
	}

	@Test
	void aPrayerCountsOnlyAfterTheMinimumTime()
	{
		assertFalse(BelieverRules.canPray(believer("Thor", 3, Duration.ofMinutes(4)), "Thor", NOW, 5));
		assertTrue(BelieverRules.canPray(believer("Thor", 3, Duration.ofMinutes(5)), "Thor", NOW, 5));
		assertTrue(BelieverRules.canPray(null, "Thor", NOW, 5), "never prayed");
		assertTrue(BelieverRules.canPray(believer("Odin", 3, Duration.ofMinutes(1)), "Thor", NOW, 5), "another god");
	}

	@Test
	void prayingCountsUpAndStartsOverForANewGod()
	{
		Believer believer = believer("Thor", 3, Duration.ofHours(1));
		believer.setJoined(NOW.minus(Duration.ofDays(3)));
		BelieverRules.recordPrayer(believer, "Thor", 1, NOW, true);
		assertEquals(4, believer.prayers());
		assertEquals(NOW, believer.lastPrayer());
		assertEquals(NOW.minus(Duration.ofDays(3)), believer.joined(), "same god: joined stays");

		BelieverRules.recordPrayer(believer, "Odin", 2, NOW, true);
		assertEquals("Odin", believer.godName());
		assertEquals(2, believer.prayers());
		assertEquals(NOW, believer.joined());

		Believer other = believer("Thor", 3, Duration.ofHours(1));
		BelieverRules.recordPrayer(other, "Odin", 2, NOW, false);
		assertNull(other.joined(), "increasePrayer doesn't record the join");
	}

	@Test
	void minutesUntilTheNextPrayer()
	{
		assertEquals(3, BelieverRules.minutesUntilCanPray(NOW.minus(Duration.ofSeconds(90)), NOW, 5));
		assertTrue(BelieverRules.minutesUntilCanPray(null, NOW, 5) < 0);
	}

	@Test
	void recentPrayerIncludesTheLimit()
	{
		assertTrue(BelieverRules.hasRecentPrayer(NOW.minus(Duration.ofMinutes(5)), NOW, 5));
		assertFalse(BelieverRules.hasRecentPrayer(NOW.minus(Duration.ofMinutes(6)), NOW, 5));
		assertFalse(BelieverRules.hasRecentPrayer(null, NOW, 5));
	}

	@Test
	void invitationsAndGodChangesExpire()
	{
		assertFalse(BelieverRules.invitationExpired(NOW.minusSeconds(30), NOW));
		assertTrue(BelieverRules.invitationExpired(NOW.minusSeconds(31), NOW));
		assertTrue(BelieverRules.invitationExpired(null, NOW));

		assertTrue(BelieverRules.isChangingGod(NOW.minusSeconds(10), NOW));
		assertFalse(BelieverRules.isChangingGod(NOW.minusSeconds(11), NOW));
		assertFalse(BelieverRules.isChangingGod(null, NOW));

		assertTrue(BelieverRules.joinedRecently(NOW.minusSeconds(100), NOW, 100));
		assertFalse(BelieverRules.joinedRecently(NOW.minusSeconds(101), NOW, 100));
		assertFalse(BelieverRules.joinedRecently(null, NOW, Integer.MAX_VALUE), "unknown join time is never recent");
	}

	@Test
	void believerPowerFadesWithTime()
	{
		assertEquals(10.0F, BelieverRules.believerPower(10, NOW, NOW));
		assertTrue(BelieverRules.believerPower(10, NOW.minus(Duration.ofDays(30)), NOW) < 10.0F);
	}
}
