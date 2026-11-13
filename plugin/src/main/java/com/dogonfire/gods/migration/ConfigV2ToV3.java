package com.dogonfire.gods.migration;

import com.dogonfire.gods.model.GodType;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Upgrades config.yml from version 2 to 3, when altar blocks moved into the god types' themes
 * (themes.yml). In version 2, {@code altars.block-types} was the only source of altar blocks, and
 * every config had Gods' defaults there; in version 3 it only holds the admin's own lists, each
 * replacing the theme's blocks for its god type.
 *
 * <ul>
 *   <li>A god type whose list is still Gods' old default (e.g. {@code frost: [snow_block]}) is
 *       removed from the section, so it gets the theme's blocks, which include the old one.
 *   <li>A list the admin changed is kept as it is.
 *   <li>A god type missing from a section that lists others had no altar blocks; it gets an empty
 *       list, so it still has none.
 * </ul>
 *
 * An empty or missing section counts as Gods' defaults (version 2 configs made by {@link
 * ConfigV1ToV2} from 0.9.x configs without altar blocks have the version 3 bundled section, which
 * is empty).
 */
public final class ConfigV2ToV3 implements Migration {
  static final String BLOCK_TYPES = "altars.block-types";

  /** The altar blocks version 2 (and 0.9.x) came with */
  static final Map<String, List<String>> OLD_DEFAULTS =
      Map.ofEntries(
          Map.entry("frost", List.of("snow_block")),
          Map.entry("love", List.of("diamond_block")),
          Map.entry("evil", List.of("obsidian")),
          Map.entry("sea", List.of("lapis_block")),
          Map.entry("moon", List.of("end_stone")),
          Map.entry("sun", List.of("sandstone")),
          Map.entry("thunder", List.of("quartz_block")),
          Map.entry("party", List.of("emerald_block")),
          Map.entry("war", List.of("netherrack")),
          Map.entry("werewolves", List.of()),
          Map.entry("creatures", List.of("oak_log")),
          Map.entry("wisdom", List.of("bookshelf")),
          Map.entry("nature", List.of("melon")));

  private final Function<String, InputStream> resources;

  /**
   * @param resources opens a file bundled in the plugin jar, like {@code Plugin#getResource}
   */
  public ConfigV2ToV3(Function<String, InputStream> resources) {
    this.resources = resources;
  }

  @Override
  public int fromVersion() {
    return 2;
  }

  @Override
  public YamlConfiguration migrate(YamlConfiguration old) throws MigrationException {
    ConfigurationSection section = old.getConfigurationSection(BLOCK_TYPES);
    if (section != null && !section.getKeys(false).isEmpty()) {
      Set<String> listed = new HashSet<>();
      for (String key : section.getKeys(false)) {
        String godType = key.toLowerCase(Locale.ROOT);
        listed.add(godType);
        if (isOldDefault(godType, section.getStringList(key))) {
          section.set(key, null);
        }
      }
      for (GodType type : GodType.values()) {
        String godType = type.name().toLowerCase(Locale.ROOT);
        if (!listed.contains(godType)) {
          section.set(godType, List.of());
        }
      }
    } else if (section == null) {
      old.createSection(BLOCK_TYPES);
    }

    // The section's comment says what it means now
    YamlConfiguration bundled = bundled("config.yml");
    old.setComments(BLOCK_TYPES, bundled.getComments(BLOCK_TYPES));
    return old;
  }

  /** True if the list is the one Gods came with for this god type (in any spelling of the names) */
  static boolean isOldDefault(String godType, List<String> blocks) {
    List<String> old = OLD_DEFAULTS.get(godType);
    if (old == null) {
      return false;
    }
    List<String> normalized = new ArrayList<>();
    for (String block : blocks) {
      String name = block.trim().toLowerCase(Locale.ROOT);
      normalized.add(name.startsWith("minecraft:") ? name.substring("minecraft:".length()) : name);
    }
    return normalized.equals(old);
  }

  private YamlConfiguration bundled(String name) throws MigrationException {
    try (InputStream in = this.resources.apply(name)) {
      if (in == null) {
        throw new MigrationException(name + " is missing from the plugin jar");
      }
      return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new MigrationException("could not read the bundled " + name + ": " + e.getMessage());
    }
  }
}
