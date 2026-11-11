package com.dogonfire.gods.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

/** /g no: rejects what the player's god asked them */
public final class RejectCommand extends SubCommand
{
	public RejectCommand(GodsContext ctx)
	{
		super(ctx, "no", "gods.reject");
		believerOnly();
	}

	@Override
	public void execute(CommandSender sender, List<String> args)
	{
		this.ctx.prayerService().believerReject(((Player) sender).getUniqueId());
	}
}
