package com.dogonfire.gods.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.message.Voice;

class LegacyLangFilesTest
{
	private static final Logger	LOG	= Logger.getLogger("test");

	@TempDir
	Path						lang;

	@Test
	void voiceKeysAreTheOldNamesInKebabCase()
	{
		assertEquals("believers-slay-quest-started", LegacyLangFiles.voiceKey("GodToBelieversSlayQuestStarted"));
		assertEquals("priest-eat-food-type", LegacyLangFiles.voiceKey("GodToPriestEatFoodType"));
		assertEquals("default-bible-text-1", LegacyLangFiles.voiceKey("DefaultBibleText1"));
		assertEquals("believers-pvp-revenge-quest-started", LegacyLangFiles.voiceKey("GodToBelieversPVPRevengeQuestStarted"));
		assertEquals("prophecy-header-bible-text", LegacyLangFiles.voiceKey("ProphecyHeaderBibleText"));
	}

	/**
	 * Voices whose text Gods rewrote after the old bundled files, because the old one no longer fits (the migration
	 * takes Gods' own old texts as unchanged, see {@link #unchangedOldFilesOfThisVersionKeepNothing})
	 */
	private static final java.util.Set<String> REWRITTEN_VOICES = java.util.Set.of(
			// "just specified his location as our home": the home is the main altar now
			Voice.BELIEVERS_SET_HOME.key(),
			// "the only true god": the god names its own gender (<gender>), so a goddess or a deity says it right
			Voice.BELIEVER_RANDOM_NEUTRAL_SPEECH.key(),
			// The holy book called the god "he" ("his creation", "the wrong God"), whatever its gender
			Voice.DEFAULT_BIBLE_TEXT_1.key(), Voice.DEFAULT_BIBLE_TEXT_2.key(), Voice.DEFAULT_BIBLE_TEXT_3.key(), Voice.DEFAULT_BIBLE_TEXT_4.key(),
			Voice.DEFAULT_BIBLE_TEXT_5.key(), Voice.DEFAULT_BIBLE_TEXT_6.key(),
			// "he will guide you", "has lost his faith": Gods never knows a player's gender, so players are "they"
			Voice.BELIEVERS_LOST_BELIEVER.key());

	/** Messages whose text Gods rewrote after the old bundled files, like {@link #REWRITTEN_VOICES} */
	private static final java.util.Set<String> REWRITTEN_MESSAGES = java.util.Set.of(
			// Right clicking the altar block or its light prays too
			"altar.pray-hint",
			// Each follower shows their devotion level
			"command.followers.line", "command.followers.you", "command.followers.you-below",
			// 0.9.x named the oak log by its old material name, LOG
			"names.item.OAK_LOG",
			// "Holy Land", as the other messages say, not "Holyland"
			"holy-land.size", "holy-land.cannot-claim-more");

	/** The bundled files were made from the old bundled ones, so converting those changes nothing */
	@Test
	void theOldBundledFilesConvertToTheNewBundledOnes() throws IOException
	{
		assertEquals(0, LegacyLangFiles.write(this.lang.resolve("messages.yml"), notRewrittenMessages(LegacyLangFiles.convertGeneral(fixture("english_general.yml"))), bundled("lang/english/messages.yml")));
		assertEquals(0, LegacyLangFiles.write(this.lang.resolve("sun_male.yml"), notRewritten(LegacyLangFiles.convertVoice(fixture("english_sun_male.yml"))), bundled("lang/english/voices/sun_male.yml")));
		assertEquals(0, LegacyLangFiles.write(this.lang.resolve("creatures_female.yml"), notRewritten(LegacyLangFiles.convertVoice(fixture("english_creatures_female.yml"))),
				bundled("lang/english/voices/creatures_female.yml")));
	}

	private static Map<String, String> notRewrittenMessages(Map<String, String> converted)
	{
		Map<String, String> kept = new java.util.LinkedHashMap<>(converted);
		kept.keySet().removeAll(REWRITTEN_MESSAGES);
		return kept;
	}

