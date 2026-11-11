package com.dogonfire.gods.commands;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

public class CommandHome extends GodsCommand
{
	CommandHome(GodsContext ctx)
	{
		super(ctx, "home");
		this.permission = "gods.home";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!hasPermission(sender))
		{
			send(sender, NO_PERMISSION);
			return;
		}
		if (sender instanceof Player == false)
		{
			send(sender, PLAYER_ONLY);
			return;
		}
		Player player = (Player) sender;
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());

		if (godName == null)
		{
			send(sender, "command.not-believer");
			return;
		}
		Location location = ctx.godService().getHomeForGod(godName);
		if (location == null || location.getWorld() == null)
		{
			send(sender, "command.home.none");
			return;
		}
		player.teleport(location);
	}
}
