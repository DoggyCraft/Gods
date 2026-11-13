package com.dogonfire.gods.message;

import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.model.GodType;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/**
 * The gods' voices: what a god says in each situation, from {@code
 * lang/<language>/voices/<type>_<gender>.yml} (e.g. {@code voices/sun_female.yml}), one file per
 * god type and gender. Each key is a list of lines in MiniMessage, of which the god says one at
 * random; {@code ''} or an empty list makes the god say nothing. Keys missing from the server's
 * file fall back to the bundled file of the same name (never to another type's voice). Non-binary
 * gods have no files of their own: each of their lines comes from their type's male or female file,
 * picked anew every time.
 *
 * <p>Thread-safe: a load replaces the voices as a whole.
 */
public final class GodVoices {
  private final LangFiles files;
  private final Logger log;
  private final Random random = new Random();

  private volatile Map<String, Map<String, List<String>>> voices = Map.of();

  GodVoices(LangFiles files, Logger log) {
    this.files = files;
    this.log = log;
  }

  void load(String language) {
    Map<String, Map<String, List<String>>> loaded = new HashMap<>();
    for (GodType type : GodType.values()) {
      for (GodGender gender : List.of(GodGender.Male, GodGender.Female)) {
        String name = name(type, gender);
        loaded.put(name, Map.copyOf(this.files.read(language, "voices/" + name + ".yml")));
      }
    }
    this.voices = Map.copyOf(loaded);
  }

  /**
   * The name of a voice ("sun_female"): the god's type and gender. Only Male and Female have voice
   * files: a non-binary god speaks with both, a god without a gender with the one its seed picks
   * ({@link com.dogonfire.gods.service.GodRules#voices}).
   *
   * @throws IllegalArgumentException for any other gender
   */
  public static String name(GodType type, GodGender gender) {
    if (gender != GodGender.Male && gender != GodGender.Female) {
      throw new IllegalArgumentException("There is no voice file for " + gender + " gods");
    }
    return type.name().toLowerCase(Locale.ROOT) + "_" + gender.name().toLowerCase(Locale.ROOT);
  }

  /** Every line the voice has for the situation (MiniMessage); empty if it has none */
  public List<String> lines(String voice, Voice situation) {
    Map<String, List<String>> lines = this.voices.get(voice);
    if (lines == null) {
      this.log.warning("There is no voice " + voice);
      return List.of();
    }
    List<String> said = lines.get(situation.key());
    if (said == null) {
      this.log.warning("The voice " + voice + " has no lines for " + situation.key());
      return List.of();
    }
    return said;
  }

  /**
   * One of the voice's lines for the situation, picked at random (MiniMessage); null if it has none
   */
  public String randomLine(String voice, Voice situation) {
    return randomLine(List.of(voice), situation, this.random);
  }

  /**
   * One of a god's lines for the situation (MiniMessage), or null: one of its voices picked at
   * random (a non-binary god has two, see {@link com.dogonfire.gods.service.GodRules#voices}), then
   * one of that voice's lines, so each utterance comes from one voice file
   */
  public String randomLine(List<String> voices, Voice situation) {
    return randomLine(voices, situation, this.random);
  }

  /** {@link #randomLine(List, Voice)} drawing from the given random (for tests) */
  public String randomLine(List<String> voices, Voice situation, Random random) {
    if (voices.isEmpty()) {
      return null;
    }
    String voice =
        voices.size() == 1 ? voices.getFirst() : voices.get(random.nextInt(voices.size()));
    List<String> lines = lines(voice, situation);
    if (lines.isEmpty()) {
      return null;
    }
    return lines.get(random.nextInt(lines.size()));
  }
}
