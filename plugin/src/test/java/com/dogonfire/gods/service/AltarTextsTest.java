package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dogonfire.gods.message.Messages;
import java.nio.file.Path;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The floating text above altars, from Gods' own messages.yml */
class AltarTextsTest {
  @TempDir Path lang;

  private Messages messages() {
    Messages messages =
        new Messages(
            this.lang,
            path -> AltarTextsTest.class.getResourceAsStream("/" + path),
            Logger.getLogger("test"));
    messages.load("english", "Doggycraft");
    return messages;
  }

  @Test
  void showsTheGodsNameAndTitleInItsStyle() {
    String text =
        AltarTexts.render(
            messages(),
            "sun",
            false,
            Messages.god("Ra"),
            Messages.component("title", Component.text("God of the Sun")));
    assertEquals("§6☀ §f§lRa§f §6☀\n§eGod of the Sun", text);
  }

  @Test
  void theMainAltarHasALineOfItsOwn() {
    String text =
        AltarTexts.render(
            messages(),
            "sun",
            true,
            Messages.god("Ra"),
            Messages.component("title", Component.text("God of the Sun")),
            Messages.text("rank", "High Noon"));
    assertEquals(3, text.split("\n").length);
    assertEquals("§6Main altar · High Noon", text.split("\n")[2]);
  }

  @Test
  void anEmptyTitleTakesNoLine() {
    String text =
        AltarTexts.render(
            messages(),
            "default",
            false,
            Messages.god("Ra"),
            Messages.component("title", Component.empty()));
    assertEquals("§f§lRa", text);
  }

  @Test
  void aStyleWithoutItsOwnTextUsesTheDefault() {
    Messages messages = messages();
    assertEquals("altar.floating.default.altar", AltarTexts.key(messages, "unknown", false));
    assertEquals("altar.floating.default.main", AltarTexts.key(messages, "unknown", true));
    assertEquals("altar.floating.frost.main", AltarTexts.key(messages, "frost", true));
  }

  @Test
  void namesFromPlayersAreNotParsed() {
    String text =
        AltarTexts.render(
            messages(),
            "default",
            false,
            Messages.god("<red>Evil"),
            Messages.component("title", Component.empty()));
    assertEquals("§f§l<red>Evil", text);
  }
}
