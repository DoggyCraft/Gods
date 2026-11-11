package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import com.dogonfire.gods.api.BelieverInfo;
import com.dogonfire.gods.api.GodInfo;
import com.dogonfire.gods.api.QuestInfo;
import com.dogonfire.gods.model.Believer;
import com.dogonfire.gods.model.God;
import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.model.GodMood;
import com.dogonfire.gods.model.GodType;
import com.dogonfire.gods.model.Quest;
import com.dogonfire.gods.model.QuestType;

class ApiSnapshotsTest
{
	private static final UUID		PRIEST	= UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID		BELIEVER	= UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final Instant	CREATED	= Instant.parse("2026-01-02T03:04:05Z");

	// A world is only needed for its presence in a Location
	private static final World		WORLD	= (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[] { World.class }, (proxy, method, args) -> switch (method.getName())
	{
		case "getName" -> "world";
		case "hashCode" -> 1;
		case "equals" -> proxy == args[0];
		default -> null;
	});

	@Test
	void everyGodTypeHasTheSameNameInTheApi()
	{
		assertEquals(names(GodType.values()), names(com.dogonfire.gods.api.GodType.values()));
		for (GodType type : GodType.values())
		{
			assertEquals(type.name(), ApiSnapshots.godType(type).name());
		}
		assertNull(ApiSnapshots.godType(null));
	}

	@Test
	void gendersAndMoodsMapToTheApi()
	{
		assertEquals(com.dogonfire.gods.api.GodGender.MALE, ApiSnapshots.gender(GodGender.Male));
		assertEquals(com.dogonfire.gods.api.GodGender.FEMALE, ApiSnapshots.gender(GodGender.Female));
		assertEquals(com.dogonfire.gods.api.GodGender.NONE, ApiSnapshots.gender(GodGender.None));
		assertEquals(com.dogonfire.gods.api.GodGender.NONE, ApiSnapshots.gender(null));

		assertEquals(names(GodMood.values()), names(com.dogonfire.gods.api.GodMood.values()));
		assertEquals(com.dogonfire.gods.api.GodMood.ANGRY, ApiSnapshots.mood(-71.0F));
		assertEquals(com.dogonfire.gods.api.GodMood.NEUTRAL, ApiSnapshots.mood(0.0F));
		assertEquals(com.dogonfire.gods.api.GodMood.EXALTED, ApiSnapshots.mood(100.0F));
	}

	@Test
	void onlyTheQuestsGodsMakesHaveTheirOwnApiType()
	{
		assertEquals(com.dogonfire.gods.api.QuestType.PILGRIMAGE, ApiSnapshots.questType(QuestType.PILGRIMAGE));
		assertEquals(com.dogonfire.gods.api.QuestType.SLAY, ApiSnapshots.questType(QuestType.SLAY));
		assertEquals(com.dogonfire.gods.api.QuestType.ITEM_SACRIFICE, ApiSnapshots.questType(QuestType.ITEMSACRIFICE));
		assertEquals(com.dogonfire.gods.api.QuestType.GIVE_ITEMS, ApiSnapshots.questType(QuestType.GIVEITEMS));
		assertEquals(com.dogonfire.gods.api.QuestType.OTHER, ApiSnapshots.questType(QuestType.CONVERT));
		assertEquals(com.dogonfire.gods.api.QuestType.OTHER, ApiSnapshots.questType(QuestType.NONE));
	}

	@Test
	void aGodSnapshotHoldsTheModelAndWhatTheServerKnows()
	{
		God god = new God("Zeus");
		god.setCreated(CREATED);
		god.setType(GodType.THUNDER);
		god.setGender(GodGender.Male);
		god.setMood(42.5F);
		god.setDescription("Lord of the sky");
		god.priests().add(PRIEST);
		god.allies().add("Hera");
		god.enemies().add("Hades");
		QuestInfo quest = new QuestInfo("Zeus", com.dogonfire.gods.api.QuestType.SLAY, "ZOMBIE", 10, 3, null, null, null);
		Location home = new Location(WORLD, 1, 64, 2);

		GodInfo info = ApiSnapshots.god(god, 12.5F, Set.of(PRIEST, BELIEVER), home, 4, quest);

		assertEquals("Zeus", info.name());
		assertEquals(com.dogonfire.gods.api.GodType.THUNDER, info.type());
		assertEquals(com.dogonfire.gods.api.GodGender.MALE, info.gender());
		assertEquals(com.dogonfire.gods.api.GodMood.PLEASED, info.mood());
		assertEquals(42.5F, info.moodValue());
		assertEquals(12.5F, info.power());
		assertEquals(Set.of(PRIEST, BELIEVER), info.believers());
		assertEquals(List.of(PRIEST), info.priests());
		assertTrue(info.isPriest(PRIEST));
		assertFalse(info.isPriest(BELIEVER));
		assertTrue(info.isBeliever(BELIEVER));
		assertEquals(List.of("Hera"), info.allies());
		assertEquals(List.of("Hades"), info.enemies());
		assertEquals("Lord of the sky", info.description());
		assertEquals(home, info.home());
		assertEquals(4, info.holyLandClaims());
		assertEquals(quest, info.quest());
		assertEquals(CREATED, info.created());
	}

