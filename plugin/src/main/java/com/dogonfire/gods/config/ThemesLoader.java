package com.dogonfire.gods.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import com.dogonfire.gods.model.GodType;

/**
 * Reads the god types' themes: the themes.yml bundled in the jar (Gods' own, never written to disk) with the admin's
 * plugins/Gods/themes.yml merged over it, key by key. In the admin's file a list replaces Gods' list, and a section
 * with {@code add:} and/or {@code remove:} changes it. A god type's section that leaves a key out gets the
 * {@code default} section's (after the admin's changes to {@code default}).
 * <p>
 * Names are Minecraft ids (polar_bear, minecraft:cooked_cod, COOKED_COD) resolved on this server, so the bundled file
 * can name mobs and blocks newer than the oldest Minecraft version Gods runs on: a name this server doesn't have is
 * skipped with a debug message when Gods' own file has it or some other Minecraft version has it, and with a warning
 * otherwise (a typo).
 */
public final class ThemesLoader
{
	/** The section that fills the gaps of the god types' sections */
	public static final String			DEFAULT			= "default";

	public static final String			HOLY_MOBS		= "holy-mobs";
	public static final String			UNHOLY_MOBS		= "unholy-mobs";
	public static final String			HOLY_FOODS		= "holy-foods";
	public static final String			UNHOLY_FOODS	= "unholy-foods";
	public static final String			SACRIFICE_NEEDS	= "sacrifice-needs";
	public static final String			SLAY_TARGETS	= "slay-targets";
	public static final String			ALTAR_BLOCKS	= "altar-blocks";
	public static final String			SIGN			= "sign";

	public static final List<String>	LIST_KEYS		= List.of(HOLY_MOBS, UNHOLY_MOBS, HOLY_FOODS, UNHOLY_FOODS, SACRIFICE_NEEDS, SLAY_TARGETS, ALTAR_BLOCKS);
	public static final List<String>	SIGN_KEYS		= List.of("messages", "dye", "glowing");

	private static final Set<String>	MOB_KEYS		= Set.of(HOLY_MOBS, UNHOLY_MOBS, SLAY_TARGETS);

	/** What a name means on this server */
	public interface Names
	{
		/** The material with this (normalized) name on this server, or null */
		Material material(String name);

		/** The entity type with this (normalized) name on this server, or null */
		EntityType entityType(String name);

		/** True if some other Minecraft version Gods runs on has this (normalized) name */
		boolean existsInOtherVersions(String name);

		/** True if the material is a block (it needs the server's registries) */
		default boolean isBlock(Material material)
		{
			return material.isBlock();
		}

		/** True if the material is an item (it needs the server's registries) */
		default boolean isItem(Material material)
		{
			return material.isItem();
		}
	}

	/**
	 * The names of the running server. {@code otherVersions} are the names some Minecraft versions have and others
	 * don't (bundled as version-names.txt), so a name the server lacks is told apart from a typo.
	 */
	public static Names serverNames(Set<String> otherVersions)
	{
		return new Names()
		{
			@Override
			public Material material(String name)
			{
				return Material.getMaterial(name);
			}

			@Override
			public EntityType entityType(String name)
			{
				try
				{
					return EntityType.valueOf(name);
				}
				catch (IllegalArgumentException ex)
				{
					return null;
				}
			}

			@Override
			public boolean existsInOtherVersions(String name)
			{
				return otherVersions.contains(name);
			}
		};
	}

	/** Reads version-names.txt (one name per line, # comments) */
	public static Set<String> readVersionNames(Function<String, InputStream> resources) throws IOException
	{
		Set<String> names = new HashSet<>();
		try (InputStream in = resources.apply("version-names.txt"))
		{
			if (in == null)
			{
				return names;
			}
			for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n"))
			{
				String name = line.trim();
				if (!name.isEmpty() && !name.startsWith("#"))
				{
					names.add(normalize(name));
				}
			}
		}
		return names;
	}

