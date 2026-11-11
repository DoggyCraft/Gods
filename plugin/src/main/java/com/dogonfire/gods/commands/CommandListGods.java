package com.dogonfire.gods.commands;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

public class CommandListGods extends GodsCommand
{
	CommandListGods(GodsContext ctx)
	{
		super(ctx, "list");
		this.permission = "gods.list";
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
		List<God> gods = new ArrayList<God>();
		String playerGod = null;

		List<String> list = ctx.godService().getAllGods();
		for (String godName : list)
		{
			int power = (int) ctx.godService().getGodPower(godName);

			int believers = ctx.believerService().getBelieversForGod(godName).size();
			if (believers > 0)
			{
				gods.add(new God(godName, power, believers));
			}
		}

		if (gods.size() == 0)
		{
			send(sender, "command.list.none");
			return;
		}

		playerGod = ctx.believerService().getGodForBeliever(player.getUniqueId());
		send(sender, "command.list.header");

		Collections.sort(gods, new TopGodsComparator());

		int l = gods.size();

		List<God> topGods = gods;
		if (l > 15)
		{
			topGods = topGods.subList(0, 15);
		}

		int n = 1;
		boolean playerGodShown = false;

		for (God god : topGods)
		{
			TagResolver row = row(god, String.format("%2d", n));
			if ((playerGod != null) && (god.name.equals(playerGod)))
			{
				playerGodShown = true;
				send(sender, "command.list.you", row);
			}
			else
			{
				send(sender, "command.list.line", row);
			}
			n++;
		}

		n = 1;

		if ((playerGod != null) && (!playerGodShown))
		{
			for (God god : gods)
			{
				if ((playerGod != null) && (god.name.equals(playerGod)))
				{
					playerGodShown = true;
					send(sender, "command.list.you-below", row(god, String.valueOf(n)));
				}
				n++;
			}
		}
		if (sender != null)
		{
			ctx.godSpeech().sendInfo(player.getUniqueId(), "help.info-hint", 80);
		}

	}

	/** <rank>, <name> (the name and title, padded to line up), <power> and <believers> of a line of the list */
	private TagResolver row(God god, String rank)
	{
		String fullGodName = String.format("%-16s", god.name) + "   " + String.format("%-16s", ctx.godService().getTitleForGod(god.name));
		return TagResolver.resolver(Messages.text("rank", rank), Messages.legacyText("name", fullGodName), Messages.text("power", god.power), Messages.text("believers", god.believers));
	}
}

class God
{
	public int		power;
	public String	name;
	public int		believers;

	God(String godName, int godPower, int godbelievers)
	{
		this.power = godPower;
		this.name = new String(godName);
		this.believers = godbelievers;
	}
}

class TopGodsComparator implements Comparator<Object>
{
	public TopGodsComparator()
	{
	}

	@Override
	public int compare(Object object1, Object object2)
	{
		God g1 = (God) object1;
		God g2 = (God) object2;

		return g2.power - g1.power;
	}
}
