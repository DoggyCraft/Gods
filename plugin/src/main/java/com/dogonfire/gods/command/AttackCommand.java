package com.dogonfire.gods.command;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

/** /g attack: teleports the player to the holy land their religion is attacking */
public final class AttackCommand extends SubCommand
{
	public AttackCommand(GodsContext ctx)
	{
		super(ctx, "attack", "gods.attack");
		believerOnly();
	}

	@Override
	public String disabledMessage()
	{
		return this.ctx.settings().holyLand().enabled() ? null : "command.disabled.holy-land";
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		Player player = (Player) sender;
		String godName = believersGod(player);
		if (!this.ctx.relationService().isAttackingHolyLand(godName))
		{
			throw new CommandException("command.attack.no-attack");
		}
		Location attackLocation = this.ctx.relationService().getContestedHolyLandAttackLocationForGod(godName);
		if (attackLocation == null || attackLocation.getWorld() == null)
		{
			throw new CommandException("command.attack.world-not-loaded");
		}
		String otherGodName = this.ctx.holyLandService().getGodAtHolyLandLocation(attackLocation);
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
