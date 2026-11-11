package com.dogonfire.gods.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

/** /g list: the 15 most powerful gods that have believers */
public final class ListCommand extends SubCommand
{
	private static final int	SHOWN	= 15;

	private record Ranked(String name, int power, int believers)
	{
	}

	public ListCommand(GodsContext ctx)
	{
		super(ctx, "list", "gods.list");
		playerOnly();
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		Player player = (Player) sender;
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

		String playerGod = godOf(player);
		send(sender, "command.list.header");
		gods.sort(Comparator.comparingInt(Ranked::power).reversed());

		boolean playerGodShown = false;
		int rank = 1;
		for (Ranked god : gods.subList(0, Math.min(SHOWN, gods.size())))
		{
			TagResolver row = row(god, String.format("%2d", rank));
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
		this.ctx.godSpeech().sendInfo(player.getUniqueId(), "help.info-hint", 80);
	}

	/** <rank>, <name> (the name and title, padded to line up), <power> and <believers> of a line of the list */
	private TagResolver row(Ranked god, String rank)
	{
		String fullGodName = String.format("%-16s", god.name()) + "   " + String.format("%-16s", this.ctx.godService().getTitleForGod(god.name()));
		return TagResolver.resolver(Messages.text("rank", rank), Messages.legacyText("name", fullGodName), Messages.text("power", god.power()), Messages.text("believers", god.believers()));
	}
}
