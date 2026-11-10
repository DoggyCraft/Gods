package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.dogonfire.gods.model.God;

class RelationRulesTest
{
	@Test
	void anAllianceEndsAWarOnBothSides()
	{
		God thor = new God("Thor");
		God odin = new God("Odin");
		RelationRules.makeEnemies(thor, odin);
		assertEquals(List.of("Odin"), thor.enemies());
		assertEquals(List.of("Thor"), odin.enemies());

		RelationRules.makeAllies(thor, odin);
		assertEquals(List.of("Odin"), thor.allies());
		assertEquals(List.of("Thor"), odin.allies());
		assertEquals(List.of(), thor.enemies());
		assertEquals(List.of(), odin.enemies());

		// Twice is the same as once
		RelationRules.makeAllies(thor, odin);
		assertEquals(List.of("Odin"), thor.allies());
	}

	@Test
	void endingARelationWorksWhenTheOtherGodIsGone()
	{
		God thor = new God("Thor");
		thor.allies().add("Odin");
		thor.allies().add("Loki");
		RelationRules.end(thor, "Thor", null, "Odin", true);
		assertEquals(List.of("Loki"), thor.allies());
	}

	@Test
	void aRemovedGodIsForgotten()
	{
		God thor = new God("Thor");
		thor.allies().add("Loki");
		thor.enemies().add("Loki");
		thor.enemies().add("Odin");
		RelationRules.forget(thor, "Loki");
		assertEquals(List.of(), thor.allies());
		assertEquals(List.of("Odin"), thor.enemies());
	}
}
