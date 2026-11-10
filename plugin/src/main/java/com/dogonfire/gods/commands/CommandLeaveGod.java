package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

public class CommandLeaveGod extends GodsCommand
{
	CommandLeaveGod(GodsContext ctx)
	{
		super(ctx, "leave");
		this.permission = "gods.leave";
		this.description = "Leave your religion";
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
		
		Player player = (Player) sender;
		
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		
		if (ctx.godManager().believerLeaveGod(player.getUniqueId()))
		{
			sender.sendMessage(ChatColor.AQUA + "You left the religion of " + ChatColor.YELLOW + godName);
		}
		else
		{
			sender.sendMessage(ChatColor.RED + "You are not part of any religion!");
		}
	}
}
