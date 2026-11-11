package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.model.Altar;
import com.dogonfire.gods.model.AltarKind;
import com.dogonfire.gods.model.BlockPos;

class GodNpcRulesTest
{
	private static final BlockPos BLOCK = new BlockPos("world", 10, 64, 10);

	private static Altar altar(int signX, int signZ)
	{
		return new Altar(new BlockPos("world", signX, 64, signZ), BLOCK, AltarKind.PRAYING, "Zeus", null, null);
	}

	/** Minecraft's yaw: 0 south (+z), 90 west (-x), 180 north (-z), -90 east (+x) */
	@Test
	void yawFollowsMinecraftsCompass()
	{
		assertEquals(0F, GodNpcRules.yawToward(0, 0, 0, 5), 0.001F);
		assertEquals(90F, GodNpcRules.yawToward(0, 0, -5, 0), 0.001F);
		assertEquals(180F, Math.abs(GodNpcRules.yawToward(0, 0, 0, -5)), 0.001F);
		assertEquals(-90F, GodNpcRules.yawToward(0, 0, 5, 0), 0.001F);
		assertEquals(-45F, GodNpcRules.yawToward(0, 0, 3, 3), 0.001F);
		assertEquals(0F, GodNpcRules.yawToward(1, 1, 1, 1), 0.001F);
	}

	@Test
	void pitchIsNegativeLookingUp()
	{
		assertEquals(-45F, GodNpcRules.pitchToward(3, 3, 0), 0.001F);
		assertEquals(45F, GodNpcRules.pitchToward(0, -3, 3), 0.001F);
		assertEquals(0F, GodNpcRules.pitchToward(4, 0, 0), 0.001F);
		assertEquals(-90F, GodNpcRules.pitchToward(0, 2, 0), 0.001F);
		assertEquals(0F, GodNpcRules.pitchToward(0, 0, 0), 0.001F);
	}

	/** It stands in front of the sign and faces the altar, whichever side the sign hangs on */
	@Test
	void facesTheAltarFromItsSpot()
	{
		// Sign on the south side (+z): it stands further south and looks north
		Altar south = altar(10, 11);
		assertEquals(new BlockPos("world", 10, 64, 12), MainAltarRules.standingSpot(south));
		assertEquals(180F, Math.abs(GodNpcRules.facingYaw(south)), 0.001F);
		// North side: looks south
		assertEquals(0F, GodNpcRules.facingYaw(altar(10, 9)), 0.001F);
		// East side (+x): looks west
		assertEquals(90F, GodNpcRules.facingYaw(altar(11, 10)), 0.001F);
		// West side: looks east
		assertEquals(-90F, GodNpcRules.facingYaw(altar(9, 10)), 0.001F);
	}

	@Test
	void turnsOnlyForMoreThanAFewDegrees()
	{
		assertFalse(GodNpcRules.shouldTurn(10F, 12F));
		assertTrue(GodNpcRules.shouldTurn(10F, 20F));
		// Across the -180/180 seam it is a small turn
		assertFalse(GodNpcRules.shouldTurn(179F, -179F));
		assertTrue(GodNpcRules.shouldTurn(170F, -170F));
		assertFalse(GodNpcRules.shouldTurn(0F, 360F));
		assertEquals(-170F, GodNpcRules.wrapDegrees(190F), 0.001F);
		assertEquals(180F, GodNpcRules.wrapDegrees(-180F), 0.001F);
	}

	/** The floating text above the altar names the god already */
	@Test
	void showsItsNameOnlyWithoutFloatingText()
	{
		assertTrue(GodNpcRules.showsName(Settings.FloatingText.OFF));
		assertFalse(GodNpcRules.showsName(Settings.FloatingText.ALL));
		assertFalse(GodNpcRules.showsName(Settings.FloatingText.MAIN));
	}

	@Test
	void fitsOnlyWhereAFigureCanStand()
	{
		assertTrue(GodNpcRules.fits(true, true));
		assertFalse(GodNpcRules.fits(false, true));
		assertFalse(GodNpcRules.fits(true, false));
	}

	@Test
	void aClickPraysAtTheGodsOwnPrayingAltar()
	{
		Altar main = altar(10, 11);
		assertEquals(GodNpcRules.Click.PRAY, GodNpcRules.click(true, "Zeus", main));
		assertEquals(GodNpcRules.Click.OFF_HAND, GodNpcRules.click(false, "Zeus", main));
		assertEquals(GodNpcRules.Click.NO_ALTAR, GodNpcRules.click(true, "Zeus", null));
		assertEquals(GodNpcRules.Click.NO_ALTAR, GodNpcRules.click(true, "Hera", main));
		assertEquals(GodNpcRules.Click.NO_ALTAR, GodNpcRules.click(true, "Zeus", new Altar(main.sign(), BLOCK, AltarKind.CURSES, "Zeus", null, null)));
	}
}
