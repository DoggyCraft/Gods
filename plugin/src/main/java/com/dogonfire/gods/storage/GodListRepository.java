package com.dogonfire.gods.storage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * whitelist.yml or blacklist.yml: god names, each with whatever value the admin gave it (the
 * whitelist's {@code <god>.MinPower}, the blacklist's {@code <god>: ""}). Kept exactly as it is in
 * the file; a god is listed when its name is a key, read like a YAML path (as before: a '.' in a
 * name looks into a section).
 */
public final class GodListRepository extends Repository {
  private final String description;
  private final Map<String, Object> entries = new LinkedHashMap<String, Object>();

  /**
   * @param description for the log, e.g. "whitelisted"
   */
  GodListRepository(DataFile file, Logger logger, String description) {
    super(file, logger);
    this.description = description;
  }

  /** Whether the god is listed */
  public boolean contains(String godName) {
    return lookup(godName) != null;
  }

  /** The god's {@code MinPower} (whitelist), 0 when not listed or not a number */
  public int minPower(String godName) {
    return lookup(godName + ".MinPower") instanceof Number number ? number.intValue() : 0;
  }

  /**
   * Adds or replaces a god's entry; {@code value} must be a plain YAML value (string, number, map,
   * list)
   */
  public void put(String godName, Object value) {
    this.entries.put(godName, value);
  }

  private Object lookup(String path) {
    Object value = this.entries;
    for (String part : path.split("\\.", -1)) {
      if (!(value instanceof Map<?, ?> map)) {
        return null;
      }
      value = map.get(part);
    }
    return value;
  }

  @Override
  public int size() {
    return this.entries.size();
  }

  @Override
  protected void clear() {
    this.entries.clear();
  }

  @Override
  protected void read(YamlConfiguration yaml) {
    for (String key : yaml.getKeys(false)) {
      this.entries.put(key, toPlain(yaml.get(key)));
    }

    logger().info("Loaded " + this.entries.size() + " " + this.description + " Gods.");
  }

  @Override
  protected void write(YamlConfiguration yaml) {
    for (Map.Entry<String, Object> entry : this.entries.entrySet()) {
      yaml.set(entry.getKey(), entry.getValue());
    }
  }
}
