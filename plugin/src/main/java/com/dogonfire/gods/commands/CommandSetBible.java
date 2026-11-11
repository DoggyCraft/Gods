package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

public class CommandSetBible extends GodsCommand
{
	CommandSetBible(GodsContext ctx)
	{
		super(ctx, "setbible");
		this.permission = "gods.priest.setbible";
		this.description = "Set the book in your hand as the Holy Book of your religion";
		this.priestOnly = true;
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!ctx.settings().bibles().enabled())
		{
			sender.sendMessage(ChatColor.RED + "Bibles are not enabled on this server");
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
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		if (godName == null)
		{
			sender.sendMessage(ChatColor.RED + "You do not believe in a God");
			return;
		}
		if (!ctx.priestService().isPriestForGod(player.getUniqueId(), godName))
		{
			sender.sendMessage(ChatColor.RED + "Only your priest can set the Bible");
			return;
		}
		if (!ctx.holyBookService().setBible(godName, player.getName()))
		{
			sender.sendMessage(ChatColor.RED + "You cannot use that as the Bible for " + ChatColor.GOLD + godName);
			return;
		}
		sender.sendMessage(ChatColor.AQUA + "You set " + ChatColor.GOLD + ctx.holyBookService().getBibleTitle(godName) + ChatColor.AQUA + " as your holy scripture!");
	}
}
