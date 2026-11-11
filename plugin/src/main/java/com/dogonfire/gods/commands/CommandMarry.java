package com.dogonfire.gods.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.message.Messages;

public class CommandMarry extends GodsCommand
{
	CommandMarry(GodsContext ctx)
	{
		super(ctx, "marry");
		this.permission = "gods.marry";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!ctx.settings().marriage().enabled())
		{
			send(sender, "command.disabled.marriage");
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
		String thisGodName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		if (thisGodName == null)
		{
			send(player, "command.not-believer");
			return;
		}

		if (args.length < 2)
		{
			send(player, "command.marry.who");
			return;
		}

		String otherPlayerName = args[1];
		Player otherPlayer = ctx.plugin().getServer().getPlayerExact(otherPlayerName);
		if (otherPlayer == null)
		{
			send(player, "command.marry.no-player", Messages.player(otherPlayerName));
			return;
		}

		if (otherPlayer.getUniqueId().equals(player.getUniqueId()))
		{
			send(player, "command.marry.yourself");
			return;
		}

		String otherGodName = ctx.believerService().getGodForBeliever(otherPlayer.getUniqueId());

		if (otherGodName == null)
		{
			send(player, "command.marry.no-god", Messages.player(otherPlayerName));
			return;
		}

		if (!thisGodName.equals(otherGodName))
		{
			send(player, "command.marry.other-god", Messages.player(otherPlayerName), Messages.god(thisGodName));
			return;
		}

		String partnerName = ctx.marriageService().getPartnerName(otherPlayer.getUniqueId());
		if (partnerName != null)
		{
			send(player, "command.marry.other-married", Messages.player(otherPlayerName), Messages.text("partner", partnerName));
			return;
		}
		partnerName = ctx.marriageService().getPartnerName(player.getUniqueId());
		if (partnerName != null)
		{
			send(player, "command.marry.married", Messages.text("partner", partnerName));
			return;
		}

		ctx.marriageService().proposeMarriage(player.getUniqueId(), otherPlayer.getUniqueId());

		ctx.godSpeech().sayWithQuestion(thisGodName, otherPlayer, Voice.BELIEVER_MARRIAGE_PROPOSAL, 1, Messages.player(player.getName()));

		send(player, "command.marry.proposed", Messages.player(otherPlayerName), Messages.god(thisGodName));
	}
}
