package com.dogonfire.gods.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.Test;

import com.dogonfire.gods.api.event.BelieverJoinGodEvent;
import com.dogonfire.gods.api.event.BelieverLeaveGodEvent;
import com.dogonfire.gods.api.event.BelieverPrayEvent;
import com.dogonfire.gods.api.event.GodAscendEvent;
import com.dogonfire.gods.api.event.GodBlessPlayerEvent;
import com.dogonfire.gods.api.event.GodCreateEvent;
import com.dogonfire.gods.api.event.GodCursePlayerEvent;
import com.dogonfire.gods.api.event.GodDescendEvent;
import com.dogonfire.gods.api.event.GodGenderChangeEvent;
import com.dogonfire.gods.api.event.GodEvent;
import com.dogonfire.gods.api.event.GodMoodChangeEvent;
import com.dogonfire.gods.api.event.GodRankChangeEvent;
import com.dogonfire.gods.api.event.GodRelationChangeEvent;
import com.dogonfire.gods.api.event.GodSpeakEvent;
import com.dogonfire.gods.api.event.HolyLandAttackStartEvent;
import com.dogonfire.gods.api.event.HolyLandClaimEvent;
import com.dogonfire.gods.api.event.PriestAssignEvent;
import com.dogonfire.gods.api.event.PriestRemoveEvent;
import com.dogonfire.gods.api.event.QuestCompleteEvent;
import com.dogonfire.gods.api.event.QuestEvent;
import com.dogonfire.gods.api.event.QuestFailEvent;
import com.dogonfire.gods.api.event.QuestStartEvent;

/**
 * The API may only expose Java, Bukkit and its own types. Gods' internals change between versions, and Adventure is
 * relocated inside the Gods jar, so other plugins couldn't use either.
 */
class ApiSurfaceTest
{
	private static final List<Class<?>> API = List.of(GodsApi.class, GodInfo.class, BelieverInfo.class, QuestInfo.class,
			GodType.class, GodGender.class, GodMood.class, GodRank.class, QuestType.class, RankProgress.class, RankProgress.Condition.class,
			ActionResult.class,
			GodEvent.class, GodSpeakEvent.class, BelieverPrayEvent.class, BelieverJoinGodEvent.class, BelieverLeaveGodEvent.class,
			GodCreateEvent.class, GodMoodChangeEvent.class, GodGenderChangeEvent.class, PriestAssignEvent.class, PriestRemoveEvent.class, GodBlessPlayerEvent.class,
			GodCursePlayerEvent.class, QuestEvent.class, QuestStartEvent.class, QuestCompleteEvent.class, QuestFailEvent.class,
			HolyLandClaimEvent.class, HolyLandAttackStartEvent.class, GodRelationChangeEvent.class, GodDescendEvent.class, GodAscendEvent.class,
			GodRankChangeEvent.class);

	@Test
	void onlyExposesJavaBukkitAndApiTypes()
	{
		List<String> problems = new ArrayList<>();
		for (Class<?> type : API)
		{
			assertTrue(type.getPackageName().startsWith(GodsApi.class.getPackageName()), type + " is not in the API package");
			for (Method method : type.getMethods())
			{
				// Only what Gods declares; inherited Object, Record, Enum and Bukkit Event methods aren't its to keep stable
				if (!method.getDeclaringClass().getPackageName().startsWith(GodsApi.class.getPackageName()))
				{
					continue;
				}
				String where = type.getSimpleName() + "." + method.getName();
				check(problems, where, method.getGenericReturnType());
				for (Type parameter : method.getGenericParameterTypes())
				{
					check(problems, where, parameter);
				}
			}
			for (Constructor<?> constructor : type.getConstructors())
			{
				for (Type parameter : constructor.getGenericParameterTypes())
				{
					check(problems, type.getSimpleName() + ".<init>", parameter);
				}
			}
			if (type.isRecord())
			{
				for (RecordComponent component : type.getRecordComponents())
				{
					check(problems, type.getSimpleName() + "." + component.getName(), component.getGenericType());
				}
			}
		}
		assertTrue(problems.isEmpty(), "Types other plugins can't rely on: " + problems);
	}

	/** Components added to a snapshot keep the constructor plugins compiled against */
	@Test
	void snapshotsKeepTheirOldConstructors() throws ReflectiveOperationException
	{
		// GodInfo before mainAltar
		GodInfo.class.getConstructor(String.class, GodType.class, GodGender.class, GodMood.class, float.class, float.class, java.util.Set.class, List.class,
				List.class, List.class, String.class, org.bukkit.Location.class, int.class, QuestInfo.class, java.time.Instant.class);
		// GodInfo before descended
		GodInfo.class.getConstructor(String.class, GodType.class, GodGender.class, GodMood.class, float.class, float.class, java.util.Set.class, List.class,
				List.class, List.class, String.class, org.bukkit.Location.class, int.class, QuestInfo.class, java.time.Instant.class, org.bukkit.Location.class);
		// GodInfo before rank
		GodInfo.class.getConstructor(String.class, GodType.class, GodGender.class, GodMood.class, float.class, float.class, java.util.Set.class, List.class,
				List.class, List.class, String.class, org.bukkit.Location.class, int.class, QuestInfo.class, java.time.Instant.class, org.bukkit.Location.class,
				java.time.Instant.class);
	}

	/** The NPC methods came after the rest and have defaults, so other implementations (test doubles) still compile */
	@Test
	void npcMethodsHaveDefaults() throws ReflectiveOperationException
	{
		assertTrue(GodsApi.class.getMethod("getNpc", String.class).isDefault());
		assertTrue(GodsApi.class.getMethod("isGodNpc", org.bukkit.entity.Entity.class).isDefault());
		// So do the rank methods
		assertTrue(GodsApi.class.getMethod("getRankProgress", String.class).isDefault());
		assertTrue(GodsApi.class.getMethod("setRank", String.class, GodRank.class).isDefault());
	}

	/** Bukkit only lets plugins listen to an event that has a static getHandlerList */
	@Test
	void everyEventCanBeListenedTo() throws ReflectiveOperationException
	{
		for (Class<?> type : API)
		{
			if (Event.class.isAssignableFrom(type) && !Modifier.isAbstract(type.getModifiers()))
			{
				Method handlerList = type.getDeclaredMethod("getHandlerList");
				assertTrue(Modifier.isStatic(handlerList.getModifiers()) && Modifier.isPublic(handlerList.getModifiers()), type.getSimpleName());
				assertTrue(handlerList.invoke(null) instanceof HandlerList, type.getSimpleName());
			}
		}
	}

	private static void check(List<String> problems, String where, Type type)
	{
		if (type instanceof ParameterizedType parameterized)
		{
			check(problems, where, parameterized.getRawType());
			for (Type argument : parameterized.getActualTypeArguments())
			{
				check(problems, where, argument);
			}
			return;
		}
		if (!(type instanceof Class<?> c))
		{
			return;
		}
		while (c.isArray())
		{
			c = c.getComponentType();
		}
		if (c.isPrimitive())
		{
			return;
		}
		String name = c.getName();
		boolean allowed = name.startsWith("java.") || name.startsWith("org.bukkit.") || name.startsWith(GodsApi.class.getPackageName() + ".");
		if (!allowed)
		{
			problems.add(where + " uses " + name);
		}
	}
}
