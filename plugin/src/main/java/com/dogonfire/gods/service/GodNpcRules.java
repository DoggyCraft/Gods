package com.dogonfire.gods.service;

import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.model.Altar;
import com.dogonfire.gods.model.AltarKind;
import com.dogonfire.gods.model.BlockPos;

/**
 * The rules of a god's NPC at its main altar (pure, tested): where it stands and which way it faces, when it turns to
 * a player, whether it shows its name, and what a click on it does.
 */
public final class GodNpcRules
{
	/** At most this many god NPCs at once, whatever the number of main altars in loaded chunks */
	public static final int		MAX_NPCS			= 256;

	/** It turns toward the nearest player this close, in blocks */
	public static final double	LOOK_RADIUS_BLOCKS	= 6.0;

	/** It only turns when the player is off by more than this many degrees (no jitter while they stand still) */
	public static final float	TURN_STEP_DEGREES	= 3.0F;

	/** How high a figure's eyes are above its feet */
	public static final double	EYE_HEIGHT			= 1.62;

	private GodNpcRules()
	{
	}

	/**
	 * The yaw (Minecraft's: 0 is south, +z; 90 is west, -x) of someone at {@code from} looking toward {@code to}; 0 for
	 * the same spot
	 */
	public static float yawToward(double fromX, double fromZ, double toX, double toZ)
	{
		double dx = toX - fromX;
		double dz = toZ - fromZ;
		if (dx == 0 && dz == 0)
		{
			return 0F;
		}
		return (float) Math.toDegrees(Math.atan2(-dx, dz));
	}

	/** The pitch (negative is up) of someone looking {@code dy} up over a horizontal distance */
	public static float pitchToward(double dx, double dy, double dz)
	{
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		if (horizontal == 0 && dy == 0)
		{
			return 0F;
		}
		return (float) -Math.toDegrees(Math.atan2(dy, horizontal));
	}

	/**
	 * The way the god faces at its altar when nobody is near: toward the altar, i.e. from its spot
	 * ({@link MainAltarRules#standingSpot}) toward the sign
	 */
	public static float facingYaw(Altar altar)
	{
		BlockPos spot = MainAltarRules.standingSpot(altar);
		BlockPos sign = altar.sign();
		return yawToward(spot.x(), spot.z(), sign.x(), sign.z());
	}

	/** Whether a figure facing {@code current} should turn to {@code target} (more than {@link #TURN_STEP_DEGREES} off) */
	public static boolean shouldTurn(float current, float target)
	{
		return Math.abs(wrapDegrees(target - current)) > TURN_STEP_DEGREES;
	}

	/** The angle in -180 (exclusive) to 180 (inclusive) */
	public static float wrapDegrees(float degrees)
	{
		float wrapped = degrees % 360F;
		if (wrapped > 180F)
		{
			wrapped -= 360F;
		}
		else if (wrapped <= -180F)
		{
			wrapped += 360F;
		}
		return wrapped;
	}

	/**
	 * Whether the NPC shows the god's name above its head: only when the altar has no floating text, which already
	 * shows it (a main altar has one with {@code altars.floating-text} all or main)
	 */
	public static boolean showsName(Settings.FloatingText floatingText)
	{
		return floatingText == Settings.FloatingText.OFF;
	}

	/** Whether a figure fits on the spot: its feet block and the one above can be walked through */
	public static boolean fits(boolean feetPassable, boolean headPassable)
	{
		return feetPassable && headPassable;
	}

	/** What a right click on a god's NPC does */
	public enum Click
	{
		/** Pray at the god's main altar, as a click on its sign does */
		PRAY,
		/** The off hand's event: already handled with the main hand's (cancelled, nothing else) */
		OFF_HAND,
		/** The god has no usable main altar now (gone, or no longer a praying altar of the god): nothing */
		NO_ALTAR
	}

	/**
	 * What a click on the NPC of {@code npcGod} does
	 *
	 * @param mainHand  whether it is the main hand's event (Bukkit fires one per hand)
	 * @param mainAltar the registered altar at the god's main altar sign, or null
	 */
	public static Click click(boolean mainHand, String npcGod, Altar mainAltar)
	{
		if (!mainHand)
		{
			return Click.OFF_HAND;
		}
		if (mainAltar == null || mainAltar.kind() != AltarKind.PRAYING || !npcGod.equals(mainAltar.god()))
		{
			return Click.NO_ALTAR;
		}
		return Click.PRAY;
	}
}
