package com.dogonfire.gods.service;

import java.time.Duration;
import java.time.Instant;

import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.model.Believer;
import com.dogonfire.gods.model.DevotionLevel;

/**
 * The time rules for believers, without Bukkit so they can be tested: when a prayer counts, how long until the next
 * one, and how recent a blessing, offer or invitation is. A missing (or unreadable) time counts as long ago, like the
 * old code's 1970 fallback, except where noted. The arithmetic (whole minutes and seconds, rounding toward zero) is
 * kept exactly as it was.
 */
public final class BelieverRules
{
	/** An invitation to join a god expires after this many seconds */
	public static final long	INVITATION_SECONDS		= 30L;
	/** A believer can be offered priesthood again after this many minutes */
	public static final long	PRIEST_OFFER_MINUTES	= 60L;
	/** Praying at another god's altar asks for confirmation: praying there again within this many seconds changes god */
	public static final long	CHANGING_GOD_SECONDS	= 10L;

	private BelieverRules()
	{
	}

	static long millis(Instant time)
	{
		return time != null ? time.toEpochMilli() : 0L;
	}

	/** Whole seconds from {@code time} (null: 1970) to {@code now} */
	public static long secondsSince(Instant time, Instant now)
	{
		return (now.toEpochMilli() - millis(time)) / 1000L;
	}

	/** Whole minutes from {@code time} (null: 1970) to {@code now} */
	public static long minutesSince(Instant time, Instant now)
	{
		return (now.toEpochMilli() - millis(time)) / 60000L;
	}

	/**
	 * Whether a prayer to {@code godName} counts now: enough minutes since the last one. Praying to another god than
	 * the current one always counts (the count starts over). {@code believer} may be null (never prayed).
	 */
	public static boolean canPray(Believer believer, String godName, Instant now, int minMinutesBetweenPrayers)
	{
		Instant lastPrayer = believer != null ? believer.lastPrayer() : null;
		String oldGod = believer != null ? believer.godName() : null;
		if (oldGod != null && !oldGod.equals(godName))
		{
			lastPrayer = null;
		}
		return minutesSince(lastPrayer, now) >= minMinutesBetweenPrayers;
	}

	/**
	 * Counts {@code amount} prayers to {@code godName} now (check {@link #canPray} first). Praying to another god
	 * starts the count over; {@code recordJoin} also sets the time they joined that god.
	 */
	public static void recordPrayer(Believer believer, String godName, int amount, Instant now, boolean recordJoin)
	{
		String oldGod = believer.godName();
		int prayers = believer.prayers();
		if (oldGod != null && !oldGod.equals(godName))
		{
			prayers = 0;
		}
		if (recordJoin && (oldGod == null || !oldGod.equals(godName)))
		{
			believer.setJoined(now);
		}
		believer.setLastPrayer(now);
		believer.setGodName(godName);
		believer.setPrayers(prayers + amount);
	}

	/**
	 * Minutes until the believer can pray again, rounded up, so it is at least 1 while they can't (never "try again in 0
	 * minutes"); negative or 0 when they can
	 */
	public static int minutesUntilCanPray(Instant lastPrayer, Instant now, int minMinutesBetweenPrayers)
	{
		long diff = minMinutesBetweenPrayers * 60 * 1000L + millis(lastPrayer) - now.toEpochMilli();
		return (int) Math.ceilDiv(diff, 60_000L);
	}

	/** Whether the believer prayed within the last {@code minMinutesBetweenPrayers} minutes (inclusive) */
	public static boolean hasRecentPrayer(Instant lastPrayer, Instant now, int minMinutesBetweenPrayers)
	{
		return minutesSince(lastPrayer, now) <= minMinutesBetweenPrayers;
	}

	/**
	 * How strong the believer's prayers are: their prayer count, weakening the longer ago they last prayed.
	 */
	public static float believerPower(int prayers, Instant lastPrayer, Instant now)
	{
		float time = 1.0F + 2.5E-008F * (now.toEpochMilli() - millis(lastPrayer));
		return prayers / time;
	}

	/** Whether the believer joined their god within the last {@code minSeconds} seconds. Never when unknown. */
	public static boolean joinedRecently(Instant joined, Instant now, int minSeconds)
	{
		return joined != null && secondsSince(joined, now) <= minSeconds;
	}

	/** Whether the believer asked to change god within the last {@link #CHANGING_GOD_SECONDS}. Never when unknown. */
	public static boolean isChangingGod(Instant changingGod, Instant now)
	{
		return changingGod != null && secondsSince(changingGod, now) <= CHANGING_GOD_SECONDS;
	}

	/** Whether an invitation sent at {@code time} has expired */
	public static boolean invitationExpired(Instant time, Instant now)
	{
		return secondsSince(time, now) > INVITATION_SECONDS;
	}

	// ---------------------------------------------------------------------
	// Prayer power decay

	/**
	 * How long a believer of {@code level} may go without a prayer before their prayer power decays:
	 * {@code believers.decay-after-days} times the level's {@code decay-after-multiplier}, to the minute. Null is a
	 * follower.
	 */
	public static Duration quietPeriod(Settings.Believers believers, DevotionLevel level)
	{
		return Duration.ofMinutes(Math.round(believers.decayAfterDays() * 24.0D * 60.0D * believers.multiplier(level)));
	}

	/** Prayer power after one day of decay: {@code percent} percent of it (rounded up) and then {@code perDay} go, never below 0 */
	public static int decayOneDay(int power, int perDay, int percent)
	{
		if (power <= 0)
		{
			return 0;
		}
		int lost = (int) (((long) power * percent + 99) / 100) + perDay;
		return Math.max(0, power - lost);
	}

	/**
	 * Prayer power after decay, up to when it is counted, and whether the believer has run out
	 *
	 * @param power        the prayer power left
	 * @param decayedUntil up to when decay is counted (null: it never decayed)
	 * @param lost         the believer leaves their religion: the decay has brought their prayer power to 0
	 */
	public record Decay(int power, Instant decayedUntil, boolean lost)
	{
	}

	/**
	 * The decay of a believer's prayer power: from {@code quiet} after their last prayer (or from where decay was
	 * counted to, if later), every whole day takes {@link #decayOneDay}. The believer is lost when at least one day of
	 * decay has passed since the quiet period ended and their prayer power is 0 then, also when it was 0 before; never
	 * while they pray within their quiet period, whatever their prayer power, and never when nothing decays
	 * ({@link Settings.Believers#decays()}). A missing last prayer counts as long ago, like the old 1970 fallback.
	 */
	public static Decay decay(Settings.Believers believers, int power, Instant lastPrayer, Instant decayedUntil, Duration quiet, Instant now)
	{
		if (!believers.decays())
		{
			return new Decay(power, decayedUntil, false);
		}
		Instant quietEnds = Instant.ofEpochMilli(millis(lastPrayer)).plus(quiet);
		Instant start = decayedUntil != null && decayedUntil.isAfter(quietEnds) ? decayedUntil : quietEnds;
		long days = now.isAfter(start) ? Duration.between(start, now).toDays() : 0;
		int left = Math.max(0, power);
		for (long day = 0; day < days && left > 0; day++)
		{
			left = decayOneDay(left, believers.decayPerDay(), believers.decayPercentPerDay());
		}
		Instant counted = days >= 1 ? start.plus(Duration.ofDays(days)) : decayedUntil;
		boolean lost = left == 0 && !now.isBefore(quietEnds.plus(Duration.ofDays(1)));
		return new Decay(left, counted, lost);
	}
}
