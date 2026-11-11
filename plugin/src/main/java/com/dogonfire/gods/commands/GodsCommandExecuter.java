package com.dogonfire.gods.commands;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

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
		ctx.messages().send(sender, "command.gods.header", Messages.text("plugin", ctx.plugin().getDescription().getFullName()), Messages.amount(ctx.godService().getAllGods().size()), Messages.text("believers", ctx.believerService().getBelieverCount()));
		if (sender != null && sender instanceof Player)
		{
			Player player = (Player) sender;
			String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
			if (godName != null)
			{
				List<UUID> priests = ctx.priestService().getPriestsForGod(godName);
				if (priests == null || !priests.contains(player.getUniqueId()))
				{
					ctx.messages().send(sender, "command.gods.believer", Messages.god(godName));
				}
				else
				{
					ctx.messages().send(sender, "command.gods.priest", Messages.god(godName));
				}

				if (ctx.settings().prayers().enabled())
				{
					ctx.messages().send(sender, "command.gods.prayer-power", Messages.amount(ctx.believerService().getPrayerPower(player.getUniqueId())));
				}
			}
			else
			{
				ctx.messages().send(sender, "command.gods.no-god");
			}
			if (ctx.settings().marriage().enabled())
			{
				String partnerName = ctx.marriageService().getPartnerName(player.getUniqueId());
				if (partnerName != null)
				{
					ctx.messages().send(sender, "command.gods.married", Messages.player(partnerName));
				}
			}
			ctx.messages().send(sender, "command.gods.separator");
			ctx.godSpeech().sendInfo(player.getUniqueId(), "help.commands-hint", 80);
			ctx.godSpeech().sendInfo(player.getUniqueId(), "help.altar-hint", 160);

			if (ctx.settings().prayers().enabled())
			{
				ctx.godSpeech().sendInfo(player.getUniqueId(), "help.prayfor-hint", 240);
			}
		}
		ctx.messages().send(sender, "command.gods.footer");
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
			ctx.messages().send(sender, "command.invalid");
		}
		else
		{
			gCmd.onCommand(sender, label, args);
		}
		
		return true;
	}
}
