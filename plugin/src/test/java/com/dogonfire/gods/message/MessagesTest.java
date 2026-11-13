package com.dogonfire.gods.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.model.GodType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MessagesTest {
  @TempDir Path lang;

  private final Map<String, String> jar = new HashMap<>();

  private Messages messages(String language) {
    Messages messages =
        new Messages(
            this.lang,
            path -> {
              String text = this.jar.get(path);
              return text == null
                  ? null
                  : (InputStream) new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
            },
            Logger.getLogger("test"));
    messages.load(language, "Doggycraft");
    return messages;
  }

  @Test
  void rendersMiniMessageToLegacyText() {
    this.jar.put(
        "lang/english/messages.yml", "a: '<aqua>Hello <hl><player></hl>, welcome to <server>'\n");
    assertEquals(
        "§bHello §6Bob§b, welcome to Doggycraft",
        messages("english").legacy("a", Messages.player("Bob")));
  }

  @Test
  void valuesFromPlayersAreNotParsed() {
    this.jar.put("lang/english/messages.yml", "a: '<aqua><god> is great'\n");
    assertEquals(
        "§b<red>Evil</red> is great",
        messages("english").legacy("a", Messages.god("<red>Evil</red>")));
  }

  @Test
  void displayNamesKeepTheirColors() {
    this.jar.put("lang/english/messages.yml", "a: '<aqua><player> loves you'\n");
    assertEquals(
        "§cBob§b loves you",
        messages("english").legacy("a", Messages.legacyText("player", "§cBob")));
  }

  @Test
  void listsAreLinesAndEmptyIsNothing() {
    this.jar.put("lang/english/messages.yml", "list:\n- one\n- two\nnothing: ''\n");
    Messages messages = messages("english");
    assertEquals(2, messages.lines("list").size());
    assertTrue(messages.lines("nothing").isEmpty());
    assertEquals(List.of("§c[Gods: missing message gone]"), messages.legacyLines("gone"));
  }

  @Test
  void serverFileWinsOverBundledWhichWinsOverEnglish() throws IOException {
    this.jar.put("lang/english/messages.yml", "a: english a\nb: english b\nc: english c\n");
    this.jar.put("lang/german/messages.yml", "messages-version: 2\na: german a\nb: german b\n");
    Files.createDirectories(this.lang.resolve("german"));
    Files.writeString(
        this.lang.resolve("german/messages.yml"), "messages-version: 2\na: server a\n");

    Messages messages = messages("german");
    assertEquals("server a", messages.legacy("a"));
    assertEquals("german b", messages.legacy("b"));
    assertEquals("english c", messages.legacy("c"));
  }

  @Test
  void bundledFileIsCopiedToTheServer() {
    this.jar.put("lang/english/messages.yml", "a: x\n");
    this.jar.put("lang/english/voices/sun_male.yml", "believer-praying:\n- Hi\n");
    messages("english");
    assertTrue(Files.exists(this.lang.resolve("english/messages.yml")));
    assertTrue(Files.exists(this.lang.resolve("english/voices/sun_male.yml")));
    // A language the jar doesn't have is not written
    messages("klingon");
    assertFalse(Files.exists(this.lang.resolve("klingon")));
  }

  @Test
  void namesFallBackToOnesMadeFromTheId() {
    this.jar.put(
        "lang/english/messages.yml",
        "names:\n  item:\n    CARROT: carrot\n  gender:\n    Female: Goddess\n  god-type:\n    SUN: <gender> of the Sun\n");
    Messages messages = messages("english");
    assertEquals("carrot", Messages.legacy(messages.itemName(Material.CARROT)));
    assertEquals("Potato", Messages.legacy(messages.itemName(Material.POTATO)));
    assertEquals("Cooked Beef", Messages.legacy(messages.itemName(Material.COOKED_BEEF)));
    assertEquals(
        "Polar Bear", Messages.legacy(messages.mobName(org.bukkit.entity.EntityType.POLAR_BEAR)));
    assertEquals(
        "Goddess of the Sun", Messages.legacy(messages.godTypeName(GodType.SUN, GodGender.Female)));
  }

  @Test
  void readableNames() {
    assertEquals("Cooked Beef", Messages.readableName("COOKED_BEEF"));
    assertEquals("Tnt", Messages.readableName("TNT"));
    assertEquals("Waxed Oxidized Cut Copper", Messages.readableName("WAXED_OXIDIZED_CUT_COPPER"));
    assertEquals("", Messages.readableName(""));
  }

  @Test
  void voicesPickOneOfTheirLines() {
    this.jar.put(
        "lang/english/voices/sun_male.yml",
        "believer-praying:\n- one\n- two\nbelievers-alliance: ''\n");
    Messages messages = messages("english");
    String line = messages.voices().randomLine("sun_male", Voice.BELIEVER_PRAYING);
    assertTrue(line.equals("one") || line.equals("two"));
    assertNull(messages.voices().randomLine("sun_male", Voice.BELIEVERS_ALLIANCE));
    assertNull(messages.voices().randomLine("moon_female", Voice.BELIEVER_PRAYING));
  }

  /**
   * A non-binary god's line comes from one of its two voice files, picked for each line, then a
   * line of that file
   */
  @Test
  void nonBinaryGodsSpeakWithBothVoices() {
    this.jar.put(
        "lang/english/voices/sun_male.yml",
        "believer-praying:\n- m1\n- m2\nbelievers-alliance: ''\n");
    this.jar.put(
        "lang/english/voices/sun_female.yml",
        "believer-praying:\n- f1\n- f2\n- f3\nbelievers-alliance: [ally]\n");
    GodVoices voices = messages("english").voices();
    List<String> both = List.of("sun_male", "sun_female");

    // Draws: which file (of 2), then which line of it
    assertEquals("m2", voices.randomLine(both, Voice.BELIEVER_PRAYING, new Draws(0, 1)));
    assertEquals("f3", voices.randomLine(both, Voice.BELIEVER_PRAYING, new Draws(1, 2)));
    assertEquals("f1", voices.randomLine(both, Voice.BELIEVER_PRAYING, new Draws(1, 0)));
    // The picked file says nothing there: the god says nothing
    assertNull(voices.randomLine(both, Voice.BELIEVERS_ALLIANCE, new Draws(0, 0)));
    assertEquals("ally", voices.randomLine(both, Voice.BELIEVERS_ALLIANCE, new Draws(1, 0)));
    // One voice: no draw for the file
    assertEquals(
        "m2", voices.randomLine(List.of("sun_male"), Voice.BELIEVER_PRAYING, new Draws(1)));
    // Bundled: a non-binary sun god says lines of both files
    Messages bundled =
        new Messages(
            this.lang,
            path -> getClass().getClassLoader().getResourceAsStream(path),
            Logger.getLogger("test"));
    bundled.load("english", "Doggycraft");
    assertEquals(
        bundled.voices().lines("sun_male", Voice.BELIEVER_PRAYING).getFirst(),
        bundled.voices().randomLine(both, Voice.BELIEVER_PRAYING, new Draws(0, 0)));
    assertEquals(
        bundled.voices().lines("sun_female", Voice.BELIEVER_PRAYING).getFirst(),
        bundled.voices().randomLine(both, Voice.BELIEVER_PRAYING, new Draws(1, 0)));
    assertEquals(
        "Deity of the Sea", Messages.legacy(bundled.godTypeName(GodType.SEA, GodGender.NonBinary)));
  }

  /** A Random that returns the given numbers from nextInt, in order */
  private static final class Draws extends java.util.Random {
    private final int[] draws;
    private int next;

    Draws(int... draws) {
      this.draws = draws;
    }

    @Override
    public int nextInt(int bound) {
      int draw = this.draws[this.next++];
      assertTrue(draw < bound);
      return draw;
    }
  }

  /**
   * A god's words with the bundled god.speech look as before, and a god or player name can't inject
   * tags
   */
  @Test
  void godSpeechLooksAsBeforeAndNamesAreNotParsed() {
    Messages messages =
        new Messages(
            this.lang,
            path -> getClass().getClassLoader().getResourceAsStream(path),
            Logger.getLogger("test"));
    messages.load("english", "Doggycraft");
    String line = messages.voices().lines("sun_male", Voice.BELIEVER_MARRIED_COUPLE).getFirst();
    String said =
        Messages.legacy(
            messages.component(
                "god.speech",
                Messages.god("<red>Thor"),
                Messages.player("<b>Bob"),
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed(
                    "message", line)));
    // §r§6 looks the same as the old §f§l§6
    assertEquals(
        "§6<<red>Thor>: §r§6<b>Bob§f§l, your love is making §6Doggycraft§f§l a better place!",
        said);
  }

  @Test
  void plainTextHasNoHighlight() {
    this.jar.put("lang/english/messages.yml", "a: x\n");
    assertEquals(
        "Thor made <server>? No: Doggycraft",
        Messages.legacy(
            messages("english")
                .parsePlain("<hl><god></hl> made \\<server>? No: <server>", Messages.god("Thor"))));
  }
}
