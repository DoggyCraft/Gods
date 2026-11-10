package com.dogonfire.gods;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.dogonfire.gods.commands.GodsCommandExecuter;
import com.dogonfire.gods.commands.GodsTabCompleter;
import com.dogonfire.gods.config.ConfigDefaults;
import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.config.SettingsLoader;
import com.dogonfire.gods.integration.GodsPlaceholderExpansion;
import com.dogonfire.gods.listeners.BlockListener;
import com.dogonfire.gods.listeners.ChatListener;
import com.dogonfire.gods.managers.ChatManager;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.migration.ConfigV1ToV2;
import com.dogonfire.gods.migration.VersionedYaml;
import com.dogonfire.gods.storage.Storage;
import com.dogonfire.gods.tasks.TaskInfo;
import com.dogonfire.gods.util.AtomicFiles;

/**
 * The plugin. It only wires things: onEnable reads the settings, builds the {@link GodsContext} (the object graph),
 * loads the data, registers the command, the always-on listeners and the repeating tasks, then starts the features
 * that can be switched on and off in config.yml ({@link #startFeatures()}). /g reload ({@link #reload()}) re-reads
 * config, language and data files and restarts those features; onDisable undoes everything and saves.
 */
public class Gods extends JavaPlugin
{
	/** The layout of config.yml this version writes; see {@link ConfigV1ToV2} */
	public static final int					CONFIG_VERSION		= 2;

	/** Replaced as a whole by /g reload */
	private volatile Settings				settings;
	private Storage							storage;
	private GodsContext						ctx;
	private GodsPlaceholderExpansion		placeholderExpansion;

	// Repeating tasks started by onEnable, cancelled by onDisable
	private final List<BukkitTask>			tasks				= new ArrayList<BukkitTask>();
	// Started by startFeatures, undone by stopFeatures
	private final List<Listener>			featureListeners	= new ArrayList<Listener>();

	/**
	 * The settings read from config.yml at the last (re)load. Read them through here (or {@link GodsContext#settings()})
	 * every time instead of keeping them, so /g reload takes effect.
	 */
	public Settings settings()
	{
		return this.settings;
	}

	@Override
	public void onEnable()
	{
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

		this.storage = new Storage(getDataFolder().toPath(), getLogger());
		this.ctx = new GodsContext(this, this.storage);

		loadData();

		GodsCommandExecuter executer = new GodsCommandExecuter(this.ctx);
		GodsTabCompleter tabCompleter = new GodsTabCompleter(this.ctx, executer);
		for (String name : List.of("gods", "g"))
		{
			PluginCommand command = getCommand(name);
			command.setExecutor(executer);
			command.setTabCompleter(tabCompleter);
		}

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(new BlockListener(this.ctx), this);
		pm.registerEvents(new ChatListener(this.ctx), this);

		if (pm.getPlugin("PlaceholderAPI") != null)
		{
			log("PlaceholderAPI found.");
			this.placeholderExpansion = new GodsPlaceholderExpansion(this.ctx);
			this.placeholderExpansion.register();
		}
		else
		{
			logDebug("PlaceholderAPI not found.");
		}

		// Moods, priests, quests, holy land attacks and the like
		this.tasks.add(getServer().getScheduler().runTaskTimer(this, () -> this.ctx.godManager().update(), 20L, 200L));
		// Writes the data files that changed (and on /g reload and disable)
		this.tasks.add(getServer().getScheduler().runTaskTimer(this, () -> this.storage.flush(), Storage.AUTOSAVE_TICKS, Storage.AUTOSAVE_TICKS));
		// Priest status and the like change in places that don't notify the chat cache, so refresh it now and then
		this.tasks.add(getServer().getScheduler().runTaskTimer(this, () -> this.ctx.chatManager().refreshAll(), 1L, ChatManager.REFRESH_TICKS));

		startFeatures();
	}

	@Override
	public void onDisable()
	{
		if (this.ctx == null)
		{
			// config.yml couldn't be read: nothing was loaded, so nothing is saved
			return;
		}

		if (this.placeholderExpansion != null)
		{
			this.placeholderExpansion.unregister();
			this.placeholderExpansion = null;
		}

		stopFeatures();
		for (BukkitTask task : this.tasks)
		{
			task.cancel();
		}
		this.tasks.clear();
		// The delayed effect tasks (god speech, powers, fireworks, ...) too
		getServer().getScheduler().cancelTasks(this);
		HandlerList.unregisterAll(this);

		// Writes every changed data file and waits for the writes
		this.storage.close();

		this.ctx = null;
		this.storage = null;
	}

