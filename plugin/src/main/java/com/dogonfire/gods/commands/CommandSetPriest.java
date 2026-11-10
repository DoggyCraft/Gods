package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

public class CommandSetPriest extends GodsCommand
{
	CommandSetPriest(GodsContext ctx)
	{
		super(ctx, "setpriest");
		this.permission = "gods.setpriest";
		this.parameters = "<god> <player>";
		this.description = "Assign someone to be the priest";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (args.length < 3)
		{
			sender.sendMessage(ChatColor.RED + "Not enough arguments");
			sendUsage(sender);
			return;
		}
		if (!hasPermission(sender))
		{
			sender.sendMessage(stringNoPermission);
			return;
		}
		String godName = ctx.godService().formatGodName(args[1]);
		if (!ctx.godService().godExist(godName))
		{
			sender.sendMessage(ChatColor.RED + "There is no god called '" + ChatColor.GOLD + godName + ChatColor.AQUA + "'");
			return;
		}

		Player otherPlayer = ctx.plugin().getServer().getPlayerExact(args[2]);
		if (otherPlayer == null)
		{
			sender.sendMessage(ChatColor.RED + "There is no such player online");
			return;
		}

		if (ctx.godManager().assignPriest(godName, otherPlayer.getUniqueId()))
		{
			sender.sendMessage(ChatColor.AQUA + "You set " + ChatColor.GOLD + otherPlayer.getName() + ChatColor.AQUA + " as priest of " + ChatColor.GOLD + godName);
		}
		else
		{
			sender.sendMessage(ChatColor.GOLD + otherPlayer.getName() + ChatColor.RED + " was not assigned as a priest of " + ChatColor.GOLD + godName);
		}
	}

}
