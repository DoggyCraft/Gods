package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

public class CommandDivorce extends GodsCommand
{
	CommandDivorce(GodsContext ctx)
	{
		super(ctx, "divorce");
		this.permission = "gods.divorce";
		this.description = "Divorce your partner";
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
		String thisGodName = ctx.believerManager().getGodForBeliever(player.getUniqueId());

		if (thisGodName == null)
		{
			player.sendMessage(ChatColor.RED + "You do not believe in a God");
			return;
		}

		String partnerName = ctx.marriageManager().getPartnerName(player.getUniqueId());

		if (partnerName == null)
		{
			player.sendMessage(ChatColor.RED + "You are not married, bozo!");
			return;
		}

		ctx.marriageManager().divorce(player.getUniqueId());

		player.sendMessage(ChatColor.AQUA + "You divorced " + ChatColor.WHITE + partnerName + "!");
	}
}
