package com.dogonfire.gods.listener;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.model.GodType;

/**
 * Believers dying: the dead believer may leave their religion (believers.leave-religion-on-death), and a believer who
 * killed a heathen (for a war god) or a believer of an enemy god earns prayers and prayer power. Always registered;
 * the logic is in {@code BelieverService} and {@code PrayerService}.
 */
public class BelieverListener implements Listener
{
	private final GodsContext ctx;

	public BelieverListener(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event)
	{
		Player player = event.getEntity();
		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return;
		}
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());

		if (godName != null)
		{
			GodType godType = ctx.godService().getDivineForceForGod(godName);

			ctx.prayerService().handleKilledPlayer(player.getUniqueId(), godName, godType);
		}

		Player killer = player.getKiller();
		if (killer == null)
		{
			return;
		}

		String killerGodName = ctx.believerService().getGodForBeliever(killer.getUniqueId());
		if (killerGodName == null)
		{
			return;
		}

		LanguageManager.LANGUAGESTRING message = null;

		if (godName == null)
		{
			// War gods reward slaying heathens
			if (ctx.godService().getDivineForceForGod(killerGodName) == GodType.WAR)
			{
				message = LanguageManager.LANGUAGESTRING.YouEarnedPowerBySlayingHeathen;
			}
		}
		else
		{
			List<String> warRelations = ctx.relationService().getWarRelations(killerGodName);
			if ((warRelations != null) && (warRelations.contains(godName)))
			{
				message = LanguageManager.LANGUAGESTRING.YouEarnedPowerBySlayingEnemy;
			}
		}

		if (message != null)
		{
			double powerBefore = ctx.believerService().getBelieverPower(killer.getUniqueId());
			ctx.believerService().increasePrayer(killer.getUniqueId(), killerGodName, 2);
			ctx.believerService().increasePrayerPower(killer.getUniqueId(), 2);
			double powerAfter = ctx.believerService().getBelieverPower(killer.getUniqueId());

			ctx.godSpeech().sendInfo(killer.getUniqueId(), message, ChatColor.AQUA, (int) (powerAfter - powerBefore), killerGodName, 20);
		}
	}
}
