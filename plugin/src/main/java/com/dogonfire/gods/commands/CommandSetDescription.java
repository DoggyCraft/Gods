package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

public class CommandSetDescription extends GodsCommand
{
	CommandSetDescription(GodsContext ctx)
	{
		super(ctx, "desc");
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
		if (!ctx.godManager().isPriest(player.getUniqueId()))
		{
			sender.sendMessage(ChatColor.RED + "Only priests can set religion info");
			return;
		}
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		if (args.length < 2)
		{
			sendUsage(sender);
			return;
		}
		String description = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
		ctx.godManager().setGodDescription(godName, description);
		sender.sendMessage(ChatColor.AQUA + "You set your religion description to " + ChatColor.YELLOW + ctx.godManager().getGodDescription(godName));
	}
}
