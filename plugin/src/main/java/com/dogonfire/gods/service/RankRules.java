package com.dogonfire.gods.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.model.GodMood;
import com.dogonfire.gods.model.GodRank;
import com.dogonfire.gods.model.GodStats;

/**
 * How a god's rank changes (pure, tested), checked on the god's turn with config.yml's {@code ranks}:
 * <ul>
 * <li>It rises one rank at a time, when it meets every condition of the next rank ({@link #progress}). The conditions
 * {@code requires-priest} and {@code min-believers} carry over to the ranks above.</li>
 * <li>Its mood and what its religion has done (the activity counters) never make it fall: an angry god stalls.</li>
 * <li>Without a priest it falls at once below every rank that needs one.</li>
 * <li>With fewer believers than its rank needs for {@code drop-grace-hours} (counted in {@code AscendingSince}) it falls
 * to the highest rank it still has the believers for.</li>
 * <li>Losing its main altar, power or altars doesn't make it fall either: it stalls until it has them again.</li>
 * </ul>
 * A god from before ranks is placed once by {@link #seed}.
 */
public final class RankRules
{
	private RankRules()
	{
	}

	/**
	 * What gods.yml keeps about a god's rank
	 *
	 * @param rank         its rank (never null here; an unranked god is a spirit)
	 * @param rankSince    when it reached it (null: unknown, so any time at it counts as enough)
	 * @param priestSince  since when it has had a priest (null: none now)
	 * @param fallingSince since when it has had fewer believers than its rank needs (null: it hasn't)
	 */
	public record State(GodRank rank, Instant rankSince, Instant priestSince, Instant fallingSince)
	{
	}

	/**
	 * How the god is doing now
	 *
	 * @param altars          its praying altars standing
	 * @param holyLands its holy lands
	 */
	public record Facts(boolean hasPriest, int believers, float power, GodMood mood, boolean hasMainAltar, int altars, int holyLands, GodStats stats,
			Instant now)
	{
	}

	/** Which features are switched on: the conditions of one that is off don't count */
	public record Features(boolean sacrifices, boolean quests, boolean holyLand)
	{
		public static final Features ALL = new Features(true, true, true);
	}

	/** One condition of a rank, by its key in config.yml */
	public enum Condition
	{
		BELIEVERS("min-believers"), POWER("min-power"), PRIEST("requires-priest"), MOOD("min-mood"), MAIN_ALTAR("requires-main-altar"),
		HOURS_AFTER_PRIEST("hours-after-priest"), HOURS_AT_PREVIOUS_RANK("hours-at-previous-rank"), PRAYERS("min-prayers"), SACRIFICES("min-sacrifices"),
		QUESTS("min-quests"), ALTARS("min-altars"), HOLY_LANDS("min-holy-land-regions"), CONVERTS("min-converts");

		private final String key;

		Condition(String key)
		{
			this.key = key;
		}

		/** Its key in config.yml */
		public String key()
		{
			return this.key;
		}

		/** What the religion does over its life (counted in Stats), which seeding fills in */
		public boolean isActivity()
		{
			return this == PRAYERS || this == SACRIFICES || this == QUESTS || this == CONVERTS;
		}
	}

	/**
	 * How far a god is with one condition: where it is and what the rank needs. A yes/no condition is 1 (yes) or 0;
	 * a mood is its {@link #moodLevel}; hours are whole hours.
	 */
	public record Progress(Condition condition, long current, long required, boolean met)
	{
	}

	/** Why a god's rank changed */
	public enum Reason
	{
		/** It met every condition of the next rank */
		RANK_UP,
		/** It has no priest any more */
		NO_PRIEST,
		/** It had too few believers for longer than the grace period */
		TOO_FEW_BELIEVERS
	}

	public enum Change
	{
		NONE, UP, DOWN
	}

	/** The god's state after the check, and whether (and why) its rank changes now */
	public record Outcome(State state, Change change, Reason reason)
	{
	}

	/** A god's place among the ranks: 0 for angry up to 4 for exalted */
	public static int moodLevel(GodMood mood)
	{
		return mood == null ? 2 : GodMood.values().length - 1 - mood.ordinal();
	}

	/** Whether any rank up to this one needs a priest (the condition carries over to the ranks above) */
	public static boolean requiresPriest(Settings.Ranks ranks, GodRank rank)
	{
		for (GodRank lower : GodRank.values())
		{
			if (lower.compareTo(rank) > 0)
			{
				break;
			}
			if (ranks.of(lower).requiresPriest())
			{
				return true;
			}
		}
		return false;
	}

	/** The believers this rank needs: the most any rank up to it asks for (the condition carries over) */
	public static int neededBelievers(Settings.Ranks ranks, GodRank rank)
	{
		int needed = 0;
		for (GodRank lower : GodRank.values())
		{
			if (lower.compareTo(rank) > 0)
			{
				break;
			}
			needed = Math.max(needed, ranks.of(lower).minBelievers());
		}
		return needed;
	}

