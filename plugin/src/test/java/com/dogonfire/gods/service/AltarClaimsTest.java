package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dogonfire.gods.service.AltarClaims.Claim;
import org.junit.jupiter.api.Test;

class AltarClaimsTest {
  /** Without GriefPrevention, or with the setting off, altars stand anywhere */
  @Test
  void onlyAppliesWithGriefPreventionAndTheSetting() {
    assertTrue(AltarClaims.applies(true, true));
    assertFalse(AltarClaims.applies(true, false));
    assertFalse(AltarClaims.applies(false, true));
    assertFalse(AltarClaims.applies(false, false));
  }

  @Test
  void onlyInAClaimTheBuilderMayBuildIn() {
    assertTrue(AltarClaims.mayBuild(Claim.TRUSTED));
    assertTrue(AltarClaims.mayBuild(Claim.TRUSTED, Claim.TRUSTED));
    assertFalse(AltarClaims.mayBuild(Claim.NONE));
    assertFalse(AltarClaims.mayBuild(Claim.UNTRUSTED));
    // The sign in the claim, the altar block just outside it
    assertFalse(AltarClaims.mayBuild(Claim.TRUSTED, Claim.NONE));
    assertFalse(AltarClaims.mayBuild(Claim.UNTRUSTED, Claim.TRUSTED));
  }

  /**
   * Nobody can claim land in a world GriefPrevention has claims off in, so altars stand there as
   * before
   */
  @Test
  void worldsWithoutClaimsAreFree() {
    assertTrue(AltarClaims.mayBuild(Claim.NO_CLAIMS_IN_WORLD, Claim.NO_CLAIMS_IN_WORLD));
  }
}
