package com.dogonfire.gods.command;

import java.util.List;

import org.bukkit.command.CommandSender;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

/** /g reload: reads config.yml, the message files and the data files again */
public final class ReloadCommand extends SubCommand
{
	public ReloadCommand(GodsContext ctx)
	{
		super(ctx, "reload", "gods.reload");
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		if (!this.ctx.plugin().reload())
		{
			throw new CommandException("command.reload.failed");
		}
		send(sender, "command.reload.done", Messages.text("plugin", this.ctx.plugin().getDescription().getFullName()));
		this.ctx.plugin().log(sender.getName() + " /gods reload");
	}
}
