package com.dogonfire.gods.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.message.Messages;

public class CommandAlliance extends GodsCommand {

	CommandAlliance(GodsContext ctx) {
		super(ctx, "ally");
		this.permission = "gods.priest.alliance";
		this.parameters = "<god>";
		this.description = "Toggle alliance with another religion";
		this.priestOnly = true;
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args) {
		if (!hasPermission(sender)) {
			sender.sendMessage(ChatColor.RED + "You do not have permission for that");
			return;
		}
		if (!(sender instanceof Player)) {
			sender.sendMessage(stringPlayerOnly);
			return;
		}
		if (args.length != 2)
		{
			sender.sendMessage(ChatColor.RED + "You need to specify a God to ally with! " + ChatColor.YELLOW + "/gods ally <godname>");
			return;
		}
		Player player = (Player) sender;
		if (!ctx.priestService().isPriest(player.getUniqueId())) {
			sender.sendMessage(ChatColor.RED + "Only priests can declare alliances");
			return;
		}
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		String allyGodName = ctx.godService().formatGodName(args[1]);
		if (!ctx.godService().godExist(args[1])) {
			player.sendMessage(ChatColor.RED + "There is no God with the name " + ChatColor.GOLD + args[1]);
			return;
		}
		if (allyGodName.equals(godName)) {
			player.sendMessage(ChatColor.RED + "You cannot ally with your own God!");
			return;
		}
		List<?> wars = ctx.relationService().getWarRelations(godName);
		if (wars.contains(allyGodName)) {
			player.sendMessage(ChatColor.RED + "You are in WAR with " + ChatColor.GOLD + args[1] + ChatColor.RED + "!");
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
