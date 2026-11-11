package com.dogonfire.gods.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

public class CommandLeaveGod extends GodsCommand
{
	CommandLeaveGod(GodsContext ctx)
	{
		super(ctx, "leave");
		this.permission = "gods.leave";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!hasPermission(sender))
		{
			send(sender, NO_PERMISSION);
			return;
		}
		
		if (sender instanceof Player == false)
		{
			send(sender, PLAYER_ONLY);
			return;
		}
		
		Player player = (Player) sender;
		
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		
		if (ctx.prayerService().believerLeaveGod(player.getUniqueId()))
		{
			send(sender, "command.leave.done", Messages.god(godName));
		}
		else
		{
			send(sender, "command.leave.no-god");
		}
	}
}
