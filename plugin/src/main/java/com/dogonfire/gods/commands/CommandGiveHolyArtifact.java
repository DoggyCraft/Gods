package com.dogonfire.gods.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

public class CommandGiveHolyArtifact extends GodsCommand
{
	CommandGiveHolyArtifact(GodsContext ctx)
	{
		super(ctx, "holyartifact");
		this.permission = "gods.holyartifact";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!ctx.settings().holyArtifacts().enabled())
		{
			send(sender, "command.disabled.holy-artifacts");
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
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		if (godName == null)
		{
			send(sender, NO_GOD);
			return;
		}
		if (ctx.holyArtifactService().hasHolyArtifact(player))
		{
			send(sender, "command.holyartifact.already");
			return;
		}
		ctx.blessingService().blessPlayerWithHolyArtifact(godName, player);
		send(sender, "command.holyartifact.done", Messages.god(godName), Messages.player(player.getName()));
	}
}
