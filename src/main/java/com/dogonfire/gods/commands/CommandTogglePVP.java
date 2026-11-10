package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;

public class CommandTogglePVP extends GodsCommand
{
	protected CommandTogglePVP()
	{
		super("pvp");
		this.permission = "gods.priest.pvp";
		this.description = "Toggle pvp for your religon";
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
		if (!GodManager.instance().isPriest(player.getUniqueId()))
		{
			player.sendMessage(stringPreistOnly);
			return;
		}
		String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());
		boolean pvp = GodManager.instance().getGodPvP(godName);
		if (pvp)
		{
			sender.sendMessage(ChatColor.AQUA + "You set PvP for your religion to " + ChatColor.YELLOW + " disabled");
			GodManager.instance().setGodPvP(godName, false);
		}
		else
		{
			sender.sendMessage(ChatColor.AQUA + "You set PvP for your religion to " + ChatColor.YELLOW + " enabled");
			GodManager.instance().setGodPvP(godName, true);
		}
	}

}
