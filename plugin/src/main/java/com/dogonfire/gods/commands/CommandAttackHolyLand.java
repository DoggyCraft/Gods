package com.dogonfire.gods.commands;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

public class CommandAttackHolyLand extends GodsCommand
{
	CommandAttackHolyLand(GodsContext ctx)
	{
		super(ctx, "attack");
		this.permission = "gods.attack";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!ctx.settings().holyLand().enabled())
		{
			send(sender, "command.disabled.holy-land");
			return;
		}
		if (!hasPermission(sender))
		{
			send(sender, "command.not-allowed");
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
			send(sender, NO_GOD);
			return;
		}
		if (ctx.relationService().getContestedHolyLandForGod(godName) == null)
		{
			send(sender, "command.attack.no-attack");
			return;
		}
		Location attackLocation = ctx.relationService().getContestedHolyLandAttackLocationForGod(godName);
		if (attackLocation == null || attackLocation.getWorld() == null)
		{
			send(sender, "command.attack.world-not-loaded");
			return;
		}
		String otherGodName = ctx.holyLandService().getGodAtHolyLandLocation(attackLocation);
		player.teleport(attackLocation);
		if (otherGodName == null)
		{
			send(sender, "command.attack.joined");
		}
		else
		{
			send(sender, "command.attack.joined-god", Messages.god(otherGodName));
		}
	}
}
