package com.dogonfire.gods.commands;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.message.Messages;

public class CommandAlliance extends GodsCommand {

	CommandAlliance(GodsContext ctx) {
		super(ctx, "ally");
		this.permission = "gods.priest.alliance";
		this.priestOnly = true;
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args) {
		if (!hasPermission(sender)) {
			send(sender, "command.not-allowed");
			return;
		}
		if (!(sender instanceof Player)) {
			send(sender, PLAYER_ONLY);
			return;
		}
		if (args.length != 2)
		{
			send(sender, "command.ally.usage");
			return;
		}
		Player player = (Player) sender;
		if (!ctx.priestService().isPriest(player.getUniqueId())) {
			send(sender, "command.ally.not-priest");
			return;
		}
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		String allyGodName = ctx.godService().formatGodName(args[1]);
		if (!ctx.godService().godExist(args[1])) {
			send(player, "command.no-such-god", Messages.god(args[1]));
			return;
		}
		if (allyGodName.equals(godName)) {
			send(player, "command.ally.own-god");
			return;
		}
		List<?> wars = ctx.relationService().getWarRelations(godName);
		if (wars.contains(allyGodName)) {
			send(player, "command.ally.at-war", Messages.god(args[1]));
			return;
		}
		if (ctx.relationService().toggleAllianceRelationForGod(godName, allyGodName)) {
			ctx.godSpeech().sayToBelievers(allyGodName, Voice.BELIEVERS_ALLIANCE, 10, Messages.player(godName));
			ctx.godSpeech().sayToBelievers(godName, Voice.BELIEVERS_ALLIANCE, 10, Messages.player(allyGodName));
		} else {
			ctx.godSpeech().sayToBelievers(allyGodName, Voice.BELIEVERS_ALLIANCE_CANCELLED, 10, Messages.player(godName));

			ctx.godSpeech().sayToBelievers(godName, Voice.BELIEVERS_ALLIANCE_CANCELLED, 10, Messages.player(allyGodName));
		}
	}
}
