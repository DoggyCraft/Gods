package com.dogonfire.gods.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.dogonfire.gods.GodsContext;

/**
 * What is left of the old BlockListener once believers, altars, holy lands, marriages and gods got their own
 * listeners: quests (mob kills, meals, quest chests, reading holy books), holy books and holy artifacts (using one,
 * picking up a second). Phase B4 splits it by area. Always registered, after {@link GodListener}, with which it shares
 * the meal {@link ActionCooldown} (reading a holy book uses it too).
 */
public class QuestAndHolyItemListener implements Listener
{
	private final GodsContext		ctx;
	private final ActionCooldown	cooldown;

	public QuestAndHolyItemListener(GodsContext ctx, ActionCooldown cooldown)
	{
		this.ctx = ctx;
		this.cooldown = cooldown;
	}

	@EventHandler
	public void onEntityDeath(EntityDeathEvent event)
	{
		Player player = event.getEntity().getKiller();

		if (player == null)
		{
			return;
		}

		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return;
		}

		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());

		if (godName == null)
		{
			return;
		}

		if (ctx.settings().quests().enabled())
		{
			ctx.questService().handleKilledMob(godName, event.getEntityType().name());
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerConsume(PlayerItemConsumeEvent event)
	{
		Player player = event.getPlayer();

		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return;
		}

		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		Material type = event.getItem().getType();
		if (godName != null && this.cooldown.ready(player.getUniqueId(), event))
		{
			if (ctx.settings().quests().enabled())
			{
				ctx.questService().handleEat(player.getName(), godName, type.name());
			}
		}
	}

	/**
	 * Reading a holy book (right-click). Clicks in
	 * the air reach listeners already cancelled, so this handler doesn't ignore
	 * cancelled events; it only skips when using the item in hand was denied.
	 */
	@EventHandler(priority = EventPriority.HIGH)
	public void onHolyBookUse(PlayerInteractEvent event)
	{
		if (!ctx.settings().bibles().enabled() || event.useItemInHand() == Event.Result.DENY || event.getHand() != EquipmentSlot.HAND)
		{
			return;
		}

		Player player = event.getPlayer();
		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return;
		}

		ItemStack book = event.getItem();
		if ((book == null) || (book.getType() != Material.WRITTEN_BOOK))
		{
			return;
		}

		Action action = event.getAction();

		if ((action == Action.RIGHT_CLICK_AIR) || (action == Action.RIGHT_CLICK_BLOCK))
		{
			String godName = ctx.holyBookService().getGodForBible(book);
			if (godName != null)
			{
				if (this.cooldown.ready(player.getUniqueId(), event))
				{
					ctx.questService().handleReadBible(godName, player.getUniqueId());
				}
			}
		}
	}

	/** Quest chests (holy land access is HolyLandListener's) */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onQuestChestOpen(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return;
		}

		if (ctx.settings().quests().enabled())
		{
			if (event.getClickedBlock() != null && ctx.questService().handleOpenChest(event.getPlayer().getUniqueId(), event.getClickedBlock().getLocation()))
			{
				event.setCancelled(true);
			}
		}
	}

	/** Using a holy artifact (right-click with it in hand) */
	@EventHandler
	public void onHolyArtifactUse(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return;
		}

		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());

		if ((event.getAction() == Action.RIGHT_CLICK_AIR) || (event.getAction() == Action.RIGHT_CLICK_BLOCK))
		{
			Material type = player.getInventory().getItemInMainHand().getType();
			if ((event.getHand() == EquipmentSlot.HAND) && (type != Material.AIR) && (godName != null) && (ctx.settings().holyArtifacts().enabled()))
			{
				ctx.holyArtifactService().handleActivate(player.getName(), player.getInventory().getItemInMainHand());
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onHolyArtifactPickup(EntityPickupItemEvent event)
	{
		if (!(event.getEntity() instanceof Player)) {
			return;
		}

		Player player = (Player) event.getEntity();

		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return;
		}

		if (ctx.settings().holyArtifacts().enabled())
		{
			if (ctx.holyArtifactService().isHolyArtifact(event.getItem().getItemStack()))
			{
				if (ctx.holyArtifactService().hasHolyArtifact(player.getName()))
				{
					event.setCancelled(true);
					return;
				}
			}
		}
	}
}
