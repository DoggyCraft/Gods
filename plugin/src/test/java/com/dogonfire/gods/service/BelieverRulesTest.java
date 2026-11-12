package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.config.TestConfigs;
import com.dogonfire.gods.model.Believer;
import com.dogonfire.gods.model.DevotionLevel;

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

	// ---------------------------------------------------------------------
	// Prayer power decay

	private static final Settings.Believers	BELIEVERS	= TestConfigs.defaultBelievers();

	private static BelieverRules.Decay decay(int power, Duration sinceLastPrayer, Instant decayedUntil, DevotionLevel level)
	{
		return BelieverRules.decay(BELIEVERS, power, NOW.minus(sinceLastPrayer), decayedUntil, BelieverRules.quietPeriod(BELIEVERS, level), NOW);
	}

	@Test
	void devotedBelieversWaitLongerBeforeTheirPowerDecays()
	{
		assertEquals(Duration.ofDays(3), BelieverRules.quietPeriod(BELIEVERS, DevotionLevel.FOLLOWER));
		assertEquals(Duration.ofDays(3), BelieverRules.quietPeriod(BELIEVERS, null));
		assertEquals(Duration.ofHours(108), BelieverRules.quietPeriod(BELIEVERS, DevotionLevel.DEVOTEE));
		assertEquals(Duration.ofDays(6), BelieverRules.quietPeriod(BELIEVERS, DevotionLevel.ZEALOT));
		assertEquals(Duration.ofDays(9), BelieverRules.quietPeriod(BELIEVERS, DevotionLevel.SAINT));
	}

	@Test
	void eachDayTakesAPercentRoundedUpAndThenAFixedAmount()
	{
		assertEquals(16, BelieverRules.decayOneDay(20, 2, 10));
		assertEquals(2, BelieverRules.decayOneDay(5, 2, 10));
		assertEquals(0, BelieverRules.decayOneDay(3, 2, 10));
		assertEquals(0, BelieverRules.decayOneDay(0, 2, 10));
		assertEquals(4, BelieverRules.decayOneDay(5, 0, 10), "a percent always takes at least one");
		assertEquals(5, BelieverRules.decayOneDay(5, 0, 0));
	}

	@Test
	void nothingDecaysWithinTheQuietPeriod()
	{
		BelieverRules.Decay decay = decay(20, Duration.ofDays(3).minusMinutes(1), null, DevotionLevel.FOLLOWER);
		assertEquals(new BelieverRules.Decay(20, null, false), decay);
		// Nor in the first day after it
		assertEquals(new BelieverRules.Decay(20, null, false), decay(20, Duration.ofDays(4).minusMinutes(1), null, DevotionLevel.FOLLOWER));
		// A saint waits three times as long
		assertEquals(new BelieverRules.Decay(20, null, false), decay(20, Duration.ofDays(9).minusMinutes(1), null, DevotionLevel.SAINT));
	}

	@Test
	void decaysPerWholeDayAfterTheQuietPeriodAndCountsEachDayOnce()
	{
		Instant lastPrayer = NOW.minus(Duration.ofDays(5).plusHours(6));
		BelieverRules.Decay decay = decay(20, Duration.ofDays(5).plusHours(6), null, DevotionLevel.FOLLOWER);
		assertEquals(12, decay.power());
		assertEquals(lastPrayer.plus(Duration.ofDays(5)), decay.decayedUntil());
		assertFalse(decay.lost());

		// The same days again change nothing
		BelieverRules.Decay again = BelieverRules.decay(BELIEVERS, decay.power(), lastPrayer, decay.decayedUntil(), Duration.ofDays(3), NOW);
		assertEquals(decay, again);
		// A day later one more day decays
		BelieverRules.Decay later = BelieverRules.decay(BELIEVERS, decay.power(), lastPrayer, decay.decayedUntil(), Duration.ofDays(3), NOW.plus(Duration.ofDays(1)));
		assertEquals(8, later.power());
	}

	@Test
	void aFollowerWith20PowerLeavesNineDaysAfterTheLastPrayerASaintFifteen()
	{
		assertFalse(decay(20, Duration.ofDays(9).minusMinutes(1), null, DevotionLevel.FOLLOWER).lost());
		BelieverRules.Decay gone = decay(20, Duration.ofDays(9), null, DevotionLevel.FOLLOWER);
		assertEquals(0, gone.power());
		assertTrue(gone.lost());

		assertFalse(decay(20, Duration.ofDays(15).minusMinutes(1), null, DevotionLevel.SAINT).lost());
		assertTrue(decay(20, Duration.ofDays(15), null, DevotionLevel.SAINT).lost());
	}

	/** The user's rule: 0 prayer power alone never makes a believer leave; only the decay of an inactive one does */
	@Test
	void zeroPowerAloneNeverMakesABelieverLeave()
	{
		// Prays regularly and spends everything on /g prayfor
		assertEquals(new BelieverRules.Decay(0, null, false), decay(0, Duration.ofHours(1), null, DevotionLevel.FOLLOWER));
		assertFalse(decay(0, Duration.ofDays(3), null, DevotionLevel.FOLLOWER).lost(), "the quiet period has just ended");
		assertFalse(decay(0, Duration.ofDays(4).minusMinutes(1), null, DevotionLevel.FOLLOWER).lost(), "no day of decay yet");
		// Inactive past the quiet period: the first day of decay finds nothing left
		assertTrue(decay(0, Duration.ofDays(4), null, DevotionLevel.FOLLOWER).lost());
		// The same believer who prays again is safe again, however little power they have
		Instant decayedUntil = NOW.minus(Duration.ofDays(1));
		assertFalse(decay(0, Duration.ofMinutes(10), decayedUntil, DevotionLevel.FOLLOWER).lost());
	}

	@Test
	void aPrayerStopsTheDecay()
	{
		// Decayed a while ago, then prayed: decay starts only after a new quiet period
		Instant decayedUntil = NOW.minus(Duration.ofDays(2));
		BelieverRules.Decay decay = decay(7, Duration.ofDays(2), decayedUntil, DevotionLevel.FOLLOWER);
		assertEquals(new BelieverRules.Decay(7, decayedUntil, false), decay);
	}

	@Test
	void withoutDecayNobodyLeaves()
	{
		Settings.Believers none = new Settings.Believers(false, 300, 3, BELIEVERS.decayMultipliers(), 0, 0);
		BelieverRules.Decay decay = BelieverRules.decay(none, 0, NOW.minus(Duration.ofDays(400)), null, Duration.ofDays(3), NOW);
		assertEquals(new BelieverRules.Decay(0, null, false), decay);
	}

	@Test
	void aMissingLastPrayerIsLongAgo()
	{
		BelieverRules.Decay decay = BelieverRules.decay(BELIEVERS, 50, null, null, Duration.ofDays(3), NOW);
		assertEquals(0, decay.power());
		assertTrue(decay.lost());
	}
}
