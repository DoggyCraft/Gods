package com.dogonfire.gods.command;

import java.util.ArrayList;
import java.util.List;

import net.kyori.adventure.text.Component;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.model.GodMood;
import com.dogonfire.gods.service.RankRules;

/**
 * How a god's progress towards its next rank reads in /g rank and /g info: each condition as "✔ Believers: 12/10"
 * ({@code command.rank.met}/{@code unmet} with its name from {@code names.rank-condition.*} and its value).
 */
final class RankLines
{
	private RankLines()
	{
	}

	/** Each condition as one line of legacy text */
	static List<String> lines(GodsContext ctx, List<RankRules.Progress> progress, String metKey, String unmetKey)
	{
		List<String> lines = new ArrayList<>();
		for (RankRules.Progress condition : progress)
		{
			lines.add(ctx.messages().legacy(condition.met() ? metKey : unmetKey, Messages.component("name", ctx.messages().component("names.rank-condition." + condition.condition().key())),
					Messages.component("value", value(ctx, condition))));
		}
		return lines;
	}

	/** Where the god is with a condition, and what it needs */
	static Component value(GodsContext ctx, RankRules.Progress condition)
	{
		return switch (condition.condition())
		{
			// Has one or not, as a count a player reads at a glance: "Priest: 0/1"
			case PRIEST, MAIN_ALTAR -> ctx.messages().component("command.rank.value.count", Messages.text("current", condition.met() ? 1 : 0), Messages.text("required", 1));
			case MOOD -> ctx.messages().component("command.rank.value.mood", Messages.component("current", ctx.messages().moodName(mood(condition.current()))),
					Messages.component("required", ctx.messages().moodName(mood(condition.required()))));
			case HOURS_AFTER_PRIEST, HOURS_AT_PREVIOUS_RANK -> ctx.messages().component("command.rank.value.hours", Messages.text("current", condition.current()),
					Messages.text("required", condition.required()));
			default -> ctx.messages().component("command.rank.value.count", Messages.text("current", condition.current()), Messages.text("required", condition.required()));
		};
	}

	/** The mood of a level of {@link RankRules#moodLevel} */
	private static GodMood mood(long level)
	{
		int last = GodMood.values().length - 1;
		int index = last - (int) Math.max(0L, Math.min((long) last, level));
		return GodMood.values()[index];
	}
}
