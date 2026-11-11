package com.dogonfire.gods;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.dogonfire.gods.api.GodsApi;
import com.dogonfire.gods.command.AcceptCommand;
import com.dogonfire.gods.command.AccessCommand;
import com.dogonfire.gods.command.AllyCommand;
import com.dogonfire.gods.command.AttackCommand;
import com.dogonfire.gods.command.BibleCommand;
import com.dogonfire.gods.command.ChatCommand;
import com.dogonfire.gods.command.CheckCommand;
import com.dogonfire.gods.command.CommandChecks;
import com.dogonfire.gods.command.CommandRouter;
import com.dogonfire.gods.command.DescriptionCommand;
import com.dogonfire.gods.command.DivorceCommand;
import com.dogonfire.gods.command.EditBibleCommand;
import com.dogonfire.gods.command.FollowersCommand;
import com.dogonfire.gods.command.GenderCommand;
import com.dogonfire.gods.command.GodSkinCommand;
import com.dogonfire.gods.command.HelpCommand;
import com.dogonfire.gods.command.HolyArtifactCommand;
import com.dogonfire.gods.command.HomeCommand;
import com.dogonfire.gods.command.HuntCommand;
import com.dogonfire.gods.command.InfoCommand;
import com.dogonfire.gods.command.InviteCommand;
import com.dogonfire.gods.command.KickCommand;
import com.dogonfire.gods.command.LeaveCommand;
import com.dogonfire.gods.command.ListCommand;
import com.dogonfire.gods.command.LoveCommand;
import com.dogonfire.gods.command.MarriagesCommand;
import com.dogonfire.gods.command.MarryCommand;
import com.dogonfire.gods.command.Overview;
import com.dogonfire.gods.command.PrayForCommand;
import com.dogonfire.gods.command.PvpCommand;
import com.dogonfire.gods.command.RankCommand;
import com.dogonfire.gods.command.RejectCommand;
import com.dogonfire.gods.command.ReloadCommand;
import com.dogonfire.gods.command.SetAltarCommand;
import com.dogonfire.gods.command.SetBibleCommand;
import com.dogonfire.gods.command.SetPriestCommand;
import com.dogonfire.gods.command.SetSafeCommand;
import com.dogonfire.gods.command.StartAttackCommand;
import com.dogonfire.gods.command.SubCommand;
import com.dogonfire.gods.command.WarCommand;
import com.dogonfire.gods.config.ConfigDefaults;
import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.config.SettingsLoader;
import com.dogonfire.gods.config.Themes;
import com.dogonfire.gods.config.ThemesLoader;
import com.dogonfire.gods.integration.GodsMetrics;
import com.dogonfire.gods.integration.GodsPlaceholderExpansion;
import com.dogonfire.gods.listener.ActionCooldown;
import com.dogonfire.gods.listener.AltarListener;
import com.dogonfire.gods.listener.AltarWorldListener;
import com.dogonfire.gods.listener.BelieverListener;
import com.dogonfire.gods.listener.ChatListener;
import com.dogonfire.gods.listener.GodListener;
import com.dogonfire.gods.listener.GodNpcListener;
import com.dogonfire.gods.listener.HolyArtifactListener;
import com.dogonfire.gods.listener.HolyBookListener;
import com.dogonfire.gods.listener.HolyLandListener;
import com.dogonfire.gods.listener.MarriageListener;
import com.dogonfire.gods.listener.QuestListener;
import com.dogonfire.gods.migration.ConfigV1ToV2;
import com.dogonfire.gods.migration.ConfigV2ToV3;
import com.dogonfire.gods.migration.ConfigV3ToV4;
import com.dogonfire.gods.migration.ConfigV4ToV5;
import com.dogonfire.gods.migration.LegacyLangFiles;
import com.dogonfire.gods.migration.VersionedYaml;
import com.dogonfire.gods.service.ChatService;
import com.dogonfire.gods.service.GodsApiService;
import com.dogonfire.gods.storage.Storage;
import com.dogonfire.gods.task.GodUpdateTask;
import com.dogonfire.gods.util.AtomicFiles;

/**
 * The plugin. It only wires things: onEnable reads the settings, builds the {@link GodsContext} (the object graph),
 * loads the data, registers the command, the always-on listeners and the repeating tasks, then starts the features
 * that can be switched on and off in config.yml ({@link #startFeatures()}). /g reload ({@link #reload()}) re-reads
 * config, message and data files and restarts those features; onDisable undoes everything and saves.
 */
