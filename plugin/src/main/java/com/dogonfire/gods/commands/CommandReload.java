package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.dogonfire.gods.GodsContext;

public class CommandReload extends GodsCommand
{
	CommandReload(GodsContext ctx)
	{
		super(ctx, "reload");
		this.permission = "gods.reload";
		this.description = "Reload the configuration and data files";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!hasPermission(sender))
		{
			sender.sendMessage(stringNoPermission);
			return;
		}

		if (!ctx.plugin().reload())
		{
			sender.sendMessage(ChatColor.RED + "Could not reload; the old settings and data are kept. See the server log.");
			return;
		}
		sender.sendMessage(ChatColor.YELLOW + ctx.plugin().getDescription().getFullName() + ": " + ChatColor.WHITE + "Reloaded configuration.");
		ctx.plugin().log(sender.getName() + " /gods reload");
	}
}
