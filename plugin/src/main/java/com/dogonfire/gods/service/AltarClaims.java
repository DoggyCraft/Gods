package com.dogonfire.gods.service;

/**
 * Whether a new altar may be built somewhere, as far as land claims go (config.yml's {@code
 * altars.require-claim}), without the server so it can be tested. It only applies while
 * GriefPrevention is installed: then new altars stand only in a claim where the builder has build
 * trust. Altars that already stand are never checked.
 */
public final class AltarClaims {
  /** What GriefPrevention says about a spot */
  public enum Claim {
    /** GriefPrevention has claims switched off in this world, so nobody can claim it */
    NO_CLAIMS_IN_WORLD,
    /** Not in any claim */
    NONE,
    /** In a claim where the player may build (its owner, trusted, or an admin ignoring claims) */
    TRUSTED,
    /** In a claim where the player may not build */
    UNTRUSTED
  }

  private AltarClaims() {}

  /**
   * True if new altars need a claim: the setting is on and GriefPrevention is installed and enabled
   */
  public static boolean applies(boolean requireClaim, boolean installed) {
    return requireClaim && installed;
  }

  /**
   * True if a new altar may be built where it covers these spots (its sign and its block), when
   * {@link #applies}: each must be in a claim the builder may build in, or in a world without
   * claims.
   */
  public static boolean mayBuild(Claim... spots) {
    for (Claim spot : spots) {
      if (spot != Claim.TRUSTED && spot != Claim.NO_CLAIMS_IN_WORLD) {
        return false;
      }
    }
    return true;
  }
}
