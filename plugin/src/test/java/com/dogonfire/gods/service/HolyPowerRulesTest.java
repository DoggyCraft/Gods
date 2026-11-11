package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import com.dogonfire.gods.model.HolyPower;
import com.dogonfire.gods.service.HolyPowerRules.PowerDescription;

class HolyPowerRulesTest
{
	@Test
	void everyPowerIsReadBackFromItsDescription()
	{
		for (HolyPower power : HolyPower.values())
		{
			for (int value : new int[] { 1, 2, 7, 30 })
			{
				PowerDescription read = HolyPowerRules.parse(HolyPowerRules.describe(power, value));
				assertEquals(power, read.power(), HolyPowerRules.describe(power, value));
				// The ice spike's text has no number: it reads as 1
				assertEquals(power == HolyPower.ICE ? 1 : value, read.value(), HolyPowerRules.describe(power, value));
			}
		}
	}

	@Test
	void legacyLoreIsReadWithoutItsColors()
	{
		// The third lore line of artifacts made before the tags: green, then the description
		assertEquals(new PowerDescription(HolyPower.CALLMOON, 1), parseLore(ChatColor.GREEN + "Calls the moon for 1 min"));
		assertEquals(new PowerDescription(HolyPower.TAME, 1), parseLore(ChatColor.GREEN + "Tame all beasts within 2 blocks"));
		assertEquals(new PowerDescription(HolyPower.TAME, 1), parseLore(ChatColor.GREEN + "Tame all beasts within 1 blocks"));
		assertEquals(new PowerDescription(HolyPower.FIREWORK, 1), parseLore(ChatColor.GREEN + "Shoots 1 firework rockets"));
		assertEquals(new PowerDescription(HolyPower.ICE, 1), parseLore(ChatColor.GREEN + "Shoots ice spike which freezes a target on hit"));
		assertEquals(new PowerDescription(HolyPower.FIREBALL, 3), parseLore(ChatColor.GREEN + "Fires a fireball with a 3 seconds cooldown"));
		assertEquals(new PowerDescription(HolyPower.DRUNK, 12), parseLore(ChatColor.GREEN + "Gets you drunk for 12 seconds!"));
		assertEquals(new PowerDescription(HolyPower.LIGHTNING_STORM, 2), parseLore(ChatColor.GREEN + "Creates a thunderstorm lasting 2 minutes"));
		// Gods 0.9.x showed "Damage: 12" or "Kills: 3" as the second line; the third is what counts
		assertNull(parseLore(ChatColor.WHITE + "Damage: 12"));
		assertNull(parseLore("Calls the moon for a while"));
		assertNull(parseLore(""));
	}

	private static PowerDescription parseLore(String line)
	{
		return HolyPowerRules.parse(ChatColor.stripColor(line));
	}

	@Test
	void cooldownsDependOnThePower()
	{
		assertEquals(10 * 60 * 1000L, HolyPowerRules.cooldownMillis(HolyPower.KNOWLEDGE, 5));
		assertEquals(1000L, HolyPowerRules.cooldownMillis(HolyPower.FIREBALL, 0));
		assertEquals(3000L, HolyPowerRules.cooldownMillis(HolyPower.FIREBALL, 3));
		assertEquals(30_000L, HolyPowerRules.cooldownMillis(HolyPower.CALLSUN, 0));
		assertEquals(120_000L, HolyPowerRules.cooldownMillis(HolyPower.CALLMOON, 2));
		assertEquals(30_000L, HolyPowerRules.cooldownMillis(HolyPower.HEALING, 9));
		assertTrue(HolyPowerRules.changesWorld(HolyPower.LIGHTNING_STORM));
		assertFalse(HolyPowerRules.changesWorld(HolyPower.LIGHTNING));
	}

	@Test
	void areaPowersAreCapped()
	{
		assertEquals(1, HolyPowerRules.effectRadius(0));
		assertEquals(10, HolyPowerRules.effectRadius(1));
		assertEquals(30, HolyPowerRules.effectRadius(3));
		assertEquals(30, HolyPowerRules.effectRadius(1000));
	}

	private static final UUID	PLAYER	= UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7");
	private static final UUID	OTHER	= UUID.fromString("0b8d4f7e-3c1a-4bfa-9a3e-6f0f1c2d3e4f");
	private static final UUID	WORLD	= UUID.fromString("5d0e2f3a-1b2c-4d5e-8f90-a1b2c3d4e5f6");
	private static final UUID	NETHER	= UUID.fromString("1a2b3c4d-1b2c-4d5e-8f90-a1b2c3d4e5f6");

