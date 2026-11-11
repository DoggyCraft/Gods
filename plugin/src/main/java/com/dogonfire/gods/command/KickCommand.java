package com.dogonfire.gods.command;

import java.util.List;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.api.event.BelieverLeaveGodEvent;
import com.dogonfire.gods.message.Messages;

/** /g kick &lt;player&gt;: removes a believer, online or not, from the priest's religion */
public final class KickCommand extends SubCommand
{
	public KickCommand(GodsContext ctx)
	{
		super(ctx, "kick", "gods.priest.kick");
		priestOnly("command.kick.not-priest");
		args(1, ANY);
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		Player player = (Player) sender;
		String godName = believersGod(player);
		String believerName = args.getFirst();
		UUID believerId = knownBeliever(godName, believerName);
		if (believerId == null)
		{
			throw new CommandException("command.kick.no-believer", Messages.player(believerName));
		}
		if (believerId.equals(player.getUniqueId()))
		{
			throw new CommandException("command.kick.yourself");
		}
		this.ctx.believerService().removeBeliever(godName, believerId, BelieverLeaveGodEvent.Reason.KICKED);
		send(sender, "command.kick.done", Messages.player(believerName));
		Player believer = this.ctx.plugin().getServer().getPlayer(believerId);
		if (believer != null)
		{
			send(believer, "command.kick.kicked", Messages.god(godName));
		}
		this.ctx.plugin().log(sender.getName() + " /gods kick " + believerName);
	}

	@Override
	public List<String> tabComplete(CommandSender sender, List<String> args)
	{
		return args.size() == 1 ? believerNames(godOf((Player) sender)) : List.of();
	}
}
