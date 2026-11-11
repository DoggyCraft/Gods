package com.dogonfire.gods.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;

import com.dogonfire.gods.api.BelieverInfo;
import com.dogonfire.gods.api.GodInfo;
import com.dogonfire.gods.api.QuestInfo;
import com.dogonfire.gods.model.Believer;
import com.dogonfire.gods.model.God;
import com.dogonfire.gods.model.Quest;

/**
 * Turns Gods' models into the API's snapshots, and its enums into the API's. Pure (no server), so the tests cover it;
 * {@link GodsApiService} gathers what needs the server (power, believers, worlds) and passes it in.
 */
public final class ApiSnapshots
{
	private ApiSnapshots()
	{
	}

	public static com.dogonfire.gods.api.GodType godType(com.dogonfire.gods.model.GodType type)
	{
		return type != null ? com.dogonfire.gods.api.GodType.valueOf(type.name()) : null;
	}

	public static com.dogonfire.gods.api.GodGender gender(com.dogonfire.gods.model.GodGender gender)
	{
		if (gender == null)
		{
			return com.dogonfire.gods.api.GodGender.NONE;
		}
		return switch (gender)
		{
			case Male -> com.dogonfire.gods.api.GodGender.MALE;
			case Female -> com.dogonfire.gods.api.GodGender.FEMALE;
			case NonBinary -> com.dogonfire.gods.api.GodGender.NON_BINARY;
			case None -> com.dogonfire.gods.api.GodGender.NONE;
		};
	}

	/** The model's gender for the API's */
	public static com.dogonfire.gods.model.GodGender modelGender(com.dogonfire.gods.api.GodGender gender)
	{
		return switch (gender)
		{
			case MALE -> com.dogonfire.gods.model.GodGender.Male;
			case FEMALE -> com.dogonfire.gods.model.GodGender.Female;
			case NON_BINARY -> com.dogonfire.gods.model.GodGender.NonBinary;
			case NONE -> com.dogonfire.gods.model.GodGender.None;
		};
	}

	public static com.dogonfire.gods.api.GodMood mood(float moodValue)
	{
		return com.dogonfire.gods.api.GodMood.valueOf(MoodRules.moodFor(moodValue).name());
	}

	public static com.dogonfire.gods.api.QuestType questType(com.dogonfire.gods.model.QuestType type)
	{
		return switch (type)
		{
			case PILGRIMAGE -> com.dogonfire.gods.api.QuestType.PILGRIMAGE;
			case SLAY -> com.dogonfire.gods.api.QuestType.SLAY;
			case ITEMSACRIFICE -> com.dogonfire.gods.api.QuestType.ITEM_SACRIFICE;
			case GIVEITEMS -> com.dogonfire.gods.api.QuestType.GIVE_ITEMS;
			default -> com.dogonfire.gods.api.QuestType.OTHER;
		};
	}

	/**
	 * The quest as the API shows it, or null if the god has none
	 *
	 * @param location where its pilgrimage leads, or null
	 */
	public static QuestInfo quest(Quest quest, Location location)
	{
		if (quest == null || quest.typeName() == null)
		{
			return null;
		}
		Instant started = quest.createdTime();
		Instant expires = (started != null && quest.maxDuration() != null) ? started.plus(Duration.ofMinutes(quest.maxDuration())) : null;
		return new QuestInfo(quest.godName(), questType(quest.type()), quest.targetType(), quest.amountOrZero(), quest.progressOrZero(), started, expires,
				location != null && location.getWorld() != null ? location : null);
	}

	/**
	 * The god as the API shows it
	 *
	 * @param power          its power now (from its believers)
	 * @param believers      everyone who believes in it
	 * @param home           its home, or null (also when its world isn't loaded)
	 * @param holyLandClaims how many regions it claimed
	 * @param quest          its quest, or null
	 */
	public static GodInfo god(God god, float power, Set<UUID> believers, Location home, int holyLandClaims, QuestInfo quest)
	{
		return new GodInfo(god.name(), godType(god.type()), gender(god.gender()), mood(god.mood()), god.mood(), power, believers, god.priests(), god.allies(),
				god.enemies(), god.description(), home != null && home.getWorld() != null ? home : null, holyLandClaims, quest, god.created());
	}

	/** The believer as the API shows them, or null if they believe in no god */
	public static BelieverInfo believer(Believer believer, boolean priest)
	{
		if (believer == null || believer.godName() == null)
		{
			return null;
		}
		return new BelieverInfo(believer.id(), believer.godName(), believer.prayers(), believer.prayerPower(), priest, believer.joined(), believer.lastPrayer());
	}
}
