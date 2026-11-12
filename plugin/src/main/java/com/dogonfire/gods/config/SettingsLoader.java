package com.dogonfire.gods.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import com.dogonfire.gods.model.DevotionLevel;
import com.dogonfire.gods.model.GodMood;
import com.dogonfire.gods.model.GodRank;
import com.dogonfire.gods.model.GodType;

/**
 * Reads {@link Settings} from config.yml. The config must have the bundled config.yml as its defaults (Bukkit's
 * {@code setDefaults}), so missing keys get the bundled value and the defaults live in one place. A value of the wrong
 * type is logged and replaced by the default; a number out of range is logged and clamped. Unknown materials and god
 * types are logged and skipped.
 */
public class SettingsLoader
{
	/** The conditions a rank may have in config.yml (ranks.<rank>) */
	public static final List<String>	RANK_CONDITIONS	= List.of("min-believers", "min-power", "requires-priest", "min-mood", "requires-main-altar", "hours-after-priest",
			"hours-at-previous-rank", "min-prayers", "min-sacrifices", "min-quests", "min-altars", "min-holy-land-regions", "min-converts");

	private final Configuration	config;
	private final Logger		logger;

	/**
	 * @param config       config.yml, with the bundled config.yml as its defaults
	 */
	public SettingsLoader(Configuration config, Logger logger)
	{
		this.config = config;
		this.logger = logger;
	}

	public Settings load()
	{
		return new Settings(
				loadGeneral(),
				loadGods(),
				loadBelievers(),
				new Settings.Priests(
						atLeast("priests.min-believers", 0),
						atLeast("priests.max-per-god", 0),
						atLeast("priests.max-hours-without-prayer", 0),
						atLeast("priests.gender-change-cooldown-hours", 0),
						string("priests.assign-command"),
						string("priests.remove-command")),
				loadAltars(),
				new Settings.Prayers(
						bool("prayers.enabled"),
						atLeast("prayers.min-minutes-between-prayers", 0),
						atLeast("prayers.power-cost.item", 0),
						atLeast("prayers.power-cost.health", 0),
						atLeast("prayers.power-cost.blessing", 0),
						atLeast("prayers.power-cost.quest", 0)),
				new Settings.Sacrifices(bool("sacrifices.enabled")),
				new Settings.Commandments(
						bool("commandments.enabled"),
						bool("commandments.broadcast-food-eaten"),
						bool("commandments.broadcast-mob-slain")),
				new Settings.Blessings(
						bool("blessings.enabled"),
						atLeast("blessings.min-seconds-between-blessings", 0),
						atLeast("blessings.blessed-seconds", 0),
						bool("blessings.effects.speed"),
						bool("blessings.effects.regeneration"),
						bool("blessings.effects.heal"),
						bool("blessings.effects.fast-digging"),
						bool("blessings.effects.increase-damage")),
				new Settings.Curses(
						bool("curses.enabled"),
						atLeast("curses.min-seconds-between-curses", 0),
						atLeast("curses.cursed-minutes", 0),
						bool("curses.lightning"),
						bool("curses.mobs")),
				new Settings.ItemBlessings(
						bool("item-blessings.enabled"),
						atLeast("item-blessings.min-minutes-between-blessings", 0),
						atLeast("item-blessings.min-god-power", 0),
						atLeast("item-blessings.god-power-for-level-1-items", 0),
						atLeast("item-blessings.god-power-for-level-2-items", 0),
						atLeast("item-blessings.god-power-for-level-3-items", 0)),
				new Settings.HolyArtifacts(
						bool("holy-artifacts.enabled"),
						atLeast("holy-artifacts.min-minutes-between-blessings", 0)),
				loadQuests(),
				new Settings.HolyLand(
						bool("holy-land.enabled"),
						bool("holy-land.default-pvp"),
						bool("holy-land.default-mob-damage"),
						bool("holy-land.lightning"),
						bool("holy-land.allow-interaction-in-neutral-lands"),
						atLeast("holy-land.delete-abandoned-after-days", 0),
						materials("holy-land.breakable-blocks")),
				new Settings.Marriage(
						bool("marriage.enabled"),
						bool("marriage.wedding-fireworks")),
				new Settings.Bibles(
						bool("bibles.enabled"),
						bool("bibles.give-on-join")),
				new Settings.Chat(bool("chat.formatting")),
				new Settings.Effects(
						bool("effects.enabled"),
						bool("effects.prayer"),
						bool("effects.sacrifice"),
						bool("effects.blessings"),
						between("effects.view-distance-blocks", 1, 128)),
				new Settings.AltarTexts(
						floatingText("altars.floating-text"),
						between("altars.floating-text-view-distance-blocks", 1, 128)),
				new Settings.Npc(
						bool("npc.enabled"),
						npcProvider("npc.provider")),
				loadRanks(),
				loadDevotion());
	}

