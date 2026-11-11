package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.dogonfire.gods.model.GodGender;

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

	private static final List<String>	MALE	= List.of("God");
	private static final List<String>	FEMALE	= List.of("Goddess", "Gudinde");

	private static AltarSigns.NewAltar newAltar(String... lines)
	{
		return AltarSigns.newPrayingAltar(lines, MALE, FEMALE);
	}

	/** The old way: only the god's name */
	@Test
	void aNewPrayingAltarHasTheGodsName()
	{
		assertEquals("Thor", newAltar("", "Thor", "", "").godName());
		assertNull(newAltar("", "Thor", "", "").gender());
		assertEquals(" thor ", newAltar("", "", "", " thor ").godName());
		assertEquals(AltarSigns.Problem.INVALID_SIGN, newAltar("Thor", "", "Odin", "").problem());
		assertEquals(AltarSigns.Problem.INVALID_SIGN, newAltar("Thor", "God", "Odin", "").problem());
		assertEquals(AltarSigns.Problem.INVALID_GOD_NAME, newAltar("", "T", "", "").problem());
		assertEquals(AltarSigns.Problem.INVALID_GOD_NAME, newAltar("", "", "", "").problem());
		// A god may still be named like a gender word, when it is the only line
		assertEquals("Goddess", newAltar("Goddess", "", "", "").godName());
		assertNull(newAltar("Goddess", "", "", "").gender());
	}

	/** "God" or "Goddess" on another line, above or below the name, any case */
	@Test
	void theGenderIsWrittenOnAnotherLine()
	{
		assertEquals(new AltarSigns.NewAltar("Freya", GodGender.Female, null), newAltar("Freya", "goddess", "", ""));
		assertEquals(new AltarSigns.NewAltar("Thor", GodGender.Male, null), newAltar("", "§6GOD", "", "Thor"));
		assertEquals(new AltarSigns.NewAltar("Frigg", GodGender.Female, null), newAltar("Frigg", "", "Gudinde", ""));
		assertEquals(AltarSigns.Problem.INVALID_SIGN, newAltar("God", "Goddess", "", "").problem());
		assertEquals(AltarSigns.Problem.INVALID_GOD_NAME, newAltar("T", "God", "", "").problem());
	}

	@Test
	void genderWords()
	{
		assertEquals(GodGender.Male, AltarSigns.genderWord(" god ", MALE, FEMALE));
		assertEquals(GodGender.Female, AltarSigns.genderWord("§dGoddess", MALE, FEMALE));
		assertNull(AltarSigns.genderWord("Gods", MALE, FEMALE));
		assertNull(AltarSigns.genderWord(null, MALE, FEMALE));
	}

	/** A tagged sign is known by its tags, whatever its text says */
	@Test
	void taggedAltarsAreKnownByTheirTags()
	{
		String id = "4b0d5e8c-1d3a-4bfa-9d3c-6c6f1f3f2a10";
		AltarSigns.AltarSign praying = AltarSigns.recognize("praying", "Thor", null, null, new String[] { "§b❄ Altar ❄", "§3of the frost", "§fThor", "" });
		assertEquals(new AltarSigns.AltarSign(AltarSigns.Kind.PRAYING, "Thor", null, null, true), praying);

		AltarSigns.AltarSign curses = AltarSigns.recognize("curses", "Loki", "Notch", id, new String[] { "", "", "", "" });
		assertEquals(new AltarSigns.AltarSign(AltarSigns.Kind.CURSES, "Loki", "Notch", java.util.UUID.fromString(id), true), curses);

		// The tag wins over the text
		assertEquals(AltarSigns.Kind.BLESSINGS, AltarSigns.recognize("blessings", null, "Notch", "not a uuid", new String[] { "Altar", "of", "Thor", "" }).kind());
		assertNull(AltarSigns.recognize("blessings", null, "Notch", "not a uuid", new String[4]).playerId());
	}

	/** Altars from before tags are known by their text, and are not tagged yet */
	@Test
	void untaggedAltarsAreKnownByTheirText()
	{
		assertEquals(new AltarSigns.AltarSign(AltarSigns.Kind.PRAYING, "Thor", null, null, false), AltarSigns.recognize(null, null, null, null, new String[] { "Altar", "of", " §6Thor ", "" }));
		assertEquals(new AltarSigns.AltarSign(AltarSigns.Kind.CURSES, null, "Notch", null, false), AltarSigns.recognize(null, null, null, null, new String[] { "Curses", "On", "Notch", "" }));
		assertEquals(new AltarSigns.AltarSign(AltarSigns.Kind.BLESSINGS, null, "Notch", null, false), AltarSigns.recognize(null, null, null, null, new String[] { "Blessings", "On", "Notch", "" }));
		// An unknown kind in the tag: read the text
		assertEquals(AltarSigns.Kind.PRAYING, AltarSigns.recognize("weather", null, null, null, new String[] { "Altar", "of", "Thor", "" }).kind());
		assertNull(AltarSigns.recognize(null, null, null, null, new String[] { "Thor", "", "", "" }));
		assertNull(AltarSigns.recognize(null, null, null, null, new String[] { "Altar", "for", "Thor", "" }));
		assertNull(AltarSigns.recognize(null, null, null, null, new String[] { "Curses", "On", "", "" }));
		assertNull(AltarSigns.recognize(null, null, null, null, null));
	}

	@Test
	void taggingKeepsTheKindAndPlayer()
	{
		java.util.UUID id = java.util.UUID.randomUUID();
		AltarSigns.AltarSign old = new AltarSigns.AltarSign(AltarSigns.Kind.CURSES, null, "Notch", null, false);
		assertEquals(new AltarSigns.AltarSign(AltarSigns.Kind.CURSES, "Loki", "Notch", id, true), old.asTagged("Loki", id));
	}

	@Test
	void signsHaveFourLines()
	{
		assertArrayEquals(new String[] { "a", "b", "", "" }, AltarSigns.fourLines(List.of("a", "b")));
		assertArrayEquals(new String[] { "a", "b", "c", "d" }, AltarSigns.fourLines(List.of("a", "b", "c", "d", "e")));
	}

	@Test
	void writtenLines()
	{
		assertEquals(1, AltarSigns.writtenLines(new String[] { "", "Thor", " ", "§a" }));
		assertEquals(2, AltarSigns.writtenLines(new String[] { "Curses", "", "Notch", null }));
	}
}
