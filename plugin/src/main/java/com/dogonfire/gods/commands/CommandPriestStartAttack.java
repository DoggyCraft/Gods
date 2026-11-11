package com.dogonfire.gods.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.message.Messages;

public class CommandPriestStartAttack extends GodsCommand
{
	CommandPriestStartAttack(GodsContext ctx)
	{
		super(ctx, "startattack");
		this.permission = "gods.priest.startattack";
		this.priestOnly = true;
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
			send(sender, NO_PERMISSION);
			return;
		}
		if (sender instanceof Player == false)
		{
			send(sender, PLAYER_ONLY);
			return;
		}
		Player player = (Player) sender;
		if (!ctx.priestService().isPriest(player.getUniqueId()))
		{
			send(player, PRIEST_ONLY);
			return;
		}
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		if (godName == null)
		{
			send(player, NO_GOD);
			return;
		}

		if (ctx.relationService().getContestedHolyLandForGod(godName) != null)
		{
			send(sender, "command.startattack.already");
			return;
		}
		String otherGodName = ctx.holyLandService().getGodAtHolyLandLocation(player.getLocation());
		if (otherGodName == null || ctx.holyLandService().isNeutralLandLocation(player.getLocation()))
		{
			send(sender, "command.startattack.not-holy-land");
			return;
		}
		if (otherGodName.equals(godName))
		{
			send(sender, "command.startattack.own-land");
			return;
		}
		if (!ctx.relationService().hasWarRelation(godName, otherGodName))
		{
			send(sender, "command.startattack.not-at-war", Messages.god(otherGodName));
			return;
		}
		if (ctx.holyLandService().isUnderAttack(player.getLocation()))
		{
			send(sender, "command.startattack.under-attack");
			return;
		}
		ctx.relationService().setContestedHolyLandForGod(godName, player.getLocation());
		send(sender, "command.startattack.done", Messages.god(otherGodName));
		ctx.godSpeech().sendInfoToBelievers(godName, "holy-land.attacking-hint");
		ctx.godSpeech().sayToBelievers(godName, Voice.BELIEVERS_ATTACK_STARTED, 40, Messages.player(otherGodName));
	}

}
