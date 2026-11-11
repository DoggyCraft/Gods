package com.dogonfire.gods;

import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.config.Themes;
import com.dogonfire.gods.integration.GriefPreventionIntegration;
import com.dogonfire.gods.integration.WorldGuardIntegration;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.service.AltarRegistry;
import com.dogonfire.gods.service.AltarService;
import com.dogonfire.gods.service.AltarTextService;
import com.dogonfire.gods.service.BelieverService;
import com.dogonfire.gods.service.BlessingService;
import com.dogonfire.gods.service.ChatService;
import com.dogonfire.gods.service.CommandmentService;
import com.dogonfire.gods.service.EffectService;
import com.dogonfire.gods.service.GodListService;
import com.dogonfire.gods.service.GodNpcService;
import com.dogonfire.gods.service.GodService;
import com.dogonfire.gods.service.GodSpeech;
import com.dogonfire.gods.service.HolyArtifactService;
import com.dogonfire.gods.service.HolyBookService;
import com.dogonfire.gods.service.HolyLandService;
import com.dogonfire.gods.service.HolyPowerService;
import com.dogonfire.gods.service.MarriageService;
import com.dogonfire.gods.service.MoodService;
import com.dogonfire.gods.service.Permissions;
import com.dogonfire.gods.service.PrayerService;
import com.dogonfire.gods.service.PriestService;
import com.dogonfire.gods.service.QuestService;
import com.dogonfire.gods.service.RelationService;
import com.dogonfire.gods.service.Titles;
import com.dogonfire.gods.storage.Storage;

/**
 * The plugin's object graph, built once per enable by {@link Gods#onEnable()} and passed to the constructor of every
 * service, listener, command and task. There is no static way to reach it.
 * <p>
 * The services call each other in cycles, so each one keeps the context and asks it for the others when it
 * needs them; a service gets its repository from {@link Storage} through its constructor. Constructors must not call
 * other services: the graph is still being built. Every service lives until the plugin is
 * disabled, also the ones of features that are switched off (their data file is then simply not loaded), so /g reload
 * can switch features on and off.
 */
public final class GodsContext
{
	private final Gods						plugin;
	private final Storage					storage;

	private final Messages					messages;
	private final Permissions				permissions;
	private final GodService				godService;
	private final MoodService				moodService;
	private final RelationService			relationService;
	private final PriestService				priestService;
	private final BlessingService			blessingService;
	private final CommandmentService		commandmentService;
	private final PrayerService				prayerService;
	private final BelieverService			believerService;
	private final QuestService				questService;
	private final AltarService				altarService;
	private final AltarRegistry				altarRegistry;
	private final AltarTextService			altarTexts;
	private final GodNpcService			godNpcs;
	private final HolyLandService			holyLandService;
	private final HolyBookService			holyBookService;
	private final MarriageService			marriageService;
	private final HolyArtifactService		holyArtifactService;
	private final HolyPowerService			holyPowerService;
	private final GodListService			godListService;
	private final ChatService				chatService;
	private final Titles					titles;
	private final GodSpeech					godSpeech;
	private final EffectService				effectService;

	// Null when WorldGuard isn't installed; its classes must not be touched then
	private final WorldGuardIntegration		worldGuard;
	// Null when GriefPrevention isn't installed; its classes must not be touched then
	private final GriefPreventionIntegration	griefPrevention;

	GodsContext(Gods plugin, Storage storage)
	{
		this.plugin = plugin;
		this.storage = storage;

		this.messages = new Messages(plugin.getDataFolder().toPath().resolve("lang"), plugin::getResource, plugin.getLogger());
		this.permissions = new Permissions();
		this.godService = new GodService(this, storage.gods());
		this.moodService = new MoodService(this, storage.gods());
		this.relationService = new RelationService(this, storage.gods());
		this.priestService = new PriestService(this, storage.gods());
		this.blessingService = new BlessingService(this, storage.gods());
		this.commandmentService = new CommandmentService(this, storage.gods());
		this.prayerService = new PrayerService(this);
		this.believerService = new BelieverService(this, storage.believers());
		this.questService = new QuestService(this, storage.quests());
		this.altarService = new AltarService(this);
		this.altarRegistry = new AltarRegistry(this, storage.altars());
		this.altarTexts = new AltarTextService(this);
		this.godNpcs = new GodNpcService(this);
		this.holyLandService = new HolyLandService(this, storage.holyLands());
		this.holyBookService = new HolyBookService(this, storage.bibles());
		this.marriageService = new MarriageService(this, storage.marriages());
		this.holyArtifactService = new HolyArtifactService(this);
		this.holyPowerService = new HolyPowerService(this);
		this.godListService = new GodListService(this, storage.whitelist(), storage.blacklist());
		this.chatService = new ChatService(this);
		this.titles = new Titles(plugin.getServer().getPluginManager());
		this.godSpeech = new GodSpeech(this);
		this.effectService = new EffectService(this);

		this.worldGuard = isEnabled(plugin.getServer().getPluginManager(), "WorldGuard") ? new WorldGuardIntegration() : null;
		this.griefPrevention = isEnabled(plugin.getServer().getPluginManager(), "GriefPrevention") ? new GriefPreventionIntegration() : null;
	}

