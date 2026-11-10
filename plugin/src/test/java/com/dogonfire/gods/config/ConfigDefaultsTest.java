package com.dogonfire.gods.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ConfigDefaultsTest
{
	private final YamlConfiguration defaults = TestConfigs.bundled();

	@Test
	void missingSettingsAreAddedWithTheirComments() throws Exception
	{
		// A config from before marriages existed
		YamlConfiguration config = TestConfigs.yaml("config-version: 2\ngeneral:\n  debug: true\n");
		assertTrue(ConfigDefaults.addMissing(config, this.defaults));

		assertTrue(config.getBoolean("general.debug"), "existing settings are kept");
		assertTrue(config.getBoolean("marriage.enabled"));
		assertFalse(config.getComments("marriage.enabled").isEmpty());
		// A missing free-form section is added whole
		assertEquals(List.of("end_stone"), config.getStringList("altars.block-types.moon"));
		assertEquals(100, config.getInt("quests.reward-values.diamond"));
	}

	@Test
	void entriesTheAdminRemovedStayRemoved() throws Exception
	{
		YamlConfiguration config = TestConfigs.yaml("""
				altars:
				  block-types:
				    love: [diamond_block]
				quests:
				  reward-values:
				    diamond: 200
				""");
		ConfigDefaults.addMissing(config, this.defaults);

		assertFalse(config.isSet("altars.block-types.frost"));
		assertFalse(config.isSet("quests.reward-values.coal"));
		assertEquals(200, config.getInt("quests.reward-values.diamond"));
		// Settings outside the free-form sections are still added
		assertTrue(config.isSet("quests.enabled"));
	}

	@Test
	void aCompleteConfigIsLeftAlone()
	{
		assertFalse(ConfigDefaults.addMissing(TestConfigs.bundled(), this.defaults));
	}
}
