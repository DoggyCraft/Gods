package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.managers.LanguageManager;

public class CommandInvite extends GodsCommand
{
	CommandInvite(GodsContext ctx)
	{
		super(ctx, "invite");
		this.permission = "gods.priest.invite";
		this.parameters = "<player>";
		this.description = "Invite a player to your religion";
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
		if (!ctx.godManager().isPriest(player.getUniqueId()))
		{
			player.sendMessage(ChatColor.RED + "Only priests can invite players");
			return;
		}
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());

		if (godName == null)
		{
			player.sendMessage(ChatColor.RED + "You dont believe in a god");
			return;
		}
		if (args.length < 2)
		{
			sendUsage(sender);
			return;
		}
		String playerName = args[1];
		Player invitedPlayer = ctx.plugin().getServer().getPlayerExact(playerName);
		if (invitedPlayer == null)
		{
			player.sendMessage(ChatColor.RED + "There is no player with the name '" + ChatColor.YELLOW + playerName + ChatColor.RED + "' online.");
			return;
		}
		String invitedPlayerGod = ctx.believerService().getGodForBeliever(invitedPlayer.getUniqueId());
		if ((invitedPlayerGod != null) && (invitedPlayerGod.equals(godName)))
		{
			player.sendMessage(ChatColor.YELLOW + playerName + ChatColor.RED + " already believes in '" + ChatColor.GOLD + godName + ChatColor.RED + "!");
			return;
		}
		ctx.believerService().setInvitation(invitedPlayer.getUniqueId(), godName);
		ctx.plugin().log(godName + " invited to " + invitedPlayer.getName() + " to join the religion");
		ctx.languageManager().setPlayerName(invitedPlayer.getName());
		ctx.godManager().GodSay(godName, invitedPlayer, LanguageManager.LANGUAGESTRING.GodToPlayerInvite, 10);
		ctx.plugin().sendInfo(invitedPlayer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverQuestionHelp, ChatColor.AQUA, "/gods yes or /gods no", "/gods yes or /gods no", 40);
		player.sendMessage(ChatColor.AQUA + "You invited " + ChatColor.YELLOW + playerName + ChatColor.AQUA + " to join " + ChatColor.GOLD + godName + ChatColor.AQUA + "!");
	}

}
