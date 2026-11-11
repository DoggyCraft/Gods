package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.config.Theme;
import com.dogonfire.gods.model.Altar;
import com.dogonfire.gods.model.AltarKind;
import com.dogonfire.gods.model.BlockPos;
import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.model.NpcSkin;

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

	private static NpcSkin skin(String id)
	{
		return new NpcSkin(null, id, "sig-" + id);
	}

	/** /g godskin wins over the themes; nothing at all is the plain look */
	@Test
	void theGodsOwnSkinComesFirst()
	{
		NpcSkin own = NpcSkin.named("Notch");
		Theme.NpcSkins theme = new Theme.NpcSkins(skin("god"), skin("goddess"), skin("deity"));
		assertEquals(own, GodNpcRules.skin(own, GodGender.Male, theme, theme));
		assertEquals(own, GodNpcRules.skin(own, GodGender.NonBinary, theme, theme));
		assertEquals(skin("god"), GodNpcRules.skin(new NpcSkin(" ", null, null), GodGender.Male, theme, null));
		assertNull(GodNpcRules.skin(null, GodGender.Male, null, null));
		assertNull(GodNpcRules.skin(null, GodGender.Female, Theme.NpcSkins.NONE, Theme.NpcSkins.NONE));
		assertNull(GodNpcRules.skin(null, GodGender.NonBinary, Theme.NpcSkins.NONE, Theme.NpcSkins.NONE));
	}

	/** Each gender has its entry: Male and None (gods from before genders) god, Female goddess, NonBinary deity */
	@Test
	void eachGenderWearsItsEntry()
	{
		Theme.NpcSkins theme = new Theme.NpcSkins(skin("god"), skin("goddess"), skin("deity"));
		assertEquals(skin("god"), GodNpcRules.skin(null, GodGender.Male, theme, null));
		assertEquals(skin("goddess"), GodNpcRules.skin(null, GodGender.Female, theme, null));
		assertEquals(skin("deity"), GodNpcRules.skin(null, GodGender.NonBinary, theme, null));
		assertEquals(skin("god"), GodNpcRules.skin(null, GodGender.None, theme, null));
		assertEquals(skin("god"), GodNpcRules.skin(null, null, theme, null));
	}

	/**
	 * The order: a god (and None) the type's god, the default god; a goddess the type's goddess, the default goddess; a
	 * deity the type's deity, the type's god, the default deity, the default god; else the plain look
	 */
	@Test
	void skinsFallBackInOrder()
	{
		Theme.NpcSkins defaults = new Theme.NpcSkins(skin("d-god"), skin("d-goddess"), null);
		Theme.NpcSkins thunder = new Theme.NpcSkins(skin("t-god"), null, null);
		Theme.NpcSkins love = new Theme.NpcSkins(null, skin("l-goddess"), null);
		Theme.NpcSkins sea = new Theme.NpcSkins(null, null, skin("s-deity"));

		assertEquals(skin("t-god"), GodNpcRules.skin(null, GodGender.Male, thunder, defaults));
		assertEquals(skin("t-god"), GodNpcRules.skin(null, GodGender.None, thunder, defaults));
		assertEquals(skin("d-goddess"), GodNpcRules.skin(null, GodGender.Female, thunder, defaults));
		assertEquals(skin("d-god"), GodNpcRules.skin(null, GodGender.Male, love, defaults));
		assertEquals(skin("d-god"), GodNpcRules.skin(null, GodGender.None, love, defaults));
		assertEquals(skin("l-goddess"), GodNpcRules.skin(null, GodGender.Female, love, defaults));
		// A deity: its type's deity, its type's god, the default deity, the default god; never a goddess
		assertEquals(skin("s-deity"), GodNpcRules.skin(null, GodGender.NonBinary, sea, defaults));
		assertEquals(skin("t-god"), GodNpcRules.skin(null, GodGender.NonBinary, thunder, defaults));
		assertEquals(skin("d-god"), GodNpcRules.skin(null, GodGender.NonBinary, love, defaults));
		Theme.NpcSkins defaultsWithDeity = new Theme.NpcSkins(skin("d-god"), skin("d-goddess"), skin("d-deity"));
		assertEquals(skin("d-deity"), GodNpcRules.skin(null, GodGender.NonBinary, love, defaultsWithDeity));
		assertEquals(skin("t-god"), GodNpcRules.skin(null, GodGender.NonBinary, thunder, defaultsWithDeity));
		// Gods and deities don't borrow a goddess's skin, nor goddesses a god's: the plain look
		Theme.NpcSkins onlyGoddess = new Theme.NpcSkins(null, skin("d-goddess"), null);
		assertNull(GodNpcRules.skin(null, GodGender.Male, love, onlyGoddess));
		assertNull(GodNpcRules.skin(null, GodGender.NonBinary, love, onlyGoddess));
		assertNull(GodNpcRules.skin(null, GodGender.Female, thunder, new Theme.NpcSkins(skin("d-god"), null, null)));
		// A god without a type: its theme is the default section
		assertEquals(skin("d-god"), GodNpcRules.skin(null, GodGender.NonBinary, defaults, defaults));
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
