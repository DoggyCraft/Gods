package com.dogonfire.gods.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

public class CommandDivorce extends GodsCommand
{
	CommandDivorce(GodsContext ctx)
	{
		super(ctx, "divorce");
		this.permission = "gods.divorce";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!ctx.settings().marriage().enabled())
		{
			send(sender, "command.disabled.marriage");
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
		String thisGodName = ctx.believerService().getGodForBeliever(player.getUniqueId());

		if (thisGodName == null)
		{
			send(player, "command.not-believer");
			return;
		}

		String partnerName = ctx.marriageService().getPartnerName(player.getUniqueId());

		if (partnerName == null)
		{
			send(player, "command.not-married");
			return;
		}

		ctx.marriageService().divorce(player.getUniqueId());

		send(player, "command.divorce.done", Messages.player(partnerName));
	}
}
