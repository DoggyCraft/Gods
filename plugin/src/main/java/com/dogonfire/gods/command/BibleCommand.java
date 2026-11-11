package com.dogonfire.gods.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

/** /g bible: the priest gets a copy of the holy book of their religion */
public final class BibleCommand extends SubCommand
{
	public BibleCommand(GodsContext ctx)
	{
		super(ctx, "bible", "gods.priest.bible");
		priestOnly("command.bible.not-priest");
	}

	@Override
	public String disabledMessage()
	{
		return this.ctx.settings().bibles().enabled() ? null : "command.disabled.bibles";
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		Player player = (Player) sender;
		String godName = believersGod(player);
		if (!this.ctx.holyBookService().giveBible(godName, player))
		{
			throw new CommandException("command.bible.failed", Messages.god(godName));
		}
		send(sender, "command.bible.produced", Messages.text("title", this.ctx.holyBookService().getBibleTitle(godName)));
	}
}
