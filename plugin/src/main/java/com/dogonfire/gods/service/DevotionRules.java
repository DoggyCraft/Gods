package com.dogonfire.gods.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.model.DevotionLevel;

/**
 * How a believer's devotion grows, fades and what it unlocks (pure, tested), with config.yml's {@code devotion}:
 * <ul>
 * <li>Deeds earn points ({@code devotion.points}); a believer rises as soon as their points reach the next level's
 * threshold ({@link #levelFor}), several levels at once if the points allow.</li>
 * <li>After {@code fade-after-days} days without a prayer the points fade by {@code fade-percent-per-day} percent per
 * whole day ({@link #fade}); a prayer stops it.</li>
 * <li>A level is kept until the points fall below {@code keep-level-percent} percent of its threshold
 * ({@link #evaluate}), so a few days off never cost a level; then the believer falls to the level their points
 * reach.</li>
 * <li>Leaving the god or changing to another keeps {@code kept-on-changing-god-percent} percent of the points
 * ({@link #afterLeaving}), and the level is what those points reach.</li>
 * <li>A believer from before devotion is placed once by {@link #seed}.</li>
 * </ul>
 */
public final class DevotionRules
{
	private DevotionRules()
	{
	}

	/** The highest level these points reach */
	public static DevotionLevel levelFor(Settings.Devotion devotion, int points)
	{
		DevotionLevel reached = DevotionLevel.FOLLOWER;
		for (DevotionLevel level : DevotionLevel.values())
		{
			if (points >= devotion.threshold(level))
			{
				reached = level;
			}
		}
		return reached;
	}

	/** The points below which a believer loses {@code level}: {@code keep-level-percent} of its threshold (rounded up) */
	public static int keepThreshold(Settings.Devotion devotion, DevotionLevel level)
	{
		long threshold = devotion.threshold(level);
		return (int) ((threshold * devotion.keepLevelPercent() + 99) / 100);
	}

	/**
	 * The believer's level after a change of points: higher when the points reach a higher level, lower only when they
	 * fell below {@link #keepThreshold} of the current one (then the level they reach), else the same
	 */
	public static DevotionLevel evaluate(Settings.Devotion devotion, DevotionLevel current, int points)
	{
		DevotionLevel level = current != null ? current : DevotionLevel.FOLLOWER;
		DevotionLevel reached = levelFor(devotion, points);
		if (reached.compareTo(level) > 0)
		{
			return reached;
		}
		if (points < keepThreshold(devotion, level))
		{
			return reached;
		}
		return level;
	}

	/**
	 * Points after fading, and how far the fading has been counted
	 *
	 * @param points     the points left
	 * @param fadedUntil up to when fading is counted (null: it never faded)
	 */
	public record Faded(int points, Instant fadedUntil)
	{
	}

	/**
	 * Fades the points of a believer who hasn't prayed: from {@code fade-after-days} days after their last prayer (or
	 * from where fading was counted to, if later), {@code fade-percent-per-day} percent of the points go for every whole
	 * day, compounded and rounded down. Nothing fades without a known last prayer, with no points or with fading off.
	 */
	public static Faded fade(Settings.Devotion devotion, int points, Instant lastPrayer, Instant fadedUntil, Instant now)
	{
		if (devotion.fadePercentPerDay() <= 0 || lastPrayer == null || points <= 0)
		{
			return new Faded(points, fadedUntil);
		}
		Instant start = lastPrayer.plus(Duration.ofDays(devotion.fadeAfterDays()));
		if (fadedUntil != null && fadedUntil.isAfter(start))
		{
			start = fadedUntil;
		}
		long days = now.isAfter(start) ? Duration.between(start, now).toDays() : 0;
		if (days < 1)
		{
			return new Faded(points, fadedUntil);
		}
		double kept = Math.pow(1.0 - devotion.fadePercentPerDay() / 100.0, days);
		return new Faded((int) Math.floor(points * kept), start.plus(Duration.ofDays(days)));
	}

	/** The points a believer keeps when they leave their god or change to another */
	public static int afterLeaving(Settings.Devotion devotion, int points)
	{
		return (int) ((long) points * devotion.keptOnChangingGodPercent() / 100);
	}

	/**
	 * The starting points of a believer from before devotion: their prayers to their god times
	 * {@code devotion.points.prayer}, at most what a {@link DevotionLevel#ZEALOT} needs (saints are earned under
	 * Gods' eyes). 0 without a god.
	 */
	public static int seed(Settings.Devotion devotion, boolean hasGod, int prayers)
	{
		if (!hasGod || prayers <= 0)
		{
			return 0;
		}
		long points = (long) prayers * devotion.points().prayer();
		return (int) Math.min(points, devotion.threshold(DevotionLevel.ZEALOT));
	}

	/** Points a believer still needs for {@code next}; 0 when they have them */
	public static int pointsToGo(Settings.Devotion devotion, DevotionLevel next, int points)
	{
		return next == null ? 0 : Math.max(0, devotion.threshold(next) - points);
	}

	/** Whether a believer of {@code level} has what needs {@code needed} */
	public static boolean unlocked(DevotionLevel level, DevotionLevel needed)
	{
		return (level != null ? level : DevotionLevel.FOLLOWER).atLeast(needed != null ? needed : DevotionLevel.FOLLOWER);
	}

	/** What /g prayfor costs with a discount of {@code percent} percent: the discount is rounded down */
	public static int discountedCost(int cost, int percent)
	{
		return cost - (int) ((long) cost * percent / 100);
	}

	/**
	 * The bound of a {@code random.nextInt(bound) == 0} roll for a blessing, made {@code multiplier} times as likely
	 * (never below 1, which always succeeds)
	 */
	public static int blessingBound(int bound, int multiplier)
	{
		return Math.max(1, bound / Math.max(1, multiplier));
	}

	/** Whether a commandment kept at {@code last} (null: never) may earn points again now */
	public static boolean commandmentReady(Instant last, Instant now, int cooldownMinutes)
	{
		return last == null || !now.isBefore(last.plus(Duration.ofMinutes(cooldownMinutes)));
	}

	// ---------------------------------------------------------------------
	// Priests

	/**
	 * A believer the god could make its priest
	 *
	 * @param devotion how devoted they are within their level: their devotion points, or their prayer power while
	 *            devotion is off
	 * @param joined when they joined the god, or null if unknown
	 */
	public record PriestCandidate<T>(T id, DevotionLevel level, double devotion, Instant joined)
	{
	}

	/**
	 * The candidates in the order the god asks them: the highest devotion level first, then the most devotion (points,
	 * or prayer power while devotion is off), then whoever joined first (unknown last)
	 */
	public static <T> List<PriestCandidate<T>> priestOrder(List<PriestCandidate<T>> candidates)
	{
		List<PriestCandidate<T>> ordered = new ArrayList<>(candidates);
		ordered.sort(Comparator.<PriestCandidate<T>, DevotionLevel> comparing(PriestCandidate::level).reversed()
				.thenComparing(Comparator.<PriestCandidate<T>>comparingDouble(PriestCandidate::devotion).reversed())
				.thenComparing(PriestCandidate::joined, Comparator.nullsLast(Comparator.naturalOrder())));
		return ordered;
	}

	/**
	 * The candidate the god offers priesthood: the first in {@link #priestOrder}, never one at random
	 *
	 * @return null if there are none
	 */
	public static <T> T pickPriest(List<PriestCandidate<T>> candidates)
	{
		List<PriestCandidate<T>> ordered = priestOrder(candidates);
		return ordered.isEmpty() ? null : ordered.getFirst().id();
	}
}
