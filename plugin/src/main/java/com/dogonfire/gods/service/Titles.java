package com.dogonfire.gods.service;

import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import com.dogonfire.gods.TitleSendEvent;

/**
 * Titles on a player's screen (the holy land or neutral land they walk into). Each one fires a {@link TitleSendEvent}
 * first, so other plugins can change the text or cancel it. Main thread only.
 */
public class Titles
{
	private final PluginManager pluginManager;

	public Titles(PluginManager pluginManager)
	{
		this.pluginManager = pluginManager;
	}

	/**
	 * Shows a title to a player. Times are in ticks. The texts are legacy text as {@code Messages.legacy} renders it, put
	 * on the screen as they are: they hold what players wrote (god names, descriptions), so no '&amp;' code or
	 * placeholder in them is read.
	 */
	public void send(Player player, Integer fadeIn, Integer stay, Integer fadeOut, String title, String subtitle)
	{
		if (player == null)
		{
			return;
		}

		TitleSendEvent titleSendEvent = new TitleSendEvent(player, title, subtitle);
		this.pluginManager.callEvent(titleSendEvent);

		if (titleSendEvent.isCancelled())
		{
			return;
		}

		player.sendTitle(nullToEmpty(titleSendEvent.getTitle()), nullToEmpty(titleSendEvent.getSubtitle()), fadeIn, stay, fadeOut);
	}

	private static String nullToEmpty(String text)
	{
		return text != null ? text : "";
	}
}
