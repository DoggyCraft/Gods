package com.dogonfire.gods.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One god in gods.yml, keyed by its name, with the keys noted on the fields (times are {@code "HH:mm:ss dd-MM-yyyy"}).
 * A god is created by the first prayer at its altar; settings a priest changes (description, home, access, PvP), its
 * mood, priests, relations to other gods, the player it blesses or curses, its commandments (holy and unholy food and
 * mobs) and what it wants sacrificed. Values that are generated on first use (seed, verbosity, commandments) are
 * {@code null}/0 until then. A plain data object: the god services change it and mark the repository dirty.
 */
public final class God
{
	private final String		name;

	private Instant				created;					// Created: a god "exists" (godExist) once this is set
	private GodType				type;						// DivineForce; null when missing or unknown
	private GodGender			gender;						// Gender; null when missing or unknown
	private boolean				privateAccess;				// PrivateAccess: only believers may use its altars
	private String				description;				// Description
	private GodHome				home;						// Home
	private BlockPos			mainAltar;					// MainAltar.World/X/Y/Z: the sign of its main altar
	private NpcSkin				npcSkin;					// NpcSkin.Name/Value/Signature: the skin of its NPC (/g godskin)

	private float				mood;						// Mood: -100 (angry) to 100 (exalted)
	private long				seed;						// Seed: 0 until generated; fixes its personality
	private int					verbosity;					// Verbosity: 0 until generated

	private List<UUID>			priests			= new ArrayList<UUID>();	// Priests
	private UUID				pendingPriest;				// PendingPriest: offered priesthood, no answer yet
	private Instant				pendingPriestTime;			// PendingPriestTime: when priesthood was last offered

	private List<String>		allies			= new ArrayList<String>();	// Allies: god names
	private List<String>		enemies			= new ArrayList<String>();	// Enemies: god names
	private ContestedLand		contestedLand;				// ContestedLand: the holy land it attacks

	private UUID				blessedPlayer;				// BlessedPlayer
	private Instant				blessedTime;				// BlessedTime
	private UUID				cursedPlayer;				// CursedPlayer
	private Instant				cursedTime;					// CursedTime

	// Commandments, as Material / EntityType names (an unknown name is kept as it is)
	private String				holyFood;					// EatFoodType
	private String				unholyFood;					// NotEatFoodType
	private String				holyMob;					// NotSlayMobType: must not be killed
	private String				unholyMob;					// SlayMobType: should be killed

	/** SacrificeValues: how much it wants each item (Material name), in file order */
	private Map<String, Float>	sacrificeValues	= new LinkedHashMap<String, Float>();

	// Holy land settings; null means the default from config.yml (MobSpawning: false)
	private Boolean				pvp;						// PvP
	private Boolean				mobDamage;					// MobDamage
	private Boolean				mobSpawning;				// MobSpawning

	private Instant				lastQuestTime;				// LastQuestTime
	private Instant				genderChangedTime;			// GenderChangedTime: when a priest (or a plugin) last changed its gender
	private Instant				priestSince;				// PriestSince: since when it has had a priest (null: none); for its descent
	private Instant				descended;					// DescendedTime: when it descended to its main altar (null: it hasn't, or ascended)
	private Instant				ascendingSince;			// AscendingSince: since when it has had too few believers for its rank (see RankRules)
	private GodRank				rank;						// Rank: null until it was first ranked (read as SPIRIT)
	private Instant				rankSince;					// RankSince: when it reached its rank
	private GodStats			stats			= GodStats.ZERO;	// Stats.Prayers/Sacrifices/Quests/Converts
	private boolean				statsSeeded;				// StatsSeeded: its stats were set for a god from before ranks (or it is new)

	/** Keys of the entry this version doesn't know (or values it can't read), written back unchanged */
	private Map<String, Object>	extras			= new LinkedHashMap<String, Object>();

	public God(String name)
	{
		this.name = name;
	}

	public String name()
	{
		return this.name;
	}

	public Instant created()
	{
		return this.created;
	}

	public void setCreated(Instant created)
	{
		this.created = created;
	}

	public GodType type()
	{
		return this.type;
	}

	public void setType(GodType type)
	{
		this.type = type;
	}

	public GodGender gender()
	{
		return this.gender;
	}

	public void setGender(GodGender gender)
	{
		this.gender = gender;
	}

	public boolean privateAccess()
	{
		return this.privateAccess;
	}

	public void setPrivateAccess(boolean privateAccess)
	{
		this.privateAccess = privateAccess;
	}

