package com.dogonfire.gods.migration;

import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.model.GodMood;
import com.dogonfire.gods.model.GodType;
import com.dogonfire.gods.util.AtomicFiles;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

/**
 * Upgrades the language files of Gods 0.9.x and of this version before MiniMessage (messages
 * version 1): {@code lang/<language>_general.yml} becomes {@code lang/<language>/messages.yml} and
 * each god voice {@code lang/<language>_<type>_<gender>.yml} becomes {@code
 * lang/<language>/voices/<type>_<gender>.yml}. The text is converted with {@link LegacyText}
 * ({@code &} codes and {@code $PlayerName}-style placeholders to MiniMessage), and the keys get
 * their new names. The new file is the bundled one with every message the old file says
 * differently, so an admin's changes are kept. A message that is one of Gods' own old texts ({@link
 * #stock}: every version of the english files Gods 0.9.x shipped or downloaded, and those of this
 * version before MiniMessage) is no change, so an unchanged old file gets Gods' new text. The old
 * files are moved to {@code lang/old/}.
 */
public final class LegacyLangFiles {
  /**
   * Where an old message of the general file goes: its new key, its color and what its placeholders
   * were
   */
  record InfoMessage(String oldKey, String newKey, String base, Map<String, String> placeholders) {}

  private static final String HL_GOD = "<hl><god></hl>";
  private static final String HL_AMOUNT = "<hl><amount></hl>";

