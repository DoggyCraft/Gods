package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dogonfire.gods.service.SacrificeRules.Verdict;
import org.junit.jupiter.api.Test;

class SacrificeRulesTest {
  @Test
  void theValueDecidesHowASacrificeIsTaken() {
    assertEquals(Verdict.GOOD, SacrificeRules.verdict(10.5F));
    assertEquals(Verdict.MEH, SacrificeRules.verdict(10.0F));
    assertEquals(Verdict.MEH, SacrificeRules.verdict(-5.0F));
    assertEquals(Verdict.BAD, SacrificeRules.verdict(-5.5F));
    assertEquals(9.5F, SacrificeRules.afterSacrifice(10.5F));
  }

  @Test
  void onlyWholeValuesAboveTenAreAskedFor() {
    assertFalse(SacrificeRules.isAskedFor(10.9F));
    assertTrue(SacrificeRules.isAskedFor(11.0F));
  }

  @Test
  void wantsGrowUpToALimitAndUnwantedItemsAreForgotten() {
    assertEquals(13.0F, SacrificeRules.wantMore(10.0F, 3));
    assertEquals(64.0F, SacrificeRules.wantMore(63.0F, 3));
    assertEquals(-10.0F, SacrificeRules.forgetUnwanted(-40.0F));
    assertEquals(-0.5F, SacrificeRules.forgetUnwanted(-2.0F));
    assertEquals(0.0F, SacrificeRules.forgetUnwanted(-1.9F));
    assertEquals(0.0F, SacrificeRules.forgetUnwanted(0.0F));
  }
}
