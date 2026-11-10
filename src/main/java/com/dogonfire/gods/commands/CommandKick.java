package com.dogonfire.gods.commands;

import java.util.UUID;

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
		this.parameters = "<player>";
		this.description = "Kick a believer from your religion";
		this.priestOnly = true;
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
		if (args.length < 2)
		{
			sendUsage(sender);
			return;
		}
		String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());
		String believerName = args[1];
		UUID believerId = findBeliever(godName, believerName);
		if (godName == null || believerId == null)
		{
			sender.sendMessage(ChatColor.RED + "There is no such believer called '" + believerName + "' in your religion");
			return;
		}
		if (believerId.equals(player.getUniqueId()))
		{
			sender.sendMessage(ChatColor.RED + "You cannot kick yourself from your own religion, Bozo!");
			return;
		}
		BelieverManager.instance().removeBeliever(godName, believerId);
		sender.sendMessage(ChatColor.AQUA + "You kicked " + ChatColor.YELLOW + believerName + ChatColor.AQUA + " from your religion!");
		Player believer = Gods.instance().getServer().getPlayer(believerId);
		if (believer != null)
		{
			believer.sendMessage(ChatColor.RED + "You were kicked from the religion of " + ChatColor.YELLOW + godName + ChatColor.RED + "!");
		}
		Gods.instance().log(sender.getName() + " /gods kick " + believerName);
	}

	/**
	 * Finds a believer of a god by name among the god's known believers, so it
	 * never asks Mojang for an unknown name.
	 */
	private UUID findBeliever(String godName, String believerName)
	{
		if (godName == null)
		{
			return null;
		}
		for (UUID believerId : BelieverManager.instance().getBelieversForGod(godName))
		{
			OfflinePlayer offlinePlayer = Gods.instance().getServer().getOfflinePlayer(believerId);
			if (believerName.equalsIgnoreCase(offlinePlayer.getName()))
			{
				return believerId;
			}
		}
		return null;
	}
}
