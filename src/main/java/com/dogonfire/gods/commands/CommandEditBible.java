package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.HolyBookManager;

public class CommandEditBible extends GodsCommand
{
	protected CommandEditBible()
	{
		super("editbible");
		this.permission = "gods.priest.editbible";
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

		if (!GodManager.instance().isPriest(player.getUniqueId()))
		{
			sender.sendMessage(ChatColor.RED + "Only your priest can edit your Holy Book");
			return;
		}
		if (!HolyBookManager.instance().giveEditBible(godName, player.getName()))
		{
			sender.sendMessage(ChatColor.RED + "Could not produce a editable bible for " + godName);
			return;
		}
		sender.sendMessage(ChatColor.AQUA + "You produced a copy of " + ChatColor.GOLD + HolyBookManager.instance().getBibleTitle(godName) + ChatColor.AQUA + "!");
		sender.sendMessage(ChatColor.AQUA + "After you have edited it, set this as your bible with " + ChatColor.WHITE + "/g setbible" + ChatColor.AQUA + "!");
	}

}