	@Test
	void aPlayerWaitsBetweenUsesOfOnePower()
	{
		PowerCooldowns cooldowns = new PowerCooldowns();
		assertTrue(cooldowns.use(PLAYER, WORLD, HolyPower.HEALING, 1, 0L) instanceof PowerCooldowns.Ready);

		PowerCooldowns.Result again = cooldowns.use(PLAYER, WORLD, HolyPower.HEALING, 1, 10_000L);
		assertEquals(new PowerCooldowns.Cooling(20_000L), again);
		assertEquals(21, ((PowerCooldowns.Cooling) again).seconds());

		// Other powers and other players are not affected
		assertTrue(cooldowns.use(PLAYER, WORLD, HolyPower.TAME, 1, 10_000L) instanceof PowerCooldowns.Ready);
		assertTrue(cooldowns.use(OTHER, WORLD, HolyPower.HEALING, 1, 10_000L) instanceof PowerCooldowns.Ready);

		assertTrue(cooldowns.use(PLAYER, WORLD, HolyPower.HEALING, 1, 30_000L) instanceof PowerCooldowns.Ready);
	}

	@Test
	void onlyOneSkyPowerMovesAWorldAtATime()
	{
		PowerCooldowns cooldowns = new PowerCooldowns();
		assertTrue(cooldowns.use(PLAYER, WORLD, HolyPower.CALLMOON, 3, 0L) instanceof PowerCooldowns.Ready);

		assertTrue(cooldowns.use(OTHER, WORLD, HolyPower.CALLSUN, 1, 60_000L) instanceof PowerCooldowns.WorldBusy);
		assertTrue(cooldowns.use(OTHER, NETHER, HolyPower.CALLSUN, 1, 60_000L) instanceof PowerCooldowns.Ready);
		// A refused use starts no cooldown
		assertTrue(cooldowns.use(OTHER, WORLD, HolyPower.LIGHTNING_STORM, 1, 180_000L) instanceof PowerCooldowns.Ready);
		assertTrue(cooldowns.use(OTHER, WORLD, HolyPower.FIREBALL, 1, 180_000L) instanceof PowerCooldowns.Ready);
	}

	@Test
	void teleportStaysAmongLoadedChunks()
	{
		assertEquals(48, HolyPowerRules.teleportDistance(1));
		assertEquals(48, HolyPowerRules.teleportDistance(500));
		assertEquals(1, HolyPowerRules.teleportDistance(0));
	}

	@Test
	void teleportOnlyToSolidGroundWithTwoFreeBlocks()
	{
		assertTrue(HolyPowerRules.isSafeSpot(Material.GRASS_BLOCK, true, Material.AIR, true, Material.AIR, true));
		assertTrue(HolyPowerRules.isSafeSpot(Material.STONE, true, Material.SHORT_GRASS, true, Material.AIR, true));

		// No ground, or ground that hurts
		assertFalse(HolyPowerRules.isSafeSpot(Material.AIR, false, Material.AIR, true, Material.AIR, true));
		assertFalse(HolyPowerRules.isSafeSpot(Material.MAGMA_BLOCK, true, Material.AIR, true, Material.AIR, true));
		assertFalse(HolyPowerRules.isSafeSpot(Material.CACTUS, true, Material.AIR, true, Material.AIR, true));

		// No room, liquid or fire
		assertFalse(HolyPowerRules.isSafeSpot(Material.STONE, true, Material.AIR, true, Material.STONE, false));
		assertFalse(HolyPowerRules.isSafeSpot(Material.STONE, true, Material.WATER, true, Material.AIR, true));
		assertFalse(HolyPowerRules.isSafeSpot(Material.STONE, true, Material.LAVA, true, Material.AIR, true));
		assertFalse(HolyPowerRules.isSafeSpot(Material.STONE, true, Material.FIRE, true, Material.AIR, true));
		assertFalse(HolyPowerRules.isSafeSpot(Material.STONE, true, Material.AIR, true, Material.WATER, true));
		assertFalse(HolyPowerRules.isSafeSpot(Material.STONE, true, Material.POWDER_SNOW, true, Material.AIR, true));
	}

	@Test
	void anEarthquakeHurtsButDoesNotKill()
	{
		assertEquals(6.0D, HolyPowerRules.earthquakeDamage(1));
		assertEquals(10.0D, HolyPowerRules.earthquakeDamage(10));
		assertTrue(HolyPowerRules.earthquakeDamage(0) > 0.0D);
	}
}
