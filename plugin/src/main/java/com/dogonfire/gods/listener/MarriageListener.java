package com.dogonfire.gods.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

import com.dogonfire.gods.GodsContext;

/**
 * Weddings: an engaged player picking up a wedding token. Registered only while marriage is enabled (see
 * {@code Gods#startFeatures}); the logic is in {@code MarriageService}.
 */
public class MarriageListener implements Listener
{
	private final GodsContext ctx;

	public MarriageListener(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerPickupItem(EntityPickupItemEvent event)
	{
		if (!(event.getEntity() instanceof Player player))
		{
			return;
		}

		if (!ctx.settings().marriage().enabled() || !ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return;
		}

		ctx.marriageService().handlePickupItem(player, event.getItem());
	}
}
