package com.dogonfire.gods.storage;

import com.dogonfire.gods.migration.HolyLandsV1ToV2;
import com.dogonfire.gods.migration.HolyLandsV2ToV3;
import com.dogonfire.gods.migration.VersionedYaml;
import com.dogonfire.gods.model.ChunkKey;
import com.dogonfire.gods.model.HolyLand;
import com.dogonfire.gods.model.NeutralLand;
import com.dogonfire.gods.util.LegacyDates;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * holyland.yml: the {@link HolyLand}s (one per altar, each a set of chunks) and the {@link
 * NeutralLand} chunks, indexed by {@link ChunkKey} so a lookup by location is a hash hit.
 *
 * <pre>
 * data-version: 3
 * Holylands.&lt;world key&gt;."cx,cz" (the land's id: its center chunk when it was made):
 *     GodName, Name, World, Center: "cx,cz", Chunks: ["cx,cz", ...], FirstPrayerTime, LastPrayerTime,
 *     AttackingGodName, ContestedTime, AttackerKills, DefenderKills, Desecration
 * Holylands.&lt;world key&gt;.claim-&lt;claim id&gt; (a claim land, /g holyclaim):
 *     GodName, Name, World, Center: "cx,cz" (its altar's chunk), Claim: &lt;GriefPrevention claim id&gt;, FirstPrayerTime,
 *     LastPrayerTime and the attack keys; no Chunks (the claim is the land)
 * NeutralLands.&lt;world key&gt;."cx,cz": &lt;time set&gt;
 * </pre>
 *
 * The world key is the world name with '.' replaced by '_'. The ids are {@code "cx,cz"}, not {@code
 * cx_cz}: YAML 1.1 reads {@code 3_4} as the number 34. Version 1 is upgraded by {@link
 * HolyLandsV1ToV2}, version 2 (128x128 block regions) by {@link HolyLandsV2ToV3}. {@code Claim} is
 * a key of 1.0.0 that needed no new version. Claim lands are indexed by their claim id; a second
 * land of the same claim (a hand edit) is kept in the file but not used.
 */
public final class HolyLandRepository extends Repository {
  public static final int DATA_VERSION = 3;
  public static final String VERSION_KEY = "data-version";

  private static final String HOLY_LANDS = "Holylands";
  private static final String NEUTRAL_LANDS = "NeutralLands";

  private static final String GOD_NAME = "GodName";
  private static final String NAME = "Name";
  private static final String WORLD = "World";
  private static final String CENTER = "Center";
  private static final String CHUNKS = "Chunks";
  private static final String CLAIM = "Claim";
  private static final String CLAIM_ID_PREFIX = "claim-";
  private static final String FIRST_PRAYER_TIME = "FirstPrayerTime";
  private static final String LAST_PRAYER_TIME = "LastPrayerTime";
  private static final String ATTACKING_GOD_NAME = "AttackingGodName";
  private static final String CONTESTED_TIME = "ContestedTime";
  private static final String ATTACKER_KILLS = "AttackerKills";
  private static final String DEFENDER_KILLS = "DefenderKills";
  private static final String DESECRATION = "Desecration";

  private static final Set<String> KNOWN =
      Set.of(
          GOD_NAME,
          NAME,
          WORLD,
          CENTER,
          CHUNKS,
          FIRST_PRAYER_TIME,
          LAST_PRAYER_TIME,
          ATTACKING_GOD_NAME,
          CONTESTED_TIME,
          ATTACKER_KILLS,
          DEFENDER_KILLS,
          DESECRATION);
  // A claim land has no chunks: Chunks on one (a hand edit) is kept as it is
  private static final Set<String> KNOWN_CLAIM =
      Set.of(
          GOD_NAME,
          NAME,
          WORLD,
          CENTER,
          CLAIM,
          FIRST_PRAYER_TIME,
          LAST_PRAYER_TIME,
          ATTACKING_GOD_NAME,
          CONTESTED_TIME,
          ATTACKER_KILLS,
          DEFENDER_KILLS,
          DESECRATION);

  private static final LegacyDates DATES = LegacyDates.MINUTES;

  // The lands by "<world key>.<id>", in file order
  private final Map<String, HolyLand> lands = new LinkedHashMap<String, HolyLand>();
  // Every claimed chunk to its land
  private final Map<ChunkKey, HolyLand> byChunk = new HashMap<ChunkKey, HolyLand>();
  // Every claim land by its GriefPrevention claim id
  private final Map<Long, HolyLand> byClaim = new HashMap<Long, HolyLand>();
  private final Map<ChunkKey, NeutralLand> neutralLands =
      new LinkedHashMap<ChunkKey, NeutralLand>();
  // Entries under Holylands/NeutralLands that aren't a land or a chunk: "Holylands.<world>.<key>"
  // -> value
  private final Map<String, Object> unknownEntries = new LinkedHashMap<String, Object>();
  // Neutral lands whose time could not be parsed: the value as it was, written back while the land
  // has no time
  private final Map<ChunkKey, Object> rawNeutralTimes = new LinkedHashMap<ChunkKey, Object>();

  /** The data version and migrations of holyland.yml */
  static VersionedYaml format(Logger logger) {
    return new VersionedYaml(
        VERSION_KEY,
        DATA_VERSION,
        1,
        List.of(new HolyLandsV1ToV2(logger), new HolyLandsV2ToV3(logger)));
  }

  HolyLandRepository(DataFile file, Logger logger) {
    super(file, logger);
  }

  /** The holy land a chunk belongs to, or null */
  public HolyLand find(ChunkKey chunk) {
    return this.byChunk.get(chunk);
  }

  /** The land with this id in this world (key), or null */
  public HolyLand findById(String world, String id) {
    return this.lands.get(world + "." + id);
  }

  /**
   * A new, unclaimed land around {@code center}, which must be free (no land holds it). Its id is
   * the center's {@code "x,z"} (with a suffix in the unlikely case that a land of that id exists).
   */
  public HolyLand create(ChunkKey center) {
    String id = center.coordinates();
    for (int n = 2; this.lands.containsKey(center.world() + "." + id); n++) {
      id = center.coordinates() + "#" + n;
    }
    HolyLand land = new HolyLand(id, center);
    this.lands.put(center.world() + "." + id, land);
    this.byChunk.put(center, land);
    return land;
  }

  /** The claim land of this GriefPrevention claim, or null */
  public HolyLand findClaim(long claimId) {
    return this.byClaim.get(claimId);
  }

  /** Whether any holy land is a claim land (lookups by location only ask GriefPrevention then) */
  public boolean hasClaimLands() {
    return !this.byClaim.isEmpty();
  }

  /** The claim lands */
  public Collection<HolyLand> claimLands() {
    return Collections.unmodifiableCollection(this.byClaim.values());
  }

  /**
   * A new claim land of the GriefPrevention claim {@code claimId}, which must not be one yet;
   * {@code center} is the chunk of its altar block (and gives its world). Its id is {@code
   * "claim-<id>"}.
   */
  public HolyLand createClaim(ChunkKey center, long claimId) {
    String id = CLAIM_ID_PREFIX + claimId;
    for (int n = 2; this.lands.containsKey(center.world() + "." + id); n++) {
      id = CLAIM_ID_PREFIX + claimId + "#" + n;
    }
    HolyLand land = new HolyLand(id, center, claimId);
    this.lands.put(center.world() + "." + id, land);
    this.byClaim.put(claimId, land);
    return land;
  }

  /**
   * Adds a free chunk (of the land's world) to the land; false if another land holds it or the land
   * is a claim land
   */
  public boolean addChunk(HolyLand land, ChunkKey chunk) {
    if (land.isClaim()) {
      return false;
    }
    HolyLand holder = this.byChunk.get(chunk);
    if (holder != null) {
      return holder == land;
    }
    land.chunks().add(chunk);
    this.byChunk.put(chunk, land);
    return true;
  }

  public void remove(HolyLand land) {
    if (land == null) {
      return;
    }
    this.lands.remove(land.world() + "." + land.id());
    for (ChunkKey chunk : land.chunks()) {
      this.byChunk.remove(chunk, land);
    }
    if (land.isClaim()) {
      this.byClaim.remove(land.claimId(), land);
    }
  }

  public Collection<HolyLand> all() {
    return Collections.unmodifiableCollection(this.lands.values());
  }

  public NeutralLand findNeutral(ChunkKey chunk) {
    return this.neutralLands.get(chunk);
  }

  public void putNeutral(NeutralLand land) {
    this.neutralLands.put(land.chunk(), land);
    this.rawNeutralTimes.remove(land.chunk());
  }

  public void removeNeutral(ChunkKey chunk) {
    this.neutralLands.remove(chunk);
    this.rawNeutralTimes.remove(chunk);
  }

  public Collection<NeutralLand> allNeutral() {
    return Collections.unmodifiableCollection(this.neutralLands.values());
  }

  /** The number of holy lands */
  @Override
  public int size() {
    return this.lands.size();
  }

  @Override
  protected void clear() {
    this.lands.clear();
    this.byChunk.clear();
    this.byClaim.clear();
    this.neutralLands.clear();
    this.unknownEntries.clear();
    this.rawNeutralTimes.clear();
  }

  @Override
  protected void read(YamlConfiguration yaml) {
    for (String key : yaml.getKeys(false)) {
      if (key.equals(HOLY_LANDS)) {
        forEachEntry(
            yaml.getConfigurationSection(key),
            HOLY_LANDS,
            (world, id, value) -> {
              if (!(value instanceof ConfigurationSection section)) {
                return false;
              }
              return readLand(world, id, section);
            });
      } else if (key.equals(NEUTRAL_LANDS)) {
        forEachEntry(
            yaml.getConfigurationSection(key),
            NEUTRAL_LANDS,
            (world, id, value) -> {
              ChunkKey chunk = ChunkKey.parse(world, id);
              if (chunk == null || value instanceof ConfigurationSection) {
                return false;
              }
              Instant since = DATES.parse(value != null ? value.toString() : null);
              if (since == null && value != null && !value.toString().isEmpty()) {
                this.rawNeutralTimes.put(chunk, value);
              }
              this.neutralLands.put(chunk, new NeutralLand(chunk, since));
              return true;
            });
      } else {
        // data-version, and whatever a newer version added
        keepUnknown(key, yaml.get(key));
      }
    }

    logger()
        .info(
            "Loaded "
                + this.lands.size()
                + " holy lands ("
                + this.byChunk.size()
                + " chunks, "
                + this.byClaim.size()
                + " claim lands) and "
                + this.neutralLands.size()
                + " neutral chunks.");
  }

  private interface EntryReader {
    /** Returns false if the value is not what was expected; it is then kept as it is */
    boolean read(String world, String id, Object value);
  }

  private void forEachEntry(ConfigurationSection lands, String sectionName, EntryReader reader) {
    if (lands == null) {
      return;
    }
    for (String world : lands.getKeys(false)) {
      ConfigurationSection worldLands = lands.getConfigurationSection(world);
      if (worldLands == null) {
        this.unknownEntries.put(sectionName + "." + world, toPlain(lands.get(world)));
        continue;
      }
      for (String id : worldLands.getKeys(false)) {
        Object value = worldLands.get(id);
        if (!reader.read(world, id, value)) {
          this.unknownEntries.put(sectionName + "." + world + "." + id, toPlain(value));
        }
      }
    }
  }

  private boolean readLand(String world, String id, ConfigurationSection section) {
    ChunkKey center = ChunkKey.parse(world, section.getString(CENTER));
    if (center == null) {
      center = ChunkKey.parse(world, id);
    }
    if (center == null || this.lands.containsKey(world + "." + id)) {
      return false;
    }
    Long claimId = null;
    if (section.contains(CLAIM)) {
      claimId = readClaimId(section.get(CLAIM));
      if (claimId == null || this.byClaim.containsKey(claimId)) {
        logger()
            .warning(
                "holyland.yml: the holy land "
                    + HOLY_LANDS
                    + "."
                    + world
                    + "."
                    + id
                    + " has "
                    + (claimId == null
                        ? "no claim id Gods can read"
                        : "the claim " + claimId + " of another holy land")
                    + "; it is kept in the file but not used.");
        return false;
      }
    }
    HolyLand land = claimId != null ? new HolyLand(id, center, claimId) : new HolyLand(id, center);
    // Times that can't be parsed are kept in the extras and written back as they were
    Map<String, Object> extras = extraKeys(section, land.isClaim() ? KNOWN_CLAIM : KNOWN);
    land.setGodName(section.getString(GOD_NAME));
    land.setName(section.getString(NAME));
    land.setWorldName(section.getString(WORLD));
    land.setFirstPrayerTime(readTime(section, FIRST_PRAYER_TIME, DATES, extras));
    land.setLastPrayerTime(readTime(section, LAST_PRAYER_TIME, DATES, extras));
    land.setAttackingGodName(section.getString(ATTACKING_GOD_NAME));
    land.setContestedTime(readTime(section, CONTESTED_TIME, DATES, extras));
    land.setAttackerKills(section.getInt(ATTACKER_KILLS));
    land.setDefenderKills(section.getInt(DEFENDER_KILLS));
    land.setDesecration(section.getInt(DESECRATION));
    land.setExtras(extras);

    if (land.isClaim()) {
      this.lands.put(world + "." + id, land);
      this.byClaim.put(land.claimId(), land);
      return true;
    }

    List<String> unreadable = new ArrayList<String>();
    for (String coordinates : section.getStringList(CHUNKS)) {
      ChunkKey chunk = ChunkKey.parse(world, coordinates);
      if (chunk == null) {
        unreadable.add(coordinates);
      } else {
        land.chunks().add(chunk);
      }
    }
    if (!unreadable.isEmpty()) {
      logger()
          .warning(
              "holyland.yml: ignored the chunks "
                  + unreadable
                  + " of the holy land "
                  + HOLY_LANDS
                  + "."
                  + world
                  + "."
                  + id
                  + " (not \"x,z\").");
    }

    this.lands.put(world + "." + id, land);
    for (ChunkKey chunk : land.chunks()) {
      // A chunk two lands claim (a hand edit) belongs to the first
      HolyLand holder = this.byChunk.putIfAbsent(chunk, land);
      if (holder != null && holder != land) {
        logger()
            .warning(
                "holyland.yml: the chunk "
                    + chunk.coordinates()
                    + " in "
                    + world
                    + " belongs to two holy lands; it counts for "
                    + holder.id()
                    + ".");
      }
    }
    return true;
  }

  /** A claim id written as a whole number (or text holding one), else null */
  private static Long readClaimId(Object value) {
    if (value instanceof Number number && !(value instanceof Double) && !(value instanceof Float)) {
      return number.longValue();
    }
    if (value instanceof String text) {
      try {
        return Long.parseLong(text.trim());
      } catch (NumberFormatException e) {
        return null;
      }
    }
    return null;
  }

  @Override
  protected void write(YamlConfiguration yaml) {
    for (HolyLand land : this.lands.values()) {
      ConfigurationSection section =
          yaml.createSection(HOLY_LANDS + "." + land.world() + "." + land.id());
      section.set(GOD_NAME, land.godName());
      section.set(NAME, land.name());
      section.set(WORLD, land.worldName());
      section.set(CENTER, land.center().coordinates());
      if (land.isClaim()) {
        section.set(CLAIM, land.claimId());
      } else {
        List<String> chunks = new ArrayList<String>(land.chunks().size());
        for (ChunkKey chunk : land.chunks()) {
          chunks.add(chunk.coordinates());
        }
        section.set(CHUNKS, chunks);
      }
      section.set(FIRST_PRAYER_TIME, DATES.format(land.firstPrayerTime()));
      section.set(LAST_PRAYER_TIME, DATES.format(land.lastPrayerTime()));
      if (land.attackingGodName() != null) {
        section.set(ATTACKING_GOD_NAME, land.attackingGodName());
        section.set(CONTESTED_TIME, DATES.format(land.contestedTime()));
        section.set(ATTACKER_KILLS, land.attackerKills());
        section.set(DEFENDER_KILLS, land.defenderKills());
        section.set(DESECRATION, land.desecration());
      }
      writeExtras(section, land.extras());
    }
    for (NeutralLand land : this.neutralLands.values()) {
      String since = DATES.format(land.since());
      Object raw = this.rawNeutralTimes.get(land.chunk());
      yaml.set(
          NEUTRAL_LANDS + "." + land.chunk().world() + "." + land.chunk().coordinates(),
          since != null ? since : raw != null ? raw : "");
    }
    for (Map.Entry<String, Object> entry : this.unknownEntries.entrySet()) {
      if (!yaml.contains(entry.getKey())) {
        yaml.set(entry.getKey(), entry.getValue());
      }
    }
  }
}