	/** One section of themes.yml before its names are resolved: the lists by key, and the sign settings */
	public record RawTheme(Map<String, List<String>> lists, Map<String, String> sign)
	{
	}

	private final Names				names;
	private final Logger			logger;
	private final Consumer<String>	debug;

	public ThemesLoader(Names names, Logger logger, Consumer<String> debug)
	{
		this.names = names;
		this.logger = logger;
		this.debug = debug;
	}

	/**
	 * The themes from the bundled themes.yml and the admin's changes ({@code overrides}, may be empty), with the altar
	 * blocks of config.yml ({@code configuredAltars}) combined as {@link Themes#combineAltars} says.
	 */
	public Themes load(ConfigurationSection bundled, ConfigurationSection overrides, Settings.Altars configuredAltars)
	{
		Set<String> bundledNames = new HashSet<>();
		for (String key : bundled.getKeys(true))
		{
			if (bundled.isList(key))
			{
				bundled.getStringList(key).forEach(name -> bundledNames.add(normalize(name)));
			}
		}

		Map<String, RawTheme> raw = merge(bundled, overrides, this.logger);

		Theme defaults = resolve(DEFAULT, raw.get(DEFAULT), bundledNames);
		Map<GodType, Theme> types = new EnumMap<>(GodType.class);
		for (GodType type : GodType.values())
		{
			types.put(type, resolve(sectionName(type), raw.get(sectionName(type)), bundledNames));
		}
		return new Themes(defaults, java.util.Collections.unmodifiableMap(types), Themes.combineAltars(configuredAltars, types));
	}

	/** The section of a god type in themes.yml: its name in lower case */
	public static String sectionName(GodType type)
	{
		return type.name().toLowerCase(Locale.ROOT);
	}

	/** A name as the enums spell it: COOKED_COD for minecraft:cooked_cod, "cooked cod" or Cooked-Cod */
	public static String normalize(String name)
	{
		String normalized = name.trim();
		if (normalized.toLowerCase(Locale.ROOT).startsWith("minecraft:"))
		{
			normalized = normalized.substring("minecraft:".length());
		}
		return normalized.replace(' ', '_').replace('-', '_').toUpperCase(Locale.ROOT);
	}

	// ---------------------------------------------------------------------
	// Merging (names are not resolved yet)

	/**
	 * Merges the admin's changes over the bundled file: the {@code default} section and one section per god type (by
	 * {@link #sectionName}), each with every key that either file or the default has. Unknown sections and keys in
	 * the admin's file are logged and skipped.
	 */
	public static Map<String, RawTheme> merge(ConfigurationSection bundled, ConfigurationSection overrides, Logger logger)
	{
		Set<String> known = new HashSet<>();
		known.add(DEFAULT);
		for (GodType type : GodType.values())
		{
			known.add(sectionName(type));
		}
		if (overrides != null)
		{
			for (String section : overrides.getKeys(false))
			{
				if (!known.contains(section.toLowerCase(Locale.ROOT)))
				{
					logger.warning("Unknown god type '" + section + "' in themes.yml, ignoring it.");
				}
			}
		}

		Map<String, RawTheme> merged = new LinkedHashMap<>();
		RawTheme defaults = apply(read(section(bundled, DEFAULT)), section(overrides, DEFAULT), null, DEFAULT, logger);
		merged.put(DEFAULT, defaults);
		for (GodType type : GodType.values())
		{
			String name = sectionName(type);
			RawTheme own = apply(read(section(bundled, name)), section(overrides, name), defaults, name, logger);
			Map<String, List<String>> lists = new LinkedHashMap<>(defaults.lists());
			lists.putAll(own.lists());
			Map<String, String> sign = new LinkedHashMap<>(defaults.sign());
			sign.putAll(own.sign());
			merged.put(name, new RawTheme(lists, sign));
		}
		return merged;
	}

