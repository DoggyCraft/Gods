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
import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.config.TestConfigs;

class ConfigV3ToV4Test
{
	private static final String				VERSION_3	= """
			# My server
			config-version: 3
			general:
			  language: danish
			priests:
			  min-believers: 7
			  # Only priests can set their religion's home with /g sethome.
			  only-priests-can-set-home: true
			  # Wait between gender changes
			  gender-change-cooldown-hours: 12
			""";

	private final TestConfigs.CapturingLogger	log			= new TestConfigs.CapturingLogger();

	@TempDir
	Path									dir;

	/** The setting goes; everything else stays as it was */
	@Test
	void dropsOnlyPriestsCanSetHome() throws Exception
	{
		YamlConfiguration migrated = new ConfigV3ToV4(this.log.logger()).migrate(TestConfigs.yaml(VERSION_3));

		assertFalse(migrated.contains("priests.only-priests-can-set-home"));
		assertEquals(7, migrated.getInt("priests.min-believers"));
		assertEquals(12, migrated.getInt("priests.gender-change-cooldown-hours"));
		assertEquals(List.of("Wait between gender changes"), migrated.getComments("priests.gender-change-cooldown-hours"));
		assertEquals("danish", migrated.getString("general.language"));
		assertTrue(this.log.warnings().isEmpty(), "Warnings: " + this.log.warnings());

		Settings settings = TestConfigs.load(TestConfigs.yaml(migrated.saveToString()), this.log);
		assertEquals(7, settings.priests().minBelievers());
		assertEquals(12, settings.priests().genderChangeCooldownHours());
	}

	@Test
	void aConfigWithoutTheSettingIsUnchanged() throws Exception
	{
		String yaml = "config-version: 3\npriests:\n  min-believers: 7\n";
		YamlConfiguration migrated = new ConfigV3ToV4(this.log.logger()).migrate(TestConfigs.yaml(yaml));
		assertEquals(TestConfigs.yaml(yaml).saveToString(), migrated.saveToString());
	}

	/** How Gods loads config.yml: upgraded in place, the old file kept as config.yml.v3.bak, comments kept */
	@Test
	void upgradesTheFileOnDiskAndKeepsABackup() throws Exception
	{
		Path file = this.dir.resolve("config.yml");
		Files.writeString(file, VERSION_3);

		VersionedYaml format = new VersionedYaml("config-version", Gods.CONFIG_VERSION, 1,
				List.of(new ConfigV1ToV2(TestConfigs::resource, this.log.logger()), new ConfigV2ToV3(TestConfigs::resource), new ConfigV3ToV4(this.log.logger())),
				ConfigV1ToV2::isVersion2Layout);
		YamlConfiguration loaded = format.load(file.toFile(), this.log.logger());

		assertEquals(4, Gods.CONFIG_VERSION);
		assertEquals(Gods.CONFIG_VERSION, loaded.getInt("config-version"));
		assertFalse(loaded.contains("priests.only-priests-can-set-home"));
		assertEquals(VERSION_3, Files.readString(this.dir.resolve("config.yml.v3.bak")));
		String onDisk = Files.readString(file);
		assertFalse(onDisk.contains("only-priests-can-set-home"));
		assertFalse(onDisk.contains("Only priests can set"), "its comment goes with it");
		assertTrue(onDisk.contains("# My server"), "comments stay");
		assertTrue(onDisk.contains("min-believers: 7"));
		assertEquals(Gods.CONFIG_VERSION, YamlConfiguration.loadConfiguration(file.toFile()).getInt("config-version"));
	}
}
