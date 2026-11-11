package com.dogonfire.gods.command;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.service.GodRules;

/**
 * /g gender &lt;god|goddess|deity&gt;: the priest changes the gender of their god (the words of altar signs, the
 * language's and English), at most once per priests.gender-change-cooldown-hours
 */
public final class GenderCommand extends SubCommand
{
	public GenderCommand(GodsContext ctx)
	{
		super(ctx, "gender", "gods.priest.gender");
		priestOnly();
		args(1, 1);
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		String godName = believersGod((Player) sender);
		GodGender gender = this.ctx.altarService().genderWords().gender(args.getFirst());
		if (gender == null)
		{
			throw new CommandException("command.gender.unknown", Messages.text("word", args.getFirst()));
		}
		if (gender == this.ctx.godService().getGenderForGod(godName))
		{
			throw new CommandException("command.gender.already", Messages.god(godName), this.ctx.godService().genderTag(godName));
		}
		Duration wait = GodRules.genderChangeWait(this.ctx.godService().getGenderChangedTime(godName), Instant.now(),
				this.ctx.settings().priests().genderChangeCooldownHours());
		if (!wait.isZero())
		{
			throw new CommandException("command.gender.cooldown", Messages.god(godName), Messages.text("hours", Math.max(1L, (wait.toMinutes() + 59L) / 60L)));
		}
		if (!this.ctx.godService().changeGender(godName, gender))
		{
			throw new CommandException("command.cancelled");
		}
		send(sender, "command.gender.done", Messages.god(godName), this.ctx.godService().genderTag(godName));
	}

	/** The gender words in lower case (the language's and English), each once */
	@Override
	public List<String> tabComplete(CommandSender sender, List<String> args)
	{
		if (args.size() != 1)
		{
			return List.of();
		}
		Set<String> words = new LinkedHashSet<>();
		for (String word : this.ctx.altarService().genderWords().all())
		{
			if (!word.isBlank())
			{
				words.add(word.toLowerCase(Locale.ROOT));
			}
		}
		return List.copyOf(words);
	}
}
