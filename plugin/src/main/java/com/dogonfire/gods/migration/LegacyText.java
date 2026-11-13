package com.dogonfire.gods.migration;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Converts the text of the old language files ({@code &} and {@code §} color codes, {@code
 * $PlayerName}-style placeholders) to MiniMessage, so it looks the same as before.
 */
public final class LegacyText {
  private LegacyText() {}

  private static final Map<Character, String> COLORS =
      Map.ofEntries(
          Map.entry('0', "black"),
          Map.entry('1', "dark_blue"),
          Map.entry('2', "dark_green"),
          Map.entry('3', "dark_aqua"),
          Map.entry('4', "dark_red"),
          Map.entry('5', "dark_purple"),
          Map.entry('6', "gold"),
          Map.entry('7', "gray"),
          Map.entry('8', "dark_gray"),
          Map.entry('9', "blue"),
          Map.entry('a', "green"),
          Map.entry('b', "aqua"),
          Map.entry('c', "red"),
          Map.entry('d', "light_purple"),
          Map.entry('e', "yellow"),
          Map.entry('f', "white"));

  private static final Map<Character, String> DECORATIONS =
      Map.of(
          'k', "obfuscated", 'l', "bold", 'm', "strikethrough", 'n', "underlined", 'o', "italic");

  /**
   * The placeholders of a god's words, and of info messages unless a message says otherwise. The
   * old placeholders were shown in gold (and not bold), which {@code <hl>} does. {@code $type} was
   * never replaced (the old code only knew {@code $Type}); it is now.
   */
  /**
   * What a god's words were shown after ("&lt;God&gt;: §f§l"), and what the old placeholders
   * switched back to
   */
  public static final String VOICE_BASE = "§f§l";

  public static final Map<String, String> VOICE_PLACEHOLDERS =
      Map.of(
          "$PlayerName", "<hl><player></hl>",
          "$Amount", "<hl><amount></hl>",
          "$Type", "<hl><type></hl>",
          "$type", "<hl><type></hl>",
          "$ServerName", "<hl><server></hl>");

  /** Converts old text without placeholders or prefix to MiniMessage */
  public static String toMiniMessage(String legacy) {
    return toMiniMessage(legacy, "", false, Map.of());
  }

  /**
   * Converts old text to MiniMessage, so it looks the same as before.
   *
   * @param legacy the old text
   * @param base the color codes the old text was shown after, which the old placeholders also
   *     switched back to: "§f§l" for a god's words (after "&lt;God&gt;: "), "§b" for an aqua info
   *     message
   * @param emitBase whether the result starts with the base style (info messages), or something
   *     else adds it (a god's words, in the speech format)
   * @param placeholders old placeholder to its MiniMessage replacement (inserted as is)
   */
  public static String toMiniMessage(
      String legacy, String base, boolean emitBase, Map<String, String> placeholders) {
    // Text that looks like a tag must stay text
    String text = legacy.replace("\\", "\\\\").replace("<", "\\<");

    // Longest first, so $Player can't eat the start of $PlayerName
    List<Map.Entry<String, String>> ordered =
        placeholders.entrySet().stream()
            .sorted(
                Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length())
                    .reversed())
            .toList();

    Converter converter = new Converter();
    if (emitBase) {
      // Written before the first text, unless the text starts with a color of its own
      converter.pendingBase = base;
    } else {
      // The style is there, only not written here
      converter.decorated = hasDecoration(base);
    }
    converter.changed = false;

    for (int i = 0; i < text.length(); ) {
      Map.Entry<String, String> match = null;
      for (Map.Entry<String, String> placeholder : ordered) {
        if (text.startsWith(placeholder.getKey(), i)) {
          match = placeholder;
          break;
        }
      }
      if (match != null) {
        converter.flushBase();
        converter.out.append(match.getValue());
        // The old placeholders ended with the base codes, undoing the codes before them
        if (converter.changed) {
          converter.pendingBase = base;
          converter.changed = false;
        }
        i += match.getKey().length();
      } else {
        i = converter.next(text, i);
      }
    }
    return converter.out.toString();
  }

  private static boolean hasDecoration(String codes) {
    for (int i = 0; i + 1 < codes.length(); i++) {
      if ((codes.charAt(i) == '&' || codes.charAt(i) == '§')
          && DECORATIONS.containsKey(Character.toLowerCase(codes.charAt(i + 1)))) {
        return true;
      }
    }
    return false;
  }

  private static final class Converter {
    final StringBuilder out = new StringBuilder();
    // In legacy text a color code clears bold, italic etc. MiniMessage tags nest instead, so a
    // color that follows
    // a decoration needs a <reset> first to look the same.
    boolean decorated;
    // Whether a code was written since the start or the last placeholder
    boolean changed;
    // The base style to switch back to before the next text, unless a color code comes first (which
    // makes it moot)
    String pendingBase;

    void codes(String codes) {
      for (int i = 0; i < codes.length(); ) {
        i = next(codes, i);
      }
    }

    /** Converts the character (or color code) at i, returns the index after it */
    int next(String text, int i) {
      if (this.pendingBase != null) {
        String base = this.pendingBase;
        this.pendingBase = null;
        if (!startsColor(text, i)) {
          codes(base);
          this.changed = false;
        }
      }

      char c = text.charAt(i);
      if ((c != '&' && c != '§') || i + 1 >= text.length()) {
        this.out.append(c);
        return i + 1;
      }

      // &#rrggbb hex colors
      if (text.charAt(i + 1) == '#'
          && i + 8 <= text.length()
          && isHex(text.substring(i + 2, i + 8))) {
        color("#" + text.substring(i + 2, i + 8));
        return i + 8;
      }

      char code = Character.toLowerCase(text.charAt(i + 1));
      if (COLORS.containsKey(code)) {
        color(COLORS.get(code));
      } else if (DECORATIONS.containsKey(code)) {
        this.out.append('<').append(DECORATIONS.get(code)).append('>');
        this.decorated = true;
      } else if (code == 'r') {
        this.out.append("<reset>");
        this.decorated = false;
      } else {
        this.out.append(c);
        return i + 1;
      }
      this.changed = true;
      return i + 2;
    }

    /** Writes the base style that is waiting for the next text */
    void flushBase() {
      if (this.pendingBase != null) {
        String base = this.pendingBase;
        this.pendingBase = null;
        codes(base);
        this.changed = false;
      }
    }

    private static boolean startsColor(String text, int i) {
      if ((text.charAt(i) != '&' && text.charAt(i) != '§') || i + 1 >= text.length()) {
        return false;
      }
      char code = Character.toLowerCase(text.charAt(i + 1));
      return COLORS.containsKey(code)
          || code == 'r'
          || (code == '#' && i + 8 <= text.length() && isHex(text.substring(i + 2, i + 8)));
    }

    private void color(String color) {
      if (this.decorated) {
        this.out.append("<reset>");
        this.decorated = false;
      }
      this.out.append('<').append(color).append('>');
    }
  }

  private static boolean isHex(String s) {
    return s.toLowerCase(Locale.ROOT).chars().allMatch(ch -> Character.digit(ch, 16) >= 0);
  }
}
