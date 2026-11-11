package com.dogonfire.gods.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.message.Voice;

/** /g prayfor item/health/blessing/quest: spends prayer power on something from the player's god */
public final class PrayForCommand extends SubCommand
{
	public PrayForCommand(GodsContext ctx)
	{
		super(ctx, "prayfor", "gods.pray");
		believerOnly();
	}

	@Override
	public String disabledMessage()
	{
		return disabledFor(null, this.ctx.settings().prayers().enabled(), this.ctx.settings().blessings().enabled(), this.ctx.settings().quests().enabled());
	}

	/**
	 * Why the player can't pray for {@code kind} (item, health, blessing or quest; null for the command itself), as a
	 * message key, or null if they can: the command needs prayers, and something to pray for; items, health and
	 * blessings are blessings, a quest needs quests.
	 */
	static String disabledFor(String kind, boolean prayers, boolean blessings, boolean quests)
	{
		if (!prayers)
		{
			return "command.disabled.prayers";
		}
		if (kind == null)
		{
			return blessings || quests ? null : "command.disabled.blessings";
		}
		return switch (kind.toLowerCase(Locale.ROOT))
		{
		case "item", "health", "blessing" -> blessings ? null : "command.disabled.blessings";
		case "quest" -> quests ? null : "command.disabled.quests";
		default -> null;
		};
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		Player player = (Player) sender;
		String godName = believersGod(player);
		String what = args.isEmpty() ? "" : args.getFirst();
		String disabled = disabledFor(what, prayers().enabled(), this.ctx.settings().blessings().enabled(), this.ctx.settings().quests().enabled());
		if (disabled != null)
		{
			throw new CommandException(disabled);
		}
		if (what.equalsIgnoreCase("health"))
		{
			prayForHealth(player, godName);
		}
		else if (what.equalsIgnoreCase("item"))
		{
			prayForItem(player, godName);
		}
		else if (what.equalsIgnoreCase("blessing"))
		{
			prayForBlessing(player, godName);
		}
		else if (what.equalsIgnoreCase("quest"))
		{
			prayForQuest(player, godName);
		}
		else
		{
			// Only what can be prayed for
			send(sender, this.ctx.settings().blessings().enabled() ? "command.prayfor.help" : "command.prayfor.help-header");
			if (this.ctx.settings().quests().enabled())
			{
				send(sender, "command.prayfor.help-quest");
			}
			send(sender, "command.prayfor.help-footer");
		}
	}

	@Override
	public List<String> tabComplete(CommandSender sender, List<String> args)
	{
		if (args.size() != 1)
		{
			return List.of();
		}
		List<String> options = new ArrayList<>();
		if (this.ctx.settings().blessings().enabled())
		{
			options.addAll(List.of("item", "health", "blessing"));
		}
		if (this.ctx.settings().quests().enabled())
		{
			options.add("quest");
		}
		return options;
	}

	/** Whether the player has the prayer power; if not, tells them how much they lack */
	private boolean canAfford(UUID playerId, String godName, int cost)
	{
		int prayerPower = this.ctx.believerService().getPrayerPower(playerId);
		if (prayerPower < cost)
		{
			this.ctx.godSpeech().sendInfo(playerId, "prayer.not-enough-power", 1, Messages.amount(cost - prayerPower), Messages.god(godName));
			return false;
		}
		return true;
	}

	private void prayForBlessing(Player player, String godName)
	{
		UUID playerId = player.getUniqueId();
		int cost = prayers().blessingCost();
		if (!canAfford(playerId, godName, cost))
		{
			return;
		}
		this.ctx.godSpeech().sendInfo(playerId, "prayer.prayed-for-blessing", 1, Messages.amount(cost), Messages.god(godName));
		if (!this.ctx.blessingService().blessPlayer(godName, playerId, this.ctx.godService().getGodPower(godName)))
		{
			this.ctx.godSpeech().sayToBeliever(godName, playerId, Voice.BELIEVER_PRAYER_TOO_SOON);
		}
		else
		{
			this.ctx.believerService().increasePrayerPower(playerId, -cost);
		}
	}

	private void prayForHealth(Player player, String godName) throws CommandException
	{
		UUID playerId = player.getUniqueId();
		int cost = prayers().healthCost();
		if (!canAfford(playerId, godName, cost))
		{
			return;
		}
		if (this.ctx.believerService().hasRecentItemBlessing(playerId))
		{
			this.ctx.godSpeech().sayToBeliever(godName, playerId, Voice.BELIEVER_PRAYER_TOO_SOON);
			return;
		}
		if (this.ctx.blessingService().getHealthNeed(godName, player) <= 1.0D)
		{
			throw new CommandException("command.prayfor.healthy");
		}
		if (!this.ctx.blessingService().healPlayer(godName, player))
		{
			throw new CommandException("command.cancelled");
		}
		this.ctx.godSpeech().sendInfo(playerId, "prayer.prayed-for-health", 1, Messages.amount(cost), Messages.god(godName));
		this.ctx.believerService().increasePrayerPower(playerId, -cost);
		this.ctx.believerService().setItemBlessingTime(playerId);
	}

	private void prayForItem(Player player, String godName)
	{
		UUID playerId = player.getUniqueId();
		int cost = prayers().itemCost();
		if (!canAfford(playerId, godName, cost))
		{
			return;
		}
		if (this.ctx.believerService().hasRecentItemBlessing(playerId))
		{
			this.ctx.godSpeech().sayToBeliever(godName, playerId, Voice.BELIEVER_PRAYER_RECENT_ITEM_BLESSING);
			return;
		}
		if (this.ctx.blessingService().blessPlayerWithItem(godName, player) == null)
		{
			this.ctx.godSpeech().sayToBeliever(godName, playerId, Voice.BELIEVER_PRAYER_WHEN_NO_ITEM_NEED);
		}
		else
		{
			this.ctx.godSpeech().sendInfo(playerId, "prayer.prayed-for-item", 1, Messages.amount(cost), Messages.god(godName));
			this.ctx.believerService().increasePrayerPower(playerId, -cost);
		}
	}

	private void prayForQuest(Player player, String godName) throws CommandException
	{
		UUID playerId = player.getUniqueId();
		int cost = prayers().questCost();
		if (!canAfford(playerId, godName, cost))
		{
			return;
		}
		if (this.ctx.questService().hasQuest(godName))
		{
			throw new CommandException("command.prayfor.has-quest", Messages.god(godName));
		}
		this.ctx.godSpeech().sendInfo(playerId, "prayer.prayed-for-quest", 1, Messages.amount(cost), Messages.god(godName));
		if (this.ctx.questService().generateQuest(godName))
		{
			this.ctx.believerService().increasePrayerPower(playerId, -cost);
		}
	}

	private Settings.Prayers prayers()
	{
		return this.ctx.settings().prayers();
	}
}