	/** A section by name, any case; null if there is none */
	private static ConfigurationSection section(ConfigurationSection parent, String name)
	{
		if (parent == null)
		{
			return null;
		}
		for (String key : parent.getKeys(false))
		{
			if (key.equalsIgnoreCase(name) && parent.isConfigurationSection(key))
			{
				return parent.getConfigurationSection(key);
			}
		}
		return null;
	}

	private static RawTheme read(ConfigurationSection section)
	{
		Map<String, List<String>> lists = new LinkedHashMap<>();
		Map<String, String> sign = new LinkedHashMap<>();
		if (section != null)
		{
			for (String key : LIST_KEYS)
			{
				if (section.isList(key))
				{
					lists.put(key, List.copyOf(section.getStringList(key)));
				}
			}
			readSign(section, sign);
		}
		return new RawTheme(lists, sign);
	}

	private static void readSign(ConfigurationSection section, Map<String, String> sign)
	{
		ConfigurationSection signSection = section.getConfigurationSection(SIGN);
		if (signSection != null)
		{
			for (String key : SIGN_KEYS)
			{
				if (signSection.isSet(key))
				{
					sign.put(key, String.valueOf(signSection.get(key)));
				}
			}
		}
	}

	/** The admin's changes to one section; a key the base lacks starts from the fallback's (the default section) */
	private static RawTheme apply(RawTheme base, ConfigurationSection override, RawTheme fallback, String name, Logger logger)
	{
		if (override == null)
		{
			return base;
		}
		Map<String, List<String>> lists = new LinkedHashMap<>(base.lists());
		Map<String, String> sign = new LinkedHashMap<>(base.sign());
		for (String key : override.getKeys(false))
		{
			if (key.equals(SIGN))
			{
				if (!override.isConfigurationSection(SIGN))
				{
					logger.warning("themes.yml " + name + ".sign must be a section (messages, dye, glowing), ignoring it.");
					continue;
				}
				for (String signKey : override.getConfigurationSection(SIGN).getKeys(false))
				{
					if (!SIGN_KEYS.contains(signKey))
					{
						logger.warning("Unknown key '" + signKey + "' in themes.yml " + name + ".sign, ignoring it.");
					}
				}
				readSign(override, sign);
				continue;
			}
			if (!LIST_KEYS.contains(key))
			{
				logger.warning("Unknown key '" + key + "' in themes.yml " + name + ", ignoring it.");
				continue;
			}
			if (override.isList(key))
			{
				lists.put(key, List.copyOf(override.getStringList(key)));
			}
			else if (override.isConfigurationSection(key))
			{
				List<String> start = lists.containsKey(key) ? lists.get(key) : (fallback != null ? fallback.lists().getOrDefault(key, List.of()) : List.of());
				lists.put(key, addAndRemove(start, override.getConfigurationSection(key), name + "." + key, logger));
			}
			else
			{
				logger.warning("themes.yml " + name + "." + key + " must be a list, or a section with add: and remove:, ignoring it.");
			}
		}
		return new RawTheme(lists, sign);
	}

	private static List<String> addAndRemove(List<String> start, ConfigurationSection change, String path, Logger logger)
	{
		for (String key : change.getKeys(false))
		{
			if (!key.equals("add") && !key.equals("remove"))
			{
				logger.warning("Unknown key '" + key + "' in themes.yml " + path + " (only add: and remove:), ignoring it.");
			}
		}
		List<String> result = new ArrayList<>(start);
		Set<String> removed = new HashSet<>();
		change.getStringList("remove").forEach(name -> removed.add(normalize(name)));
		result.removeIf(name -> removed.contains(normalize(name)));
		Set<String> present = new HashSet<>();
		result.forEach(name -> present.add(normalize(name)));
		for (String name : change.getStringList("add"))
		{
			if (present.add(normalize(name)))
			{
				result.add(name);
			}
		}
		return List.copyOf(result);
	}

	// ---------------------------------------------------------------------
	// Resolving names

