package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.HolyLandManager;
import com.dogonfire.gods.managers.TitleManager;



public class CommandSetNeutralLand extends GodsCommand
{
	public CommandSetNeutralLand()
	{
		super("setsafe");
		this.permission = "gods.setsafe";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!GodsConfiguration.instance().isHolyLandEnabled())
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
		if (HolyLandManager.instance().isNeutralLandLocation(player.getLocation()))
		{
			HolyLandManager.instance().clearNeutralLand(player.getLocation());
			TitleManager.sendTitle(player, 1*20, 3*20, 1*20, ChatColor.DARK_GREEN + "Wilderness", ChatColor.WHITE + "");
			sender.sendMessage(ChatColor.AQUA + "You set cleared the neutral land in this location.");
		}
		else
		{
			HolyLandManager.instance().setNeutralLand(player.getLocation());
			TitleManager.sendTitle(player, 1*20, 3*20, 1*20, ChatColor.WHITE + "Neutral Land", ChatColor.GREEN + "You are safe against mobs and PvP");
			sender.sendMessage(ChatColor.AQUA + "You set neutral land in this location.");
		}
	}
}
