package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.QuestManager;
import com.dogonfire.gods.managers.WhitelistManager;

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
		
		GodsConfiguration.instance().loadSettings();
		GodManager.instance().load();
		QuestManager.instance().load();
		BelieverManager.instance().load();
		WhitelistManager.instance().load();
		sender.sendMessage(ChatColor.YELLOW + Gods.instance().getDescription().getFullName() + ": " + ChatColor.WHITE + "Reloaded configuration.");
		Gods.instance().log(sender.getName() + " /gods reload");
	}
}
