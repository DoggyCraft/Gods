package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.managers.LanguageManager;

public class CommandPrayFor extends GodsCommand
{
	CommandPrayFor(GodsContext ctx)
	{
		super(ctx, "prayfor");
		this.permission = "gods.pray";
		this.parameters = "<Item/Health/Blessing/Quest>";
		this.description = "Pray to your God for something specific";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!ctx.settings().blessings().enabled())
		{
			sender.sendMessage(ChatColor.RED + "Blessings are not enabled on this server");
			return;
		}
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
		if (ctx.believerService().getGodForBeliever(((Player) sender).getUniqueId()) == null)
		{
			sender.sendMessage(stringNoGod);
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
		sender.sendMessage(ChatColor.YELLOW + "--------------- How to pray for things ---------------");
		sender.sendMessage(ChatColor.AQUA + "Pray for something specific from your God:");
		sender.sendMessage("");
		sender.sendMessage(ChatColor.WHITE + "/g prayfor item - " + ChatColor.AQUA + " pray for an item");
		sender.sendMessage(ChatColor.WHITE + "/g prayfor health - " + ChatColor.AQUA + " pray for health");
		sender.sendMessage(ChatColor.WHITE + "/g prayfor blessing - " + ChatColor.AQUA + " pray for a magical blessing");
		if (ctx.settings().quests().enabled())
		{
			sender.sendMessage(ChatColor.WHITE + "/g prayfor quest - " + ChatColor.AQUA + " pray for a quest");
		}
		sender.sendMessage("");
		sender.sendMessage(ChatColor.AQUA + "Note that you need prayer power to perform these prayers.");
	}

	private void prayForBlessing(CommandSender sender)
	{
		Player player = (Player) sender;
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		int currentPrayerPower = ctx.believerService().getPrayerPower(player.getUniqueId());
		if (currentPrayerPower < ctx.settings().prayers().blessingCost())
		{
			ctx.godSpeech().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.NotEnoughPrayerPower, ChatColor.AQUA, ctx.settings().prayers().blessingCost() - currentPrayerPower, godName, 1);
			return;
		}
		ctx.godSpeech().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.PrayedForBlessing, ChatColor.AQUA, ctx.settings().prayers().blessingCost(), godName, 1);
		if (!ctx.godManager().blessPlayer(godName, player.getUniqueId(), ctx.godManager().getGodPower(godName)))
		{
			ctx.godSpeech().sayToBeliever(godName, player.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverPrayerTooSoon);
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
			ctx.godSpeech().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.NotEnoughPrayerPower, ChatColor.AQUA, ctx.settings().prayers().healthCost() - currentPrayerPower, godName, 1);
			return;
		}
		if (ctx.believerService().hasRecentItemBlessing(player.getUniqueId()))
		{
			ctx.godSpeech().sayToBeliever(godName, player.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverPrayerTooSoon);
			return;
		}
		double healing = ctx.godManager().getHealthNeed(godName, player);
		if (healing <= 1.0D)
		{
			sender.sendMessage(ChatColor.AQUA + "You are already in good health.");
			return;
		}
		ctx.godSpeech().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.PrayedForHealth, ChatColor.AQUA, ctx.settings().prayers().healthCost(), godName, 1);
		ctx.godManager().healPlayer(godName, player);

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
			ctx.godSpeech().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.NotEnoughPrayerPower, ChatColor.AQUA, ctx.settings().prayers().itemCost() - currentPrayerPower, godName, 1);
			return;
		}
		if (ctx.believerService().hasRecentItemBlessing(player.getUniqueId()))
		{
			ctx.godSpeech().sayToBeliever(godName, player.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverPrayerRecentItemBlessing);
			return;
		}
		if (ctx.godManager().blessPlayerWithItem(godName, player) == null)
		{
			ctx.godSpeech().sayToBeliever(godName, player.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverPrayerWhenNoItemNeed);
		}
		else
		{
			ctx.godSpeech().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.PrayedForItem, ChatColor.AQUA, ctx.settings().prayers().itemCost(), godName, 1);
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
			ctx.godSpeech().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.NotEnoughPrayerPower, ChatColor.AQUA, ctx.settings().prayers().questCost() - currentPrayerPower, godName, 1);
			return;
		}
		if (ctx.questManager().hasQuest(godName))
		{
			sender.sendMessage(ChatColor.GOLD + godName + ChatColor.AQUA + " already has given a quest!");
			return;
		}
		ctx.godSpeech().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.PrayedForQuest, ChatColor.AQUA, ctx.settings().prayers().questCost(), godName, 1);
		if (ctx.questManager().generateQuest(godName))
		{
			ctx.believerService().increasePrayerPower(player.getUniqueId(), -ctx.settings().prayers().questCost());
		}
	}

}