	private Settings.Believers loadBelievers()
	{
		Map<DevotionLevel, Double> multipliers = new EnumMap<>(DevotionLevel.class);
		for (DevotionLevel level : DevotionLevel.values())
		{
			String path = "believers.decay-after-multiplier." + level.key();
			double multiplier = atLeast(path, 0.1D);
			if (multiplier > 100.0D)
			{
				warn(path, multiplier, 100.0D);
				multiplier = 100.0D;
			}
			multipliers.put(level, multiplier);
		}
		return new Settings.Believers(
				bool("believers.leave-religion-on-death"),
				atLeast("believers.min-seconds-between-changing-god", 0),
				between("believers.decay-after-days", 1, 365),
				Collections.unmodifiableMap(multipliers),
				atLeast("believers.decay-per-day", 0),
				between("believers.decay-percent-per-day", 0, 100));
	}

	private Settings.Ranks loadRanks()
	{
		Map<GodRank, Settings.RankConditions> conditions = new EnumMap<>(GodRank.class);
		for (GodRank rank : GodRank.values())
		{
			conditions.put(rank, rank == GodRank.SPIRIT ? Settings.RankConditions.NONE : rankConditions("ranks." + rank.key()));
		}
		Map<GodRank, Integer> extraRegions = new EnumMap<>(GodRank.class);
		for (GodRank rank : GodRank.values())
		{
			extraRegions.put(rank, atLeast("ranks.unlocks.holy-land-extra-regions." + rank.key(), 0));
		}
		Settings.Unlocks unlocks = new Settings.Unlocks(
				rank("ranks.unlocks.bibles"),
				rank("ranks.unlocks.quests"),
				rank("ranks.unlocks.holy-artifacts"),
				rank("ranks.unlocks.relations"),
				rank("ranks.unlocks.extra-priest"),
				rank("ranks.unlocks.aura"),
				Collections.unmodifiableMap(extraRegions));
		String effect = string("ranks.aura.effect").trim().toLowerCase(Locale.ROOT);
		if (effect.startsWith("minecraft:"))
		{
			effect = effect.substring("minecraft:".length());
		}
		Settings.Aura aura = new Settings.Aura(
				bool("ranks.aura.enabled"),
				effect,
				between("ranks.aura.amplifier", 0, 4),
				between("ranks.aura.seconds", 1, 600));
		return new Settings.Ranks(Collections.unmodifiableMap(conditions), atLeast("ranks.drop-grace-hours", 0), unlocks, aura);
	}


	/**
	 * One rank's conditions ({@code ranks.<rank>}, a free-form section, see {@link ConfigDefaults#FREE_FORM_SECTIONS}):
	 * a condition the section leaves out doesn't count, so an admin drops one by removing it; a section that is
	 * missing altogether is the bundled one. A value of the wrong type is warned about and doesn't count.
	 */
	private Settings.RankConditions rankConditions(String path)
	{
		ConfigurationSection section = section(path);
		if (section == null)
		{
			if (this.config.isSet(path))
			{
				this.logger.warning(path + " in config.yml must be a section of conditions; " + path.substring(path.indexOf('.') + 1) + " has none.");
			}
			return Settings.RankConditions.NONE;
		}
		for (String key : section.getKeys(false))
		{
			if (!RANK_CONDITIONS.contains(key))
			{
				this.logger.warning("Unknown condition '" + key + "' in " + path + " in config.yml, ignoring it.");
			}
		}
		return new Settings.RankConditions(
				condition(section, path, "min-believers"),
				condition(section, path, "min-power"),
				conditionFlag(section, path, "requires-priest"),
				conditionMood(section, path, "min-mood"),
				conditionFlag(section, path, "requires-main-altar"),
				condition(section, path, "hours-after-priest"),
				condition(section, path, "hours-at-previous-rank"),
				condition(section, path, "min-prayers"),
				condition(section, path, "min-sacrifices"),
				condition(section, path, "min-quests"),
				condition(section, path, "min-altars"),
				condition(section, path, "min-holy-land-regions"),
				condition(section, path, "min-converts"));
	}

