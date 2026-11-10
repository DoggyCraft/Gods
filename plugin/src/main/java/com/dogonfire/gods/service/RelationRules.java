package com.dogonfire.gods.service;

import java.util.List;

import com.dogonfire.gods.model.God;

/**
 * Alliances and wars between gods, on the models so they can be tested. A relation always goes both ways, and two gods
 * are never allies and enemies at once. A god that doesn't exist (null) is left alone; the other one still loses or
 * gains the relation by name.
 */
public final class RelationRules
{
	private RelationRules()
	{
	}

	/** Makes the gods allies, ending any war between them */
	public static void makeAllies(God god, God other)
	{
		add(god, true, other.name());
		add(other, true, god.name());
		remove(god, false, other.name());
		remove(other, false, god.name());
	}

	/** Puts the gods at war, ending any alliance between them */
	public static void makeEnemies(God god, God other)
	{
		add(god, false, other.name());
		add(other, false, god.name());
		remove(god, true, other.name());
		remove(other, true, god.name());
	}

	/** Ends the alliance (or with {@code allies} false, the war) between the two gods; either may be null */
	public static void end(God god, String godName, God other, String otherName, boolean allies)
	{
		remove(god, allies, otherName);
		remove(other, allies, godName);
	}

	/** Takes a god (that is removed) off {@code god}'s allies and enemies */
	public static void forget(God god, String otherName)
	{
		god.allies().remove(otherName);
		god.enemies().remove(otherName);
	}

	private static void add(God god, boolean allies, String otherName)
	{
		List<String> gods = allies ? god.allies() : god.enemies();
		if (!gods.contains(otherName))
		{
			gods.add(otherName);
		}
	}

	private static void remove(God god, boolean allies, String otherName)
	{
		if (god != null)
		{
			(allies ? god.allies() : god.enemies()).remove(otherName);
		}
	}
}