	public String description()
	{
		return this.description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public GodHome home()
	{
		return this.home;
	}

	public void setHome(GodHome home)
	{
		this.home = home;
	}

	/** The sign of the god's main altar (where it was founded, chosen on its own or with /g setaltar), or null */
	public BlockPos mainAltar()
	{
		return this.mainAltar;
	}

	public void setMainAltar(BlockPos mainAltar)
	{
		this.mainAltar = mainAltar;
	}

	/** The skin of the god's NPC set with /g godskin, or null for its theme's */
	public NpcSkin npcSkin()
	{
		return this.npcSkin;
	}

	public void setNpcSkin(NpcSkin npcSkin)
	{
		this.npcSkin = npcSkin;
	}

	public float mood()
	{
		return this.mood;
	}

	public void setMood(float mood)
	{
		this.mood = mood;
	}

	public long seed()
	{
		return this.seed;
	}

	public void setSeed(long seed)
	{
		this.seed = seed;
	}

	public int verbosity()
	{
		return this.verbosity;
	}

	public void setVerbosity(int verbosity)
	{
		this.verbosity = verbosity;
	}

	/** The priests, in the order they were appointed. Change the list itself. */
	public List<UUID> priests()
	{
		return this.priests;
	}

	public UUID pendingPriest()
	{
		return this.pendingPriest;
	}

	public void setPendingPriest(UUID pendingPriest)
	{
		this.pendingPriest = pendingPriest;
	}

	public Instant pendingPriestTime()
	{
		return this.pendingPriestTime;
	}

	public void setPendingPriestTime(Instant pendingPriestTime)
	{
		this.pendingPriestTime = pendingPriestTime;
	}

	/** Change the list itself */
	public List<String> allies()
	{
		return this.allies;
	}

	/** Change the list itself */
	public List<String> enemies()
	{
		return this.enemies;
	}

	public ContestedLand contestedLand()
	{
		return this.contestedLand;
	}

	public void setContestedLand(ContestedLand contestedLand)
	{
		this.contestedLand = contestedLand;
	}

	public UUID blessedPlayer()
	{
		return this.blessedPlayer;
	}

	public Instant blessedTime()
	{
		return this.blessedTime;
	}

	/** Both null to clear */
	public void setBlessed(UUID player, Instant time)
	{
		this.blessedPlayer = player;
		this.blessedTime = time;
	}

	public UUID cursedPlayer()
	{
		return this.cursedPlayer;
	}

	public Instant cursedTime()
	{
		return this.cursedTime;
	}

	/** Both null to clear */
	public void setCursed(UUID player, Instant time)
	{
		this.cursedPlayer = player;
		this.cursedTime = time;
	}

	public String holyFood()
	{
		return this.holyFood;
	}

	public void setHolyFood(String holyFood)
	{
		this.holyFood = holyFood;
	}

	public String unholyFood()
	{
		return this.unholyFood;
	}

	public void setUnholyFood(String unholyFood)
	{
		this.unholyFood = unholyFood;
	}

	public String holyMob()
	{
		return this.holyMob;
	}

	public void setHolyMob(String holyMob)
	{
		this.holyMob = holyMob;
	}

	public String unholyMob()
	{
		return this.unholyMob;
	}

	public void setUnholyMob(String unholyMob)
	{
		this.unholyMob = unholyMob;
	}

	/** By Material name, in file order. Change the map itself. */
	public Map<String, Float> sacrificeValues()
	{
		return this.sacrificeValues;
	}

	public Boolean pvp()
	{
		return this.pvp;
	}

	public void setPvp(Boolean pvp)
	{
		this.pvp = pvp;
	}

	public Boolean mobDamage()
	{
		return this.mobDamage;
	}

	public void setMobDamage(Boolean mobDamage)
	{
		this.mobDamage = mobDamage;
	}

	public Boolean mobSpawning()
	{
		return this.mobSpawning;
	}

	public void setMobSpawning(Boolean mobSpawning)
	{
		this.mobSpawning = mobSpawning;
	}

	public Instant lastQuestTime()
	{
		return this.lastQuestTime;
	}

	public void setLastQuestTime(Instant lastQuestTime)
	{
		this.lastQuestTime = lastQuestTime;
	}

	public Instant genderChangedTime()
	{
		return this.genderChangedTime;
	}

	public void setGenderChangedTime(Instant genderChangedTime)
	{
		this.genderChangedTime = genderChangedTime;
	}

	/** Since when the god has had a priest, or null (see {@code DescentRules}) */
	public Instant priestSince()
	{
		return this.priestSince;
	}

	public void setPriestSince(Instant priestSince)
	{
		this.priestSince = priestSince;
	}

	/** When the god descended to its main altar (its NPC shows), or null */
	public Instant descended()
	{
		return this.descended;
	}

	public void setDescended(Instant descended)
	{
		this.descended = descended;
	}

	/** Since when the god has had fewer believers than its rank needs, or null (see {@code RankRules}) */
	public Instant ascendingSince()
	{
		return this.ascendingSince;
	}

	public void setAscendingSince(Instant ascendingSince)
	{
		this.ascendingSince = ascendingSince;
	}

	/** Its rank, or null for a god that was never ranked (it is a spirit) */
	public GodRank rank()
	{
		return this.rank;
	}

	public void setRank(GodRank rank)
	{
		this.rank = rank;
	}

	/** When it reached its rank, or null */
	public Instant rankSince()
	{
		return this.rankSince;
	}

	public void setRankSince(Instant rankSince)
	{
		this.rankSince = rankSince;
	}

	/** What its religion has done (never null) */
	public GodStats stats()
	{
		return this.stats;
	}

	public void setStats(GodStats stats)
	{
		this.stats = stats != null ? stats : GodStats.ZERO;
	}

	/** Whether its stats were seeded (a god from before ranks) or it was created with them */
	public boolean statsSeeded()
	{
		return this.statsSeeded;
	}

	public void setStatsSeeded(boolean statsSeeded)
	{
		this.statsSeeded = statsSeeded;
	}

	public Map<String, Object> extras()
	{
		return this.extras;
	}

	public void setExtras(Map<String, Object> extras)
	{
		this.extras = extras;
	}
}
