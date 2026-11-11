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

	/** The NPC section: {@code npc.skin}, a player name or a section with value and signature */
	public static final String			NPC				= "npc";
	public static final String			SKIN			= "skin";
	public static final List<String>	SKIN_KEYS		= List.of("name", "value", "signature");

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
	 * {@code effects.pleased.particles}), the sign settings, the effects' symbol and the NPC skin ({@link #SKIN_KEYS}) (empty
	 * map: not set; a set npc section replaces the default's as a whole)
	 */
	public record RawTheme(Map<String, List<String>> lists, Map<String, String> sign, Map<String, String> symbol, Map<String, String> npc)
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
			Map<String, String> symbol = new LinkedHashMap<>(defaults.symbol());
			symbol.putAll(own.symbol());
			merged.put(name, new RawTheme(lists, sign, symbol, own.npc().isEmpty() ? defaults.npc() : own.npc()));
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
		return new RawTheme(lists, sign, symbol, section != null ? readNpc(section, null, null) : Map.of());
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

	private static void readSymbol(ConfigurationSection section, Map<String, String> symbol)
	{
		String path = EFFECTS + "." + SYMBOL;
		if (section.isSet(path) && !section.isConfigurationSection(path))
		{
			symbol.put(SYMBOL, String.valueOf(section.get(path)));
		}
	}

	/**
	 * The {@code npc} section of a theme: {@code skin: <player name>}, or {@code skin:} with {@code value} and
	 * {@code signature} (and maybe {@code name}). The map holds what is set ({@link #SKIN_KEYS}); a skin that is set
	 * but empty ({@code skin: ''}) is kept as an empty name, so it replaces the default's skin with none.
	 *
	 * @param logger null for Gods' own file (no warnings)
	 */
	public static Map<String, String> readNpc(ConfigurationSection section, String name, Logger logger)
	{
		Map<String, String> npc = new LinkedHashMap<>();
		if (!section.isSet(NPC))
		{
			return npc;
		}
		if (!section.isConfigurationSection(NPC))
		{
			warn(logger, "themes.yml " + name + ".npc must be a section (skin), ignoring it.");
			return npc;
		}
		ConfigurationSection npcSection = section.getConfigurationSection(NPC);
		for (String key : npcSection.getKeys(false))
		{
			if (!key.equals(SKIN))
			{
				warn(logger, "Unknown key '" + key + "' in themes.yml " + name + ".npc (skin), ignoring it.");
			}
		}
		if (npcSection.isConfigurationSection(SKIN))
		{
			ConfigurationSection skin = npcSection.getConfigurationSection(SKIN);
			for (String key : skin.getKeys(false))
			{
				if (SKIN_KEYS.contains(key))
				{
					npc.put(key, String.valueOf(skin.get(key)));
				}
				else
				{
					warn(logger, "Unknown key '" + key + "' in themes.yml " + name + ".npc.skin (name, value, signature), ignoring it.");
				}
			}
		}
		else if (npcSection.isSet(SKIN))
		{
			npc.put("name", String.valueOf(npcSection.get(SKIN)));
		}
		if (npc.isEmpty())
		{
			npc.put("name", "");
		}
		return npc;
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
		Map<String, String> npc = base.npc();
		for (String key : override.getKeys(false))
		{
			if (key.equals(NPC))
			{
				// Replaces the whole skin: a name doesn't mix with another skin's texture
				npc = readNpc(override, name, logger);
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
		return new RawTheme(lists, sign, symbol, npc);
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
				npcSkin(section, raw.npc()));
	}

	/**
	 * The NPC skin of a theme, or null for none; a texture value that isn't one is warned about and left out (a name
	 * is checked when it is resolved)
	 */
	private NpcSkin npcSkin(String section, Map<String, String> npc)
	{
		NpcSkin skin = new NpcSkin(npc.get("name"), npc.get("value"), npc.get("signature"));
		if (skin.hasTexture() && SkinTextures.decode(skin.value()) == null)
		{
			this.logger.warning("themes.yml " + section + ".npc.skin.value is not a skin texture, ignoring it.");
			skin = new NpcSkin(skin.name(), null, null);
		}
		return skin.isEmpty() ? null : skin;
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
