package com.dogonfire.gods;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.integration.WorldGuardIntegration;
import com.dogonfire.gods.managers.AltarManager;
import com.dogonfire.gods.managers.ChatManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.HolyArtifactManager;
import com.dogonfire.gods.managers.HolyBookManager;
import com.dogonfire.gods.managers.HolyPowerManager;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.managers.PermissionsManager;
import com.dogonfire.gods.managers.QuestManager;
import com.dogonfire.gods.service.BelieverService;
import com.dogonfire.gods.service.GodListService;
import com.dogonfire.gods.service.HolyLandService;
import com.dogonfire.gods.service.MarriageService;
import com.dogonfire.gods.storage.Storage;

/**
 * The plugin's object graph, built once per enable by {@link Gods#onEnable()} and passed to the constructor of every
 * manager, listener, command and task. There is no static way to reach it.
 * <p>
 * The managers call each other in cycles, so each one keeps the context and asks it for the others when it needs them.
 * Constructors must not call other managers: the graph is still being built. Every manager lives until the plugin is
 * disabled, also the ones of features that are switched off (their data file is then simply not loaded), so /g reload
 * can switch features on and off.
 */
public final class GodsContext
{
	private final Gods						plugin;
	private final Storage					storage;

	private final LanguageManager			languageManager;
	private final PermissionsManager		permissionsManager;
	private final GodManager				godManager;
	private final BelieverService			believerService;
	private final QuestManager				questManager;
	private final AltarManager				altarManager;
	private final HolyLandService			holyLandService;
	private final HolyBookManager			holyBookManager;
	private final MarriageService			marriageService;
	private final HolyArtifactManager		holyArtifactManager;
	private final HolyPowerManager			holyPowerManager;
	private final GodListService			godListService;
	private final ChatManager				chatManager;

	// Null when WorldGuard isn't installed; its classes must not be touched then
	private final WorldGuardIntegration		worldGuard;

	GodsContext(Gods plugin, Storage storage)
	{
		this.plugin = plugin;
		this.storage = storage;

		this.languageManager = new LanguageManager(this);
		this.permissionsManager = new PermissionsManager();
		this.godManager = new GodManager(this);
		this.believerService = new BelieverService(this, storage.believers());
		this.questManager = new QuestManager(this);
		this.altarManager = new AltarManager(this);
		this.holyLandService = new HolyLandService(this, storage.holyLands());
		this.holyBookManager = new HolyBookManager(this);
		this.marriageService = new MarriageService(this, storage.marriages());
		this.holyArtifactManager = new HolyArtifactManager(this);
		this.holyPowerManager = new HolyPowerManager(this);
		this.godListService = new GodListService(this, storage.whitelist(), storage.blacklist());
		this.chatManager = new ChatManager(this);

		this.worldGuard = isEnabled(plugin.getServer().getPluginManager(), "WorldGuard") ? new WorldGuardIntegration() : null;
	}

	static boolean isEnabled(PluginManager pm, String name)
	{
		Plugin other = pm.getPlugin(name);
		return other != null && other.isEnabled();
	}

	public Gods plugin()
	{
		return this.plugin;
	}

	/** The current settings; replaced on /g reload, so read them when needed instead of keeping them */
	public Settings settings()
	{
		return this.plugin.settings();
	}

	public Storage storage()
	{
		return this.storage;
	}

	public LanguageManager languageManager()
	{
		return this.languageManager;
	}

	public PermissionsManager permissionsManager()
	{
		return this.permissionsManager;
	}

	public GodManager godManager()
	{
		return this.godManager;
	}

	public BelieverService believerService()
	{
		return this.believerService;
	}

	public QuestManager questManager()
	{
		return this.questManager;
	}

	public AltarManager altarManager()
	{
		return this.altarManager;
	}

	/** Its data is only loaded, and its listener (HolyLandListener) only registered, while holy land is enabled */
	public HolyLandService holyLandService()
	{
		return this.holyLandService;
	}

	/** Its data is only loaded while bibles are enabled */
	public HolyBookManager holyBookManager()
	{
		return this.holyBookManager;
	}

	/** Its data is only loaded, and its listener (MarriageListener) only registered, while marriage is enabled */
	public MarriageService marriageService()
	{
		return this.marriageService;
	}

	public HolyArtifactManager holyArtifactManager()
	{
		return this.holyArtifactManager;
	}

	public HolyPowerManager holyPowerManager()
	{
		return this.holyPowerManager;
	}

	/** The god whitelist and blacklist */
	public GodListService godListService()
	{
		return this.godListService;
	}

	public ChatManager chatManager()
	{
		return this.chatManager;
	}

	/** Null when WorldGuard is not installed */
	public WorldGuardIntegration worldGuard()
	{
		return this.worldGuard;
	}
}
