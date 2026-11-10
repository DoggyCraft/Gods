package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.HolyLandManager;

public class CommandAttackHolyLand extends GodsCommand
{
	protected CommandAttackHolyLand()
	{
		super("attack");
		this.permission = "gods.priest.attack";
		this.description = "Join the attack on the enemy Holy Land";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
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
		Location attackLocation = GodManager.instance().getContestedHolyLandAttackLocationForGod(godName);
		player.teleport(attackLocation);
		String otherGodName = HolyLandManager.instance().getGodAtHolyLandLocation(attackLocation);
		sender.sendMessage(ChatColor.AQUA + "You joined the attack on the Holy Land of " + ChatColor.GOLD + otherGodName);
	}
}
