package com.dogonfire.gods.storage;

import com.dogonfire.gods.migration.FirstDataVersion;
import com.dogonfire.gods.migration.VersionedYaml;
import com.dogonfire.gods.model.Quest;
import com.dogonfire.gods.model.QuestLocation;
import com.dogonfire.gods.model.QuestTarget;
import com.dogonfire.gods.model.QuestType;
import com.dogonfire.gods.util.LegacyDates;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * quests.yml: one {@link Quest} per god name and the {@link QuestTarget}s, with the keys Gods has
 * always written:
 *
 * <pre>
 * &lt;god name&gt;: Type, Amount, TargetType, MaxDuration, Progress, Players, Contributors, CreatedTime,
 *                 Location.World/X/Y/Z/Ground
 * QuestTargets.&lt;hash&gt;: God, Location.World/X/Y/Z
 * </pre>
 *
 * plus {@code data-version: 2}. Version 1 (no {@code data-version}) has the same keys; its upgrade
 * ({@link FirstDataVersion}) makes the {@code quests.yml.v1.bak} backup before the cleanup below
 * first rewrites it. Loading drops what Gods 0.9.x left that can't be used any more, and writes the
 * file back at once: the {@code Global} section of the removed global quests, and the quests (with
 * their targets) of the types that no longer exist (HOLYWAR, SLAYDRAGON, GETHOLYARTIFACT,
 * PVPREVENGE). Keys this version doesn't know, and values it can't read (an {@code Amount} that is
 * not a number), are kept unchanged.
 */
public final class QuestRepository extends Repository {
  public static final int DATA_VERSION = 2;
  public static final String VERSION_KEY = "data-version";

  public static final String TARGETS = "QuestTargets";

  /** The global quests of Gods 0.9.x, dropped on load */
  static final String GLOBAL = "Global";

  private static final String TYPE = "Type";
  private static final String AMOUNT = "Amount";
  private static final String TARGET_TYPE = "TargetType";
  private static final String MAX_DURATION = "MaxDuration";
  private static final String PROGRESS = "Progress";
  private static final String PLAYERS = "Players";
  private static final String CONTRIBUTORS = "Contributors";
  private static final String CREATED_TIME = "CreatedTime";
  private static final String LOCATION = "Location";
  private static final String WORLD = "World";
  private static final String GROUND = "Ground";
  private static final String GOD = "God";

  private static final Set<String> KNOWN =
      Set.of(
          TYPE,
          AMOUNT,
          TARGET_TYPE,
          MAX_DURATION,
          PROGRESS,
          PLAYERS,
          CONTRIBUTORS,
          CREATED_TIME,
          LOCATION);
  private static final Set<String> KNOWN_TARGET = Set.of(GOD, LOCATION);

  /**
   * Quest types Gods 0.9.x made that can no longer be completed or talked about; dropped on load
   */
  static final Set<QuestType> OBSOLETE =
      Set.of(
          QuestType.HOLYWAR, QuestType.SLAYDRAGON, QuestType.GETHOLYARTIFACT, QuestType.PVPREVENGE);

  private static final LegacyDates DATES = LegacyDates.MINUTES;

  private final Map<String, Quest> quests = new LinkedHashMap<String, Quest>();
  private final Map<String, QuestTarget> targets = new LinkedHashMap<String, QuestTarget>();
  // Entries under QuestTargets that are no target, written back unchanged
  private final Map<String, Object> otherTargetKeys = new LinkedHashMap<String, Object>();

  /** The data version and migrations of quests.yml */
  static VersionedYaml format() {
    return new VersionedYaml(VERSION_KEY, DATA_VERSION, 1, List.of(new FirstDataVersion()));
  }

  QuestRepository(DataFile file, Logger logger) {
    super(file, logger);
  }

  /** The god's quest section, or null */
  public Quest find(String godName) {
    return (godName != null) ? this.quests.get(godName) : null;
  }

  /** The god's quest section, created (empty) if there is none */
  public Quest getOrCreate(String godName) {
    return this.quests.computeIfAbsent(godName, Quest::new);
  }

  public void remove(String godName) {
    this.quests.remove(godName);
  }

  public Collection<Quest> all() {
    return Collections.unmodifiableCollection(this.quests.values());
  }

  /** The target stored under this hash, or null */
  public QuestTarget target(String key) {
    return this.targets.get(key);
  }

  public void putTarget(QuestTarget target) {
    this.targets.put(target.key(), target);
  }

  public void removeTarget(String key) {
    this.targets.remove(key);
  }

  public Collection<QuestTarget> targets() {
    return Collections.unmodifiableCollection(this.targets.values());
  }

  /** The number of god sections */
  @Override
  public int size() {
    return this.quests.size();
  }

  @Override
  protected void clear() {
    this.quests.clear();
    this.targets.clear();
    this.otherTargetKeys.clear();
  }

