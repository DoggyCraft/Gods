package com.dogonfire.gods.command;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.model.GodRank;

/**
 * One {@code /g <name>} command. The constructor says who may use it ({@link #playerOnly()}, {@link #believerOnly()},
 * {@link #priestOnly()}, {@link #args(int, int)}); the {@link CommandRouter} checks all of that, the permission and
 * {@link #disabledMessage()} before {@link #execute} is called. /g help shows {@code command.<name>.parameters} and
 * {@code command.<name>.description} from messages.yml.
 */
public abstract class SubCommand
{
	/** No upper limit on the number of arguments */
	protected static final int	ANY				= Integer.MAX_VALUE;

	/** The context; null only in tests of the router */
	protected final GodsContext	ctx;
	private final String		name;
	private final String		permission;
	private List<String>		aliases			= List.of();
	private boolean				playerOnly;
	private String				notBeliever;
	private String				notPriest;
	private int					minArgs;
	private int					maxArgs			= ANY;

	protected SubCommand(GodsContext ctx, String name, String permission)
	{
		this.ctx = ctx;
		this.name = name;
		this.permission = permission;
	}

	// What the constructor of a command says about it

	protected final void aliases(String... aliases)
	{
		this.aliases = List.of(aliases);
	}

	/** Only players can use it (the router answers command.player-only to the console) */
	protected final void playerOnly()
	{
		this.playerOnly = true;
	}

	/** Only players who believe in a god can use it; others get command.no-god */
	protected final void believerOnly()
	{
		believerOnly("command.no-god");
	}

	/** Only players who believe in a god can use it; others get the message */
	protected final void believerOnly(String messageKey)
	{
		playerOnly();
		this.notBeliever = messageKey;
	}

	/** Only priests can use it (who also believe); others get command.priest-only. Help shows it to priests only. */
	protected final void priestOnly()
	{
		priestOnly("command.priest-only");
	}

	/** Only priests can use it (who also believe); others get the message. Help shows it to priests only. */
	protected final void priestOnly(String messageKey)
	{
		playerOnly();
		this.notPriest = messageKey;
		if (this.notBeliever == null)
		{
			this.notBeliever = "command.no-god";
		}
	}

	/** How many arguments it takes after its name ({@link #ANY}: no limit); others get the usage */
	protected final void args(int min, int max)
	{
		this.minArgs = min;
		this.maxArgs = max;
	}

	// What the router asks

	public final String name()
	{
		return this.name;
	}

	public final List<String> aliases()
	{
		return this.aliases;
	}

	public final String permission()
	{
		return this.permission;
	}

	/**
	 * Whether the sender may use this command, given a check of single permission nodes (ops have every node). By
	 * default: the command's {@link #permission()}. A command with permissions per action overrides it.
	 */
	public boolean permitted(Predicate<String> hasNode)
	{
		return this.permission == null || hasNode.test(this.permission);
	}

	public final boolean isPlayerOnly()
	{
		return this.playerOnly;
	}

	/** The message for players who don't believe in a god, or null if they can use it too */
	public final String notBelieverMessage()
	{
		return this.notBeliever;
	}

	/** The message for players who aren't priests, or null if it isn't a priest command */
	public final String notPriestMessage()
	{
		return this.notPriest;
	}

	public final boolean isPriestOnly()
	{
		return this.notPriest != null;
	}

	public final boolean acceptsArgs(int count)
	{
		return count >= this.minArgs && count <= this.maxArgs;
	}

	/**
	 * Null when the command can be used; otherwise the message saying why not, when its feature is turned off in
	 * config.yml (command.disabled.*). A disabled command isn't offered in help and tab completion.
	 */
	public String disabledMessage()
	{
		return null;
	}

	public final boolean isAvailable()
	{
		return disabledMessage() == null;
	}

	/** {@code <usage>}: the command name and its parameters (command.&lt;name&gt;.parameters) */
	public final TagResolver usage(Messages messages)
	{
		Component parameters = messages.component("command." + this.name + ".parameters");
		Component usage = Component.text(this.name);
		if (!Messages.plain(parameters).isEmpty())
		{
			usage = usage.append(Component.text(" ")).append(parameters);
		}
		return Messages.component("usage", usage);
	}

	/** {@code <description>}: what the command does (command.&lt;name&gt;.description) */
	public final TagResolver description(Messages messages)
	{
		return Messages.component("description", messages.component("command." + this.name + ".description"));
	}

