package com.dogonfire.gods.commands;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import java.util.Date;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.model.MarriedCouple;

public class CommandMarriages extends GodsCommand
{
	CommandMarriages(GodsContext ctx)
	{
		super(ctx, "marriages");
		this.permission = "gods.marriages";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!ctx.settings().marriage().enabled())
		{
			send(sender, "command.disabled.marriages");
			return;
		}
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
		List<MarriedCouple> couples = ctx.marriageService().getMarriedCouples();
		if (couples.size() == 0)
		{
			send(sender, "command.marriages.none");
			return;
		}
		send(sender, "command.marriages.header");
		int l = couples.size();

		List<MarriedCouple> couplesList = couples;
		if (l > 15)
		{
			couplesList = couplesList.subList(0, 15);
		}

		int n = 1;
		boolean playerShown = false;
		Date thisDate = new Date();

		for (MarriedCouple couple : couplesList)
		{
			TagResolver row = TagResolver.resolver(Messages.text("rank", n), Messages.text("couple", String.format("%-30s", couple(couple))), Messages.component("when", timeAgo(couple.lastLove(), thisDate)));
			if ((couple.player1Id().equals(player.getUniqueId())) || (couple.player2Id().equals(player.getUniqueId())))
			{
				playerShown = true;
				send(sender, "command.marriages.you", row);
			}
			else
			{
				send(sender, "command.marriages.line", row);
			}
			n++;
		}

		n = 1;

		if (!playerShown)
		{
			for (MarriedCouple couple : couples)
			{

				if ((couple.player1Id().equals(player.getUniqueId())) || (couple.player2Id().equals(player.getUniqueId())))
				{
					send(sender, "command.marriages.you-below", Messages.text("rank", n), Messages.text("couple", String.format("%-40s", couple(couple))), Messages.text("when", couple.lastLove()));
				}
				n++;
			}
		}
	}

	/** "Anna & Bob (Thor)" */
	private String couple(MarriedCouple couple)
	{
		return Messages.plain(ctx.messages().component("command.marriages.couple",
				Messages.text("first", ctx.plugin().getServer().getOfflinePlayer(couple.player1Id()).getName()),
				Messages.text("second", ctx.plugin().getServer().getOfflinePlayer(couple.player2Id()).getName()),
				Messages.god(couple.godName())));
	}
}
