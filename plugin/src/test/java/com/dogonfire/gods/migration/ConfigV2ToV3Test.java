package com.dogonfire.gods.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.config.TestConfigs;
import com.dogonfire.gods.model.GodType;

class ConfigV2ToV3Test
{
	private final TestConfigs.CapturingLogger log = new TestConfigs.CapturingLogger();

	private YamlConfiguration migrate(String yaml) throws Exception
	{
		return new ConfigV2ToV3(TestConfigs::resource).migrate(TestConfigs.yaml(yaml));
	}

	/** A version 2 config with Gods' defaults: every god type gets its theme's blocks */
	@Test
	void theOldDefaultsGoToTheThemes() throws Exception
	{
		StringBuilder yaml = new StringBuilder("config-version: 2\naltars:\n  block-types:\n");
		ConfigV2ToV3.OLD_DEFAULTS.forEach((type, blocks) -> yaml.append("    ").append(type).append(": ").append(blocks).append('\n'));
		YamlConfiguration migrated = migrate(yaml.toString());

		assertTrue(migrated.isConfigurationSection("altars.block-types"));
		assertTrue(migrated.getConfigurationSection("altars.block-types").getKeys(false).isEmpty());
		assertFalse(migrated.getComments("altars.block-types").isEmpty());
		assertEquals(Settings.Altars.of(Map.of()), TestConfigs.load(TestConfigs.yaml(migrated.saveToString()), this.log).altars());
	}

	/** An admin's own lists stay; god types the admin left out still have no altar blocks */
	@Test
	void theAdminsListsStay() throws Exception
	{
		YamlConfiguration migrated = migrate("""
				config-version: 2
				altars:
				  block-types:
				    frost: [SNOW_BLOCK, packed_ice]
				    love: [minecraft:diamond_block]
				    sea: [lapis_block]
				    werewolves: [mossy_cobblestone]
				""");

		assertEquals(List.of("SNOW_BLOCK", "packed_ice"), migrated.getStringList("altars.block-types.frost"));
		assertEquals(List.of("mossy_cobblestone"), migrated.getStringList("altars.block-types.werewolves"));
		assertFalse(migrated.isSet("altars.block-types.love"));
		assertFalse(migrated.isSet("altars.block-types.sea"));
		assertTrue(migrated.isList("altars.block-types.moon"));
		assertTrue(migrated.getStringList("altars.block-types.moon").isEmpty());

		Settings.Altars altars = TestConfigs.load(TestConfigs.yaml(migrated.saveToString()), this.log).altars();
		assertEquals(List.of(Material.SNOW_BLOCK, Material.PACKED_ICE), altars.blocksFor(GodType.FROST));
		assertEquals(List.of(), altars.blocksFor(GodType.MOON));
		Set<GodType> listed = altars.blockTypes().keySet();
		assertFalse(listed.contains(GodType.LOVE));
		assertFalse(listed.contains(GodType.SEA));
	}

	@Test
	void aConfigWithoutAltarBlocksGetsTheThemes() throws Exception
	{
		YamlConfiguration migrated = migrate("config-version: 2\ngeneral:\n  debug: true\n");
		assertTrue(migrated.getConfigurationSection("altars.block-types").getKeys(false).isEmpty());
		assertTrue(migrated.getBoolean("general.debug"));
	}

	@Test
	void oldDefaultsAreRecognizedInAnySpelling()
	{
		assertTrue(ConfigV2ToV3.isOldDefault("frost", List.of(" minecraft:SNOW_BLOCK ")));
		assertTrue(ConfigV2ToV3.isOldDefault("werewolves", List.of()));
		assertFalse(ConfigV2ToV3.isOldDefault("frost", List.of("snow_block", "ice")));
		assertFalse(ConfigV2ToV3.isOldDefault("frots", List.of("snow_block")));
	}
}
