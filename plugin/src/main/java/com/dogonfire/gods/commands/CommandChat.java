package com.dogonfire.gods.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

public class CommandChat extends GodsCommand
{
	CommandChat(GodsContext ctx)
	{
		super(ctx, "chat");
		this.permission = "gods.chat";
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

		if (ctx.believerService().getReligionChat(player.getUniqueId()))
		{
			ctx.believerService().setReligionChat(player.getUniqueId(), false);
			send(sender, "command.chat.public");
		}
		else
		{
			ctx.believerService().setReligionChat(player.getUniqueId(), true);
			send(sender, "command.chat.religion", Messages.god(godName));
		}
	}

}
