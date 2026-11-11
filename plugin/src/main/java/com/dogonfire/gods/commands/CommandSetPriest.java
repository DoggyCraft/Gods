package com.dogonfire.gods.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

public class CommandSetPriest extends GodsCommand
{
	CommandSetPriest(GodsContext ctx)
	{
		super(ctx, "setpriest");
		this.permission = "gods.setpriest";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (args.length < 3)
		{
			send(sender, "command.setpriest.not-enough-arguments");
			sendUsage(sender);
			return;
		}
		if (!hasPermission(sender))
		{
			send(sender, NO_PERMISSION);
			return;
		}
		String godName = ctx.godService().formatGodName(args[1]);
		if (!ctx.godService().godExist(godName))
		{
			send(sender, "command.setpriest.no-god", Messages.god(godName));
			return;
		}

		Player otherPlayer = ctx.plugin().getServer().getPlayerExact(args[2]);
		if (otherPlayer == null)
		{
			send(sender, "command.setpriest.no-player");
			return;
		}

		if (ctx.priestService().assignPriest(godName, otherPlayer.getUniqueId()))
		{
			send(sender, "command.setpriest.done", Messages.player(otherPlayer.getName()), Messages.god(godName));
		}
		else
		{
			send(sender, "command.setpriest.failed", Messages.player(otherPlayer.getName()), Messages.god(godName));
		}
	}

}
