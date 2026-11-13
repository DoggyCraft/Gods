package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.dogonfire.gods.model.Altar;
import com.dogonfire.gods.model.AltarKind;
import com.dogonfire.gods.model.BlockPos;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MainAltarRulesTest {
  private static final BlockPos BLOCK = new BlockPos("world", 10, 64, 10);
  private static final Altar PRAYING =
      new Altar(new BlockPos("world", 10, 64, 11), BLOCK, AltarKind.PRAYING, "Zeus", null, null);
  private static final Altar CURSES =
      new Altar(new BlockPos("world", 11, 64, 10), BLOCK, AltarKind.CURSES, "Zeus", null, null);
  private static final Altar OTHER_GOD =
      new Altar(new BlockPos("world", 9, 64, 10), BLOCK, AltarKind.PRAYING, "Hera", null, null);

  private final Map<BlockPos, Altar> bySign = new HashMap<>();
  private final Map<BlockPos, List<Altar>> byBlock = new HashMap<>();

  private List<Altar> candidates(BlockPos looked) {
    return MainAltarRules.candidates(
        looked, this.bySign::get, block -> this.byBlock.getOrDefault(block, List.of()));
  }

  private void register(Altar... altars) {
    for (Altar altar : altars) {
      this.bySign.put(altar.sign(), altar);
      this.byBlock.computeIfAbsent(altar.block(), b -> new java.util.ArrayList<>()).add(altar);
    }
  }

  @Test
  void theSignTheBlockOrTheLightNameTheAltar() {
    register(CURSES, PRAYING);
    assertEquals(List.of(PRAYING), candidates(PRAYING.sign()));
    assertEquals(List.of(CURSES, PRAYING), candidates(BLOCK));
    // The light on top
    assertEquals(List.of(CURSES, PRAYING), candidates(BLOCK.up(1)));
    assertEquals(List.of(), candidates(BLOCK.up(2)));
  }

  @Test
  void onlyAPrayingAltarOfThePriestsGod() {
    assertEquals(
        new MainAltarRules.Target(MainAltarRules.Verdict.OK, PRAYING),
        MainAltarRules.choose(List.of(CURSES, OTHER_GOD, PRAYING), "Zeus"));
    assertEquals(
        MainAltarRules.Verdict.NO_ALTAR, MainAltarRules.choose(List.of(), "Zeus").verdict());
    assertEquals(
        MainAltarRules.Verdict.NOT_PRAYING,
        MainAltarRules.choose(List.of(CURSES), "Zeus").verdict());
    MainAltarRules.Target other = MainAltarRules.choose(List.of(CURSES, OTHER_GOD), "Zeus");
    assertEquals(MainAltarRules.Verdict.OTHER_GOD, other.verdict());
    assertNull(other.altar());
  }

  @Test
  void aGodStandsInFrontOfTheSign() {
    // The sign hangs south of its block, so the spot is one further south
    assertEquals(new BlockPos("world", 10, 64, 12), MainAltarRules.standingSpot(PRAYING));
    assertEquals(new BlockPos("world", 12, 64, 10), MainAltarRules.standingSpot(CURSES));
  }

  private static Altar praying(int x, Instant created) {
    return new Altar(
        new BlockPos("world", x, 64, 1),
        new BlockPos("world", x, 64, 0),
        AltarKind.PRAYING,
        "Zeus",
        created,
        null);
  }

  /** A god without a main altar gets the oldest of its praying altars whose time is known */
  @Test
  void theOldestKnownPrayingAltarBecomesTheMainAltar() {
    Altar found = praying(1, null);
    Altar newer = praying(2, Instant.parse("2026-05-02T10:00:00Z"));
    Altar older = praying(3, Instant.parse("2026-05-01T10:00:00Z"));
    assertSame(older, MainAltarRules.automatic(List.of(CURSES, found, newer, older)));
    // With one left (the main altar was destroyed), that one
    assertSame(newer, MainAltarRules.automatic(List.of(found, newer)));
  }

  /** Altars found or used rather than built have no time: the first the registry found */
  @Test
  void withoutTimesTheFirstFoundBecomesTheMainAltar() {
    Altar first = praying(1, null);
    Altar second = praying(2, null);
    assertSame(first, MainAltarRules.automatic(List.of(CURSES, first, second)));
    // The first altar registered for a god from before main altars
    assertSame(second, MainAltarRules.automatic(List.of(second)));
  }

  /** No praying altar left: no main altar */
  @Test
  void noPrayingAltarNoMainAltar() {
    assertNull(MainAltarRules.automatic(List.of()));
    assertNull(MainAltarRules.automatic(List.of(CURSES)));
  }
}
