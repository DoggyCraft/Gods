package com.dogonfire.gods.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class GodSkinCommandTest {
  /**
   * /g godskin without a name takes the priest's own skin, as /g godskin with their own name does
   */
  @Test
  void withoutANameThePriestsOwnSkin() {
    assertEquals("Hero", GodSkinCommand.skinOf(List.of(), "Hero"));
    assertEquals("Hero", GodSkinCommand.skinOf(List.of("Hero"), "Hero"));
    assertEquals("Notch", GodSkinCommand.skinOf(List.of("Notch"), "Hero"));
    assertEquals(
        GodSkinCommand.RESET, GodSkinCommand.skinOf(List.of(GodSkinCommand.RESET), "Hero"));
  }

  @Test
  void tabCompletionOffersThePriestFirst() {
    assertEquals(
        List.of("Hero", "Notch", "jeb_", GodSkinCommand.RESET),
        GodSkinCommand.completions("Hero", List.of("Notch", "hero", "jeb_")));
    assertEquals(
        List.of("Hero", GodSkinCommand.RESET), GodSkinCommand.completions("Hero", List.of()));
  }
}
