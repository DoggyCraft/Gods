package com.dogonfire.gods.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import com.dogonfire.gods.model.GodType;

class SettingsLoaderTest
{
	private final TestConfigs.CapturingLogger log = new TestConfigs.CapturingLogger();

	private Settings load(String yaml) throws Exception
	{
		return TestConfigs.load(TestConfigs.yaml(yaml), this.log);
	}

	private void assertWarned(String key)
	{
		assertTrue(this.log.warnings().stream().anyMatch(warning -> warning.contains(key)), "No warning about " + key + " in " + this.log.warnings());
	}

	@Test
	void wrongTypesGetTheDefault() throws Exception
	{
		Settings settings = load("""
				general:
				  debug: yes please
				prayers:
				  power-cost:
				    item: lots
				gods:
				  verbosity: loud
				""");

		assertFalse(settings.general().debug());
		assertEquals(20, settings.prayers().itemCost());
		assertEquals(1.0D, settings.gods().verbosity());
		assertWarned("general.debug");
		assertWarned("prayers.power-cost.item");
		assertWarned("gods.verbosity");
	}

	@Test
	void negativeNumbersAreClamped() throws Exception
	{
		Settings settings = load("""
				curses:
				  cursed-minutes: -5
				gods:
				  mood-falloff: -0.5
				""");

		assertEquals(0, settings.curses().cursedMinutes());
		assertEquals(0.0D, settings.gods().moodFalloff());
		assertWarned("curses.cursed-minutes");
		assertWarned("gods.mood-falloff");
	}

	@Test
	void validValuesAreUsedAsTheyAre() throws Exception
	{
		Settings settings = load("""
				blessings:
				  blessed-seconds: 30
				priests:
				  assign-command: say hi $PlayerName
				general:
				  server-name: 1234
				""");

		assertEquals(30, settings.blessings().blessedSeconds());
		assertEquals("say hi $PlayerName", settings.priests().assignCommand());
		assertEquals("1234", settings.general().serverName());
		assertTrue(this.log.warnings().isEmpty(), "Warnings: " + this.log.warnings());
	}

	@Test
	void unknownBlocksAndGodTypesAreSkipped() throws Exception
	{
		Settings settings = load("""
				altars:
				  block-types:
				    frost: [snow_block, not_a_block, PACKED_ICE]
				    dragons: [dragon_egg]
				holy-land:
				  breakable-blocks: [gravel, minecraft:dirt, ROCK]
				""");

		assertEquals(List.of(Material.SNOW_BLOCK, Material.PACKED_ICE), settings.altars().blocksFor(GodType.FROST));
		assertEquals(List.of(GodType.FROST), settings.altars().godTypesFor(Material.PACKED_ICE));
		assertEquals(Set.of(Material.GRAVEL, Material.DIRT), settings.holyLand().breakableBlocks());
		assertWarned("not_a_block");
		assertWarned("dragons");
		assertWarned("ROCK");
	}

	/** An altars section the admin has is used as it is: god types left out of it can't be made. */
	@Test
	void godTypesMissingFromTheAltarsSectionHaveNoBlocks() throws Exception
	{
		Settings settings = load("""
				altars:
				  block-types:
				    love: [diamond_block]
				    party: [diamond_block]
				""");

		assertTrue(settings.altars().blocksFor(GodType.FROST).isEmpty());
		assertTrue(settings.altars().godTypesFor(Material.SNOW_BLOCK).isEmpty());
		assertEquals(List.of(GodType.LOVE, GodType.PARTY), settings.altars().godTypesFor(Material.DIAMOND_BLOCK));
	}

	/** altars.require-claim is read with the altar blocks, and kept by the themes' combined altars */
	@Test
	void requireClaimCanBeSwitchedOff() throws Exception
	{
		assertTrue(load("").altars().requireClaim());
		Settings settings = load("""
				altars:
				  require-claim: false
				""");
		assertFalse(settings.altars().requireClaim());
		assertFalse(Themes.combineAltars(settings.altars(), java.util.Map.of()).requireClaim());
	}

	@Test
	void questRewardsWithoutValidEntriesUseTheDefaults() throws Exception
	{
		Settings settings = load("""
				quests:
				  reward-values:
				    unobtainium: 10
				""");

		assertEquals(BundledConfigTest.oldDefaults().quests().rewardValues(), settings.quests().rewardValues());
		assertWarned("unobtainium");
		assertWarned("quests.reward-values");
	}

	@Test
	void questRewardsReplaceTheDefaults() throws Exception
	{
		Settings settings = load("""
				quests:
				  reward-values:
				    diamond: 200
				    emerald: -1
				    stick: many
				""");

		assertEquals(Map.of(Material.DIAMOND, 200, Material.EMERALD, 0), settings.quests().rewardValues());
		assertWarned("quests.reward-values.emerald");
		assertWarned("quests.reward-values.stick");
	}

	@Test
	void theWhitelistWinsOverTheBlacklist() throws Exception
	{
		Settings settings = load("""
				gods:
				  whitelist: true
				  blacklist: true
				""");

		assertTrue(settings.gods().whitelist());
		assertFalse(settings.gods().blacklist());
		assertWarned("gods.blacklist");
	}

	@Test
	void noWorldsMeansEveryWorld() throws Exception
	{
		assertEquals(List.of(), load("general:\n  worlds: []\n").general().worlds());
		assertTrue(load("general:\n  worlds: []\n").general().isActiveIn("a_world_loaded_later"));
		assertFalse(load("general:\n  worlds: [survival]\n").general().isActiveIn("creative"));
		assertEquals(List.of("survival"), load("general:\n  worlds: [survival, survival, '']\n").general().worlds());
	}

	@Test
	void aBlankLanguageIsEnglish() throws Exception
	{
		assertEquals("english", load("general:\n  language: ' '\n").general().language());
		assertWarned("general.language");
	}

	/** all, main or off, in any case; a bare off (or on) is what YAML reads as a boolean */
	@Test
	void floatingTextIsAllMainOrOff() throws Exception
	{
		assertEquals(Settings.FloatingText.ALL, load("").altarTexts().floatingText());
		assertEquals(Settings.FloatingText.MAIN, load("altars:\n  floating-text: Main\n").altarTexts().floatingText());
		assertEquals(Settings.FloatingText.OFF, load("altars:\n  floating-text: off\n").altarTexts().floatingText());
		assertFalse(load("altars:\n  floating-text: off\n").altarTexts().enabled());
		assertEquals(Settings.FloatingText.ALL, load("altars:\n  floating-text: on\n").altarTexts().floatingText());
		assertTrue(this.log.warnings().isEmpty(), "" + this.log.warnings());

		assertEquals(Settings.FloatingText.ALL, load("altars:\n  floating-text: some\n").altarTexts().floatingText());
		assertWarned("altars.floating-text");
		assertEquals(128, load("altars:\n  floating-text-view-distance-blocks: 1000\n").altarTexts().viewDistanceBlocks());
	}
}
