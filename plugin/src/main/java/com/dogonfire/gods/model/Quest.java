package com.dogonfire.gods.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A god's current quest: one section per god name in quests.yml, with the keys noted on the fields.
 * A god has a quest while its section has a {@code Type}. Missing numbers read as 0, as they always
 * did; a missing {@code Progress}, {@code Amount} or {@code Players} stays missing in the file.
 * {@code CreatedTime} is {@code "HH:mm dd-MM-yyyy"}. A plain data object: {@code QuestService}
 * changes it.
 */
public final class Quest {
  private final String godName;

  private String typeName; // Type, as in the file ("Pilgrimage"); null: no quest
  private Integer amount; // Amount: how many to slay, sacrifice, convert, ...
  private Integer progress; // Progress: how many are done
  private String
      targetType; // TargetType: the mob, the item, the holy book title, the holy mountain
  private Integer maxDuration; // MaxDuration, in minutes
  private List<String>
      players; // Players: UUIDs of the players that counted already (GIVEITEMS, CONVERT)
  private List<String>
      contributors; // Contributors: UUIDs of the believers whose deeds counted (they earn devotion)
  private QuestLocation location; // Location.World/X/Y/Z/Ground: the pilgrimage chest
  private Instant createdTime; // CreatedTime

  /** Keys of the section this version doesn't know, written back unchanged */
  private Map<String, Object> extras = new LinkedHashMap<String, Object>();

  public Quest(String godName) {
    this.godName = godName;
  }

  public String godName() {
    return this.godName;
  }

  /** The type as stored; null if the section has none (then the god has no quest) */
  public String typeName() {
    return this.typeName;
  }

  /** The type, or {@link QuestType#NONE} if there is none or it is not known */
  public QuestType type() {
    return QuestType.parse(this.typeName);
  }

  public void setType(QuestType type) {
    this.extras.remove("Type");
    this.typeName = (type != null) ? type.toString() : null;
  }

  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }

  /** Null if not stored */
  public Integer amount() {
    return this.amount;
  }

  public int amountOrZero() {
    return (this.amount != null) ? this.amount : 0;
  }

  public void setAmount(Integer amount) {
    this.extras.remove("Amount");
    this.amount = amount;
  }

  /** Null if not stored */
  public Integer progress() {
    return this.progress;
  }

  public int progressOrZero() {
    return (this.progress != null) ? this.progress : 0;
  }

  public void setProgress(Integer progress) {
    this.extras.remove("Progress");
    this.progress = progress;
  }

  public String targetType() {
    return this.targetType;
  }

  public void setTargetType(String targetType) {
    this.targetType = targetType;
  }

  /** Null if not stored */
  public Integer maxDuration() {
    return this.maxDuration;
  }

  public int maxDurationOrZero() {
    return (this.maxDuration != null) ? this.maxDuration : 0;
  }

  public void setMaxDuration(Integer maxDuration) {
    this.extras.remove("MaxDuration");
    this.maxDuration = maxDuration;
  }

  /** Null if not stored */
  public List<String> players() {
    return this.players;
  }

  /** The players as a new list to change (empty if none are stored) */
  public List<String> playersOrEmpty() {
    return (this.players != null) ? new ArrayList<String>(this.players) : new ArrayList<String>();
  }

  public void setPlayers(List<String> players) {
    this.players = players;
  }

  /**
   * The believers whose deeds counted for it (slain mobs, sacrifices, the pilgrimage); null if none
   * are stored
   */
  public List<String> contributors() {
    return this.contributors;
  }

  public void setContributors(List<String> contributors) {
    this.contributors = contributors;
  }

  public QuestLocation location() {
    return this.location;
  }

  public void setLocation(QuestLocation location) {
    this.extras.remove("Location");
    this.location = location;
  }

  /** Null if not stored or not readable */
  public Instant createdTime() {
    return this.createdTime;
  }

  public void setCreatedTime(Instant createdTime) {
    this.createdTime = createdTime;
  }

  public Map<String, Object> extras() {
    return this.extras;
  }

  public void setExtras(Map<String, Object> extras) {
    this.extras = extras;
  }
}
