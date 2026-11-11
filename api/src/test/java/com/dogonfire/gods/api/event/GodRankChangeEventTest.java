package com.dogonfire.gods.api.event;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.dogonfire.gods.api.GodRank;

class GodRankChangeEventTest
{
	/** A rise can be cancelled; a fall and the placing of an old god can't */
	@Test
	void onlyRisesCanBeCancelled()
	{
		GodRankChangeEvent rise = new GodRankChangeEvent("Zeus", GodRank.RECOGNIZED, GodRank.DESCENDED, GodRankChangeEvent.Cause.RANK_UP);
		assertTrue(rise.isRise());
		assertTrue(rise.isCancellable());
		rise.setCancelled(true);
		assertTrue(rise.isCancelled());

		GodRankChangeEvent fall = new GodRankChangeEvent("Zeus", GodRank.GREATER, GodRank.SPIRIT, GodRankChangeEvent.Cause.NO_PRIEST);
		assertFalse(fall.isRise());
		assertFalse(fall.isCancellable());
		fall.setCancelled(true);
		assertFalse(fall.isCancelled());

		GodRankChangeEvent seeded = new GodRankChangeEvent("Zeus", GodRank.SPIRIT, GodRank.EXALTED, GodRankChangeEvent.Cause.SEEDED);
		seeded.setCancelled(true);
		assertFalse(seeded.isCancelled());
		assertTrue(new GodRankChangeEvent("Zeus", GodRank.EXALTED, GodRank.SPIRIT, GodRankChangeEvent.Cause.API).isCancellable());
	}
}
