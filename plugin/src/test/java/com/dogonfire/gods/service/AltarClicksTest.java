package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.dogonfire.gods.service.AltarClicks.Candidate;
import com.dogonfire.gods.service.AltarClicks.Part;
import com.dogonfire.gods.service.AltarSigns.Kind;

class AltarClicksTest
{
	@Test
	void anEmptyHandPraysAtTheBlockAndTheLight()
	{
		assertTrue(AltarClicks.praysOnClick(Part.BLOCK, true, false, false, "SNOW_BLOCK", "AIR"));
		assertTrue(AltarClicks.praysOnClick(Part.LIGHT, true, false, false, "TORCH", "AIR"));
		// A lit candle stays lit: the click prays instead of blowing it out
		assertTrue(AltarClicks.praysOnClick(Part.LIGHT, true, false, false, "RED_CANDLE", "AIR"));
		assertFalse(AltarClicks.praysOnClick(Part.NONE, true, false, false, "STONE", "AIR"));
	}

	@Test
	void theOffHandAndSneakingKeepVanilla()
	{
		assertFalse(AltarClicks.praysOnClick(Part.BLOCK, false, false, false, "SNOW_BLOCK", "AIR"));
		// Sneak-clicking places blocks against the altar
		assertFalse(AltarClicks.praysOnClick(Part.BLOCK, true, true, false, "SNOW_BLOCK", "STONE"));
		assertFalse(AltarClicks.praysOnClick(Part.LIGHT, true, true, false, "LANTERN", "AIR"));
	}

	@Test
	void blocksWithTheirOwnUseKeepIt()
	{
		assertFalse(AltarClicks.praysOnClick(Part.BLOCK, true, false, true, "NOTE_BLOCK", "AIR"));
		assertFalse(AltarClicks.praysOnClick(Part.BLOCK, true, false, true, "JUKEBOX", "MUSIC_DISC_CAT"));
		// A candle cake is eaten
		assertFalse(AltarClicks.praysOnClick(Part.LIGHT, true, false, false, "CANDLE_CAKE", "AIR"));
		assertFalse(AltarClicks.praysOnClick(Part.LIGHT, true, false, false, "LIME_CANDLE_CAKE", "AIR"));
	}

	@Test
	void itemsThatWorkOnTheBlockKeepVanilla()
	{
		assertFalse(AltarClicks.praysOnClick(Part.LIGHT, true, false, false, "CANDLE", "FLINT_AND_STEEL"));
		assertFalse(AltarClicks.praysOnClick(Part.LIGHT, true, false, false, "CANDLE", "FIRE_CHARGE"));
		// More candles of the same color
		assertFalse(AltarClicks.praysOnClick(Part.LIGHT, true, false, false, "BLUE_CANDLE", "BLUE_CANDLE"));
		assertFalse(AltarClicks.praysOnClick(Part.BLOCK, true, false, false, "OAK_LOG", "IRON_AXE"));
		assertFalse(AltarClicks.praysOnClick(Part.BLOCK, true, false, false, "COPPER_BLOCK", "HONEYCOMB"));
		assertFalse(AltarClicks.praysOnClick(Part.BLOCK, true, false, false, "PUMPKIN", "SHEARS"));
		assertFalse(AltarClicks.praysOnClick(Part.BLOCK, true, false, false, "MOSS_BLOCK", "BONE_MEAL"));
		assertFalse(AltarClicks.praysOnClick(Part.BLOCK, true, false, false, "PODZOL", "WOODEN_SHOVEL"));
		assertFalse(AltarClicks.praysOnClick(Part.BLOCK, true, false, false, "COARSE_DIRT", "DIAMOND_HOE"));
		assertFalse(AltarClicks.praysOnClick(Part.BLOCK, true, false, false, "SNOW_BLOCK", "WATER_BUCKET"));
		assertFalse(AltarClicks.praysOnClick(Part.BLOCK, true, false, false, "SNOW_BLOCK", "COW_SPAWN_EGG"));
		// Putting a sign (wall or hanging) on the altar block
		assertFalse(AltarClicks.praysOnClick(Part.BLOCK, true, false, false, "NETHERRACK", "OAK_SIGN"));
		assertFalse(AltarClicks.praysOnClick(Part.BLOCK, true, false, false, "NETHERRACK", "CRIMSON_HANGING_SIGN"));
	}

	@Test
	void otherItemsPray()
	{
		assertTrue(AltarClicks.praysOnClick(Part.BLOCK, true, false, false, "SNOW_BLOCK", "DIAMOND_SWORD"));
		assertTrue(AltarClicks.praysOnClick(Part.BLOCK, true, false, false, "SNOW_BLOCK", "IRON_PICKAXE"));
		assertTrue(AltarClicks.praysOnClick(Part.BLOCK, true, false, false, "SNOW_BLOCK", "BREAD"));
		// A block in hand prays; sneaking places it
		assertTrue(AltarClicks.praysOnClick(Part.BLOCK, true, false, false, "SNOW_BLOCK", "COBBLESTONE"));
		assertTrue(AltarClicks.praysOnClick(Part.LIGHT, true, false, false, "CANDLE", "RED_CANDLE"));
	}

	@Test
	void aTaggedPrayingSignIsPreferred()
	{
		assertEquals("tagged", AltarClicks.chooseSign(List.of(new Candidate<>("curses", Kind.CURSES, true), new Candidate<>("old", Kind.PRAYING, false),
				new Candidate<>("tagged", Kind.PRAYING, true), new Candidate<>("second", Kind.PRAYING, true))));
		assertEquals("old", AltarClicks.chooseSign(List.of(new Candidate<>("blessings", Kind.BLESSINGS, true), new Candidate<>("old", Kind.PRAYING, false))));
	}

	@Test
	void curseAndBlessingSignsAreNeverUsed()
	{
		assertNull(AltarClicks.chooseSign(List.of(new Candidate<>("curses", Kind.CURSES, true), new Candidate<>("blessings", Kind.BLESSINGS, true))));
		assertNull(AltarClicks.chooseSign(List.<Candidate<String>> of()));
	}
}
