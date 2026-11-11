package com.dogonfire.gods.commands;

import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

public class CommandLove extends GodsCommand
{
	CommandLove(GodsContext ctx)
	{
		super(ctx, "love");
		this.permission = "gods.love";
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
		UUID partnerId = ctx.marriageService().getPartnerId(player.getUniqueId());
		if (partnerId == null)
		{
			send(player, "command.not-married");
			return;
		}
		Player partner = ctx.plugin().getServer().getPlayer(partnerId);
		if (partner == null)
		{
			send(player, "command.love.offline", Messages.player(ctx.plugin().getServer().getOfflinePlayer(partnerId).getName()));
			return;
		}
		ctx.marriageService().love(player.getUniqueId());
		send(player, "command.love.done", Messages.displayName(partner.getDisplayName()));
	}

}