	/** Whole hours a rank reached at {@code since} has been held until now; 0 for null */
	public static long hoursHeld(Instant since)
	{
		return hoursSince(since, Instant.now());
	}

	/** Whole hours from {@code since} to {@code now}; 0 for null or the future */
	static long hoursSince(Instant since, Instant now)
	{
		return since == null ? 0 : Math.max(0, Duration.between(since, now).toHours());
	}

	/**
	 * How far the god is with each condition of {@code rank} (in the order of {@link Condition}); the conditions that
	 * don't count (0, false, none, or a feature that is off) are left out. Empty for {@link GodRank#SPIRIT}.
	 */
	public static List<Progress> progress(Settings.Ranks ranks, GodRank rank, State state, Facts facts, Features features)
	{
		List<Progress> progress = new ArrayList<>();
		if (rank == null || rank == GodRank.SPIRIT)
		{
			return progress;
		}
		Settings.RankConditions conditions = ranks.of(rank);
		int believers = neededBelievers(ranks, rank);
		if (believers > 0)
		{
			progress.add(atLeast(Condition.BELIEVERS, facts.believers(), believers));
		}
		if (conditions.minPower() > 0)
		{
			progress.add(atLeast(Condition.POWER, (long) Math.floor(facts.power()), conditions.minPower()));
		}
		if (requiresPriest(ranks, rank))
		{
			progress.add(atLeast(Condition.PRIEST, facts.hasPriest() ? 1 : 0, 1));
		}
		if (conditions.minMood() != null)
		{
			progress.add(atLeast(Condition.MOOD, moodLevel(facts.mood()), moodLevel(conditions.minMood())));
		}
		if (conditions.requiresMainAltar())
		{
			progress.add(atLeast(Condition.MAIN_ALTAR, facts.hasMainAltar() ? 1 : 0, 1));
		}
		if (conditions.hoursAfterPriest() > 0)
		{
			Instant priestSince = facts.hasPriest() ? (state.priestSince() != null ? state.priestSince() : facts.now()) : null;
			progress.add(atLeast(Condition.HOURS_AFTER_PRIEST, hoursSince(priestSince, facts.now()), conditions.hoursAfterPriest()));
		}
		if (conditions.hoursAtPreviousRank() > 0)
		{
			// A rank held since an unknown time has been held long enough
			long held = state.rankSince() == null ? conditions.hoursAtPreviousRank() : hoursSince(state.rankSince(), facts.now());
			progress.add(atLeast(Condition.HOURS_AT_PREVIOUS_RANK, held, conditions.hoursAtPreviousRank()));
		}
		addActivity(progress, conditions, facts.stats(), features);
		if (conditions.minAltars() > 0)
		{
			progress.add(atLeast(Condition.ALTARS, facts.altars(), conditions.minAltars()));
		}
		if (conditions.minHolyLands() > 0 && features.holyLand())
		{
			progress.add(atLeast(Condition.HOLY_LANDS, facts.holyLands(), conditions.minHolyLands()));
		}
		if (conditions.minConverts() > 0)
		{
			progress.add(atLeast(Condition.CONVERTS, facts.stats().converts(), conditions.minConverts()));
		}
		return progress;
	}

	private static void addActivity(List<Progress> progress, Settings.RankConditions conditions, GodStats stats, Features features)
	{
		if (conditions.minPrayers() > 0)
		{
			progress.add(atLeast(Condition.PRAYERS, stats.prayers(), conditions.minPrayers()));
		}
		if (conditions.minSacrifices() > 0 && features.sacrifices())
		{
			progress.add(atLeast(Condition.SACRIFICES, stats.sacrifices(), conditions.minSacrifices()));
		}
		if (conditions.minQuests() > 0 && features.quests())
		{
			progress.add(atLeast(Condition.QUESTS, stats.quests(), conditions.minQuests()));
		}
	}

	private static Progress atLeast(Condition condition, long current, long required)
	{
		return new Progress(condition, current, required, current >= required);
	}

	/** Whether every condition of {@code rank} holds */
	public static boolean qualifies(Settings.Ranks ranks, GodRank rank, State state, Facts facts, Features features)
	{
		return progress(ranks, rank, state, facts, features).stream().allMatch(Progress::met);
	}

