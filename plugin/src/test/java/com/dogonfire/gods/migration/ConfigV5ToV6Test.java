package com.dogonfire.gods.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.ConfigDefaults;
import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.config.TestConfigs;

class ConfigV5ToV6Test
{
	private static final String				VERSION_5	= """
			# My server
			config-version: 5
			general:
			  language: danish
			believers:
			  leave-religion-on-death: true
			  # Our players are patient
			  max-hours-without-prayer: 240
			priests:
			  max-hours-without-prayer: 48
			""";

	private final TestConfigs.CapturingLogger	log			= new TestConfigs.CapturingLogger();

	@TempDir
	Path									dir;

	private ConfigV5ToV6 migration()
	{
		return new ConfigV5ToV6(TestConfigs::resource, this.log.logger());
	}

	/** Half the old time, in days, rounded, at least one */
	@Test
	void theOldHoursBecomeHalfAsManyDays()
	{
		assertEquals(3, ConfigV5ToV6.decayAfterDays(154));
		assertEquals(5, ConfigV5ToV6.decayAfterDays(240));
		assertEquals(1, ConfigV5ToV6.decayAfterDays(24));
		assertEquals(1, ConfigV5ToV6.decayAfterDays(0));
	}

	@Test
	void replacesMaxHoursWithoutPrayerByTheQuietPeriod() throws Exception
	{
		YamlConfiguration migrated = migration().migrate(TestConfigs.yaml(VERSION_5));

		assertFalse(migrated.contains("believers.max-hours-without-prayer"));
		assertEquals(5, migrated.getInt("believers.decay-after-days"));
		assertFalse(migrated.getComments("believers.decay-after-days").isEmpty(), "the bundled comment comes along");
		assertEquals(48, migrated.getInt("priests.max-hours-without-prayer"), "the priests' setting stays");
		assertTrue(migrated.getBoolean("believers.leave-religion-on-death"));
		assertTrue(this.log.warnings().isEmpty(), "Warnings: " + this.log.warnings());

		// The other new settings come from the bundled config
		ConfigDefaults.addMissing(migrated, TestConfigs.bundled());
		Settings settings = TestConfigs.load(TestConfigs.yaml(migrated.saveToString()), this.log);
		assertEquals(TestConfigs.believers(true, 300, 5), settings.believers());
		assertEquals(48, settings.priests().maxHoursWithoutPrayer());
		assertTrue(this.log.warnings().isEmpty(), "Warnings: " + this.log.warnings());
	}

	/** A config at the defaults gets the default quiet period */
	@Test
	void theOldDefaultBecomesTheNewDefault() throws Exception
	{
		YamlConfiguration migrated = migration().migrate(TestConfigs.yaml("config-version: 5\nbelievers:\n  max-hours-without-prayer: 154\n"));
		Settings settings = TestConfigs.load(TestConfigs.yaml(migrated.saveToString()), this.log);
		assertEquals(TestConfigs.defaultBelievers(), settings.believers());
	}

	/** A decay-after-days the admin already wrote is kept; a value that isn't a number is dropped with a warning */
	@Test
	void anExistingQuietPeriodWinsAndNonsenseIsDropped() throws Exception
	{
		YamlConfiguration kept = migration().migrate(TestConfigs.yaml("config-version: 5\nbelievers:\n  max-hours-without-prayer: 480\n  decay-after-days: 2\n"));
		assertEquals(2, kept.getInt("believers.decay-after-days"));
		assertFalse(kept.contains("believers.max-hours-without-prayer"));

		YamlConfiguration dropped = migration().migrate(TestConfigs.yaml("config-version: 5\nbelievers:\n  max-hours-without-prayer: forever\n"));
		assertFalse(dropped.contains("believers.max-hours-without-prayer"));
		assertFalse(dropped.contains("believers.decay-after-days"));
		assertEquals(1, this.log.warnings().size());
	}

	/** How Gods loads config.yml: upgraded in place, the old file kept as config.yml.v5.bak, comments kept */
	@Test
	void upgradesTheFileOnDiskAndKeepsABackup() throws Exception
	{
		Path file = this.dir.resolve("config.yml");
		Files.writeString(file, VERSION_5);

		VersionedYaml format = new VersionedYaml("config-version", Gods.CONFIG_VERSION, 1,
				List.of(new ConfigV1ToV2(TestConfigs::resource, this.log.logger()), new ConfigV2ToV3(TestConfigs::resource), new ConfigV3ToV4(this.log.logger()),
						new ConfigV4ToV5(TestConfigs::resource, this.log.logger()), migration()),
				ConfigV1ToV2::isVersion2Layout);
		YamlConfiguration loaded = format.load(file.toFile(), this.log.logger());

		assertEquals(6, Gods.CONFIG_VERSION);
		assertEquals(Gods.CONFIG_VERSION, loaded.getInt("config-version"));
		assertEquals(VERSION_5, Files.readString(this.dir.resolve("config.yml.v5.bak")));
		String onDisk = Files.readString(file);
		assertFalse(onDisk.contains("Our players are patient"), "its comment goes with it");
		assertTrue(onDisk.contains("# My server"), "comments stay");
		assertTrue(onDisk.contains("decay-after-days: 5"));
	}
}
