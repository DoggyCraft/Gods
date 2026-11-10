package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.HolyBookManager;

public class CommandSetBible extends GodsCommand
{
	protected CommandSetBible()
	{
		super("setbible");
		this.permission = "gods.priest.setbible";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!GodsConfiguration.instance().isBiblesEnabled())
		{
			sender.sendMessage(ChatColor.RED + "Bibles are not enabled on this server");
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
		String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());
		if (godName == null)
		{
			sender.sendMessage(ChatColor.RED + "You do not believe in a God");
			return;
		}
		if (!GodManager.instance().isPriestForGod(player.getUniqueId(), godName))
		{
			sender.sendMessage(ChatColor.RED + "Only your priest can set the Bible");
			return;
		}
		if (!HolyBookManager.instance().setBible(godName, player.getName()))
		{
			sender.sendMessage(ChatColor.RED + "You cannot use that as the Bible for " + ChatColor.GOLD + godName);
			return;
		}
		sender.sendMessage(ChatColor.AQUA + "You set " + ChatColor.GOLD + HolyBookManager.instance().getBibleTitle(godName) + ChatColor.AQUA + " as your holy scripture!");
	}
}
