package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.LanguageManager;

public class CommandPriestStartAttack extends GodsCommand
{
	protected CommandPriestStartAttack()
	{
		super("startattack");
		this.permission = "gods.priest.startattack";
		this.description = "Start an attack on the enemy Holy Lands";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!hasPermission(sender))
		{
			sender.sendMessage(stringNoPermission);
			return;
		}
		if (sender instanceof Player == false)
		{
			sender.sendMessage(stringPlayerOnly);
			return;
		}
		Player player = (Player) sender;
		if (!GodManager.instance().isPriest(player.getUniqueId()))
		{
			player.sendMessage(stringPreistOnly);
			return;
		}
		String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());

		if (GodManager.instance().getContestedHolyLandForGod(godName) != null)
		{
			sender.sendMessage(ChatColor.RED + "You are already attacking a Holy Land!");
			return;
		}
		String otherGodName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());
		if (!GodManager.instance().hasWarRelation(godName, otherGodName))
		{
			sender.sendMessage(ChatColor.RED + "You are not in war with " + ChatColor.GOLD + otherGodName + "!");
			return;
		}
		GodManager.instance().setContestedHolyLandForGod(godName, player.getLocation());
		sender.sendMessage(ChatColor.AQUA + "You started an attack on the Holy Land of " + ChatColor.GOLD + otherGodName);
		GodManager.instance().sendInfoToBelievers(godName, LanguageManager.LANGUAGESTRING.AttackingHolyLandsHelp, ChatColor.AQUA, otherGodName, 10, 10, 80);
		LanguageManager.instance().setPlayerName(otherGodName);
		GodManager.instance().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversAttackStarted, 40);
	}

}
