package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.HolyLandManager;

public class CommandAttackHolyLand extends GodsCommand
{
	protected CommandAttackHolyLand()
	{
		super("attack");
		this.permission = "gods.attack";
		this.description = "Join the attack of your religion on an enemy Holy Land";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!Gods.instance().settings().holyLand().enabled())
		{
			sender.sendMessage(ChatColor.RED + "Holy Land is not enabled on this server");
			return;
		}
		if (!hasPermission(sender))
		{
			sender.sendMessage(ChatColor.RED + "You do not have permission for that");
			return;
		}
		if (sender instanceof Player == false)
		{
			sender.sendMessage(stringPlayerOnly);
			return;
		}
		Player player = (Player) sender;
		String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());
		if (godName == null)
		{
			sender.sendMessage(stringNoGod);
			return;
		}
		if (GodManager.instance().getContestedHolyLandForGod(godName) == null)
		{
			sender.sendMessage(ChatColor.RED + "Your religion is not attacking a Holy Land. Your priest can start an attack with " + ChatColor.WHITE + "/gods startattack");
			return;
		}
		Location attackLocation = GodManager.instance().getContestedHolyLandAttackLocationForGod(godName);
		if (attackLocation == null || attackLocation.getWorld() == null)
		{
			sender.sendMessage(ChatColor.RED + "The Holy Land your religion is attacking is in a world that is not loaded");
			return;
		}
		String otherGodName = HolyLandManager.instance().getGodAtHolyLandLocation(attackLocation);
		player.teleport(attackLocation);
		if (otherGodName == null)
		{
			sender.sendMessage(ChatColor.AQUA + "You joined the attack on the enemy Holy Land");
		}
		else
		{
			sender.sendMessage(ChatColor.AQUA + "You joined the attack on the Holy Land of " + ChatColor.GOLD + otherGodName);
		}
	}
}
