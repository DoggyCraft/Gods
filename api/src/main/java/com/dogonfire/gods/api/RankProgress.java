package com.dogonfire.gods.api;

import java.util.List;

/**
 * How far a god is towards its next rank, as it was when it was fetched.
 *
 * @param rank       the god's rank now
 * @param next       the rank it can rise to next, or null when it has the highest
 * @param conditions every condition of {@code next} (or, for a god with the highest rank, of its own rank) that counts
 *                   on this server, in a fixed order
 */
public record RankProgress(GodRank rank, GodRank next, List<Condition> conditions)
{
	public RankProgress
	{
		conditions = List.copyOf(conditions);
	}

	/**
	 * One condition of a rank.
	 *
	 * @param name     its key in Gods' config: {@code min-believers}, {@code min-power}, {@code requires-priest},
	 *                 {@code min-mood}, {@code requires-main-altar}, {@code hours-after-priest},
	 *                 {@code hours-at-previous-rank}, {@code min-prayers}, {@code min-sacrifices}, {@code min-quests},
	 *                 {@code min-altars}, {@code min-holy-land-regions} or {@code min-converts}
	 * @param current  where the god is: a count, whole hours, 1 or 0 for a yes/no condition, or for the mood its level
	 *                 from 0 ({@link GodMood#ANGRY}) to 4 ({@link GodMood#EXALTED})
	 * @param required what the rank needs, in the same unit
	 * @param met      whether the god meets it
	 */
	public record Condition(String name, long current, long required, boolean met)
	{
	}

	/** Whether the god meets every condition (it rises on one of its next turns, unless a plugin stops it) */
	public boolean isComplete()
	{
		return this.conditions.stream().allMatch(Condition::met);
	}
}
