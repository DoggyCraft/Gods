package com.dogonfire.gods.message;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.StyleBuilderApplicable;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;

import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.model.GodMood;
import com.dogonfire.gods.model.GodType;

/**
 * The texts players see, from {@code lang/<language>/messages.yml} (and the gods' voices, {@link GodVoices}). They are
 * MiniMessage, rendered to legacy (§) text before they reach Bukkit, since Spigot has no Adventure of its own. Keys
 * missing from the server's file fall back to the bundled file. A message can be a list (one line per entry), and
 * {@code ''} sends nothing.
 * <p>
 * Every message knows {@code <server>} (the server name from config.yml) and {@code <hl>}, the gold highlight Gods
 * always used for names and numbers. Values that come from players (names, god names, descriptions, chat) must be
 * passed with {@link #text} or one of the helpers built on it, so they are shown as typed and never parsed as tags.
 * <p>
 * Thread-safe: a load replaces the texts as a whole, so the chat thread may render messages too.
 */
public final class Messages
{
	/** The layout of the message files this version writes; version 1 were the old lang/&lt;language&gt;_*.yml files */
	public static final int							MESSAGES_VERSION	= 2;
	public static final String						VERSION_KEY			= "messages-version";

	private static final MiniMessage				MINI_MESSAGE		= MiniMessage.miniMessage();
	/** §x§r§r§g§g§b§b hex colors are understood by both Spigot and Paper */
	private static final LegacyComponentSerializer	LEGACY				= LegacyComponentSerializer.builder()
			.character(LegacyComponentSerializer.SECTION_CHAR)
			.hexColors()
			.useUnusualXRepeatedCharacterHexFormat()
			.build();

	/** {@code <hl>}: gold and not bold, as the old $-placeholders were shown */
	static final TagResolver						HIGHLIGHT			= TagResolver.resolver("hl", Tag.styling(NamedTextColor.GOLD, TextDecoration.BOLD.withState(false)));
	/** {@code <hl>} where text has no colors (book pages) */
	static final TagResolver						NO_HIGHLIGHT		= TagResolver.resolver("hl", Tag.styling(new StyleBuilderApplicable[0]));

	private final LangFiles							files;
	private final Logger							log;
	private final GodVoices							voices;

	private volatile Map<String, List<String>>		messages			= Map.of();
	private volatile TagResolver					globals				= HIGHLIGHT;
	private volatile TagResolver					plainGlobals		= NO_HIGHLIGHT;

	/**
	 * @param langFolder {@code plugins/Gods/lang}
	 * @param resources  opens a file in the plugin jar (null if it isn't there), like {@code Plugin#getResource}
	 */
	public Messages(Path langFolder, Function<String, InputStream> resources, Logger log)
	{
		this.files = new LangFiles(langFolder, resources, log);
		this.log = log;
		this.voices = new GodVoices(this.files, log);
	}

	/** Reads the messages and voices of a language (config.yml's general.language), on enable and /g reload */
	public void load(String language, String serverName)
	{
		this.messages = Map.copyOf(this.files.read(language, "messages.yml"));
		this.globals = TagResolver.resolver(HIGHLIGHT, text("server", serverName));
		this.plainGlobals = TagResolver.resolver(NO_HIGHLIGHT, text("server", serverName));
		this.voices.load(language);
	}

	/** The gods' voices */
	public GodVoices voices()
	{
		return this.voices;
	}

	/** {@code <server>} and {@code <hl>} */
	TagResolver globals()
	{
		return this.globals;
	}

	/** Whether the key exists (in the server's or the bundled file) */
	public boolean has(String key)
	{
		return this.messages.containsKey(key);
	}

	/** The raw MiniMessage lines of a message; a placeholder line if the key is missing */
	public List<String> raw(String key)
	{
		List<String> raw = this.messages.get(key);
		if (raw == null)
		{
			this.log.warning("Missing message " + key);
			return List.of("<red>[Gods: missing message " + key + "]");
		}
		return raw;
	}

	/** The lines of a message. A list gives one line per entry; an empty message gives no lines. */
	public List<Component> lines(String key, TagResolver... tags)
	{
		List<String> raw = raw(key);
		TagResolver resolver = TagResolver.resolver(this.globals, TagResolver.resolver(tags));
		List<Component> lines = new ArrayList<>(raw.size());
		for (String line : raw)
		{
			lines.add(MINI_MESSAGE.deserialize(line, resolver));
		}
		return lines;
	}

	/** A message as one component (lines joined with newlines), for use inside other messages */
	public Component component(String key, TagResolver... tags)
	{
		return Component.join(JoinConfiguration.newlines(), lines(key, tags));
	}

	/** A message as legacy text (lines joined with newlines), for titles, item names, signs and the like */
	public String legacy(String key, TagResolver... tags)
	{
		return legacy(component(key, tags));
	}

	/** A message as legacy text lines, e.g. for item lore */
	public List<String> legacyLines(String key, TagResolver... tags)
	{
		return lines(key, tags).stream().map(Messages::legacy).toList();
	}

