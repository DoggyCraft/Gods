package com.dogonfire.gods;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.dogonfire.gods.commands.GodsCommandExecuter;
import com.dogonfire.gods.commands.GodsTabCompleter;
import com.dogonfire.gods.config.ConfigDefaults;
import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.config.SettingsLoader;
import com.dogonfire.gods.listeners.BlockListener;
import com.dogonfire.gods.listeners.ChatListener;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.HolyBookManager;
import com.dogonfire.gods.managers.HolyLandManager;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.managers.MarriageManager;
import com.dogonfire.gods.managers.PermissionsManager;
import com.dogonfire.gods.managers.QuestManager;
import com.dogonfire.gods.managers.WhitelistManager;
import com.dogonfire.gods.migration.ConfigV1ToV2;
import com.dogonfire.gods.migration.VersionedYaml;
import com.dogonfire.gods.storage.Storage;
import com.dogonfire.gods.tasks.TaskInfo;
import com.dogonfire.gods.util.AtomicFiles;

public class Gods extends JavaPlugin
{
	/** The layout of config.yml this version writes; see {@link ConfigV1ToV2} */
	public static final int		CONFIG_VERSION	= 2;

	private static Gods			pluginInstance;

	/** Replaced as a whole by /g reload */
	private volatile Settings	settings;
	private Storage				storage;

	public static Gods instance()
	{
		return pluginInstance;
	}

	/**
	 * The settings read from config.yml at the last (re)load. Read them through here every time instead of keeping
	 * them, so /g reload takes effect.
	 */
	public Settings settings()
	{
		return this.settings;
	}

	public boolean isBlacklistedGod(String godName)
	{
		if (this.settings.gods().blacklist())
		{
			return WhitelistManager.instance().isBlacklistedGod(godName);
		}
		return false;
	}

	public boolean isEnabledInWorld(World world)
	{
		return this.settings.general().worlds().contains(world.getName());
	}

	public boolean isWhitelistedGod(String godName)
	{
		if (this.settings.gods().whitelist())
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
		Settings current = this.settings;
		if (current != null && current.general().debug())
		{
			this.getLogger().info("[Debug] " + message);
		}
	}

	@Override
	public void onDisable()
	{
		// Nothing was loaded if config.yml couldn't be read
		if (this.storage != null)
		{
			getServer().getScheduler().cancelTasks(this);
			// Writes every changed data file and waits for the writes
			this.storage.close();
			this.storage = null;
		}

		pluginInstance = null;
	}

	/** The data files; see {@link Storage} */
	public Storage storage()
	{
		return this.storage;
	}

	@Override
	public void onEnable()
	{
		pluginInstance = this;

		try
		{
			this.settings = loadSettings();
		}
		catch (VersionedYaml.LoadException e)
		{
			getLogger().severe(e.getMessage());
			getLogger().severe("Gods is disabling itself so it doesn't overwrite your files. Fix the problem and restart.");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		getCommand("gods").setExecutor(GodsCommandExecuter.instance());
		getCommand("g").setExecutor(GodsCommandExecuter.instance());
		getCommand("gods").setTabCompleter(GodsTabCompleter.instance());
		getCommand("g").setTabCompleter(GodsTabCompleter.instance());
		
		PluginManager pm = getServer().getPluginManager();

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
		
		this.storage = new Storage(getDataFolder().toPath(), getLogger());
		loadManagers();

		// Writes the data files that changed every few minutes (and on /g reload and disable)
		getServer().getScheduler().runTaskTimer(this, () -> this.storage.flush(), Storage.AUTOSAVE_TICKS, Storage.AUTOSAVE_TICKS);

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

	/**
	 * Saves every manager, re-reads config.yml and loads everything again from disk. Used by /gods reload. If config.yml
	 * can't be read, the old settings stay and false is returned.
	 */
	public boolean reloadSettings()
	{
		// Write what changed, so the files read again below have it
		this.storage.flush();
		this.storage.awaitWrites(10, TimeUnit.SECONDS);

		try
		{
			this.settings = loadSettings();
		}
		catch (VersionedYaml.LoadException e)
		{
			getLogger().severe("Reload failed: " + e.getMessage());
			return false;
		}

		loadManagers();
		return true;
	}

	/** Loads every enabled manager's data, in the order they depend on each other. */
	private void loadManagers()
	{
		if (this.settings.bibles().enabled())
		{
			HolyBookManager.instance().load();
		}
		if (this.settings.marriage().enabled())
		{
			MarriageManager.instance().load();
		}
		if (this.settings.holyLand().enabled())
		{
			HolyLandManager.instance().load();
		}

		PermissionsManager.instance().load();
		LanguageManager.instance().load();
		GodManager.instance().load();
		QuestManager.instance().load();
		BelieverManager.instance().load();
		WhitelistManager.instance().load();

		registerHolyLandListener();
	}

	/**
	 * Reads config.yml: writes the bundled one on first start, upgrades an old one, and adds settings that are missing
	 * because they were added in a newer version of Gods. Gods never writes config.yml otherwise.
	 */
	private Settings loadSettings() throws VersionedYaml.LoadException
	{
		File file = new File(getDataFolder(), "config.yml");
		if (!file.exists())
		{
			saveResource("config.yml", false);
		}
		VersionedYaml format = new VersionedYaml("config-version", CONFIG_VERSION, 1, List.of(new ConfigV1ToV2(this::getResource, getLogger())));
		YamlConfiguration config = format.load(file, getLogger());
		YamlConfiguration defaults = bundledConfig();

		if (ConfigDefaults.addMissing(config, defaults))
		{
			try
			{
				AtomicFiles.write(file.toPath(), config.saveToString());
				getLogger().info("Added new settings to config.yml.");
			}
			catch (IOException e)
			{
				getLogger().warning("Could not add new settings to config.yml: " + e.getMessage());
			}
		}

		config.setDefaults(defaults);
		List<String> serverWorlds = getServer().getWorlds().stream().map(World::getName).toList();
		return new SettingsLoader(config, getLogger(), serverWorlds).load();
	}

	private YamlConfiguration bundledConfig() throws VersionedYaml.LoadException
	{
		try (InputStream in = getResource("config.yml"))
		{
			if (in == null)
			{
				throw new VersionedYaml.LoadException("config.yml is missing from the plugin jar", null);
			}
			return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
		}
		catch (IOException e)
		{
			throw new VersionedYaml.LoadException("Could not read the bundled config.yml", e);
		}
	}

	/**
	 * Listens for holy land events only while holy land is enabled, so /g reload can switch it on or off.
	 */
	private void registerHolyLandListener()
	{
		HandlerList.unregisterAll(HolyLandManager.instance());
		if (this.settings.holyLand().enabled())
		{
			getServer().getPluginManager().registerEvents(HolyLandManager.instance(), this);
		}
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