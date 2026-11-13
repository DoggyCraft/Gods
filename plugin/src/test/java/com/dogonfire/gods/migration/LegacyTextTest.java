package com.dogonfire.gods.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.Test;

class LegacyTextTest {
  private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
  private static final TagResolver HIGHLIGHT =
      TagResolver.resolver(
          "hl", Tag.styling(NamedTextColor.GOLD, TextDecoration.BOLD.withState(false)));

  @Test
  void convertsColorsAndPlaceholders() {
    assertEquals(
        "<red>You left the religion of <hl><player></hl>",
        LegacyText.toMiniMessage(
            "You left the religion of $PlayerName", "&c", true, LegacyText.VOICE_PLACEHOLDERS));
  }

  @Test
  void colorAfterDecorationResetsIt() {
    // In legacy text "&l...&c" turns bold off; MiniMessage needs an explicit <reset> for that
    assertEquals("<bold>a<reset><red>b", LegacyText.toMiniMessage("&la&cb"));
    // A god's words follow a bold prefix, so their first color ends the bold too
    assertEquals(
        "a<reset><gold>b",
        LegacyText.toMiniMessage("a&6b", LegacyText.VOICE_BASE, false, Map.of()));
  }

  @Test
  void convertsHexColorsAndSectionSigns() {
    assertEquals("<#12ab34>x<green>y", LegacyText.toMiniMessage("&#12ab34x§ay"));
  }

  @Test
  void escapesTextThatLooksLikeTags() {
    assertEquals("Use /g info \\<godname>", LegacyText.toMiniMessage("Use /g info <godname>"));
  }

  @Test
  void leavesUnknownCodesAndOtherDollarsAlone() {
    assertEquals(
        "Fish & chips for $5",
        LegacyText.toMiniMessage("Fish & chips for $5", "", false, LegacyText.VOICE_PLACEHOLDERS));
  }

  @Test
  void replacesTheLowerCaseTypeThatWasNeverReplacedBefore() {
    assertEquals(
        "Claim the land of <hl><type></hl>!",
        LegacyText.toMiniMessage(
            "Claim the land of $type!", "", false, LegacyText.VOICE_PLACEHOLDERS));
  }

  /**
   * A god's words must look exactly as they did: "§6<God>: §f§l" + the line, placeholders as
   * "§6value§f§l"
   */
  @Test
  void rendersLikeTheOldGodSpeech() {
    String line =
        "Well done! You have uphold the justice in &6$ServerName&f. Here is your reward, $PlayerName!";
    String old =
        "§6<Thor>: §f§l"
            + line.replace("&", "§")
                .replace("$ServerName", "§6Doggycraft§f§l")
                .replace("$PlayerName", "§6Bob§f§l");

    String converted =
        LegacyText.toMiniMessage(line, LegacyText.VOICE_BASE, false, LegacyText.VOICE_PLACEHOLDERS);
    String format = "<gold>\\<<god>>: <white><bold><message>";
    String rendered =
        LEGACY.serialize(
            MiniMessage.miniMessage()
                .deserialize(
                    format,
                    TagResolver.resolver(
                        HIGHLIGHT,
                        Placeholder.unparsed("god", "Thor"),
                        Placeholder.parsed("message", converted),
                        Placeholder.unparsed("server", "Doggycraft"),
                        Placeholder.unparsed("player", "Bob"))));

    assertEquals(normalized(old), normalized(rendered));
  }

  /** Info messages: the color + the text, placeholders as "§6value" + the color */
  @Test
  void rendersLikeTheOldInfoMessage() {
    String old = "§bYou prayed to §6Thor§b for a blessing! §cReally!";
    String converted =
        LegacyText.toMiniMessage(
            "You prayed to $PlayerName for a blessing! &cReally!",
            "&b",
            true,
            Map.of("$PlayerName", "<hl><god></hl>"));
    String rendered =
        LEGACY.serialize(
            MiniMessage.miniMessage()
                .deserialize(
                    converted,
                    TagResolver.resolver(HIGHLIGHT, Placeholder.unparsed("god", "Thor"))));

    assertEquals(normalized(old), normalized(rendered));
  }

  /** After a color code, an old placeholder switched back to the base style */
  @Test
  void placeholderAfterAColorSwitchesBackToTheBase() {
    assertEquals(
        "<aqua>a <red>b <hl><god></hl><aqua> c",
        LegacyText.toMiniMessage(
            "a &cb $PlayerName c", "&b", true, Map.of("$PlayerName", "<hl><god></hl>")));
  }

  /** Legacy text can say the same thing in different ways; this is the shortest */
  private static String normalized(String legacy) {
    return LEGACY.serialize(LEGACY.deserialize(legacy));
  }
}
