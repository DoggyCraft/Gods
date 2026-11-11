package com.dogonfire.gods.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

public class CommandCheck extends GodsCommand
{
	CommandCheck(GodsContext ctx)
	{
		super(ctx, "check");
		this.permission = "gods.check";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!hasPermission(sender))
		{
			send(sender, NO_PERMISSION);
			return;
		}
		if (args.length < 2)
		{
			sendUsage(sender);
			return;
		}
		Player believer = Bukkit.getPlayerExact(args[1]);
		if (believer == null)
		{
			send(sender, "command.check.no-player");
			return;
		}

		String godName = ctx.believerService().getGodForBeliever(believer.getUniqueId());

		if (godName == null)
		{
			send(sender, "command.check.no-god", Messages.displayName(believer.getDisplayName()));
		}
		else if (ctx.priestService().isPriest(believer.getUniqueId()))
		{
			send(sender, "command.check.priest", Messages.displayName(believer.getDisplayName()), Messages.god(godName));
		}
		else
		{
			send(sender, "command.check.believer", Messages.displayName(believer.getDisplayName()), Messages.god(godName));
		}

	}

}
