package com.dogonfire.gods.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.managers.LanguageManager;

public class CommandToggleWar extends GodsCommand
{
	CommandToggleWar(GodsContext ctx)
	{
		super(ctx, "war");
		this.permission = "gods.priest.war";
		this.parameters = "<god>";
		this.description = "Toggle war with another religion";
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
		if (args.length != 2)
		{
			sender.sendMessage(ChatColor.RED + "You need to specify a God which you want to go to WAR with! " + ChatColor.YELLOW + "/gods war <godname>");
			return;
		}
		Player player = (Player) sender;
		if (!ctx.godManager().isPriest(player.getUniqueId()))
		{
			player.sendMessage(stringPreistOnly);
			return;
		}
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		String enemyGodName = ctx.godManager().formatGodName(args[1]);
		if (!ctx.godManager().godExist(args[1]))
		{
			player.sendMessage(ChatColor.RED + "There is no God with the name " + ChatColor.GOLD + args[1]);
			return;
		}
		if (enemyGodName.equals(godName))
		{
			player.sendMessage(ChatColor.RED + "You cannot go to war with your own God!");
			return;
		}
		List<String> alliances = ctx.godManager().getAllianceRelations(godName);
		if (alliances.contains(enemyGodName))
		{
			player.sendMessage(ChatColor.RED + "You are ALLIED with " + ChatColor.GOLD + args[1] + ChatColor.RED + "!");
			return;
		}
		if (ctx.godManager().toggleWarRelationForGod(godName, enemyGodName))
		{
			ctx.languageManager().setPlayerName(godName);
			ctx.godSpeech().sayToBelievers(enemyGodName, LanguageManager.LANGUAGESTRING.GodToBelieversWar, 10);

			ctx.languageManager().setPlayerName(enemyGodName);
			ctx.godSpeech().sayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversWar, 10);
		}
		else
		{
			ctx.languageManager().setPlayerName(godName);
			ctx.godSpeech().sayToBelievers(enemyGodName, LanguageManager.LANGUAGESTRING.GodToBelieversWarCancelled, 10);

			ctx.languageManager().setPlayerName(enemyGodName);
			ctx.godSpeech().sayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversWarCancelled, 10);
		}

	}
}
