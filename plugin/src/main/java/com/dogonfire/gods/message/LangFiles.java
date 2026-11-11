package com.dogonfire.gods.message;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.dogonfire.gods.migration.VersionedYaml;

/**
 * Reads the message files of one language from {@code plugins/Gods/lang/<language>/}, e.g. {@code messages.yml} or
 * {@code voices/sun_male.yml}. A file missing from the server is copied there from the jar (when the jar has it for
 * that language). A key missing from the server's file falls back to the bundled file of the same language, then to
 * the bundled english one.
 */
final class LangFiles
{
	static final String					DEFAULT_LANGUAGE	= "english";

	private final Path					langFolder;
	private final Function<String, InputStream>	resources;
	private final Logger				log;

	/**
	 * @param langFolder {@code plugins/Gods/lang}
	 * @param resources  opens a file in the plugin jar (null if it isn't there), like {@code Plugin#getResource}
	 */
	LangFiles(Path langFolder, Function<String, InputStream> resources, Logger log)
	{
		this.langFolder = langFolder;
		this.resources = resources;
		this.log = log;
	}

	/**
	 * Every key of the file with its value: a list of lines, one line for a plain string, no lines for {@code ''}.
	 * Keys of the server's file win over the bundled file's. Returns an empty map if no layer has the file.
	 */
	Map<String, List<String>> read(String language, String path)
	{
		Map<String, List<String>> result = new LinkedHashMap<>();

		YamlConfiguration english = bundled(DEFAULT_LANGUAGE, path);
		YamlConfiguration bundled = language.equals(DEFAULT_LANGUAGE) ? english : bundled(language, path);
		flatten(english, result);
		if (bundled != english)
		{
			flatten(bundled, result);
		}

		Path file = this.langFolder.resolve(language).resolve(path);
		if (!Files.exists(file) && bundled != null)
		{
			copyBundled(language, path, file);
		}

		if (Files.exists(file))
		{
			try
			{
				flatten(new VersionedYaml(Messages.VERSION_KEY, Messages.MESSAGES_VERSION, Messages.MESSAGES_VERSION, List.of()).load(file.toFile(), this.log), result);
			}
			catch (VersionedYaml.LoadException e)
			{
				this.log.warning(e.getMessage() + " Using the bundled messages instead.");
			}
		}
		else if (english != null)
		{
			this.log.info("lang/" + language + "/" + path + " was not found. Using english.");
		}

		return result;
	}

	private YamlConfiguration bundled(String language, String path)
	{
		try (InputStream in = this.resources.apply("lang/" + language + "/" + path))
		{
			if (in == null)
			{
				return null;
			}
			return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
		}
		catch (IOException e)
		{
			this.log.warning("Could not read the bundled lang/" + language + "/" + path + ": " + e.getMessage());
			return null;
		}
	}

	private void copyBundled(String language, String path, Path file)
	{
		try (InputStream in = this.resources.apply("lang/" + language + "/" + path))
		{
			if (in != null)
			{
				Files.createDirectories(file.getParent());
				Files.copy(in, file);
			}
		}
		catch (IOException e)
		{
			this.log.warning("Could not write " + file + ": " + e.getMessage());
		}
	}

	private static void flatten(ConfigurationSection yaml, Map<String, List<String>> into)
	{
		if (yaml == null)
		{
			return;
		}
		for (String key : yaml.getKeys(true))
		{
			if (key.equals(Messages.VERSION_KEY) || yaml.isConfigurationSection(key))
			{
				continue;
			}
			if (yaml.isList(key))
			{
				List<String> lines = new ArrayList<>();
				for (Object line : yaml.getList(key))
				{
					lines.add(String.valueOf(line));
				}
				into.put(key, List.copyOf(lines));
			}
			else
			{
				String value = String.valueOf(yaml.get(key));
				into.put(key, value.isEmpty() ? List.of() : List.of(value));
			}
		}
	}
}
