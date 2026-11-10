package com.dogonfire.gods.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.HolyLandManager;
import com.dogonfire.gods.managers.GodManager.GodMood;
import com.dogonfire.gods.managers.LanguageManager;


public class CommandInfo extends GodsCommand
{
	protected CommandInfo()
	{
		super("info");
		this.permission = "gods.info";
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
			godName = GodManager.instance().formatGodName(args[1]);
		}

		if (godName == null)
		{
			if (sender instanceof Player)
				godName = BelieverManager.instance().getGodForBeliever(((Player) sender).getUniqueId());
			if (godName == null)
			{
				sender.sendMessage(ChatColor.RED + "You do not believe in any God.");
				return;
			}
		}

		if (!GodManager.instance().godExist(godName))
		{
			sender.sendMessage(ChatColor.RED + "There is no God with such name.");
			return;
		}

		List<UUID> priests = GodManager.instance().getPriestsForGod(godName);

		if (priests == null)
		{
			priests = new ArrayList<UUID>();
		}

		sender.sendMessage(ChatColor.YELLOW + "--------- " + godName + " " + GodManager.instance().getColorForGod(godName) + GodManager.instance().getTitleForGod(godName) + ChatColor.YELLOW + " ---------");

		sender.sendMessage("" + ChatColor.DARK_PURPLE + ChatColor.ITALIC + GodManager.instance().getGodDescription(godName));

		ChatColor moodColor = ChatColor.AQUA;
		GodMood godMood = GodManager.instance().getMoodForGod(godName);

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

		sender.sendMessage(moodColor + godName + " is " + LanguageManager.instance().getGodMoodName(godMood));
	
		Material neededItem = GodManager.instance().getSacrificeItemTypeForGod(godName);
		if (neededItem != null)
		{
			sender.sendMessage(ChatColor.GOLD + godName + ChatColor.AQUA + " wants more " + ChatColor.WHITE + LanguageManager.instance().getItemTypeName(neededItem));
		}

		if (priests.size() == 0)
		{
			sender.sendMessage(ChatColor.AQUA + "Priest: " + ChatColor.YELLOW + "None");
		}
		else if (priests.size() == 1)
		{
			sender.sendMessage(ChatColor.AQUA + "Priest: " + ChatColor.YELLOW + Gods.instance().getServer().getOfflinePlayer(priests.get(0)).getName());
		}
		else
		{
			sender.sendMessage(ChatColor.AQUA + "Priests: ");
			for (UUID priest : priests)
			{
				sender.sendMessage(ChatColor.YELLOW + " - " + Gods.instance().getServer().getOfflinePlayer(priest).getName());
			}
		}

		sender.sendMessage(ChatColor.AQUA + "Believers: " + ChatColor.YELLOW + BelieverManager.instance().getBelieversForGod(godName).size());
		sender.sendMessage(ChatColor.AQUA + "Exact power: " + ChatColor.YELLOW + GodManager.instance().getGodPower(godName));
		if (GodsConfiguration.instance().isCommandmentsEnabled())
		{
			sender.sendMessage(ChatColor.AQUA + "Holy food: " + ChatColor.YELLOW + LanguageManager.instance().getItemTypeName(GodManager.instance().getHolyFoodTypeForGod(godName)));
			sender.sendMessage(ChatColor.AQUA + "Unholy food: " + ChatColor.YELLOW + LanguageManager.instance().getItemTypeName(GodManager.instance().getUnholyFoodTypeForGod(godName)));

			sender.sendMessage(ChatColor.AQUA + "Holy creature: " + ChatColor.YELLOW + LanguageManager.instance().getMobTypeName(GodManager.instance().getHolyMobTypeForGod(godName)));
			sender.sendMessage(ChatColor.AQUA + "Unholy creature: " + ChatColor.YELLOW + LanguageManager.instance().getMobTypeName(GodManager.instance().getUnholyMobTypeForGod(godName)));
		}

		List<String> allyRelations = GodManager.instance().getAllianceRelations(godName);
		Object warRelations = GodManager.instance().getWarRelations(godName);

		if ((((List<?>) warRelations).size() > 0) || (allyRelations.size() > 0))
		{
			sender.sendMessage(ChatColor.AQUA + "Religious relations: ");
			for (String ally : GodManager.instance().getAllianceRelations(godName))
			{
				sender.sendMessage(ChatColor.GREEN + " Alliance with " + ChatColor.GOLD + ally);
			}
			List<String> enemies = GodManager.instance().getWarRelations(godName);
			for (String enemy : enemies)
			{
				sender.sendMessage(ChatColor.RED + " War with " + ChatColor.GOLD + enemy);
			}
		}
		
		if(GodsConfiguration.instance().isHolyLandEnabled())
		{
			int claimed = HolyLandManager.instance().getClaimedChunksForGod(godName);
			int maxclaim = HolyLandManager.instance().getMaximumChunksForGod(godName);
			sender.sendMessage(ChatColor.AQUA + "Holyland claimed: " + ChatColor.GOLD + claimed + "/" + maxclaim);					
		}
	}
}