	private Theme resolve(String section, RawTheme raw, Set<String> bundledNames)
	{
		Map<String, String> sign = raw.sign();
		return new Theme(
				mobs(section, HOLY_MOBS, raw, bundledNames),
				mobs(section, UNHOLY_MOBS, raw, bundledNames),
				materials(section, HOLY_FOODS, raw, bundledNames, false),
				materials(section, UNHOLY_FOODS, raw, bundledNames, false),
				materials(section, SACRIFICE_NEEDS, raw, bundledNames, false),
				mobs(section, SLAY_TARGETS, raw, bundledNames),
				materials(section, ALTAR_BLOCKS, raw, bundledNames, true),
				new Theme.SignStyle(
						sign.getOrDefault("messages", Theme.SignStyle.PLAIN.messages()).trim().toLowerCase(Locale.ROOT),
						dye(section, sign.get("dye")),
						glowing(section, sign.get("glowing"))));
	}

	private List<EntityType> mobs(String section, String key, RawTheme raw, Set<String> bundledNames)
	{
		List<EntityType> mobs = new ArrayList<>();
		for (String name : raw.lists().getOrDefault(key, List.of()))
		{
			String normalized = normalize(name);
			EntityType type = this.names.entityType(normalized);
			if (type == null)
			{
				missing(section, key, name, normalized, bundledNames);
			}
			else if (!type.isAlive())
			{
				this.logger.warning("'" + name + "' in themes.yml " + section + "." + key + " is not a mob, ignoring it.");
			}
			else if (!mobs.contains(type))
			{
				mobs.add(type);
			}
		}
		return List.copyOf(mobs);
	}

	private List<Material> materials(String section, String key, RawTheme raw, Set<String> bundledNames, boolean blocks)
	{
		List<Material> materials = new ArrayList<>();
		for (String name : raw.lists().getOrDefault(key, List.of()))
		{
			String normalized = normalize(name);
			Material material = this.names.material(normalized);
			if (material == null)
			{
				missing(section, key, name, normalized, bundledNames);
			}
			else if (blocks ? !this.names.isBlock(material) : !this.names.isItem(material))
			{
				this.logger.warning("'" + name + "' in themes.yml " + section + "." + key + " is not " + (blocks ? "a block" : "an item") + ", ignoring it.");
			}
			else if (!materials.contains(material))
			{
				materials.add(material);
			}
		}
		return List.copyOf(materials);
	}

	/** A name this server doesn't have: newer or older than the server (debug), or a typo (warning) */
	private void missing(String section, String key, String name, String normalized, Set<String> bundledNames)
	{
		if (bundledNames.contains(normalized) || this.names.existsInOtherVersions(normalized))
		{
			this.debug.accept("This server's Minecraft version has no '" + name + "' (themes.yml " + section + "." + key + "), skipping it.");
		}
		else
		{
			this.logger.warning("Unknown " + (MOB_KEYS.contains(key) ? "mob" : key.equals(ALTAR_BLOCKS) ? "block" : "item") + " '" + name + "' in themes.yml " + section + "." + key + ", ignoring it.");
		}
	}

	private DyeColor dye(String section, String name)
	{
		if (name == null)
		{
			return Theme.SignStyle.PLAIN.dye();
		}
		try
		{
			return DyeColor.valueOf(normalize(name));
		}
		catch (IllegalArgumentException ex)
		{
			this.logger.warning("Unknown dye color '" + name + "' in themes.yml " + section + ".sign.dye, using black.");
			return Theme.SignStyle.PLAIN.dye();
		}
	}

	private boolean glowing(String section, String value)
	{
		if (value == null)
		{
			return Theme.SignStyle.PLAIN.glowing();
		}
		if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false"))
		{
			this.logger.warning("themes.yml " + section + ".sign.glowing must be true or false, not '" + value + "'.");
			return Theme.SignStyle.PLAIN.glowing();
		}
		return Boolean.parseBoolean(value);
	}
}
