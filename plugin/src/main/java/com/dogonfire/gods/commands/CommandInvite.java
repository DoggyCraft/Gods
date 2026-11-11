package com.dogonfire.gods.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.message.Messages;

public class CommandInvite extends GodsCommand
{
	CommandInvite(GodsContext ctx)
	{
		super(ctx, "invite");
		this.permission = "gods.priest.invite";
		this.priestOnly = true;
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
		if (!ctx.priestService().isPriest(player.getUniqueId()))
		{
			send(player, "command.invite.not-priest");
			return;
		}
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());

		if (godName == null)
		{
			send(player, "command.invite.no-god");
			return;
		}
		if (args.length < 2)
		{
			sendUsage(sender);
			return;
		}
		String playerName = args[1];
		Player invitedPlayer = ctx.plugin().getServer().getPlayerExact(playerName);
		if (invitedPlayer == null)
		{
			send(player, "command.invite.no-player", Messages.player(playerName));
			return;
		}
		String invitedPlayerGod = ctx.believerService().getGodForBeliever(invitedPlayer.getUniqueId());
		if ((invitedPlayerGod != null) && (invitedPlayerGod.equals(godName)))
		{
			send(player, "command.invite.already", Messages.player(playerName), Messages.god(godName));
			return;
		}
		ctx.believerService().setInvitation(invitedPlayer.getUniqueId(), godName);
		ctx.plugin().log(godName + " invited to " + invitedPlayer.getName() + " to join the religion");
		ctx.godSpeech().say(godName, invitedPlayer, Voice.PLAYER_INVITE, 10, Messages.player(invitedPlayer.getName()));
		ctx.godSpeech().sendInfo(invitedPlayer.getUniqueId(), "god.question-hint", 40);
		send(player, "command.invite.done", Messages.player(playerName), Messages.god(godName));
	}

}
