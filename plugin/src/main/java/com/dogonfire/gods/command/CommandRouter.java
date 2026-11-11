package com.dogonfire.gods.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.service.Permissions;

/**
 * Handles {@code /g} (alias {@code /gods}): without arguments it shows the overview, otherwise it hands the
 * arguments to the {@link SubCommand} named by the first one (ignoring case). Before the command runs it checks, in
 * this order: the command is turned on ({@link SubCommand#disabledMessage()}), the permission, player-only, priest-only,
 * believer-only and the number of arguments. A thrown {@link CommandException} or {@link UsageException} becomes a
 * message. Help and tab completion come from the registered commands.
 */
public final class CommandRouter implements TabExecutor
{
	private final Messages					messages;
	private final Permissions				permissions;
	private final CommandChecks				checks;
	private final Consumer<CommandSender>	overview;
	/** By name, so help lists them alphabetically */
	private final Map<String, SubCommand>	commands	= new TreeMap<>();
	/** By name and alias, lower case */
	private final Map<String, SubCommand>	byName		= new HashMap<>();

	/**
	 * @param overview what {@code /g} alone shows
	 */
	public CommandRouter(Messages messages, Permissions permissions, CommandChecks checks, Consumer<CommandSender> overview)
	{
		this.messages = messages;
		this.permissions = permissions;
		this.checks = checks;
		this.overview = overview;
	}

	public void register(SubCommand command)
	{
		if (this.byName.containsKey(command.name()))
		{
			throw new IllegalArgumentException("Command " + command.name() + " is registered twice");
		}
		this.commands.put(command.name(), command);
		this.byName.put(command.name(), command);
		for (String alias : command.aliases())
		{
			this.byName.put(alias.toLowerCase(Locale.ROOT), command);
		}
	}

	/** Every command, by name */
	public List<SubCommand> commands()
	{
		return List.copyOf(this.commands.values());
	}

	/** The command with that name or alias (ignoring case), or null */
	public SubCommand find(String name)
	{
		return this.byName.get(name.toLowerCase(Locale.ROOT));
	}

	/** Whether the sender has the permission node; ops have every one */
	public boolean hasPermission(CommandSender sender, String permission)
	{
		if (sender.isOp())
		{
			return true;
		}
		if (permission == null)
		{
			return false;
		}
		if (sender instanceof Player player)
		{
			return this.permissions.hasPermission(player, permission);
		}
		return sender.hasPermission(permission);
	}

	/**
	 * Whether help and tab completion offer the command to the sender: it is turned on, they have its permission,
	 * are a player if it needs one and a priest if it is a priest command.
	 */
	public boolean canUse(CommandSender sender, SubCommand command)
	{
		if (!command.isAvailable() || !hasPermission(sender, command.permission()))
		{
			return false;
		}
		if (command.isPlayerOnly() && !(sender instanceof Player))
		{
			return false;
		}
		return !command.isPriestOnly() || this.checks.isPriest(((Player) sender).getUniqueId());
	}

	/** The commands the sender can use, by name */
	public List<SubCommand> commandsFor(CommandSender sender)
	{
		List<SubCommand> usable = new ArrayList<>();
		for (SubCommand command : this.commands.values())
		{
			if (canUse(sender, command))
			{
				usable.add(command);
			}
		}
		return usable;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if (args.length == 0)
		{
			this.overview.accept(sender);
			return true;
		}
		dispatch(sender, args[0], List.of(args).subList(1, args.length));
		return true;
	}

	/** Runs {@code /g <name> <args...>} for the sender */
	public void dispatch(CommandSender sender, String name, List<String> args)
	{
		SubCommand sub = find(name);
		if (sub == null)
		{
			this.messages.send(sender, "command.invalid");
			return;
		}
		String disabled = sub.disabledMessage();
		if (disabled != null)
		{
			this.messages.send(sender, disabled);
			return;
		}
		if (!hasPermission(sender, sub.permission()))
		{
			this.messages.send(sender, "command.no-permission");
			return;
		}
		if (sub.isPlayerOnly() && !(sender instanceof Player))
		{
			this.messages.send(sender, "command.player-only");
			return;
		}
		if (sender instanceof Player player)
		{
			if (sub.isPriestOnly() && !this.checks.isPriest(player.getUniqueId()))
			{
				this.messages.send(sender, sub.notPriestMessage());
				return;
			}
			if (sub.notBelieverMessage() != null && this.checks.godOf(player.getUniqueId()) == null)
			{
				this.messages.send(sender, sub.notBelieverMessage());
				return;
			}
		}
		if (!sub.acceptsArgs(args.size()))
		{
			sendUsage(sender, sub);
			return;
		}
		try
		{
			sub.execute(sender, args);
		}
		catch (CommandException e)
		{
			this.messages.send(sender, e.messageKey(), e.tags());
		}
		catch (UsageException e)
		{
			sendUsage(sender, sub);
		}
	}

	private void sendUsage(CommandSender sender, SubCommand sub)
	{
		this.messages.send(sender, "command.usage", sub.usage(this.messages));
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args)
	{
		List<String> options = new ArrayList<>();
		if (args.length == 1)
		{
			for (SubCommand sub : commandsFor(sender))
			{
				options.add(sub.name());
				options.addAll(sub.aliases());
			}
		}
		else if (args.length > 1)
		{
			SubCommand sub = find(args[0]);
			if (sub != null && canUse(sender, sub))
			{
				options.addAll(sub.tabComplete(sender, List.of(args).subList(1, args.length)));
			}
		}
		else
		{
			return List.of();
		}
		List<String> matches = StringUtil.copyPartialMatches(args[args.length - 1], options, new ArrayList<>());
		Collections.sort(matches);
		return matches;
	}
}
