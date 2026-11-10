package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

public class CommandSetNeutralLand extends GodsCommand
{
	CommandSetNeutralLand(GodsContext ctx)
	{
		super(ctx, "setsafe");
		this.permission = "gods.setsafe";
		this.description = "Set or clear neutral land at your location";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!ctx.settings().holyLand().enabled())
		{
			sender.sendMessage(ChatColor.RED + "Holy Land is not enabled on this server");
			return;
		}
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
		Player player = (Player) sender;
		if (ctx.holyLandService().isNeutralLandLocation(player.getLocation()))
		{
			ctx.holyLandService().clearNeutralLand(player.getLocation());
			ctx.titles().send(player, 1*20, 3*20, 1*20, ChatColor.DARK_GREEN + "Wilderness", ChatColor.WHITE + "");
			sender.sendMessage(ChatColor.AQUA + "You set cleared the neutral land in this location.");
		}
		else
		{
			ctx.holyLandService().setNeutralLand(player.getLocation());
			ctx.titles().send(player, 1*20, 3*20, 1*20, ChatColor.WHITE + "Neutral Land", ChatColor.GREEN + "You are safe against mobs and PvP");
			sender.sendMessage(ChatColor.AQUA + "You set neutral land in this location.");
		}
	}
}
