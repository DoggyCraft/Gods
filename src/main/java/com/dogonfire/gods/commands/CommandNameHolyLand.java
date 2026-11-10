package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.HolyLandManager;

public class CommandNameHolyLand extends GodsCommand
{
	protected CommandNameHolyLand()
	{
		super("attack");
		this.permission = "gods.priest.name";
		this.description = "Name your Holy Land region";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!hasPermission(sender))
		{
			sender.sendMessage(ChatColor.RED + "You do not have permission for that");
			return;
		}
		if (sender instanceof Player == false)
		{
			sender.sendMessage(stringPlayerOnly);
			return;
		}	
		
		Player player = (Player)sender;
		
		if(!GodsConfiguration.instance().isHolyLandEnabled())
		{
			return;			
		}
			
		if(HolyLandManager.instance().isNeutralLandLocation(player.getLocation()))
		{
			sender.sendMessage(ChatColor.RED + "This is neutral land");
			return;			
		}

		String godName = HolyLandManager.instance().getGodAtHolyLandLocation(player.getLocation());
		
		if(godName==null)
		{
			sender.sendMessage(ChatColor.RED + "This is not a holy land");
			return;			
		}

		if(!GodManager.instance().isPriest(player.getUniqueId()))
		{
			sender.sendMessage(ChatColor.RED + "Only a priest can set names for holy land");
			return;			
		}	
		
		String playerGodName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());
		
		if(playerGodName==null || !godName.equals(playerGodName))
		{
			sender.sendMessage(ChatColor.RED + "You cannot set the name of this holy land");
			return;			
		}

		HolyLandManager.instance().setHolyLandName(player.getLocation(), args[1]);
		HolyLandManager.instance().sendTitleForHolyLand(player, playerGodName, player.getLocation());
		sender.sendMessage(ChatColor.AQUA + "You set the name of this Holy Land to " + ChatColor.GOLD + name);
	}
}