	/**
	 * /g reload: saves the data, reads config.yml again, then the language files and the data files, and restarts the
	 * features so switching them on or off in config.yml takes effect. If config.yml can't be read, the old settings
	 * and data stay and false is returned.
	 */
	public boolean reload()
	{
		// Write what changed, so the files read again below have it
		this.storage.flush();
		if (!this.storage.awaitWrites(10, TimeUnit.SECONDS))
		{
			getLogger().severe("Reload failed: the data files could not be saved in time, so they are not read again.");
			return false;
		}

		try
		{
			this.settings = loadSettings();
		}
		catch (VersionedYaml.LoadException e)
		{
			getLogger().severe("Reload failed: " + e.getMessage());
			return false;
		}

		stopFeatures();
		loadData();
		startFeatures();
		return true;
	}

	/**
	 * Loads the language files and the data of every enabled feature, in the order the managers depend on each other.
	 * Data of a feature that is switched off is not read (and so not written).
	 */
	private void loadData()
	{
		if (this.settings.bibles().enabled())
		{
			this.ctx.holyBookManager().load();
		}
		if (this.settings.marriage().enabled())
		{
			this.ctx.marriageManager().load();
		}
		if (this.settings.holyLand().enabled())
		{
			this.ctx.holyLandManager().load();
		}

		this.ctx.languageManager().load();
		this.ctx.godManager().load();
		this.ctx.questManager().load();
		this.ctx.believerService().load();
		this.ctx.whitelistManager().load();
	}

	/** The features that can be switched on and off in config.yml */
	private void startFeatures()
	{
		if (this.settings.holyLand().enabled())
		{
			registerFeature(this.ctx.holyLandManager());
		}
	}

	private void stopFeatures()
	{
		for (Listener listener : this.featureListeners)
		{
			HandlerList.unregisterAll(listener);
		}
		this.featureListeners.clear();
	}

	private void registerFeature(Listener listener)
	{
		getServer().getPluginManager().registerEvents(listener, this);
		this.featureListeners.add(listener);
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

	public boolean isBlacklistedGod(String godName)
	{
		if (this.settings.gods().blacklist())
		{
			return this.ctx.whitelistManager().isBlacklistedGod(godName);
		}
		return false;
	}

	public boolean isWhitelistedGod(String godName)
	{
		if (this.settings.gods().whitelist())
		{
			return this.ctx.whitelistManager().isWhitelistedGod(godName);
		}
		return true;
	}

	public boolean isEnabledInWorld(World world)
	{
		return this.settings.general().worlds().contains(world.getName());
	}

	public void log(String message)
	{
		getLogger().info(message);
	}

	public void logDebug(String message)
	{
		Settings current = this.settings;
		if (current != null && current.general().debug())
		{
			getLogger().info("[Debug] " + message);
		}
	}

	public void sendInfo(UUID playerId, LanguageManager.LANGUAGESTRING message, ChatColor color, int amount, String name, int delay)
	{
		if (isOnline(playerId))
		{
			getServer().getScheduler().runTaskLater(this, new TaskInfo(this.ctx, color, playerId, message, amount, name), delay);
		}
	}

	public void sendInfo(UUID playerId, LanguageManager.LANGUAGESTRING message, ChatColor color, String name, int amount1, int amount2, int delay)
	{
		if (isOnline(playerId))
		{
			getServer().getScheduler().runTaskLater(this, new TaskInfo(this.ctx, color, playerId, message, name, amount1, amount2), delay);
		}
	}

	public void sendInfo(UUID playerId, LanguageManager.LANGUAGESTRING message, ChatColor color, String name1, String name2, int delay)
	{
		if (isOnline(playerId))
		{
			getServer().getScheduler().runTaskLater(this, new TaskInfo(this.ctx, color, playerId, message, name1, name2), delay);
		}
	}

	private boolean isOnline(UUID playerId)
	{
		Player player = getServer().getPlayer(playerId);
		if (player == null)
		{
			logDebug("sendInfo can not find online player with id " + playerId);
			return false;
		}
		return true;
	}
}
