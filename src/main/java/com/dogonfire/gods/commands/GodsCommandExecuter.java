package com.dogonfire.gods.commands;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.managers.MarriageManager;

public class GodsCommandExecuter implements CommandExecutor
{
	private static GodsCommandExecuter instance;

	public static GodsCommandExecuter instance()
	{
		if (instance == null)
			instance = new GodsCommandExecuter();
		return instance;
	}

	// TODO: Change all commands into subclasses and add them here
	private Map<String, GodsCommand> commandList;

	private GodsCommandExecuter()
	{
		commandList = new TreeMap<String, GodsCommand>();
		registerCommand(new CommandAccept());
		registerCommand(new CommandAccess());
		registerCommand(new CommandAlliance());
		registerCommand(new CommandAttackHolyLand());
		registerCommand(new CommandBible());
		registerCommand(new CommandChat());
		registerCommand(new CommandCheck());
		registerCommand(new CommandDivorce());
		registerCommand(new CommandEditBible());
		registerCommand(new CommandFollowers());
		registerCommand(new CommandGiveHolyArtifact());
		registerCommand(new CommandHelp());
		registerCommand(new CommandHome());
		registerCommand(new CommandHunt());
		registerCommand(new CommandInfo());
		registerCommand(new CommandInvite());
		registerCommand(new CommandKick());
		registerCommand(new CommandLeaveGod());
		registerCommand(new CommandListGods());
		registerCommand(new CommandLove());
		registerCommand(new CommandMarriages());
		registerCommand(new CommandMarry());
		registerCommand(new CommandPrayFor());
		registerCommand(new CommandReject());
		registerCommand(new CommandReload());
		registerCommand(new CommandSetBible());
		registerCommand(new CommandSetDescription());
		registerCommand(new CommandSetHome());
		registerCommand(new CommandSetNeutralLand());
		registerCommand(new CommandSetPriest());
		registerCommand(new CommandPriestStartAttack());
		registerCommand(new CommandTogglePVP());
		registerCommand(new CommandToggleWar());
	}

	protected Collection<GodsCommand> getCommands()
	{
		return Collections.unmodifiableCollection(commandList.values());
	}

	protected void registerCommand(GodsCommand command)
	{
		if (commandList.containsKey(command.name))
			return;
		commandList.put(command.name.toLowerCase(), command);
	}

	private void CommandGods(CommandSender sender)
	{
		sender.sendMessage(ChatColor.YELLOW + "------------------ " + Gods.instance().getDescription().getFullName() + " ------------------");
		sender.sendMessage(ChatColor.AQUA + "By DogOnFire");
		sender.sendMessage("" + ChatColor.AQUA);
		sender.sendMessage(ChatColor.AQUA + "There are currently " + ChatColor.WHITE + GodManager.instance().getAllGods().size() + ChatColor.AQUA + " Gods and");
		sender.sendMessage("" + ChatColor.WHITE + BelieverManager.instance().getBelievers().size() + ChatColor.AQUA + " believers in " + GodsConfiguration.instance().getServerName());
		sender.sendMessage("" + ChatColor.AQUA);
		if (sender != null && sender instanceof Player)
		{
			Player player = (Player) sender;
			String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());
			if (godName != null)
			{
				List<UUID> priests = GodManager.instance().getPriestsForGod(godName);
				if (priests == null || !priests.contains(player.getUniqueId()))
				{
					sender.sendMessage(ChatColor.WHITE + "You believe in " + ChatColor.GOLD + godName);
				}
				else
				{
					sender.sendMessage(ChatColor.WHITE + "You are the priest of " + ChatColor.GOLD + godName);
				}

				if (GodsConfiguration.instance().isPrayersEnabled())
				{
					sender.sendMessage(ChatColor.WHITE + "You have " + ChatColor.GOLD + BelieverManager.instance().getPrayerPower(player.getUniqueId()) + ChatColor.WHITE + " prayer power");
				}
			}
			else
			{
				sender.sendMessage(ChatColor.RED + "You do not believe in any god");
			}
			if (GodsConfiguration.instance().isMarriageEnabled())
			{
				String partnerName = MarriageManager.instance().getPartnerName(player.getUniqueId());
				if (partnerName != null)
				{
					sender.sendMessage(ChatColor.WHITE + "You are married to " + ChatColor.GOLD + partnerName);
				}
			}
			sender.sendMessage("" + ChatColor.AQUA);
			Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.GodsHelp, ChatColor.AQUA, 0, ChatColor.WHITE + "/g help", 80);
			Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.AltarHelp, ChatColor.AQUA, 0, ChatColor.WHITE + "/g help altar", 160);

			if (GodsConfiguration.instance().isPrayersEnabled())
			{
				Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.PrayForHelp, ChatColor.AQUA, 0, ChatColor.WHITE + "/g prayfor", 240);
			}
		}
		sender.sendMessage(ChatColor.GREEN + "For the full command list, please use " + ChatColor.AQUA + "/gods help");
	}

	// TODO: END OF FILE, REMOVE EVERYTHING BELOW HERE
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if (args.length == 0)
		{
			CommandGods(sender);
			Gods.instance().log(sender.getName() + " /gods");
			return true;
		}

		GodsCommand gCmd = commandList.get(args[0].toLowerCase());
		
		if (gCmd == null)
		{
			sender.sendMessage(ChatColor.RED + "Invalid Gods command!");
		}
		else
		{
			gCmd.onCommand(sender, label, args);
		}
		
		return true;
	}
}