	/** A number condition: 0 when missing; one that isn't a number is warned about (0), a negative one is 0 */
	private int condition(ConfigurationSection section, String path, String key)
	{
		if (!section.isSet(key))
		{
			return 0;
		}
		Object value = section.get(key);
		if (!(value instanceof Number number))
		{
			warn(path + "." + key, value, 0);
			return 0;
		}
		if (number.intValue() < 0)
		{
			warn(path + "." + key, value, 0);
			return 0;
		}
		return number.intValue();
	}

	/** A true/false condition: false when missing or not true/false (warned about) */
	private boolean conditionFlag(ConfigurationSection section, String path, String key)
	{
		if (!section.isSet(key))
		{
			return false;
		}
		Object value = section.get(key);
		if (!(value instanceof Boolean flag))
		{
			warn(path + "." + key, value, false);
			return false;
		}
		return flag;
	}

	/** A mood band (angry, displeased, neutral, pleased, exalted), or null when missing or unknown (warned about) */
	private GodMood conditionMood(ConfigurationSection section, String path, String key)
	{
		if (!section.isSet(key))
		{
			return null;
		}
		Object value = section.get(key);
		if (value instanceof String text)
		{
			for (GodMood mood : GodMood.values())
			{
				if (mood.name().equalsIgnoreCase(text.trim()))
				{
					return mood;
				}
			}
		}
		this.logger.warning("Invalid mood '" + value + "' for " + path + "." + key + " in config.yml (angry, displeased, neutral, pleased or exalted); it doesn't count.");
		return null;
	}

	private Settings.Devotion loadDevotion()
	{
		Settings.DevotionPoints points = new Settings.DevotionPoints(
				atLeast("devotion.points.prayer", 0),
				atLeast("devotion.points.sacrifice", 0),
				atLeast("devotion.points.quest", 0),
				atLeast("devotion.points.convert", 0),
				atLeast("devotion.points.holy-food", 0),
				atLeast("devotion.points.unholy-mob", 0));
		// Each level needs at least one point, and never fewer than the level below
		Map<DevotionLevel, Integer> levels = new EnumMap<>(DevotionLevel.class);
		int below = 0;
		for (DevotionLevel level : DevotionLevel.values())
		{
			int needed = level == DevotionLevel.FOLLOWER ? 0 : atLeast("devotion.levels." + level.key(), below + 1);
			levels.put(level, needed);
			below = needed;
		}
		Settings.DevotionUnlocks unlocks = new Settings.DevotionUnlocks(
				devotionLevel("devotion.unlocks.prayfor-discount"),
				devotionLevel("devotion.unlocks.holy-artifacts"),
				devotionLevel("devotion.unlocks.more-blessings"),
				devotionLevel("devotion.unlocks.praise"),
				devotionLevel("devotion.unlocks.title"),
				devotionLevel("devotion.unlocks.priesthood"));
		return new Settings.Devotion(
				bool("devotion.enabled"),
				points,
				Collections.unmodifiableMap(levels),
				atLeast("devotion.commandment-cooldown-minutes", 0),
				atLeast("devotion.fade-after-days", 0),
				between("devotion.fade-percent-per-day", 0, 100),
				between("devotion.keep-level-percent", 0, 100),
				between("devotion.kept-on-changing-god-percent", 0, 100),
				unlocks,
				between("devotion.prayfor-discount-percent", 0, 100),
				between("devotion.blessing-chance-multiplier", 1, 10),
				bool("devotion.chat-title"));
	}

	/** A devotion level (follower, devotee, zealot, saint); an unknown one is warned about and the default used */
	private DevotionLevel devotionLevel(String path)
	{
		Object value = this.config.get(path);
		DevotionLevel level = value instanceof String text ? DevotionLevel.byKey(text) : null;
		if (level != null)
		{
			return level;
		}
		DevotionLevel fallback = DevotionLevel.byKey(defaults().getString(path, "follower"));
		fallback = fallback != null ? fallback : DevotionLevel.FOLLOWER;
		warn(path, value, fallback.key());
		return fallback;
	}

	/** A rank (spirit, recognized, descended, exalted, greater); an unknown one is warned about and the default used */
	private GodRank rank(String path)
	{
		Object value = this.config.get(path);
		GodRank rank = value instanceof String text ? GodRank.byKey(text) : null;
		if (rank != null)
		{
			return rank;
		}
		GodRank fallback = GodRank.byKey(defaults().getString(path, "spirit"));
		fallback = fallback != null ? fallback : GodRank.SPIRIT;
		warn(path, value, fallback.key());
		return fallback;
	}

