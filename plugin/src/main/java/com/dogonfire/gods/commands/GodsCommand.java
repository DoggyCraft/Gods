package com.dogonfire.gods.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

public abstract class GodsCommand
{
	// Messages for common responses to player commands
	protected static final String	NO_PERMISSION	= "command.no-permission";
	protected static final String	NO_GOD			= "command.no-god";
	protected static final String	PLAYER_ONLY	= "command.player-only";
	protected static final String	PRIEST_ONLY	= "command.priest-only";

	protected final GodsContext		ctx;
	protected final String			name;
	protected String				permission;

	/**
	 * True for commands only priests can use. Help and tab completion show
	 * them to priests only.
	 */
	protected boolean				priestOnly			= false;

	protected GodsCommand(GodsContext ctx, String name)
	{
		this.ctx = ctx;
		this.name = name;
	}

	/** Sends a message from messages.yml */
	protected void send(CommandSender to, String key, TagResolver... tags)
	{
		ctx.messages().send(to, key, tags);
	}

	/** What follows the command name, e.g. "&lt;player&gt;" (command.&lt;name&gt;.parameters); "" if nothing */
	protected Component parameters()
	{
		return ctx.messages().component("command." + name + ".parameters");
	}

	/** What the command does, for /g help (command.&lt;name&gt;.description) */
	protected Component description()
	{
		return ctx.messages().component("command." + name + ".description");
	}

	/** {@code <usage>}: the command name and its parameters */
	protected TagResolver usage()
	{
		Component parameters = parameters();
		Component usage = Component.text(name);
		if (!Messages.plain(parameters).isEmpty())
		{
			usage = usage.append(Component.text(" ")).append(parameters);
		}
		return Messages.component("usage", usage);
	}

	/** "3 days ago", "5 hours ago", "12 min ago": how long before {@code now} {@code then} was */
	protected Component timeAgo(java.util.Date then, java.util.Date now)
	{
		long millis = now.getTime() - then.getTime();
		long days = millis / 86400000L;
		long hours = millis / 3600000L;
		if (days > 0L)
		{
			return ctx.messages().component("time.days-ago", Messages.amount(days));
		}
		if (hours > 0L)
		{
			return ctx.messages().component("time.hours-ago", Messages.amount(hours));
		}
		return ctx.messages().component("time.minutes-ago", Messages.amount(millis / 60000L));
	}

	protected void sendUsage(CommandSender sender)
	{
		send(sender, "command.usage", usage());
	}

	/**
	 * Determines if someone has permission to use a command
	 *
	 * @param sender
	 *            the person you would like to check the permissions of
	 * @return true if they have permission, false otherwise
	 */
	protected final boolean hasPermission(CommandSender sender)
	{
		if (sender == null)
			return false;
		if (sender.isOp())
			return true;
		if (permission == null)
			return false;
		if (sender instanceof Player)
			return ctx.permissions().hasPermission((Player) sender, permission);
		return sender.hasPermission(permission);
	}

	/**
	 * Determines if help and tab completion should offer this command to
	 * someone: they have its permission, and are a priest if it is a priest
	 * command.
	 */
	protected final boolean isAvailableTo(CommandSender sender, boolean isPriest)
	{
		return hasPermission(sender) && (!priestOnly || isPriest);
	}

	/**
	 * The onCommand method for the Command classes. This is to be called when
	 * you want a command to attempt execution
	 *
	 * @param sender
	 *            the user issuing the command
	 * @param command
	 *            the name of the command
	 * @param args
	 *            the arguments that the user passed to the command
	 */
	public abstract void onCommand(CommandSender sender, String command, String... args);
}
