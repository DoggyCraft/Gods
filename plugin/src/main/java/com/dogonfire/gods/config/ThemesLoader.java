package com.dogonfire.gods.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import com.dogonfire.gods.integration.npc.SkinTextures;
import com.dogonfire.gods.model.GodRank;
import com.dogonfire.gods.model.GodType;
import com.dogonfire.gods.model.NpcSkin;

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
 * <p>
 * The {@code effects} section is merged the same way: {@code symbol}, and per mood (pleased, neutral, angry) the lists
 * {@code particles} (particle ids), {@code sounds} (sound ids, checked against the server's sounds) and {@code colors}
 * (dye colors or #RRGGBB).
 * <p>
 * {@code npc.skin} holds a signed texture per gender ({@code god}, {@code goddess}, {@code deity}); the admin's file
 * replaces the entries it lists ({@link #readNpc}). Player names are not taken.
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

	/**
	 * The NPC section: {@code npc.skin}, a section with a signed texture ({@code value} and {@code signature}) per
	 * gender ({@link #SKIN_ENTRIES})
	 */
	public static final String			NPC				= "npc";
	public static final String			SKIN			= "skin";
	public static final List<String>	SKIN_KEYS		= List.of("value", "signature");
	/** The entries of {@code npc.skin}: god (Male, and None), goddess (Female), deity (NonBinary) */
	public static final String			SKIN_GOD		= "god";
	public static final String			SKIN_GODDESS	= "goddess";
	public static final String			SKIN_DEITY		= "deity";
	public static final List<String>	SKIN_ENTRIES	= List.of(SKIN_GOD, SKIN_GODDESS, SKIN_DEITY);
	/** What a skin in themes.yml must be, for the warnings */
	public static final String			SKIN_HINT		= "needs the value and signature of a signed texture, e.g. from mineskin.org, or use /g godskin in game";

	/** The names of the ranks: {@code ranks.<rank>} ({@link GodRank#key()}), each a text */
	public static final String			RANKS			= "ranks";

	public static final String			EFFECTS			= "effects";
	public static final String			SYMBOL			= "symbol";
	public static final List<String>	EFFECT_KINDS	= List.of("particles", "sounds", "colors");
	/** The effect lists, as paths: effects.pleased.particles, ... */
	public static final List<String>	EFFECT_KEYS		= effectKeys();

	private static final Set<String>	MOB_KEYS		= Set.of(HOLY_MOBS, UNHOLY_MOBS, SLAY_TARGETS);

	private static List<String> effectKeys()
	{
		List<String> keys = new ArrayList<>();
		for (Theme.EffectMood mood : Theme.EffectMood.values())
		{
			for (String kind : EFFECT_KINDS)
			{
				keys.add(EFFECTS + "." + mood.key() + "." + kind);
			}
		}
		return List.copyOf(keys);
	}

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

		/** The particle with this (normalized) name or id on this server, or null */
		default Particle particle(String name)
		{
			for (Particle particle : Particle.values())
			{
				if (particle.name().equals(name) || particle.getKey().getKey().toUpperCase(Locale.ROOT).equals(name))
				{
					return particle;
				}
			}
			return null;
		}

		/**
		 * The full id of the sound with this id ({@code block.amethyst_block.chime} or
		 * {@code minecraft:block.amethyst_block.chime}) if this server has it, or null. Without a server only the
		 * spelling is checked.
		 */
		default String sound(String id)
		{
			NamespacedKey key = soundKey(id);
			return key != null ? key.toString() : null;
		}
	}

	/** A sound id as a key, or null if it can't be one */
	static NamespacedKey soundKey(String id)
	{
		String trimmed = id.trim().toLowerCase(Locale.ROOT);
		return trimmed.isEmpty() ? null : NamespacedKey.fromString(trimmed);
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

			@Override
			public String sound(String id)
			{
				NamespacedKey key = soundKey(id);
				return key != null && Registry.SOUNDS.get(key) != null ? key.toString() : null;
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

	/**
	 * One section of themes.yml before its names are resolved: the lists by key (the effect lists by their path, like
	 * {@code effects.pleased.particles}), the sign settings, the effects' symbol and the NPC skins by entry
	 * ({@link #SKIN_ENTRIES}; only valid signed textures; a section's skins don't take the default section's, that
	 * fallback is {@code GodNpcRules.skin}'s)
	 */
	public record RawTheme(Map<String, List<String>> lists, Map<String, String> sign, Map<String, String> symbol, Map<String, RawSkin> npc, Map<String, String> ranks)
	{
	}

	/** A signed texture in themes.yml: its base64 value and Mojang's signature */
	public record RawSkin(String value, String signature)
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
		RawTheme defaults = apply(read(section(bundled, DEFAULT), DEFAULT, logger), section(overrides, DEFAULT), null, DEFAULT, logger);
		merged.put(DEFAULT, defaults);
		for (GodType type : GodType.values())
		{
			String name = sectionName(type);
			RawTheme own = apply(read(section(bundled, name), name, logger), section(overrides, name), defaults, name, logger);
			Map<String, List<String>> lists = new LinkedHashMap<>(defaults.lists());
			lists.putAll(own.lists());
			Map<String, String> sign = new LinkedHashMap<>(defaults.sign());
			sign.putAll(own.sign());
			Map<String, String> symbol = new LinkedHashMap<>(defaults.symbol());
			symbol.putAll(own.symbol());
			Map<String, String> ranks = new LinkedHashMap<>(defaults.ranks());
			ranks.putAll(own.ranks());
			merged.put(name, new RawTheme(lists, sign, symbol, own.npc(), ranks));
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

	private static RawTheme read(ConfigurationSection section, String name, Logger logger)
	{
		Map<String, List<String>> lists = new LinkedHashMap<>();
		Map<String, String> sign = new LinkedHashMap<>();
		Map<String, String> symbol = new LinkedHashMap<>();
		if (section != null)
		{
			for (String key : LIST_KEYS)
			{
				if (section.isList(key))
				{
					lists.put(key, List.copyOf(section.getStringList(key)));
				}
			}
			for (String key : EFFECT_KEYS)
			{
				if (section.isList(key))
				{
					lists.put(key, List.copyOf(section.getStringList(key)));
				}
			}
			readSign(section, sign);
			readSymbol(section, symbol);
		}
		Map<String, String> ranks = new LinkedHashMap<>();
		if (section != null)
		{
			readRanks(section, ranks, name, logger);
		}
		return new RawTheme(lists, sign, symbol, section != null ? readNpc(section, name, logger, Map.of()) : Map.of(), ranks);
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

	/**
	 * The {@code ranks} section: a name per rank ({@code spirit}, {@code recognized}, ...). An unknown rank or a name
	 * that isn't text is warned about and skipped.
	 */
	private static void readRanks(ConfigurationSection section, Map<String, String> ranks, String name, Logger logger)
	{
		if (!section.isSet(RANKS))
		{
			return;
		}
		ConfigurationSection names = section.getConfigurationSection(RANKS);
		if (names == null)
		{
			logger.warning("themes.yml " + name + ".ranks must be a section with a name per rank (spirit, recognized, descended, exalted, greater), ignoring it.");
			return;
		}
		for (String key : names.getKeys(false))
		{
			GodRank rank = GodRank.byKey(key);
			Object value = names.get(key);
			if (rank == null)
			{
				logger.warning("Unknown rank '" + key + "' in themes.yml " + name + ".ranks (spirit, recognized, descended, exalted, greater), ignoring it.");
			}
			else if (!(value instanceof String text) || text.isBlank())
			{
				logger.warning("themes.yml " + name + ".ranks." + key + " must be a name, ignoring it.");
			}
			else
			{
				ranks.put(rank.key(), text.trim());
			}
		}
	}

	private static void readSymbol(ConfigurationSection section, Map<String, String> symbol)
	{
		String path = EFFECTS + "." + SYMBOL;
		if (section.isSet(path) && !section.isConfigurationSection(path))
		{
			symbol.put(SYMBOL, String.valueOf(section.get(path)));
		}
	}

	/**
	 * The {@code npc} section of a theme over {@code base} (the skins so far): {@code skin:} with {@code god},
	 * {@code goddess} and/or {@code deity}, each a {@code value} and {@code signature} (a texture Mojang signed),
	 * replaces those entries; {@code skin: ''} removes them all, an entry set to '' that one. A player name, a texture
	 * without a signature, a value that isn't a texture or a texture without a gender is warned about
	 * ({@link #SKIN_HINT}) and ignored.
	 *
	 * @param name the section's name, for warnings
	 */
	public static Map<String, RawSkin> readNpc(ConfigurationSection section, String name, Logger logger, Map<String, RawSkin> base)
	{
		if (!section.isSet(NPC))
		{
			return base;
		}
		if (!section.isConfigurationSection(NPC))
		{
			warn(logger, "themes.yml " + name + ".npc must be a section (skin), ignoring it.");
			return base;
		}
		ConfigurationSection npcSection = section.getConfigurationSection(NPC);
		for (String key : npcSection.getKeys(false))
		{
			if (!key.equals(SKIN))
			{
				warn(logger, "Unknown key '" + key + "' in themes.yml " + name + ".npc (skin), ignoring it.");
			}
		}
		String path = name + ".npc.skin";
		if (!npcSection.isSet(SKIN))
		{
			return base;
		}
		if (!npcSection.isConfigurationSection(SKIN))
		{
			if (String.valueOf(npcSection.get(SKIN)).isBlank())
			{
				return Map.of();
			}
			nameWarning(logger, path, npcSection.get(SKIN));
			return base;
		}
		ConfigurationSection skin = npcSection.getConfigurationSection(SKIN);
		if (skin.isSet("value") || skin.isSet("signature") || skin.isSet("name"))
		{
			warn(logger, "themes.yml " + path + " needs a skin per gender (god, goddess, deity), each with a value and signature; ignoring it.");
			return base;
		}
		Map<String, RawSkin> skins = new LinkedHashMap<>(base);
		for (String key : skin.getKeys(false))
		{
			if (!SKIN_ENTRIES.contains(key))
			{
				warn(logger, "Unknown key '" + key + "' in themes.yml " + path + " (god, goddess, deity), ignoring it.");
			}
			else if (skin.isConfigurationSection(key))
			{
				RawSkin entry = readSkin(skin.getConfigurationSection(key), path + "." + key, logger);
				if (entry != null)
				{
					skins.put(key, entry);
				}
			}
			else if (String.valueOf(skin.get(key)).isBlank())
			{
				skins.remove(key);
			}
			else
			{
				nameWarning(logger, path + "." + key, skin.get(key));
			}
		}
		return skins;
	}

	/** One signed texture (value and signature), or null (warned about) if it isn't one */
	private static RawSkin readSkin(ConfigurationSection skin, String path, Logger logger)
	{
		for (String key : skin.getKeys(false))
		{
			if (key.equals("name"))
			{
				nameWarning(logger, path, skin.get(key));
				return null;
			}
			if (!SKIN_KEYS.contains(key))
			{
				warn(logger, "Unknown key '" + key + "' in themes.yml " + path + " (value, signature), ignoring it.");
			}
		}
		String value = skin.isSet("value") ? String.valueOf(skin.get("value")).trim() : "";
		String signature = skin.isSet("signature") ? String.valueOf(skin.get("signature")).trim() : "";
		if (value.isEmpty() || signature.isEmpty())
		{
			warn(logger, "themes.yml " + path + " has no " + (value.isEmpty() ? "value" : "signature") + ": a skin " + SKIN_HINT + ". Ignoring it.");
			return null;
		}
		if (SkinTextures.decode(value) == null)
		{
			warn(logger, "themes.yml " + path + ".value is not a skin texture: a skin " + SKIN_HINT + ". Ignoring it.");
			return null;
		}
		return new RawSkin(value, signature);
	}

	private static void nameWarning(Logger logger, String path, Object name)
	{
		warn(logger, "themes.yml " + path + " is a player name ('" + name + "'); a skin " + SKIN_HINT + ". Ignoring it.");
	}

	private static void warn(Logger logger, String message)
	{
		if (logger != null)
		{
			logger.warning(message);
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
		Map<String, String> symbol = new LinkedHashMap<>(base.symbol());
		Map<String, RawSkin> npc = base.npc();
		Map<String, String> ranks = new LinkedHashMap<>(base.ranks());
		for (String key : override.getKeys(false))
		{
			if (key.equals(RANKS))
			{
				readRanks(override, ranks, name, logger);
				continue;
			}
			if (key.equals(NPC))
			{
				// Replaces the gender entries it has
				npc = readNpc(override, name, logger, npc);
				continue;
			}
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
			if (key.equals(EFFECTS))
			{
				applyEffects(override, lists, symbol, fallback, name, logger);
				continue;
			}
			if (!LIST_KEYS.contains(key))
			{
				logger.warning("Unknown key '" + key + "' in themes.yml " + name + ", ignoring it.");
				continue;
			}
			applyList(override, key, lists, fallback, name, logger);
		}
		return new RawTheme(lists, sign, symbol, npc, ranks);
	}

	/** The admin's changes to the effects: {@code symbol}, and a section per mood with the effect lists */
	private static void applyEffects(ConfigurationSection override, Map<String, List<String>> lists, Map<String, String> symbol, RawTheme fallback, String name, Logger logger)
	{
		if (!override.isConfigurationSection(EFFECTS))
		{
			logger.warning("themes.yml " + name + ".effects must be a section (symbol, pleased, neutral, angry), ignoring it.");
			return;
		}
		for (String key : override.getConfigurationSection(EFFECTS).getKeys(false))
		{
			String path = EFFECTS + "." + key;
			if (key.equals(SYMBOL))
			{
				readSymbol(override, symbol);
				continue;
			}
			if (!override.isConfigurationSection(path) || Arrays.stream(Theme.EffectMood.values()).noneMatch(mood -> mood.key().equals(key)))
			{
				logger.warning("Unknown key '" + key + "' in themes.yml " + name + ".effects (symbol, pleased, neutral, angry), ignoring it.");
				continue;
			}
			for (String kind : override.getConfigurationSection(path).getKeys(false))
			{
				if (!EFFECT_KINDS.contains(kind))
				{
					logger.warning("Unknown key '" + kind + "' in themes.yml " + name + "." + path + " (particles, sounds, colors), ignoring it.");
					continue;
				}
				applyList(override, path + "." + kind, lists, fallback, name, logger);
			}
		}
	}

	/** The admin's change to one list (by its path): a list replaces it, add: and remove: change it */
	private static void applyList(ConfigurationSection override, String key, Map<String, List<String>> lists, RawTheme fallback, String name, Logger logger)
	{
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
						glowing(section, sign.get("glowing"))),
				effects(section, raw, bundledNames),
				npcSkins(raw.npc()),
				rankNames(raw.ranks()));
	}

	/** The rank names of a theme: its own, else the default section's, else the rank's key with a capital */
	private static Theme.RankNames rankNames(Map<String, String> ranks)
	{
		Map<GodRank, String> names = new java.util.EnumMap<>(GodRank.class);
		for (GodRank rank : GodRank.values())
		{
			String name = ranks.get(rank.key());
			names.put(rank, name != null ? name : Theme.RankNames.fallback(rank));
		}
		return new Theme.RankNames(java.util.Collections.unmodifiableMap(names));
	}

	/** The NPC skins of a theme (checked when read) */
	private static Theme.NpcSkins npcSkins(Map<String, RawSkin> npc)
	{
		return npc.isEmpty() ? Theme.NpcSkins.NONE
				: new Theme.NpcSkins(npcSkin(npc.get(SKIN_GOD)), npcSkin(npc.get(SKIN_GODDESS)), npcSkin(npc.get(SKIN_DEITY)));
	}

	private static NpcSkin npcSkin(RawSkin raw)
	{
		return raw != null ? new NpcSkin(null, raw.value(), raw.signature()) : null;
	}

	private Theme.Effects effects(String section, RawTheme raw, Set<String> bundledNames)
	{
		Map<Theme.EffectMood, Theme.EffectStyle> styles = new EnumMap<>(Theme.EffectMood.class);
		for (Theme.EffectMood mood : Theme.EffectMood.values())
		{
			String path = EFFECTS + "." + mood.key() + ".";
			styles.put(mood, new Theme.EffectStyle(particles(section, path + "particles", raw, bundledNames), sounds(section, path + "sounds", raw), colors(section, path + "colors", raw)));
		}
		return new Theme.Effects(raw.symbol().getOrDefault(SYMBOL, "").trim(), styles.get(Theme.EffectMood.PLEASED), styles.get(Theme.EffectMood.NEUTRAL), styles.get(Theme.EffectMood.ANGRY));
	}

	/** The kinds of particle data Gods can fill in: none, a color, dust (a color and a size), a color change, a number */
	public static boolean isShowable(Particle particle)
	{
		Class<?> data = particle.getDataType();
		return data == Void.class || data == Color.class || data == Particle.DustOptions.class || data == Particle.DustTransition.class || data == Float.class || data == Integer.class;
	}

	private List<Particle> particles(String section, String key, RawTheme raw, Set<String> bundledNames)
	{
		List<Particle> particles = new ArrayList<>();
		for (String name : raw.lists().getOrDefault(key, List.of()))
		{
			String normalized = normalize(name);
			Particle particle = this.names.particle(normalized);
			if (particle == null)
			{
				missing(section, key, name, normalized, bundledNames);
			}
			else if (!isShowable(particle))
			{
				this.logger.warning("The particle '" + name + "' in themes.yml " + section + "." + key + " needs a block or an item, ignoring it.");
			}
			else if (!particles.contains(particle))
			{
				particles.add(particle);
			}
		}
		return List.copyOf(particles);
	}

	private List<String> sounds(String section, String key, RawTheme raw)
	{
		List<String> sounds = new ArrayList<>();
		for (String id : raw.lists().getOrDefault(key, List.of()))
		{
			String sound = this.names.sound(id);
			if (sound == null)
			{
				// Sounds are many and change between versions; a missing one is only worth a debug message
				this.debug.accept("This server has no sound '" + id + "' (themes.yml " + section + "." + key + "), skipping it.");
			}
			else if (!sounds.contains(sound))
			{
				sounds.add(sound);
			}
		}
		return List.copyOf(sounds);
	}

	private List<Color> colors(String section, String key, RawTheme raw)
	{
		List<Color> colors = new ArrayList<>();
		for (String name : raw.lists().getOrDefault(key, List.of()))
		{
			Color color = color(name);
			if (color == null)
			{
				this.logger.warning("Unknown color '" + name + "' in themes.yml " + section + "." + key + " (a dye color like light_blue, or #RRGGBB), ignoring it.");
			}
			else if (!colors.contains(color))
			{
				colors.add(color);
			}
		}
		return List.copyOf(colors);
	}

	/** A color: a dye color's name (light_blue) or #RRGGBB; null if it is neither */
	public static Color color(String name)
	{
		String trimmed = name.trim();
		if (trimmed.startsWith("#"))
		{
			if (trimmed.length() != 7)
			{
				return null;
			}
			try
			{
				return Color.fromRGB(Integer.parseInt(trimmed.substring(1), 16));
			}
			catch (NumberFormatException ex)
			{
				return null;
			}
		}
		try
		{
			return DyeColor.valueOf(normalize(trimmed)).getColor();
		}
		catch (IllegalArgumentException ex)
		{
			return null;
		}
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
			this.logger.warning("Unknown " + (MOB_KEYS.contains(key) ? "mob" : key.equals(ALTAR_BLOCKS) ? "block" : key.endsWith(".particles") ? "particle" : "item") + " '" + name + "' in themes.yml " + section + "." + key + ", ignoring it.");
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
