package com.dogonfire.gods.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.ConfigDefaults;
import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.config.TestConfigs;
import com.dogonfire.gods.model.GodRank;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigV6ToV7Test {
  private static final String VERSION_6 =
      """
      # My server
      config-version: 6
      holy-land:
        enabled: true
      ranks:
        unlocks:
          # Our quests come late
          quests: descended
          # More land for greater gods
          holy-land-extra-regions:
            spirit: 0
            recognized: 2
            descended: 2
            exalted: 5
            greater: 10
      """;

  private final TestConfigs.CapturingLogger log = new TestConfigs.CapturingLogger();

  @TempDir Path dir;

  private ConfigV6ToV7 migration() {
    return new ConfigV6ToV7(TestConfigs::resource, this.log.logger());
  }

  @Test
  void theExtraRegionsMakeWayForTheRadius() throws Exception {
    YamlConfiguration migrated = migration().migrate(TestConfigs.yaml(VERSION_6));

    assertFalse(migrated.contains("ranks.unlocks.holy-land-extra-regions"));
    assertEquals(0, migrated.getInt("ranks.unlocks.holy-land-radius-chunks.spirit"));
    assertEquals(4, migrated.getInt("ranks.unlocks.holy-land-radius-chunks.greater"));
    assertFalse(
        migrated.getComments("ranks.unlocks.holy-land-radius-chunks").isEmpty(),
        "the bundled comment comes along");
    assertEquals("descended", migrated.getString("ranks.unlocks.quests"), "the other unlocks stay");
    assertTrue(migrated.getBoolean("holy-land.enabled"));
    assertTrue(this.log.warnings().isEmpty(), "Warnings: " + this.log.warnings());

    ConfigDefaults.addMissing(migrated, TestConfigs.bundled());
    Settings settings = TestConfigs.load(TestConfigs.yaml(migrated.saveToString()), this.log);
    assertEquals(
        TestConfigs.defaultRanks().unlocks().holyLandRadiusChunks(),
        settings.ranks().unlocks().holyLandRadiusChunks());
    assertEquals(GodRank.DESCENDED, settings.ranks().unlocks().quests());
    assertTrue(this.log.warnings().isEmpty(), "Warnings: " + this.log.warnings());
  }

  /** A radius the admin already wrote is kept */
  @Test
  void anExistingRadiusIsKept() throws Exception {
    YamlConfiguration kept =
        migration()
            .migrate(
                TestConfigs.yaml(
                    "config-version: 6\nranks:\n  unlocks:\n    holy-land-radius-chunks:\n      greater: 6\n"));
    assertEquals(6, kept.getInt("ranks.unlocks.holy-land-radius-chunks.greater"));
    assertFalse(kept.contains("ranks.unlocks.holy-land-radius-chunks.spirit"));
  }

  /**
   * How Gods loads config.yml: upgraded in place, the old file kept as config.yml.v6.bak, comments
   * kept
   */
  @Test
  void upgradesTheFileOnDiskAndKeepsABackup() throws Exception {
    Path file = this.dir.resolve("config.yml");
    Files.writeString(file, VERSION_6);

    VersionedYaml format =
        new VersionedYaml(
            "config-version",
            Gods.CONFIG_VERSION,
            1,
            List.of(
                new ConfigV1ToV2(TestConfigs::resource, this.log.logger()),
                new ConfigV2ToV3(TestConfigs::resource),
                new ConfigV3ToV4(this.log.logger()),
                new ConfigV4ToV5(TestConfigs::resource, this.log.logger()),
                new ConfigV5ToV6(TestConfigs::resource, this.log.logger()),
                migration()),
            ConfigV1ToV2::isVersion2Layout);
    YamlConfiguration loaded = format.load(file.toFile(), this.log.logger());

    assertEquals(7, Gods.CONFIG_VERSION);
    assertEquals(Gods.CONFIG_VERSION, loaded.getInt("config-version"));
    assertEquals(VERSION_6, Files.readString(this.dir.resolve("config.yml.v6.bak")));
    String onDisk = Files.readString(file);
    assertFalse(onDisk.contains("holy-land-extra-regions"));
    assertFalse(onDisk.contains("More land for greater gods"), "its comment goes with it");
    assertTrue(onDisk.contains("# My server"), "comments stay");
    assertTrue(onDisk.contains("# Our quests come late"), "comments stay");
    assertTrue(onDisk.contains("holy-land-radius-chunks:"));
  }
}
