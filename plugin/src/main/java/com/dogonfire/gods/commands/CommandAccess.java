package com.dogonfire.gods.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

public class CommandAccess extends GodsCommand
{
	CommandAccess(GodsContext ctx)
	{
		super(ctx, "access");
		this.permission = "gods.priest.access";
		this.priestOnly = true;
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!hasPermission(sender))
		{
			send(sender, "command.not-allowed");
			return;
		}

		if (sender instanceof Player == false)
		{
			send(sender, PLAYER_ONLY);
			return;
		}
		
		Player player = (Player) sender;
		
		if (!ctx.priestService().isPriest(player.getUniqueId()))
		{
			send(player, "command.access.not-priest");
			return;
		}
		
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());

		if (godName == null || !ctx.godService().godExist(godName))
		{
			send(player, "command.access.no-god");
			return;
		}

		if (args.length < 2)
		{
			sendUsage(sender);
			return;
		}

		String access = args[1];
		
		if (access.equalsIgnoreCase("open"))
		{
			ctx.godService().setPrivateAccess(godName, false);
			ctx.plugin().log(player.getName() + " /gods open");
			send(player, "command.access.opened");
		}
		else if (access.equalsIgnoreCase("close"))
		{
			ctx.godService().setPrivateAccess(godName, true);
			ctx.plugin().log(player.getName() + " /gods close");
			send(player, "command.access.closed");
		}
		else
		{
			send(player, "command.access.invalid");
		}
	}
}
