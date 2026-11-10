package com.dogonfire.gods.commands;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.MarriageManager;

public class CommandLove extends GodsCommand
{
	protected CommandLove()
	{
		super("love");
		this.permission = "gods.love";
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
		UUID partnerId = MarriageManager.instance().getPartnerId(player.getUniqueId());
		if (partnerId == null)
		{
			player.sendMessage(ChatColor.RED + "You are not married, bozo!");
			return;
		}
		Player partner = Gods.instance().getServer().getPlayer(partnerId);
		if (partner == null)
		{
			player.sendMessage(ChatColor.WHITE + Gods.instance().getServer().getOfflinePlayer(partnerId).getName() + ChatColor.RED + " is not online!");
			return;
		}
		MarriageManager.instance().love(player.getUniqueId());
		player.sendMessage(ChatColor.AQUA + "You love " + ChatColor.WHITE + Gods.instance().getServer().getPlayer(partnerId).getDisplayName() + "!");
	}

}