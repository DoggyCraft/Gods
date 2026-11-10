package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;

public class CommandGiveHolyArtifact extends GodsCommand
{
	protected CommandGiveHolyArtifact()
	{
		super("holyartifact");
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!GodsConfiguration.instance().isHolyArtifactsEnabled())
		{
			sender.sendMessage(ChatColor.RED + "Holy Artifacts are not enabled on this server");
			return;
		}
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
		String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());
		GodManager.instance().blessPlayerWithHolyArtifact(godName, player);
		sender.sendMessage(ChatColor.GOLD + godName + ChatColor.AQUA + " gave " + player.getName() + ChatColor.AQUA + " a Holy artifact!");
	}
}
