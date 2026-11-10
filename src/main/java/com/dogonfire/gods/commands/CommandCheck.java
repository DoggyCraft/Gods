package com.dogonfire.gods.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;

public class CommandCheck extends GodsCommand
{
	protected CommandCheck()
	{
		super("check");
		this.permission = "gods.check";
		this.parameters = "<player>";
		this.description = "Show the religion of a player";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!hasPermission(sender))
		{
			sender.sendMessage(stringNoPermission);
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
			sender.sendMessage(ChatColor.RED + "There is no player online with that name");
			return;
		}

		String godName = BelieverManager.instance().getGodForBeliever(believer.getUniqueId());

		if (godName == null)
		{
			sender.sendMessage(ChatColor.AQUA + believer.getDisplayName() + " does not believe in a god");
		}
		else if (GodManager.instance().isPriest(believer.getUniqueId()))
		{
			sender.sendMessage(ChatColor.AQUA + believer.getDisplayName() + " is the Priest of " + ChatColor.YELLOW + godName);
		}
		else
		{
			sender.sendMessage(ChatColor.AQUA + believer.getDisplayName() + " believes in " + ChatColor.YELLOW + godName);
		}

	}

}
