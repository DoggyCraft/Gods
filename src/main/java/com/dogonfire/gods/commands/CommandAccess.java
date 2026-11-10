package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;

public class CommandAccess extends GodsCommand
{
	protected CommandAccess()
	{
		super("access");
		this.permission = "gods.priest.access";
		this.parameters = "[open/close]";
		this.description = "Set your religion as open or invite only";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!hasPermission(sender))
		{
			sender.sendMessage(ChatColor.RED + "You do not have permission for that");
		}
		
		if (sender instanceof Player == false)
		{
			sender.sendMessage(stringPlayerOnly);
			return;
		}
		
		Player player = (Player) sender;
		
		if (!GodManager.instance().isPriest(player.getUniqueId()))
		{
			player.sendMessage(ChatColor.RED + "Only priests can set religion access");
			return;
		}
		
		String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());
		
		if (!GodManager.instance().godExist(godName))
		{
			player.sendMessage(ChatColor.RED + "That God does not exist");
			return;
		}
		
		String access = args[0];
		
		if (access.equalsIgnoreCase("open"))
		{
			GodManager.instance().setPrivateAccess(godName, false);
			Gods.instance().log(player.getName() + " /gods open");
			player.sendMessage(ChatColor.AQUA + "You set the religion access to " + ChatColor.YELLOW + "open" + ChatColor.AQUA + ".");
			player.sendMessage(ChatColor.AQUA + "Players can join religion by praying at altars.");
		}
		else if (access.equalsIgnoreCase("close"))
		{
			GodManager.instance().setPrivateAccess(godName, true);
			Gods.instance().log(player.getName() + " /gods close");
			player.sendMessage(ChatColor.AQUA + "You set the religion access to " + ChatColor.RED + "closed" + ChatColor.AQUA + ".");
			player.sendMessage(ChatColor.AQUA + "Players can now only pray to this religion by invitation.");
		}
		else
		{
			player.sendMessage(ChatColor.RED + "That is not a valid command");
		}
	}
}
