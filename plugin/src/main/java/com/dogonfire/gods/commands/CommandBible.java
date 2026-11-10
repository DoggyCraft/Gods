package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

public class CommandBible extends GodsCommand
{
	CommandBible(GodsContext ctx)
	{
		super(ctx, "bible");
		this.permission = "gods.priest.bible";
		this.description = "Produce a copy of the Holy Book of your religion";
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
			sender.sendMessage(ChatColor.RED + "You do not have permission for that");
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

		if (!ctx.godManager().isPriest(player.getUniqueId()))
		{
			sender.sendMessage(ChatColor.RED + "Only your priest can produce the Holy Book");
			return;
		}
		if (!ctx.holyBookManager().giveBible(godName, player.getName()))
		{
			sender.sendMessage(ChatColor.RED + "Could not produce a Holy Book for " + godName);
			return;
		}
		sender.sendMessage(ChatColor.AQUA + "You produced a copy of " + ChatColor.GOLD + ctx.holyBookManager().getBibleTitle(godName) + ChatColor.AQUA + "!");
	}
}
