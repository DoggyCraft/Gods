package com.dogonfire.gods.storage;

import com.dogonfire.gods.migration.VersionedYaml;
import com.dogonfire.gods.model.Altar;
import com.dogonfire.gods.model.AltarKind;
import com.dogonfire.gods.model.BlockPos;
import com.dogonfire.gods.util.LegacyDates;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * altars.yml: every altar Gods knows of ({@link Altar}), as a list (world names may hold the YAML
 * path separator, so they are values, not keys):
 *
 * <pre>
 * data-version: 1
 * Altars:
 * - World: world
 *   Sign: 10,64,-3         # the wall sign, "x,y,z"
 *   Block: 10,64,-4        # the altar block it hangs on
 *   Kind: praying          # praying, curses or blessings
 *   God: Zeus
 *   Created: 12:00:00 01-01-2026
 *   Creator: &lt;uuid&gt;
 * </pre>
 *
 * {@code Created} and {@code Creator} are left out when not known (altars found in the world). An
 * entry that can't be read (no world, coordinates or kind) is kept as it is and written back; keys
 * this version doesn't know are kept with their entry. In memory the altars are indexed by sign, by
 * altar block and by chunk (both the sign's and the altar block's), so the lookups of block events
 * and chunk loads are cheap. The file is the registry's memory between restarts; the world is the
 * truth, and {@code AltarRegistry} brings this in line with it as chunks load.
 */
public final class AltarRepository extends Repository {
  public static final int DATA_VERSION = 1;
  public static final String VERSION_KEY = "data-version";

  private static final String ALTARS = "Altars";
  private static final String WORLD = "World";
  private static final String SIGN = "Sign";
  private static final String BLOCK = "Block";
  private static final String KIND = "Kind";
  private static final String GOD = "God";
  private static final String CREATED = "Created";
  private static final String CREATOR = "Creator";

  private static final Set<String> KNOWN = Set.of(WORLD, SIGN, BLOCK, KIND, GOD, CREATED, CREATOR);

  private static final LegacyDates DATES = LegacyDates.SECONDS;

  private final Map<BlockPos, Altar> bySign = new LinkedHashMap<>();
  private final Map<BlockPos, List<Altar>> byBlock = new HashMap<>();
  private final Map<BlockPos.ChunkPos, Set<Altar>> byChunk = new HashMap<>();
  // Entries that could not be read, written back as they were
  private final List<Object> unreadable = new ArrayList<>();

  /**
   * The data version of altars.yml: 1 from the start, so a later change of layout can be upgraded
   */
  static VersionedYaml format() {
    return new VersionedYaml(VERSION_KEY, DATA_VERSION, DATA_VERSION, List.of());
  }

  AltarRepository(DataFile file, Logger logger) {
    super(file, logger);
  }

  /** The altar whose sign this is, or null */
  public Altar atSign(BlockPos sign) {
    return this.bySign.get(sign);
  }

  /** The altars whose signs hang on this block (empty if none) */
  public List<Altar> onBlock(BlockPos block) {
    List<Altar> altars = this.byBlock.get(block);
    return altars != null ? List.copyOf(altars) : List.of();
  }

  /** The altars with their sign or their altar block in this chunk (empty if none) */
  public List<Altar> inChunk(BlockPos.ChunkPos chunk) {
    Set<Altar> altars = this.byChunk.get(chunk);
    return altars != null ? List.copyOf(altars) : List.of();
  }

  /** The altars whose sign is in this chunk (empty if none) */
  public List<Altar> signsInChunk(BlockPos.ChunkPos chunk) {
    List<Altar> altars = new ArrayList<>();
    for (Altar altar : inChunk(chunk)) {
      if (altar.sign().chunk().equals(chunk)) {
        altars.add(altar);
      }
    }
    return altars;
  }

  /** Whether a block is the sign or the altar block of an altar */
  public boolean isAltarPart(BlockPos block) {
    return this.bySign.containsKey(block) || this.byBlock.containsKey(block);
  }

  /** The god's altars (of every kind) */
  public List<Altar> forGod(String godName) {
    List<Altar> altars = new ArrayList<>();
    for (Altar altar : this.bySign.values()) {
      if (godName != null && godName.equals(altar.god())) {
        altars.add(altar);
      }
    }
    return altars;
  }

  public Collection<Altar> all() {
    return Collections.unmodifiableCollection(this.bySign.values());
  }

  public boolean isEmpty() {
    return this.bySign.isEmpty();
  }

  /**
   * Adds the altar, replacing the one with the same sign. Changing its god or kind later needs no
   * re-adding.
   */
  public void add(Altar altar) {
    remove(altar.sign());
    this.bySign.put(altar.sign(), altar);
    this.byBlock.computeIfAbsent(altar.block(), b -> new ArrayList<>()).add(altar);
    this.byChunk.computeIfAbsent(altar.sign().chunk(), c -> new LinkedHashSet<>()).add(altar);
    this.byChunk.computeIfAbsent(altar.block().chunk(), c -> new LinkedHashSet<>()).add(altar);
  }

