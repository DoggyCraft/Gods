package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.message.Messages;

public class CommandSetHome extends GodsCommand
{
	CommandSetHome(GodsContext ctx)
	{
		super(ctx, "sethome");
		this.permission = "gods.sethome";
		this.description = "Set the home of your religion";
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
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		if (godName == null)
		{
			sender.sendMessage(ChatColor.RED + "You do not believe in a God");
			return;
		}
		if ((ctx.settings().priests().onlyPriestsCanSetHome()) && (!ctx.priestService().isPriest(player.getUniqueId())))
		{
			sender.sendMessage(ChatColor.RED + "Only your priest can set the home for your religion");
			return;
		}
		if (ctx.settings().holyLand().enabled())
		{
			if (ctx.holyLandService().isNeutralLandLocation(player.getLocation()))
			{
				sender.sendMessage(ChatColor.RED + "You can only set religion home within your Holy Land");
				return;
			}
			String locationGod = ctx.holyLandService().getGodAtHolyLandLocation(player.getLocation());
			if ((locationGod == null) || (!locationGod.equals(godName)))
			{
				sender.sendMessage(ChatColor.RED + "You can only set religion home within your Holy Land");
				return;
			}
		}
		ctx.godService().setHomeForGod(godName, player.getLocation());
		ctx.godSpeech().sayToBelievers(godName, Voice.BELIEVERS_SET_HOME, 2, Messages.player(player.getName()));
	}
}
