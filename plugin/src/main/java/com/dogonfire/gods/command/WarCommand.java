package com.dogonfire.gods.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

/** /g war &lt;god&gt;: starts or ends a war of the priest's god with another god */
public final class WarCommand extends SubCommand
{
	public WarCommand(GodsContext ctx)
	{
		super(ctx, "war", "gods.priest.war");
		priestOnly();
		args(1, 1);
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		String godName = believersGod((Player) sender);
		String typed = args.getFirst();
		if (!this.ctx.godService().godExist(typed))
		{
			throw new CommandException("command.no-such-god", Messages.god(typed));
		}
		String enemyGodName = this.ctx.godService().formatGodName(typed);
		if (enemyGodName.equals(godName))
		{
			throw new CommandException("command.war.own-god");
		}
		if (this.ctx.relationService().getAllianceRelations(godName).contains(enemyGodName))
		{
			throw new CommandException("command.war.allied", Messages.god(typed));
		}
		boolean atWar = !this.ctx.relationService().hasWarRelation(godName, enemyGodName);
		if (atWar)
		{
			// Ending a war is always possible
			requireRank(godName, this.ctx.settings().ranks().unlocks().relations());
		}
		if (!this.ctx.relationService().setWar(godName, enemyGodName, atWar))
		{
			throw new CommandException("command.cancelled");
		}
	}

	@Override
	public List<String> tabComplete(CommandSender sender, List<String> args)
	{
		return args.size() == 1 ? godNames() : List.of();
	}
}
