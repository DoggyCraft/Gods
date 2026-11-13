package com.dogonfire.gods.migration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Upgrades believers.yml to version 2, the first with a {@code data-version}. The keys of an entry
 * stay the same; very old versions keyed believers by player name instead of UUID, and those
 * entries are moved to the player's UUID, looked up among the players the server has seen
 * (case-insensitive, like {@link GodsV1ToV2}'s priests). A name the server doesn't know, or whose
 * player already has an entry by UUID, is moved under {@code Unresolved.<name>} (logged), so
 * nothing is lost and the next lookup can still be done by hand. Entries already keyed by UUID are
 * left alone.
 */
public final class BelieversV1ToV2 implements Migration {
  /** Where entries keyed by a name that could not be turned into a UUID are kept */
  public static final String UNRESOLVED = "Unresolved";

  private final Function<String, UUID> playerIdByName;
  private final Logger log;

  /**
   * @param playerIdByName the UUID of a player the server knows, by name (any case), or null
   */
  public BelieversV1ToV2(Function<String, UUID> playerIdByName, Logger log) {
    this.playerIdByName = playerIdByName;
    this.log = log;
  }

  @Override
  public int fromVersion() {
    return 1;
  }

  @Override
  public YamlConfiguration migrate(YamlConfiguration old) {
    int converted = 0;
    int unresolved = 0;
    for (String key : new ArrayList<String>(old.getKeys(false))) {
      ConfigurationSection entry = old.getConfigurationSection(key);
      if (entry == null || key.equals(UNRESOLVED) || isUUID(key)) {
        continue;
      }

      Map<String, Object> values = plain(entry);
      old.set(key, null);

      UUID id = this.playerIdByName.apply(key);
      if (id != null && !old.contains(id.toString())) {
        old.createSection(id.toString(), values);
        converted++;
        continue;
      }

      this.log.warning(
          "believers.yml: kept the believer '"
              + key
              + "' under "
              + UNRESOLVED
              + ": "
              + (id == null
                  ? "the server doesn't know a player of that name"
                  : "that player already has an entry by UUID"));
      old.createSection(UNRESOLVED + "." + key, values);
      unresolved++;
    }
    if (converted > 0) {
      this.log.info(
          "believers.yml: converted "
              + converted
              + " believers kept by player name to their UUID.");
    }
    if (unresolved > 0) {
      this.log.warning(
          "believers.yml: "
              + unresolved
              + " believers kept by player name could not be converted; they are under "
              + UNRESOLVED
              + ".");
    }
    return old;
  }

  private static boolean isUUID(String text) {
    try {
      UUID.fromString(text);
      return true;
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }

  /** The section as nested maps, so it can be put somewhere else */
  private static Map<String, Object> plain(ConfigurationSection section) {
    Map<String, Object> map = new LinkedHashMap<String, Object>();
    for (String key : section.getKeys(false)) {
      Object value = section.get(key);
      map.put(key, value instanceof ConfigurationSection child ? plain(child) : value);
    }
    return map;
  }
}
