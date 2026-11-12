package com.dogonfire.gods.service;

import java.util.List;
import java.util.Random;

import com.dogonfire.gods.config.Theme;
import com.dogonfire.gods.model.GodMood;
import com.dogonfire.gods.model.GodRank;

/**
 * The numbers and choices of the gods' effects ({@link EffectService}), without the server so they can be tested:
 * which of a theme's styles an effect uses, how many particles in how many waves, how high they rise, and the pitch of
 * the sound. Every count is small and fixed, so an effect costs the same however often it is shown.
 */
public final class EffectRules
{
	/** The most effect displays (floating items and symbols) alive at once; more effects then skip theirs */
	public static final int		MAX_DISPLAYS	= 32;

	/**
	 * Particles in waves: {@code waves} rings of {@code particles} each, {@code ticksApart} apart, from the start height
	 * up by {@code rise} (down when negative), on a ring of {@code radius} that turns a little each wave.
	 *
	 * @param pitch the sound's pitch (0.5 low to 2.0 high)
	 */
	public record Waves(int waves, int particles, double rise, double radius, int ticksApart, float pitch)
	{
		/** Every particle this effect shows, per viewer */
		public int total()
		{
			return this.waves * this.particles;
		}
	}

	/**
	 * A sacrifice floating up as a display: up by {@code rise} blocks in {@code steps} steps of {@code ticksPerStep},
	 * turning {@code spinDegrees} each step (less than 180, so the client turns the right way), then shrinking away.
	 *
	 * @param pitch the sound's pitch
	 */
	public record Rise(double rise, int steps, int ticksPerStep, float spinDegrees, float pitch)
	{
		/** The ticks the display lives */
		public int ticks()
		{
			return this.steps * this.ticksPerStep;
		}
	}

	/** A blessing: a soft glow rising around the player, with a high chime */
	public static final Waves	BLESSING		= new Waves(4, 8, 1.8, 0.7, 3, 1.6F);
	/** A curse: a dark swirl sinking around the player, with a low sound */
	public static final Waves	CURSE			= new Waves(4, 8, -1.6, 0.8, 3, 0.5F);

	private EffectRules()
	{
	}

	/** The effect style of a god's mood: exalted and pleased gods are pleased, displeased and angry ones angry */
	public static Theme.EffectMood moodOf(GodMood mood)
	{
		if (mood == null)
		{
			return Theme.EffectMood.NEUTRAL;
		}
		return switch (mood)
		{
			case EXALTED, PLEASED -> Theme.EffectMood.PLEASED;
			case DISPLEASED, ANGRY -> Theme.EffectMood.ANGRY;
			case NEUTRAL -> Theme.EffectMood.NEUTRAL;
		};
	}

	/** The effect style of a sacrifice: wanted ones please, unwanted ones anger */
	public static Theme.EffectMood sacrificeMood(SacrificeRules.Verdict verdict)
	{
		return switch (verdict)
		{
			case GOOD -> Theme.EffectMood.PLEASED;
			case MEH -> Theme.EffectMood.NEUTRAL;
			case BAD -> Theme.EffectMood.ANGRY;
		};
	}

	/** A prayer: bright and rising high when the god is pleased, low and short when it is angry */
	public static Waves prayer(Theme.EffectMood mood)
	{
		return switch (mood)
		{
			case PLEASED -> new Waves(5, 10, 2.2, 0.8, 3, 1.4F);
			case NEUTRAL -> new Waves(4, 8, 1.4, 0.8, 3, 1.0F);
			case ANGRY -> new Waves(3, 8, 0.5, 0.6, 2, 0.6F);
		};
	}

	/**
	 * How grand a god's altar effects are, by its rank: a spirit's prayers are a modest flicker, the waves grow with each
	 * rank, the symbol floats up from the descended rank on and a sacrifice floats up as an item from recognized on; a
	 * greater god's are the grandest.
	 *
	 * @param scale            how many waves and particles, and how high they rise, against the base effect
	 * @param symbol           whether the theme's symbol floats up at prayers
	 * @param sacrificeDisplay whether a sacrifice floats up as an item (else only particles at the fire)
	 */
	public record Tier(double scale, boolean symbol, boolean sacrificeDisplay)
	{
	}

	/** The tier of a rank's effects (null: a spirit's) */
	public static Tier tier(GodRank rank)
	{
		return switch (rank != null ? rank : GodRank.SPIRIT)
		{
			case SPIRIT -> new Tier(0.4, false, false);
			case RECOGNIZED -> new Tier(0.7, false, true);
			case DESCENDED -> new Tier(1.0, true, true);
			case EXALTED -> new Tier(1.25, true, true);
			case GREATER -> new Tier(1.5, true, true);
		};
	}

