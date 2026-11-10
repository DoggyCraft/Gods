package com.dogonfire.gods.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.dogonfire.gods.TitleSendEvent;

public class TitleManager
{
	private TitleManager()
	{
	}

	/**
	 * Shows a title to a player. Fires a {@link TitleSendEvent} first, so other
	 * plugins can change the text or cancel it. Times are in ticks.
	 */
	public static void sendTitle(Player player, Integer fadeIn, Integer stay, Integer fadeOut, String title, String subtitle)
	{
		if (player == null)
		{
			return;
		}

		TitleSendEvent titleSendEvent = new TitleSendEvent(player, title, subtitle);
		Bukkit.getPluginManager().callEvent(titleSendEvent);

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