public class Gods extends JavaPlugin
{
	/** The layout of config.yml this version writes; see {@link ConfigV1ToV2}, {@link ConfigV2ToV3}, {@link ConfigV3ToV4} and {@link ConfigV4ToV5} */
	public static final int					CONFIG_VERSION		= 5;

	/** Replaced as a whole by /g reload */
	private volatile Settings				settings;
	/** Replaced as a whole by /g reload, with the settings */
	private volatile Themes					themes;
	private Storage							storage;
	private GodsContext						ctx;
	private GodsPlaceholderExpansion		placeholderExpansion;
	private GodsMetrics						metrics;

	// Repeating tasks started by onEnable, cancelled by onDisable
	private final List<BukkitTask>			tasks				= new ArrayList<BukkitTask>();
	// Meals (GodListener, QuestListener) and reading holy books (HolyBookListener) share this cooldown
	private ActionCooldown					actionCooldown;
	// Started by startFeatures, undone by stopFeatures
	private final List<Listener>			featureListeners	= new ArrayList<Listener>();
	// Players the server has seen, by lower case name; built when an old data file needs it, dropped after loading
	private Map<String, UUID>				knownPlayerIds;
	// Names some Minecraft versions have and others don't (version-names.txt), read once
	private Set<String>						versionNames;

	/**
	 * The settings read from config.yml at the last (re)load. Read them through here (or {@link GodsContext#settings()})
	 * every time instead of keeping them, so /g reload takes effect.
	 */
	public Settings settings()
	{
		return this.settings;
	}

	/** The god types' themes read at the last (re)load; like {@link #settings()}, read them through here every time */
	public Themes themes()
	{
		return this.themes;
	}

