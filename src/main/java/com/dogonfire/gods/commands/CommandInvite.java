package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.LanguageManager;

public class CommandInvite extends GodsCommand
{
	protected CommandInvite()
	{
		super("invite");
		this.permission = "gods.priest.invite";
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
			player.sendMessage(ChatColor.RED + "Only priests can invite players");
			return;
		}
		String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());

		if (godName == null)
		{
			player.sendMessage(ChatColor.RED + "You dont believe in a god");
			return;
		}
		String playerName = args[1];
		Player invitedPlayer = Gods.instance().getServer().getPlayer(playerName);
		if (invitedPlayer == null)
		{
			player.sendMessage(ChatColor.RED + "There is no player with the name '" + ChatColor.YELLOW + playerName + ChatColor.RED + " online.");
			return;
		}
		String invitedPlayerGod = BelieverManager.instance().getGodForBeliever(invitedPlayer.getUniqueId());
		if ((invitedPlayerGod != null) && (invitedPlayerGod.equals(godName)))
		{
			player.sendMessage(ChatColor.YELLOW + playerName + ChatColor.RED + " already believes in '" + ChatColor.GOLD + godName + ChatColor.RED + "!");
			return;
		}
		BelieverManager.instance().setInvitation(invitedPlayer.getUniqueId(), godName);
		Gods.instance().log(godName + " invited to " + invitedPlayer.getName() + " to join the religion");
		LanguageManager.instance().setPlayerName(invitedPlayer.getName());
		GodManager.instance().GodSay(godName, invitedPlayer, LanguageManager.LANGUAGESTRING.GodToPlayerInvite, 10);
		Gods.instance().sendInfo(invitedPlayer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverQuestionHelp, ChatColor.AQUA, "/gods yes or /gods no", "/gods yes or /gods no", 40);
		player.sendMessage(ChatColor.AQUA + "You invited " + ChatColor.YELLOW + playerName + ChatColor.AQUA + " to join " + ChatColor.GOLD + godName + ChatColor.AQUA + "!");
	}

}
