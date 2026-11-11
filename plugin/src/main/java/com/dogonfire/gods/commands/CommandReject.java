package com.dogonfire.gods.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

public class CommandReject extends GodsCommand
{
	CommandReject(GodsContext ctx)
	{
		super(ctx, "no");
		this.permission = "gods.reject";
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

		if (ctx.believerService().getGodForBeliever(((Player) sender).getUniqueId()) == null)
		{
			send(sender, NO_GOD);
			return;
		}

		Player player = (Player) sender;
		ctx.prayerService().believerReject(player.getUniqueId());
	}

}
