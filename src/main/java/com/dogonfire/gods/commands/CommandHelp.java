package com.dogonfire.gods.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.managers.AltarManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.LanguageManager;

public class CommandHelp extends GodsCommand
{
	protected CommandHelp()
	{
		super("help");
		this.permission = "gods.help";
		this.parameters = "[altar/blocks]";
		this.description = "Show the list of Gods commands";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!hasPermission(sender))
		{
			sender.sendMessage(stringNoPermission);
			return;
		}
		if (args.length < 2)
		{
			helpGeneral(sender);
			return;
		}
		if (args[1].equalsIgnoreCase("Altar"))
		{
			helpAltar(sender);
			return;
		}
		if (args[1].equalsIgnoreCase("blocks"))
		{
			helpBlocks(sender);
			return;
		}
		helpGeneral(sender);
	}

	private void helpGeneral(CommandSender sender)
	{
		boolean isPriest = (sender instanceof Player) && GodManager.instance().isPriest(((Player) sender).getUniqueId());

		sender.sendMessage(ChatColor.YELLOW + "------------------ " + Gods.instance().getDescription().getFullName() + " ------------------");
		sender.sendMessage(ChatColor.AQUA + "/gods" + ChatColor.WHITE + " - Basic info");

		for (GodsCommand localCommand : GodsCommandExecuter.instance().getCommands())
		{
			if (!localCommand.isAvailableTo(sender, isPriest))
			{
				continue;
			}

			String usage = ChatColor.AQUA + "/gods " + localCommand.name;
			if (localCommand.parameters != null && !localCommand.parameters.isEmpty())
			{
				usage += " " + localCommand.parameters;
			}
			if (localCommand.description != null && !localCommand.description.isEmpty())
			{
				usage += ChatColor.WHITE + " - " + localCommand.description;
			}
			sender.sendMessage(usage);
		}
	}

	private void helpAltar(CommandSender sender)
	{
		sender.sendMessage(ChatColor.YELLOW + "--------------- How to build an Altar ---------------");
		sender.sendMessage(ChatColor.AQUA + "Build an altar to your God by following these simple steps:");
		sender.sendMessage("");
		sender.sendMessage(ChatColor.WHITE + "  1  - Place a block");
		sender.sendMessage(ChatColor.WHITE + "  2a - Place a torch on top for a male god");
		sender.sendMessage(ChatColor.WHITE + "  2b - Place a redstone torch on top for a female god");
		sender.sendMessage(ChatColor.WHITE + "  3  - Place a sign on the side of the block");
		sender.sendMessage(ChatColor.WHITE + "  4  - Write the name of your God on the sign");
		sender.sendMessage("");
		sender.sendMessage(ChatColor.AQUA + "You can now pray to your God by right-clicking the sign!");
		sender.sendMessage(ChatColor.AQUA + "Check what type of god you can make with " + ChatColor.WHITE + "/g help blocks");
	}

	private void helpBlocks(CommandSender sender)
	{
		sender.sendMessage(ChatColor.YELLOW + "--------------- God Types ---------------");
		sender.sendMessage(ChatColor.AQUA + "These are the block types you can use for making Gods:");
		sender.sendMessage("");
		
		for (GodManager.GodType godType : GodManager.GodType.values())
		{
			List<String> materials = AltarManager.instance().getAltarBlockTypesFromGodType(godType);
			if (materials != null)
			{
				for (String blockMaterial : materials)
				{
					sender.sendMessage(ChatColor.WHITE + blockMaterial + "  --->  " + GodManager.instance().getColorForGodType(godType) + LanguageManager.instance().getGodTypeName(godType, "God"));
				}
			}
		}
		
		sender.sendMessage("");
		sender.sendMessage(ChatColor.AQUA + "Check how to build an altar with " + ChatColor.WHITE + "/g help altar");

	}

}
