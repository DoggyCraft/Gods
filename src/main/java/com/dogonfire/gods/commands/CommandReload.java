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
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!hasPermission(sender))
		{
			sender.sendMessage(stringNoPermission);
			return;
		}

		Gods.instance().reloadSettings();
		sender.sendMessage(ChatColor.YELLOW + Gods.instance().getDescription().getFullName() + ": " + ChatColor.WHITE + "Reloaded configuration.");
		Gods.instance().log(sender.getName() + " /gods reload");
	}
}