	static boolean isEnabled(PluginManager pm, String name)
	{
		Plugin other = pm.getPlugin(name);
		return other != null && other.isEnabled();
	}

	/**
	 * Calls an event (the API's events in {@code com.dogonfire.gods.api.event}) and returns it, to read what listeners
	 * did with it. The services fire them, so commands, listeners, the gods' turns and the API all do.
	 */
	public <T extends Event> T fire(T event)
	{
		this.plugin.getServer().getPluginManager().callEvent(event);
		return event;
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

	/** The god types' themes (themes.yml); replaced on /g reload, so read them when needed instead of keeping them */
	public Themes themes()
	{
		return this.plugin.themes();
	}

	public Storage storage()
	{
		return this.storage;
	}

	/** The texts players see (messages.yml) and the gods' voices; read again on /g reload */
	public Messages messages()
	{
		return this.messages;
	}

	/** Permission checks (Bukkit's own) */
	public Permissions permissions()
	{
		return this.permissions;
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

	/** Quests: new ones, progress, completion and failure; loads quests.yml */
	public QuestService questService()
	{
		return this.questService;
	}

	public AltarService altarService()
	{
		return this.altarService;
	}

	/** Where the altars are (altars.yml) and each god's main altar */
	public AltarRegistry altarRegistry()
	{
		return this.altarRegistry;
	}

	/** The text floating above praying altars; started and stopped with the features */
	public AltarTextService altarTexts()
	{
		return this.altarTexts;
	}

	/** The gods' NPCs at their main altars; started and stopped with the features */
	public GodNpcService godNpcs()
	{
		return this.godNpcs;
	}

	/** Its data is only loaded, and its listener (HolyLandListener) only registered, while holy land is enabled */
	public HolyLandService holyLandService()
	{
		return this.holyLandService;
	}

	/** Its data is only loaded while bibles are enabled */
	public HolyBookService holyBookService()
	{
		return this.holyBookService;
	}

	/** Its data is only loaded, and its listener (MarriageListener) only registered, while marriage is enabled */
	public MarriageService marriageService()
	{
		return this.marriageService;
	}

	/** Holy artifacts: making, recognizing and using them */
	public HolyArtifactService holyArtifactService()
	{
		return this.holyArtifactService;
	}

	/** Holy powers (the effects of holy artifacts) and their cooldowns */
	public HolyPowerService holyPowerService()
	{
		return this.holyPowerService;
	}

	/** The god whitelist and blacklist */
	public GodListService godListService()
	{
		return this.godListService;
	}

	/** Religion chat, and the thread-safe snapshot the chat thread and PlaceholderAPI read */
	public ChatService chatService()
	{
		return this.chatService;
	}

	/** Gods speaking to players, and the info messages */
	public GodSpeech godSpeech()
	{
		return this.godSpeech;
	}

	/** Themed effects at prayers, sacrifices, blessings and curses; their displays are removed by stop() */
	public EffectService effectService()
	{
		return this.effectService;
	}

	/** Titles on the screen (holy land you enter and the like) */
	public Titles titles()
	{
		return this.titles;
	}

	/** Null when WorldGuard is not installed */
	public WorldGuardIntegration worldGuard()
	{
		return this.worldGuard;
	}

	/** Null when GriefPrevention is not installed (or not enabled) */
	public GriefPreventionIntegration griefPrevention()
	{
		return this.griefPrevention;
	}
}
