package com.dogonfire.gods.migration;

import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Upgrades config.yml from version 3 to 4, when a religion's home became its main altar: /g sethome
 * is /g setaltar now, which only priests can use, so {@code priests.only-priests-can-set-home} is
 * removed. Everything else stays as it is.
 */
public final class ConfigV3ToV4 implements Migration {
  static final String ONLY_PRIESTS_CAN_SET_HOME = "priests.only-priests-can-set-home";

  private final Logger log;

  public ConfigV3ToV4(Logger log) {
    this.log = log;
  }

  @Override
  public int fromVersion() {
    return 3;
  }

  @Override
  public YamlConfiguration migrate(YamlConfiguration old) {
    if (old.contains(ONLY_PRIESTS_CAN_SET_HOME)) {
      old.set(ONLY_PRIESTS_CAN_SET_HOME, null);
      this.log.info(
          "Removed "
              + ONLY_PRIESTS_CAN_SET_HOME
              + " from config.yml: a religion's home is its main altar now, and only priests choose it (/g setaltar, also /g sethome).");
    }
    return old;
  }
}
