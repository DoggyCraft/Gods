package com.dogonfire.gods.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.dogonfire.gods.GodsContext;

public class GodsTabCompleter implements TabCompleter
{
	private final GodsContext			ctx;
	private final GodsCommandExecuter	executer;

	public GodsTabCompleter(GodsContext ctx, GodsCommandExecuter executer)
	{
		this.ctx = ctx;
		this.executer = executer;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args)
	{
		List<String> result = new ArrayList<String>();

		if (!(sender instanceof Player))
		{
			return result;
		}

		Player player = (Player) sender;

		if (args.length == 1)
		{
			boolean isPriest = ctx.godManager().isPriest(player.getUniqueId());
			List<String> commands = new ArrayList<String>();
			for (GodsCommand command : this.executer.getCommands())
			{
				if (command.isAvailableTo(player, isPriest))
				{
					commands.add(command.name);
				}
			}
			StringUtil.copyPartialMatches(args[0], commands, result);
		}
		else if (args.length == 2 || (args.length == 3 && args[0].equalsIgnoreCase("setpriest")))
		{
			GodsCommand command = this.executer.getCommand(args[0]);
			if (command == null || !command.hasPermission(player))
			{
				return result;
			}

			List<String> options = new ArrayList<String>();
			switch (command.name)
			{
			case "help":
				options.add("altar");
				options.add("blocks");
				break;
			case "access":
				options.add("open");
				options.add("close");
				break;
			case "prayfor":
				options.add("item");
				options.add("health");
				options.add("blessing");
				options.add("quest");
				break;
			case "ally":
			case "war":
			case "followers":
			case "info":
				options.addAll(ctx.godManager().getAllGods());
				break;
			case "setpriest":
				if (args.length == 3)
				{
					// Player names
					return null;
				}
				options.addAll(ctx.godManager().getAllGods());
				break;
			case "marry":
			case "invite":
			case "kick":
			case "check":
				// Player names
				return null;
			default:
				break;
			}

			StringUtil.copyPartialMatches(args[args.length - 1], options, result);
		}

		Collections.sort(result);
		return result;
	}
}
