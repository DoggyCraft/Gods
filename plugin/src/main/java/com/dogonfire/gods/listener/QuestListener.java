package com.dogonfire.gods.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;

import com.dogonfire.gods.GodsContext;

/**
 * Quests: believers killing mobs and eating (slay and holy feast quests) and opening pilgrimage chests. Registered only
 * while quests are enabled ({@code Gods.startFeatures}). Shares the meal {@link ActionCooldown} with
 * {@link GodListener} and {@link HolyBookListener}.
 */
public class QuestListener implements Listener
{
	private final GodsContext		ctx;
	private final ActionCooldown	cooldown;

	public QuestListener(GodsContext ctx, ActionCooldown cooldown)
	{
		this.ctx = ctx;
		this.cooldown = cooldown;
	}

	@EventHandler
	public void onEntityDeath(EntityDeathEvent event)
	{
		Player player = event.getEntity().getKiller();

		if (player == null || !ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return;
		}

		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());

		if (godName == null)
		{
			return;
		}

		ctx.questService().handleKilledMob(godName, player.getUniqueId(), event.getEntityType().name());
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
			ctx.questService().handleEat(player.getName(), godName, type.name());
		}
	}

	/**
	 * Pilgrimage chests: opening one (a right click with the main hand) completes the pilgrimage; only believers of the
	 * god whose chest it is may open it (holy land access is HolyLandListener's)
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onQuestChestOpen(PlayerInteractEvent event)
	{
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null)
		{
			return;
		}
		Player player = event.getPlayer();
		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return;
		}

		if (event.getHand() != EquipmentSlot.HAND)
		{
			// The off hand never opens it (nor completes the pilgrimage a second time)
			if (ctx.questService().isQuestTarget(event.getClickedBlock().getLocation()))
			{
				event.setCancelled(true);
			}
			return;
		}

		if (ctx.questService().handleOpenChest(player.getUniqueId(), event.getClickedBlock().getLocation()))
		{
			event.setCancelled(true);
		}
	}
}
