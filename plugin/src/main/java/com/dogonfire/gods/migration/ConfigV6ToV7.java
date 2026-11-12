package com.dogonfire.gods.migration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Upgrades config.yml from version 6 to 7, when holy land went from 128x128 block regions to chunks, one land per
 * altar. {@code ranks.unlocks.holy-land-extra-regions} (more regions per rank) is removed: a god may hold power / 10 + 3
 * holy lands at every rank, and its rank decides how far each land reaches instead,
 * {@code ranks.unlocks.holy-land-radius-chunks}, added from the bundled config.yml with its comment. A radius the admin
 * already wrote is kept; everything else stays as it is.
 */
public final class ConfigV6ToV7 implements Migration
{
	static final String							EXTRA_REGIONS	= "ranks.unlocks.holy-land-extra-regions";
	static final String							RADIUS_CHUNKS	= "ranks.unlocks.holy-land-radius-chunks";

	private final Function<String, InputStream>	resources;
	private final Logger						log;

	/** @param resources opens a file bundled in the plugin jar, like {@code Plugin#getResource} */
	public ConfigV6ToV7(Function<String, InputStream> resources, Logger log)
	{
		this.resources = resources;
		this.log = log;
	}

	@Override
	public int fromVersion()
	{
		return 6;
	}

	@Override
	public YamlConfiguration migrate(YamlConfiguration old) throws MigrationException
	{
		if (old.contains(EXTRA_REGIONS))
		{
			old.set(EXTRA_REGIONS, null);
			this.log.info("Removed " + EXTRA_REGIONS + " from config.yml: holy land is claimed per chunk now, one land per altar, and the god's rank decides how far each land reaches ("
					+ RADIUS_CHUNKS + ").");
		}
		if (!old.contains(RADIUS_CHUNKS) && old.isConfigurationSection("ranks.unlocks"))
		{
			ConfigurationSection bundled = bundled().getConfigurationSection(RADIUS_CHUNKS);
			if (bundled == null)
			{
				throw new MigrationException("the bundled config.yml has no " + RADIUS_CHUNKS);
			}
			ConfigurationSection radius = old.createSection(RADIUS_CHUNKS);
			for (String rank : bundled.getKeys(false))
			{
				radius.set(rank, bundled.get(rank));
			}
			old.setComments(RADIUS_CHUNKS, bundled.getRoot().getComments(RADIUS_CHUNKS));
		}
		return old;
	}

	private YamlConfiguration bundled() throws MigrationException
	{
		try (InputStream in = this.resources.apply("config.yml"))
		{
			if (in == null)
			{
				throw new MigrationException("config.yml is missing from the plugin jar");
			}
			return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
		}
		catch (IOException e)
		{
			throw new MigrationException("could not read the bundled config.yml: " + e.getMessage());
		}
	}
}
