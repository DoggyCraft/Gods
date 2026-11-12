package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.junit.jupiter.api.Test;

import com.dogonfire.gods.model.ChunkKey;

class HolyLandRulesTest
{
	private static ChunkKey chunk(int x, int z)
	{
		return new ChunkKey("world", x, z);
	}

	@Test
	void theWorldsOwnSpawnsAreKeptOut()
	{
		for (SpawnReason reason : new SpawnReason[] { SpawnReason.NATURAL, SpawnReason.CHUNK_GEN, SpawnReason.SPAWNER, SpawnReason.TRIAL_SPAWNER, SpawnReason.JOCKEY, SpawnReason.MOUNT, SpawnReason.PATROL, SpawnReason.RAID, SpawnReason.REINFORCEMENTS, SpawnReason.VILLAGE_INVASION })
		{
			assertTrue(HolyLandRules.isWorldSpawn(reason), reason.name());
		}
	}

	@Test
	void whatPlayersAndPluginsSpawnIsLetIn()
	{
		for (SpawnReason reason : new SpawnReason[] { SpawnReason.CUSTOM, SpawnReason.COMMAND, SpawnReason.SPAWNER_EGG, SpawnReason.DISPENSE_EGG, SpawnReason.EGG, SpawnReason.BREEDING, SpawnReason.BUILD_IRONGOLEM, SpawnReason.BUILD_SNOWMAN, SpawnReason.BUILD_WITHER, SpawnReason.BUCKET, SpawnReason.CURED, SpawnReason.VILLAGE_DEFENSE, SpawnReason.SHOULDER_ENTITY, SpawnReason.DEFAULT })
		{
			assertFalse(HolyLandRules.isWorldSpawn(reason), reason.name());
		}
		assertFalse(HolyLandRules.isWorldSpawn(null));
	}

	/** An altar inside one chunk claims that chunk */
	@Test
	void anAltarInsideAChunkClaimsOne()
	{
		// Block at 5,5, sign east of it at 6,5, the figure stands at 7,5
		assertEquals(List.of(chunk(0, 0)), HolyLandRules.footprint("world", 5, 5, 6, 5));
	}

	/** The test server's altar: block at x=-1, sign at x=0; the spot in front of the sign at x=1 */
	@Test
	void anAltarOnAChunkBorderClaimsBothSides()
	{
		assertEquals(List.of(chunk(-1, 0), chunk(0, 0)), HolyLandRules.footprint("world", -1, 5, 0, 5));
		// Block at 15, sign at 16 facing away: spot at 17, same chunk as the sign
		assertEquals(List.of(chunk(0, 0), chunk(1, 0)), HolyLandRules.footprint("world", 15, 0, 16, 0));
		// Sign at the edge of the block's chunk, the spot over the border
		assertEquals(List.of(chunk(0, 0), chunk(0, 1)), HolyLandRules.footprint("world", 3, 14, 3, 15));
		assertEquals(List.of(chunk(0, -1), chunk(0, -2)), HolyLandRules.footprint("world", 0, -15, 0, -16));
	}

	@Test
	void theSquareOfARadiusIsCenteredOnTheCenter()
	{
		assertEquals(List.of(chunk(3, 4)), HolyLandRules.square(chunk(3, 4), 0));
		assertEquals(List.of(chunk(3, 4)), HolyLandRules.square(chunk(3, 4), -1));
		List<ChunkKey> square = HolyLandRules.square(chunk(3, 4), 1);
		assertEquals(9, square.size());
		assertEquals(chunk(3, 4), square.get(0));
		assertTrue(square.containsAll(List.of(chunk(2, 3), chunk(4, 5), chunk(2, 5), chunk(4, 3))));
		assertEquals(81, Set.copyOf(HolyLandRules.square(chunk(0, 0), 4)).size());
	}

	@Test
	void onlyFreeChunksAreClaimedOnce()
	{
		Set<ChunkKey> taken = Set.of(chunk(1, 0));
		List<ChunkKey> wanted = List.of(chunk(0, 0), chunk(1, 0), chunk(0, 0), chunk(2, 0));
		assertEquals(List.of(chunk(0, 0), chunk(2, 0)), HolyLandRules.claimable(wanted, chunk -> !taken.contains(chunk)));
	}
}
