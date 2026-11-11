package com.dogonfire.gods.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.message.Voice;

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
		Voice voice = this.ctx.relationService().toggleWarRelationForGod(godName, enemyGodName) ? Voice.BELIEVERS_WAR : Voice.BELIEVERS_WAR_CANCELLED;
		this.ctx.godSpeech().sayToBelievers(enemyGodName, voice, 10, Messages.player(godName));
		this.ctx.godSpeech().sayToBelievers(godName, voice, 10, Messages.player(enemyGodName));
	}

	@Override
	public List<String> tabComplete(CommandSender sender, List<String> args)
	{
		return args.size() == 1 ? godNames() : List.of();
	}
}
