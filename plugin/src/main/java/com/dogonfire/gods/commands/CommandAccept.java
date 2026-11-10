package com.dogonfire.gods.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

public class CommandAccept extends GodsCommand
{
	CommandAccept(GodsContext ctx)
	{
		super(ctx, "yes");
		this.permission = "gods.accept";
		this.description = "Accept a proposal from your God";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!hasPermission(sender))
		{
			sender.sendMessage(stringNoPermission);
			return;
		}
		if (sender instanceof Player == false)
		{
			sender.sendMessage(stringPlayerOnly);
			return;
		}

		if (ctx.believerService().getGodForBeliever(((Player) sender).getUniqueId()) == null)
		{
			sender.sendMessage(stringNoGod);
			return;
		}
		
		ctx.godManager().believerAccept(((Player) sender).getUniqueId());
	}
}
