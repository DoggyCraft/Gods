package com.dogonfire.gods.service;

import org.bukkit.ChatColor;

/**
 * Reading the text of altar signs, without the server so it can be tested. Altars are written on the front of a wall
 * sign on an altar block: "Altar / of / &lt;god&gt;" (praying), "Curses / On / &lt;player&gt;" and
 * "Blessings / On / &lt;player&gt;". A new praying altar is a sign with only the god's name on it.
 */
public final class AltarSigns
{
	public static final String	ALTAR		= "Altar";
	public static final String	CURSES		= "curses";
	public static final String	BLESSINGS	= "blessings";

	/** Why the text of a new praying altar sign is not a god name */
	public enum Problem
	{
		/** More than one line is written */
		INVALID_SIGN,
		/** The name is too short */
		INVALID_GOD_NAME
	}

	/** The god name written on a new praying altar sign (untrimmed), or the problem with the sign */
	public record GodNameLine(String godName, Problem problem)
	{
	}

	private AltarSigns()
	{
	}

	/** The text of a line without colors and surrounding spaces; "" for null */
	public static String plain(String line)
	{
		return line != null ? ChatColor.stripColor(line).trim() : "";
	}

	/** True if the lines show a finished altar ("Altar of ...", "Curses On ...", "Blessings On ...") */
	public static boolean isFormatted(String[] lines)
	{
		if (lines == null || lines.length < 2)
		{
			return false;
		}

		String first = plain(lines[0]);
		String second = plain(lines[1]);

		return (first.equals(ALTAR) && second.equals("of")) || ((first.equals("Curses") || first.equals("Blessings")) && second.equals("On"));
	}

	/**
	 * The player name on line 3 of a sign whose first line is {@code keyword} (any case), e.g. "curses"; null if the
	 * sign isn't like that or names nobody.
	 */
	public static String namedPlayer(String[] lines, String keyword)
	{
		if (lines == null || lines.length < 3 || lines[0] == null)
		{
			return null;
		}

		if (!plain(lines[0]).equalsIgnoreCase(keyword))
		{
			return null;
		}

		String playerName = lines[2];
		if ((playerName == null) || (playerName.trim().length() < 1))
		{
			return null;
		}

		return plain(playerName);
	}

	/** The number of lines with visible text */
	public static int writtenLines(String[] lines)
	{
		int written = 0;
		for (String text : lines)
		{
			if ((text != null) && !ChatColor.stripColor(text).isBlank())
			{
				written++;
			}
		}
		return written;
	}

	/**
	 * The god's name on a new praying altar sign: the first written line, with every other line empty. The name is
	 * returned as written (the caller trims and formats it).
	 */
	public static GodNameLine godNameOnNewSign(String[] lines)
	{
		String godName = "";
		int line = 0;
		while (godName.isEmpty() && line < 4)
		{
			String text = lines[line++];
			godName = (text != null) ? text : "";
		}
		line--;

		for (int other = 0; other < 4; other++)
		{
			if (other == line)
			{
				continue;
			}
			String text = lines[other];
			if ((text != null) && (text.length() > 0))
			{
				return new GodNameLine(null, Problem.INVALID_SIGN);
			}
		}

		if (godName.length() <= 1)
		{
			return new GodNameLine(null, Problem.INVALID_GOD_NAME);
		}
		return new GodNameLine(godName, null);
	}
}