  @Override
  protected void read(YamlConfiguration yaml) {
    boolean cleaned = false;

    for (String key : yaml.getKeys(false)) {
      ConfigurationSection section = yaml.getConfigurationSection(key);

      if (key.equals(VERSION_KEY)) {
        continue;
      }
      if (key.equals(GLOBAL)) {
        cleaned = true;
        continue;
      }
      if (key.equals(TARGETS) && section != null) {
        readTargets(section);
        continue;
      }
      if (section == null) {
        keepUnknown(key, yaml.get(key));
        continue;
      }
      this.quests.put(key, readQuest(key, section));
    }

    for (Quest quest : new ArrayList<Quest>(this.quests.values())) {
      if (OBSOLETE.contains(quest.type())) {
        logger()
            .info(
                "Removing the "
                    + quest.type().name()
                    + " quest of "
                    + quest.godName()
                    + ": that quest type no longer exists");
        if (quest.location() != null) {
          removeTarget(QuestTarget.keyFor(quest.location()));
        }
        this.quests.remove(quest.godName());
        cleaned = true;
      }
    }

    if (cleaned) {
      markDirty();
    }

    logger().info("Loaded " + this.quests.size() + " quests.");
  }

  private static Quest readQuest(String godName, ConfigurationSection section) {
    Quest quest = new Quest(godName);
    Map<String, Object> extras = extraKeys(section, KNOWN);

    quest.setTypeName(section.getString(TYPE));
    quest.setAmount(readInt(section, AMOUNT, extras));
    quest.setTargetType(section.getString(TARGET_TYPE));
    quest.setMaxDuration(readInt(section, MAX_DURATION, extras));
    quest.setProgress(readInt(section, PROGRESS, extras));
    if (section.isList(PLAYERS)) {
      quest.setPlayers(new ArrayList<String>(section.getStringList(PLAYERS)));
    } else if (section.contains(PLAYERS)) {
      extras.put(PLAYERS, toPlain(section.get(PLAYERS)));
    }
    if (section.isList(CONTRIBUTORS)) {
      quest.setContributors(new ArrayList<String>(section.getStringList(CONTRIBUTORS)));
    } else if (section.contains(CONTRIBUTORS)) {
      extras.put(CONTRIBUTORS, toPlain(section.get(CONTRIBUTORS)));
    }
    // One that can't be parsed is kept in the extras and written back as it was
    quest.setCreatedTime(readTime(section, CREATED_TIME, DATES, extras));

    QuestLocation location = readLocation(section);
    if (location != null) {
      quest.setLocation(location);
    } else if (section.contains(LOCATION)) {
      extras.put(LOCATION, toPlain(section.get(LOCATION)));
    }

    quest.setExtras(extras);
    return quest;
  }

  private void readTargets(ConfigurationSection targetsSection) {
    for (String key : targetsSection.getKeys(false)) {
      ConfigurationSection section = targetsSection.getConfigurationSection(key);
      if (section == null) {
        // Not a target; kept as it is
        this.otherTargetKeys.put(key, toPlain(targetsSection.get(key)));
        continue;
      }

      Map<String, Object> extras = extraKeys(section, KNOWN_TARGET);
      QuestLocation location = readLocation(section);
      if (location == null && section.contains(LOCATION)) {
        extras.put(LOCATION, toPlain(section.get(LOCATION)));
      }
      QuestTarget target = new QuestTarget(key, section.getString(GOD), location);
      target.setExtras(extras);
      this.targets.put(key, target);
    }
  }

  /** Location.World/X/Y/Z(/Ground), or null if there is no world */
  private static QuestLocation readLocation(ConfigurationSection section) {
    ConfigurationSection location = section.getConfigurationSection(LOCATION);
    if (location == null || location.getString(WORLD) == null) {
      return null;
    }
    return new QuestLocation(
        location.getString(WORLD),
        location.getInt("X"),
        location.getInt("Y"),
        location.getInt("Z"),
        location.getString(GROUND));
  }

  /**
   * A number, or null if missing; a value that is not a number is kept in extras as it is (and
   * reads as 0)
   */
  private static Integer readInt(
      ConfigurationSection section, String key, Map<String, Object> extras) {
    Object value = section.get(key);
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.intValue();
    }
    extras.put(key, toPlain(value));
    return null;
  }

  @Override
  protected void write(YamlConfiguration yaml) {
    for (Quest quest : this.quests.values()) {
      ConfigurationSection section = yaml.createSection(quest.godName());
      section.set(TYPE, quest.typeName());
      section.set(AMOUNT, quest.amount());
      section.set(TARGET_TYPE, quest.targetType());
      section.set(MAX_DURATION, quest.maxDuration());
      section.set(PROGRESS, quest.progress());
      if (quest.players() != null) {
        section.set(PLAYERS, new ArrayList<String>(quest.players()));
      }
      if (quest.contributors() != null) {
        section.set(CONTRIBUTORS, new ArrayList<String>(quest.contributors()));
      }
      section.set(CREATED_TIME, DATES.format(quest.createdTime()));
      if (quest.location() != null) {
        writeLocation(section, quest.location());
      }
      writeExtras(section, quest.extras());
    }

    if (!this.targets.isEmpty() || !this.otherTargetKeys.isEmpty()) {
      ConfigurationSection targetsSection = yaml.createSection(TARGETS);
      writeExtras(targetsSection, this.otherTargetKeys);
      for (QuestTarget target : this.targets.values()) {
        ConfigurationSection section = targetsSection.createSection(target.key());
        section.set(GOD, target.godName());
        if (target.location() != null) {
          writeLocation(section, target.location());
        }
        writeExtras(section, target.extras());
      }
    }
  }

  private static void writeLocation(ConfigurationSection section, QuestLocation location) {
    ConfigurationSection out = section.createSection(LOCATION);
    out.set(WORLD, location.world());
    out.set("X", location.x());
    out.set("Y", location.y());
    out.set("Z", location.z());
    if (location.ground() != null) {
      out.set(GROUND, location.ground());
    }
  }
}
