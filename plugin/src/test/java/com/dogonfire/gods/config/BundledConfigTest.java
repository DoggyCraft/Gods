package com.dogonfire.gods.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.model.GodType;

class BundledConfigTest
{
	/**
	 * What Gods 0.9.x and this branch before Settings ended up with on a fresh install: the defaults passed to
	 * config.getX(key, default) in GodsConfiguration.loadSettings(), AltarManager.resetAltarBlockTypes(),
	 * QuestManager.resetItemRewardValues() and smooth stone as the only breakable holy land block. The old altar blocks
	 * are in the themes now (ThemesLoaderTest checks they make the same god types), so config.yml lists none.
	 */
	static Settings oldDefaults()
	{
		Map<GodType, List<Material>> altars = new EnumMap<>(GodType.class);

		Map<Material, Integer> rewards = new LinkedHashMap<>();
		rewards.put(Material.NETHER_WART, 33);
		rewards.put(Material.GLOWSTONE, 50);
		rewards.put(Material.COAL, 5);
		rewards.put(Material.IRON_INGOT, 10);
		rewards.put(Material.GOLD_INGOT, 50);
		rewards.put(Material.DIAMOND, 100);
		rewards.put(Material.PLAYER_HEAD, 75);
		rewards.put(Material.ENCHANTED_BOOK, 75);
		rewards.put(Material.POTION, 75);
		rewards.put(Material.COCOA, 30);
		rewards.put(Material.BLAZE_ROD, 50);
		rewards.put(Material.BOOK, 30);
		rewards.put(Material.COMPASS, 50);
		rewards.put(Material.PAPER, 20);
		rewards.put(Material.SUGAR_CANE, 25);
		rewards.put(Material.MELON_SEEDS, 5);
		rewards.put(Material.PUMPKIN_SEEDS, 5);
		rewards.put(Material.POPPY, 1);
		rewards.put(Material.SUNFLOWER, 1);

		return new Settings(
				new Settings.General(false, "english", "Your Server", List.of()),
				new Settings.GodSettings(true, false, true, 1.0D, 0.03D, false, false),
				new Settings.Believers(false, 5 * 60, 154),
				new Settings.Priests(3, 1, 72, 24, "", ""),
				Settings.Altars.of(altars),
				new Settings.Prayers(true, 30, 20, 10, 10, 50),
				new Settings.Sacrifices(true),
				new Settings.Commandments(true, true, true),
				new Settings.Blessings(true, 600, 180, true, true, true, true, true),
				new Settings.Curses(true, 5, 10, true, true),
				new Settings.ItemBlessings(true, 10, 3, 10, 50, 100),
				new Settings.HolyArtifacts(true, 1440),
				new Settings.Quests(false, 180, 1, true, true, true, false, rewards),
				new Settings.HolyLand(false, false, true, false, true, 14, Set.of(Material.SMOOTH_STONE)),
				new Settings.Marriage(true, true),
				new Settings.Bibles(true, false),
				new Settings.Chat(false),
				new Settings.Effects(true, true, true, true, 32),
				new Settings.AltarTexts(Settings.FloatingText.ALL, 32),
				new Settings.Npc(true, Settings.NpcProvider.AUTO),
				TestConfigs.defaultRanks());
	}

	@Test
	void defaultsAreTheOldDefaults()
	{
		TestConfigs.CapturingLogger log = new TestConfigs.CapturingLogger();
		Settings settings = TestConfigs.load(TestConfigs.bundled(), log);

		assertEquals(oldDefaults(), settings);
		assertTrue(log.warnings().isEmpty(), "Warnings: " + log.warnings());
	}

	@Test
	void anEmptyConfigGetsTheDefaults()
	{
		assertEquals(oldDefaults(), TestConfigs.load(new YamlConfiguration(), new TestConfigs.CapturingLogger()));
	}

	@Test
	void hasTheCurrentConfigVersion()
	{
		assertEquals(Gods.CONFIG_VERSION, TestConfigs.bundled().getInt("config-version"));
	}

	/** Every setting has a comment, its own or (for entries of a list section like altars.block-types) its section's. */
	@Test
	void everySettingIsExplained()
	{
		YamlConfiguration config = TestConfigs.bundled();
		List<String> unexplained = new ArrayList<>();
		for (String key : config.getKeys(true))
		{
			if (config.isConfigurationSection(key) || !config.getComments(key).isEmpty())
			{
				continue;
			}
			int dot = key.lastIndexOf('.');
			if (dot < 0 || config.getComments(key.substring(0, dot)).isEmpty())
			{
				unexplained.add(key);
			}
		}
		assertTrue(unexplained.isEmpty(), "No comment for " + unexplained);
	}
}
