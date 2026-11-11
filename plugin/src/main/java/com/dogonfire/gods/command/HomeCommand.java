package com.dogonfire.gods.command;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

/** /g home: teleports the player to the home of their religion */
public final class HomeCommand extends SubCommand
{
	public HomeCommand(GodsContext ctx)
	{
		super(ctx, "home", "gods.home");
		believerOnly();
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		Player player = (Player) sender;
		Location location = this.ctx.godService().getHomeForGod(believersGod(player));
		if (location == null || location.getWorld() == null)
		{
			throw new CommandException("command.home.none");
		}
		player.teleport(location);
	}
}
