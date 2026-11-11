package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.dogonfire.gods.model.Altar;
import com.dogonfire.gods.model.AltarKind;
import com.dogonfire.gods.model.BlockPos;

class MainAltarRulesTest
{
	private static final BlockPos	BLOCK		= new BlockPos("world", 10, 64, 10);
	private static final Altar		PRAYING		= new Altar(new BlockPos("world", 10, 64, 11), BLOCK, AltarKind.PRAYING, "Zeus", null, null);
	private static final Altar		CURSES		= new Altar(new BlockPos("world", 11, 64, 10), BLOCK, AltarKind.CURSES, "Zeus", null, null);
	private static final Altar		OTHER_GOD	= new Altar(new BlockPos("world", 9, 64, 10), BLOCK, AltarKind.PRAYING, "Hera", null, null);

	private final Map<BlockPos, Altar>			bySign	= new HashMap<>();
	private final Map<BlockPos, List<Altar>>	byBlock	= new HashMap<>();

	private List<Altar> candidates(BlockPos looked)
	{
		return MainAltarRules.candidates(looked, this.bySign::get, block -> this.byBlock.getOrDefault(block, List.of()));
	}

	private void register(Altar... altars)
	{
		for (Altar altar : altars)
		{
			this.bySign.put(altar.sign(), altar);
			this.byBlock.computeIfAbsent(altar.block(), b -> new java.util.ArrayList<>()).add(altar);
		}
	}

	@Test
	void theSignTheBlockOrTheLightNameTheAltar()
	{
		register(CURSES, PRAYING);
		assertEquals(List.of(PRAYING), candidates(PRAYING.sign()));
		assertEquals(List.of(CURSES, PRAYING), candidates(BLOCK));
		// The light on top
		assertEquals(List.of(CURSES, PRAYING), candidates(BLOCK.up(1)));
		assertEquals(List.of(), candidates(BLOCK.up(2)));
	}

	@Test
	void onlyAPrayingAltarOfThePriestsGod()
	{
		assertEquals(new MainAltarRules.Target(MainAltarRules.Verdict.OK, PRAYING), MainAltarRules.choose(List.of(CURSES, OTHER_GOD, PRAYING), "Zeus"));
		assertEquals(MainAltarRules.Verdict.NO_ALTAR, MainAltarRules.choose(List.of(), "Zeus").verdict());
		assertEquals(MainAltarRules.Verdict.NOT_PRAYING, MainAltarRules.choose(List.of(CURSES), "Zeus").verdict());
		MainAltarRules.Target other = MainAltarRules.choose(List.of(CURSES, OTHER_GOD), "Zeus");
		assertEquals(MainAltarRules.Verdict.OTHER_GOD, other.verdict());
		assertNull(other.altar());
	}

	@Test
	void aGodStandsInFrontOfTheSign()
	{
		// The sign hangs south of its block, so the spot is one further south
		assertEquals(new BlockPos("world", 10, 64, 12), MainAltarRules.standingSpot(PRAYING));
		assertEquals(new BlockPos("world", 12, 64, 10), MainAltarRules.standingSpot(CURSES));
	}
}
