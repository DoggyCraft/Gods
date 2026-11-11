package com.dogonfire.gods.config;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

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