	@Override
	public void onEnable()
	{
		try
		{
			this.settings = loadSettings();
			this.themes = loadThemes(this.settings, null);
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

		registerCommands();

		PluginManager pm = getServer().getPluginManager();
		this.actionCooldown = new ActionCooldown();
		pm.registerEvents(new GodListener(this.ctx, this.actionCooldown), this);
		pm.registerEvents(new BelieverListener(this.ctx), this);
		pm.registerEvents(new AltarListener(this.ctx), this);
		pm.registerEvents(new ChatListener(this.ctx), this);
		pm.registerEvents(new AltarWorldListener(this.ctx), this);

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
		// Scans a few chunks per tick for altars (queued as chunks load, and all loaded ones on enable and reload)
		this.tasks.add(getServer().getScheduler().runTaskTimer(this, () -> this.ctx.altarRegistry().processQueue(), 1L, 1L));

		startFeatures();

		// The API for other plugins, unregistered by onDisable
		getServer().getServicesManager().register(GodsApi.class, new GodsApiService(this.ctx), this, ServicePriority.Normal);
	}

	@Override
	public void onDisable()
	{
		if (this.ctx == null)
		{
			// config.yml couldn't be read: nothing was loaded, so nothing is saved
			return;
		}

		getServer().getServicesManager().unregisterAll(this);

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
	 * /g reload: reads config.yml again, then the message files and the data files, and restarts the features so
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

		Themes newThemes = loadThemes(newSettings, this.themes);

		if (!this.storage.prepareReload(10, TimeUnit.SECONDS))
		{
			getLogger().severe("Reload failed: the data files could not be saved in time, so they are not read again.");
			return false;
		}

		this.settings = newSettings;
		this.themes = newThemes;
		stopFeatures();
		loadData();
		startFeatures();
		this.storage.finishReload();
		this.metrics.refresh();
		return true;
	}

	/**
	 * Loads the messages and the data of every enabled feature, in the order the services depend on each other.
	 * Data of a feature that is switched off is not read (and so not written). A step that fails is logged and the
	 * others still run; nothing is then removed because of what another file says until the next (re)load works.
	 */
	private void loadData()
	{
		boolean complete = true;
		if (this.settings.bibles().enabled())
		{
			complete &= loadStep("bibles.yml", () -> this.ctx.holyBookService().load(), true);
		}
		if (this.settings.marriage().enabled())
		{
			complete &= loadStep("marriages.yml", () -> this.ctx.marriageService().load(), true);
		}
		if (this.settings.holyLand().enabled())
		{
			complete &= loadStep("holyland.yml", () -> this.ctx.holyLandService().load(), true);
		}

		// Old lang/<language>_*.yml files (Gods 0.9.x and before MiniMessage) become the new message files first
		loadStep("the old language files", () -> LegacyLangFiles.migrate(getDataFolder().toPath().resolve("lang"), this::getResource, getLogger()));
		loadStep("the message files", () -> this.ctx.messages().load(this.settings.general().language(), this.settings.general().serverName()));
		complete &= loadStep("gods.yml", () -> this.ctx.godService().load(), true);
		// Where the altars are; not needed to judge dead gods, so it doesn't hold back cleanups
		loadStep("altars.yml", () -> this.ctx.altarRegistry().load());
		complete &= loadStep("quests.yml", () -> this.ctx.questService().load(), true);
		complete &= loadStep("believers.yml", () -> this.ctx.believerService().load(), true);
		complete &= loadStep("whitelist.yml and blacklist.yml", () -> this.ctx.godListService().load(), true);

		this.knownPlayerIds = null;
		// Nothing is removed because of what another file says for a few minutes, in case an edit is still going on
		this.storage.dataLoaded(complete);
	}

	/**
	 * Runs one step of {@link #loadData()}; returns false (after logging it) if it failed. Only the steps whose result
	 * goes into the cleanup guard ({@code complete &=}) stop dead gods and inactive priests from being removed.
	 */
	private boolean loadStep(String what, Runnable step)
	{
		return loadStep(what, step, false);
	}

	private boolean loadStep(String what, Runnable step, boolean blocksCleanup)
	{
		try
		{
			step.run();
			return true;
		}
		catch (RuntimeException ex)
		{
			getLogger().log(Level.SEVERE, "Could not load " + what + "; going on without it." + (blocksCleanup ? " Dead gods and inactive priests are not removed until this is fixed and Gods is reloaded." : ""), ex);
			return false;
		}
	}

	/** Registers /g (alias /gods) and every subcommand: the one place a new command is added */
	private void registerCommands()
	{
		CommandRouter router = new CommandRouter(this.ctx.messages(), this.ctx.permissions(), CommandChecks.of(this.ctx), new Overview(this.ctx));
		for (SubCommand command : List.of(
				new AcceptCommand(this.ctx), new AccessCommand(this.ctx), new AllyCommand(this.ctx), new AttackCommand(this.ctx),
				new BibleCommand(this.ctx), new ChatCommand(this.ctx), new CheckCommand(this.ctx), new DescriptionCommand(this.ctx),
				new DivorceCommand(this.ctx), new EditBibleCommand(this.ctx), new FollowersCommand(this.ctx), new GenderCommand(this.ctx), new GodSkinCommand(this.ctx),
				new HelpCommand(this.ctx, router), new HolyArtifactCommand(this.ctx), new HomeCommand(this.ctx), new HuntCommand(this.ctx),
				new InfoCommand(this.ctx), new InviteCommand(this.ctx), new KickCommand(this.ctx), new LeaveCommand(this.ctx),
				new ListCommand(this.ctx), new LoveCommand(this.ctx), new MarriagesCommand(this.ctx), new MarryCommand(this.ctx),
				new PrayForCommand(this.ctx), new PvpCommand(this.ctx), new RankCommand(this.ctx), new RejectCommand(this.ctx), new ReloadCommand(this.ctx),
				new SetAltarCommand(this.ctx), new SetBibleCommand(this.ctx), new SetPriestCommand(this.ctx), new SetSafeCommand(this.ctx),
				new StartAttackCommand(this.ctx), new WarCommand(this.ctx)))
		{
			router.register(command);
		}
		PluginCommand command = getCommand("g");
		command.setExecutor(router);
		command.setTabCompleter(router);
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
					this.knownPlayerIds.put(player.getName().toLowerCase(Locale.ROOT), player.getUniqueId());
				}
			}
		}
		return this.knownPlayerIds.get(name.toLowerCase(Locale.ROOT));
	}

	/** The features that can be switched on and off in config.yml */
	private void startFeatures()
	{
		// Registered before HolyLandListener, as they always were
		if (this.settings.quests().enabled())
		{
			registerFeature(new QuestListener(this.ctx, this.actionCooldown));
		}
		if (this.settings.bibles().enabled())
		{
			registerFeature(new HolyBookListener(this.ctx, this.actionCooldown));
		}
		if (this.settings.holyArtifacts().enabled())
		{
			registerFeature(new HolyArtifactListener(this.ctx));
		}
		if (this.settings.holyLand().enabled())
		{
			registerFeature(new HolyLandListener(this.ctx));
		}
		if (this.settings.marriage().enabled())
		{
			registerFeature(new MarriageListener(this.ctx));
		}
		// The text above altars (also removes strays from before when it is off)
		this.ctx.altarTexts().start();
		// The gods at their main altars (also removes strays from before)
		if (this.settings.npc().enabled())
		{
			registerFeature(new GodNpcListener(this.ctx));
		}
		this.ctx.godNpcs().start();
	}

	private void stopFeatures()
	{
		// Ice spikes put back what they changed
		this.ctx.holyPowerService().stop();
		// The floating items and symbols of effects still running
		this.ctx.effectService().stop();
		// The floating texts above altars
		this.ctx.altarTexts().stop();
		// The gods' NPCs
		this.ctx.godNpcs().stop();
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
		YamlConfiguration config = configFormat().load(file, getLogger());
		YamlConfiguration defaults = bundledConfig();

		if (configFormat().isNewer(config))
		{
			// From a newer Gods: read what this version knows, but never write it back in the old shape
			getLogger().warning("config.yml is from a newer version of Gods; it is used as it is, and no missing settings are added to it.");
		}
		else if (ConfigDefaults.addMissing(config, defaults))
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
		return new SettingsLoader(config, getLogger()).load();
	}

	/**
	 * Reads the god types' themes: the themes.yml in the jar with the admin's plugins/Gods/themes.yml merged over it.
	 * The admin's file is written once, as a commented example, and never again. If it can't be read, the previous
	 * themes stay (on /g reload) or Gods' own are used (on enable).
	 */
	private Themes loadThemes(Settings settings, Themes previous)
	{
		File file = new File(getDataFolder(), "themes.yml");
		if (!file.exists())
		{
			try (InputStream in = getResource("themes-overrides.yml"))
			{
				if (in != null)
				{
					AtomicFiles.write(file.toPath(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
				}
			}
			catch (IOException e)
			{
				getLogger().warning("Could not write the example themes.yml: " + e.getMessage());
			}
		}

		YamlConfiguration overrides = new YamlConfiguration();
		boolean overridesRead = true;
		if (file.exists())
		{
			try
			{
				overrides.load(file);
			}
			catch (IOException | InvalidConfigurationException e)
			{
				overridesRead = false;
				getLogger().severe("Could not read themes.yml: " + e.getMessage());
			}
		}
		if (!overridesRead && previous != null)
		{
			getLogger().severe("Keeping the themes from before; fix themes.yml and run /g reload.");
			return new Themes(previous.defaults(), previous.types(), Themes.combineAltars(settings.altars(), previous.types()));
		}
		if (!overridesRead)
		{
			getLogger().severe("Using Gods' own themes without your changes; fix themes.yml and run /g reload.");
			overrides = new YamlConfiguration();
		}

		YamlConfiguration bundled;
		try (InputStream in = getResource("themes.yml"))
		{
			bundled = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
			if (this.versionNames == null)
			{
				this.versionNames = ThemesLoader.readVersionNames(this::getResource);
			}
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Could not read the bundled themes.yml", e);
		}

		boolean debug = settings.general().debug();
		ThemesLoader loader = new ThemesLoader(ThemesLoader.serverNames(this.versionNames), getLogger(), message -> {
			if (debug)
			{
				getLogger().info("[Debug] " + message);
			}
		});
		return loader.load(bundled, overrides, settings.altars());
	}

	/**
	 * config.yml's versions: 1 (no config-version, CamelCase sections) is upgraded by {@link ConfigV1ToV2}, 2 by
	 * {@link ConfigV2ToV3} (altar blocks moved into the themes), 3 by {@link ConfigV3ToV4} (the home is the main altar), 4
	 * by {@link ConfigV4ToV5} (the descent became the descended rank); a
	 * file without config-version that already has the kebab-case
	 * sections only lost its key and is taken as the current version
	 */
	private VersionedYaml configFormat()
	{
		return new VersionedYaml("config-version", CONFIG_VERSION, 1, List.of(new ConfigV1ToV2(this::getResource, getLogger()), new ConfigV2ToV3(this::getResource), new ConfigV3ToV4(getLogger()),
				new ConfigV4ToV5(this::getResource, getLogger())), ConfigV1ToV2::isVersion2Layout);
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
		return this.settings.general().isActiveIn(world.getName());
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
