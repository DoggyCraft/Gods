package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.message.Messages;

public class CommandMarry extends GodsCommand
{
	CommandMarry(GodsContext ctx)
	{
		super(ctx, "marry");
		this.permission = "gods.marry";
		this.parameters = "<name>";
		this.description = "Ask another player to marry you";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!ctx.settings().marriage().enabled())
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
		String thisGodName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		if (thisGodName == null)
		{
			player.sendMessage(ChatColor.RED + "You do not believe in a God");
			return;
		}

		if (args.length < 2)
		{
			player.sendMessage(ChatColor.RED + "Marry who!?");
			return;
		}

		String otherPlayerName = args[1];
		Player otherPlayer = ctx.plugin().getServer().getPlayerExact(otherPlayerName);
		if (otherPlayer == null)
		{
			player.sendMessage(ChatColor.RED + "There is no player with the name '" + ChatColor.WHITE + otherPlayerName + ChatColor.RED + "' online.");
			return;
		}

		if (otherPlayer.getUniqueId().equals(player.getUniqueId()))
		{
			player.sendMessage(ChatColor.RED + "Marry yourself!? Think again...");
			return;
		}

		String otherGodName = ctx.believerService().getGodForBeliever(otherPlayer.getUniqueId());

		if (otherGodName == null)
		{
			player.sendMessage(ChatColor.WHITE + otherPlayerName + ChatColor.RED + " does not believe in a God");
			return;
		}

		if (!thisGodName.equals(otherGodName))
		{
			player.sendMessage(ChatColor.WHITE + otherPlayerName + ChatColor.RED + " does not believe in " + ChatColor.GOLD + thisGodName);
			return;
		}

		String partnerName = ctx.marriageService().getPartnerName(otherPlayer.getUniqueId());
		if (partnerName != null)
		{
			player.sendMessage(ChatColor.WHITE + otherPlayerName + ChatColor.RED + " is already married to " + ChatColor.WHITE + partnerName + "!");
			return;
		}
		partnerName = ctx.marriageService().getPartnerName(player.getUniqueId());
		if (partnerName != null)
		{
			player.sendMessage(ChatColor.RED + "You are already married to " + ChatColor.WHITE + partnerName + "!");
			return;
		}

		ctx.marriageService().proposeMarriage(player.getUniqueId(), otherPlayer.getUniqueId());

		ctx.godSpeech().sayWithQuestion(thisGodName, otherPlayer, Voice.BELIEVER_MARRIAGE_PROPOSAL, 1, Messages.player(player.getName()));

		player.sendMessage(ChatColor.AQUA + "You proposed " + ChatColor.WHITE + otherPlayerName + ChatColor.AQUA + " to marry you in the name of " + ChatColor.GOLD + thisGodName + "!");
	}
}
