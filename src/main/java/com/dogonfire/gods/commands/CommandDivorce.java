package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.MarriageManager;


public class CommandDivorce extends GodsCommand
{
	protected CommandDivorce()
	{
		super("divorce");
		this.permission = "gods.divorce";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!GodsConfiguration.instance().isMarriageEnabled())
		{
			sender.sendMessage(ChatColor.RED + "Marriage is not enabled on this server");
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
		String thisGodName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());

		if (thisGodName == null)
		{
			player.sendMessage(ChatColor.RED + "You do not believe in a God");
			return;
		}

		String partnerName = MarriageManager.instance().getPartnerName(player.getUniqueId());

		if (partnerName == null)
		{
			player.sendMessage(ChatColor.RED + "You are not married, bozo!");
			return;
		}

		MarriageManager.instance().divorce(player.getUniqueId());

		player.sendMessage(ChatColor.AQUA + "You divorced " + ChatColor.WHITE + partnerName + "!");
	}
}