	private Settings.General loadGeneral()
	{
		String language = string("general.language").trim();
		if (language.isEmpty())
		{
			language = defaults().getString("general.language", "english");
			warn("general.language", "", language);
		}

		List<String> worlds = new ArrayList<>();
		for (String world : this.config.getStringList("general.worlds"))
		{
			if (!world.isBlank() && !worlds.contains(world))
			{
				worlds.add(world);
			}
		}
		this.logger.info("Gods is active in " + (worlds.isEmpty() ? "every world" : "the worlds " + String.join(", ", worlds)) + ".");

		return new Settings.General(bool("general.debug"), language, string("general.server-name"), List.copyOf(worlds));
	}

	private Settings.GodSettings loadGods()
	{
		boolean whitelist = bool("gods.whitelist");
		boolean blacklist = bool("gods.blacklist");
		if (whitelist && blacklist)
		{
			this.logger.warning("gods.whitelist and gods.blacklist are both on in config.yml; only the whitelist is used.");
			blacklist = false;
		}

		return new Settings.GodSettings(
				bool("gods.broadcast-new-gods"),
				bool("gods.private-by-default"),
				bool("gods.titles"),
				atLeast("gods.verbosity", 0.0D),
				atLeast("gods.mood-falloff", 0.0D),
				whitelist,
				blacklist);
	}

	private Settings.Altars loadAltars()
	{
		String path = "altars.block-types";
		Map<GodType, List<Material>> blockTypes = new EnumMap<>(GodType.class);
		ConfigurationSection section = section(path);
		if (section != null)
		{
			for (String key : section.getKeys(false))
			{
				GodType godType = godType(key);
				if (godType == null)
				{
					this.logger.warning("Unknown god type '" + key + "' in " + path + " in config.yml, ignoring it.");
					continue;
				}
				List<Material> blocks = new ArrayList<>();
				for (String name : section.getStringList(key))
				{
					Material material = material(name);
					if (material == null)
					{
						this.logger.warning("Unknown block '" + name + "' in " + path + "." + key + " in config.yml, ignoring it.");
						continue;
					}
					if (!blocks.contains(material))
					{
						blocks.add(material);
					}
				}
				blockTypes.put(godType, List.copyOf(blocks));
			}
		}
		return Settings.Altars.of(blockTypes, bool("altars.require-claim"));
	}

	/** all, main or off (YAML reads a bare off as false and on as true) */
	private Settings.FloatingText floatingText(String path)
	{
		Object value = this.config.get(path);
		if (value instanceof Boolean on)
		{
			return on ? Settings.FloatingText.ALL : Settings.FloatingText.OFF;
		}
		if (value instanceof String text)
		{
			for (Settings.FloatingText mode : Settings.FloatingText.values())
			{
				if (mode.name().equalsIgnoreCase(text.trim()))
				{
					return mode;
				}
			}
		}
		Settings.FloatingText fallback = Settings.FloatingText.valueOf(defaults().getString(path, "all").toUpperCase(Locale.ROOT));
		warn(path, value, fallback.name().toLowerCase(Locale.ROOT));
		return fallback;
	}

	/** auto, builtin, citizens, fancynpcs or off (YAML reads a bare off as false) */
	private Settings.NpcProvider npcProvider(String path)
	{
		Object value = this.config.get(path);
		if (value instanceof Boolean on)
		{
			return on ? Settings.NpcProvider.AUTO : Settings.NpcProvider.OFF;
		}
		if (value instanceof String text)
		{
			for (Settings.NpcProvider provider : Settings.NpcProvider.values())
			{
				if (provider.name().equalsIgnoreCase(text.trim()))
				{
					return provider;
				}
			}
		}
		Settings.NpcProvider fallback = Settings.NpcProvider.valueOf(defaults().getString(path, "auto").toUpperCase(Locale.ROOT));
		warn(path, value, fallback.name().toLowerCase(Locale.ROOT));
		return fallback;
	}

	private Settings.Quests loadQuests()
	{
		return new Settings.Quests(
				bool("quests.enabled"),
				atLeast("quests.min-minutes-between-quests", 0),
				atLeast("quests.required-believers", 0),
				bool("quests.types.slay"),
				bool("quests.types.sacrifice"),
				bool("quests.types.pilgrimage"),
				bool("quests.types.give-items"),
				rewardValues());
	}

