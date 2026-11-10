package com.dogonfire.gods.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.LanguageManager;

public class CommandAlliance extends GodsCommand {

	protected CommandAlliance() {
		super("ally");
		this.permission = "gods.priest.alliance";
		this.parameters = "<god>";
		this.description = "Toggle alliance with another religion";
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
		if (!GodManager.instance().isPriest(player.getUniqueId())) {
			sender.sendMessage(ChatColor.RED + "Only priests can declare religous wars");
			return;
		}
		String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());
		String allyGodName = GodManager.instance().formatGodName(args[1]);
		if (!GodManager.instance().godExist(args[1])) {
			player.sendMessage(ChatColor.RED + "There is no God with the name " + ChatColor.GOLD + args[1]);
			return;
		}
		List<?> wars = GodManager.instance().getWarRelations(godName);
		if (wars.contains(allyGodName)) {
			player.sendMessage(ChatColor.RED + "You are in WAR with " + ChatColor.GOLD + args[1] + ChatColor.RED + "!");
			return;
		}
		if (GodManager.instance().toggleAllianceRelationForGod(godName, allyGodName)) {
			LanguageManager.instance().setPlayerName(godName);
			GodManager.instance().godSayToBelievers(allyGodName, LanguageManager.LANGUAGESTRING.GodToBelieversAlliance, 10);
			LanguageManager.instance().setPlayerName(allyGodName);
			GodManager.instance().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversAlliance, 10);
		} else {
			LanguageManager.instance().setPlayerName(godName);
			GodManager.instance().godSayToBelievers(allyGodName, LanguageManager.LANGUAGESTRING.GodToBelieversAllianceCancelled, 10);

			LanguageManager.instance().setPlayerName(allyGodName);
			GodManager.instance().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversAllianceCancelled, 10);
		}
	}
}
