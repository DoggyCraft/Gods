package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;

public class CommandKick extends GodsCommand
{
	protected CommandKick()
	{
		super("kick");
		this.permission = "gods.priest.kick";
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
		if (!GodManager.instance().isPriest(player.getUniqueId()))
		{
			sender.sendMessage(ChatColor.RED + "Only priests can kick believers from a religion");
			return;
		}
		String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());
		String believerName = args[1];
		OfflinePlayer offlineBeliever = Gods.instance().getServer().getOfflinePlayer(believerName);
		String believerGodName = BelieverManager.instance().getGodForBeliever(offlineBeliever.getUniqueId());
		if ((believerGodName == null) || (!believerGodName.equals(godName)))
		{
			sender.sendMessage(ChatColor.RED + "There is no such believer called '" + believerName + "' in your religion");
			return;
		}
		if (believerGodName.equalsIgnoreCase(sender.getName()))
		{
			sender.sendMessage(ChatColor.RED + "You cannot kick yourself from your own religion, Bozo!");
			return;
		}
		BelieverManager.instance().removeBeliever(godName, offlineBeliever.getUniqueId());
		sender.sendMessage(ChatColor.AQUA + "You kicked " + ChatColor.YELLOW + believerName + ChatColor.AQUA + " from your religion!");
		Player believer = Gods.instance().getServer().getPlayer(believerName);
		if (believer != null)
		{
			believer.sendMessage(ChatColor.RED + "You were kicked from the religion of " + ChatColor.YELLOW + godName + ChatColor.AQUA + "!");
		}
		Gods.instance().log(sender.getName() + " /gods kick " + believerName);

	}

}
