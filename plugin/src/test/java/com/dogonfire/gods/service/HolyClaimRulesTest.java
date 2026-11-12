package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.dogonfire.gods.model.BlockPos;
import com.dogonfire.gods.model.ChunkKey;
import com.dogonfire.gods.service.HolyClaimRules.Answer;
import com.dogonfire.gods.service.HolyClaimRules.ClaimInfo;
import com.dogonfire.gods.service.HolyClaimRules.Loss;

class HolyClaimRulesTest
{
	private static final UUID	PRIEST	= UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID	OTHER	= UUID.fromString("00000000-0000-0000-0000-000000000002");

	/** A top-level claim of the priest from 0,0 to 20,20 in world */
	private static ClaimInfo claim()
	{
		return new ClaimInfo(42, PRIEST, false, true, "world", 0, 0, 20, 20);
	}

	@Test
	void theAreaIsTheClaimsAtAnyHeightCornersIncluded()
	{
		ClaimInfo claim = claim();
		assertTrue(claim.contains("world", 0, 0));
		assertTrue(claim.contains("world", 20, 20));
		assertTrue(claim.contains(new BlockPos("world", 10, -60, 10)));
		assertTrue(claim.contains(new BlockPos("world", 10, 300, 10)));
		assertFalse(claim.contains("world", 21, 10));
		assertFalse(claim.contains("world", 10, -1));
		assertFalse(claim.contains("world_nether", 10, 10));
		assertFalse(claim.contains((BlockPos) null));
	}

	@Test
	void aPriestClaimsTheirOwnClaimWithAnAltarInIt()
	{
		assertEquals(Answer.CLAIM, HolyClaimRules.decide(claim(), PRIEST, "Thor", null, true, 2, 3));
	}

	@Test
	void onlyAWholeClaimOfTheirOwnCanBeHolyLand()
	{
		assertEquals(Answer.NO_CLAIM, HolyClaimRules.decide(null, PRIEST, "Thor", null, true, 0, 3));
		assertEquals(Answer.SUBCLAIM, HolyClaimRules.decide(new ClaimInfo(43, PRIEST, false, false, "world", 0, 0, 5, 5), PRIEST, "Thor", null, true, 0, 3));
		assertEquals(Answer.ADMIN_CLAIM, HolyClaimRules.decide(new ClaimInfo(44, null, true, true, "world", 0, 0, 5, 5), PRIEST, "Thor", null, true, 0, 3));
		assertEquals(Answer.NOT_OWNER, HolyClaimRules.decide(claim(), OTHER, "Thor", null, true, 0, 3));
	}

	@Test
	void theGodsOwnClaimLandIsReleasedAnotherGodsRefused()
	{
		assertEquals(Answer.RELEASE, HolyClaimRules.decide(claim(), PRIEST, "Thor", "Thor", true, 3, 3), "even at the limit");
		assertEquals(Answer.RELEASE, HolyClaimRules.decide(claim(), PRIEST, "Thor", "Thor", false, 1, 3), "even without an altar");
		assertEquals(Answer.OTHER_GOD, HolyClaimRules.decide(claim(), PRIEST, "Thor", "Odin", true, 0, 3));
		assertEquals(Answer.NOT_OWNER, HolyClaimRules.decide(claim(), OTHER, "Thor", "Thor", true, 0, 3), "only the owner releases it");
	}

	@Test
	void anAltarMustStandInItAndItCountsAsOneLand()
	{
		assertEquals(Answer.NO_ALTAR, HolyClaimRules.decide(claim(), PRIEST, "Thor", null, false, 0, 3));
		assertEquals(Answer.AT_LIMIT, HolyClaimRules.decide(claim(), PRIEST, "Thor", null, true, 3, 3));
		assertEquals(Answer.AT_LIMIT, HolyClaimRules.decide(claim(), PRIEST, "Thor", null, true, 4, 3));
		assertEquals(Answer.CLAIM, HolyClaimRules.decide(claim(), PRIEST, "Thor", null, true, 2, 3));
	}

