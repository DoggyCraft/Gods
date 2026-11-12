package com.dogonfire.gods.config;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import com.dogonfire.gods.model.DevotionLevel;
import com.dogonfire.gods.model.GodMood;
import com.dogonfire.gods.model.GodRank;

/** Config files and a logger that remembers its warnings, for the config and migration tests. */
public final class TestConfigs
{
	private TestConfigs()
	{
	}

	/** Opens a file from the test or main resources, like {@code Plugin#getResource}. */
	public static InputStream resource(String name)
	{
		return TestConfigs.class.getClassLoader().getResourceAsStream(name);
	}

	/** The config.yml bundled in the plugin jar. */
	public static YamlConfiguration bundled()
	{
		return YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(resource("config.yml")), StandardCharsets.UTF_8));
	}

	public static YamlConfiguration yaml(String content) throws InvalidConfigurationException
	{
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.loadFromString(content);
		return yaml;
	}

	/** The ranks of the bundled config.yml (ranks section) */
	public static Settings.Ranks defaultRanks()
	{
		Map<GodRank, Settings.RankConditions> conditions = new EnumMap<>(GodRank.class);
		conditions.put(GodRank.SPIRIT, Settings.RankConditions.NONE);
		conditions.put(GodRank.RECOGNIZED, new Settings.RankConditions(3, 0, true, null, false, 0, 0, 0, 0, 0, 0, 0, 0));
		conditions.put(GodRank.DESCENDED, new Settings.RankConditions(3, 0, true, GodMood.NEUTRAL, true, 24, 0, 0, 0, 0, 0, 0, 0));
		conditions.put(GodRank.EXALTED, new Settings.RankConditions(10, 50, false, GodMood.PLEASED, false, 0, 72, 150, 20, 2, 2, 0, 2));
		conditions.put(GodRank.GREATER, new Settings.RankConditions(20, 150, false, GodMood.PLEASED, false, 0, 168, 500, 75, 6, 4, 4, 5));
		Map<GodRank, Integer> extraRegions = new EnumMap<>(GodRank.class);
		extraRegions.put(GodRank.SPIRIT, 0);
		extraRegions.put(GodRank.RECOGNIZED, 1);
		extraRegions.put(GodRank.DESCENDED, 2);
		extraRegions.put(GodRank.EXALTED, 4);
		extraRegions.put(GodRank.GREATER, 8);
		return new Settings.Ranks(conditions, 24,
				new Settings.Unlocks(GodRank.RECOGNIZED, GodRank.RECOGNIZED, GodRank.EXALTED, GodRank.EXALTED, GodRank.GREATER, GodRank.GREATER, extraRegions),
				new Settings.Aura(true, "regeneration", 0, 15));
	}

	/** The devotion of the bundled config.yml (devotion section) */
	public static Settings.Devotion defaultDevotion()
	{
		Map<DevotionLevel, Integer> levels = new EnumMap<>(DevotionLevel.class);
		levels.put(DevotionLevel.FOLLOWER, 0);
		levels.put(DevotionLevel.DEVOTEE, 150);
		levels.put(DevotionLevel.ZEALOT, 600);
		levels.put(DevotionLevel.SAINT, 2000);
		return new Settings.Devotion(true, new Settings.DevotionPoints(10, 15, 40, 30, 5, 5), levels, 10, 3, 5, 75, 0,
				new Settings.DevotionUnlocks(DevotionLevel.DEVOTEE, DevotionLevel.ZEALOT, DevotionLevel.ZEALOT, DevotionLevel.SAINT, DevotionLevel.SAINT,
						DevotionLevel.SAINT),
				25, 2, true);
	}

	/** Settings from {@code config}, with the bundled config as its defaults, the way Gods loads them. */
	public static Settings load(YamlConfiguration config, CapturingLogger log)
	{
		config.setDefaults(bundled());
		return new SettingsLoader(config, log.logger()).load();
	}

	/** A logger that keeps the warnings it is given instead of printing them. */
	public static final class CapturingLogger
	{
		private final Logger		logger		= Logger.getAnonymousLogger();
		private final List<String>	warnings	= new ArrayList<>();

		public CapturingLogger()
		{
			this.logger.setUseParentHandlers(false);
			this.logger.addHandler(new Handler()
			{
				@Override
				public void publish(LogRecord record)
				{
					if (record.getLevel().intValue() >= Level.WARNING.intValue())
					{
						warnings.add(record.getMessage());
					}
				}

				@Override
				public void flush()
				{
				}

				@Override
				public void close()
				{
				}
			});
		}

		public Logger logger()
		{
			return this.logger;
		}

		public List<String> warnings()
		{
			return this.warnings;
		}
	}
}
