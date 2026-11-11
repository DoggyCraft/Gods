package com.dogonfire.gods.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

/** /g chat: switches between public chat and chat with the believers of the player's god */
public final class ChatCommand extends SubCommand
{
	public ChatCommand(GodsContext ctx)
	{
		super(ctx, "chat", "gods.chat");
		believerOnly();
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		Player player = (Player) sender;
		String godName = believersGod(player);
		if (this.ctx.believerService().getReligionChat(player.getUniqueId()))
		{
			this.ctx.believerService().setReligionChat(player.getUniqueId(), false);
			send(sender, "command.chat.public");
		}
		else
		{
			this.ctx.believerService().setReligionChat(player.getUniqueId(), true);
			send(sender, "command.chat.religion", Messages.god(godName));
		}
	}
}
