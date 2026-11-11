package com.dogonfire.gods.commands;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.message.Messages;

public class CommandToggleWar extends GodsCommand
{
	CommandToggleWar(GodsContext ctx)
	{
		super(ctx, "war");
		this.permission = "gods.priest.war";
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
		if (args.length != 2)
		{
			send(sender, "command.war.usage");
			return;
		}
		Player player = (Player) sender;
		if (!ctx.priestService().isPriest(player.getUniqueId()))
		{
			send(player, PRIEST_ONLY);
			return;
		}
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		String enemyGodName = ctx.godService().formatGodName(args[1]);
		if (!ctx.godService().godExist(args[1]))
		{
			send(player, "command.no-such-god", Messages.god(args[1]));
			return;
		}
		if (enemyGodName.equals(godName))
		{
			send(player, "command.war.own-god");
			return;
		}
		List<String> alliances = ctx.relationService().getAllianceRelations(godName);
		if (alliances.contains(enemyGodName))
		{
			send(player, "command.war.allied", Messages.god(args[1]));
			return;
		}
		if (ctx.relationService().toggleWarRelationForGod(godName, enemyGodName))
		{
			ctx.godSpeech().sayToBelievers(enemyGodName, Voice.BELIEVERS_WAR, 10, Messages.player(godName));

			ctx.godSpeech().sayToBelievers(godName, Voice.BELIEVERS_WAR, 10, Messages.player(enemyGodName));
		}
		else
		{
			ctx.godSpeech().sayToBelievers(enemyGodName, Voice.BELIEVERS_WAR_CANCELLED, 10, Messages.player(godName));

			ctx.godSpeech().sayToBelievers(godName, Voice.BELIEVERS_WAR_CANCELLED, 10, Messages.player(enemyGodName));
		}

	}
}
