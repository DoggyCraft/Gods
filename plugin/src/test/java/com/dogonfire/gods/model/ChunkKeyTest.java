package com.dogonfire.gods.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChunkKeyTest
{
	@Test
	void blocksAreInTheChunkTheirCoordinatesDividedBy16RoundedDownGive()
	{
		assertEquals(new ChunkKey("world", 0, 0), ChunkKey.of("world", 0, 15));
		assertEquals(new ChunkKey("world", 1, 0), ChunkKey.of("world", 16, 0));
		assertEquals(new ChunkKey("world", -1, 0), ChunkKey.of("world", -1, 0));
		assertEquals(new ChunkKey("world", -1, -2), ChunkKey.of("world", -16, -17));
	}

	/** The altar of the test server: its block at x=-1 and its sign at x=0 are in different chunks */
	@Test
	void sameChunk()
	{
		assertTrue(ChunkKey.sameChunk(0, 0, 15, 15));
		assertFalse(ChunkKey.sameChunk(0, 0, 16, 0));
		assertFalse(ChunkKey.sameChunk(-1, 0, 0, 0));
	}

	@Test
	void worldNamesWithADotGetAKeyWithout()
	{
		assertEquals("my_world", ChunkKey.of("my.world", 0, 0).world());
	}

	@Test
	void coordinatesAreWrittenAndReadAsXCommaZ()
	{
		ChunkKey chunk = new ChunkKey("world", -3, 12);
		assertEquals("-3,12", chunk.coordinates());
		assertEquals(chunk, ChunkKey.parse("world", "-3,12"));
		assertEquals(chunk, ChunkKey.parse("world", " -3 , 12 "));
		assertEquals(new ChunkKey("world", -2, 13), chunk.offset(1, 1));
		assertNull(ChunkKey.parse("world", "3"));
		assertNull(ChunkKey.parse("world", "3,4,5"));
		assertNull(ChunkKey.parse("world", "a,b"));
		assertNull(ChunkKey.parse("world", null));
		assertNull(ChunkKey.parse(null, "1,2"));
	}
}