	/** A prayer's waves for a god of this rank: the mood's waves ({@link #prayer(Theme.EffectMood)}) scaled by its tier */
	public static Waves prayer(Theme.EffectMood mood, GodRank rank)
	{
		return scaled(prayer(mood), tier(rank).scale());
	}

	/** Whether the symbol floats up at a prayer to a god of this rank: from the descended rank on, not for an angry god */
	public static boolean showsSymbol(Theme.EffectMood mood, String symbol, GodRank rank)
	{
		return tier(rank).symbol() && showsSymbol(mood, symbol);
	}

	/**
	 * A god rose to a new rank: pleased waves at its main altar, grander for higher ranks (more than a prayer of that
	 * rank, and a little more again for each rank)
	 */
	/**
	 * A believer rose to a devotion level: the god's pleased particles rise around them, more for higher levels, with a
	 * chime that rises with the level
	 */
	public static Waves devotionUp(com.dogonfire.gods.model.DevotionLevel level)
	{
		int step = level != null ? level.ordinal() : 0;
		return new Waves(BLESSING.waves() + step, BLESSING.particles() + 2 * step, BLESSING.rise() + 0.2 * step, BLESSING.radius() + 0.1 * step, BLESSING.ticksApart(),
				1.2F + 0.15F * step);
	}

	public static Waves rankUp(GodRank rank)
	{
		int step = rank != null ? rank.ordinal() : 0;
		Waves base = scaled(prayer(Theme.EffectMood.PLEASED), tier(rank).scale());
		return new Waves(base.waves() + 1 + step, base.particles() + step, base.rise() + 0.4 * step, base.radius() + 0.1 * step, base.ticksApart(), 1.2F + 0.1F * step);
	}

	/** Waves with {@code scale} times the waves, particles and rise (at least two waves of four particles) */
	static Waves scaled(Waves waves, double scale)
	{
		return new Waves(Math.max(2, (int) Math.round(waves.waves() * scale)), Math.max(4, (int) Math.round(waves.particles() * scale)), waves.rise() * scale,
				waves.radius() * Math.min(1.3, Math.max(0.7, scale)), waves.ticksApart(), waves.pitch());
	}

	/** A sacrifice: a wanted one floats high and spins, an unwanted one barely rises and is cast down */
	public static Rise sacrifice(Theme.EffectMood mood)
	{
		return switch (mood)
		{
			case PLEASED -> new Rise(1.6, 4, 8, 120F, 1.5F);
			case NEUTRAL -> new Rise(1.0, 3, 8, 90F, 1.0F);
			case ANGRY -> new Rise(0.4, 3, 6, 45F, 0.6F);
		};
	}

	/** Whether the symbol floats up at a prayer: not for an angry god */
	public static boolean showsSymbol(Theme.EffectMood mood, String symbol)
	{
		return mood != Theme.EffectMood.ANGRY && symbol != null && !symbol.isBlank();
	}

	/** True if a player this far (squared) from an effect sees and hears it */
	public static boolean inView(double distanceSquared, int viewDistance)
	{
		return distanceSquared <= (double) viewDistance * viewDistance;
	}

	/** One of the list at random, or null if it is empty */
	public static <T> T pick(List<T> list, Random random)
	{
		return list.isEmpty() ? null : list.get(random.nextInt(list.size()));
	}

	/**
	 * Where a particle of a wave goes, from the effect's center: {x, y, z}. The ring turns by a fixed step each wave, so
	 * the waves spiral.
	 */
	public static double[] ringOffset(Waves waves, int wave, int index)
	{
		double angle = 2 * Math.PI * index / waves.particles() + wave * 0.6;
		double height = waves.waves() > 1 ? waves.rise() * wave / (waves.waves() - 1) : 0;
		return new double[] { Math.cos(angle) * waves.radius(), height, Math.sin(angle) * waves.radius() };
	}

	/** An RGB color as legacy text color codes ({@code §x§r§r§g§g§b§b}) */
	public static String legacyColor(int rgb)
	{
		String hex = String.format("%06x", rgb & 0xFFFFFF);
		StringBuilder code = new StringBuilder("§x");
		for (char digit : hex.toCharArray())
		{
			code.append('§').append(digit);
		}
		return code.toString();
	}
}
