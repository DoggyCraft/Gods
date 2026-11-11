package com.dogonfire.gods.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.config.TestConfigs;
import com.dogonfire.gods.model.GodType;

class ConfigV1ToV2Test
{
	private final TestConfigs.CapturingLogger	log	= new TestConfigs.CapturingLogger();

	@TempDir
	Path										dir;

	private static String fixture(String name) throws Exception
	{
		try (InputStream in = Objects.requireNonNull(TestConfigs.resource("legacy/" + name)))
		{
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private YamlConfiguration migrate(YamlConfiguration old) throws Exception
	{
		return new ConfigV1ToV2(TestConfigs::resource, this.log.logger()).migrate(old);
	}

	private Settings migrateAndLoad(String fixture) throws Exception
	{
		YamlConfiguration migrated = migrate(TestConfigs.yaml(fixture(fixture)));
		// What Gods does next: read the migrated file back with the bundled config as defaults
		return TestConfigs.load(TestConfigs.yaml(migrated.saveToString()), this.log);
	}

	/** The config written by this branch before config versions: every value differs from its default. */
	@Test
	void movesEverySettingToItsNewKey() throws Exception
	{
		Settings settings = migrateAndLoad("config-1.0-pre.yml");

		Map<GodType, List<Material>> altars = new EnumMap<>(GodType.class);
		altars.put(GodType.FROST, List.of(Material.SNOW_BLOCK, Material.PACKED_ICE));
		altars.put(GodType.LOVE, List.of(Material.DIAMOND_BLOCK));
		altars.put(GodType.EVIL, List.of(Material.OBSIDIAN));
		altars.put(GodType.SEA, List.of(Material.LAPIS_BLOCK));
		altars.put(GodType.MOON, List.of(Material.END_STONE));
		altars.put(GodType.SUN, List.of(Material.SANDSTONE));
		altars.put(GodType.THUNDER, List.of(Material.QUARTZ_BLOCK));
		altars.put(GodType.PARTY, List.of(Material.EMERALD_BLOCK));
		altars.put(GodType.WAR, List.of(Material.NETHERRACK));
		altars.put(GodType.WEREWOLVES, List.of());
		altars.put(GodType.CREATURES, List.of(Material.OAK_LOG));
		altars.put(GodType.WISDOM, List.of(Material.BOOKSHELF));
		altars.put(GodType.NATURE, List.of(Material.MELON));

		Settings expected = new Settings(
				new Settings.General(true, "danish", "DoggyCraft", List.of("world", "world_nether")),
				new Settings.GodSettings(false, true, false, 0.5D, 0.05D, true, false),
				new Settings.Believers(true, 600, 200),
				new Settings.Priests(5, 2, 48, true, 24, "lp user $PlayerName parent add priest", "lp user $PlayerName parent remove priest"),
				Settings.Altars.of(altars),
				new Settings.Prayers(false, 15, 25, 12, 15, 55),
				new Settings.Sacrifices(false),
				new Settings.Commandments(false, false, false),
				new Settings.Blessings(false, 900, 240, false, false, false, false, false),
				new Settings.Curses(false, 30, 20, false, false),
				new Settings.ItemBlessings(false, 20, 7, 15, 60, 120),
				new Settings.HolyArtifacts(false, 720),
				new Settings.Quests(true, 60, 4, false, false, false, true, Map.of(Material.DIAMOND, 200, Material.EMERALD, 150)),
				new Settings.HolyLand(true, true, false, true, false, 30, Set.of(Material.SMOOTH_STONE, Material.GRAVEL)),
				new Settings.Marriage(false, false),
				new Settings.Bibles(false, true),
				new Settings.Chat(true),
				new Settings.Effects(true, true, true, true, 32),
				new Settings.AltarTexts(Settings.FloatingText.ALL, 32),
				new Settings.Npc(true, Settings.NpcProvider.AUTO));

		assertEquals(expected, settings);
		assertTrue(this.log.warnings().isEmpty(), "Warnings: " + this.log.warnings());
	}

	/** Every old key the migration knows ends up somewhere in the new config. */
	@Test
	void everyMovedKeyExistsInTheBundledConfig()
	{
		YamlConfiguration bundled = TestConfigs.bundled();
		for (String newKey : ConfigV1ToV2.MOVED.values())
		{
			assertTrue(bundled.isSet(newKey) && !bundled.isConfigurationSection(newKey), newKey + " is not a setting in config.yml");
		}
	}

	/** A config from Gods 0.9.x: obsolete settings are dropped, and the old spelling of the item blessing power is read. */
	@Test
	void upgradesAGods09Config() throws Exception
	{
		YamlConfiguration migrated = migrate(TestConfigs.yaml(fixture("config-0.9.yml")));

		for (String obsolete : ConfigV1ToV2.OBSOLETE)
		{
			assertFalse(migrated.contains(obsolete), obsolete);
		}
		for (String section : List.of("Settings", "Altars", "ItemBlessing", "Blessing", "Cursing", "Quests", "Sacrifices", "Commandments", "HolyLand", "ChatFormatting", "Bibles", "Prophecies", "HolyArtifacts", "Marriage", "Prayers"))
		{
			assertFalse(migrated.contains(section), "old section " + section + " is left");
		}
		assertTrue(this.log.warnings().isEmpty(), "Warnings: " + this.log.warnings());

		// And on to version 3, which leaves the old default altar blocks to the themes
		migrated = new ConfigV2ToV3(TestConfigs::resource).migrate(migrated);
		Settings settings = TestConfigs.load(TestConfigs.yaml(migrated.saveToString()), this.log);
		assertEquals(5, settings.itemBlessings().minGodPower());
		assertTrue(settings.gods().blacklist());
		assertTrue(settings.holyLand().enabled());
		assertTrue(settings.quests().enabled());
		assertEquals(List.of("world", "world_nether", "world_the_end"), settings.general().worlds());

		// Everything else was at its default, and settings 0.9.x didn't have get theirs
		Settings defaults = TestConfigs.load(new YamlConfiguration(), new TestConfigs.CapturingLogger());
		assertEquals(defaults.altars(), settings.altars());
		assertEquals(defaults.quests().rewardValues(), settings.quests().rewardValues());
		assertEquals(defaults.holyLand().breakableBlocks(), settings.holyLand().breakableBlocks());
		assertEquals(defaults.prayers(), settings.prayers());
		assertEquals(defaults.blessings(), settings.blessings());
		assertEquals(defaults.curses(), settings.curses());
		assertEquals(defaults.priests(), settings.priests());
		assertEquals(defaults.believers(), settings.believers());
		assertEquals(defaults.bibles(), settings.bibles());
		assertEquals(defaults.holyArtifacts(), settings.holyArtifacts());
	}

	@Test
	void theCorrectSpellingWinsOverTheOldOne() throws Exception
	{
		YamlConfiguration migrated = migrate(TestConfigs.yaml("""
				ItemBlessing:
				  MinGodPowerForItemBlessings: 8
				  MinGodPowerItemBlessings: 5
				"""));
		assertEquals(8, migrated.getInt("item-blessings.min-god-power"));
	}

	/** Old configs without these sections, or with empty ones, used the built-in defaults, which config.yml now holds. */
	@Test
	void missingOrEmptyListsGetTheDefaults() throws Exception
	{
		YamlConfiguration migrated = migrate(TestConfigs.yaml("""
				HolyLand:
				  BreakableBlockTypes: []
				Altars:
				  BlockTypes: {}
				"""));
		YamlConfiguration bundled = TestConfigs.bundled();
		assertEquals(bundled.getStringList("holy-land.breakable-blocks"), migrated.getStringList("holy-land.breakable-blocks"));
		assertEquals(bundled.getConfigurationSection("altars.block-types").getValues(true), migrated.getConfigurationSection("altars.block-types").getValues(true));
		assertEquals(bundled.getConfigurationSection("quests.reward-values").getValues(true), migrated.getConfigurationSection("quests.reward-values").getValues(true));
	}

	@Test
	void anEmptyOldConfigGivesTheDefaults() throws Exception
	{
		Settings settings = TestConfigs.load(TestConfigs.yaml(migrate(new YamlConfiguration()).saveToString()), this.log);
		assertEquals(TestConfigs.load(new YamlConfiguration(), new TestConfigs.CapturingLogger()), settings);
	}

	@Test
	void keepsTheBundledComments() throws Exception
	{
		YamlConfiguration migrated = migrate(TestConfigs.yaml(fixture("config-1.0-pre.yml")));
		assertFalse(migrated.getComments("blessings.blessed-seconds").isEmpty());
		// Sections replaced with the admin's entries keep their comment too
		assertFalse(migrated.getComments("altars.block-types").isEmpty());
		assertFalse(migrated.getComments("quests.reward-values").isEmpty());
	}

	@Test
	void mentionsUnknownSettings() throws Exception
	{
		migrate(TestConfigs.yaml("Settings:\n  Debug: true\n  Debgu: false\n"));
		assertTrue(this.log.warnings().stream().anyMatch(warning -> warning.contains("Settings.Debgu")), "Warnings: " + this.log.warnings());
	}

	/** How Gods loads config.yml: the old file is upgraded in place and kept as config.yml.v1.bak. */
	@Test
	void upgradesTheFileOnDiskAndKeepsABackup() throws Exception
	{
		Path file = this.dir.resolve("config.yml");
		String old = fixture("config-0.9.yml");
		Files.writeString(file, old);

		VersionedYaml format = new VersionedYaml("config-version", Gods.CONFIG_VERSION, 1, List.of(new ConfigV1ToV2(TestConfigs::resource, this.log.logger()), new ConfigV2ToV3(TestConfigs::resource)));
		YamlConfiguration loaded = format.load(file.toFile(), this.log.logger());

		assertEquals(Gods.CONFIG_VERSION, loaded.getInt("config-version"));
		assertEquals(old, Files.readString(this.dir.resolve("config.yml.v1.bak")));
		YamlConfiguration onDisk = YamlConfiguration.loadConfiguration(file.toFile());
		assertEquals(Gods.CONFIG_VERSION, onDisk.getInt("config-version"));
		assertEquals(List.of("world", "world_nether", "world_the_end"), onDisk.getStringList("general.worlds"));
		assertTrue(Files.readString(file).contains("# Gods configuration"), "the bundled header comment is kept");

		// Loading it again changes nothing
		String upgraded = Files.readString(file);
		format.load(file.toFile(), this.log.logger());
		assertEquals(upgraded, Files.readString(file));
	}

	/** What Gods.loadSettings uses */
	private VersionedYaml format()
	{
		return new VersionedYaml("config-version", Gods.CONFIG_VERSION, 1, List.of(new ConfigV1ToV2(TestConfigs::resource, this.log.logger()), new ConfigV2ToV3(TestConfigs::resource)), ConfigV1ToV2::isVersion2Layout);
	}

	/** A version 2 config whose config-version was removed by hand is not reset to the defaults */
	@Test
	void aVersion2ConfigWithoutItsVersionKeepsItsSettings() throws Exception
	{
		Path file = this.dir.resolve("config.yml");
		Files.writeString(file, "# My server\ngeneral:\n  debug: true\n  language: danish\nblessings:\n  blessed-seconds: 77\n");

		YamlConfiguration loaded = format().load(file.toFile(), this.log.logger());

		assertEquals("danish", loaded.getString("general.language"));
		assertEquals(77, loaded.getInt("blessings.blessed-seconds"));
		assertEquals(Gods.CONFIG_VERSION, loaded.getInt("config-version"));
		YamlConfiguration onDisk = YamlConfiguration.loadConfiguration(file.toFile());
		assertEquals(Gods.CONFIG_VERSION, onDisk.getInt("config-version"));
		assertEquals(77, onDisk.getInt("blessings.blessed-seconds"));
		assertTrue(Files.readString(file).contains("# My server"), "comments stay");
		try (var files = Files.list(this.dir))
		{
			assertEquals(List.of("config.yml"), files.map(path -> path.getFileName().toString()).toList(), "nothing was upgraded");
		}
	}

	@Test
	void aConfigFromANewerGodsIsReadButNeverWritten() throws Exception
	{
		Path file = this.dir.resolve("config.yml");
		String newer = "config-version: " + (Gods.CONFIG_VERSION + 1) + "\ngeneral:\n  language: danish\n";
		Files.writeString(file, newer);

		VersionedYaml format = format();
		YamlConfiguration loaded = format.load(file.toFile(), this.log.logger());

		assertEquals("danish", loaded.getString("general.language"));
		assertTrue(format.isNewer(loaded), "Gods then skips adding missing settings");
		assertEquals(newer, Files.readString(file));
	}

	@Test
	void anOldConfigIsNotMistakenForVersion2() throws Exception
	{
		assertFalse(ConfigV1ToV2.isVersion2Layout(TestConfigs.yaml("Settings:\n  Debug: true\nHolyLand:\n  Enabled: true\n")));
		assertTrue(ConfigV1ToV2.isVersion2Layout(TestConfigs.yaml("general:\n  debug: true\n")));
	}
}
