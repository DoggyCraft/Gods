package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

public class CommandAccess extends GodsCommand
{
	CommandAccess(GodsContext ctx)
	{
		super(ctx, "access");
		this.permission = "gods.priest.access";
		this.parameters = "[open/close]";
		this.description = "Set your religion as open or invite only";
		this.priestOnly = true;
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
		
		if (!ctx.godManager().isPriest(player.getUniqueId()))
		{
			player.sendMessage(ChatColor.RED + "Only priests can set religion access");
			return;
		}
		
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());

		if (godName == null || !ctx.godService().godExist(godName))
		{
			player.sendMessage(ChatColor.RED + "That God does not exist");
			return;
		}

		if (args.length < 2)
		{
			sendUsage(sender);
			return;
		}

		String access = args[1];
		
		if (access.equalsIgnoreCase("open"))
		{
			ctx.godService().setPrivateAccess(godName, false);
			ctx.plugin().log(player.getName() + " /gods open");
			player.sendMessage(ChatColor.AQUA + "You set the religion access to " + ChatColor.YELLOW + "open" + ChatColor.AQUA + ".");
			player.sendMessage(ChatColor.AQUA + "Players can join religion by praying at altars.");
		}
		else if (access.equalsIgnoreCase("close"))
		{
			ctx.godService().setPrivateAccess(godName, true);
			ctx.plugin().log(player.getName() + " /gods close");
			player.sendMessage(ChatColor.AQUA + "You set the religion access to " + ChatColor.RED + "closed" + ChatColor.AQUA + ".");
			player.sendMessage(ChatColor.AQUA + "Players can now only pray to this religion by invitation.");
		}
		else
		{
			player.sendMessage(ChatColor.RED + "That is not a valid command");
		}
	}
}
