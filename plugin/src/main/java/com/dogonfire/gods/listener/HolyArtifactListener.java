package com.dogonfire.gods.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import com.dogonfire.gods.GodsContext;

/**
 * Holy artifacts: a believer using one (right-click with it in hand), and a player who carries one can't pick up
 * another, and the ice of an ice spike can't be broken. Registered only while holy artifacts are enabled ({@code Gods.startFeatures}).
 */
public class HolyArtifactListener implements Listener
{
	private final GodsContext ctx;

	public HolyArtifactListener(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	@EventHandler
	public void onHolyArtifactUse(PlayerInteractEvent event)
	{
		// Using the item was denied, e.g. because the click prayed at an altar
		if (event.useItemInHand() == Event.Result.DENY)
		{
			return;
		}

		Player player = event.getPlayer();
		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return;
		}

		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());

		if ((event.getAction() == Action.RIGHT_CLICK_AIR) || (event.getAction() == Action.RIGHT_CLICK_BLOCK))
		{
			Material type = player.getInventory().getItemInMainHand().getType();
			if ((event.getHand() == EquipmentSlot.HAND) && (type != Material.AIR) && (godName != null))
			{
				ctx.holyArtifactService().handleActivate(player, player.getInventory().getItemInMainHand());
			}
		}
	}

	/** The ice an ice spike left can't be broken (or mined with silk touch); it goes away by itself */
	@EventHandler(ignoreCancelled = true)
	public void onEncasingBreak(BlockBreakEvent event)
	{
		if (ctx.holyPowerService().isEncased(event.getBlock()))
		{
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onHolyArtifactPickup(EntityPickupItemEvent event)
	{
		if (!(event.getEntity() instanceof Player player))
		{
			return;
		}

		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return;
		}

		if (ctx.holyArtifactService().isHolyArtifact(event.getItem().getItemStack()) && ctx.holyArtifactService().hasHolyArtifact(player))
		{
			event.setCancelled(true);
		}
	}
}
