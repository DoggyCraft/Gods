package com.dogonfire.gods.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.model.GodRank;
import com.dogonfire.gods.service.RankRules;

/**
 * /g rank [god]: a god's rank (the player's own by default) and, for its believers (and the console), what its next
 * rank needs, condition by condition
 */
public final class RankCommand extends SubCommand
{
	public RankCommand(GodsContext ctx)
	{
		super(ctx, "rank", "gods.rank");
		args(0, 1);
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		String godName = args.size() == 1 ? this.ctx.godService().formatGodName(args.getFirst()) : null;
		if (godName == null && sender instanceof Player player)
		{
			godName = godOf(player);
		}
		if (godName == null)
		{
			throw new CommandException("command.rank.no-god");
		}
		if (!this.ctx.godService().godExist(godName))
		{
			throw new CommandException("command.rank.unknown-god");
		}
		GodRank rank = this.ctx.rankService().getRank(godName);
		send(sender, "command.rank.header", Messages.god(godName));
		send(sender, "command.rank.rank", Messages.god(godName), Messages.text("rank", this.ctx.rankService().getRankName(godName)),
				Messages.text("hours", RankRules.hoursHeld(this.ctx.rankService().getRankSince(godName))));
		if (sender instanceof Player player && !godName.equals(godOf(player)))
		{
			return;
		}
		GodRank next = rank.next();
		if (next == null)
		{
			send(sender, "command.rank.top", Messages.god(godName));
			return;
		}
		send(sender, "command.rank.next", Messages.text("rank", this.ctx.rankService().rankName(godName, next)));
		for (String line : RankLines.lines(this.ctx, this.ctx.rankService().progress(godName, next), "command.rank.met", "command.rank.unmet"))
		{
			sender.sendMessage(line);
		}
	}

	@Override
	public List<String> tabComplete(CommandSender sender, List<String> args)
	{
		return args.size() == 1 ? godNames() : List.of();
	}
}
