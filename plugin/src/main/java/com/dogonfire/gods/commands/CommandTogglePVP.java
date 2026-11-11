package com.dogonfire.gods.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

public class CommandTogglePVP extends GodsCommand
{
	CommandTogglePVP(GodsContext ctx)
	{
		super(ctx, "pvp");
		this.permission = "gods.priest.pvp";
		this.priestOnly = true;
	}

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
		if (!ctx.priestService().isPriest(player.getUniqueId()))
		{
			send(player, PRIEST_ONLY);
			return;
		}
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		boolean pvp = ctx.godService().getGodPvP(godName);
		if (pvp)
		{
			send(sender, "command.pvp.disabled");
			ctx.godService().setGodPvP(godName, false);
		}
		else
		{
			send(sender, "command.pvp.enabled");
			ctx.godService().setGodPvP(godName, true);
		}
	}

}
