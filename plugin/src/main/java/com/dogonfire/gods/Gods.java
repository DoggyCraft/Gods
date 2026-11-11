package com.dogonfire.gods;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
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
import com.dogonfire.gods.integration.GodsMetrics;
import com.dogonfire.gods.integration.GodsPlaceholderExpansion;
import com.dogonfire.gods.listener.ActionCooldown;
import com.dogonfire.gods.listener.AltarListener;
import com.dogonfire.gods.listener.BelieverListener;
import com.dogonfire.gods.listener.ChatListener;
import com.dogonfire.gods.listener.GodListener;
import com.dogonfire.gods.listener.HolyLandListener;
import com.dogonfire.gods.listener.MarriageListener;
import com.dogonfire.gods.listener.QuestAndHolyItemListener;
import com.dogonfire.gods.migration.ConfigV1ToV2;
import com.dogonfire.gods.migration.VersionedYaml;
import com.dogonfire.gods.service.ChatService;
import com.dogonfire.gods.storage.Storage;
import com.dogonfire.gods.task.GodUpdateTask;
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
	private GodsMetrics						metrics;

	// Repeating tasks started by onEnable, cancelled by onDisable
	private final List<BukkitTask>			tasks				= new ArrayList<BukkitTask>();
	// Started by startFeatures, undone by stopFeatures
	private final List<Listener>			featureListeners	= new ArrayList<Listener>();
	// Players the server has seen, by lower case name; built when an old data file needs it, dropped after loading
	private Map<String, UUID>				knownPlayerIds;

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

		this.storage = new Storage(getDataFolder().toPath(), getLogger(), this::knownPlayerId);
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
		// Both handle meals; they share the cooldown
		ActionCooldown mealCooldown = new ActionCooldown();
		pm.registerEvents(new GodListener(this.ctx, mealCooldown), this);
		pm.registerEvents(new QuestAndHolyItemListener(this.ctx, mealCooldown), this);
		pm.registerEvents(new BelieverListener(this.ctx), this);
		pm.registerEvents(new AltarListener(this.ctx), this);
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

		this.metrics = new GodsMetrics(this.ctx);

		// Moods, priests, quests, holy land attacks and the like
		this.tasks.add(getServer().getScheduler().runTaskTimer(this, new GodUpdateTask(this.ctx), GodUpdateTask.DELAY_TICKS, GodUpdateTask.PERIOD_TICKS));
		// Writes the data files that changed (and on /g reload and disable)
		this.tasks.add(getServer().getScheduler().runTaskTimer(this, () -> this.storage.flush(), Storage.AUTOSAVE_TICKS, Storage.AUTOSAVE_TICKS));
		// Priest status and the like change in places that don't notify the chat cache, so refresh it now and then
		this.tasks.add(getServer().getScheduler().runTaskTimer(this, () -> this.ctx.chatService().refreshAll(), 1L, ChatService.REFRESH_TICKS));
		// The numbers the bStats charts show (bStats reads them on its own thread)
		this.tasks.add(getServer().getScheduler().runTaskTimer(this, () -> this.metrics.refresh(), GodsMetrics.REFRESH_TICKS, GodsMetrics.REFRESH_TICKS));

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
		if (this.metrics != null)
		{
			this.metrics.shutdown();
			this.metrics = null;
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
	 * /g reload: reads config.yml again, then the language files and the data files, and restarts the features so
	 * switching them on or off in config.yml takes effect. A data file that was edited on disk is read again (the edit
	 * wins over changes not yet saved); the others keep what is in memory and are saved. If config.yml can't be read,
	 * or the data files couldn't be saved in time, the old settings and data stay and false is returned.
	 */
	public boolean reload()
	{
		Settings newSettings;
		try
		{
			newSettings = loadSettings();
		}
		catch (VersionedYaml.LoadException e)
		{
			getLogger().severe("Reload failed: " + e.getMessage());
			return false;
		}

		if (!this.storage.prepareReload(10, TimeUnit.SECONDS))
		{
			getLogger().severe("Reload failed: the data files could not be saved in time, so they are not read again.");
			return false;
		}

		this.settings = newSettings;
		stopFeatures();
		loadData();
		startFeatures();
		this.storage.finishReload();
		this.metrics.refresh();
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
			this.ctx.marriageService().load();
		}
		if (this.settings.holyLand().enabled())
		{
			this.ctx.holyLandService().load();
		}

		this.ctx.languageManager().load();
		this.ctx.godService().load();
		this.ctx.questService().load();
		this.ctx.believerService().load();
		this.ctx.godListService().load();

		this.knownPlayerIds = null;
	}

	/** The UUID of a player the server has seen, by name (any case), or null; for upgrading old data files */
	private UUID knownPlayerId(String name)
	{
		if (this.knownPlayerIds == null)
		{
			this.knownPlayerIds = new HashMap<String, UUID>();
			for (OfflinePlayer player : getServer().getOfflinePlayers())
			{
				if (player.getName() != null)
				{
					this.knownPlayerIds.put(player.getName().toLowerCase(), player.getUniqueId());
				}
			}
		}
		return this.knownPlayerIds.get(name.toLowerCase());
	}

	/** The features that can be switched on and off in config.yml */
	private void startFeatures()
	{
		if (this.settings.holyLand().enabled())
		{
			registerFeature(new HolyLandListener(this.ctx));
		}
		if (this.settings.marriage().enabled())
		{
			registerFeature(new MarriageListener(this.ctx));
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
}
