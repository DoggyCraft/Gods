package com.dogonfire.gods.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

public class CommandSetDescription extends GodsCommand
{
	CommandSetDescription(GodsContext ctx)
	{
		super(ctx, "desc");
		this.permission = "gods.priest.description";
		this.priestOnly = true;
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
		if (!ctx.priestService().isPriest(player.getUniqueId()))
		{
			send(sender, "command.desc.not-priest");
			return;
		}
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		if (args.length < 2)
		{
			sendUsage(sender);
			return;
		}
		String description = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
		ctx.godService().setGodDescription(godName, description);
		send(sender, "command.desc.done", Messages.text("description", ctx.godService().getGodDescription(godName)));
	}
}
