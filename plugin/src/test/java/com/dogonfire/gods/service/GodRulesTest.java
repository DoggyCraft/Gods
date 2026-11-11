package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.model.GodType;

class GodRulesTest
{
	@Test
	void godNamesAreCapitalized()
	{
		assertEquals("Thor", GodRules.formatGodName("tHOR"));
		assertEquals("Odin", GodRules.formatGodName("Odin"));
	}

	@Test
	void everyTypeHasItsColor()
	{
		assertEquals(ChatColor.DARK_GRAY, GodRules.color(GodType.THUNDER));
		assertEquals(ChatColor.RED, GodRules.color(GodType.EVIL));
		assertEquals(ChatColor.WHITE, GodRules.color(GodType.PARTY));
		assertEquals(ChatColor.WHITE, GodRules.color(null));
	}

	@Test
	void theVoiceComesFromTypeAndGender()
	{
		assertEquals("thunder_male", GodRules.voice(GodType.THUNDER, GodGender.Male, 0L));
		assertEquals("love_female", GodRules.voice(GodType.LOVE, GodGender.Female, 1L));
		// No gender: the seed picks one, always the same
		assertEquals("sea_female", GodRules.voice(GodType.SEA, GodGender.None, 42L));
		assertEquals("sea_male", GodRules.voice(GodType.SEA, GodGender.None, 43L));
		// No such god
		assertEquals("sun_male", GodRules.voice(null, null, 0L));
	}

	@Test
	void powerIsTheBelieversPowerButAtLeastTheMinimum()
	{
		assertEquals(3.5F, GodRules.godPower(List.of(1.0F, 2.5F), 0));
		assertEquals(10.0F, GodRules.godPower(List.of(1.0F, 2.5F), 10));
		assertEquals(0.0F, GodRules.godPower(List.of(), 0));
	}

	@Test
	void aGodWithoutBelieversAndPowerIsDead()
	{
		assertTrue(GodRules.isDead(0, 0.5F));
		assertFalse(GodRules.isDead(0, 1.0F), "the whitelist keeps it alive");
		assertFalse(GodRules.isDead(1, 0.0F));
	}

	@Test
	void verbosityGrowsWithPowerAndTheSetting()
	{
		long now = 0L;
		int quiet = GodRules.verbosity(1, 7L, 0.0F, 1.0, now);
		int loud = GodRules.verbosity(50, 7L, 1000.0F, 1.0, now);
		assertTrue(quiet >= 1);
		assertTrue(loud > quiet);
		assertEquals(1, GodRules.verbosity(50, 7L, 1000.0F, 0.0, now), "verbosity 0 in config.yml");
		assertEquals(GodRules.verbosity(10, 7L, 5.0F, 2.0, now), GodRules.verbosity(10, 7L, 5.0F, 2.0, now), "the seed fixes it");
	}
}
