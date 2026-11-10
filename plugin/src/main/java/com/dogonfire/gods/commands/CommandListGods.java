package com.dogonfire.gods.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.managers.LanguageManager;

public class CommandListGods extends GodsCommand
{
	CommandListGods(GodsContext ctx)
	{
		super(ctx, "list");
		this.permission = "gods.list";
		this.description = "List the Gods of this server";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!hasPermission(sender))
		{
			sender.sendMessage(stringNoPermission);
			return;
		}
		if (sender instanceof Player == false)
		{
			sender.sendMessage(stringPlayerOnly);
			return;
		}
		Player player = (Player) sender;
		List<God> gods = new ArrayList<God>();
		String playerGod = null;

		List<String> list = ctx.godManager().getTopGods();
		for (String godName : list)
		{
			int power = (int) ctx.godManager().getGodPower(godName);

			int believers = ctx.believerService().getBelieversForGod(godName).size();
			if (believers > 0)
			{
				gods.add(new God(godName, power, believers));
			}
		}

		if (gods.size() == 0)
		{
			sender.sendMessage(ChatColor.GOLD + "There are no Gods in " + ctx.settings().general().serverName() + "!");
			return;
		}

		playerGod = ctx.believerService().getGodForBeliever(player.getUniqueId());
		sender.sendMessage(ChatColor.YELLOW + "--------- The Gods of " + ctx.settings().general().serverName() + " ---------");

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
			String fullGodName = String.format("%-16s", new Object[] { god.name }) + "   " + String.format("%-16s", new Object[] { ctx.godManager().getTitleForGod(god.name) });
			if (sender != null)
			{
				if ((playerGod != null) && (god.name.equals(playerGod)))
				{
					playerGodShown = true;
					sender.sendMessage(ChatColor.GOLD + String.format("%2d", new Object[] { Integer.valueOf(n) }) + " - " +

							fullGodName + ChatColor.GOLD + String.format("%-2s", new StringBuilder().append(" Power ").append(god.power).toString()) + String.format("%-2s", new StringBuilder().append(" Believers ").append(god.believers).toString()));
				}
				else
				{
					sender.sendMessage(ChatColor.YELLOW + String.format("%2d", new Object[] { Integer.valueOf(n) }) + ChatColor.AQUA + " - " + fullGodName + ChatColor.GOLD + String.format("%-2s", new StringBuilder().append(" Power ").append(god.power).toString())
							+ String.format("%-2s", new StringBuilder().append(" Believers ").append(god.believers).toString()));
				}
			}
			else
			{
				ctx.plugin().log(String.format("%2d", new Object[] { Integer.valueOf(n) }) + " - " + fullGodName + ChatColor.GOLD + String.format("%-2s", new StringBuilder().append(" Mood ").append(ctx.godManager().getExactMoodForGod(god.name)).toString())
						+ String.format("%-2s", new StringBuilder().append(" Power ").append(god.power).toString()) + String.format("%-2s", new StringBuilder().append(" Believers ").append(god.believers).toString()));
			}
			n++;
		}

		n = 1;

		if ((playerGod != null) && (!playerGodShown))
		{
			for (God god : gods)
			{
				String fullGodName = String.format("%-16s", new Object[] { god.name }) + "   " + String.format("%-16s", new Object[] { ctx.godManager().getTitleForGod(god.name) });
				if ((playerGod != null) && (god.name.equals(playerGod)))
				{
					playerGodShown = true;
					sender.sendMessage("" + ChatColor.GOLD + n + " - " + fullGodName + String.format("%-2s", new StringBuilder().append(" Power ").append(god.power).toString()) + String.format("%-2s", new StringBuilder().append(" Believers ").append(god.believers).toString()));
				}
				n++;
			}
		}
		if (sender != null)
		{
			ctx.godSpeech().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.InfoHelp, ChatColor.AQUA, 0, ChatColor.WHITE + "/g info <godname>", 80);
		}

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
