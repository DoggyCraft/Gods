package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class HolyLandNamesTest {
  @Test
  void biomesFallInTheirGroups() {
    Map<String, String> expected =
        Map.ofEntries(
            Map.entry("plains", "plains"),
            Map.entry("sunflower_plains", "plains"),
            Map.entry("meadow", "plains"),
            Map.entry("forest", "forest"),
            Map.entry("birch_forest", "forest"),
            Map.entry("flower_forest", "forest"),
            Map.entry("taiga", "forest"),
            Map.entry("old_growth_spruce_taiga", "forest"),
            Map.entry("dark_forest", "dark-forest"),
            Map.entry("pale_garden", "dark-forest"),
            Map.entry("snowy_plains", "snow"),
            Map.entry("snowy_taiga", "snow"),
            Map.entry("ice_spikes", "snow"),
            Map.entry("grove", "snow"),
            Map.entry("frozen_peaks", "snow"),
            Map.entry("frozen_ocean", "snow"),
            Map.entry("desert", "desert"),
            Map.entry("badlands", "badlands"),
            Map.entry("wooded_badlands", "badlands"),
            Map.entry("jungle", "jungle"),
            Map.entry("bamboo_jungle", "jungle"),
            Map.entry("swamp", "swamp"),
            Map.entry("mangrove_swamp", "swamp"),
            Map.entry("ocean", "ocean"),
            Map.entry("deep_lukewarm_ocean", "ocean"),
            Map.entry("beach", "ocean"),
            Map.entry("river", "ocean"),
            Map.entry("stony_shore", "ocean"),
            Map.entry("windswept_hills", "mountains"),
            Map.entry("windswept_forest", "mountains"),
            Map.entry("stony_peaks", "mountains"),
            Map.entry("jagged_peaks", "mountains"),
            Map.entry("cherry_grove", "cherry"),
            Map.entry("mushroom_fields", "mushroom"),
            Map.entry("savanna", "savanna"),
            Map.entry("windswept_savanna", "savanna"),
            Map.entry("nether_wastes", "nether"),
            Map.entry("crimson_forest", "nether"),
            Map.entry("warped_forest", "nether"),
            Map.entry("soul_sand_valley", "nether"),
            Map.entry("basalt_deltas", "nether"),
            Map.entry("the_end", "end"),
            Map.entry("end_highlands", "end"),
            Map.entry("small_end_islands", "end"),
            Map.entry("end_barrens", "end"),
            Map.entry("lush_caves", "default"),
            Map.entry("deep_dark", "default"),
            Map.entry("the_void", "default"));
    for (Map.Entry<String, String> entry : expected.entrySet()) {
      assertEquals(entry.getValue(), HolyLandNames.group(entry.getKey()), entry.getKey());
    }
    assertEquals("plains", HolyLandNames.group("minecraft:PLAINS"));
    assertEquals(HolyLandNames.DEFAULT, HolyLandNames.group(null));
  }

  @Test
  void everyGroupIsKnown() {
    for (String biome :
        List.of(
            "plains",
            "forest",
            "dark_forest",
            "snowy_plains",
            "desert",
            "badlands",
            "jungle",
            "swamp",
            "ocean",
            "windswept_hills",
            "cherry_grove",
            "mushroom_fields",
            "savanna",
            "nether_wastes",
            "the_end",
            "lush_caves")) {
      assertTrue(HolyLandNames.GROUPS.contains(HolyLandNames.group(biome)), biome);
    }
  }

  @Test
  void aNameJoinsItsPartsAsTheyAre() {
    Random random = new Random(1);
    assertEquals(
        "Upper Flowerfield",
        HolyLandNames.pick(List.of("Upper "), List.of("Flower"), List.of("field"), random));
    assertEquals(
        "Flowerfield",
        HolyLandNames.pick(List.of(""), List.of("Flower"), List.of("field"), random));
    assertEquals(
        "Old Salt Haven",
        HolyLandNames.pick(List.of("Old  "), List.of("Salt"), List.of(" Haven "), random));
    assertEquals("Flower", HolyLandNames.pick(List.of(), List.of("Flower"), List.of(), random));
    assertNull(HolyLandNames.pick(List.of(""), List.of(), List.of(""), random));
  }

  @Test
  void aNameMayHaveUpToThirtyTwoCharactersAndNoColors() {
    assertEquals("Holy Hill", HolyLandNames.validName("  Holy   Hill "));
    assertEquals("x".repeat(32), HolyLandNames.validName("x".repeat(32)));
    assertNull(HolyLandNames.validName("x".repeat(33)));
    assertNull(HolyLandNames.validName("   "));
    assertNull(HolyLandNames.validName("§cRed"));
    assertNull(HolyLandNames.validName("&cRed"));
    assertNull(HolyLandNames.validName(null));
  }

  /** Every group has every part in the bundled messages, with words that make names */
  @Test
  void theBundledWordsMakeNames() throws Exception {
    YamlConfiguration messages;
    try (InputStream in = getClass().getResourceAsStream("/lang/english/messages.yml")) {
      assertNotNull(in);
      messages =
          YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
    }
    Random random = new Random(42);
    for (String group : HolyLandNames.GROUPS) {
      for (String part : HolyLandNames.PARTS) {
        List<String> words = messages.getStringList(HolyLandNames.key(group, part));
        assertTrue(words.size() >= 4 && words.size() <= 8, HolyLandNames.key(group, part));
        for (String word : words) {
          assertFalse(
              word.contains("<") || word.contains("&") || word.contains("§"),
              HolyLandNames.key(group, part) + ": " + word);
        }
      }
      for (int i = 0; i < 20; i++) {
        String name =
            HolyLandNames.pick(
                messages.getStringList(HolyLandNames.key(group, "prefix")),
                messages.getStringList(HolyLandNames.key(group, "middle")),
                messages.getStringList(HolyLandNames.key(group, "suffix")),
                random);
        assertNotNull(HolyLandNames.validName(name), group + ": " + name);
        assertEquals(name, HolyLandNames.validName(name), group + ": " + name);
      }
    }
    assertFalse(messages.getStringList("holy-land.names.plains.middle").contains("Cold"));
  }
}
