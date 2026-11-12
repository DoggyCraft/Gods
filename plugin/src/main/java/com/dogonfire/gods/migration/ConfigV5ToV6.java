package com.dogonfire.gods.migration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.logging.Logger;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Upgrades config.yml from version 5 to 6, when believers stopped leaving after a fixed time without prayer: their
 * prayer power now decays after a quiet period and they leave when it is gone. {@code believers.max-hours-without-prayer}
 * is removed and becomes {@code believers.decay-after-days} ({@link #decayAfterDays}: half the old time, in days, at
 * least 1), so a server keeps roughly its patience: about half of it is the quiet period, the decay takes about as long
 * again. The other new settings ({@code decay-after-multiplier}, {@code decay-per-day}, {@code decay-percent-per-day})
 * come from the bundled config.yml through {@code ConfigDefaults}. {@code priests.max-hours-without-prayer} stays.
 */
public final class ConfigV5ToV6 implements Migration
{
	static final String							MAX_HOURS			= "believers.max-hours-without-prayer";
	static final String							DECAY_AFTER_DAYS	= "believers.decay-after-days";

	private final Function<String, InputStream>	resources;
	private final Logger						log;

	/** @param resources opens a file bundled in the plugin jar, like {@code Plugin#getResource} */
	public ConfigV5ToV6(Function<String, InputStream> resources, Logger log)
	{
		this.resources = resources;
		this.log = log;
	}

	/** The quiet period for an old {@code max-hours-without-prayer}: hours / 48, rounded, at least 1 day */
	public static int decayAfterDays(double maxHoursWithoutPrayer)
	{
		return (int) Math.max(1L, Math.round(maxHoursWithoutPrayer / 48.0D));
	}

	@Override
	public int fromVersion()
	{
		return 5;
	}

	@Override
	public YamlConfiguration migrate(YamlConfiguration old) throws MigrationException
	{
		if (!old.contains(MAX_HOURS))
		{
			return old;
		}
		Object hours = old.get(MAX_HOURS);
		old.set(MAX_HOURS, null);
		if (old.contains(DECAY_AFTER_DAYS))
		{
			this.log.info("Removed " + MAX_HOURS + " from config.yml: believers now leave when their prayer power has decayed (believers.decay-after-days).");
			return old;
		}
		if (hours instanceof Number number)
		{
			int days = decayAfterDays(number.doubleValue());
			old.set(DECAY_AFTER_DAYS, days);
			old.setComments(DECAY_AFTER_DAYS, bundled().getComments(DECAY_AFTER_DAYS));
			this.log.info("Replaced " + MAX_HOURS + ": " + hours + " in config.yml by " + DECAY_AFTER_DAYS + ": " + days
					+ ": believers now leave when their prayer power has decayed after that many days without a prayer (see the believers section).");
		}
		else
		{
			this.log.warning("Dropped " + MAX_HOURS + ": '" + hours + "' from config.yml (not a number); " + DECAY_AFTER_DAYS + " gets its default.");
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
