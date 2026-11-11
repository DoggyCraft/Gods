package com.dogonfire.gods.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

/** /g pvp: turns PvP in the priest's holy land on or off */
public final class PvpCommand extends SubCommand
{
	public PvpCommand(GodsContext ctx)
	{
		super(ctx, "pvp", "gods.priest.pvp");
		priestOnly();
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		String godName = believersGod((Player) sender);
		boolean pvp = !this.ctx.godService().getGodPvP(godName);
		send(sender, pvp ? "command.pvp.enabled" : "command.pvp.disabled");
		this.ctx.godService().setGodPvP(godName, pvp);
	}
}
