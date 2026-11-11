package com.dogonfire.gods.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

public class CommandSetNeutralLand extends GodsCommand
{
	CommandSetNeutralLand(GodsContext ctx)
	{
		super(ctx, "setsafe");
		this.permission = "gods.setsafe";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!ctx.settings().holyLand().enabled())
		{
			send(sender, "command.disabled.holy-land");
			return;
		}
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
		if (ctx.holyLandService().isNeutralLandLocation(player.getLocation()))
		{
			ctx.holyLandService().clearNeutralLand(player.getLocation());
			ctx.titles().send(player, 1*20, 3*20, 1*20, ctx.messages().legacy("holy-land.title.wilderness"), ctx.messages().legacy("holy-land.title.wilderness-subtitle"));
			send(sender, "command.setsafe.cleared");
		}
		else
		{
			ctx.holyLandService().setNeutralLand(player.getLocation());
			ctx.titles().send(player, 1*20, 3*20, 1*20, ctx.messages().legacy("holy-land.title.neutral"), ctx.messages().legacy("holy-land.title.neutral-subtitle"));
			send(sender, "command.setsafe.set");
		}
	}
}
