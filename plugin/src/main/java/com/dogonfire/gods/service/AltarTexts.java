package com.dogonfire.gods.service;

import java.util.ArrayList;
import java.util.List;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import com.dogonfire.gods.message.Messages;

/**
 * The text floating above an altar (no server needed, tested): {@code altar.floating.<style>.altar} in messages.yml,
 * or {@code .main} for a god's main altar, falling back to the {@code default} style, rendered to legacy text with
 * one line per entry. Blank lines are left out, so a title that is empty (gods.titles off) takes no room.
 */
public final class AltarTexts
{
	/** At most this many floating texts at once, whatever the number of altars in loaded chunks */
	public static final int		MAX_DISPLAYS	= 512;

	/** How far above the altar block the text floats: over the light on top of it */
	public static final double	HEIGHT			= 2.1;

	private AltarTexts()
	{
	}

	/** The key of the text: the style's own, else the default style's */
	public static String key(Messages messages, String style, boolean main)
	{
		String kind = main ? "main" : "altar";
		String key = "altar.floating." + style + "." + kind;
		return messages.has(key) ? key : "altar.floating.default." + kind;
	}

	/**
	 * The text in legacy format, lines joined by '\n'
	 *
	 * @param tags {@code <god>}, {@code <title>}, {@code <gender>}, {@code <mood>}, {@code <believers>}
	 */
	public static String render(Messages messages, String style, boolean main, TagResolver... tags)
	{
		List<String> lines = new ArrayList<>();
		for (String line : messages.legacyLines(key(messages, style, main), tags))
		{
			if (!AltarSigns.plain(line).isBlank())
			{
				lines.add(line);
			}
		}
		return String.join("\n", lines);
	}
}
