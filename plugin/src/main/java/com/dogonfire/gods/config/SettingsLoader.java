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

import com.dogonfire.gods.model.GodType;

/**
 * Reads {@link Settings} from config.yml. The config must have the bundled config.yml as its defaults (Bukkit's
 * {@code setDefaults}), so missing keys get the bundled value and the defaults live in one place. A value of the wrong
 * type is logged and replaced by the default; a number out of range is logged and clamped. Unknown materials and god
 * types are logged and skipped.
 */
public class SettingsLoader
{
	private final Configuration	config;
	private final Logger		logger;
	private final List<String>	serverWorlds;

	/**
	 * @param config       config.yml, with the bundled config.yml as its defaults
	 * @param serverWorlds the worlds to use when general.worlds is empty
	 */
	public SettingsLoader(Configuration config, Logger logger, List<String> serverWorlds)
	{
		this.config = config;
		this.logger = logger;
		this.serverWorlds = List.copyOf(serverWorlds);
	}

	public Settings load()
	{
		return new Settings(
				loadGeneral(),
				loadGods(),
				new Settings.Believers(
						bool("believers.leave-religion-on-death"),
						atLeast("believers.min-seconds-between-changing-god", 0),
						atLeast("believers.max-hours-without-prayer", 0)),
				new Settings.Priests(
						atLeast("priests.min-believers", 0),
						atLeast("priests.max-per-god", 0),
						atLeast("priests.max-hours-without-prayer", 0),
						bool("priests.only-priests-can-set-home"),
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
				new Settings.Chat(bool("chat.formatting")));
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
		if (worlds.isEmpty())
		{
			worlds.addAll(this.serverWorlds);
		}
		this.logger.info("Gods is active in " + (worlds.isEmpty() ? "no worlds" : "the worlds " + String.join(", ", worlds)) + ".");

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
		return Settings.Altars.of(blockTypes);
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
