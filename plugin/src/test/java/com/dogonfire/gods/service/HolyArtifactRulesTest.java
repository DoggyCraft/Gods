package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

class HolyArtifactRulesTest
{
	@Test
	void artifactsRankUpWithUse()
	{
		assertEquals("", HolyArtifactRules.rankName(4));
		assertEquals(ChatColor.GREEN + "Shiny", HolyArtifactRules.rankName(5));
		assertEquals(ChatColor.GREEN + "Saintly", HolyArtifactRules.rankName(149));
		assertEquals(ChatColor.BLUE + "Amazing", HolyArtifactRules.rankName(150));
		assertEquals(ChatColor.DARK_PURPLE + "Epic", HolyArtifactRules.rankName(999));
		assertEquals(ChatColor.GOLD + "Legendary", HolyArtifactRules.rankName(1000));
	}

	@Test
	void theUsesLineCountsUses()
	{
		assertEquals(ChatColor.WHITE + "Used 0 times", HolyArtifactRules.usesLine(0));
		assertEquals(ChatColor.WHITE + "Used 1 time", HolyArtifactRules.usesLine(1));
	}

	@Test
	void oldArtifactsAreRecognizedByTheirFirstLine()
	{
		assertTrue(HolyArtifactRules.isArtifactLine("A Holy artifact of frost!"));
		assertTrue(HolyArtifactRules.isArtifactLine("An Unholy artifact of EVIL"));
		assertTrue(HolyArtifactRules.isArtifactLine("A Holy artifact given by Thor"));
		assertFalse(HolyArtifactRules.isArtifactLine("A holy artifact of frost"));
		assertFalse(HolyArtifactRules.isArtifactLine("Sharpness V"));
	}
}