	private static Map<String, List<String>> notRewritten(Map<String, List<String>> converted)
	{
		Map<String, List<String>> kept = new java.util.LinkedHashMap<>(converted);
		kept.keySet().removeAll(REWRITTEN_VOICES);
		return kept;
	}

	/** Gods 0.9.x's files (bundled, or downloaded from GitHub) differ a lot from the new text, but nobody changed them */
	@Test
	void unchangedOldFilesOfGods09KeepNothing() throws IOException
	{
		for (String name : fixtureNames("legacy/lang/0.9/"))
		{
			String oldName = name.equals("english_general-github.yml") ? "english_general.yml" : name;
			YamlConfiguration stock = LegacyLangFiles.stock(this::resource, oldName);
			YamlConfiguration old = fixture("0.9/" + name);
			Map<String, ?> converted = oldName.equals("english_general.yml") ? LegacyLangFiles.convertGeneral(old, stock) : LegacyLangFiles.convertVoice(old, stock);
			assertEquals(Map.of(), converted, name);
		}
	}

	/** The files of this version before MiniMessage aren't customizations either, whatever the new text says */
	@Test
	void unchangedOldFilesOfThisVersionKeepNothing() throws IOException
	{
		assertEquals(Map.of(), LegacyLangFiles.convertGeneral(fixture("english_general.yml"), LegacyLangFiles.stock(this::resource, "english_general.yml")));
		assertEquals(Map.of(), LegacyLangFiles.convertVoice(fixture("english_sun_male.yml"), LegacyLangFiles.stock(this::resource, "english_sun_male.yml")));
	}

	@Test
	void theStockOfGods09IsThereForEveryFile() throws IOException
	{
		for (String name : fixtureNames("legacy/lang/0.9/"))
		{
			if (!name.equals("english_general-github.yml"))
			{
				assertNotNull(LegacyLangFiles.stock(this::resource, name), name);
			}
		}
		assertNull(LegacyLangFiles.stock(this::resource, "klingon_general.yml"));
	}

	/** A customized 0.9.x file keeps its changes, under the keys this version renamed too, and loses its stock lines */
	@Test
	void aCustomizedGods09FileKeepsOnlyItsChanges() throws IOException
	{
		YamlConfiguration general = fixture("0.9/english_general.yml");
		general.set("Info.AltarPrayingNowAllowed", "&4Nur Priester beten hier");
		general.set("Info.YourPrayerPower", "&eDu hast $Amount Gebetskraft");
		Map<String, String> messages = LegacyLangFiles.convertGeneral(general, LegacyLangFiles.stock(this::resource, "english_general.yml"));
		assertEquals(Map.of("altar.praying-not-allowed", "<dark_red>Nur Priester beten hier", "prayer.power", "<yellow>Du hast <hl><amount></hl><aqua> Gebetskraft"), messages);

		YamlConfiguration sun = fixture("0.9/english_sun_male.yml");
		sun.set("GodToBelieversGiveBiblesQuestStarted", List.of("Bring Bücher, $PlayerName!"));
		sun.set("GodToBelieversGetHolyArtifactThisQuestFailed", List.of("Zu spät"));
		List<String> praying = new java.util.ArrayList<>(sun.getStringList("GodToBelieverPraying"));
		praying.add("Bete lauter!");
		sun.set("GodToBelieverPraying", praying);
		Map<String, List<String>> voice = LegacyLangFiles.convertVoice(sun, LegacyLangFiles.stock(this::resource, "english_sun_male.yml"));
		assertEquals(List.of("believer-praying", "believers-give-items-quest-started", "believers-get-holy-artifact-quest-failed"), List.copyOf(voice.keySet()));
		assertEquals(List.of("Bring Bücher, <hl><player></hl>!"), voice.get("believers-give-items-quest-started"));
		assertEquals("Bete lauter!", voice.get("believer-praying").getLast());
	}