	/** The god's turn: does its rank change now? */
	public static Outcome evaluate(Settings.Ranks ranks, State state, Facts facts, Features features)
	{
		Instant now = facts.now();
		GodRank rank = state.rank() != null ? state.rank() : GodRank.SPIRIT;
		Instant priestSince = facts.hasPriest() ? (state.priestSince() != null ? state.priestSince() : now) : null;

		// No priest: below every rank that needs one, at once
		if (!facts.hasPriest() && requiresPriest(ranks, rank))
		{
			GodRank target = rank;
			while (target != GodRank.SPIRIT && requiresPriest(ranks, target))
			{
				target = target.previous();
			}
			Instant falling = facts.believers() < neededBelievers(ranks, target) ? orNow(state.fallingSince(), now) : null;
			return new Outcome(new State(target, now, null, falling), Change.DOWN, Reason.NO_PRIEST);
		}

		// Too few believers: after the grace period, to the highest rank it still has the believers for
		if (facts.believers() < neededBelievers(ranks, rank))
		{
			Instant falling = orNow(state.fallingSince(), now);
			if (now.isBefore(falling.plus(Duration.ofHours(ranks.dropGraceHours()))))
			{
				return new Outcome(new State(rank, state.rankSince(), priestSince, falling), Change.NONE, null);
			}
			GodRank target = rank;
			while (target != GodRank.SPIRIT && facts.believers() < neededBelievers(ranks, target))
			{
				target = target.previous();
			}
			return new Outcome(new State(target, now, priestSince, null), Change.DOWN, Reason.TOO_FEW_BELIEVERS);
		}

		State steady = new State(rank, state.rankSince(), priestSince, null);
		GodRank next = rank.next();
		if (next != null && qualifies(ranks, next, steady, facts, features))
		{
			return new Outcome(new State(next, now, priestSince, null), Change.UP, Reason.RANK_UP);
		}
		return new Outcome(steady, Change.NONE, null);
	}

	private static Instant orNow(Instant time, Instant now)
	{
		return time != null ? time : now;
	}

	/**
	 * Where a god from before ranks starts, and its seeded counters
	 *
	 * @param rank  the highest rank it qualifies for
	 * @param stats its counters: what that rank (and the ones below) ask for, or more if it had more
	 */
	public record Seed(GodRank rank, GodStats stats)
	{
	}

	/**
	 * Places a god from before ranks (once): the highest rank whose conditions, and those of every rank below, it
	 * meets without the activity counters (believers, power, priest, mood, main altar, altars, holy land, time), with
	 * every time condition measured against its age, since Gods didn't keep that before (a god whose creation isn't
	 * known counts as old). Its counters are set to exactly what that rank and the ones below ask for (keeping any it
	 * counted already), so it stays there. A god that had descended keeps at least {@code floor}.
	 *
	 * @param created when the god was created, or null
	 * @param floor   the lowest rank it gets, e.g. {@link GodRank#DESCENDED} for a god that had descended
	 */
	public static Seed seed(Settings.Ranks ranks, Facts facts, Features features, Instant created, GodRank floor)
	{
		long age = created == null ? Long.MAX_VALUE : hoursSince(created, facts.now());
		GodRank placed = GodRank.SPIRIT;
		long timeNeeded = 0;
		for (GodRank rank : GodRank.values())
		{
			if (rank == GodRank.SPIRIT)
			{
				continue;
			}
			Settings.RankConditions conditions = ranks.of(rank);
			timeNeeded += conditions.hoursAtPreviousRank();
			boolean time = age >= timeNeeded && (conditions.hoursAfterPriest() == 0 || (facts.hasPriest() && age >= conditions.hoursAfterPriest()));
			// Time is checked above, against its age; the counters are what seeding sets
			boolean others = progress(ranks, rank, new State(placed, null, facts.now(), null), facts, features).stream()
					.filter(progress -> !progress.condition().isActivity() && progress.condition() != Condition.HOURS_AFTER_PRIEST)
					.allMatch(Progress::met);
			if (!time || !others)
			{
				break;
			}
			placed = rank;
		}
		if (floor != null && floor.compareTo(placed) > 0)
		{
			placed = floor;
		}
		return new Seed(placed, facts.stats().atLeast(requiredStats(ranks, placed)));
	}

	/** The counters a god needs to have reached this rank: the most each rank up to it asks for */
	public static GodStats requiredStats(Settings.Ranks ranks, GodRank rank)
	{
		GodStats stats = GodStats.ZERO;
		for (GodRank lower : GodRank.values())
		{
			if (lower.compareTo(rank) > 0)
			{
				break;
			}
			Settings.RankConditions conditions = ranks.of(lower);
			stats = stats.atLeast(new GodStats(conditions.minPrayers(), conditions.minSacrifices(), conditions.minQuests(), conditions.minConverts()));
		}
		return stats;
	}

	// ---------------------------------------------------------------------
	// What a rank unlocks

	/** Whether a god of {@code rank} has what needs {@code needed} */
	public static boolean unlocked(GodRank rank, GodRank needed)
	{
		return (rank != null ? rank : GodRank.SPIRIT).atLeast(needed != null ? needed : GodRank.SPIRIT);
	}

	/** How many priests a god of this rank may have: priests.max-per-god, one more from {@code extraPriest} on */
	public static int maxPriests(int maxPerGod, GodRank rank, GodRank extraPriest)
	{
		return unlocked(rank, extraPriest) ? maxPerGod + 1 : maxPerGod;
	}

	/** How many holy lands (one per altar that claimed land) a god may hold: power / 10 + 3, as always */
	public static int maxHolyLands(float power)
	{
		return (int) (power / 10 + 3);
	}
}
