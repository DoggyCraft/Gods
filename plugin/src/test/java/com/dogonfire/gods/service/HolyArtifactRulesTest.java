package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HolyArtifactRulesTest
{
	@Test
	void artifactsRankUpWithUse()
	{
		assertEquals("", HolyArtifactRules.rank(4));
		assertEquals("shiny", HolyArtifactRules.rank(5));
		assertEquals("saintly", HolyArtifactRules.rank(149));
		assertEquals("amazing", HolyArtifactRules.rank(150));
		assertEquals("epic", HolyArtifactRules.rank(999));
		assertEquals("legendary", HolyArtifactRules.rank(1000));
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