	/** A whole 0.9.x language folder, unchanged, becomes Gods' own new files */
	@Test
	void migratingUnchangedGods09FilesKeepsNothing() throws IOException
	{
		for (String name : fixtureNames("legacy/lang/0.9/"))
		{
			if (!name.equals("english_general-github.yml"))
			{
				copy("0.9/" + name, name);
			}
		}

		LegacyLangFiles.migrate(this.lang, this::resource, LOG);

		for (String path : List.of("messages.yml", "voices/war_female.yml", "voices/sun_male.yml", "voices/creatures_female.yml"))
		{
			assertEquals(bundledYaml("lang/english/" + path).saveToString(), YamlConfiguration.loadConfiguration(this.lang.resolve("english").resolve(path).toFile()).saveToString(), path);
		}
	}

	/** Situations the old files never had (the bundled voice files have them, and servers fall back to those) */
	private static final java.util.Set<Voice> NEW_VOICES = java.util.Set.of(Voice.BELIEVER_GOD_CHOSE_GENDER, Voice.BELIEVERS_GENDER_CHANGED,
			Voice.BELIEVERS_GOD_DESCENDED, Voice.BELIEVERS_GOD_ASCENDED, Voice.BELIEVERS_GOD_RANK_UP, Voice.BELIEVERS_GOD_RANK_DOWN, Voice.PRIESTS_MAIN_ALTAR_MOVED,
			Voice.PRIESTS_MAIN_ALTAR_LOST, Voice.BELIEVER_DEVOTION_LEVEL_UP, Voice.BELIEVER_DEVOTION_LEVEL_DOWN, Voice.BELIEVERS_DEVOTED_PRAISED);

	@Test
	void everyOldVoiceKeyTheCodeUsesHasANewKey() throws IOException
	{
		Map<String, List<String>> converted = LegacyLangFiles.convertVoice(fixture("english_sun_male.yml"));
		for (Voice voice : Voice.values())
		{
			if (NEW_VOICES.contains(voice))
			{
				continue;
			}
			assertTrue(converted.containsKey(voice.key()), voice.key());
		}
	}

	@Test
	void customizedFilesAreConvertedAndMoved() throws IOException
	{
		copy("customized_general.yml", "english_general.yml");
		copy("customized_sun_male.yml", "english_sun_male.yml");
		copy("customized_general.yml", "klingon_general.yml");
		copy("english_creatures_female.yml", "english_creatures_female.yml");
		Files.writeString(this.lang.resolve("notes.yml"), "mine: true\n");

		LegacyLangFiles.migrate(this.lang, this::resource, LOG);

		YamlConfiguration messages = YamlConfiguration.loadConfiguration(this.lang.resolve("english/messages.yml").toFile());
		assertEquals(Messages.MESSAGES_VERSION, messages.getInt(Messages.VERSION_KEY));
		// Changed by the admin
		assertEquals("<yellow>Du hast jetzt <bold><hl><amount></hl><reset><yellow> Gebetskraft", messages.getString("prayer.power"));
		assertEquals("<aqua>Type <white>/g info \\<godname></white> \\<now>", messages.getString("help.info-hint"));
		assertEquals("Möhre", messages.getString("names.item.CARROT"));
		assertEquals("<gender> der Sonne", messages.getString("names.god-type.SUN"));
		// The same as Gods' own, or missing from the old file: Gods' own
		assertEquals("<aqua>You prayed to <hl><god></hl> for a blessing!", messages.getString("prayer.prayed-for-blessing"));
		assertEquals("<aqua>You healed <hl><amount></hl> beings", messages.getString("power.healed"));
		assertFalse(messages.contains("names.item.STONE") && !messages.getString("names.item.STONE").equals("stone"));
		// The bundled file's comments are kept
		assertTrue(Files.readString(this.lang.resolve("english/messages.yml")).contains("# How a god's words look"));

		YamlConfiguration sun = YamlConfiguration.loadConfiguration(this.lang.resolve("english/voices/sun_male.yml").toFile());
		assertEquals(List.of("<reset><red>Kneel, <hl><player></hl><white><bold>!"), sun.getStringList("believer-praying"));
		assertEquals(List.of("We are allied with <hl><player></hl> now"), sun.getStringList("believers-alliance"));
		assertEquals(bundledYaml("lang/english/voices/sun_male.yml").getStringList("believers-war"), sun.getStringList("believers-war"));

		// A language Gods doesn't have keeps only what its old file said
		YamlConfiguration klingon = YamlConfiguration.loadConfiguration(this.lang.resolve("klingon/messages.yml").toFile());
		assertEquals("Möhre", klingon.getString("names.item.CARROT"));
		assertFalse(klingon.contains("power.healed"));

		// The old files are kept in lang/old; other files are left alone
		for (String name : List.of("english_general.yml", "english_sun_male.yml", "klingon_general.yml", "english_creatures_female.yml"))
		{
			assertFalse(Files.exists(this.lang.resolve(name)), name);
			assertTrue(Files.exists(this.lang.resolve("old").resolve(name)), name);
		}
		assertTrue(Files.exists(this.lang.resolve("notes.yml")));
	}

