package com.dogonfire.gods.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

/** /g setbible: the priest makes the book in their hand the holy book of their religion */
public final class SetBibleCommand extends SubCommand
{
	public SetBibleCommand(GodsContext ctx)
	{
		super(ctx, "setbible", "gods.priest.setbible");
		priestOnly("command.setbible.not-priest");
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
		requireRank(godName, this.ctx.settings().ranks().unlocks().bibles());
		// A priest of their own god, not just of any god
		if (!this.ctx.priestService().isPriestForGod(player.getUniqueId(), godName))
		{
			throw new CommandException("command.setbible.not-priest");
		}
		if (!this.ctx.holyBookService().setBible(godName, player))
		{
			throw new CommandException("command.setbible.failed", Messages.god(godName));
		}
		send(sender, "command.setbible.done", Messages.text("title", this.ctx.holyBookService().getBibleTitle(godName)));
	}
}
