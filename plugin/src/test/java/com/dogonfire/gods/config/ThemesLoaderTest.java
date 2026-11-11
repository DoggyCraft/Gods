package com.dogonfire.gods.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import com.dogonfire.gods.integration.npc.SkinTextures;
import com.dogonfire.gods.model.GodType;
import com.dogonfire.gods.model.NpcSkin;

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

	/** The bundled skins: signed textures that decode to a skin URL, where Gods' themes have them, and only there */
	@Test
	void theBundledSkinsAreSignedTextures() throws Exception
	{
		Themes themes = load("");
		assertTrue(this.log.warnings().isEmpty(), "Warnings: " + this.log.warnings());
		assertBundledSkin(themes.defaults().npcSkins().god(), false);
		assertBundledSkin(themes.defaults().npcSkins().goddess(), true);
		assertNull(themes.defaults().npcSkins().deity());
		assertBundledSkin(themes.of(GodType.WEREWOLVES).npcSkins().god(), false);
		assertBundledSkin(themes.of(GodType.WEREWOLVES).npcSkins().goddess(), true);
		assertBundledSkin(themes.of(GodType.EVIL).npcSkins().god(), false);
		assertEquals(themes.of(GodType.EVIL).npcSkins().god(), themes.of(GodType.EVIL).npcSkins().goddess());
		assertBundledSkin(themes.of(GodType.THUNDER).npcSkins().god(), false);
		assertBundledSkin(themes.of(GodType.LOVE).npcSkins().goddess(), false);
		assertBundledSkin(themes.of(GodType.SEA).npcSkins().god(), false);
		assertBundledSkin(themes.of(GodType.SEA).npcSkins().goddess(), true);
		// The werewolf alpha is the account WerewolfAlpha's texture
		assertTrue(new String(Base64.getDecoder().decode(themes.of(GodType.WEREWOLVES).npcSkins().goddess().value()), StandardCharsets.UTF_8).contains("\"WerewolfAlpha\""));
		Map<GodType, Theme.NpcSkins> expected = Map.of(
				GodType.WEREWOLVES, new Theme.NpcSkins(themes.of(GodType.WEREWOLVES).npcSkins().god(), themes.of(GodType.WEREWOLVES).npcSkins().goddess(), null),
				GodType.EVIL, new Theme.NpcSkins(themes.of(GodType.EVIL).npcSkins().god(), themes.of(GodType.EVIL).npcSkins().god(), null),
				GodType.THUNDER, new Theme.NpcSkins(themes.of(GodType.THUNDER).npcSkins().god(), null, null),
				GodType.LOVE, new Theme.NpcSkins(null, themes.of(GodType.LOVE).npcSkins().goddess(), null),
				GodType.SEA, new Theme.NpcSkins(themes.of(GodType.SEA).npcSkins().god(), themes.of(GodType.SEA).npcSkins().goddess(), null));
		for (GodType type : GodType.values())
		{
			assertEquals(expected.getOrDefault(type, Theme.NpcSkins.NONE), themes.of(type).npcSkins(), type.name());
		}
	}

	private static void assertBundledSkin(NpcSkin skin, boolean slim)
	{
		assertNotNull(skin);
		assertTrue(skin.isSigned() && skin.name() == null, skin.toString());
		String json = new String(Base64.getDecoder().decode(skin.value()), StandardCharsets.UTF_8);
		com.google.gson.JsonObject textures = com.google.gson.JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("textures");
		assertTrue(textures.getAsJsonObject("SKIN").get("url").getAsString().startsWith("http://textures.minecraft.net/texture/"), json);
		SkinTextures.Texture texture = SkinTextures.decode(skin.value());
		assertNotNull(texture);
		assertEquals(slim, texture.slim(), json);
	}

	private static String texture(String id)
	{
		return SkinTextures.encode(new SkinTextures.Texture("http://textures.minecraft.net/texture/" + id, false));
	}

	/** A signed texture per gender; a section doesn't take the default section's (that is GodNpcRules.skin's) */
	@Test
	void npcSkinsArePerGender() throws Exception
	{
		String god = texture("god");
		String goddess = texture("goddess");
		String deity = texture("deity");
		Themes themes = load("""
				moon:
				  npc:
				    skin:
				      god:
				        value: %s
				        signature: sig0
				      goddess:
				        value: %s
				        signature: sig1
				      deity:
				        value: %s
				        signature: sig2
				""".formatted(god, goddess, deity));
		assertTrue(this.log.warnings().isEmpty(), "Warnings: " + this.log.warnings());
		assertEquals(new Theme.NpcSkins(new NpcSkin(null, god, "sig0"), new NpcSkin(null, goddess, "sig1"), new NpcSkin(null, deity, "sig2")), themes.of(GodType.MOON).npcSkins());
		assertTrue(themes.of(GodType.FROST).npcSkins().isEmpty());
	}

	/** The admin's file replaces each gender entry it lists, keeps the others; '' removes one, skin: '' all */
	@Test
	void overridesReplaceSkinsPerGender() throws Exception
	{
		Themes bundledThemes = load("");
		NpcSkin werewolfGod = bundledThemes.of(GodType.WEREWOLVES).npcSkins().god();
		NpcSkin defaultGoddess = bundledThemes.defaults().npcSkins().goddess();
		NpcSkin evil = bundledThemes.of(GodType.EVIL).npcSkins().god();
		String mine = texture("mine");
		Themes themes = load("""
				werewolves:
				  npc:
				    skin:
				      goddess:
				        value: %1$s
				        signature: sig
				      deity:
				        value: %1$s
				        signature: sig
				default:
				  npc:
				    skin:
				      god: ''
				evil:
				  npc:
				    skin:
				      god:
				        value: %1$s
				        signature: sig
				thunder:
				  npc:
				    skin: ''
				""".formatted(mine));
		assertTrue(this.log.warnings().isEmpty(), "Warnings: " + this.log.warnings());
		NpcSkin own = new NpcSkin(null, mine, "sig");
		assertEquals(new Theme.NpcSkins(werewolfGod, own, own), themes.of(GodType.WEREWOLVES).npcSkins());
		assertEquals(new Theme.NpcSkins(null, defaultGoddess, null), themes.defaults().npcSkins());
		assertEquals(new Theme.NpcSkins(own, evil, null), themes.of(GodType.EVIL).npcSkins());
		assertTrue(themes.of(GodType.THUNDER).npcSkins().isEmpty());
	}

	/**
	 * Player names, textures without a signature, values that aren't textures and a texture without a gender are
	 * warned about (once, with a hint) and ignored
	 */
	@Test
	void badNpcSkinsAreMentionedAndIgnored() throws Exception
	{
		Themes bundledThemes = load("");
		this.log.warnings().clear();
		Themes themes = load("""
				sun:
				  npc:
				    skin: Notch
				werewolves:
				  npc:
				    skin:
				      god: Notch
				      godess:
				        value: %s
				        signature: sig
				love:
				  npc:
				    skin:
				      goddess:
				        value: %s
				party:
				  npc:
				    skin:
				      deity:
				        name: Notch
				war:
				  npc:
				    skin:
				      deity:
				        value: not-a-texture
				        signature: sig
				      add: [Notch]
				nature:
				  npc:
				    skin:
				      value: %s
				      signature: sig
				moon:
				  npc: Notch
				sea:
				  npc:
				    skn: Notch
				""".formatted(texture("a"), texture("b"), texture("c")));
		assertTrue(themes.of(GodType.SUN).npcSkins().isEmpty());
		assertEquals(bundledThemes.of(GodType.WEREWOLVES).npcSkins(), themes.of(GodType.WEREWOLVES).npcSkins());
		// Gods' own love goddess stays
		assertEquals(bundledThemes.of(GodType.LOVE).npcSkins(), themes.of(GodType.LOVE).npcSkins());
		assertTrue(themes.of(GodType.PARTY).npcSkins().isEmpty());
		assertTrue(themes.of(GodType.WAR).npcSkins().isEmpty());
		assertTrue(themes.of(GodType.NATURE).npcSkins().isEmpty());
		for (String expected : new String[] { "sun.npc.skin is a player name ('Notch')", "werewolves.npc.skin.god is a player name", "godess",
				"love.npc.skin.goddess has no signature", "party.npc.skin.deity is a player name", "war.npc.skin.deity.value is not a skin texture", "'add'",
				"nature.npc.skin needs a skin per gender", "moon.npc", "skn" })
		{
			assertTrue(this.log.warnings().stream().anyMatch(warning -> warning.contains(expected)), expected + " in " + this.log.warnings());
		}
		assertTrue(this.log.warnings().stream().filter(warning -> warning.contains("player name") || warning.contains("no signature"))
				.allMatch(warning -> warning.contains("mineskin.org") && warning.contains("/g godskin")), "" + this.log.warnings());
		// Once each
		assertEquals(1, this.log.warnings().stream().filter(warning -> warning.contains("sun.npc.skin")).count());
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


	/** Every god type has its own effects: a symbol and, for each mood, particles, sounds and colors */
	@Test
	void everyGodTypeHasEffects() throws Exception
	{
		Themes themes = load("");
		for (GodType type : GodType.values())
		{
			Theme.Effects effects = themes.of(type).effects();
			assertFalse(effects.symbol().isBlank(), type + " symbol");
			for (Theme.EffectMood mood : Theme.EffectMood.values())
			{
				Theme.EffectStyle style = effects.of(mood);
				assertFalse(style.particles().isEmpty(), type + " " + mood + " particles");
				assertFalse(style.sounds().isEmpty(), type + " " + mood + " sounds");
				assertFalse(style.colors().isEmpty(), type + " " + mood + " colors");
				style.particles().forEach(particle -> assertTrue(ThemesLoader.isShowable(particle), type + " " + particle));
			}
			assertFalse(effects.equals(themes.of(null).effects()), type + " has the default effects");
		}
		Theme.Effects frost = themes.of(GodType.FROST).effects();
		assertEquals("❄", frost.symbol());
		assertTrue(frost.of(Theme.EffectMood.PLEASED).particles().contains(Particle.SNOWFLAKE));
		assertTrue(frost.of(Theme.EffectMood.PLEASED).sounds().contains("minecraft:block.amethyst_block.chime"));
		assertEquals(Color.fromRGB(0xAEE8FF), frost.of(Theme.EffectMood.PLEASED).colors().get(0));
	}

	/**
	 * Every sound the bundled themes name exists in the newest API (a sound's constant is its id in upper case with _
	 * for .), except the ones named for older servers next to a newer one
	 */
	@Test
	void everyBundledSoundExists() throws Exception
	{
		Set<String> olderOnly = Set.of("entity.wolf.howl");
		YamlConfiguration bundled = bundled();
		List<String> missing = new ArrayList<>();
		for (String key : bundled.getKeys(true))
		{
			if (!key.endsWith(".sounds"))
			{
				continue;
			}
			for (String sound : bundled.getStringList(key))
			{
				try
				{
					org.bukkit.Sound.class.getField(sound.toUpperCase(java.util.Locale.ROOT).replace('.', '_'));
				}
				catch (NoSuchFieldException ex)
				{
					if (!olderOnly.contains(sound))
					{
						missing.add(key + ": " + sound);
					}
				}
			}
		}
		assertTrue(missing.isEmpty(), "" + missing);
	}

	@Test
	void effectsAreChangedLikeTheOtherLists() throws Exception
	{
		Themes themes = load("""
				frost:
				  effects:
				    symbol: "*"
				    pleased:
				      particles: [cloud, minecraft:end_rod]
				      sounds:
				        add: [block.bell.use]
				      colors: ["#FF0000", red]
				""");
		Theme.Effects frost = themes.of(GodType.FROST).effects();
		Theme.Effects bundled = load("").of(GodType.FROST).effects();
		assertEquals("*", frost.symbol());
		assertEquals(List.of(Particle.CLOUD, Particle.END_ROD), frost.of(Theme.EffectMood.PLEASED).particles());
		assertTrue(frost.of(Theme.EffectMood.PLEASED).sounds().containsAll(bundled.of(Theme.EffectMood.PLEASED).sounds()));
		assertTrue(frost.of(Theme.EffectMood.PLEASED).sounds().contains("minecraft:block.bell.use"));
		assertEquals(List.of(Color.fromRGB(0xFF0000), DyeColor.RED.getColor()), frost.of(Theme.EffectMood.PLEASED).colors());
		// The rest stays Gods' own
		assertEquals(bundled.of(Theme.EffectMood.ANGRY), frost.of(Theme.EffectMood.ANGRY));
		assertEquals(load("").of(GodType.SEA), themes.of(GodType.SEA));
		assertTrue(this.log.warnings().isEmpty(), "Warnings: " + this.log.warnings());
	}

	/** A god type that leaves out effects gets the default's */
	@Test
	void theDefaultFillsTheEffects() throws Exception
	{
		YamlConfiguration bundled = TestConfigs.yaml("""
				default:
				  effects:
				    symbol: "+"
				    angry:
				      particles: [smoke]
				sea:
				  effects:
				    angry:
				      sounds: [entity.guardian.ambient]
				""");
		Themes themes = new ThemesLoader(namesWithout(Set.of(), Set.of()), this.log.logger(), this.debug::add).load(bundled, new YamlConfiguration(), Settings.Altars.of(Map.of()));
		Theme.Effects sea = themes.of(GodType.SEA).effects();
		assertEquals("+", sea.symbol());
		assertEquals(List.of(Particle.SMOKE), sea.of(Theme.EffectMood.ANGRY).particles());
		assertEquals(List.of("minecraft:entity.guardian.ambient"), sea.of(Theme.EffectMood.ANGRY).sounds());
		assertEquals(Theme.EffectStyle.NONE, sea.of(Theme.EffectMood.PLEASED));
	}

	@Test
	void unknownEffectsAreMentioned() throws Exception
	{
		load("""
				sea:
				  effects:
				    happy:
				      particles: [end_rod]
				    pleased:
				      particle: [end_rod]
				      particles: [not_a_particle, block, end_rod]
				      colors: [sky, "#12345"]
				""");
		List<String> warnings = this.log.warnings();
		assertTrue(warnings.stream().anyMatch(warning -> warning.contains("'happy'")), "Warnings: " + warnings);
		assertTrue(warnings.stream().anyMatch(warning -> warning.contains("'particle'")));
		assertTrue(warnings.stream().anyMatch(warning -> warning.contains("not_a_particle")));
		assertTrue(warnings.stream().anyMatch(warning -> warning.contains("'block'") && warning.contains("needs a block")));
		assertTrue(warnings.stream().anyMatch(warning -> warning.contains("sky")));
		assertTrue(warnings.stream().anyMatch(warning -> warning.contains("#12345")));
	}

	/** Colors are dye colors or #RRGGBB */
	@Test
	void colorsAreParsed()
	{
		assertEquals(DyeColor.LIGHT_BLUE.getColor(), ThemesLoader.color("light_blue"));
		assertEquals(Color.fromRGB(0xAEE8FF), ThemesLoader.color("#aee8ff"));
		assertNull(ThemesLoader.color("#xyz123"));
		assertNull(ThemesLoader.color("sky"));
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