	public void send(CommandSender to, String key, TagResolver... tags)
	{
		for (Component line : lines(key, tags))
		{
			to.sendMessage(legacy(line));
		}
	}

	/** Sends a message to every player and the console, like {@code Server#broadcastMessage} */
	public void broadcast(Server server, String key, TagResolver... tags)
	{
		for (Component line : lines(key, tags))
		{
			server.broadcastMessage(legacy(line));
		}
	}

	/** Parses MiniMessage text with the global tags ({@code <server>}, {@code <hl>}) and the given ones */
	public Component parse(String miniMessage, TagResolver... tags)
	{
		return MINI_MESSAGE.deserialize(miniMessage, TagResolver.resolver(this.globals, TagResolver.resolver(tags)));
	}

	/** Like {@link #parse}, but {@code <hl>} doesn't color anything: for book pages */
	public Component parsePlain(String miniMessage, TagResolver... tags)
	{
		return MINI_MESSAGE.deserialize(miniMessage, TagResolver.resolver(this.plainGlobals, TagResolver.resolver(tags)));
	}

	// ---------------------------------------------------------------------
	// Names

	/** The name of an item, from {@code names.item.<MATERIAL>}; one made from the material when there is none */
	public Component itemName(Material material)
	{
		return name("names.item." + material.name(), readableName(material.name()));
	}

	/** The name of a mob, from {@code names.mob.<TYPE>}; one made from the type when there is none */
	public Component mobName(EntityType type)
	{
		return name("names.mob." + type.name(), readableName(type.name()));
	}

	/** A name made from a Minecraft id, for things messages.yml has no name for: COOKED_BEEF is "Cooked Beef" */
	public static String readableName(String id)
	{
		StringBuilder name = new StringBuilder();
		for (String word : id.toLowerCase(Locale.ROOT).split("[_ ]"))
		{
			if (word.isEmpty())
			{
				continue;
			}
			if (name.length() > 0)
			{
				name.append(' ');
			}
			name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return name.toString();
	}

	/** "God", "Goddess" */
	public Component genderName(GodGender gender)
	{
		return name("names.gender." + gender.name(), gender.name());
	}

	public Component moodName(GodMood mood)
	{
		return name("names.mood." + mood.name(), mood.name());
	}

	/** "Goddess of the Sun" and the like */
	public Component godTypeName(GodType type, GodGender gender)
	{
		if (!has("names.god-type." + type.name()))
		{
			return component("names.god-type.none", component("gender", genderName(gender)));
		}
		return component("names.god-type." + type.name(), component("gender", genderName(gender)));
	}

	private Component name(String key, String fallback)
	{
		List<String> raw = this.messages.get(key);
		if (raw == null || raw.isEmpty())
		{
			return Component.text(fallback);
		}
		return MINI_MESSAGE.deserialize(raw.getFirst(), this.globals);
	}

	// ---------------------------------------------------------------------
	// Rendering and tags

	public static String legacy(Component component)
	{
		return LEGACY.serialize(component);
	}

	/** Legacy text without its colors */
	public static String plain(Component component)
	{
		return ChatColor.stripColor(legacy(component));
	}

	/** {@code <name>} shows the value as typed: tags in it are not parsed */
	public static TagResolver text(String name, Object value)
	{
		return Placeholder.unparsed(name, String.valueOf(value));
	}

	/** {@code <name>} inserts a component */
	public static TagResolver component(String name, Component value)
	{
		return Placeholder.component(name, value);
	}

	/**
	 * {@code <name>} shows text that may hold § color codes, like a player's display name or an item's name, with
	 * those colors; MiniMessage tags in it are not parsed.
	 */
	public static TagResolver legacyText(String name, String legacy)
	{
		return Placeholder.component(name, LEGACY.deserialize(legacy == null ? "" : legacy));
	}

	/** {@code <name>...</name>} colors the text between in a {@link ChatColor} (e.g. a god type's color) */
	public static TagResolver color(String name, ChatColor color)
	{
		NamedTextColor textColor = NamedTextColor.NAMES.value(color.name().toLowerCase(Locale.ROOT));
		return TagResolver.resolver(name, Tag.styling(textColor != null ? textColor : NamedTextColor.WHITE));
	}

	/** {@code <player>} */
	public static TagResolver player(String name)
	{
		return text("player", name == null ? "" : name);
	}

	/** {@code <player>} as a display name, which may hold § colors */
	public static TagResolver displayName(String displayName)
	{
		return legacyText("player", displayName);
	}

	/** {@code <god>} */
	public static TagResolver god(String godName)
	{
		return text("god", godName == null ? "" : godName);
	}

	/** {@code <amount>} */
	public static TagResolver amount(long amount)
	{
		return text("amount", amount);
	}

	/** {@code <type>} as text */
	public static TagResolver type(String type)
	{
		return text("type", type == null ? "" : type);
	}

	/** {@code <type>} as a name from the message file (an item, a mob) */
	public static TagResolver type(Component type)
	{
		return component("type", type);
	}
}
