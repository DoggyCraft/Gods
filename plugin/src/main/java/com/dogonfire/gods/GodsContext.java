package com.dogonfire.gods;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.integration.WorldGuardIntegration;
import com.dogonfire.gods.managers.ChatManager;
import com.dogonfire.gods.managers.HolyArtifactManager;
import com.dogonfire.gods.managers.HolyBookManager;
import com.dogonfire.gods.managers.HolyPowerManager;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.managers.PermissionsManager;
import com.dogonfire.gods.managers.QuestManager;
import com.dogonfire.gods.service.AltarService;
import com.dogonfire.gods.service.BelieverService;
import com.dogonfire.gods.service.BlessingService;
import com.dogonfire.gods.service.CommandmentService;
import com.dogonfire.gods.service.GodListService;
import com.dogonfire.gods.service.GodService;
import com.dogonfire.gods.service.GodSpeech;
import com.dogonfire.gods.service.HolyLandService;
import com.dogonfire.gods.service.MarriageService;
import com.dogonfire.gods.service.MoodService;
import com.dogonfire.gods.service.PrayerService;
import com.dogonfire.gods.service.PriestService;
import com.dogonfire.gods.service.RelationService;
import com.dogonfire.gods.storage.Storage;

/**
 * The plugin's object graph, built once per enable by {@link Gods#onEnable()} and passed to the constructor of every
 * manager, service, listener, command and task. There is no static way to reach it.
 * <p>
 * The managers and services call each other in cycles, so each one keeps the context and asks it for the others when it
 * needs them; a service gets its repository from {@link Storage} through its constructor. Constructors must not call
 * other managers or services: the graph is still being built. Every manager and service lives until the plugin is
 * disabled, also the ones of features that are switched off (their data file is then simply not loaded), so /g reload
 * can switch features on and off.
 */
public final class GodsContext
{
	private final Gods						plugin;
	private final Storage					storage;

	private final LanguageManager			languageManager;
	private final PermissionsManager		permissionsManager;
	private final GodService				godService;
	private final MoodService				moodService;
	private final RelationService			relationService;
	private final PriestService				priestService;
	private final BlessingService			blessingService;
	private final CommandmentService		commandmentService;
	private final PrayerService				prayerService;
	private final BelieverService			believerService;
	private final QuestManager				questManager;
	private final AltarService				altarService;
	private final HolyLandService			holyLandService;
	private final HolyBookManager			holyBookManager;
	private final MarriageService			marriageService;
	private final HolyArtifactManager		holyArtifactManager;
	private final HolyPowerManager			holyPowerManager;
	private final GodListService			godListService;
	private final ChatManager				chatManager;
	private final GodSpeech					godSpeech;

	// Null when WorldGuard isn't installed; its classes must not be touched then
	private final WorldGuardIntegration		worldGuard;

	GodsContext(Gods plugin, Storage storage)
	{
		this.plugin = plugin;
		this.storage = storage;

		this.languageManager = new LanguageManager(this);
		this.permissionsManager = new PermissionsManager();
		this.godService = new GodService(this, storage.gods());
		this.moodService = new MoodService(this, storage.gods());
		this.relationService = new RelationService(this, storage.gods());
		this.priestService = new PriestService(this, storage.gods());
		this.blessingService = new BlessingService(this, storage.gods());
		this.commandmentService = new CommandmentService(this, storage.gods());
		this.prayerService = new PrayerService(this);
		this.believerService = new BelieverService(this, storage.believers());
		this.questManager = new QuestManager(this);
		this.altarService = new AltarService(this);
		this.holyLandService = new HolyLandService(this, storage.holyLands());
		this.holyBookManager = new HolyBookManager(this);
		this.marriageService = new MarriageService(this, storage.marriages());
		this.holyArtifactManager = new HolyArtifactManager(this);
		this.holyPowerManager = new HolyPowerManager(this);
		this.godListService = new GodListService(this, storage.whitelist(), storage.blacklist());
		this.chatManager = new ChatManager(this);
		this.godSpeech = new GodSpeech(this);

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

	/** Gods: which exist and are online, what they are, their power and what their priests set; loads gods.yml */
	public GodService godService()
	{
		return this.godService;
	}

	/** Gods' moods */
	public MoodService moodService()
	{
		return this.moodService;
	}

	/** Alliances, wars and the holy land a god attacks */
	public RelationService relationService()
	{
		return this.relationService;
	}

	/** Priests, priest offers and reminders */
	public PriestService priestService()
	{
		return this.priestService;
	}

	/** Blessings and curses on players, and a god's blessed and cursed player */
	public BlessingService blessingService()
	{
		return this.blessingService;
	}

	/** Holy and unholy food and mobs, and sacrifices */
	public CommandmentService commandmentService()
	{
		return this.commandmentService;
	}

	/** Praying at altars, joining, answering and leaving a god */
	public PrayerService prayerService()
	{
		return this.prayerService;
	}

	public BelieverService believerService()
	{
		return this.believerService;
	}

	public QuestManager questManager()
	{
		return this.questManager;
	}

	public AltarService altarService()
	{
		return this.altarService;
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

	/** Gods speaking to players, and the info messages */
	public GodSpeech godSpeech()
	{
		return this.godSpeech;
	}

	/** Null when WorldGuard is not installed */
	public WorldGuardIntegration worldGuard()
	{
		return this.worldGuard;
	}
}
