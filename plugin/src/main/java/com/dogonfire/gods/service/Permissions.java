package com.dogonfire.gods.service;

import org.bukkit.entity.Player;

/**
 * Permission checks go through Bukkit's own permission system. Every permission plugin (LuckPerms and the like) hooks
 * into it, and ops get the nodes plugin.yml gives them by default. There is no Vault dependency.
 */
public class Permissions
{
	/** Whether the player (null: nobody) has the permission node */
	public boolean hasPermission(Player player, String node)
	{
		return player != null && player.hasPermission(node);
	}
}
