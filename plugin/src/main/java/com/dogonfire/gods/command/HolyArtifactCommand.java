package com.dogonfire.gods.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

/** /g holyartifact: the player's god gives them a holy artifact, if they don't carry one */
public final class HolyArtifactCommand extends SubCommand
{
	public HolyArtifactCommand(GodsContext ctx)
	{
		super(ctx, "holyartifact", "gods.holyartifact");
		believerOnly();
	}

	@Override
	public String disabledMessage()
	{
		return this.ctx.settings().holyArtifacts().enabled() ? null : "command.disabled.holy-artifacts";
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		Player player = (Player) sender;
		String godName = believersGod(player);
		if (this.ctx.holyArtifactService().hasHolyArtifact(player))
		{
			throw new CommandException("command.holyartifact.already");
		}
		this.ctx.blessingService().blessPlayerWithHolyArtifact(godName, player);
		send(sender, "command.holyartifact.done", Messages.god(godName), Messages.player(player.getName()));
	}
}
