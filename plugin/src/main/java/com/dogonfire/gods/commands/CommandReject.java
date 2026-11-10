package com.dogonfire.gods.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

public class CommandReject extends GodsCommand
{
	CommandReject(GodsContext ctx)
	{
		super(ctx, "no");
		this.permission = "gods.reject";
		this.description = "Reject a proposal from your God";
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

		if (ctx.believerManager().getGodForBeliever(((Player) sender).getUniqueId()) == null)
		{
			sender.sendMessage(stringNoGod);
			return;
		}

		Player player = (Player) sender;
		ctx.godManager().believerReject(player.getUniqueId());
	}

}
