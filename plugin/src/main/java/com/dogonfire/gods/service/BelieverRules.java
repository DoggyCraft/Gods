package com.dogonfire.gods.service;

import java.time.Instant;

import com.dogonfire.gods.model.Believer;

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

	/** Minutes until the believer can pray again (negative or 0 when they can) */
	public static int minutesUntilCanPray(Instant lastPrayer, Instant now, int minMinutesBetweenPrayers)
	{
		long diff = minMinutesBetweenPrayers * 60 * 1000 + millis(lastPrayer) - now.toEpochMilli();
		long diffSeconds = diff / 1000L;
		return (int) (diffSeconds / 60L);
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
}