	/** Quest reward values. When none is valid, the bundled ones are used, as Gods always did. */
	private Map<Material, Integer> rewardValues()
	{
		String path = "quests.reward-values";
		Map<Material, Integer> values = rewardValues(section(path), path);
		if (values.isEmpty() && this.config.isSet(path))
		{
			this.logger.warning(path + " in config.yml has no valid entries, using the default reward values.");
			values = rewardValues(defaults().getConfigurationSection(path), path);
		}
		return Collections.unmodifiableMap(values);
	}

	private Map<Material, Integer> rewardValues(ConfigurationSection section, String path)
	{
		Map<Material, Integer> values = new LinkedHashMap<>();
		if (section == null)
		{
			return values;
		}
		for (String key : section.getKeys(false))
		{
			Material material = material(key);
			if (material == null)
			{
				this.logger.warning("Unknown item '" + key + "' in " + path + " in config.yml, ignoring it.");
				continue;
			}
			Object value = section.get(key);
			if (!(value instanceof Number number))
			{
				this.logger.warning("Invalid value '" + value + "' for " + path + "." + key + " in config.yml, ignoring it.");
				continue;
			}
			int worth = number.intValue();
			if (worth < 0)
			{
				warn(path + "." + key, worth, 0);
				worth = 0;
			}
			values.put(material, worth);
		}
		return values;
	}

	private Set<Material> materials(String path)
	{
		Set<Material> materials = new HashSet<>();
		for (String name : this.config.getStringList(path))
		{
			Material material = material(name);
			if (material == null)
			{
				this.logger.warning("Unknown block '" + name + "' in " + path + " in config.yml, ignoring it.");
				continue;
			}
			materials.add(material);
		}
		return Set.copyOf(materials);
	}

	/** Accepts Minecraft ids (smooth_stone, minecraft:smooth_stone) and Bukkit names (SMOOTH_STONE). */
	static Material material(String name)
	{
		return name == null || name.isBlank() ? null : Material.matchMaterial(name.trim());
	}

	/** Accepts god type names in any case (frost, FROST). */
	static GodType godType(String name)
	{
		try
		{
			return GodType.valueOf(name.trim().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}

	/**
	 * The section at path in config.yml, or the bundled one when the file doesn't have it at all. Bukkit's own
	 * getConfigurationSection would return an empty section in that case. A section the file has is used as it is:
	 * entries the admin removed are not filled in from the bundled one.
	 */
	private ConfigurationSection section(String path)
	{
		if (this.config.isSet(path))
		{
			return this.config.getConfigurationSection(path);
		}
		return defaults().getConfigurationSection(path);
	}

	private Configuration defaults()
	{
		Configuration defaults = this.config.getDefaults();
		if (defaults == null)
		{
			throw new IllegalStateException("config.yml has no defaults; set the bundled config.yml as its defaults");
		}
		return defaults;
	}

	private boolean bool(String path)
	{
		Object value = this.config.get(path);
		if (value instanceof Boolean b)
		{
			return b;
		}
		boolean fallback = defaults().getBoolean(path);
		warn(path, value, fallback);
		return fallback;
	}

	private String string(String path)
	{
		Object value = this.config.get(path);
		if (value instanceof String || value instanceof Number || value instanceof Boolean)
		{
			return value.toString();
		}
		String fallback = defaults().getString(path, "");
		if (value != null)
		{
			warn(path, value, fallback);
		}
		return fallback;
	}

	private int atLeast(String path, int min)
	{
		Object value = this.config.get(path);
		int number;
		if (value instanceof Number n)
		{
			number = n.intValue();
		}
		else
		{
			number = defaults().getInt(path);
			warn(path, value, number);
		}
		if (number < min)
		{
			warn(path, number, min);
			return min;
		}
		return number;
	}

	private int between(String path, int min, int max)
	{
		int number = atLeast(path, min);
		if (number > max)
		{
			warn(path, number, max);
			return max;
		}
		return number;
	}

	private double atLeast(String path, double min)
	{
		Object value = this.config.get(path);
		double number;
		if (value instanceof Number n)
		{
			number = n.doubleValue();
		}
		else
		{
			number = defaults().getDouble(path);
			warn(path, value, number);
		}
		if (number < min)
		{
			warn(path, number, min);
			return min;
		}
		return number;
	}

	private void warn(String path, Object value, Object used)
	{
		this.logger.warning("Invalid value '" + value + "' for " + path + " in config.yml, using " + used + ".");
	}
}
