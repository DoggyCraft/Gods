package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import org.junit.jupiter.api.Test;

import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.message.Voice;

/** What the API does without a server: voice situations and placeholders */
class GodsApiServiceTest
{
	@Test
	void everyVoiceIsASituationByItsKey()
	{
		var situations = new GodsApiService(null).getVoiceSituations();

		assertEquals(Voice.values().length, situations.size());
		for (Voice voice : Voice.values())
		{
			assertTrue(situations.contains(voice.key()), voice.key());
			assertEquals(voice, GodsApiService.situation(voice.key()));
			assertEquals(voice, Voice.byKey(voice.key()).orElseThrow());
		}
		assertThrows(UnsupportedOperationException.class, () -> situations.add("new-situation"));
	}

	@Test
	void anUnknownSituationIsTheCallersMistake()
	{
		assertTrue(Voice.byKey("BELIEVER_PRAYING").isEmpty());
		assertTrue(Voice.byKey(null).isEmpty());
		assertThrows(IllegalArgumentException.class, () -> GodsApiService.situation("believer-dancing"));
		assertThrows(IllegalArgumentException.class, () -> GodsApiService.situation(null));
	}

	@Test
	void placeholdersAreInsertedAsPlainText()
	{
		Map<String, String> placeholders = new LinkedHashMap<>();
		placeholders.put("amount", "5");
		placeholders.put("type", "<red>Zombie");
		placeholders.put("player_2", "Bob");

		String rendered = Messages.plain(MiniMessage.miniMessage().deserialize("<amount> <type> <player_2>", TagResolver.resolver(GodsApiService.placeholders(placeholders))));

		assertEquals("5 <red>Zombie Bob", rendered);
	}

	@Test
	void placeholderNamesMustBeTagNames()
	{
		assertThrows(IllegalArgumentException.class, () -> GodsApiService.placeholders(Map.of("Amount", "5")));
		assertThrows(IllegalArgumentException.class, () -> GodsApiService.placeholders(Map.of("the type", "x")));
		assertThrows(IllegalArgumentException.class, () -> GodsApiService.placeholders(Map.of("", "x")));
		assertEquals(0, GodsApiService.placeholders(Map.of()).length);
	}
}
