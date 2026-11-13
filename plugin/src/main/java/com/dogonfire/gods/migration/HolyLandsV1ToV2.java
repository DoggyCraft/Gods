package com.dogonfire.gods.migration;

import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Upgrades holyland.yml to version 2, the first with a {@code data-version}.
 *
 * <p>Version 1 is every file without a version: from Gods 0.9.x, which kept lands under {@code
 * Holyland.<hash>} and {@code NeutralLand.<hash>}, and from 1.0 before data versions, which keeps
 * them under {@code Holylands.<world>.<hash>} and {@code NeutralLands.<world>.<hash>} (unchanged in
 * version 2). The 0.9.x hash put many regions, from every world, under the same key, so which land
 * was where is lost and those entries can't be converted: they are dropped (the backup {@link
 * VersionedYaml} keeps has them). Gods claim their land again when believers build or pray at their
 * altars.
 */
public final class HolyLandsV1ToV2 implements Migration {
  public static final String LEGACY_HOLY_LANDS = "Holyland";
  public static final String LEGACY_NEUTRAL_LANDS = "NeutralLand";

  private final Logger log;

  public HolyLandsV1ToV2(Logger log) {
    this.log = log;
  }

  @Override
  public int fromVersion() {
    return 1;
  }

  @Override
  public YamlConfiguration migrate(YamlConfiguration old) {
    if (!old.contains(LEGACY_HOLY_LANDS) && !old.contains(LEGACY_NEUTRAL_LANDS)) {
      return old;
    }

    old.set(LEGACY_HOLY_LANDS, null);
    old.set(LEGACY_NEUTRAL_LANDS, null);

    this.log.warning(
        "holyland.yml was in the old format, whose land positions were broken and can't be converted.");
    this.log.warning(
        "All holy lands and neutral lands were reset; the old file is kept as the holyland.yml.v1 backup.");
    this.log.warning(
        "Gods claim holy land again when believers build or pray at their altars. Set neutral lands again with /g setsafe.");
    return old;
  }
}
