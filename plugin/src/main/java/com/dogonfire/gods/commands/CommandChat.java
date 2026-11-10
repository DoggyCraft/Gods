package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

public class CommandChat extends GodsCommand
{
	CommandChat(GodsContext ctx)
	{
		super(ctx, "chat");
		this.permission = "gods.chat";
		this.description = "Chat only with believers within your religion";
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
		String godName = ctx.believerManager().getGodForBeliever(player.getUniqueId());

		if (godName == null)
		{
			sender.sendMessage(ChatColor.RED + "You do not believe in a God");
			return;
		}

		if (ctx.believerManager().getReligionChat(player.getUniqueId()))
		{
			ctx.believerManager().setReligionChat(player.getUniqueId(), false);
			sender.sendMessage(ChatColor.AQUA + "You are now chatting public");
		}
		else
		{
			ctx.believerManager().setReligionChat(player.getUniqueId(), true);
			sender.sendMessage(ChatColor.AQUA + "You are now only chatting with the believers of " + ChatColor.YELLOW + godName);
		}
	}

}
