package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.managers.QuestManager;

public class CommandPrayFor extends GodsCommand
{
	protected CommandPrayFor()
	{
		super("prayfor");
		this.permission = "gods.pray";
		this.parameters = "<Item/Health/Blessing/Quest>";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!GodsConfiguration.instance().isBlessingEnabled())
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
		if (args[1].equalsIgnoreCase("quest"))
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
		if (GodsConfiguration.instance().isQuestsEnabled())
		{
			sender.sendMessage(ChatColor.WHITE + "/g prayfor quest - " + ChatColor.AQUA + " pray for a quest");
		}
		sender.sendMessage("");
		sender.sendMessage(ChatColor.AQUA + "Note that you need prayer power to perform these prayers.");
	}

	private void prayForBlessing(CommandSender sender)
	{
		Player player = (Player) sender;
		String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());
		int currentPrayerPower = BelieverManager.instance().getPrayerPower(player.getUniqueId());
		if (currentPrayerPower < GodsConfiguration.instance().getPrayerPowerForBlessing())
		{
			Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.NotEnoughPrayerPower, ChatColor.AQUA, GodsConfiguration.instance().getPrayerPowerForBlessing() - currentPrayerPower, godName, 1);
			return;
		}
		Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.PrayedForBlessing, ChatColor.AQUA, GodsConfiguration.instance().getPrayerPowerForBlessing() - currentPrayerPower, godName, 1);
		if (!GodManager.instance().blessPlayer(godName, player.getUniqueId(), GodManager.instance().getGodPower(godName)))
		{
			GodManager.instance().godSayToBeliever(godName, player.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverPrayerTooSoon);
		}
		else
		{
			BelieverManager.instance().increasePrayerPower(player.getUniqueId(), -GodsConfiguration.instance().getPrayerPowerForBlessing());
		}
	}

	private void prayForHealth(CommandSender sender)
	{
		Player player = (Player) sender;
		String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());
		int currentPrayerPower = BelieverManager.instance().getPrayerPower(player.getUniqueId());
		if (currentPrayerPower < GodsConfiguration.instance().getPrayerPowerForHealth())
		{
			Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.NotEnoughPrayerPower, ChatColor.AQUA, GodsConfiguration.instance().getPrayerPowerForHealth() - currentPrayerPower, godName, 1);
			return;
		}
		if (BelieverManager.instance().hasRecentItemBlessing(player.getUniqueId()))
		{
			GodManager.instance().godSayToBeliever(godName, player.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverPrayerTooSoon);
			return;
		}
		Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.PrayedForHealth, ChatColor.AQUA, GodsConfiguration.instance().getPrayerPowerForBlessing() - currentPrayerPower, godName, 1);
		double healing = GodManager.instance().getHealthNeed(godName, player);
		if (healing > 1.0D)
		{
			GodManager.instance().healPlayer(godName, player, GodManager.instance().getHealthBlessing(godName));

			BelieverManager.instance().increasePrayerPower(player.getUniqueId(), -GodsConfiguration.instance().getPrayerPowerForHealth());
			BelieverManager.instance().setItemBlessingTime(player.getUniqueId());

			return;
		}
	}

	private void prayForItem(CommandSender sender)
	{
		Player player = (Player) sender;
		String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());
		int currentPrayerPower = BelieverManager.instance().getPrayerPower(player.getUniqueId());
		if (currentPrayerPower < GodsConfiguration.instance().getPrayerPowerForItem())
		{
			Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.NotEnoughPrayerPower, ChatColor.AQUA, GodsConfiguration.instance().getPrayerPowerForItem() - currentPrayerPower, godName, 1);
			return;
		}
		if (BelieverManager.instance().hasRecentItemBlessing(player.getUniqueId()))
		{
			GodManager.instance().godSayToBeliever(godName, player.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverPrayerRecentItemBlessing);
			return;
		}
		if (GodManager.instance().blessPlayerWithItem(godName, player) == null)
		{
			GodManager.instance().godSayToBeliever(godName, player.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverPrayerWhenNoItemNeed);
		}
		else
		{
			Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.PrayedForItem, ChatColor.AQUA, GodsConfiguration.instance().getPrayerPowerForItem() - currentPrayerPower, godName, 1);
			BelieverManager.instance().increasePrayerPower(player.getUniqueId(), -GodsConfiguration.instance().getPrayerPowerForItem());
		}
	}

	private void prayForQuest(CommandSender sender)
	{
		Player player = (Player) sender;
		String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());
		int currentPrayerPower = BelieverManager.instance().getPrayerPower(player.getUniqueId());
		if (currentPrayerPower < GodsConfiguration.instance().getPrayerPowerForQuest())
		{
			Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.NotEnoughPrayerPower, ChatColor.AQUA, GodsConfiguration.instance().getPrayerPowerForQuest() - currentPrayerPower, godName, 1);
			return;
		}
		if (QuestManager.instance().hasQuest(godName))
		{
			sender.sendMessage(ChatColor.GOLD + godName + ChatColor.AQUA + " already has given a quest!");
			return;
		}
		Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.PrayedForQuest, ChatColor.AQUA, GodsConfiguration.instance().getPrayerPowerForQuest() - currentPrayerPower, godName, 1);
		if (QuestManager.instance().generateQuest(godName))
		{
			BelieverManager.instance().increasePrayerPower(player.getUniqueId(), -GodsConfiguration.instance().getPrayerPowerForQuest());
		}
	}

}
