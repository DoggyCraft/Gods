package com.dogonfire.gods.service;

import org.bukkit.ChatColor;
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
	 * Shows a title to a player. Times are in ticks; '&amp;' color codes and %player% are replaced.
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

		player.sendTitle(format(player, titleSendEvent.getTitle()), format(player, titleSendEvent.getSubtitle()), fadeIn, stay, fadeOut);
	}

	private static String format(Player player, String text)
	{
		if (text == null)
		{
			return "";
		}

		return ChatColor.translateAlternateColorCodes('&', text).replace("%player%", player.getDisplayName());
	}
}
