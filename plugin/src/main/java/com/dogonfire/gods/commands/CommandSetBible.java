package com.dogonfire.gods.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

public class CommandSetBible extends GodsCommand
{
	CommandSetBible(GodsContext ctx)
	{
		super(ctx, "setbible");
		this.permission = "gods.priest.setbible";
		this.priestOnly = true;
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!ctx.settings().bibles().enabled())
		{
			send(sender, "command.disabled.bibles");
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
			send(sender, "command.not-believer");
			return;
		}
		if (!ctx.priestService().isPriestForGod(player.getUniqueId(), godName))
		{
			send(sender, "command.setbible.not-priest");
			return;
		}
		if (!ctx.holyBookService().setBible(godName, player.getName()))
		{
			send(sender, "command.setbible.failed", Messages.god(godName));
			return;
		}
		send(sender, "command.setbible.done", Messages.text("title", ctx.holyBookService().getBibleTitle(godName)));
	}
}
