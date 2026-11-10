package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

public class CommandTogglePVP extends GodsCommand
{
	CommandTogglePVP(GodsContext ctx)
	{
		super(ctx, "pvp");
		this.permission = "gods.priest.pvp";
		this.description = "Toggle PvP for your religion";
		this.priestOnly = true;
	}

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
		Player player = (Player) sender;
		if (!ctx.godManager().isPriest(player.getUniqueId()))
		{
			player.sendMessage(stringPreistOnly);
			return;
		}
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		boolean pvp = ctx.godManager().getGodPvP(godName);
		if (pvp)
		{
			sender.sendMessage(ChatColor.AQUA + "You set PvP for your religion to " + ChatColor.YELLOW + " disabled");
			ctx.godManager().setGodPvP(godName, false);
		}
		else
		{
			sender.sendMessage(ChatColor.AQUA + "You set PvP for your religion to " + ChatColor.YELLOW + " enabled");
			ctx.godManager().setGodPvP(godName, true);
		}
	}

}
