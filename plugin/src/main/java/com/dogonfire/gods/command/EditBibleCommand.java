package com.dogonfire.gods.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

/** /g editbible: the priest gets a book and quill of the holy book of their religion to edit */
public final class EditBibleCommand extends SubCommand
{
	public EditBibleCommand(GodsContext ctx)
	{
		super(ctx, "editbible", "gods.priest.editbible");
		priestOnly("command.editbible.not-priest");
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
		if (!this.ctx.holyBookService().giveEditBible(godName, player))
		{
			throw new CommandException("command.editbible.failed", Messages.god(godName));
		}
		send(sender, "command.bible.produced", Messages.text("title", this.ctx.holyBookService().getBibleTitle(godName)));
		send(sender, "command.editbible.hint");
	}
}
