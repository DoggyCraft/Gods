package com.dogonfire.gods.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Reading the arguments of /g admin, without a server so it can be tested: amounts that set a value or change it
 * ({@code 20}, {@code +5}, {@code -5}) and names of ranks and devotion levels (their key or the name a god type gives
 * them, ignoring case, spaces and underscores).
 */
public final class AdminArgs
{
	/** The actions of /g admin, in the order help lists them; each needs gods.admin.&lt;action&gt; */
	public static final List<String> ACTIONS = List.of("info", "setrank", "devotion", "prayerpower", "mood", "gender", "godskin", "quest", "setaltar");

	/** What /g admin quest does */
	public static final List<String> QUEST_ACTIONS = List.of("start", "complete", "fail");

	private AdminArgs()
	{
	}

	/** The permission node of an action */
	public static String permission(String action)
	{
		return "gods.admin." + action;
	}

	/** The action with that name (ignoring case), or null */
	public static String action(String name)
	{
		String lower = name.toLowerCase(Locale.ROOT);
		return ACTIONS.contains(lower) ? lower : null;
	}

	/**
	 * A whole number to set, or to add when it starts with + or - ({@code -5} takes 5)
	 *
	 * @param relative whether it changes the value rather than setting it
	 */
	public record Amount(boolean relative, long value)
	{
		/** The value after applying this to {@code current} */
		public long applyTo(long current)
		{
			return this.relative ? current + this.value : this.value;
		}
	}

	/** {@code 20}, {@code +5} or {@code -5}; null if it is no whole number */
	public static Amount amount(String text)
	{
		String trimmed = text.trim();
		boolean relative = trimmed.startsWith("+") || trimmed.startsWith("-");
		String digits = relative ? trimmed.substring(1) : trimmed;
		if (digits.isEmpty() || digits.length() > 9 || !digits.chars().allMatch(Character::isDigit))
		{
			return null;
		}
		long value = Long.parseLong(digits);
		return new Amount(relative, trimmed.startsWith("-") ? -value : value);
	}

	/** A number to set, or to add when it starts with + or -; null if it is none (or not finite) */
	public record Decimal(boolean relative, double value)
	{
		public double applyTo(double current)
		{
			return this.relative ? current + this.value : this.value;
		}
	}

	/**
	 * {@code 20} or {@code 12.5} to set, {@code +5} or {@code -5} to add, {@code =-50} to set a negative value; null if it
	 * is no number
	 */
	public static Decimal decimal(String text)
	{
		String trimmed = text.trim();
		boolean set = trimmed.startsWith("=");
		String number = set ? trimmed.substring(1) : trimmed;
		boolean relative = !set && (number.startsWith("+") || number.startsWith("-"));
		if (number.startsWith("+"))
		{
			number = number.substring(1);
		}
		if (!number.matches("-?\\d{1,6}(\\.\\d{1,4})?"))
		{
			return null;
		}
		return new Decimal(relative, Double.parseDouble(number));
	}

	/** Lower case, without spaces, underscores and hyphens: how names are compared */
	static String normalize(String name)
	{
		return name.toLowerCase(Locale.ROOT).replaceAll("[\\s_-]", "");
	}

	/**
	 * The constant {@code text} names: its name ({@code exalted}) or what {@code names} calls it (a god type's name for
	 * it, with colors left out), ignoring case, spaces and underscores; null if none
	 */
	public static <E extends Enum<E>> E named(String text, E[] values, Function<E, String> names)
	{
		String wanted = normalize(text);
		if (wanted.isEmpty())
		{
			return null;
		}
		for (E value : values)
		{
			if (normalize(value.name()).equals(wanted))
			{
				return value;
			}
		}
		for (E value : values)
		{
			String name = names.apply(value);
			if (name != null && normalize(name).equals(wanted))
			{
				return value;
			}
		}
		return null;
	}

	/** The constants' names in lower case and the names {@code names} gives them (without spaces), for tab completion */
	public static <E extends Enum<E>> List<String> completions(E[] values, Function<E, String> names)
	{
		List<String> completions = new ArrayList<>();
		for (E value : values)
		{
			completions.add(value.name().toLowerCase(Locale.ROOT));
		}
		for (E value : values)
		{
			String name = names.apply(value);
			if (name != null && !name.isBlank() && !name.contains(" ") && !completions.contains(name.toLowerCase(Locale.ROOT)))
			{
				completions.add(name.toLowerCase(Locale.ROOT));
			}
		}
		return completions;
	}
}