  /** The old "Info" messages still in use. The color is the one the code sent them in. */
  static final List<InfoMessage> INFO =
      List.of(
          info(
              "HolylandSize",
              "holy-land.size",
              "&b",
              "$Amount",
              "<hl><claimed></hl>",
              "$PlayerName",
              "<hl><max></hl>"),
          info("CannotBuildAltarToOtherGods", "altar.other-god", "&4", "$PlayerName", HL_GOD),
          info("CannotClaimMoreHolyland", "holy-land.cannot-claim-more", "&c"),
          info("YourPrayerPower", "prayer.power", "&b", "$Amount", HL_AMOUNT),
          info("PrivateGodNoAccess", "altar.no-access", "&4"),
          info("YouLeftReligion", "prayer.left-religion", "&c", "$PlayerName", HL_GOD),
          info("GodsHelp", "help.commands-hint", "&b", "$PlayerName", "<white>/g help</white>"),
          info("AltarHelp", "help.altar-hint", "&b", "$PlayerName", "<white>/g help altar</white>"),
          info(
              "PrayForHelp", "help.prayfor-hint", "&b", "$PlayerName", "<white>/g prayfor</white>"),
          info(
              "InviteHelp",
              "priest.invite-hint",
              "&b",
              "$PlayerName",
              "<white>/gods invite \\<playername></white>"),
          info(
              "FollowersHelp",
              "priest.followers-hint",
              "&b",
              "$PlayerName",
              "<white>/gods followers</white>"),
          info(
              "DescriptionHelp",
              "priest.description-hint",
              "&b",
              "$PlayerName",
              "<white>/gods desc</white>"),
          info(
              "AttackHelp",
              "holy-land.attack-hint",
              "&b",
              "$PlayerName",
              "<white>/gods startattack</white>"),
          info("AttackingHolyLandsHelp", "holy-land.attacking-hint", "&b"),
          info(
              "ContestedLandKillsStatus",
              "holy-land.contested-kills",
              "&b",
              "$PlayerName",
              "<hl><minutes></hl>",
              "$Type",
              "<hl><attacker_kills></hl>",
              "$Amount",
              "<hl><defender_kills></hl>"),
          info(
              "ContestedLandDesecrationStatus",
              "holy-land.contested-desecration",
              "&b",
              "$PlayerName",
              HL_GOD,
              "$Amount",
              "<hl><desecrations></hl>"),
          info("PrayAlterHelp", "altar.pray-hint", "&b", "$PlayerName", HL_GOD),
          info("PrayToBlacklistedGodNotAllowed", "altar.god-not-allowed", "&4"),
          info(
              "TeleportIntoHolylandNotAllowed",
              "holy-land.no-teleport",
              "&4",
              "$PlayerName",
              HL_GOD),
          info("AltarPrayingNotAllowed", "altar.praying-not-allowed", "&4"),
          info(
              "YouEarnedPowerBySlayingHeathen",
              "believer.power-for-heathen-kill",
              "&b",
              "$PlayerName",
              HL_GOD,
              "$Amount",
              HL_AMOUNT),
          info(
              "YouEarnedPowerBySlayingEnemy",
              "believer.power-for-enemy-kill",
              "&b",
              "$PlayerName",
              HL_GOD,
              "$Amount",
              HL_AMOUNT),
          info(
              "ConfirmChangeToOtherReligion", "prayer.confirm-change", "&e", "$PlayerName", HL_GOD),
          info("CannotChangeGodSoSoon", "prayer.change-too-soon", "&c"),
          info("CannotPraySoSoon", "prayer.too-soon", "&c", "$Amount", "<hl><minutes></hl>"),
          info(
              "InfoHelp",
              "help.info-hint",
              "&b",
              "$PlayerName",
              "<white>/g info \\<godname></white>"),
          info("YouHealedBeings", "power.healed", "&b", "$Amount", HL_AMOUNT),
          info(
              "DivorcedYou",
              "marriage.divorced-you",
              "&c",
              "$PlayerName",
              "<hl><player></hl>",
              "$Type",
              "<hl>DIVORCED</hl>"),
          info(
              "MarrigeLovesYou",
              "marriage.loves-you",
              "&a",
              "$PlayerName",
              "<hl><player></hl>",
              "$Type",
              "<dark_red>LOVES</dark_red>"),
          info("RejectedJoinOffer", "prayer.rejected-invitation", "&c", "$PlayerName", HL_GOD),
          info("InvalidGodName", "altar.invalid-god-name", "&4"),
          info("InvalidAltarSign", "altar.invalid-sign", "&4"),
          info("BlessingsHelp", "altar.blessing-hint", "&b"),
          info("CursesHelp", "altar.curse-hint", "&b"),
          info("CursesNotAllowed", "altar.curse-not-allowed", "&4"),
          info("BlessingsNotAllowed", "altar.blessing-not-allowed", "&4"),
          info("BuildAltarNotAllowed", "altar.build-not-allowed", "&4"),
          info("CreateGodNotAllowed", "altar.create-god-not-allowed", "&c"),
          info("SacrificeHelp", "quest.sacrifice-hint", "&b"),
          info("QuestTargetRange", "quest.target-range", "&b", "$Amount", "<hl><distance></hl>"),
          info("QuestTargetHelp", "quest.target-hint", "&b"),
          info(
              "GodToBelieverQuestionHelp",
              "god.question-hint",
              "&b",
              "$PlayerName",
              "<white>/gods yes or /gods no</white>"),
          info(
              "NotEnoughPrayerPower",
              "prayer.not-enough-power",
              "&b",
              "$PlayerName",
              HL_GOD,
              "$Amount",
              HL_AMOUNT),
          info(
              "PrayedForBlessing",
              "prayer.prayed-for-blessing",
              "&b",
              "$PlayerName",
              HL_GOD,
              "$Amount",
              HL_AMOUNT),
          info(
              "PrayedForItem",
              "prayer.prayed-for-item",
              "&b",
              "$PlayerName",
              HL_GOD,
              "$Amount",
              HL_AMOUNT),
          info(
              "PrayedForHealth",
              "prayer.prayed-for-health",
              "&b",
              "$PlayerName",
              HL_GOD,
              "$Amount",
              HL_AMOUNT),
          info(
              "PrayedForQuest",
              "prayer.prayed-for-quest",
              "&b",
              "$PlayerName",
              HL_GOD,
              "$Amount",
              HL_AMOUNT));

  /**
   * Whether a name in a section of the old general file is still asked for (old files name things
   * that are gone)
   */
  private static boolean isKnownName(String section, String name) {
    return switch (section) {
      case "Items" -> Material.getMaterial(name) != null;
      case "Mobs" -> Arrays.stream(EntityType.values()).anyMatch(type -> type.name().equals(name));
      case "GodTypes" -> Arrays.stream(GodType.values()).anyMatch(type -> type.name().equals(name));
      case "GodGender" ->
          Arrays.stream(GodGender.values()).anyMatch(gender -> gender.name().equals(name));
      case "GodMood" -> Arrays.stream(GodMood.values()).anyMatch(mood -> mood.name().equals(name));
      default -> false;
    };
  }

