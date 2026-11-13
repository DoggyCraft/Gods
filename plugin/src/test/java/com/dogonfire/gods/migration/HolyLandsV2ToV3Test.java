package com.dogonfire.gods.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.logging.Logger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class HolyLandsV2ToV3Test {
  private static YamlConfiguration yaml(String text) throws InvalidConfigurationException {
    YamlConfiguration yaml = new YamlConfiguration();
    yaml.loadFromString(text);
    return yaml;
  }

  /**
   * Region rx, rz holds the chunks rx*8 .. rx*8+7 and rz*8 .. rz*8+7; the hash is rx in the high 32
   * bits, rz in the low
   */
  @Test
  void aRegionIsItsSixtyFourChunks() {
    long hash = (1L << 32) | (-2 & 0xFFFFFFFFL);
    assertEquals(1, HolyLandsV2ToV3.regionX(hash));
    assertEquals(-2, HolyLandsV2ToV3.regionZ(hash));
    List<String> chunks = HolyLandsV2ToV3.regionChunks(hash);
    assertEquals(64, chunks.size());
    assertEquals("8,-16", chunks.get(0));
    assertEquals("15,-9", chunks.get(63));
    assertEquals("12,-12", HolyLandsV2ToV3.regionCenter(hash));
    assertEquals(64, HolyLandsV2ToV3.regionChunks(-1L).stream().distinct().count());
    assertEquals("-4,-4", HolyLandsV2ToV3.regionCenter(-1L));
    assertEquals("4,4", HolyLandsV2ToV3.regionCenter(0L));
  }

  @Test
  void landsKeepTheirKeysAndNeutralRegionsBecomeNeutralChunks() throws Exception {
    YamlConfiguration migrated =
        new HolyLandsV2ToV3(Logger.getAnonymousLogger())
            .migrate(
                yaml(
                    """
                    data-version: 2
                    Holylands:
                      world:
                        '0':
                          GodName: Thor
                          Banner: red
                        odd: kept
                    NeutralLands:
                      world:
                        '-1': 12:00 01-01-2025
                    """));

    assertEquals("Thor", migrated.getString("Holylands.world.4,4.GodName"));
    assertEquals("red", migrated.getString("Holylands.world.4,4.Banner"));
    assertEquals("4,4", migrated.getString("Holylands.world.4,4.Center"));
    assertEquals(64, migrated.getStringList("Holylands.world.4,4.Chunks").size());
    assertTrue(migrated.getStringList("Holylands.world.4,4.Chunks").contains("7,7"));
    assertFalse(migrated.contains("Holylands.world.0"));
    assertEquals("kept", migrated.getString("Holylands.world.odd"));
    assertEquals(64, migrated.getConfigurationSection("NeutralLands.world").getKeys(false).size());
    assertEquals("12:00 01-01-2025", migrated.getString("NeutralLands.world.-8,-8"));
    assertEquals("12:00 01-01-2025", migrated.getString("NeutralLands.world.-1,-1"));
  }
}
