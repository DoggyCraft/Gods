package com.dogonfire.gods.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

/** /g setsafe: makes the 3x3 chunks around the player neutral land, or wilderness again if the player's chunk is neutral */
public final class SetSafeCommand extends SubCommand
{
	public SetSafeCommand(GodsContext ctx)
	{
		super(ctx, "setsafe", "gods.setsafe");
		playerOnly();
	}

	@Override
	public String disabledMessage()
	{
		return this.ctx.settings().holyLand().enabled() ? null : "command.disabled.holy-land";
	}

	@Override
	public void execute(CommandSender sender, List<String> args)
	{
		Player player = (Player) sender;
		if (this.ctx.holyLandService().isNeutralLandLocation(player.getLocation()))
		{
			this.ctx.holyLandService().clearNeutralLand(player.getLocation());
			this.ctx.titles().send(player, 1 * 20, 3 * 20, 1 * 20, this.ctx.messages().legacy("holy-land.title.wilderness"), this.ctx.messages().legacy("holy-land.title.wilderness-subtitle"));
			send(sender, "command.setsafe.cleared");
		}
		else
		{
			this.ctx.holyLandService().setNeutralLand(player.getLocation());
			this.ctx.titles().send(player, 1 * 20, 3 * 20, 1 * 20, this.ctx.messages().legacy("holy-land.title.neutral"), this.ctx.messages().legacy("holy-land.title.neutral-subtitle"));
			send(sender, "command.setsafe.set");
		}
	}
}
