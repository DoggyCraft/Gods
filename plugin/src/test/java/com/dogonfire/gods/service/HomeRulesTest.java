package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.dogonfire.gods.model.Altar;
import com.dogonfire.gods.model.AltarKind;
import com.dogonfire.gods.model.BlockPos;
import com.dogonfire.gods.model.GodHome;

class HomeRulesTest
{
	// The sign hangs south of the altar block, so the home is one further south
	private static final BlockPos	BLOCK	= new BlockPos("world", 10, 64, 10);
	private static final BlockPos	SIGN	= new BlockPos("world", 10, 64, 11);
	private static final BlockPos	SPOT	= new BlockPos("world", 10, 64, 12);
	private static final Altar		MAIN	= new Altar(SIGN, BLOCK, AltarKind.PRAYING, "Zeus", null, null);
	private static final GodHome	LEGACY	= new GodHome("world", -20.7, 70, 5.2);

	@Test
	void theMainAltarIsTheHome()
	{
		HomeRules.Home home = HomeRules.home(MAIN, LEGACY);
		assertEquals(SPOT, home.spot());
		assertEquals(10.5, home.x());
		assertEquals(12.5, home.z());
		assertEquals(BLOCK, home.altarBlock());
		assertEquals(GodNpcRules.facingYaw(MAIN), home.yaw());
		assertTrue(home.keepClear().containsAll(Set.of(SIGN, BLOCK, BLOCK.up(1), BLOCK.up(2))));
	}

	/** A god without a main altar keeps the home from before main altars */
	@Test
	void withoutAMainAltarTheOldHome()
	{
		HomeRules.Home home = HomeRules.home(null, LEGACY);
		assertEquals(new HomeRules.Home("world", -20.7, 70, 5.2, 0.0F, null, Set.of()), home);
		assertEquals(new BlockPos("world", -21, 70, 5), home.spot());
		assertNull(HomeRules.home(null, null));
		assertNull(HomeRules.home(null, new GodHome(null, 1, 2, 3)));
	}

	@Test
	void theSpotItselfWhenItIsSafe()
	{
		assertEquals(SPOT, HomeRules.safeLanding(SPOT, Set.of(), pos -> true));
	}

	/** Blocked: the nearest safe spot, at the same height before a step up before a step down */
	@Test
	void theNearestSafeSpotWhenBlocked()
	{
		BlockPos east = new BlockPos("world", 11, 64, 12);
		BlockPos up = new BlockPos("world", 10, 65, 12);
		BlockPos down = new BlockPos("world", 10, 63, 12);
		BlockPos far = new BlockPos("world", 12, 64, 12);
		assertEquals(east, HomeRules.safeLanding(SPOT, Set.of(), Set.of(far, down, up, east)::contains));
		assertEquals(up, HomeRules.safeLanding(SPOT, Set.of(), Set.of(far, down, up)::contains));
		assertEquals(down, HomeRules.safeLanding(SPOT, Set.of(), Set.of(far, down)::contains));
		assertEquals(far, HomeRules.safeLanding(SPOT, Set.of(), Set.of(far)::contains));
	}

	/** Never on the altar's sign, its block or the light on top, even if they would hold a player */
	@Test
	void notInTheAltar()
	{
		HomeRules.Home home = HomeRules.home(MAIN, null);
		Set<BlockPos> safe = new HashSet<>(home.keepClear());
		BlockPos aside = new BlockPos("world", 11, 64, 9);
		safe.add(aside);
		assertEquals(aside, HomeRules.safeLanding(home.spot(), home.keepClear(), safe::contains));
	}

	/** Only a few blocks around: none further away, and none at all is null */
	@Test
	void noSafeSpotNearby()
	{
		int r = HomeRules.SEARCH_RADIUS_BLOCKS;
		BlockPos edge = new BlockPos("world", 10 + r, 64 - r, 12 - r);
		assertEquals(edge, HomeRules.safeLanding(SPOT, Set.of(), edge::equals));
		assertNull(HomeRules.safeLanding(SPOT, Set.of(), new BlockPos("world", 10 + r + 1, 64, 12)::equals));
		assertNull(HomeRules.safeLanding(SPOT, Set.of(), pos -> false));
	}

	@Test
	void facesTheAltar()
	{
		// West of the altar block, looking east (-90 in Minecraft's yaw)
		assertEquals(-90.0F, HomeRules.facing(new BlockPos("world", 8, 64, 10), BLOCK), 0.001F);
	}
}
