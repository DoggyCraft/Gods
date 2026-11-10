package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;

public class CommandLeaveGod extends GodsCommand
{
	protected CommandLeaveGod()
	{
		super("leave");
		this.permission = "gods.leave";
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
		
		String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());
		
		if (GodManager.instance().believerLeaveGod(player.getUniqueId()))
		{
			sender.sendMessage(ChatColor.AQUA + "You left the religion of " + ChatColor.YELLOW + godName);
		}
		else
		{
			sender.sendMessage(ChatColor.RED + "You are not part of any religion!");
		}
	}
}