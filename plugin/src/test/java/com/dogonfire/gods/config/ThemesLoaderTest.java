package com.dogonfire.gods.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import com.dogonfire.gods.model.GodType;

class ThemesLoaderTest
{
	private final TestConfigs.CapturingLogger	log		= new TestConfigs.CapturingLogger();
	private final List<String>					debug	= new ArrayList<>();

	private static YamlConfiguration bundled()
	{
		return YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(TestConfigs.resource("themes.yml")), StandardCharsets.UTF_8));
	}

	/** The names of the newest API (the one Gods compiles against), but without {@code missing} */
	private static ThemesLoader.Names namesWithout(Set<String> missing, Set<String> otherVersions)
	{
		ThemesLoader.Names all = ThemesLoader.serverNames(otherVersions);
		return new ThemesLoader.Names()
		{
			@Override
			public Material material(String name)
			{
				return missing.contains(name) ? null : all.material(name);
			}

			@Override
			public EntityType entityType(String name)
			{
				return missing.contains(name) ? null : all.entityType(name);
			}

			@Override
			public boolean existsInOtherVersions(String name)
			{
				return all.existsInOtherVersions(name);
			}

			// Material.isBlock() needs a server; good enough for the names in these tests
			@Override
			public boolean isBlock(Material material)
			{
				return !material.name().endsWith("_SWORD");
			}

			@Override
			public boolean isItem(Material material)
			{
				return true;
			}
		};
	}

	private Themes load(String overrides) throws Exception
	{
		return load(overrides, namesWithout(Set.of(), Set.of()));
	}

	private Themes load(String overrides, ThemesLoader.Names names) throws Exception
	{
		return new ThemesLoader(names, this.log.logger(), this.debug::add).load(bundled(), TestConfigs.yaml(overrides), Settings.Altars.of(Map.of()));
	}

	@Test
	void everyNameInTheBundledThemesExists() throws Exception
	{
		load("");
		assertTrue(this.log.warnings().isEmpty(), "Warnings: " + this.log.warnings());
		assertTrue(this.debug.isEmpty(), "Skipped: " + this.debug);
	}

	@Test
	void everyGodTypeHasEveryList() throws Exception
	{
		Themes themes = load("");
		for (GodType type : GodType.values())
		{
			Theme theme = themes.of(type);
			assertFalse(theme.holyMobs().isEmpty(), type + " holy mobs");
			assertFalse(theme.unholyMobs().isEmpty(), type + " unholy mobs");
			assertFalse(theme.holyFoods().isEmpty(), type + " holy foods");
			assertFalse(theme.unholyFoods().isEmpty(), type + " unholy foods");
			assertFalse(theme.sacrificeNeeds().isEmpty(), type + " sacrifice needs");
			assertFalse(theme.slayTargets().isEmpty(), type + " slay targets");
			// A god must be able to pick a holy and a different unholy mob and food
			Set<EntityType> mobs = new HashSet<>(theme.holyMobs());
			mobs.addAll(theme.unholyMobs());
			assertTrue(mobs.size() > 1, type + " mobs");
			Set<Material> foods = new HashSet<>(theme.holyFoods());
			foods.addAll(theme.unholyFoods());
			assertTrue(foods.size() > 1, type + " foods");
			assertEquals(ThemesLoader.sectionName(type), theme.sign().messages());
			// As before, no altar makes a werewolf god
			assertFalse(theme.altarBlocks().isEmpty(), type + " altar blocks");
		}
		assertFalse(themes.of(null).holyMobs().isEmpty());
		assertEquals(Theme.SignStyle.PLAIN, themes.of(null).sign());
	}

	/** The altar blocks of 0.9.x still make the same god types, and no block makes two god types */
	@Test
	void altarBlocksKeepTheOldOnesAndDontOverlap() throws Exception
	{
		Themes themes = load("");
		Map<GodType, Material> old = new EnumMap<>(GodType.class);
		old.put(GodType.FROST, Material.SNOW_BLOCK);
		old.put(GodType.LOVE, Material.DIAMOND_BLOCK);
		old.put(GodType.EVIL, Material.OBSIDIAN);
		old.put(GodType.SEA, Material.LAPIS_BLOCK);
		old.put(GodType.MOON, Material.END_STONE);
		old.put(GodType.SUN, Material.SANDSTONE);
		old.put(GodType.THUNDER, Material.QUARTZ_BLOCK);
		old.put(GodType.PARTY, Material.EMERALD_BLOCK);
		old.put(GodType.WAR, Material.NETHERRACK);
		old.put(GodType.CREATURES, Material.OAK_LOG);
		old.put(GodType.WISDOM, Material.BOOKSHELF);
		old.put(GodType.NATURE, Material.MELON);
		old.forEach((type, block) -> assertEquals(List.of(type), themes.altars().godTypesFor(block), block.name()));

		Map<Material, GodType> seen = new HashMap<>();
		for (GodType type : GodType.values())
		{
			for (Material block : themes.of(type).altarBlocks())
			{
				GodType other = seen.put(block, type);
				assertNull(other, block + " is an altar block of " + other + " and " + type);
			}
		}
		// Every copper block, however oxidized or waxed, so a thunder altar keeps working as it ages
		assertTrue(themes.of(GodType.THUNDER).altarBlocks().containsAll(List.of(Material.COPPER_BLOCK, Material.OXIDIZED_COPPER, Material.WAXED_WEATHERED_CUT_COPPER, Material.WAXED_OXIDIZED_CHISELED_COPPER)));
	}

	@Test
	void aListReplacesAndAddRemoveChanges() throws Exception
	{
		Themes themes = load("""
				frost:
				  holy-mobs: [polar_bear, minecraft:fox]
				  holy-foods:
				    add: [pumpkin_pie, cooked_cod]
				    remove: [COOKED_COD, sweet_berries]
				  sign:
				    dye: blue
				    glowing: false
				""");
		Theme frost = themes.of(GodType.FROST);
		assertEquals(List.of(EntityType.POLAR_BEAR, EntityType.FOX), frost.holyMobs());
		assertEquals(List.of(Material.COOKED_SALMON, Material.RABBIT_STEW, Material.COOKED_RABBIT, Material.BEETROOT_SOUP, Material.APPLE, Material.PUMPKIN_PIE, Material.COOKED_COD), frost.holyFoods());
		assertEquals(new Theme.SignStyle("frost", DyeColor.BLUE, false), frost.sign());
		// The rest stays Gods' own
		assertEquals(load("").of(GodType.FROST).unholyMobs(), frost.unholyMobs());
		assertEquals(load("").of(GodType.SEA), themes.of(GodType.SEA));
		assertTrue(this.log.warnings().isEmpty(), "Warnings: " + this.log.warnings());
	}

	/** A key a god type leaves out comes from default, with the admin's changes to default */
	@Test
	void theDefaultFillsTheGaps() throws Exception
	{
		YamlConfiguration bundled = TestConfigs.yaml("""
				default:
				  holy-mobs: [cow]
				  unholy-mobs: [zombie]
				  sign:
				    dye: white
				sea:
				  holy-mobs: [dolphin]
				  sign:
				    messages: sea
				""");
		Themes themes = new ThemesLoader(namesWithout(Set.of(), Set.of()), this.log.logger(), this.debug::add).load(bundled, TestConfigs.yaml("""
				default:
				  unholy-mobs:
				    add: [drowned]
				sea:
				  holy-mobs:
				    add: [turtle]
				frost:
				  holy-mobs:
				    add: [fox]
				"""), Settings.Altars.of(Map.of()));

		assertEquals(List.of(EntityType.DOLPHIN, EntityType.TURTLE), themes.of(GodType.SEA).holyMobs());
		assertEquals(List.of(EntityType.ZOMBIE, EntityType.DROWNED), themes.of(GodType.SEA).unholyMobs());
		assertEquals(new Theme.SignStyle("sea", DyeColor.WHITE, false), themes.of(GodType.SEA).sign());
		// Adding to a list frost doesn't have adds to the default's
		assertEquals(List.of(EntityType.COW, EntityType.FOX), themes.of(GodType.FROST).holyMobs());
		assertEquals(List.of(EntityType.COW), themes.of(null).holyMobs());
		assertEquals(List.of(EntityType.ZOMBIE, EntityType.DROWNED), themes.of(null).unholyMobs());
	}

	/**
	 * Names the server doesn't have are skipped: quietly if Gods' own file or another Minecraft version has them,
	 * with a warning if they are typos.
	 */
	@Test
	void namesTheServerLacksAreSkipped() throws Exception
	{
		ThemesLoader.Names oldServer = namesWithout(Set.of("HAPPY_GHAST", "COPPER_GOLEM", "CREAKING"), Set.of("CREAKING"));
		Themes themes = load("""
				nature:
				  holy-mobs:
				    add: [creaking, polar_bare]
				  altar-blocks:
				    add: [diamond_sword, end_stone_brick]
				""", oldServer);

		assertFalse(themes.of(GodType.PARTY).holyMobs().contains(EntityType.HAPPY_GHAST));
		assertTrue(themes.of(GodType.PARTY).holyMobs().contains(EntityType.PARROT));
		assertFalse(themes.of(GodType.NATURE).holyMobs().contains(EntityType.CREAKING));
		assertTrue(this.debug.stream().anyMatch(message -> message.contains("happy_ghast")), "Debug: " + this.debug);
		assertTrue(this.debug.stream().anyMatch(message -> message.contains("creaking")), "Debug: " + this.debug);
		assertEquals(3, this.log.warnings().size(), "Warnings: " + this.log.warnings());
		assertTrue(this.log.warnings().stream().anyMatch(warning -> warning.contains("polar_bare")));
		assertTrue(this.log.warnings().stream().anyMatch(warning -> warning.contains("diamond_sword") && warning.contains("not a block")));
		assertTrue(this.log.warnings().stream().anyMatch(warning -> warning.contains("end_stone_brick")));
	}

	@Test
	void unknownSectionsAndKeysAreMentioned() throws Exception
	{
		load("""
				frots:
				  holy-mobs: [fox]
				sea:
				  holy-mob: [fox]
				  sign:
				    dye: sky
				    colour: blue
				""");
		assertTrue(this.log.warnings().stream().anyMatch(warning -> warning.contains("frots")), "Warnings: " + this.log.warnings());
		assertTrue(this.log.warnings().stream().anyMatch(warning -> warning.contains("holy-mob")));
		assertTrue(this.log.warnings().stream().anyMatch(warning -> warning.contains("sky")));
		assertTrue(this.log.warnings().stream().anyMatch(warning -> warning.contains("colour")));
	}

	/** config.yml's altars.block-types wins for the god types it lists, even with an empty list */
	@Test
	void configuredAltarBlocksReplaceTheThemes() throws Exception
	{
		Map<GodType, List<Material>> configured = new EnumMap<>(GodType.class);
		configured.put(GodType.FROST, List.of(Material.ICE));
		configured.put(GodType.SEA, List.of());
		Themes themes = new ThemesLoader(namesWithout(Set.of(), Set.of()), this.log.logger(), this.debug::add).load(bundled(), new YamlConfiguration(), Settings.Altars.of(configured));

		assertEquals(List.of(Material.ICE), themes.altars().blocksFor(GodType.FROST));
		assertEquals(List.of(), themes.altars().blocksFor(GodType.SEA));
		assertEquals(List.of(), themes.altars().godTypesFor(Material.SNOW_BLOCK));
		assertEquals(List.of(), themes.altars().godTypesFor(Material.LAPIS_BLOCK));
		assertEquals(List.of(GodType.MOON), themes.altars().godTypesFor(Material.PURPUR_BLOCK));
	}

	@Test
	void theVersionNamesAreBundled() throws Exception
	{
		Set<String> names = ThemesLoader.readVersionNames(TestConfigs::resource);
		assertTrue(names.containsAll(Set.of("HAPPY_GHAST", "COPPER_GOLEM", "COPPER_TORCH", "COPPER_LANTERN")), "" + names.size());
		assertFalse(names.contains("POLAR_BEAR"));
	}

	@Test
	void namesAreNormalized()
	{
		assertEquals("COOKED_COD", ThemesLoader.normalize(" minecraft:cooked_cod "));
		assertEquals("COOKED_COD", ThemesLoader.normalize("Cooked-Cod"));
		assertEquals("COOKED_COD", ThemesLoader.normalize("cooked cod"));
	}
}
