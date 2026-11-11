package com.dogonfire.gods.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

/** /g followers [god]: the 15 believers of a god (the player's own by default) who prayed last */
public final class FollowersCommand extends SubCommand
{
	private static final int	SHOWN	= 15;

	private record Follower(UUID believerId, Date lastPrayer)
	{
	}

	public FollowersCommand(GodsContext ctx)
	{
		super(ctx, "followers", "gods.followers");
		playerOnly();
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		Player player = (Player) sender;
		String godName = args.isEmpty() ? godOf(player) : this.ctx.godService().formatGodName(args.getFirst());
		if (godName == null)
		{
			throw new CommandException("command.no-god");
		}

		List<Follower> followers = new ArrayList<>();
		for (UUID believerId : this.ctx.believerService().getBelieversForGod(godName))
		{
			followers.add(new Follower(believerId, this.ctx.believerService().getLastPrayerTime(believerId)));
		}
		if (followers.isEmpty())
		{
			throw new CommandException("command.followers.none", Messages.god(godName));
		}

		boolean believes = godOf(player) != null;
		send(sender, "command.followers.header", Messages.god(godName));
		followers.sort(Comparator.comparing(Follower::lastPrayer).reversed());

		boolean playerShown = false;
		Date now = new Date();
		for (Follower follower : followers.subList(0, Math.min(SHOWN, followers.size())))
		{
			TagResolver row = TagResolver.resolver(Messages.player(String.format("%-20s", nameOf(follower.believerId()))), Messages.component("when", timeAgo(follower.lastPrayer(), now)));
			if (believes && follower.believerId().equals(player.getUniqueId()))
			{
				playerShown = true;
				send(sender, "command.followers.you", row);
			}
			else
			{
				send(sender, "command.followers.line", row);
			}
		}

		if (believes && !playerShown)
		{
			for (Follower follower : followers)
			{
				if (follower.believerId().equals(player.getUniqueId()))
				{
					send(sender, "command.followers.you-below", Messages.player(String.format("%-20s", nameOf(follower.believerId()))), Messages.component("when", timeAgo(follower.lastPrayer(), now)));
				}
			}
		}
	}

	@Override
	public List<String> tabComplete(CommandSender sender, List<String> args)
	{
		return args.size() == 1 ? godNames() : List.of();
	}
}
