package com.dogonfire.gods.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.message.Messages;

public class CommandPrayFor extends GodsCommand
{
	CommandPrayFor(GodsContext ctx)
	{
		super(ctx, "prayfor");
		this.permission = "gods.pray";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!ctx.settings().blessings().enabled())
		{
			send(sender, "command.disabled.blessings");
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
		if (ctx.believerService().getGodForBeliever(((Player) sender).getUniqueId()) == null)
		{
			send(sender, NO_GOD);
			return;
		}
		if (args.length < 2)
		{
			prayForHelp(sender);
			return;
		}
		if (args[1].equalsIgnoreCase("health"))
		{
			prayForHealth(sender);
			return;
		}
		if (args[1].equalsIgnoreCase("item"))
		{
			prayForItem(sender);
			return;
		}
		if (args[1].equalsIgnoreCase("blessing"))
		{
			prayForBlessing(sender);
			return;
		}
		if (args[1].equalsIgnoreCase("quest") && ctx.settings().quests().enabled())
		{
			prayForQuest(sender);
			return;
		}
		prayForHelp(sender);
	}

	private void prayForHelp(CommandSender sender)
	{
		send(sender, "command.prayfor.help");
		if (ctx.settings().quests().enabled())
		{
			send(sender, "command.prayfor.help-quest");
		}
		send(sender, "command.prayfor.help-footer");
	}

	private void prayForBlessing(CommandSender sender)
	{
		Player player = (Player) sender;
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		int currentPrayerPower = ctx.believerService().getPrayerPower(player.getUniqueId());
		if (currentPrayerPower < ctx.settings().prayers().blessingCost())
		{
			ctx.godSpeech().sendInfo(player.getUniqueId(), "prayer.not-enough-power", 1, Messages.amount(ctx.settings().prayers().blessingCost() - currentPrayerPower), Messages.god(godName));
			return;
		}
		ctx.godSpeech().sendInfo(player.getUniqueId(), "prayer.prayed-for-blessing", 1, Messages.amount(ctx.settings().prayers().blessingCost()), Messages.god(godName));
		if (!ctx.blessingService().blessPlayer(godName, player.getUniqueId(), ctx.godService().getGodPower(godName)))
		{
			ctx.godSpeech().sayToBeliever(godName, player.getUniqueId(), Voice.BELIEVER_PRAYER_TOO_SOON);
		}
		else
		{
			ctx.believerService().increasePrayerPower(player.getUniqueId(), -ctx.settings().prayers().blessingCost());
		}
	}

	private void prayForHealth(CommandSender sender)
	{
		Player player = (Player) sender;
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		int currentPrayerPower = ctx.believerService().getPrayerPower(player.getUniqueId());
		if (currentPrayerPower < ctx.settings().prayers().healthCost())
		{
			ctx.godSpeech().sendInfo(player.getUniqueId(), "prayer.not-enough-power", 1, Messages.amount(ctx.settings().prayers().healthCost() - currentPrayerPower), Messages.god(godName));
			return;
		}
		if (ctx.believerService().hasRecentItemBlessing(player.getUniqueId()))
		{
			ctx.godSpeech().sayToBeliever(godName, player.getUniqueId(), Voice.BELIEVER_PRAYER_TOO_SOON);
			return;
		}
		double healing = ctx.blessingService().getHealthNeed(godName, player);
		if (healing <= 1.0D)
		{
			send(sender, "command.prayfor.healthy");
			return;
		}
		ctx.godSpeech().sendInfo(player.getUniqueId(), "prayer.prayed-for-health", 1, Messages.amount(ctx.settings().prayers().healthCost()), Messages.god(godName));
		ctx.blessingService().healPlayer(godName, player);

		ctx.believerService().increasePrayerPower(player.getUniqueId(), -ctx.settings().prayers().healthCost());
		ctx.believerService().setItemBlessingTime(player.getUniqueId());
	}

	private void prayForItem(CommandSender sender)
	{
		Player player = (Player) sender;
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		int currentPrayerPower = ctx.believerService().getPrayerPower(player.getUniqueId());
		if (currentPrayerPower < ctx.settings().prayers().itemCost())
		{
			ctx.godSpeech().sendInfo(player.getUniqueId(), "prayer.not-enough-power", 1, Messages.amount(ctx.settings().prayers().itemCost() - currentPrayerPower), Messages.god(godName));
			return;
		}
		if (ctx.believerService().hasRecentItemBlessing(player.getUniqueId()))
		{
			ctx.godSpeech().sayToBeliever(godName, player.getUniqueId(), Voice.BELIEVER_PRAYER_RECENT_ITEM_BLESSING);
			return;
		}
		if (ctx.blessingService().blessPlayerWithItem(godName, player) == null)
		{
			ctx.godSpeech().sayToBeliever(godName, player.getUniqueId(), Voice.BELIEVER_PRAYER_WHEN_NO_ITEM_NEED);
		}
		else
		{
			ctx.godSpeech().sendInfo(player.getUniqueId(), "prayer.prayed-for-item", 1, Messages.amount(ctx.settings().prayers().itemCost()), Messages.god(godName));
			ctx.believerService().increasePrayerPower(player.getUniqueId(), -ctx.settings().prayers().itemCost());
		}
	}

	private void prayForQuest(CommandSender sender)
	{
		Player player = (Player) sender;
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		int currentPrayerPower = ctx.believerService().getPrayerPower(player.getUniqueId());
		if (currentPrayerPower < ctx.settings().prayers().questCost())
		{
			ctx.godSpeech().sendInfo(player.getUniqueId(), "prayer.not-enough-power", 1, Messages.amount(ctx.settings().prayers().questCost() - currentPrayerPower), Messages.god(godName));
			return;
		}
		if (ctx.questService().hasQuest(godName))
		{
			send(sender, "command.prayfor.has-quest", Messages.god(godName));
			return;
		}
		ctx.godSpeech().sendInfo(player.getUniqueId(), "prayer.prayed-for-quest", 1, Messages.amount(ctx.settings().prayers().questCost()), Messages.god(godName));
		if (ctx.questService().generateQuest(godName))
		{
			ctx.believerService().increasePrayerPower(player.getUniqueId(), -ctx.settings().prayers().questCost());
		}
	}

}
