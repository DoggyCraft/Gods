package com.dogonfire.gods.commands;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.MarriageManager;

public class CommandMarriages extends GodsCommand
{
	protected CommandMarriages()
	{
		super("marriages");
		this.permission = "gods.marriages";
		this.description = "List the most loving married couples";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!GodsConfiguration.instance().isMarriageEnabled())
		{
			sender.sendMessage(ChatColor.RED + "Marriages are not enabled on this server");
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
		List<MarriageManager.MarriedCouple> couples = MarriageManager.instance().getMarriedCouples();
		if (couples.size() == 0)
		{
			sender.sendMessage("There are no married couples yet!");
			return;
		}
		sender.sendMessage(ChatColor.YELLOW + "--------- The Married Couples in " + GodsConfiguration.instance().getServerName() + " ---------");
		int l = couples.size();

		List<MarriageManager.MarriedCouple> couplesList = couples;
		if (l > 15)
		{
			couplesList = couplesList.subList(0, 15);
		}

		int n = 1;
		boolean playerShown = false;
		Date thisDate = new Date();

		for (MarriageManager.MarriedCouple couple : couplesList)
		{
			long minutes = (thisDate.getTime() - couple.lastLove.getTime()) / 60000L;
			long hours = (thisDate.getTime() - couple.lastLove.getTime()) / 3600000L;
			long days = (thisDate.getTime() - couple.lastLove.getTime()) / 86400000L;

			String date = "";
			if (days > 0L)
			{
				date = days + " days ago";
			}
			else if (hours > 0L)
			{
				date = hours + " hours ago";
			}
			else
			{
				date = minutes + " min ago";
			}

			String player1Name = Gods.instance().getServer().getOfflinePlayer(couple.player1Id).getName();
			String player2Name = Gods.instance().getServer().getOfflinePlayer(couple.player2Id).getName();

			if (sender != null)
			{

				if ((couple.player1Id.equals(player.getUniqueId())) || (couple.player2Id.equals(player.getUniqueId())))
				{
					playerShown = true;
					sender.sendMessage("" + ChatColor.GOLD + n + " - " + StringUtils.rightPad(new StringBuilder(player1Name).append(" & ").append(player2Name).append(" (").append(couple.godName).append(")").toString(), 30) + ChatColor.AQUA
							+ StringUtils.rightPad(new StringBuilder().append(" Loved ").append(ChatColor.GOLD).append(date).toString(), 18));
				}
				else
				{
					sender.sendMessage("" + ChatColor.WHITE + n + " - " + StringUtils.rightPad(new StringBuilder(player1Name).append(" & ").append(player2Name).append(" (").append(couple.godName).append(")").toString(), 30) + ChatColor.AQUA
							+ StringUtils.rightPad(new StringBuilder().append(" Loved ").append(ChatColor.GOLD).append(date).toString(), 18));
				}
			}
			else
			{
				Gods.instance().log(StringUtils.rightPad(new StringBuilder(player1Name).append(" & ").append(player2Name).append(" (").append(couple.godName).append(")").toString(), 30) + ChatColor.AQUA
						+ StringUtils.rightPad(new StringBuilder().append(" Loved ").append(ChatColor.GOLD).append(date).toString(), 18));
			}
			n++;
		}

		n = 1;

		if (!playerShown)
		{
			for (MarriageManager.MarriedCouple couple : couples)
			{
				String player1Name = Gods.instance().getServer().getOfflinePlayer(couple.player1Id).getName();
				String player2Name = Gods.instance().getServer().getOfflinePlayer(couple.player2Id).getName();

				if ((couple.player1Id.equals(player.getUniqueId())) || (couple.player2Id.equals(player.getUniqueId())))
				{
					sender.sendMessage("" + ChatColor.GOLD + n + " - " + StringUtils.rightPad(new StringBuilder(player1Name).append(" & ").append(player2Name).append(" (").append(couple.godName).append(")").toString(), 40)
							+ StringUtils.rightPad(new StringBuilder().append(" Loved ").append(couple.lastLove).toString(), 18));
				}
				n++;
			}
		}
	}
}
