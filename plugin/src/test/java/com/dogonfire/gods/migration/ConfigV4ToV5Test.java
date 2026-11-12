package com.dogonfire.gods.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.dogonfire.gods.model.GodMood;
import com.dogonfire.gods.model.GodRank;

class ConfigV4ToV5Test
{
	private static final String				VERSION_4	= """
			# My server
			config-version: 4
			general:
			  language: danish
			npc:
			  # Figures at altars
			  enabled: true
			  provider: citizens
			  # When gods descend
			  descend:
			    requires-priest: false
			    min-believers: 5
			    hours-after-priest: 12
			    min-power: 40
			    # How long before it goes again
			    ascend-grace-hours: 6
			""";

	private final TestConfigs.CapturingLogger	log			= new TestConfigs.CapturingLogger();

	@TempDir
	Path									dir;

	private ConfigV4ToV5 migration()
	{
		return new ConfigV4ToV5(TestConfigs::resource, this.log.logger());
	}

	/** npc.descend moves into ranks.descended (and its grace period becomes ranks.drop-grace-hours) */
	@Test
	void movesTheDescentIntoTheDescendedRank() throws Exception
	{
		YamlConfiguration migrated = migration().migrate(TestConfigs.yaml(VERSION_4));

		assertFalse(migrated.contains("npc.descend"));
		assertEquals("citizens", migrated.getString("npc.provider"));
		assertEquals(List.of("Figures at altars"), migrated.getComments("npc.enabled"));
		assertFalse(migrated.getBoolean("ranks.descended.requires-priest"));
		assertEquals(5, migrated.getInt("ranks.descended.min-believers"));
		assertEquals(12, migrated.getInt("ranks.descended.hours-after-priest"));
		assertEquals(40, migrated.getInt("ranks.descended.min-power"));
		assertEquals(6, migrated.getInt("ranks.drop-grace-hours"));
		// The rest of the bundled ranks, with their comments
		assertEquals("neutral", migrated.getString("ranks.descended.min-mood"));
		assertEquals(10, migrated.getInt("ranks.exalted.min-believers"));
		assertEquals("exalted", migrated.getString("ranks.unlocks.holy-artifacts"));
		assertFalse(migrated.getComments("ranks.drop-grace-hours").isEmpty());
		assertFalse(migrated.getComments("ranks.exalted").isEmpty());
		assertTrue(this.log.warnings().isEmpty(), "Warnings: " + this.log.warnings());

		Settings settings = TestConfigs.load(TestConfigs.yaml(migrated.saveToString()), this.log);
		Settings.RankConditions descended = settings.ranks().of(GodRank.DESCENDED);
		assertEquals(new Settings.RankConditions(5, 40, false, GodMood.NEUTRAL, true, 12, 0, 0, 0, 0, 0, 0, 0), descended);
		assertEquals(6, settings.ranks().dropGraceHours());
		assertEquals(new Settings.Npc(true, Settings.NpcProvider.CITIZENS), settings.npc());
		assertTrue(this.log.warnings().isEmpty(), "Warnings: " + this.log.warnings());
	}

	/** A config without npc.descend (it had the defaults) gets the bundled ranks as they are */
	@Test
	void withoutADescentTheBundledRanksAreUsed() throws Exception
	{
		YamlConfiguration migrated = migration().migrate(TestConfigs.yaml("config-version: 4\nnpc:\n  enabled: false\n"));
		assertFalse(migrated.getBoolean("npc.enabled"));
		Settings settings = TestConfigs.load(TestConfigs.yaml(migrated.saveToString()), this.log);
		assertEquals(TestConfigs.defaultRanks(), settings.ranks());
	}

	/** A ranks section the admin already wrote (by hand, ahead of the upgrade) is kept */
	@Test
	void anExistingRanksSectionIsKept() throws Exception
	{
		YamlConfiguration migrated = migration().migrate(TestConfigs.yaml("""
				config-version: 4
				ranks:
				  recognized:
				    min-believers: 1
				"""));
		assertEquals(1, migrated.getInt("ranks.recognized.min-believers"));
		assertFalse(migrated.contains("ranks.recognized.requires-priest"));
		assertNull(migrated.get("ranks.greater"));
	}

	/** A condition an admin removes stays removed: rank sections are free-form */
	@Test
	void aRemovedConditionStaysRemoved() throws Exception
	{
		YamlConfiguration config = migration().migrate(TestConfigs.yaml(VERSION_4));
		config.set("ranks.exalted.min-quests", null);
		ConfigDefaults.addMissing(config, TestConfigs.bundled());
		assertFalse(config.contains("ranks.exalted.min-quests"));
		Settings settings = TestConfigs.load(TestConfigs.yaml(config.saveToString()), this.log);
		assertEquals(0, settings.ranks().of(GodRank.EXALTED).minQuests());
		assertEquals(150, settings.ranks().of(GodRank.EXALTED).minPrayers());
	}

	/** How Gods loads config.yml: upgraded in place, the old file kept as config.yml.v4.bak, comments kept */
	@Test
	void upgradesTheFileOnDiskAndKeepsABackup() throws Exception
	{
		Path file = this.dir.resolve("config.yml");
		Files.writeString(file, VERSION_4);

		VersionedYaml format = new VersionedYaml("config-version", Gods.CONFIG_VERSION, 1,
				List.of(new ConfigV1ToV2(TestConfigs::resource, this.log.logger()), new ConfigV2ToV3(TestConfigs::resource), new ConfigV3ToV4(this.log.logger()), migration(),
						new ConfigV5ToV6(TestConfigs::resource, this.log.logger())),
				ConfigV1ToV2::isVersion2Layout);
		YamlConfiguration loaded = format.load(file.toFile(), this.log.logger());

		assertTrue(Gods.CONFIG_VERSION >= 5);
		assertEquals(Gods.CONFIG_VERSION, loaded.getInt("config-version"));
		assertEquals(VERSION_4, Files.readString(this.dir.resolve("config.yml.v4.bak")));
		String onDisk = Files.readString(file);
		assertFalse(onDisk.contains("descend:"));
		assertFalse(onDisk.contains("How long before it goes again"), "its comment goes with it");
		assertTrue(onDisk.contains("# My server"), "comments stay");
		assertTrue(onDisk.contains("drop-grace-hours: 6"));
		assertTrue(onDisk.contains("# A religion grows through five ranks"), "the bundled comments come along");
	}
}
