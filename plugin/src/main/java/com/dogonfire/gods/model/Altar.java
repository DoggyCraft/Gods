package com.dogonfire.gods.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * One altar Gods knows of (altars.yml): its wall sign, the altar block the sign hangs on, what the
 * sign is for and its god (a praying altar's god; on a curse or blessing sign the god of whoever
 * made or tagged it), when it was registered and who built it. The sign identifies the altar: an
 * altar block may carry several signs, each its own altar. Altars found in the world (not built
 * while Gods watched) have no creator and no creation time. A plain data object: {@code
 * AltarRegistry} changes it.
 */
public final class Altar {
  private final BlockPos sign;
  private final BlockPos block;
  private AltarKind kind;
  private String god;
  private final Instant created;
  private final UUID creator;

  /** Keys of the entry this version doesn't know, written back unchanged */
  private Map<String, Object> extras = new LinkedHashMap<String, Object>();

  /**
   * @param sign the wall sign
   * @param block the block the sign hangs on (in the same world)
   * @param created when it was registered, or null if that isn't known
   * @param creator who built it, or null if that isn't known
   */
  public Altar(
      BlockPos sign, BlockPos block, AltarKind kind, String god, Instant created, UUID creator) {
    this.sign = Objects.requireNonNull(sign);
    this.block = Objects.requireNonNull(block);
    this.kind = Objects.requireNonNull(kind);
    this.god = god;
    this.created = created;
    this.creator = creator;
  }

  public String world() {
    return this.sign.world();
  }

  public BlockPos sign() {
    return this.sign;
  }

  public BlockPos block() {
    return this.block;
  }

  public AltarKind kind() {
    return this.kind;
  }

  public void setKind(AltarKind kind) {
    this.kind = Objects.requireNonNull(kind);
  }

  /** The god; null for a curse or blessing sign of a player without a god */
  public String god() {
    return this.god;
  }

  public void setGod(String god) {
    this.god = god;
  }

  public Instant created() {
    return this.created;
  }

  public UUID creator() {
    return this.creator;
  }

  public Map<String, Object> extras() {
    return this.extras;
  }

  public void setExtras(Map<String, Object> extras) {
    this.extras = extras;
  }

  @Override
  public String toString() {
    return this.kind.key() + " altar of " + this.god + " at " + this.sign;
  }
}
