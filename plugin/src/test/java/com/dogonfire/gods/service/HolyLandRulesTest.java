package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.junit.jupiter.api.Test;

class HolyLandRulesTest
{
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
}
