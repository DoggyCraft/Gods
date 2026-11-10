package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;

public class CommandSetDescription extends GodsCommand
{
	protected CommandSetDescription()
	{
		super("desc");
		this.permission = "gods.priest.description";
		this.parameters = "<text>";
		this.description = "Set the description of your religion";
		this.priestOnly = true;
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
			sender.sendMessage(ChatColor.RED + "Only priests can set religion info");
			return;
		}
		String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());
		if (args.length < 2)
		{
			sendUsage(sender);
			return;
		}
		String description = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
		GodManager.instance().setGodDescription(godName, description);
		sender.sendMessage(ChatColor.AQUA + "You set your religion description to " + ChatColor.YELLOW + GodManager.instance().getGodDescription(godName));
	}
}
