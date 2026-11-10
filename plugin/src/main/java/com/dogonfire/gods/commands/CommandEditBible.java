package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.HolyBookManager;

public class CommandEditBible extends GodsCommand
{
	protected CommandEditBible()
	{
		super("editbible");
		this.permission = "gods.priest.editbible";
		this.description = "Get an editable copy of the Holy Book of your religion";
		this.priestOnly = true;
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!Gods.instance().settings().bibles().enabled())
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
