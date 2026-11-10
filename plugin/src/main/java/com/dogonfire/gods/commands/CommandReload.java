package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.dogonfire.gods.Gods;

public class CommandReload extends GodsCommand
{
	protected CommandReload()
	{
		super("reload");
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

		if (!Gods.instance().reloadSettings())
		{
			sender.sendMessage(ChatColor.RED + "Could not reload config.yml; the old settings are kept. See the server log.");
			return;
		}
		sender.sendMessage(ChatColor.YELLOW + Gods.instance().getDescription().getFullName() + ": " + ChatColor.WHITE + "Reloaded configuration.");
		Gods.instance().log(sender.getName() + " /gods reload");
	}
}
