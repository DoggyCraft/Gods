package com.dogonfire.gods.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.managers.AltarManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.managers.PermissionsManager;

public class CommandHelp extends GodsCommand
{
	protected CommandHelp()
	{
		super("help");
		this.permission = "gods.help";
		this.description = "List of all gods";
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
		if (args[1].equals("blocks"))
		{
			helpBlocks(sender);
			return;
		}
		helpGeneral(sender);
	}

	private void helpGeneral(CommandSender sender)
	{
		sender.sendMessage(ChatColor.YELLOW + "------------------ " + Gods.instance().getDescription().getFullName() + " ------------------");
		sender.sendMessage(ChatColor.AQUA + "/gods" + ChatColor.WHITE + " - Basic info");
		sender.sendMessage(ChatColor.AQUA + "/gods help altar" + ChatColor.WHITE + " - How to build an altar to a God");
		sender.sendMessage(ChatColor.AQUA + "/gods help blocks" + ChatColor.WHITE + " - What type of blocks are used for God altars");

		for (GodsCommand localCommand : GodsCommandExecuter.instance().getCommands())
		{
			if (localCommand.hasPermission(sender))
			{
				sender.sendMessage(String.format("%s /gods %s %s %s - %s", ChatColor.AQUA.toString(), localCommand.name, localCommand.parameters == null ? "" : localCommand.parameters, ChatColor.WHITE.toString(), localCommand.description));
			}
		}

		if ((sender.isOp()) || (PermissionsManager.instance().hasPermission((Player) sender, "gods.info")))
		{
			sender.sendMessage(ChatColor.AQUA + "/gods info" + ChatColor.WHITE + " - Gets info about your god");
		}
		if ((sender.isOp()) || (PermissionsManager.instance().hasPermission((Player) sender, "gods.info")))
		{
			sender.sendMessage(ChatColor.AQUA + "/gods info <godname>" + ChatColor.WHITE + " - Show info about a specific God");
		}
		if ((sender.isOp()) || (PermissionsManager.instance().hasPermission((Player) sender, "gods.followers")))
		{
			sender.sendMessage(ChatColor.AQUA + "/gods followers" + ChatColor.WHITE + " - Shows the followers of your God");
		}
		if ((sender.isOp()) || (PermissionsManager.instance().hasPermission((Player) sender, "gods.followers")))
		{
			sender.sendMessage(ChatColor.AQUA + "/gods followers <godname>" + ChatColor.WHITE + " - Shows the followers of a specific God");
		}
		if ((sender.isOp()) || (PermissionsManager.instance().hasPermission((Player) sender, "gods.marry")))
		{
			sender.sendMessage(ChatColor.AQUA + "/gods marry <playername>" + ChatColor.WHITE + " - Marry that special someone");
		}
		if ((sender.isOp()) || (PermissionsManager.instance().hasPermission((Player) sender, "gods.love")))
		{
			sender.sendMessage(ChatColor.AQUA + "/gods love" + ChatColor.WHITE + " - Love your partner");
		}
		if ((sender.isOp()) || (PermissionsManager.instance().hasPermission((Player) sender, "gods.home")))
		{
			sender.sendMessage(ChatColor.AQUA + "/gods home" + ChatColor.WHITE + " - Teleports you to your religion home");
		}
		if ((sender.isOp()) || (PermissionsManager.instance().hasPermission((Player) sender, "gods.sethome")))
		{
			sender.sendMessage(ChatColor.AQUA + "/gods sethome" + ChatColor.WHITE + " - Sets the home of your religion");
		}
		if ((sender.isOp()) || (PermissionsManager.instance().hasPermission((Player) sender, "gods.leave")))
		{
			sender.sendMessage(ChatColor.AQUA + "/gods leave" + ChatColor.WHITE + " - Leave your religion");
		}

		sender.sendMessage(ChatColor.AQUA + "/gods yes" + ChatColor.WHITE + " - Accept a proposal from your god");
		sender.sendMessage(ChatColor.AQUA + "/gods no" + ChatColor.WHITE + " - Reject a proposal from your god");

		if ((sender.isOp()) || (PermissionsManager.instance().hasPermission((Player) sender, "gods.reload")))
		{
			sender.sendMessage(ChatColor.AQUA + "/gods reload" + ChatColor.WHITE + " - Reload config for gods system");
		}

		if (GodManager.instance().isPriest(((Player) sender).getUniqueId()))
		{
			if ((sender.isOp()) || (PermissionsManager.instance().hasPermission((Player) sender, "gods.priest.invite")))
			{
				sender.sendMessage(ChatColor.AQUA + "/gods invite <playername>" + ChatColor.WHITE + " - Invite a player to your religion");
			}
			if ((sender.isOp()) || (PermissionsManager.instance().hasPermission((Player) sender, "gods.priest.kick")))
			{
				sender.sendMessage(ChatColor.AQUA + "/gods kick <playername>" + ChatColor.WHITE + " - Kick a believer from your religion");
			}
			if ((sender.isOp()) || (PermissionsManager.instance().hasPermission((Player) sender, "gods.priest.alliance")))
			{
				sender.sendMessage(ChatColor.AQUA + "/gods ally <godname>" + ChatColor.WHITE + " - Become allies with another religion!");
			}
			if ((sender.isOp()) || (PermissionsManager.instance().hasPermission((Player) sender, "gods.priest.war")))
			{
				sender.sendMessage(ChatColor.AQUA + "/gods war <godname>" + ChatColor.WHITE + " - Go to war with another religion!");
			}
			if ((sender.isOp()) || (PermissionsManager.instance().hasPermission((Player) sender, "gods.priest.editbible")))
			{
				sender.sendMessage(ChatColor.AQUA + "/gods editbible" + ChatColor.WHITE + " - Edits the Holy Book for your religion");
			}
			if ((sender.isOp()) || (PermissionsManager.instance().hasPermission((Player) sender, "gods.priest.setbible")))
			{
				sender.sendMessage(ChatColor.AQUA + "/gods setbible" + ChatColor.WHITE + " - Sets a book to be the Holy Book for your religion");
			}
			if ((sender.isOp()) || (PermissionsManager.instance().hasPermission((Player) sender, "gods.priest.description")))
			{
				sender.sendMessage(ChatColor.AQUA + "/gods desc <text>" + ChatColor.WHITE + " - Set the description for your religion");
			}
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
