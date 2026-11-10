package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AltarSignsTest
{
	@Test
	void finishedAltars()
	{
		assertTrue(AltarSigns.isFormatted(new String[] { "Altar", "of", "Thor", "" }));
		assertTrue(AltarSigns.isFormatted(new String[] { "§6Curses ", "On", "Notch", "" }));
		assertTrue(AltarSigns.isFormatted(new String[] { "Blessings", "On", "", "" }));
		assertFalse(AltarSigns.isFormatted(new String[] { "Thor", "", "", "" }));
		assertFalse(AltarSigns.isFormatted(new String[] { "curses", "on", "Notch", "" }), "finished signs are written by Gods, in its case");
		assertFalse(AltarSigns.isFormatted(null));
	}

	@Test
	void thePlayerOnACurseOrBlessingSign()
	{
		assertEquals("Notch", AltarSigns.namedPlayer(new String[] { "CURSES", "", " §cNotch ", "" }, AltarSigns.CURSES));
		assertNull(AltarSigns.namedPlayer(new String[] { "Curses", "On", "  ", "" }, AltarSigns.CURSES));
		assertNull(AltarSigns.namedPlayer(new String[] { "Blessings", "On", "Notch", "" }, AltarSigns.CURSES));
		assertNull(AltarSigns.namedPlayer(new String[] { "Curses", "On" }, AltarSigns.CURSES));
	}

	@Test
	void aNewPrayingAltarHasOnlyTheGodsName()
	{
		assertEquals("Thor", AltarSigns.godNameOnNewSign(new String[] { "", "Thor", "", "" }).godName());
		assertEquals(" thor ", AltarSigns.godNameOnNewSign(new String[] { "", "", "", " thor " }).godName());
		assertEquals(AltarSigns.Problem.INVALID_SIGN, AltarSigns.godNameOnNewSign(new String[] { "Thor", "", "Odin", "" }).problem());
		assertEquals(AltarSigns.Problem.INVALID_GOD_NAME, AltarSigns.godNameOnNewSign(new String[] { "", "T", "", "" }).problem());
		assertEquals(AltarSigns.Problem.INVALID_GOD_NAME, AltarSigns.godNameOnNewSign(new String[] { "", "", "", "" }).problem());
	}

	@Test
	void writtenLines()
	{
		assertEquals(1, AltarSigns.writtenLines(new String[] { "", "Thor", " ", "§a" }));
		assertEquals(2, AltarSigns.writtenLines(new String[] { "Curses", "", "Notch", null }));
	}
}
