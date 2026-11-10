package com.dogonfire.gods;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.dogonfire.gods.commands.GodsCommandExecuter;
import com.dogonfire.gods.commands.GodsTabCompleter;
import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.listeners.BlockListener;
import com.dogonfire.gods.listeners.ChatListener;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.HolyBookManager;
import com.dogonfire.gods.managers.HolyLandManager;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.managers.PermissionsManager;
import com.dogonfire.gods.managers.QuestManager;
import com.dogonfire.gods.managers.WhitelistManager;
import com.dogonfire.gods.tasks.TaskInfo;

public class Gods extends JavaPlugin
{
	private static Gods pluginInstance;

	public boolean vaultEnabled	= false;

	public static Gods instance()
	{
		return pluginInstance;
	}

	public boolean isBlacklistedGod(String godName)
	{
		if (GodsConfiguration.instance().isUseBlacklist())
		{
			return WhitelistManager.instance().isBlacklistedGod(godName);
		}
		return false;
	}

	public boolean isEnabledInWorld(World world)
	{
		return GodsConfiguration.instance().getWorlds().contains(world.getName());
	}

	public boolean isWhitelistedGod(String godName)
	{
		if (GodsConfiguration.instance().isUseWhitelist())
		{
			return WhitelistManager.instance().isWhitelistedGod(godName);
		}
		return true;
	}

	public void log(String message)
	{
		this.getLogger().info(message);
	}

	public void logDebug(String message)
	{
		if (GodsConfiguration.instance().isDebug())
		{
			this.getLogger().info("[Debug] " + message);
		}
	}

	@Override
	public void onDisable()
	{
		reloadSettings();

		GodManager.instance().save();
		QuestManager.instance().save();
		BelieverManager.instance().save();
		
		if ((GodsConfiguration.instance().isUseBlacklist()) || (GodsConfiguration.instance().isUseWhitelist()))
			WhitelistManager.instance().save();
		if (GodsConfiguration.instance().isHolyLandEnabled())
			HolyLandManager.instance().save();
		if (GodsConfiguration.instance().isBiblesEnabled())
			HolyBookManager.instance().save();
		
		pluginInstance = null;
	}

	@Override
	public void onEnable()
	{
		pluginInstance = this;
		
		getCommand("gods").setExecutor(GodsCommandExecuter.instance());
		getCommand("g").setExecutor(GodsCommandExecuter.instance());
		getCommand("gods").setTabCompleter(GodsTabCompleter.instance());
		getCommand("g").setTabCompleter(GodsTabCompleter.instance());
		
		PluginManager pm = getServer().getPluginManager();

		// Check for Vault
		if (pm.getPlugin("Vault") != null)
		{
			this.vaultEnabled = true;

			log("Vault detected.");
		}
		else
		{
			log("Vault not found.");
		}
		// Check for PlaceholderAPI
		if (pm.getPlugin("PlaceholderAPI") != null)
		{
			log("PlaceholderAPI found.");
			new GodsPlaceholderExpansion(Gods.instance()).register();
		}
		else
		{
			logDebug("PlaceholderAPI not found.");
		}
		
		GodsConfiguration.instance().loadSettings();
		GodsConfiguration.instance().saveSettings();
		PermissionsManager.instance().load();
		LanguageManager.instance().load();
		GodManager.instance().load();
		QuestManager.instance().load();
		BelieverManager.instance().load();
		WhitelistManager.instance().load();
		
		if(GodsConfiguration.instance().isHolyLandEnabled())
		{
			HolyLandManager.instance().load();
			getServer().getPluginManager().registerEvents(HolyLandManager.instance(), this);
		}		
		
		getServer().getPluginManager().registerEvents(new BlockListener(), this);
		getServer().getPluginManager().registerEvents(new ChatListener(), this);

		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				GodManager.instance().update();
			}
		}.runTaskTimer(this, 20L, 200L);
	}

	public void reloadSettings()
	{
		reloadConfig();

		GodsConfiguration.instance().loadSettings();

		WhitelistManager.instance().load();
	}

	public void sendInfo(UUID playerId, LanguageManager.LANGUAGESTRING message, ChatColor color, int amount, String name, int delay)
	{
		Player player = getServer().getPlayer(playerId);

		if (player == null)
		{
			logDebug("sendInfo can not find online player with id " + playerId);
			return;
		}

		getServer().getScheduler().runTaskLater(this, new TaskInfo(color, playerId, message, amount, name), delay);
	}

	public void sendInfo(UUID playerId, LanguageManager.LANGUAGESTRING message, ChatColor color, String name, int amount1, int amount2, int delay)
	{
		Player player = getServer().getPlayer(playerId);
		if (player == null)
		{
			logDebug("sendInfo can not find online player with id " + playerId);
			return;
		}
		getServer().getScheduler().runTaskLater(this, new TaskInfo(color, playerId, message, name, amount1, amount2), delay);
	}

	public void sendInfo(UUID playerId, LanguageManager.LANGUAGESTRING message, ChatColor color, String name1, String name2, int delay)
	{
		Player player = getServer().getPlayer(playerId);
		if (player == null)
		{
			logDebug("sendInfo can not find online player with id " + playerId);
			return;
		}

		getServer().getScheduler().runTaskLater(this, new TaskInfo(color, playerId, message, name1, name2), delay);
	}
}