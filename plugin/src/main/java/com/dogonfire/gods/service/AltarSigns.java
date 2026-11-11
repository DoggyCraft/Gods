package com.dogonfire.gods.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;

import com.dogonfire.gods.model.GodGender;

/**
 * Reading altar signs, without the server so it can be tested. Altars are written on the front of a wall sign on an
 * altar block. A new praying altar is a sign with the god's name on it, and maybe "God", "Goddess" or "Deity" on
 * another line; a new curse or blessing altar says "Curses" or "Blessings" on its first line and a player's name on
 * its third. Gods then tags the sign (praying, curses or blessings, the god, the player) and writes it in the god type's style.
 * Altars from before tags say "Altar / of / &lt;god&gt;", "Curses / On / &lt;player&gt;" or
 * "Blessings / On / &lt;player&gt;"; they are recognized by that text and tagged when first used.
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

	/** What an altar sign is for; the key is its value in the sign's tag and its part of the sign text keys */
	public enum Kind
	{
		PRAYING("praying"), CURSES("curses"), BLESSINGS("blessings");

		private final String key;

		Kind(String key)
		{
			this.key = key;
		}

		public String key()
		{
			return this.key;
		}

		/** The kind with this key, or null */
		public static Kind byKey(String key)
		{
			for (Kind kind : values())
			{
				if (kind.key.equals(key))
				{
					return kind;
				}
			}
			return null;
		}
	}

	/**
	 * A finished altar sign.
	 *
	 * @param kind       praying, curses or blessings
	 * @param god        a praying altar's god (as written on an untagged sign); for a curse or blessing sign the god of
	 *                   whoever made it, whose style the sign has (may be null)
	 * @param playerName who a curse or blessing sign is about, else null
	 * @param playerId   their UUID, null if unknown (an untagged sign names the player only)
	 * @param tagged     true if read from the sign's tags; false for an altar from before tags, read from its text
	 */
	public record AltarSign(Kind kind, String god, String playerName, UUID playerId, boolean tagged)
	{
		/** The same altar, as it is once tagged */
		public AltarSign asTagged(String god, UUID playerId)
		{
			return new AltarSign(this.kind, god, this.playerName, playerId, true);
		}
	}

	/**
	 * Recognizes an altar sign: by its tags first (a sign Gods made or already tagged), else by the text altars had
	 * before tags ("Altar / of / &lt;god&gt;", "Curses / On / &lt;player&gt;", "Blessings / On / &lt;player&gt;").
	 * Null if it is no altar sign.
	 *
	 * @param tagKind the sign's kind tag, null if it has none
	 * @param lines   the front text of the sign
	 */
	public static AltarSign recognize(String tagKind, String tagGod, String tagPlayerName, String tagPlayerId, String[] lines)
	{
		Kind kind = tagKind != null ? Kind.byKey(tagKind) : null;
		if (kind != null)
		{
			return new AltarSign(kind, tagGod, tagPlayerName, uuid(tagPlayerId), true);
		}
		if (lines == null || lines.length < 3)
		{
			return null;
		}
		if (plain(lines[0]).equals(ALTAR) && isFormatted(lines))
		{
			return new AltarSign(Kind.PRAYING, plain(lines[2]), null, null, false);
		}
		String cursed = namedPlayer(lines, CURSES);
		if (cursed != null)
		{
			return new AltarSign(Kind.CURSES, null, cursed, null, false);
		}
		String blessed = namedPlayer(lines, BLESSINGS);
		if (blessed != null)
		{
			return new AltarSign(Kind.BLESSINGS, null, blessed, null, false);
		}
		return null;
	}

	private static UUID uuid(String text)
	{
		try
		{
			return text != null ? UUID.fromString(text) : null;
		}
		catch (IllegalArgumentException ex)
		{
			return null;
		}
	}

	/** The four lines of a sign: the given ones, cut or padded with empty lines */
	public static String[] fourLines(List<String> lines)
	{
		String[] four = new String[4];
		for (int line = 0; line < 4; line++)
		{
			four[line] = line < lines.size() && lines.get(line) != null ? lines.get(line) : "";
		}
		return four;
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
	 * the player chose by writing "God", "Goddess" or "Deity" (the language's words, any case) on another line, or null
	 * when they didn't (the god then chooses). A sign with only one written line is the god's name, as altars always
	 * were, even if that name is a gender word. Problem is null when the sign is fine.
	 */
	public record NewAltar(String godName, GodGender gender, Problem problem)
	{
		static NewAltar problem(Problem problem)
		{
			return new NewAltar(null, null, problem);
		}
	}

	/**
	 * The words that name a god's gender, on an altar sign and in /g gender: the language's
	 * ({@code altar.gender-words.*} in messages.yml) and the English ones
	 *
	 * @param male      the words for a male god ("God")
	 * @param female    the words for a female god ("Goddess")
	 * @param nonBinary the words for a non-binary god ("Deity")
	 */
	public record GenderWords(Collection<String> male, Collection<String> female, Collection<String> nonBinary)
	{
		/** Only the English words */
		public static final GenderWords ENGLISH = new GenderWords(List.of("God"), List.of("Goddess"), List.of("Deity"));

		/** The gender a line names (any case, colors ignored), or null if it is no gender word */
		public GodGender gender(String line)
		{
			String word = plain(line);
			if (matches(this.female, word))
			{
				return GodGender.Female;
			}
			if (matches(this.male, word))
			{
				return GodGender.Male;
			}
			if (matches(this.nonBinary, word))
			{
				return GodGender.NonBinary;
			}
			return null;
		}

		/** Every word, for tab completion */
		public List<String> all()
		{
			List<String> all = new ArrayList<>();
			all.addAll(this.male);
			all.addAll(this.female);
			all.addAll(this.nonBinary);
			return all;
		}

		private static boolean matches(Collection<String> words, String word)
		{
			return !word.isEmpty() && words.stream().anyMatch(candidate -> plain(candidate).equalsIgnoreCase(word));
		}
	}

	/**
	 * Reads a new praying altar sign: one line with the god's name, and maybe one more with a gender word; nothing
	 * else.
	 */
	public static NewAltar newPrayingAltar(String[] lines, GenderWords words)
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
			GodGender first = words.gender(written.get(0));
			GodGender second = words.gender(written.get(1));
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
}
