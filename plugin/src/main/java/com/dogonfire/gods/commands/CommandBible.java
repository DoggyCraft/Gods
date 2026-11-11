package com.dogonfire.gods.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

public class CommandBible extends GodsCommand
{
	CommandBible(GodsContext ctx)
	{
		super(ctx, "bible");
		this.permission = "gods.priest.bible";
		this.priestOnly = true;
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!ctx.settings().bibles().enabled())
		{
			send(sender, "command.disabled.bibles");
			return;
		}
		if (!hasPermission(sender))
		{
			send(sender, "command.not-allowed");
			return;
		}
		if (sender instanceof Player == false)
		{
			send(sender, PLAYER_ONLY);
			return;
		}
		Player player = (Player) sender;
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		if (godName == null)
		{
			send(sender, "command.not-believer");
			return;
		}

		if (!ctx.priestService().isPriest(player.getUniqueId()))
		{
			send(sender, "command.bible.not-priest");
			return;
		}
		if (!ctx.holyBookService().giveBible(godName, player))
		{
			send(sender, "command.bible.failed", Messages.god(godName));
			return;
		}
		send(sender, "command.bible.produced", Messages.text("title", ctx.holyBookService().getBibleTitle(godName)));
	}
}