  /**
   * Old keys of the general file that this version renamed before MiniMessage, to the name {@link
   * #INFO} knows them by, so an admin's text under the 0.9.x name carries over
   */
  static final Map<String, String> INFO_RENAMES =
      Map.of("AltarPrayingNowAllowed", "AltarPrayingNotAllowed");

  /**
   * Old keys of the voice files that this version renamed before MiniMessage (the 0.9.x names), to
   * the names {@link #voiceKey} makes the new keys from
   */
  static final Map<String, String> VOICE_RENAMES =
      Map.of(
          "GodToBelieversGiveBiblesQuestStarted", "GodToBelieversGiveItemsQuestStarted",
          "GodToBelieversGiveBiblesQuestStatus", "GodToBelieversGiveItemsQuestStatus",
          "GodToBelieversGiveBiblesQuestCompleted", "GodToBelieversGiveItemsQuestCompleted",
          "GodToBelieversGiveBiblesQuestFailed", "GodToBelieversGiveItemsQuestFailed",
          "GodToBelieversGetHolyArtifactThisQuestStarted",
              "GodToBelieversGetHolyArtifactQuestStarted",
          "GodToBelieversGetHolyArtifactThisQuestStatus",
              "GodToBelieversGetHolyArtifactQuestStatus",
          "GodToBelieversGetHolyArtifactThisQuestFailed",
              "GodToBelieversGetHolyArtifactQuestFailed");

  /** The old sections of names in the general file, and where they go */
  static final Map<String, String> NAMES =
      Map.of(
          "Items", "names.item",
          "Mobs", "names.mob",
          "GodTypes", "names.god-type",
          "GodGender", "names.gender",
          "GodMood", "names.mood");

  static final int LINE_WIDTH = 4096;

  private static final Pattern GENERAL = Pattern.compile("(.+)_general\\.yml");
  private static final Pattern VOICE =
      Pattern.compile(
          "(.+)_(frost|love|evil|sea|moon|sun|thunder|party|war|werewolves|creatures|wisdom|nature)_(male|female)\\.yml");

  private LegacyLangFiles() {}

  private static InfoMessage info(
      String oldKey, String newKey, String base, String... placeholders) {
    Map<String, String> map = new LinkedHashMap<>(LegacyText.VOICE_PLACEHOLDERS);
    for (int i = 0; i < placeholders.length; i += 2) {
      map.put(placeholders[i], placeholders[i + 1]);
    }
    return new InfoMessage(oldKey, newKey, base, Map.copyOf(map));
  }

