package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.service.DescentRules.Change;
import com.dogonfire.gods.service.DescentRules.Facts;
import com.dogonfire.gods.service.DescentRules.Outcome;
import com.dogonfire.gods.service.DescentRules.State;

class DescentRulesTest
{
	/** config.yml's defaults */
	private static final Settings.RankConditions	RULES	= descend(true, 3, 24, 0);
	private static final Instant			NOW		= Instant.parse("2026-11-11T12:00:00Z");
	private static final State				NOTHING	= new State(null, null, null);

	private static Settings.RankConditions descend(boolean priest, int believers, int hoursAfterPriest, int power)
	{
		return new Settings.RankConditions(believers, power, priest, null, false, hoursAfterPriest, 0, 0, 0, 0, 0, 0, 0);
	}

	private static Instant hoursAgo(long hours)
	{
		return NOW.minus(Duration.ofHours(hours));
	}

	private static Facts facts(boolean priest, int believers)
	{
		return new Facts(priest, believers, 0F, true, NOW);
	}

	@Test
	void aFirstPriestStartsTheClock()
	{
		Outcome outcome = DescentRules.check(RULES, 24, NOTHING, facts(true, 5));
		assertEquals(Change.NONE, outcome.change());
		assertEquals(NOW, outcome.state().priestSince());
		// It keeps counting from then, and stops when the priest is gone
		assertEquals(hoursAgo(3), DescentRules.check(RULES, 24, new State(hoursAgo(3), null, null), facts(true, 5)).state().priestSince());
		assertNull(DescentRules.check(RULES, 24, new State(hoursAgo(3), null, null), facts(false, 5)).state().priestSince());
	}

	@Test
	void itDescendsWhenEverythingHolds()
	{
		Outcome outcome = DescentRules.check(RULES, 24, new State(hoursAgo(24), null, null), facts(true, 3));
		assertEquals(Change.DESCEND, outcome.change());
		assertEquals(new State(hoursAgo(24), NOW, null), outcome.state());
	}

	@Test
	void itWaitsForEachCondition()
	{
		State priestLongAgo = new State(hoursAgo(30), null, null);
		// The priest is too new
		assertEquals(Change.NONE, DescentRules.check(RULES, 24, new State(hoursAgo(23), null, null), facts(true, 3)).change());
		// Too few believers
		assertEquals(Change.NONE, DescentRules.check(RULES, 24, priestLongAgo, facts(true, 2)).change());
		// No main altar to descend to
		assertEquals(Change.NONE, DescentRules.check(RULES, 24, priestLongAgo, new Facts(true, 5, 0F, false, NOW)).change());
		// Not enough power
		Settings.RankConditions power = descend(true, 3, 24, 100);
		assertEquals(Change.NONE, DescentRules.check(power, 24, priestLongAgo, new Facts(true, 5, 99F, true, NOW)).change());
		assertEquals(Change.DESCEND, DescentRules.check(power, 24, priestLongAgo, new Facts(true, 5, 100F, true, NOW)).change());
	}

	@Test
	void withoutAPriestRequiredOnlyBelieversCount()
	{
		Settings.RankConditions noPriest = descend(false, 3, 24, 0);
		assertEquals(Change.DESCEND, DescentRules.check(noPriest, 24, NOTHING, facts(false, 3)).change());
		// And it doesn't ascend for lack of one
		assertEquals(Change.NONE, DescentRules.check(noPriest, 24, new State(null, hoursAgo(5), null), facts(false, 3)).change());
	}

	@Test
	void itAscendsAtOnceWithoutAPriest()
	{
		Outcome outcome = DescentRules.check(RULES, 24, new State(hoursAgo(50), hoursAgo(20), null), facts(false, 10));
		assertEquals(Change.ASCEND, outcome.change());
		assertEquals(NOTHING, outcome.state());
	}

	/** Too few believers for a while is tolerated; for the whole grace period it ascends */
	@Test
	void tooFewBelieversMakeItAscendAfterTheGracePeriod()
	{
		State down = new State(hoursAgo(50), hoursAgo(20), null);
		Outcome first = DescentRules.check(RULES, 24, down, facts(true, 1));
		assertEquals(Change.NONE, first.change());
		assertEquals(NOW, first.state().ascendingSince());

		State fading = new State(hoursAgo(50), hoursAgo(20), hoursAgo(23));
		assertEquals(Change.NONE, DescentRules.check(RULES, 24, fading, facts(true, 2)).change());
		// Enough believers again: the count starts over
		assertEquals(new State(hoursAgo(50), hoursAgo(20), null), DescentRules.check(RULES, 24, fading, facts(true, 3)).state());

		Outcome gone = DescentRules.check(RULES, 24, new State(hoursAgo(50), hoursAgo(30), hoursAgo(24)), facts(true, 2));
		assertEquals(Change.ASCEND, gone.change());
		assertEquals(new State(hoursAgo(50), null, null), gone.state());
	}

	/** Losing the main altar doesn't make it ascend */
	@Test
	void itStaysDownWithoutAMainAltar()
	{
		assertEquals(Change.NONE, DescentRules.check(RULES, 24, new State(hoursAgo(50), hoursAgo(20), null), new Facts(true, 5, 0F, false, NOW)).change());
	}
}
