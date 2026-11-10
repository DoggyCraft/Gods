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

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.managers.LanguageManager;

public class GodsCommandExecuter implements CommandExecutor
{
	private final GodsContext				ctx;
	private final Map<String, GodsCommand>	commandList;

	public GodsCommandExecuter(GodsContext ctx)
	{
		this.ctx = ctx;
		commandList = new TreeMap<String, GodsCommand>();
		registerCommand(new CommandAccept(ctx));
		registerCommand(new CommandAccess(ctx));
		registerCommand(new CommandAlliance(ctx));
		registerCommand(new CommandAttackHolyLand(ctx));
		registerCommand(new CommandBible(ctx));
		registerCommand(new CommandChat(ctx));
		registerCommand(new CommandCheck(ctx));
		registerCommand(new CommandDivorce(ctx));
		registerCommand(new CommandEditBible(ctx));
		registerCommand(new CommandFollowers(ctx));
		registerCommand(new CommandGiveHolyArtifact(ctx));
		registerCommand(new CommandHelp(ctx, this));
		registerCommand(new CommandHome(ctx));
		registerCommand(new CommandHunt(ctx));
		registerCommand(new CommandInfo(ctx));
		registerCommand(new CommandInvite(ctx));
		registerCommand(new CommandKick(ctx));
		registerCommand(new CommandLeaveGod(ctx));
		registerCommand(new CommandListGods(ctx));
		registerCommand(new CommandLove(ctx));
		registerCommand(new CommandMarriages(ctx));
		registerCommand(new CommandMarry(ctx));
		registerCommand(new CommandPrayFor(ctx));
		registerCommand(new CommandReject(ctx));
		registerCommand(new CommandReload(ctx));
		registerCommand(new CommandSetBible(ctx));
		registerCommand(new CommandSetDescription(ctx));
		registerCommand(new CommandSetHome(ctx));
		registerCommand(new CommandSetNeutralLand(ctx));
		registerCommand(new CommandSetPriest(ctx));
		registerCommand(new CommandPriestStartAttack(ctx));
		registerCommand(new CommandTogglePVP(ctx));
		registerCommand(new CommandToggleWar(ctx));
	}

	protected Collection<GodsCommand> getCommands()
	{
		return Collections.unmodifiableCollection(commandList.values());
	}

	protected GodsCommand getCommand(String name)
	{
		return commandList.get(name.toLowerCase());
	}

	protected void registerCommand(GodsCommand command)
	{
		if (commandList.containsKey(command.name.toLowerCase()))
			return;
		commandList.put(command.name.toLowerCase(), command);
	}

	private void CommandGods(CommandSender sender)
	{
		sender.sendMessage(ChatColor.YELLOW + "------------------ " + ctx.plugin().getDescription().getFullName() + " ------------------");
		sender.sendMessage(ChatColor.AQUA + "By DogOnFire");
		sender.sendMessage("" + ChatColor.AQUA);
		sender.sendMessage(ChatColor.AQUA + "There are currently " + ChatColor.WHITE + ctx.godService().getAllGods().size() + ChatColor.AQUA + " Gods and");
		sender.sendMessage("" + ChatColor.WHITE + ctx.believerService().getBelieverCount() + ChatColor.AQUA + " believers in " + ctx.settings().general().serverName());
		sender.sendMessage("" + ChatColor.AQUA);
		if (sender != null && sender instanceof Player)
		{
			Player player = (Player) sender;
			String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
			if (godName != null)
			{
				List<UUID> priests = ctx.priestService().getPriestsForGod(godName);
				if (priests == null || !priests.contains(player.getUniqueId()))
				{
					sender.sendMessage(ChatColor.WHITE + "You believe in " + ChatColor.GOLD + godName);
				}
				else
				{
					sender.sendMessage(ChatColor.WHITE + "You are the priest of " + ChatColor.GOLD + godName);
				}

				if (ctx.settings().prayers().enabled())
				{
					sender.sendMessage(ChatColor.WHITE + "You have " + ChatColor.GOLD + ctx.believerService().getPrayerPower(player.getUniqueId()) + ChatColor.WHITE + " prayer power");
				}
			}
			else
			{
				sender.sendMessage(ChatColor.RED + "You do not believe in any god");
			}
			if (ctx.settings().marriage().enabled())
			{
				String partnerName = ctx.marriageService().getPartnerName(player.getUniqueId());
				if (partnerName != null)
				{
					sender.sendMessage(ChatColor.WHITE + "You are married to " + ChatColor.GOLD + partnerName);
				}
			}
			sender.sendMessage("" + ChatColor.AQUA);
			ctx.godSpeech().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.GodsHelp, ChatColor.AQUA, 0, ChatColor.WHITE + "/g help", 80);
			ctx.godSpeech().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.AltarHelp, ChatColor.AQUA, 0, ChatColor.WHITE + "/g help altar", 160);

			if (ctx.settings().prayers().enabled())
			{
				ctx.godSpeech().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.PrayForHelp, ChatColor.AQUA, 0, ChatColor.WHITE + "/g prayfor", 240);
			}
		}
		sender.sendMessage(ChatColor.GREEN + "For the full command list, please use " + ChatColor.AQUA + "/gods help");
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if (args.length == 0)
		{
			CommandGods(sender);
			ctx.plugin().log(sender.getName() + " /gods");
			return true;
		}

		GodsCommand gCmd = getCommand(args[0]);
		
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
