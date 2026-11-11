package com.dogonfire.gods.command;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

/** /g hunt: how far the player is from the target of their god's quest */
public final class HuntCommand extends SubCommand
{
	public HuntCommand(GodsContext ctx)
	{
		super(ctx, "hunt", "gods.hunt");
		believerOnly();
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		Player player = (Player) sender;
		Location target = this.ctx.questService().getQuestLocation(believersGod(player));
		if (player.isFlying())
		{
			throw new CommandException("command.hunt.flying");
		}
		if (target == null)
		{
			throw new CommandException("command.hunt.no-target");
		}
		if (target.getWorld() == null || !target.getWorld().getName().equals(player.getWorld().getName()))
		{
			this.ctx.plugin().logDebug("PilgrimageQuest for '" + player.getDisplayName() + "' is wrong world");
			return;
		}
		int distance = (int) target.toVector().subtract(player.getLocation().toVector()).length();
		this.ctx.godSpeech().sendInfo(player.getUniqueId(), "quest.target-range", 20, Messages.text("distance", distance));
	}
}
