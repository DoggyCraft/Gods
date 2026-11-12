package com.dogonfire.gods.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.model.DevotionLevel;

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
		requireRank(godName, this.ctx.settings().ranks().unlocks().holyArtifacts());
		DevotionLevel needed = this.ctx.settings().devotion().unlocks().holyArtifacts();
		if (!this.ctx.devotionService().allows(player.getUniqueId(), needed))
		{
			throw new CommandException("command.holyartifact.devotion-locked", Messages.god(godName), Messages.text("level", this.ctx.devotionService().levelName(godName, needed)));
		}
		if (this.ctx.holyArtifactService().hasHolyArtifact(player))
		{
			throw new CommandException("command.holyartifact.already");
		}
		if (!this.ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			throw new CommandException("command.holyartifact.world-disabled");
		}
		if (this.ctx.godService().getDivineForceForGod(godName) == null)
		{
			throw new CommandException("command.holyartifact.failed", Messages.god(godName));
		}
		if (!this.ctx.blessingService().blessPlayerWithHolyArtifact(godName, player))
		{
			throw new CommandException("command.cancelled");
		}
		send(sender, "command.holyartifact.done", Messages.god(godName), Messages.player(player.getName()));
	}
}
