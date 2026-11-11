package com.dogonfire.gods.commands;

import org.bukkit.command.CommandSender;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

public class CommandReload extends GodsCommand
{
	CommandReload(GodsContext ctx)
	{
		super(ctx, "reload");
		this.permission = "gods.reload";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!hasPermission(sender))
		{
			send(sender, NO_PERMISSION);
			return;
		}

		if (!ctx.plugin().reload())
		{
			send(sender, "command.reload.failed");
			return;
		}
		send(sender, "command.reload.done", Messages.text("plugin", ctx.plugin().getDescription().getFullName()));
		ctx.plugin().log(sender.getName() + " /gods reload");
	}
}
