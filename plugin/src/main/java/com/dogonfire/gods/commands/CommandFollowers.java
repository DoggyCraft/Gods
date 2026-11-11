package com.dogonfire.gods.commands;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

public class CommandFollowers extends GodsCommand
{
	CommandFollowers(GodsContext ctx)
	{
		super(ctx, "followers");
		this.permission = "gods.followers";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!hasPermission(sender))
		{
			send(sender, NO_PERMISSION);
			return;
		}
		if (sender instanceof Player == false)
		{
			send(sender, PLAYER_ONLY);
			return;
		}
		Player player = (Player) sender;
		List<Believer> believers = new ArrayList<Believer>();
		String playerGod = null;

		String godName = "";
		if (args.length >= 2)
		{
			godName = ctx.godService().formatGodName(args[1]);
		}
		else
		{
			godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		}

		if (godName == null)
		{
			send(sender, "command.not-believer");
			return;
		}

		Set<UUID> list = ctx.believerService().getBelieversForGod(godName);

		for (UUID believerId : list)
		{
			Date lastPrayer = ctx.believerService().getLastPrayerTime(believerId);
			believers.add(new Believer(believerId, lastPrayer));
		}

		if (believers.size() == 0)
		{
			send(sender, "command.followers.none", Messages.god(godName));
			return;
		}

		playerGod = ctx.believerService().getGodForBeliever(player.getUniqueId());
		send(sender, "command.followers.header", Messages.god(godName));
		Collections.sort(believers, new BelieversComparator());

		int l = believers.size();

		List<Believer> believersList = believers;

		if (l > 15)
		{
			believersList = believersList.subList(0, 15);
		}

		boolean playerShown = false;

		Date thisDate = new Date();

		for (Believer believer : believersList)
		{
			String believerName = ctx.plugin().getServer().getOfflinePlayer(believer.believerId).getName();
			TagResolver row = TagResolver.resolver(Messages.player(String.format("%-20s", believerName)), Messages.component("when", timeAgo(believer.lastPrayer, thisDate)));

			if (playerGod != null && (believer.believerId.equals(player.getUniqueId())))
			{
				playerShown = true;
				send(sender, "command.followers.you", row);
			}
			else
			{
				send(sender, "command.followers.line", row);
			}
		}

		if ((playerGod != null) && (!playerShown))
		{
			for (Believer believer : believers)
			{
				String believerName = ctx.plugin().getServer().getOfflinePlayer(believer.believerId).getName();

				if ((playerGod != null) && (believer.believerId.equals(player.getUniqueId())))
				{
					send(sender, "command.followers.you-below", Messages.player(String.format("%-20s", believerName)), Messages.text("when", believer.lastPrayer));
				}
			}
		}
	}
}

class Believer
{
	public UUID	believerId;
	public Date	lastPrayer;

	Believer(UUID believerId, Date lastPrayer)
	{
		this.believerId = believerId;
		this.lastPrayer = lastPrayer;
	}
}

class BelieversComparator implements Comparator<Object>
{
	public BelieversComparator()
	{
	}

	@Override
	public int compare(Object object1, Object object2)
	{
		Believer b1 = (Believer) object1;
		Believer b2 = (Believer) object2;

		return Long.compare(b2.lastPrayer.getTime(), b1.lastPrayer.getTime());
	}
}
