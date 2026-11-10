package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.managers.LanguageManager;

public class CommandHunt extends GodsCommand
{
	CommandHunt(GodsContext ctx)
	{
		super(ctx, "hunt");
		this.permission = "gods.hunt";
		this.description = "Show how far you are from the quest target";
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
		String godName = ctx.believerManager().getGodForBeliever(player.getUniqueId());
		if (godName == null)
		{
			sender.sendMessage(ChatColor.RED + "You do not believe in a God");
			return;
		}
		Location pilgrimageLocation = ctx.questManager().getQuestLocation(godName);
		if (player == null || player.isFlying())
		{
			sender.sendMessage(ChatColor.RED + "No flying allowed.");
			return;
		}
		if (pilgrimageLocation == null)
		{
			sender.sendMessage(ChatColor.RED + "There is no quest target to hunt for, Mr. fancy pants!");
			return;
		}
		if (!pilgrimageLocation.getWorld().getName().equals(player.getWorld().getName()))
		{
			ctx.plugin().logDebug("PilgrimageQuest for '" + player.getDisplayName() + "' is wrong world");
			return;
		}
		Vector vector = pilgrimageLocation.toVector().subtract(player.getLocation().toVector());
		ctx.languageManager().setAmount((int) vector.length());
		ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.QuestTargetRange, ChatColor.AQUA, (int) vector.length(), "", 20);

	}

}
