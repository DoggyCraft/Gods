package com.dogonfire.gods.migration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Upgrades holyland.yml from version 2 to 3, when holy land went from 128x128 block regions to
 * chunks.
 *
 * <p>Version 2 keeps {@code Holylands.<world key>.<hash>} and {@code NeutralLands.<world
 * key>.<hash>}, the hash being the region X in the high 32 bits and the region Z in the low 32
 * bits. In version 3 a holy land is the land of one altar: {@code Holylands.<world key>."cx,cz"}
 * with {@code Center: "cx,cz"} and {@code Chunks: ["cx,cz", ...]}, and a neutral land is one chunk,
 * {@code NeutralLands.<world key>."cx,cz"}. Each region land becomes one land with the region's 64
 * chunks (region rx, rz holds chunks rx*8 .. rx*8+7 and rz*8 .. rz*8+7) and the middle one as its
 * center (rx*8+4, rz*8+4), keeping all its other keys; each neutral region becomes its 64 neutral
 * chunks, with its time. Entries whose key isn't a region hash are left as they are.
 */
public final class HolyLandsV2ToV3 implements Migration {
  static final String HOLY_LANDS = "Holylands";
  static final String NEUTRAL_LANDS = "NeutralLands";
  static final String CENTER = "Center";
  static final String CHUNKS = "Chunks";

  /** A region is 8x8 chunks */
  public static final int CHUNKS_PER_REGION = 8;

  private final Logger log;

  public HolyLandsV2ToV3(Logger log) {
    this.log = log;
  }

  @Override
  public int fromVersion() {
    return 2;
  }

  /** The region X of a version 2 hash */
  public static int regionX(long hash) {
    return (int) (hash >> 32);
  }

  /** The region Z of a version 2 hash */
  public static int regionZ(long hash) {
    return (int) hash;
  }

  /** The chunks of a region as {@code "cx,cz"}, row by row */
  public static List<String> regionChunks(long hash) {
    int baseX = regionX(hash) * CHUNKS_PER_REGION;
    int baseZ = regionZ(hash) * CHUNKS_PER_REGION;
    List<String> chunks = new ArrayList<String>(CHUNKS_PER_REGION * CHUNKS_PER_REGION);
    for (int x = 0; x < CHUNKS_PER_REGION; x++) {
      for (int z = 0; z < CHUNKS_PER_REGION; z++) {
        chunks.add((baseX + x) + "," + (baseZ + z));
      }
    }
    return chunks;
  }

  /** The middle chunk of a region as {@code "cx,cz"} */
  public static String regionCenter(long hash) {
    int half = CHUNKS_PER_REGION / 2;
    return (regionX(hash) * CHUNKS_PER_REGION + half)
        + ","
        + (regionZ(hash) * CHUNKS_PER_REGION + half);
  }

  @Override
  public YamlConfiguration migrate(YamlConfiguration old) {
    int lands = convert(old, HOLY_LANDS, true);
    int neutral = convert(old, NEUTRAL_LANDS, false);
    if (lands > 0 || neutral > 0) {
      this.log.info(
          "holyland.yml: "
              + lands
              + " holy land regions and "
              + neutral
              + " neutral land regions became chunks (64 each); a holy land now grows from its altar with the god's rank.");
    }
    return old;
  }

  private static int convert(YamlConfiguration yaml, String sectionName, boolean holyLands) {
    ConfigurationSection section = yaml.getConfigurationSection(sectionName);
    if (section == null) {
      return 0;
    }
    int converted = 0;
    for (String world : section.getKeys(false)) {
      ConfigurationSection worldLands = section.getConfigurationSection(world);
      if (worldLands == null) {
        continue;
      }
      // Rebuilt in the same order
      Map<String, Object> entries = new LinkedHashMap<String, Object>();
      for (String key : worldLands.getKeys(false)) {
        Object value = worldLands.get(key);
        Long hash = parseHash(key);
        if (hash == null) {
          entries.put(key, value);
        } else if (holyLands && value instanceof ConfigurationSection land) {
          Map<String, Object> values = new LinkedHashMap<String, Object>();
          for (String landKey : land.getKeys(false)) {
            values.put(landKey, land.get(landKey));
          }
          values.put(CENTER, regionCenter(hash));
          values.put(CHUNKS, regionChunks(hash));
          entries.put(regionCenter(hash), values);
          converted++;
        } else if (!holyLands && !(value instanceof ConfigurationSection)) {
          for (String chunk : regionChunks(hash)) {
            entries.put(chunk, value);
          }
          converted++;
        } else {
          entries.put(key, value);
        }
      }
      ConfigurationSection rebuilt = section.createSection(world);
      for (Map.Entry<String, Object> entry : entries.entrySet()) {
        setValue(rebuilt, entry.getKey(), entry.getValue());
      }
    }
    return converted;
  }

  @SuppressWarnings("unchecked")
  private static void setValue(ConfigurationSection parent, String key, Object value) {
    if (value instanceof ConfigurationSection section) {
      ConfigurationSection child = parent.createSection(key);
      for (String childKey : section.getKeys(false)) {
        setValue(child, childKey, section.get(childKey));
      }
    } else if (value instanceof Map<?, ?> map) {
      ConfigurationSection child = parent.createSection(key);
      for (Map.Entry<String, Object> entry : ((Map<String, Object>) map).entrySet()) {
        setValue(child, entry.getKey(), entry.getValue());
      }
    } else {
      parent.set(key, value);
    }
  }

  private static Long parseHash(String key) {
    try {
      return Long.parseLong(key);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