	@Test
	void anExistingNewFileIsNotOverwritten() throws IOException
	{
		Files.createDirectories(this.lang.resolve("english"));
		Files.writeString(this.lang.resolve("english/messages.yml"), "messages-version: 2\nprayer:\n  power: mine\n");
		copy("customized_general.yml", "english_general.yml");

		LegacyLangFiles.migrate(this.lang, this::resource, LOG);

		assertEquals("messages-version: 2\nprayer:\n  power: mine\n", Files.readString(this.lang.resolve("english/messages.yml")));
		assertTrue(Files.exists(this.lang.resolve("old/english_general.yml")));
	}

	@Test
	void aBrokenOldFileStaysWhereItIs() throws IOException
	{
		Files.writeString(this.lang.resolve("english_general.yml"), "Info: [unclosed\n");

		LegacyLangFiles.migrate(this.lang, this::resource, LOG);

		assertTrue(Files.exists(this.lang.resolve("english_general.yml")));
		assertFalse(Files.exists(this.lang.resolve("english/messages.yml")));
	}

	/** After the upgrade, Messages shows the admin's text */
	@Test
	void messagesReadTheConvertedFiles() throws IOException
	{
		copy("customized_general.yml", "english_general.yml");
		LegacyLangFiles.migrate(this.lang, this::resource, LOG);

		Messages messages = new Messages(this.lang, this::resource, LOG);
		messages.load("english", "Doggycraft");
		assertEquals("§eDu hast jetzt §65§e Gebetskraft", messages.legacy("prayer.power", Messages.amount(5)));
	}

	/** The files in a test resource folder */
	private List<String> fixtureNames(String folder) throws IOException
	{
		try (java.util.stream.Stream<Path> files = Files.list(Path.of(java.net.URI.create(Objects.requireNonNull(getClass().getClassLoader().getResource(folder), folder).toString()))))
		{
			return files.map(file -> file.getFileName().toString()).sorted().toList();
		}
	}

	private void copy(String fixture, String as) throws IOException
	{
		try (InputStream in = resource("legacy/lang/" + fixture))
		{
			Files.copy(Objects.requireNonNull(in, fixture), this.lang.resolve(as));
		}
	}

	private YamlConfiguration fixture(String name) throws IOException
	{
		return bundledYaml("legacy/lang/" + name);
	}

	private InputStream resource(String path)
	{
		return getClass().getClassLoader().getResourceAsStream(path);
	}

	private String bundled(String path) throws IOException
	{
		try (InputStream in = resource(path))
		{
			return new String(Objects.requireNonNull(in, path).readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private YamlConfiguration bundledYaml(String path) throws IOException
	{
		try (InputStream in = resource(path))
		{
			return YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(in, path), StandardCharsets.UTF_8));
		}
	}
}