	/**
	 * Runs the command. {@code args} holds what follows the command name. The sender passed every check the
	 * constructor asked for: a player-only command gets a {@link Player}, a believer-only one a player with a god.
	 */
	public abstract void execute(CommandSender sender, List<String> args) throws CommandException, UsageException;

	/** Suggestions for the last argument ({@code args} holds everything typed after the command name) */
	public List<String> tabComplete(CommandSender sender, List<String> args)
	{
		return List.of();
	}

	// Helpers for commands

	protected final void send(CommandSender to, String key, TagResolver... tags)
	{
		this.ctx.messages().send(to, key, tags);
	}

	/** The god the player believes in, or null */
	protected final String godOf(Player player)
	{
		return this.ctx.believerService().getGodForBeliever(player.getUniqueId());
	}

	/** The god of a player of a {@link #believerOnly()} command */
	protected final String believersGod(Player player) throws CommandException
	{
		String godName = godOf(player);
		if (godName == null)
		{
			throw new CommandException(this.notBeliever != null ? this.notBeliever : "command.no-god");
		}
		return godName;
	}

	/**
	 * Stops the command ({@code command.rank-locked}, naming the rank as the god's type calls it) when the god's rank
	 * hasn't unlocked a feature yet (config.yml's {@code ranks.unlocks})
	 */
	protected final void requireRank(String godName, GodRank needed) throws CommandException
	{
		if (!this.ctx.rankService().hasUnlocked(godName, needed))
		{
			throw new CommandException("command.rank-locked", Messages.god(godName), Messages.text("rank", this.ctx.rankService().rankName(godName, needed)));
		}
	}

	/** The online player with exactly that name (ignoring case), or else the message */
	protected final Player onlinePlayer(String name, String notFoundKey, TagResolver... tags) throws CommandException
	{
		Player player = this.ctx.plugin().getServer().getPlayerExact(name);
		if (player == null)
		{
			throw new CommandException(notFoundKey, tags);
		}
		return player;
	}

	/**
	 * A believer of the god with that name (ignoring case), online or not, or null. Only the god's known believers
	 * are looked at, so an unknown name never asks Mojang.
	 */
	protected final UUID knownBeliever(String godName, String name)
	{
		if (godName == null)
		{
			return null;
		}
		for (UUID believerId : this.ctx.believerService().getBelieversForGod(godName))
		{
			OfflinePlayer offlinePlayer = this.ctx.plugin().getServer().getOfflinePlayer(believerId);
			if (name.equalsIgnoreCase(offlinePlayer.getName()))
			{
				return believerId;
			}
		}
		return null;
	}

	/** The name of a player we know by UUID (null if the server has never seen them) */
	protected final String nameOf(UUID playerId)
	{
		return this.ctx.plugin().getServer().getOfflinePlayer(playerId).getName();
	}

	/** Names of the online players the sender can see, for tab completion */
	protected final List<String> onlinePlayerNames(CommandSender sender)
	{
		List<String> names = new ArrayList<>();
		for (Player player : this.ctx.plugin().getServer().getOnlinePlayers())
		{
			if (!(sender instanceof Player viewer) || viewer.canSee(player))
			{
				names.add(player.getName());
			}
		}
		return names;
	}

	/** Names of the believers of a god, for tab completion */
	protected final List<String> believerNames(String godName)
	{
		List<String> names = new ArrayList<>();
		if (godName != null)
		{
			for (UUID believerId : this.ctx.believerService().getBelieversForGod(godName))
			{
				String name = nameOf(believerId);
				if (name != null)
				{
					names.add(name);
				}
			}
		}
		return names;
	}

	protected final List<String> godNames()
	{
		return this.ctx.godService().getAllGods();
	}

	/** "3 days ago", "5 hours ago", "12 min ago": how long before {@code now} {@code then} was */
	protected final Component timeAgo(Date then, Date now)
	{
		long millis = now.getTime() - then.getTime();
		long days = millis / 86400000L;
		long hours = millis / 3600000L;
		if (days > 0L)
		{
			return this.ctx.messages().component("time.days-ago", Messages.amount(days));
		}
		if (hours > 0L)
		{
			return this.ctx.messages().component("time.hours-ago", Messages.amount(hours));
		}
		return this.ctx.messages().component("time.minutes-ago", Messages.amount(millis / 60000L));
	}
}
