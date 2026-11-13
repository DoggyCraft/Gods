package com.dogonfire.gods.integration;

import com.dogonfire.gods.service.AltarClaims;
import com.dogonfire.gods.service.HolyClaimRules;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * GriefPrevention claims. Only created when GriefPrevention is installed and enabled (see {@link
 * com.dogonfire.gods.GodsContext}), since this class references GriefPrevention's classes. Claims
 * leave it as Gods' own {@link HolyClaimRules.ClaimInfo}, never as GriefPrevention's types.
 */
public final class GriefPreventionIntegration {
  /** Where a location is, as far as building an altar there is concerned ({@link AltarClaims}) */
  public AltarClaims.Claim claimAt(Player player, Location location) {
    GriefPrevention griefPrevention = GriefPrevention.instance;
    if (griefPrevention == null
        || location.getWorld() == null
        || !griefPrevention.claimsEnabledForWorld(location.getWorld())) {
      return AltarClaims.Claim.NO_CLAIMS_IN_WORLD;
    }
    Claim claim = griefPrevention.dataStore.getClaimAt(location, true, null);
    if (claim == null) {
      return AltarClaims.Claim.NONE;
    }
    // Null means allowed: the owner, a player with build trust, or an admin ignoring claims
    return claim.checkPermission(player, ClaimPermission.Build, null) == null
        ? AltarClaims.Claim.TRUSTED
        : AltarClaims.Claim.UNTRUSTED;
  }

  /**
   * The claim at a location, at any height: the top-level claim with {@code topLevel}, else the
   * subclaim there if there is one. Null outside claims. GriefPrevention looks claims up by chunk,
   * so this is cheap.
   */
  public HolyClaimRules.ClaimInfo claimInfoAt(Location location, boolean topLevel) {
    GriefPrevention griefPrevention = GriefPrevention.instance;
    if (griefPrevention == null || location.getWorld() == null) {
      return null;
    }
    return info(griefPrevention.dataStore.getClaimAt(location, true, topLevel, null));
  }

  /** The id of the top-level claim at a location (at any height), or null outside claims */
  public Long topLevelClaimIdAt(Location location) {
    GriefPrevention griefPrevention = GriefPrevention.instance;
    if (griefPrevention == null || location.getWorld() == null) {
      return null;
    }
    Claim claim = griefPrevention.dataStore.getClaimAt(location, true, true, null);
    return claim != null ? claim.getID() : null;
  }

  /** The claim of this id as GriefPrevention has it now, or null if it has none (deleted) */
  public HolyClaimRules.ClaimInfo claim(long id) {
    GriefPrevention griefPrevention = GriefPrevention.instance;
    if (griefPrevention == null) {
      return null;
    }
    Claim claim = griefPrevention.dataStore.getClaim(id);
    return claim != null && claim.inDataStore ? info(claim) : null;
  }

  static HolyClaimRules.ClaimInfo info(Claim claim) {
    if (claim == null || claim.getID() == null) {
      return null;
    }
    Location lesser = claim.getLesserBoundaryCorner();
    Location greater = claim.getGreaterBoundaryCorner();
    String world = lesser.getWorld() != null ? lesser.getWorld().getName() : null;
    return new HolyClaimRules.ClaimInfo(
        claim.getID(),
        claim.isAdminClaim() ? null : claim.getOwnerID(),
        claim.isAdminClaim(),
        claim.parent == null,
        world,
        Math.min(lesser.getBlockX(), greater.getBlockX()),
        Math.min(lesser.getBlockZ(), greater.getBlockZ()),
        Math.max(lesser.getBlockX(), greater.getBlockX()),
        Math.max(lesser.getBlockZ(), greater.getBlockZ()));
  }
}
