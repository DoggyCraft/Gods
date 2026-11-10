package com.dogonfire.gods.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.PermissionsManager;

public class GodsTabCompleter implements TabCompleter
{
	private static GodsTabCompleter instance;

	public static GodsTabCompleter instance()
	{
		if (instance == null)
			instance = new GodsTabCompleter();
		return instance;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args)
	{

		List<String> result = new ArrayList<String>();

		Player player = null;
		if (sender instanceof Player)
		{
			player = (Player) sender;
		}

		if (args.length == 1 && (cmd.getName().equalsIgnoreCase("gods") || cmd.getName().equalsIgnoreCase("g")))
		{
			List<String> arg1 = new ArrayList<String>();
			arg1.add("help");
			if (player.isOp() || PermissionsManager.instance().hasPermission(player, "gods.info"))
			{
				arg1.add("info");
			}
			if (player.isOp() || PermissionsManager.instance().hasPermission(player, "gods.followers"))
			{
				arg1.add("followers");
			}
			if (player.isOp() || PermissionsManager.instance().hasPermission(player, "gods.love"))
			{
				arg1.add("love");
			}
			if (player.isOp() || PermissionsManager.instance().hasPermission(player, "gods.home"))
			{
				arg1.add("home");
			}
			if (player.isOp() || PermissionsManager.instance().hasPermission(player, "gods.sethome"))
			{
				arg1.add("sethome");
			}
			if (player.isOp() || PermissionsManager.instance().hasPermission(player, "gods.leave"))
			{
				arg1.add("leave");
			}
			if (player.isOp() || PermissionsManager.instance().hasPermission(player, "gods.marry"))
			{
				arg1.add("marry");
			}
			if (player.isOp() || PermissionsManager.instance().hasPermission(player, "gods.reload"))
			{
				arg1.add("reload");
			}
			if (player.isOp() || PermissionsManager.instance().hasPermission(player, "gods.answer"))
			{
				arg1.add("yes");
				arg1.add("no");
			}

			if (GodManager.instance().isPriest(player.getUniqueId()))
			{
				if (player.isOp() || PermissionsManager.instance().hasPermission(player, "gods.priest.invite"))
				{
					arg1.add("invite");
				}
				if (player.isOp() || PermissionsManager.instance().hasPermission(player, "gods.priest.kick"))
				{
					arg1.add("kick");
				}
				if (player.isOp() || PermissionsManager.instance().hasPermission(player, "gods.priest.alliance"))
				{
					arg1.add("ally");
				}
				if (player.isOp() || PermissionsManager.instance().hasPermission(player, "gods.priest.war"))
				{
					arg1.add("war");
				}
				if (player.isOp() || PermissionsManager.instance().hasPermission(player, "gods.priest.editbible"))
				{
					arg1.add("editbible");
				}
				if (player.isOp() || PermissionsManager.instance().hasPermission(player, "gods.priest.setbible"))
				{
					arg1.add("setbible");
				}
				if (player.isOp() || PermissionsManager.instance().hasPermission(player, "gods.priest.desc"))
				{
					arg1.add("desc");
				}
			}
			Iterable<String> FIRST_ARGUMENTS = arg1;
			StringUtil.copyPartialMatches(args[0], FIRST_ARGUMENTS, result);
		}
		else if (args.length == 2)
		{
			List<String> arg2 = new ArrayList<String>();
			if (args[0].equalsIgnoreCase("help"))
			{
				arg2.add("altar");
				arg2.add("blocks");
			}
			if (args[0].equalsIgnoreCase("desc"))
			{
				arg2.add("<description>");
			}
			if (args[0].equalsIgnoreCase("ally") || args[0].equalsIgnoreCase("war") || args[0].equalsIgnoreCase("followers") || args[0].equalsIgnoreCase("info"))
			{
				Set<String> gods = GodManager.instance().getAllGods();
				for (String god : gods)
				{
					arg2.add(god);
				}
			}

			if ((args[0].equalsIgnoreCase("marry") || args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick")))
			{
				return null;
			}
			
			Iterable<String> SECOND_ARGUMENTS = arg2;
			StringUtil.copyPartialMatches(args[1], SECOND_ARGUMENTS, result);
		}

		Collections.sort(result);
		return result;
	}
}