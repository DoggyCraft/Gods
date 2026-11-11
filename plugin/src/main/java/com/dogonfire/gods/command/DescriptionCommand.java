package com.dogonfire.gods.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

/** /g desc &lt;text&gt;: sets the description of the priest's religion */
public final class DescriptionCommand extends SubCommand
{
	public DescriptionCommand(GodsContext ctx)
	{
		super(ctx, "desc", "gods.priest.description");
		priestOnly("command.desc.not-priest");
		args(1, ANY);
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		String godName = believersGod((Player) sender);
		this.ctx.godService().setGodDescription(godName, String.join(" ", args));
		send(sender, "command.desc.done", Messages.text("description", this.ctx.godService().getGodDescription(godName)));
	}
}
