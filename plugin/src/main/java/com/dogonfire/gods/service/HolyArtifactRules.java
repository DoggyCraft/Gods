package com.dogonfire.gods.service;

import org.bukkit.ChatColor;

/**
 * The texts of holy artifacts, without a server so they can be tested: the uses line of the lore, the rank an
 * artifact earns by being used, and how artifacts from before the tags are recognized by their lore. Kept exactly as
 * they were.
 */
public final class HolyArtifactRules
{
	private HolyArtifactRules()
	{
	}

	/** The second lore line */
	public static String usesLine(int uses)
	{
		return ChatColor.WHITE + "Used " + uses + (uses == 1 ? " time" : " times");
	}

	/** The rank shown before the artifact's name; empty below 5 uses */
	public static String rankName(int uses)
	{
		if (uses < 5)
		{
			return "";
		}
		if (uses < 20)
		{
			return ChatColor.GREEN + "Shiny";
		}
		if (uses < 50)
		{
			return ChatColor.GREEN + "Honorable";
		}
		if (uses < 100)
		{
			return ChatColor.GREEN + "Holy";
		}
		if (uses < 150)
		{
			return ChatColor.GREEN + "Saintly";
		}
		if (uses < 200)
		{
			return ChatColor.BLUE + "Amazing";
		}
		if (uses < 300)
		{
			return ChatColor.BLUE + "Mega";
		}
		if (uses < 500)
		{
			return ChatColor.BLUE + "Awesome";
		}
		if (uses < 1000)
		{
			return ChatColor.DARK_PURPLE + "Epic";
		}
		return ChatColor.GOLD + "Legendary";
	}

	/** The first lore line (without colors) of an artifact made before artifacts were tagged */
	public static boolean isArtifactLine(String firstLine)
	{
		return firstLine.startsWith("A Holy artifact") || firstLine.startsWith("An Unholy artifact");
	}
}