  /** Removes the altar with this sign; returns it, or null if there was none */
  public Altar remove(BlockPos sign) {
    Altar altar = this.bySign.remove(sign);
    if (altar == null) {
      return null;
    }
    List<Altar> onBlock = this.byBlock.get(altar.block());
    if (onBlock != null) {
      onBlock.remove(altar);
      if (onBlock.isEmpty()) {
        this.byBlock.remove(altar.block());
      }
    }
    removeFromChunk(altar.sign().chunk(), altar);
    removeFromChunk(altar.block().chunk(), altar);
    return altar;
  }

  private void removeFromChunk(BlockPos.ChunkPos chunk, Altar altar) {
    Set<Altar> altars = this.byChunk.get(chunk);
    if (altars != null) {
      altars.remove(altar);
      if (altars.isEmpty()) {
        this.byChunk.remove(chunk);
      }
    }
  }

  @Override
  public int size() {
    return this.bySign.size();
  }

  @Override
  protected void clear() {
    this.bySign.clear();
    this.byBlock.clear();
    this.byChunk.clear();
    this.unreadable.clear();
  }

  @Override
  protected void read(YamlConfiguration yaml) {
    for (String key : yaml.getKeys(false)) {
      if (!key.equals(VERSION_KEY) && !key.equals(ALTARS)) {
        keepUnknown(key, yaml.get(key));
      }
    }
    if (yaml.contains(ALTARS) && !yaml.isList(ALTARS)) {
      keepUnknown(ALTARS, yaml.get(ALTARS));
      logger().warning("altars.yml: " + ALTARS + " is not a list; it is kept as it is.");
      return;
    }

    for (Object entry : yaml.getList(ALTARS, List.of())) {
      Altar altar = entry instanceof Map<?, ?> map ? readAltar(map) : null;
      if (altar == null || this.bySign.containsKey(altar.sign())) {
        this.unreadable.add(toPlain(entry));
        continue;
      }
      add(altar);
    }
    if (!this.unreadable.isEmpty()) {
      logger()
          .warning(
              "altars.yml has "
                  + this.unreadable.size()
                  + " entries that could not be read; they are kept as they are.");
    }
    logger().info("Loaded " + this.bySign.size() + " altars.");
  }

  private static Altar readAltar(Map<?, ?> map) {
    String world = string(map.get(WORLD));
    BlockPos sign = BlockPos.parse(world, string(map.get(SIGN)));
    BlockPos block = BlockPos.parse(world, string(map.get(BLOCK)));
    AltarKind kind = AltarKind.byKey(string(map.get(KIND)));
    if (sign == null || block == null || kind == null) {
      return null;
    }

    Map<String, Object> extras = new LinkedHashMap<>();
    for (Map.Entry<?, ?> field : map.entrySet()) {
      String name = String.valueOf(field.getKey());
      if (!KNOWN.contains(name)) {
        extras.put(name, toPlain(field.getValue()));
      }
    }
    Instant created = null;
    String createdText = string(map.get(CREATED));
    if (createdText != null) {
      created = DATES.parse(createdText);
      if (created == null) {
        extras.put(CREATED, createdText);
      }
    }
    UUID creator = null;
    String creatorText = string(map.get(CREATOR));
    if (creatorText != null) {
      try {
        creator = UUID.fromString(creatorText.trim());
      } catch (IllegalArgumentException e) {
        extras.put(CREATOR, creatorText);
      }
    }

    Altar altar = new Altar(sign, block, kind, string(map.get(GOD)), created, creator);
    altar.setExtras(extras);
    return altar;
  }

  private static String string(Object value) {
    return value != null ? value.toString() : null;
  }

  @Override
  protected void write(YamlConfiguration yaml) {
    List<Object> entries = new ArrayList<>();
    for (Altar altar : this.bySign.values()) {
      Map<String, Object> entry = new LinkedHashMap<>();
      entry.put(WORLD, altar.world());
      entry.put(SIGN, altar.sign().coordinates());
      entry.put(BLOCK, altar.block().coordinates());
      entry.put(KIND, altar.kind().key());
      if (altar.god() != null) {
        entry.put(GOD, altar.god());
      }
      if (altar.created() != null) {
        entry.put(CREATED, DATES.format(altar.created()));
      }
      if (altar.creator() != null) {
        entry.put(CREATOR, altar.creator().toString());
      }
      for (Map.Entry<String, Object> extra : altar.extras().entrySet()) {
        entry.putIfAbsent(extra.getKey(), extra.getValue());
      }
      entries.add(entry);
    }
    entries.addAll(this.unreadable);
    yaml.set(ALTARS, entries);
  }
}
