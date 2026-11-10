package com.dogonfire.gods.managers;

import org.bukkit.entity.Player;

/**
 * Permission checks go through Bukkit's own permission system. Every
 * permission plugin (LuckPerms and the like) hooks into it, and ops get the nodes plugin.yml gives them by default.
 */
public class PermissionsManager
{
	public PermissionsManager()
	{
	}

	public boolean hasPermission(Player player, String node)
	{
		return player != null && player.hasPermission(node);
	}
}
