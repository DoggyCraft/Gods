package com.dogonfire.gods.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

/** /g setpriest &lt;god&gt; &lt;player&gt;: makes an online player a priest of a god (admins) */
public final class SetPriestCommand extends SubCommand
{
	public SetPriestCommand(GodsContext ctx)
	{
		super(ctx, "setpriest", "gods.setpriest");
		args(2, ANY);
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		String godName = this.ctx.godService().formatGodName(args.get(0));
		if (!this.ctx.godService().godExist(godName))
		{
			throw new CommandException("command.setpriest.no-god", Messages.god(godName));
		}
		Player priest = onlinePlayer(args.get(1), "command.setpriest.no-player");
		boolean believer = godName.equals(godOf(priest));
		if (!this.ctx.priestService().assignPriest(godName, priest.getUniqueId()))
		{
			// A believer who isn't made priest was stopped by another plugin
			throw believer ? new CommandException("command.cancelled") : new CommandException("command.setpriest.failed", Messages.player(priest.getName()), Messages.god(godName));
		}
		send(sender, "command.setpriest.done", Messages.player(priest.getName()), Messages.god(godName));
	}

	@Override
	public List<String> tabComplete(CommandSender sender, List<String> args)
	{
		return switch (args.size())
		{
			case 1 -> godNames();
			case 2 -> onlinePlayerNames(sender);
			default -> List.of();
		};
	}
}