  /**
   * The key of a god's voice line: the old name without "GodTo", in kebab case.
   * "GodToBelieversSlayQuestStarted" becomes "believers-slay-quest-started", "DefaultBibleText1"
   * "default-bible-text-1".
   */
  public static String voiceKey(String oldKey) {
    String camel = oldKey.startsWith("GodTo") ? oldKey.substring(5) : oldKey;
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < camel.length(); i++) {
      char c = camel.charAt(i);
      if (i > 0) {
        char previous = camel.charAt(i - 1);
        boolean lowerNext = i + 1 < camel.length() && Character.isLowerCase(camel.charAt(i + 1));
        if ((Character.isUpperCase(c)
                && (Character.isLowerCase(previous) || Character.isDigit(previous)))
            || (Character.isUpperCase(c) && Character.isUpperCase(previous) && lowerNext)
            || (Character.isDigit(c) && Character.isLetter(previous))) {
          out.append('-');
        }
      }
      out.append(Character.toLowerCase(c));
    }
    return out.toString();
  }

  /**
   * {@link #convertGeneral(ConfigurationSection, ConfigurationSection)} keeping every message,
   * stock or not
   */
  public static Map<String, String> convertGeneral(ConfigurationSection old) {
    return convertGeneral(old, null);
  }

  /**
   * The messages of an old general file under their new keys, converted to MiniMessage. Messages no
   * longer used are left out, and so are those that are one of Gods' own old texts ({@code stock},
   * see {@link #stock}): no admin changed them, and the new file has Gods' new text for them.
   *
   * @param stock every text Gods' own old file had per key, or null to keep every message
   */
  public static Map<String, String> convertGeneral(
      ConfigurationSection old, ConfigurationSection stock) {
    Map<String, String> converted = new LinkedHashMap<>();
    for (InfoMessage message : INFO) {
      String key = "Info." + message.oldKey();
      String text = old.getString(key);
      for (Map.Entry<String, String> rename : INFO_RENAMES.entrySet()) {
        if (text == null && rename.getValue().equals(message.oldKey())) {
          key = "Info." + rename.getKey();
          text = old.getString(key);
        }
      }
      if (text != null && !isStock(stock, key, text)) {
        converted.put(
            message.newKey(),
            LegacyText.toMiniMessage(text, message.base(), true, message.placeholders()));
      }
    }

    for (Map.Entry<String, String> section : NAMES.entrySet()) {
      ConfigurationSection names = old.getConfigurationSection(section.getKey());
      if (names == null) {
        continue;
      }
      for (String name : names.getKeys(false)) {
        String text = names.getString(name);
        // A name that is the key itself is what Gods shows without one
        if (text != null
            && !text.equals(name)
            && isKnownName(section.getKey(), name)
            && !isStock(stock, section.getKey() + "." + name, text)) {
          converted.put(
              section.getValue() + "." + name,
              LegacyText.toMiniMessage(text, "", false, Map.of("$Gender", "<gender>")));
        }
      }
    }
    return converted;
  }

  /**
   * {@link #convertVoice(ConfigurationSection, ConfigurationSection)} keeping every line, stock or
   * not
   */
  public static Map<String, List<String>> convertVoice(ConfigurationSection old) {
    return convertVoice(old, null);
  }

  /**
   * The lines of an old voice file under their new keys, converted to MiniMessage. A key whose
   * lines are all among Gods' own old lines for it ({@code stock}) is left out: the new file has
   * Gods' new lines for it. Keys this version renamed ({@link #VOICE_RENAMES}) go under the new
   * name.
   *
   * @param stock every line Gods' own old file had per key, or null to keep every key
   */
  public static Map<String, List<String>> convertVoice(
      ConfigurationSection old, ConfigurationSection stock) {
    Map<String, List<String>> converted = new LinkedHashMap<>();
    for (String key : old.getKeys(false)) {
      if (key.equals("Version") || !old.isList(key)) {
        continue;
      }
      String renamed = VOICE_RENAMES.get(key);
      if (renamed != null && old.isList(renamed)) {
        // The file has the new name too
        continue;
      }
      List<String> oldLines = old.getStringList(key);
      if (!oldLines.isEmpty() && oldLines.stream().allMatch(line -> isStock(stock, key, line))) {
        continue;
      }
      List<String> lines = new ArrayList<>();
      for (String line : oldLines) {
        lines.add(
            LegacyText.toMiniMessage(
                line, LegacyText.VOICE_BASE, false, LegacyText.VOICE_PLACEHOLDERS));
      }
      converted.put(voiceKey(renamed != null ? renamed : key), List.copyOf(lines));
    }
    return converted;
  }

  /**
   * Converts every old language file in {@code plugins/Gods/lang} to the new layout and moves it to
   * {@code lang/old/}. A new file that exists already is not overwritten: the old file is then only
   * moved. A file that can't be read stays where it is.
   *
   * @param resources opens a file in the plugin jar (null if it isn't there), like {@code
   *     Plugin#getResource}
   */
  public static void migrate(Path langFolder, Function<String, InputStream> resources, Logger log) {
    if (!Files.isDirectory(langFolder)) {
      return;
    }

    List<Path> oldFiles = new ArrayList<>();
    try (DirectoryStream<Path> files = Files.newDirectoryStream(langFolder, "*.yml")) {
      for (Path file : files) {
        String name = file.getFileName().toString();
        if (GENERAL.matcher(name).matches() || VOICE.matcher(name).matches()) {
          oldFiles.add(file);
        }
      }
    } catch (IOException e) {
      log.warning("Could not look for old language files in " + langFolder + ": " + e.getMessage());
      return;
    }
    if (oldFiles.isEmpty()) {
      return;
    }

    int converted = 0;
    int changedMessages = 0;
    for (Path file : oldFiles.stream().sorted().toList()) {
      String name = file.getFileName().toString();
      Matcher general = GENERAL.matcher(name);
      Matcher voice = VOICE.matcher(name);
      String language;
      String newPath;
      if (general.matches()) {
        language = general.group(1);
        newPath = "messages.yml";
      } else {
        voice.matches();
        language = voice.group(1);
        newPath = "voices/" + voice.group(2) + "_" + voice.group(3) + ".yml";
      }
      Path target = langFolder.resolve(language).resolve(newPath);

      YamlConfiguration old = new YamlConfiguration();
      try {
        old.load(file.toFile());
      } catch (IOException | InvalidConfigurationException e) {
        log.warning(
            "Could not read the old language file "
                + name
                + ", so it was not converted: "
                + e.getMessage());
        continue;
      }

      try {
        if (Files.exists(target)) {
          log.info(
              "lang/"
                  + language
                  + "/"
                  + newPath
                  + " exists already, so the old "
                  + name
                  + " was not converted.");
        } else {
          YamlConfiguration stock = stock(resources, name);
          changedMessages +=
              write(
                  target,
                  general.matches() ? convertGeneral(old, stock) : convertVoice(old, stock),
                  bundled(resources, language, newPath));
          converted++;
        }
        moveToOld(file, langFolder.resolve("old"));
      } catch (IOException e) {
        log.warning("Could not convert the old language file " + name + ": " + e.getMessage());
      }
    }

    log.info(
        "Converted "
            + converted
            + " old language files to the new message files in lang/<language>/ (MiniMessage), keeping "
            + changedMessages
            + " messages that differ from Gods' own. The old files were moved to lang/old/.");
  }

  /**
   * Writes the new file: the bundled one (if the jar has it for that language) with every converted
   * message that differs from it. Returns how many messages differed.
   */
  static int write(Path target, Map<String, ?> converted, String bundled) throws IOException {
    YamlConfiguration yaml = new YamlConfiguration();
    // Long lines stay on one line
    yaml.options().width(LINE_WIDTH);
    if (bundled != null) {
      try {
        yaml.loadFromString(bundled);
      } catch (InvalidConfigurationException e) {
        throw new IOException("The bundled file is broken", e);
      }
    } else {
      yaml.set(Messages.VERSION_KEY, Messages.MESSAGES_VERSION);
    }

    int changed = 0;
    for (Map.Entry<String, ?> entry : converted.entrySet()) {
      Object current =
          yaml.isList(entry.getKey())
              ? yaml.getStringList(entry.getKey())
              : yaml.getString(entry.getKey());
      if (!Objects.equals(current, entry.getValue())) {
        yaml.set(entry.getKey(), entry.getValue());
        changed++;
      }
    }

    Files.createDirectories(target.getParent());
    AtomicFiles.write(target, yaml.saveToString());
    return bundled != null ? changed : 0;
  }

  /**
   * Every text Gods' own old language file of this name had, per key: {@code legacy-lang/<name>} in
   * the jar, made from every version of Gods' english files in its git history (0.9.x, which
   * downloaded them from GitHub, and this version before MiniMessage). Null for a file Gods never
   * had (another language).
   */
  static YamlConfiguration stock(Function<String, InputStream> resources, String oldFileName)
      throws IOException {
    try (InputStream in = resources.apply("legacy-lang/" + oldFileName)) {
      if (in == null) {
        return null;
      }
      YamlConfiguration stock = new YamlConfiguration();
      stock.loadFromString(new String(in.readAllBytes(), StandardCharsets.UTF_8));
      return stock;
    } catch (InvalidConfigurationException e) {
      throw new IOException("The bundled legacy-lang/" + oldFileName + " is broken", e);
    }
  }

  /** Whether the text is one Gods' own old file had for this key */
  private static boolean isStock(ConfigurationSection stock, String key, String text) {
    return stock != null && stock.getStringList(key).contains(text);
  }

  private static String bundled(
      Function<String, InputStream> resources, String language, String path) throws IOException {
    try (InputStream in = resources.apply("lang/" + language + "/" + path)) {
      return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static void moveToOld(Path file, Path oldFolder) throws IOException {
    Files.createDirectories(oldFolder);
    Path target = oldFolder.resolve(file.getFileName());
    if (Files.exists(target)) {
      target = oldFolder.resolve(file.getFileName() + "." + System.currentTimeMillis());
    }
    Files.move(file, target);
  }
}
