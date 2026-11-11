package com.dogonfire.gods.commands;

import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

public class CommandKick extends GodsCommand
{
	CommandKick(GodsContext ctx)
	{
		super(ctx, "kick");
		this.permission = "gods.priest.kick";
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
			send(sender, "command.kick.not-priest");
			return;
		}
		if (args.length < 2)
		{
			sendUsage(sender);
			return;
		}
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		String believerName = args[1];
		UUID believerId = findBeliever(godName, believerName);
		if (godName == null || believerId == null)
		{
			send(sender, "command.kick.no-believer", Messages.player(believerName));
			return;
		}
		if (believerId.equals(player.getUniqueId()))
		{
			send(sender, "command.kick.yourself");
			return;
		}
		ctx.believerService().removeBeliever(godName, believerId);
		send(sender, "command.kick.done", Messages.player(believerName));
		Player believer = ctx.plugin().getServer().getPlayer(believerId);
		if (believer != null)
		{
			send(believer, "command.kick.kicked", Messages.god(godName));
		}
		ctx.plugin().log(sender.getName() + " /gods kick " + believerName);
	}

	/**
	 * Finds a believer of a god by name among the god's known believers, so it
	 * never asks Mojang for an unknown name.
	 */
	private UUID findBeliever(String godName, String believerName)
	{
		if (godName == null)
		{
			return null;
		}
		for (UUID believerId : ctx.believerService().getBelieversForGod(godName))
		{
			OfflinePlayer offlinePlayer = ctx.plugin().getServer().getOfflinePlayer(believerId);
			if (believerName.equalsIgnoreCase(offlinePlayer.getName()))
			{
				return believerId;
			}
		}
		return null;
	}
}
