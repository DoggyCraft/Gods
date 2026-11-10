package com.dogonfire.gods.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.LanguageManager;

public class CommandToggleWar extends GodsCommand
{
	protected CommandToggleWar()
	{
		super("war");
		this.permission = "gods.priest.war";
		this.parameters = "<god>";
		this.description = "Toggle war with another religion";
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
		if (args.length != 2)
		{
			sender.sendMessage(ChatColor.RED + "You need to specify a God which you want to go to WAR with! " + ChatColor.YELLOW + "/gods war <godname>");
			return;
		}
		Player player = (Player) sender;
		if (!GodManager.instance().isPriest(player.getUniqueId()))
		{
			player.sendMessage(stringPreistOnly);
			return;
		}
		String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());
		String enemyGodName = GodManager.instance().formatGodName(args[1]);
		if (!GodManager.instance().godExist(args[1]))
		{
			player.sendMessage(ChatColor.RED + "There is no God with the name " + ChatColor.GOLD + args[1]);
			return;
		}
		List<String> alliances = GodManager.instance().getAllianceRelations(godName);
		if (alliances.contains(enemyGodName))
		{
			player.sendMessage(ChatColor.RED + "You are ALLIED with " + ChatColor.GOLD + args[1] + ChatColor.RED + "!");
			return;
		}
		if (GodManager.instance().toggleWarRelationForGod(godName, enemyGodName))
		{
			LanguageManager.instance().setPlayerName(godName);
			GodManager.instance().godSayToBelievers(enemyGodName, LanguageManager.LANGUAGESTRING.GodToBelieversWar, 10);

			LanguageManager.instance().setPlayerName(enemyGodName);
			GodManager.instance().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversWar, 10);
		}
		else
		{
			LanguageManager.instance().setPlayerName(godName);
			GodManager.instance().godSayToBelievers(enemyGodName, LanguageManager.LANGUAGESTRING.GodToBelieversWarCancelled, 10);

			LanguageManager.instance().setPlayerName(enemyGodName);
			GodManager.instance().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversWarCancelled, 10);
		}

	}
}
