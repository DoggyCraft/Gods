package com.dogonfire.gods.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

/** /g access open/close: whether players can join the priest's religion at altars or only by invitation */
public final class AccessCommand extends SubCommand
{
	public AccessCommand(GodsContext ctx)
	{
		super(ctx, "access", "gods.priest.access");
		priestOnly("command.access.not-priest");
		believerOnly("command.access.no-god");
		args(1, ANY);
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		Player player = (Player) sender;
		String godName = believersGod(player);
		if (!this.ctx.godService().godExist(godName))
		{
			throw new CommandException("command.access.no-god");
		}

		String access = args.getFirst();
		if (access.equalsIgnoreCase("open"))
		{
			this.ctx.godService().setPrivateAccess(godName, false);
			this.ctx.plugin().log(player.getName() + " /gods open");
			send(player, "command.access.opened");
		}
		else if (access.equalsIgnoreCase("close"))
		{
			this.ctx.godService().setPrivateAccess(godName, true);
			this.ctx.plugin().log(player.getName() + " /gods close");
			send(player, "command.access.closed");
		}
		else
		{
			throw new CommandException("command.access.invalid");
		}
	}

	@Override
	public List<String> tabComplete(CommandSender sender, List<String> args)
	{
		return args.size() == 1 ? List.of("open", "close") : List.of();
	}
}
