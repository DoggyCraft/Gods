package com.dogonfire.gods.managers;

import org.bukkit.entity.Player;

/**
 * Permission checks go through Bukkit's own permission system. Every
 * permission plugin (LuckPerms and the like) hooks into it, and ops get the nodes plugin.yml gives them by default.
 */
public class PermissionsManager
{
	private static PermissionsManager instance = null;

	public static PermissionsManager instance()
	{
		if (instance == null)
			instance = new PermissionsManager();
		return instance;
	}

	private PermissionsManager()
	{
	}

	public void load()
	{
		// Nothing to load: permissions come from Bukkit
	}

	public boolean hasPermission(Player player, String node)
	{
		return player != null && player.hasPermission(node);
	}
}
