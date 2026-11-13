package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dogonfire.gods.model.Altar;
import com.dogonfire.gods.model.AltarKind;
import com.dogonfire.gods.model.BlockPos;
import java.util.List;
import org.junit.jupiter.api.Test;

class AltarScanTest {
  private static BlockPos pos(int x, int y, int z) {
    return new BlockPos("world", x, y, z);
  }

  private static Altar registered(BlockPos sign, BlockPos block, AltarKind kind, String god) {
    return new Altar(sign, block, kind, god, null, null);
  }

  @Test
  void aChunkThatMatchesChangesNothing() {
    Altar zeus = registered(pos(1, 64, 1), pos(1, 64, 0), AltarKind.PRAYING, "Zeus");
    AltarScan.Changes changes =
        AltarScan.reconcile(
            List.of(zeus),
            List.of(new AltarScan.Found(pos(1, 64, 1), pos(1, 64, 0), AltarKind.PRAYING, "Zeus")));
    assertTrue(changes.isEmpty());
  }

  @Test
  void signsThatAreGoneOrUntaggedAreRemoved() {
    Altar zeus = registered(pos(1, 64, 1), pos(1, 64, 0), AltarKind.PRAYING, "Zeus");
    Altar hera = registered(pos(5, 64, 1), pos(5, 64, 0), AltarKind.PRAYING, "Hera");
    // Only Hera's sign is still a tagged altar sign
    AltarScan.Changes changes =
        AltarScan.reconcile(
            List.of(zeus, hera),
            List.of(new AltarScan.Found(pos(5, 64, 1), pos(5, 64, 0), AltarKind.PRAYING, "Hera")));
    assertEquals(List.of(zeus.sign()), changes.removed());
    assertEquals(List.of(), changes.added());
    assertEquals(List.of(), changes.changed());
  }

  @Test
  void taggedSignsTheRegistryLacksAreAdded() {
    AltarScan.Found found =
        new AltarScan.Found(pos(2, 70, 2), pos(3, 70, 2), AltarKind.BLESSINGS, "Odin");
    AltarScan.Changes changes = AltarScan.reconcile(List.of(), List.of(found));
    assertEquals(List.of(found), changes.added());
    assertFalse(changes.isEmpty());
  }

  @Test
  void aSignThatSaysOtherwiseIsUpdated() {
    Altar zeus = registered(pos(1, 64, 1), pos(1, 64, 0), AltarKind.PRAYING, "Zeus");
    AltarScan.Found otherGod =
        new AltarScan.Found(pos(1, 64, 1), pos(1, 64, 0), AltarKind.PRAYING, "Hera");
    AltarScan.Found otherKind =
        new AltarScan.Found(pos(1, 64, 1), pos(1, 64, 0), AltarKind.CURSES, "Zeus");
    AltarScan.Found otherBlock =
        new AltarScan.Found(pos(1, 64, 1), pos(2, 64, 1), AltarKind.PRAYING, "Zeus");
    for (AltarScan.Found found : List.of(otherGod, otherKind, otherBlock)) {
      AltarScan.Changes changes = AltarScan.reconcile(List.of(zeus), List.of(found));
      assertEquals(List.of(found), changes.changed(), found.toString());
      assertEquals(List.of(), changes.removed());
      assertEquals(List.of(), changes.added());
    }
  }

  @Test
  void aGodlessCurseSignMatchesItself() {
    Altar curse = registered(pos(1, 64, 1), pos(1, 64, 0), AltarKind.CURSES, null);
    assertTrue(
        AltarScan.matches(
            curse, new AltarScan.Found(pos(1, 64, 1), pos(1, 64, 0), AltarKind.CURSES, null)));
  }
}
