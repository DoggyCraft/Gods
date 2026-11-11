package com.dogonfire.gods.command;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.model.MarriedCouple;

/** /g marriages: the 15 couples who loved each other last */
public final class MarriagesCommand extends SubCommand
{
	private static final int	SHOWN	= 15;

	public MarriagesCommand(GodsContext ctx)
	{
		super(ctx, "marriages", "gods.marriages");
		playerOnly();
	}

	@Override
	public String disabledMessage()
	{
		return this.ctx.settings().marriage().enabled() ? null : "command.disabled.marriages";
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		UUID playerId = ((Player) sender).getUniqueId();
		List<MarriedCouple> couples = this.ctx.marriageService().getMarriedCouples();
		if (couples.isEmpty())
		{
			throw new CommandException("command.marriages.none");
		}
		send(sender, "command.marriages.header");

		int rank = 1;
		boolean playerShown = false;
		Date now = new Date();
		for (MarriedCouple couple : couples.subList(0, Math.min(SHOWN, couples.size())))
		{
			TagResolver row = TagResolver.resolver(Messages.text("rank", rank), Messages.text("couple", String.format("%-30s", couple(couple))), Messages.component("when", timeAgo(couple.lastLove(), now)));
			if (isIn(couple, playerId))
			{
				playerShown = true;
				send(sender, "command.marriages.you", row);
			}
			else
			{
				send(sender, "command.marriages.line", row);
			}
			rank++;
		}

		if (!playerShown)
		{
			rank = 1;
			for (MarriedCouple couple : couples)
			{
				if (isIn(couple, playerId))
				{
					send(sender, "command.marriages.you-below", Messages.text("rank", rank), Messages.text("couple", String.format("%-40s", couple(couple))), Messages.component("when", timeAgo(couple.lastLove(), now)));
				}
				rank++;
			}
		}
	}

	private static boolean isIn(MarriedCouple couple, UUID playerId)
	{
		return couple.player1Id().equals(playerId) || couple.player2Id().equals(playerId);
	}

	/** "Anna &amp; Bob (Thor)" */
	private String couple(MarriedCouple couple)
	{
		return Messages.plain(this.ctx.messages().component("command.marriages.couple",
				Messages.text("first", nameOf(couple.player1Id())),
				Messages.text("second", nameOf(couple.player2Id())),
				Messages.god(couple.godName())));
	}
}
