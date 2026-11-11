package com.dogonfire.gods.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.ChatColor;

import com.dogonfire.gods.model.GodGender;

/**
 * Reading the text of altar signs, without the server so it can be tested. Altars are written on the front of a wall
 * sign on an altar block: "Altar / of / &lt;god&gt;" (praying), "Curses / On / &lt;player&gt;" and
 * "Blessings / On / &lt;player&gt;". A new praying altar is a sign with the god's name on it, and maybe "God" or
 * "Goddess" on another line.
 */
public final class AltarSigns
{
	public static final String	ALTAR		= "Altar";
	public static final String	CURSES		= "curses";
	public static final String	BLESSINGS	= "blessings";

	/** Why the text of a new praying altar sign is not a god name */
	public enum Problem
	{
		/** More than the name and a gender word is written */
		INVALID_SIGN,
		/** The name is too short */
		INVALID_GOD_NAME
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
	 * What a new praying altar sign says: the god's name (as written; the caller trims and formats it), and the gender
	 * the player chose by writing "God" or "Goddess" (the language's words, any case) on another line, or null when
	 * they didn't (the god then chooses). A sign with only one written line is the god's name, as altars always were,
	 * even if that name is a gender word. Problem is null when the sign is fine.
	 */
	public record NewAltar(String godName, GodGender gender, Problem problem)
	{
		static NewAltar problem(Problem problem)
		{
			return new NewAltar(null, null, problem);
		}
	}

	/**
	 * Reads a new praying altar sign: one line with the god's name, and maybe one more with a gender word; nothing
	 * else.
	 *
	 * @param maleWords   the words for a male god ("God")
	 * @param femaleWords the words for a female god ("Goddess")
	 */
	public static NewAltar newPrayingAltar(String[] lines, Collection<String> maleWords, Collection<String> femaleWords)
	{
		List<String> written = new ArrayList<>();
		for (String text : lines)
		{
			if (text != null && !ChatColor.stripColor(text).isBlank())
			{
				written.add(text);
			}
		}

		if (written.isEmpty())
		{
			return NewAltar.problem(Problem.INVALID_GOD_NAME);
		}
		if (written.size() > 2)
		{
			return NewAltar.problem(Problem.INVALID_SIGN);
		}

		String godName = written.get(0);
		GodGender gender = null;
		if (written.size() == 2)
		{
			GodGender first = genderWord(written.get(0), maleWords, femaleWords);
			GodGender second = genderWord(written.get(1), maleWords, femaleWords);
			if ((first == null) == (second == null))
			{
				// Two names, or two gender words
				return NewAltar.problem(Problem.INVALID_SIGN);
			}
			godName = first == null ? written.get(0) : written.get(1);
			gender = first == null ? second : first;
		}

		if (godName.trim().length() <= 1)
		{
			return NewAltar.problem(Problem.INVALID_GOD_NAME);
		}
		return new NewAltar(godName, gender, null);
	}

	/** The gender a line names (any case, colors ignored), or null if it is no gender word */
	public static GodGender genderWord(String line, Collection<String> maleWords, Collection<String> femaleWords)
	{
		String word = plain(line);
		if (femaleWords.stream().anyMatch(female -> plain(female).equalsIgnoreCase(word)))
		{
			return GodGender.Female;
		}
		if (maleWords.stream().anyMatch(male -> plain(male).equalsIgnoreCase(word)))
		{
			return GodGender.Male;
		}
		return null;
	}
}
