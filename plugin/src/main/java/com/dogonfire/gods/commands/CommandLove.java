package com.dogonfire.gods.commands;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

public class CommandLove extends GodsCommand
{
	CommandLove(GodsContext ctx)
	{
		super(ctx, "love");
		this.permission = "gods.love";
		this.description = "Show your love to your partner";
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
		UUID partnerId = ctx.marriageService().getPartnerId(player.getUniqueId());
		if (partnerId == null)
		{
			player.sendMessage(ChatColor.RED + "You are not married, bozo!");
			return;
		}
		Player partner = ctx.plugin().getServer().getPlayer(partnerId);
		if (partner == null)
		{
			player.sendMessage(ChatColor.WHITE + ctx.plugin().getServer().getOfflinePlayer(partnerId).getName() + ChatColor.RED + " is not online!");
			return;
		}
		ctx.marriageService().love(player.getUniqueId());
		player.sendMessage(ChatColor.AQUA + "You love " + ChatColor.WHITE + ctx.plugin().getServer().getPlayer(partnerId).getDisplayName() + "!");
	}

}
