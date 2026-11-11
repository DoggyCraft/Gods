package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class AltarLightsTest
{
	/** On the newest Minecraft every light is there, the old torches too */
	@Test
	void everyLightExistsOnTheNewestServer()
	{
		List<String> missing = new ArrayList<>();
		Set<Material> lights = AltarLights.resolve(Material::getMaterial, List.of(Material.CANDLE, Material.RED_CANDLE_CAKE), missing::add);
		assertTrue(missing.isEmpty(), "" + missing);
		assertTrue(lights.containsAll(List.of(Material.TORCH, Material.REDSTONE_TORCH, Material.SOUL_TORCH, Material.LANTERN, Material.SOUL_LANTERN, Material.CANDLE, Material.RED_CANDLE_CAKE)));
		assertFalse(lights.contains(Material.WALL_TORCH));
	}

	/** On older servers the copper lights are skipped, the rest still mark altars */
	@Test
	void lightsTheServerLacksAreSkipped()
	{
		List<String> missing = new ArrayList<>();
		Set<Material> lights = AltarLights.resolve(name -> name.contains("COPPER") ? null : Material.getMaterial(name), List.of(), missing::add);
		assertEquals(9, missing.size(), "" + missing);
		assertTrue(missing.contains("COPPER_TORCH"));
		assertEquals(Set.of(Material.TORCH, Material.SOUL_TORCH, Material.REDSTONE_TORCH, Material.LANTERN, Material.SOUL_LANTERN), lights);
	}

	/** Every name is a real one: none is skipped as missing on the newest API */
	@Test
	void namesAreMaterials()
	{
		for (String name : AltarLights.NAMES)
		{
			assertTrue(Material.getMaterial(name) != null, name);
		}
	}
}
