package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

class CommandmentRulesTest
{
	@Test
	void picksAnythingButWhatToAvoid()
	{
		Random random = new Random(1);
		Set<String> seen = new HashSet<>();
		for (int i = 0; i < 200; i++)
		{
			String pick = CommandmentRules.pick(random, List.of("COW", "PIG", "SHEEP"), "PIG");
			assertNotEquals("PIG", pick);
			seen.add(pick);
		}
		assertEquals(Set.of("COW", "SHEEP"), seen);
	}

	@Test
	void picksWhatToAvoidOnlyWhenNothingElseIsThere()
	{
		Random random = new Random(1);
		assertEquals("PIG", CommandmentRules.pick(random, List.of("PIG"), "PIG"));
		assertEquals("PIG", CommandmentRules.pick(random, List.of("PIG"), null));
		assertNull(CommandmentRules.pick(random, List.<String> of(), "PIG"));
	}

	/** A god's seed picks the same few needs every time; its holy food is never asked for */
	@Test
	void sacrificeNeedsComeFromTheSeed()
	{
		List<String> needs = List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P");
		Random random = new Random(2);
		Set<String> seen = new HashSet<>();
		for (int i = 0; i < 500; i++)
		{
			String need = CommandmentRules.sacrificeNeed(random, 42L, needs, () -> "UNHOLY", "A");
			assertNotEquals("A", need);
			seen.add(need);
		}
		assertTrue(seen.size() <= CommandmentRules.NEEDS_PER_GOD, "" + seen);
		assertEquals(seen, sacrificeNeeds(new Random(3), 42L, needs));
	}

	@Test
	void sacrificeNeedsEndWhenNothingFits()
	{
		Random random = new Random(2);
		assertNull(CommandmentRules.sacrificeNeed(random, 7L, List.of("A"), () -> "A", "A"));
		assertEquals("UNHOLY", CommandmentRules.sacrificeNeed(random, 7L, List.of(), () -> "UNHOLY", "A"));
		assertNull(CommandmentRules.sacrificeNeed(random, 7L, List.of(), () -> null, "A"));
	}

	private static Set<String> sacrificeNeeds(Random random, long seed, List<String> needs)
	{
		Set<String> seen = new HashSet<>();
		for (int i = 0; i < 500; i++)
		{
			seen.add(CommandmentRules.sacrificeNeed(random, seed, needs, () -> "UNHOLY", "A"));
		}
		return seen;
	}
}
