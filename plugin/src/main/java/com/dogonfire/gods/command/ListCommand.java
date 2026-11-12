package com.dogonfire.gods.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

/** /g list: the 15 most powerful gods that have believers (players and the console) */
public final class ListCommand extends SubCommand
{
	private static final int	SHOWN	= 15;

	private record Ranked(String name, int power, int believers)
	{
	}

	public ListCommand(GodsContext ctx)
	{
		super(ctx, "list", "gods.list");
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		// The console sees the list too, without a god of its own
		Player player = sender instanceof Player p ? p : null;
		List<Ranked> gods = new ArrayList<>();
		for (String godName : this.ctx.godService().getAllGods())
		{
			int believers = this.ctx.believerService().getBelieversForGod(godName).size();
			if (believers > 0)
			{
				gods.add(new Ranked(godName, (int) this.ctx.godService().getGodPower(godName), believers));
			}
		}
		if (gods.isEmpty())
		{
			throw new CommandException("command.list.none");
		}

		String playerGod = player != null ? godOf(player) : null;
		send(sender, "command.list.header");
		gods.sort(Comparator.comparingInt(Ranked::power).reversed());

		boolean playerGodShown = false;
		int rank = 1;
		for (Ranked god : gods.subList(0, Math.min(SHOWN, gods.size())))
		{
			TagResolver row = row(god, String.valueOf(rank));
			if (god.name().equals(playerGod))
			{
				playerGodShown = true;
				send(sender, "command.list.you", row);
			}
			else
			{
				send(sender, "command.list.line", row);
			}
			rank++;
		}

		if (playerGod != null && !playerGodShown)
		{
			rank = 1;
			for (Ranked god : gods)
			{
				if (god.name().equals(playerGod))
				{
					send(sender, "command.list.you-below", row(god, String.valueOf(rank)));
				}
				rank++;
			}
		}
		if (player != null)
		{
			this.ctx.godSpeech().sendInfo(player.getUniqueId(), "help.info-hint", 80);
		}
	}

	/**
	 * <rank> (its place), <god> (its name), <title>, <power>, <believers> and <god-rank> of a line of the list; <name> is
	 * the name too, for message files from before <god> (chat fonts aren't monospaced, so nothing is padded to line up)
	 */
	private TagResolver row(Ranked god, String rank)
	{
		return TagResolver.resolver(Messages.text("rank", rank), Messages.god(god.name()), Messages.text("name", god.name()),
				Messages.legacyText("title", this.ctx.godService().getTitleForGod(god.name())), Messages.text("power", god.power()), Messages.text("believers", god.believers()),
				Messages.text("god-rank", this.ctx.rankService().getRankName(god.name())));
	}
}
