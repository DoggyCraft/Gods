package com.dogonfire.gods.service;

import java.time.Duration;
import java.time.Instant;

import com.dogonfire.gods.config.Settings;

/**
 * When a god descends to its main altar (its NPC appears) and when it ascends again (pure, tested), checked on the
 * god's turn with config.yml's {@code npc.descend}:
 * <ul>
 * <li>It descends when it has a main altar, enough believers ({@code min-believers}) and power ({@code min-power}, 0
 * for none), and, with {@code requires-priest}, a priest for at least {@code hours-after-priest} (counted from when it
 * last went from no priest to one, {@code PriestSince} in gods.yml).</li>
 * <li>It ascends at once when a priest is required and it has none; or when its believers stay fewer than
 * {@code min-believers} for {@code ascend-grace-hours} (counted in {@code AscendingSince}), so a believer leaving for a
 * moment doesn't make it come and go. Losing its main altar doesn't make it ascend: it reappears at the next one.</li>
 * </ul>
 */
public final class DescentRules
{
	private DescentRules()
	{
	}

	/**
	 * What gods.yml keeps about it
	 *
	 * @param priestSince    since when it has had a priest (null: none now)
	 * @param descended      when it descended (null: it hasn't, or it ascended)
	 * @param ascendingSince since when its believers have been too few while it is down (null: they aren't)
	 */
	public record State(Instant priestSince, Instant descended, Instant ascendingSince)
	{
	}

	/** How the god is doing now */
	public record Facts(boolean hasPriest, int believers, float power, boolean hasMainAltar, Instant now)
	{
	}

	public enum Change
	{
		NONE, DESCEND, ASCEND
	}

	/** The god's state after the check, and whether it descends or ascends now */
	public record Outcome(State state, Change change)
	{
	}

	public static Outcome check(Settings.Descend rules, State state, Facts facts)
	{
		Instant priestSince = facts.hasPriest() ? (state.priestSince() != null ? state.priestSince() : facts.now()) : null;
		boolean enoughBelievers = facts.believers() >= rules.minBelievers();

		if (state.descended() == null)
		{
			boolean priestOk = !rules.requiresPriest() || (priestSince != null && !facts.now().isBefore(priestSince.plus(Duration.ofHours(rules.hoursAfterPriest()))));
			boolean powerOk = rules.minPower() <= 0 || facts.power() >= rules.minPower();
			if (facts.hasMainAltar() && priestOk && enoughBelievers && powerOk)
			{
				return new Outcome(new State(priestSince, facts.now(), null), Change.DESCEND);
			}
			return new Outcome(new State(priestSince, null, null), Change.NONE);
		}

		if (rules.requiresPriest() && !facts.hasPriest())
		{
			return new Outcome(new State(null, null, null), Change.ASCEND);
		}
		if (enoughBelievers)
		{
			return new Outcome(new State(priestSince, state.descended(), null), Change.NONE);
		}
		Instant ascendingSince = state.ascendingSince() != null ? state.ascendingSince() : facts.now();
		if (!facts.now().isBefore(ascendingSince.plus(Duration.ofHours(rules.ascendGraceHours()))))
		{
			return new Outcome(new State(priestSince, null, null), Change.ASCEND);
		}
		return new Outcome(new State(priestSince, state.descended(), ascendingSince), Change.NONE);
	}
}