	@Test
	void aGodSnapshotDoesNotChangeWithTheGod()
	{
		God god = new God("Zeus");
		god.priests().add(PRIEST);
		Location home = new Location(WORLD, 1, 64, 2);
		GodInfo info = ApiSnapshots.god(god, 0.0F, Set.of(), home, 0, null);

		god.priests().clear();
		god.allies().add("Hera");
		home.setX(100);
		info.home().setY(0);

		assertEquals(List.of(PRIEST), info.priests());
		assertEquals(List.of(), info.allies());
		assertEquals(new Location(WORLD, 1, 64, 2), info.home());
		assertNotSame(info.home(), info.home());
		assertThrows(UnsupportedOperationException.class, () -> info.priests().add(BELIEVER));
	}

	@Test
	void aGodWithoutTypeGenderOrLoadedHomeStillHasASnapshot()
	{
		GodInfo info = ApiSnapshots.god(new God("Nobody"), 0.0F, Set.of(), new Location(null, 1, 2, 3), 0, null);

		assertNull(info.type());
		assertEquals(com.dogonfire.gods.api.GodGender.NONE, info.gender());
		assertEquals(com.dogonfire.gods.api.GodMood.NEUTRAL, info.mood());
		assertNull(info.home());
		assertNull(info.description());
		assertNull(info.created());
	}

	@Test
	void aQuestSnapshotKnowsWhenItExpires()
	{
		Quest quest = new Quest("Zeus");
		quest.setType(QuestType.PILGRIMAGE);
		quest.setTargetType("Mount Olympus");
		quest.setAmount(1);
		quest.setMaxDuration(90);
		quest.setCreatedTime(CREATED);
		Location chest = new Location(WORLD, 10, 70, -5);

		QuestInfo info = ApiSnapshots.quest(quest, chest);

		assertEquals("Zeus", info.god());
		assertEquals(com.dogonfire.gods.api.QuestType.PILGRIMAGE, info.type());
		assertEquals("Mount Olympus", info.target());
		assertEquals(1, info.amount());
		assertEquals(0, info.progress());
		assertEquals(CREATED, info.started());
		assertEquals(CREATED.plusSeconds(90 * 60), info.expires());
		assertEquals(chest, info.location());
	}

	@Test
	void aQuestWithMissingValuesOrNoTypeReadsAsGodsDoes()
	{
		Quest quest = new Quest("Zeus");
		quest.setType(QuestType.SLAY);
		QuestInfo info = ApiSnapshots.quest(quest, new Location(null, 0, 0, 0));
		assertEquals(0, info.amount());
		assertNull(info.started());
		assertNull(info.expires());
		assertNull(info.location());

		assertNull(ApiSnapshots.quest(new Quest("Hera"), null));
		assertNull(ApiSnapshots.quest(null, null));
	}

	@Test
	void onlyAPlayerWithAGodIsABeliever()
	{
		Believer believer = new Believer(BELIEVER);
		assertNull(ApiSnapshots.believer(believer, false));
		assertNull(ApiSnapshots.believer(null, false));

		believer.setGodName("Zeus");
		believer.setPrayers(7);
		believer.setPrayerPower(3);
		believer.setJoined(CREATED);
		believer.setLastPrayer(CREATED.plusSeconds(60));

		BelieverInfo info = ApiSnapshots.believer(believer, true);
		assertEquals(new BelieverInfo(BELIEVER, "Zeus", 7, 3, true, CREATED, CREATED.plusSeconds(60)), info);
	}

	private static List<String> names(Enum<?>[] values)
	{
		return Arrays.stream(values).map(Enum::name).toList();
	}
}
