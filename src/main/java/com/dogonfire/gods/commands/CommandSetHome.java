package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.HolyLandManager;
import com.dogonfire.gods.managers.LanguageManager;

public class CommandSetHome extends GodsCommand
{
	protected CommandSetHome()
	{
		super("sethome");
		this.permission = "gods.sethome";
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
		if ((GodsConfiguration.instance().isOnlyPriestCanSetHome()) && (!GodManager.instance().isPriest(player.getUniqueId())))
		{
			sender.sendMessage(ChatColor.RED + "Only your priest can set the home for your religion");
			return;
		}
		if (GodsConfiguration.instance().isHolyLandEnabled())
		{
			if (HolyLandManager.instance().isNeutralLandLocation(player.getLocation()))
			{
				sender.sendMessage(ChatColor.RED + "You can only set religion home within your Holy Land");
				return;
			}
			String locationGod = HolyLandManager.instance().getGodAtHolyLandLocation(player.getLocation());
			if ((locationGod == null) || (!locationGod.equals(godName)))
			{
				sender.sendMessage(ChatColor.RED + "You can only set religion home within your Holy Land");
				return;
			}
		}
		GodManager.instance().setHomeForGod(godName, player.getLocation());
		LanguageManager.instance().setPlayerName(player.getName());
		GodManager.instance().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSetHome, 2);
	}
}
