package com.dogonfire.gods.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.model.GodMood;

public class CommandInfo extends GodsCommand
{
	CommandInfo(GodsContext ctx)
	{
		super(ctx, "info");
		this.permission = "gods.info";
		this.parameters = "[god]";
		this.description = "Show info about your/a God";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!hasPermission(sender))
		{
			sender.sendMessage(stringNoPermission);
			return;
		}
		
		String godName = null;

		if (args.length == 2)
		{
			godName = ctx.godService().formatGodName(args[1]);
		}

		if (godName == null)
		{
			if (sender instanceof Player)
				godName = ctx.believerService().getGodForBeliever(((Player) sender).getUniqueId());
			if (godName == null)
			{
				sender.sendMessage(ChatColor.RED + "You do not believe in any God.");
				return;
			}
		}

		if (!ctx.godService().godExist(godName))
		{
			sender.sendMessage(ChatColor.RED + "There is no God with such name.");
			return;
		}

		List<UUID> priests = ctx.priestService().getPriestsForGod(godName);

		if (priests == null)
		{
			priests = new ArrayList<UUID>();
		}

		sender.sendMessage(ChatColor.YELLOW + "--------- " + godName + " " + ctx.godService().getColorForGod(godName) + ctx.godService().getTitleForGod(godName) + ChatColor.YELLOW + " ---------");

		sender.sendMessage("" + ChatColor.DARK_PURPLE + ChatColor.ITALIC + ctx.godService().getGodDescription(godName));

		ChatColor moodColor = ChatColor.AQUA;
		GodMood godMood = ctx.moodService().getMoodForGod(godName);

		switch (godMood)
		{
		case EXALTED:
			moodColor = ChatColor.GOLD;
			break;
		case PLEASED:
			moodColor = ChatColor.DARK_GREEN;
			break;
		case NEUTRAL:
			moodColor = ChatColor.WHITE;
			break;
		case DISPLEASED:
			moodColor = ChatColor.GRAY;
			break;
		case ANGRY:
			moodColor = ChatColor.DARK_RED;
			break;
		}

		sender.sendMessage(moodColor + godName + " is " + Messages.legacy(ctx.messages().moodName(godMood)));
	
		Material neededItem = ctx.commandmentService().getSacrificeItemTypeForGod(godName);
		if (neededItem != null)
		{
			sender.sendMessage(ChatColor.GOLD + godName + ChatColor.AQUA + " wants more " + ChatColor.WHITE + Messages.legacy(ctx.messages().itemName(neededItem)));
		}

		if (priests.size() == 0)
		{
			sender.sendMessage(ChatColor.AQUA + "Priest: " + ChatColor.YELLOW + "None");
		}
		else if (priests.size() == 1)
		{
			sender.sendMessage(ChatColor.AQUA + "Priest: " + ChatColor.YELLOW + ctx.plugin().getServer().getOfflinePlayer(priests.get(0)).getName());
		}
		else
		{
			sender.sendMessage(ChatColor.AQUA + "Priests: ");
			for (UUID priest : priests)
			{
				sender.sendMessage(ChatColor.YELLOW + " - " + ctx.plugin().getServer().getOfflinePlayer(priest).getName());
			}
		}

		sender.sendMessage(ChatColor.AQUA + "Believers: " + ChatColor.YELLOW + ctx.believerService().getBelieversForGod(godName).size());
		sender.sendMessage(ChatColor.AQUA + "Exact power: " + ChatColor.YELLOW + ctx.godService().getGodPower(godName));
		if (ctx.settings().commandments().enabled())
		{
			sender.sendMessage(ChatColor.AQUA + "Holy food: " + ChatColor.YELLOW + Messages.legacy(ctx.messages().itemName(ctx.commandmentService().getHolyFoodTypeForGod(godName))));
			sender.sendMessage(ChatColor.AQUA + "Unholy food: " + ChatColor.YELLOW + Messages.legacy(ctx.messages().itemName(ctx.commandmentService().getUnholyFoodTypeForGod(godName))));

			sender.sendMessage(ChatColor.AQUA + "Holy creature: " + ChatColor.YELLOW + Messages.legacy(ctx.messages().mobName(ctx.commandmentService().getHolyMobTypeForGod(godName))));
			sender.sendMessage(ChatColor.AQUA + "Unholy creature: " + ChatColor.YELLOW + Messages.legacy(ctx.messages().mobName(ctx.commandmentService().getUnholyMobTypeForGod(godName))));
		}

		List<String> allyRelations = ctx.relationService().getAllianceRelations(godName);
		Object warRelations = ctx.relationService().getWarRelations(godName);

		if ((((List<?>) warRelations).size() > 0) || (allyRelations.size() > 0))
		{
			sender.sendMessage(ChatColor.AQUA + "Religious relations: ");
			for (String ally : ctx.relationService().getAllianceRelations(godName))
			{
				sender.sendMessage(ChatColor.GREEN + " Alliance with " + ChatColor.GOLD + ally);
			}
			List<String> enemies = ctx.relationService().getWarRelations(godName);
			for (String enemy : enemies)
			{
				sender.sendMessage(ChatColor.RED + " War with " + ChatColor.GOLD + enemy);
			}
		}
		
		if(ctx.settings().holyLand().enabled())
		{
			int claimed = ctx.holyLandService().getClaimedChunksForGod(godName);
			int maxclaim = ctx.holyLandService().getMaximumChunksForGod(godName);
			sender.sendMessage(ChatColor.AQUA + "Holyland claimed: " + ChatColor.GOLD + claimed + "/" + maxclaim);					
		}
	}
}