	@Test
	void aClaimLandGoesWithItsLastAltar()
	{
		ClaimInfo claim = claim();
		BlockPos inside = new BlockPos("world", 5, 64, 5);
		BlockPos outside = new BlockPos("world", 50, 64, 50);
		assertTrue(HolyClaimRules.goesWithAltar("Thor", "Thor", claim, List.of()));
		assertTrue(HolyClaimRules.goesWithAltar("Thor", "Thor", claim, List.of(outside)));
		assertFalse(HolyClaimRules.goesWithAltar("Thor", "Thor", claim, List.of(outside, inside)), "another altar of the god stands in it");
		assertFalse(HolyClaimRules.goesWithAltar("Odin", "Thor", claim, List.of()), "another god's altar");
		assertFalse(HolyClaimRules.goesWithAltar("Thor", null, claim, List.of()));
		assertFalse(HolyClaimRules.goesWithAltar("Thor", "Thor", null, List.of()), "a claim GriefPrevention doesn't know isn't judged here");
	}

	@Test
	void aClaimLandIsLostWithItsClaim()
	{
		assertNull(HolyClaimRules.check(claim(), null));
		assertNull(HolyClaimRules.check(claim(), true));
		assertEquals(Loss.CLAIM_GONE, HolyClaimRules.check(null, null));
		assertEquals(Loss.CLAIM_GONE, HolyClaimRules.check(new ClaimInfo(43, PRIEST, false, false, "world", 0, 0, 5, 5), null));
		assertEquals(Loss.NEW_OWNER, HolyClaimRules.check(new ClaimInfo(42, null, true, true, "world", 0, 0, 20, 20), null));
		assertEquals(Loss.NO_ALTAR, HolyClaimRules.check(claim(), false), "resized away from its altars");
	}

	@Test
	void aNewOwnerKeepsItOnlyAsAPriestOfItsGod()
	{
		assertTrue(HolyClaimRules.keptByNewOwner(OTHER, true));
		assertFalse(HolyClaimRules.keptByNewOwner(OTHER, false));
		assertFalse(HolyClaimRules.keptByNewOwner(null, true), "an admin claim");
	}

	@Test
	void anyInsideLooksAtEveryBlock()
	{
		assertFalse(HolyClaimRules.anyInside(null, List.of(new BlockPos("world", 1, 1, 1))));
		assertFalse(HolyClaimRules.anyInside(claim(), List.of()));
		assertTrue(HolyClaimRules.anyInside(claim(), List.of(new BlockPos("world", 99, 1, 1), new BlockPos("world", 1, 1, 1))));
	}

	/** A claim takes the place of the god's land when every altar of that land stands in the claim */
	@Test
	void aClaimReplacesTheLandOfItsAltars()
	{
		// The land of chunks 0,0 to 1,1 (blocks 0..31); the claim covers blocks 0..20
		java.util.Set<ChunkKey> land = java.util.Set.of(new ChunkKey("world", 0, 0), new ChunkKey("world", 1, 0), new ChunkKey("world", 0, 1), new ChunkKey("world", 1, 1));
		BlockPos inside = new BlockPos("world", 5, 64, 5);
		BlockPos outsideClaimInLand = new BlockPos("world", 25, 64, 25);
		BlockPos elsewhere = new BlockPos("world", 500, 64, 500);

		assertTrue(HolyClaimRules.replaces(claim(), land, List.of(inside, elsewhere)));
		// Another altar of the land stands outside the claim: the land stays
		assertFalse(HolyClaimRules.replaces(claim(), land, List.of(inside, outsideClaimInLand)));
		// A land without an altar of the god isn't the claim's to replace
		assertFalse(HolyClaimRules.replaces(claim(), land, List.of(elsewhere)));
		assertFalse(HolyClaimRules.replaces(null, land, List.of(inside)));
	}
}
