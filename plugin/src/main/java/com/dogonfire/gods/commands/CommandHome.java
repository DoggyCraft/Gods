package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;

public class CommandHome extends GodsCommand
{
	protected CommandHome()
	{
		super("home");
		this.permission = "gods.home";
		this.description = "Teleport to the home of your religion";
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

		if (godName == null)
		{
			sender.sendMessage(ChatColor.RED + "You do not believe in a God");
			return;
		}
		Location location = GodManager.instance().getHomeForGod(godName);
		if (location == null || location.getWorld() == null)
		{
			sender.sendMessage(ChatColor.RED + "Your religion does not have a home");
			return;
		}
		player.teleport(location);
	}
}
