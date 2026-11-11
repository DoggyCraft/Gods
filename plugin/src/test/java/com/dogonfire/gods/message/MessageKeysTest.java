package com.dogonfire.gods.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.model.GodType;

/** Every message and voice key the code uses must exist in the bundled files, or players see "missing message" */
class MessageKeysTest
{
	private static final Pattern	KEY	= Pattern.compile("\"([a-z][a-z-]*(?:\\.[a-z0-9_-]+)+)\"");

	/** Keys built at runtime, which the scan can't see */
	private static final List<String>	DYNAMIC_KEYS	= List.of();

	@Test
	void everyUsedMessageKeyExists() throws IOException
	{
		YamlConfiguration messages = bundled("lang/english/messages.yml");
		Set<String> sections = messages.getKeys(false);
		// Settings look the same (holy-land.enabled), so they are told apart by being in config.yml
		YamlConfiguration config = bundled("config.yml");

		TreeSet<String> used = new TreeSet<>(DYNAMIC_KEYS);
		try (Stream<Path> files = Files.walk(Path.of("src/main/java")))
		{
			// migration/ names the keys of old files
			for (Path file : files.filter(f -> f.toString().endsWith(".java") && !f.toString().contains("/migration/")).toList())
			{
				Matcher matcher = KEY.matcher(Files.readString(file));
				while (matcher.find())
				{
					String key = matcher.group(1);
					// Permissions (gods.listen) and the like look the same; only keys of a message section count
					if (sections.contains(key.substring(0, key.indexOf('.'))) && !config.contains(key))
					{
						used.add(key);
					}
				}
			}
		}

		assertFalse(used.isEmpty());
		List<String> missing = new ArrayList<>();
		for (String key : used)
		{
			if (!messages.contains(key) || messages.isConfigurationSection(key))
			{
				missing.add(key);
			}
		}
		assertTrue(missing.isEmpty(), "Missing from messages.yml: " + missing);
	}

	/** /gods help shows command.&lt;name&gt;.parameters and .description of every command */
	@Test
	void everyCommandHasParametersAndADescription() throws IOException
	{
		YamlConfiguration messages = bundled("lang/english/messages.yml");
		Pattern command = Pattern.compile("super\\(ctx, \"([a-z]+)\", ");
		List<String> missing = new ArrayList<>();
		int commands = 0;
		try (Stream<Path> files = Files.list(Path.of("src/main/java/com/dogonfire/gods/command")))
		{
			for (Path file : files.toList())
			{
				Matcher matcher = command.matcher(Files.readString(file));
				while (matcher.find())
				{
					commands++;
					for (String part : List.of("parameters", "description"))
					{
						if (!messages.isString("command." + matcher.group(1) + "." + part))
						{
							missing.add("command." + matcher.group(1) + "." + part);
						}
					}
				}
			}
		}
		assertTrue(commands > 30);
		assertTrue(missing.isEmpty(), "Missing from messages.yml: " + missing);
	}

	@Test
	void everyVoiceKeyIsInEveryVoiceFile() throws IOException
	{
		List<String> missing = new ArrayList<>();
		for (GodType type : GodType.values())
		{
			for (GodGender gender : List.of(GodGender.Male, GodGender.Female))
			{
				String name = GodVoices.name(type, gender);
				YamlConfiguration voice = bundled("lang/english/voices/" + name + ".yml");
				assertEquals(Messages.MESSAGES_VERSION, voice.getInt(Messages.VERSION_KEY), name);
				for (Voice situation : Voice.values())
				{
					if (voice.getStringList(situation.key()).isEmpty())
					{
						missing.add(name + ": " + situation.key());
					}
				}
			}
		}
		assertTrue(missing.isEmpty(), "Missing voice lines: " + missing);
	}

	/** Every mob and item in Gods' own themes has a name in messages.yml (others get one made from the id) */
	@Test
	void everyNameInTheThemesHasAName() throws IOException
	{
		YamlConfiguration messages = bundled("lang/english/messages.yml");
		YamlConfiguration themes = bundled("themes.yml");
		List<String> missing = new ArrayList<>();
		for (String key : themes.getKeys(true))
		{
			// Effects are never named to players
			if (!themes.isList(key) || key.contains(".effects."))
			{
				continue;
			}
			String list = key.substring(key.indexOf('.') + 1);
			String section = list.endsWith("mobs") || list.equals("slay-targets") ? "names.mob." : "names.item.";
			for (String name : themes.getStringList(key))
			{
				String nameKey = section + name.toUpperCase(java.util.Locale.ROOT);
				if (!messages.isString(nameKey) && !missing.contains(nameKey))
				{
					missing.add(nameKey);
				}
			}
		}
		assertTrue(missing.isEmpty(), "Missing from messages.yml: " + missing);
	}

	/** Every sign style in Gods' own themes has the text of each kind of altar sign, four lines at most */
	@Test
	void everySignStyleHasItsText() throws IOException
	{
		YamlConfiguration messages = bundled("lang/english/messages.yml");
		YamlConfiguration themes = bundled("themes.yml");
		List<String> problems = new ArrayList<>();
		Set<String> styles = new TreeSet<>();
		for (String section : themes.getKeys(false))
		{
			styles.add(themes.getString(section + ".sign.messages", "default"));
		}
		assertTrue(styles.contains("default"));
		for (String style : styles)
		{
			for (String kind : List.of("praying", "curses", "blessings"))
			{
				String key = "altar.sign." + style + "." + kind;
				List<String> lines = messages.getStringList(key);
				if (lines.isEmpty() || lines.size() > 4)
				{
					problems.add(key);
				}
			}
		}
		assertEquals(GodType.values().length + 1, styles.size(), "" + styles);
		assertTrue(problems.isEmpty(), "Missing or too long in messages.yml: " + problems);
	}

	/** Every sign style in Gods' own themes has the floating text of altars and main altars, three lines at most */
	@Test
	void everySignStyleHasItsFloatingText() throws IOException
	{
		YamlConfiguration messages = bundled("lang/english/messages.yml");
		YamlConfiguration themes = bundled("themes.yml");
		List<String> problems = new ArrayList<>();
		Set<String> styles = new TreeSet<>();
		for (String section : themes.getKeys(false))
		{
			styles.add(themes.getString(section + ".sign.messages", "default"));
		}
		for (String style : styles)
		{
			for (String kind : List.of("altar", "main"))
			{
				String key = "altar.floating." + style + "." + kind;
				List<String> lines = messages.getStringList(key);
				if (lines.isEmpty() || lines.size() > 3 || !lines.getFirst().contains("<god>"))
				{
					problems.add(key);
				}
			}
		}
		assertTrue(problems.isEmpty(), "Missing, too long or without <god> in messages.yml: " + problems);
	}

	@Test
	void theBundledMessagesHaveTheirVersion() throws IOException
	{
		assertEquals(Messages.MESSAGES_VERSION, bundled("lang/english/messages.yml").getInt(Messages.VERSION_KEY));
	}

	private YamlConfiguration bundled(String path) throws IOException
	{
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(path))
		{
			YamlConfiguration yaml = new YamlConfiguration();
			try
			{
				yaml.loadFromString(new String(Objects.requireNonNull(in, path).readAllBytes(), StandardCharsets.UTF_8));
			}
			catch (org.bukkit.configuration.InvalidConfigurationException e)
			{
				throw new IOException(path + " is broken", e);
			}
			return yaml;
		}
	}
}
