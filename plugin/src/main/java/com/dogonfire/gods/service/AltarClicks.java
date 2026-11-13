package com.dogonfire.gods.service;

import java.util.List;
import java.util.Set;
import org.bukkit.block.BlockFace;

/**
 * Praying by right-clicking the altar block or its light instead of the sign, without the server so
 * it can be tested: which clicks pray (and which keep their vanilla use), and which of the altar's
 * signs they use.
 */
public final class AltarClicks {
  /** The sides of an altar block a wall sign can hang on */
  public static final List<BlockFace> SIDES =
      List.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);

  /**
   * Items that do something to the block they are used on (light candles, strip logs, wax copper,
   * ...), and signs (so a sign is put on an altar block without sneaking): vanilla wins
   */
  private static final Set<String> BLOCK_TOOLS =
      Set.of(
          "FLINT_AND_STEEL",
          "FIRE_CHARGE",
          "HONEYCOMB",
          "SHEARS",
          "BONE_MEAL",
          "BUCKET",
          "POTION",
          "BRUSH");

  private static final List<String> BLOCK_TOOL_SUFFIXES =
      List.of("_AXE", "_SHOVEL", "_HOE", "_BUCKET", "_SPAWN_EGG", "_SIGN");

  /** The part of an altar a click is on */
  public enum Part {
    /** The altar block (a block of an altar type with a light on top) */
    BLOCK,
    /** The light on top of an altar block */
    LIGHT,
    /** Anything else */
    NONE
  }

  /** One altar sign hanging on an altar block, as {@link #chooseSign} sees it */
  public record Candidate<T>(T sign, AltarSigns.Kind kind, boolean tagged) {}

  private AltarClicks() {}

  /**
   * True if a right click on the altar block or its light prays, as clicking the altar's sign does.
   * Never with the off hand, never while sneaking (sneak-clicking keeps vanilla's use, like placing
   * a block against the altar), never on a block with a use of its own (a note block, jukebox,
   * chiseled bookshelf, smithing table; a candle cake, which is eaten), and never with an item that
   * does something to the clicked block (flint and steel lights a candle, an axe strips or scrapes,
   * honeycomb waxes, a candle is added to candles of its color, ...).
   *
   * @param part what was clicked
   * @param mainHand the click is the main hand's
   * @param sneaking the player is sneaking
   * @param blockHasOwnUse the clicked altar block has a right-click use of its own (only asked for
   *     {@link Part#BLOCK})
   * @param clickedType the clicked block's Material name
   * @param heldType the Material name of the item in the main hand ("AIR" for none)
   */
  public static boolean praysOnClick(
      Part part,
      boolean mainHand,
      boolean sneaking,
      boolean blockHasOwnUse,
      String clickedType,
      String heldType) {
    if (part == Part.NONE || !mainHand || sneaking) {
      return false;
    }
    if (part == Part.BLOCK && blockHasOwnUse) {
      return false;
    }
    if (part == Part.LIGHT && clickedType.endsWith("CAKE")) {
      return false;
    }
    if (heldType == null || heldType.equals("AIR")) {
      return true;
    }
    // More of the same: candles of a color on candles of that color
    if (heldType.equals(clickedType)) {
      return false;
    }
    return !usesOnBlocks(heldType);
  }

  /** True if the item (a Material name) does something to the block it is used on */
  public static boolean usesOnBlocks(String heldType) {
    if (BLOCK_TOOLS.contains(heldType)) {
      return true;
    }
    for (String suffix : BLOCK_TOOL_SUFFIXES) {
      if (heldType.endsWith(suffix)) {
        return true;
      }
    }
    return false;
  }

  /**
   * The sign a click on the altar block or its light prays with: a praying altar sign, tagged ones
   * first (an old untagged one, recognized by its text, only if there is no tagged one), in the
   * order given. Curse and blessing signs are never used this way. Null if the altar has no praying
   * sign.
   */
  public static <T> T chooseSign(List<Candidate<T>> candidates) {
    T untagged = null;
    for (Candidate<T> candidate : candidates) {
      if (candidate.kind() != AltarSigns.Kind.PRAYING) {
        continue;
      }
      if (candidate.tagged()) {
        return candidate.sign();
      }
      if (untagged == null) {
        untagged = candidate.sign();
      }
    }
    return untagged;
  }
}
