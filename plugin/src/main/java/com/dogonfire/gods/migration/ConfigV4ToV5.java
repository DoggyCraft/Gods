package com.dogonfire.gods.migration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Upgrades config.yml from version 4 to 5, when gods got ranks: the descent ({@code npc.descend}) became the descended
 * rank. The bundled {@code ranks} section is added (with its comments, unless the config has one already), and the
 * admin's descent settings move into it:
 * <ul>
 * <li>{@code npc.descend.requires-priest}, {@code min-believers}, {@code hours-after-priest} and {@code min-power} to
 * {@code ranks.descended.*} (same names);</li>
 * <li>{@code npc.descend.ascend-grace-hours} to {@code ranks.drop-grace-hours} (it now holds for every rank).</li>
 * </ul>
 * {@code npc.descend} is removed. Everything else stays as it is.
 */
public final class ConfigV4ToV5 implements Migration
{
	static final String							DESCEND		= "npc.descend";
	static final String							RANKS		= "ranks";

	/** Each old npc.descend key and where it goes */
	static final Map<String, String>			MOVED		= movedKeys();

	private final Function<String, InputStream>	resources;
	private final Logger						log;

	/** @param resources opens a file bundled in the plugin jar, like {@code Plugin#getResource} */
	public ConfigV4ToV5(Function<String, InputStream> resources, Logger log)
	{
		this.resources = resources;
		this.log = log;
	}

	private static Map<String, String> movedKeys()
	{
		Map<String, String> moved = new LinkedHashMap<>();
		moved.put("requires-priest", "ranks.descended.requires-priest");
		moved.put("min-believers", "ranks.descended.min-believers");
		moved.put("hours-after-priest", "ranks.descended.hours-after-priest");
		moved.put("min-power", "ranks.descended.min-power");
		moved.put("ascend-grace-hours", "ranks.drop-grace-hours");
		return moved;
	}

	@Override
	public int fromVersion()
	{
		return 4;
	}

	@Override
	public YamlConfiguration migrate(YamlConfiguration old) throws MigrationException
	{
		if (!old.isSet(RANKS))
		{
			copySection(bundled("config.yml"), old, RANKS);
		}
		if (old.isSet(DESCEND))
		{
			ConfigurationSection descend = old.getConfigurationSection(DESCEND);
			if (descend != null)
			{
				for (Map.Entry<String, String> move : MOVED.entrySet())
				{
					if (descend.isSet(move.getKey()))
					{
						old.set(move.getValue(), descend.get(move.getKey()));
					}
				}
				for (String key : descend.getKeys(false))
				{
					if (!MOVED.containsKey(key))
					{
						this.log.warning("Dropped unknown setting " + DESCEND + "." + key + " from config.yml.");
					}
				}
			}
			old.set(DESCEND, null);
			this.log.info("Moved npc.descend in config.yml to ranks.descended: a god's descent is its descended rank now (see the ranks section).");
		}
		return old;
	}

	/** Copies a section of {@code from} into {@code to}, in order and with its comments */
	private static void copySection(YamlConfiguration from, YamlConfiguration to, String path)
	{
		if (!from.isConfigurationSection(path))
		{
			return;
		}
		to.createSection(path);
		to.setComments(path, from.getComments(path));
		for (String key : from.getConfigurationSection(path).getKeys(true))
		{
			String full = path + "." + key;
			if (from.isConfigurationSection(full))
			{
				to.createSection(full);
			}
			else
			{
				to.set(full, from.get(full));
			}
			to.setComments(full, from.getComments(full));
		}
	}

	private YamlConfiguration bundled(String name) throws MigrationException
	{
		try (InputStream in = this.resources.apply(name))
		{
			if (in == null)
			{
				throw new MigrationException(name + " is missing from the plugin jar");
			}
			return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
		}
		catch (IOException e)
		{
			throw new MigrationException("could not read the bundled " + name + ": " + e.getMessage());
		}
	}
}
