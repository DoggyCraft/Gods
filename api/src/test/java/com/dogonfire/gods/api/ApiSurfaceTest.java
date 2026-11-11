package com.dogonfire.gods.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The API may only expose Java, Bukkit and its own types. Gods' internals change between versions, and Adventure is
 * relocated inside the Gods jar, so other plugins couldn't use either.
 */
class ApiSurfaceTest
{
	private static final List<Class<?>> API = List.of(GodsApi.class, GodInfo.class, BelieverInfo.class, QuestInfo.class,
			GodType.class, GodGender.class, GodMood.class, QuestType.class);

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
