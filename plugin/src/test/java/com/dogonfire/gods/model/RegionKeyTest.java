package com.dogonfire.gods.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegionKeyTest
{
	@Test
	void regionsAre128BlocksWide()
	{
		assertEquals(RegionKey.hash(0, 0), RegionKey.hash(127, 127));
		assertNotEquals(RegionKey.hash(0, 0), RegionKey.hash(128, 0));
		assertNotEquals(RegionKey.hash(0, 0), RegionKey.hash(0, 128));
		assertNotEquals(RegionKey.hash(0, 0), RegionKey.hash(-1, 0));
	}

	@Test
	void theHashPacksRegionXAndZ()
	{
		assertEquals(0L, RegionKey.hash(0, 0));
		assertEquals((1L << 32) | 2L, RegionKey.hash(128, 256));
		// Negative coordinates round down; Z doesn't spill into X
		assertEquals(-1L, RegionKey.hash(-1, -1));
		assertEquals(0xFFFFFFFFL, RegionKey.hash(0, -1));

		RegionKey key = RegionKey.of("world", -129, 300);
		assertEquals(-2, key.regionX());
		assertEquals(2, key.regionZ());
	}

	@Test
	void worldNamesLoseTheirDots()
	{
		assertEquals("my_world", RegionKey.of("my.world", 0, 0).world());
		assertEquals(RegionKey.of("my.world", 1, 1), new RegionKey("my_world", 0L));
	}

	@Test
	void sameRegion()
	{
		assertTrue(RegionKey.sameRegion(0, 0, 127, 127));
		assertFalse(RegionKey.sameRegion(0, 0, 128, 0));
		assertFalse(RegionKey.sameRegion(-1, 0, 0, 0));
	}
}
