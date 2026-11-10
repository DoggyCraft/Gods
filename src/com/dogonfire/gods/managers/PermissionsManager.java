package com.dogonfire.gods.managers;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import com.dogonfire.gods.Gods;

import net.milkbowl.vault.permission.Permission;

public class PermissionsManager
{
	private static PermissionsManager instance = null;

	private String				pluginName			= "null";
	private Gods				plugin;
	private Permission 			vaultPermission		= null;

	public static PermissionsManager instance()
	{
		if (instance == null)
			instance = new PermissionsManager(Gods.instance());
		return instance;
	}

	public PermissionsManager(Gods g)
	{
		this.plugin = g;
			
		if (g.vaultEnabled)
		{
			RegisteredServiceProvider<Permission> permissionProvider = plugin.getServer().getServicesManager().getRegistration(Permission.class);

			if (permissionProvider == null)
			{
				Gods.instance().log(ChatColor.RED + "Could not detect Vault plugin.");
				return;
			}

			vaultPermission = permissionProvider.getProvider();
		}
	}

	public void load()
	{
		// Nothing to see here
	}

	public String getPermissionPluginName()
	{
		return pluginName;
	}

	public boolean hasPermission(Player player, String node)
	{
		if (Gods.instance().vaultEnabled)
		{
			return vaultPermission.has(player, node);
		}
		return false;
	}

	public String getGroup(String playerName)
	{
		if (Gods.instance().vaultEnabled)
		{
			return getGroup(plugin.getServer().getPlayer(playerName).getUniqueId());
		}
		return "";
	}

	public String getGroup(UUID playerId)
	{
		if (Gods.instance().vaultEnabled)
		{
			return vaultPermission.getPrimaryGroup(null, plugin.getServer().getPlayer(playerId));
		}
		return "";
	}

	public void setGroup(String playerName, String groupName)
	{
		if (Gods.instance().vaultEnabled)
		{
			Player player = plugin.getServer().getPlayer(playerName);
			setGroup(player.getUniqueId(), groupName);
		}
	}

	public void setGroup(UUID playerId, String groupName)
	{
		if (Gods.instance().vaultEnabled)
		{
			Player player = plugin.getServer().getPlayer(playerId);
			vaultPermission.playerAddGroup(null, player, groupName);
		}
	}
}