package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

public abstract class GodsCommand
{
	// These are the Strings that will be used for common responses to player
	// commands
	protected static final String	stringNoPermission	= ChatColor.RED + "You do not have permission to perform this command.";
	protected static final String	stringNoGod			= ChatColor.RED + "You do not believe in a God.";
	protected static final String	stringPlayerOnly	= ChatColor.RED + "This is a Player-Only command.";
	protected static final String	stringPreistOnly	= ChatColor.RED + "This is a Priest-Only command.";

	protected final GodsContext		ctx;
	protected final String			name;
	protected String				permission, parameters, description;

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

	protected void sendUsage(CommandSender sender)
	{
		sender.sendMessage(String.format("%sUsage: /gods %s %s", ChatColor.GREEN.toString(), name, parameters == null ? "" : parameters).trim());
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
