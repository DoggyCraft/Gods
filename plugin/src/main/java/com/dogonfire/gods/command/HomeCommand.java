package com.dogonfire.gods.command;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.model.BlockPos;
import com.dogonfire.gods.service.HomeRules;
import com.dogonfire.gods.util.SafeSpots;

/**
 * /g home: teleports the believer to the home of their religion, its main altar ({@link HomeRules}): onto the spot in
 * front of its sign, facing the altar, or the nearest safe spot around it. A god without a main altar keeps the home
 * stored before main altars.
 */
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
		String godName = believersGod(player);
		HomeRules.Home home = this.ctx.godService().getHome(godName);
		World world = home != null ? this.ctx.plugin().getServer().getWorld(home.world()) : null;
		if (world == null)
		{
			throw new CommandException("command.home.none", Messages.god(godName));
		}

		BlockPos spot = home.spot();
		BlockPos feet = HomeRules.safeLanding(spot, home.keepClear(), pos -> SafeSpots.isSafe(world, world.getBlockAt(pos.x(), pos.y(), pos.z())));
		if (feet == null)
		{
			throw new CommandException("command.home.unsafe", Messages.god(godName));
		}

		Location to;
		if (feet.equals(spot))
		{
			to = new Location(world, home.x(), home.y(), home.z(), home.yaw(), 0.0F);
		}
		else
		{
			float yaw = home.altarBlock() != null ? HomeRules.facing(feet, home.altarBlock()) : home.yaw();
			to = new Location(world, feet.x() + 0.5D, feet.y(), feet.z() + 0.5D, yaw, 0.0F);
		}
		player.teleport(to, TeleportCause.COMMAND);
	}
}
