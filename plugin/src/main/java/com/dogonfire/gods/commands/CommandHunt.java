package com.dogonfire.gods.commands;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

public class CommandHunt extends GodsCommand
{
	CommandHunt(GodsContext ctx)
	{
		super(ctx, "hunt");
		this.permission = "gods.hunt";
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
		Player player = (Player) sender;
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		if (godName == null)
		{
			send(sender, "command.not-believer");
			return;
		}
		Location pilgrimageLocation = ctx.questService().getQuestLocation(godName);
		if (player == null || player.isFlying())
		{
			send(sender, "command.hunt.flying");
			return;
		}
		if (pilgrimageLocation == null)
		{
			send(sender, "command.hunt.no-target");
			return;
		}
		if (!pilgrimageLocation.getWorld().getName().equals(player.getWorld().getName()))
		{
			ctx.plugin().logDebug("PilgrimageQuest for '" + player.getDisplayName() + "' is wrong world");
			return;
		}
		Vector vector = pilgrimageLocation.toVector().subtract(player.getLocation().toVector());
		ctx.godSpeech().sendInfo(player.getUniqueId(), "quest.target-range", 20, Messages.text("distance", (int) vector.length()));

	}

}
