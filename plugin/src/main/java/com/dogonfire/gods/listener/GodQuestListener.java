package com.dogonfire.gods.listener;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.dogonfire.gods.GodsContext;

/**
 * What is left of the old BlockListener once believers, altars, holy lands and marriages got their own listeners:
 * gods (commandments on killing and eating, the online gods list on join/quit), quests (mob kills, meals, quest
 * chests, reading holy books) and holy artifacts (using one, picking up a second). Phases B3 (gods) and B4 (quests,
 * holy books, holy artifacts) split it further. Always registered.
 */
public class GodQuestListener implements Listener
{
	/** Last time a player ate or read a holy book, to rate-limit those effects. */
	private final HashMap<UUID, Long> lastEatTimes = new HashMap<UUID, Long>();

	private final GodsContext ctx;

	public GodQuestListener(GodsContext ctx)
	{
		this.ctx = ctx;
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
			ctx.questManager().handleKilledMob(godName, event.getEntityType().name());
		}

		if ((!player.isOp()) && (!ctx.permissionsManager().hasPermission(player, "gods.commandments")))
		{
			return;
		}

		ctx.godManager().handleKilled(player, godName, event.getEntityType().name());
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
		if (godName != null)
		{
			Long lastEatTime = this.lastEatTimes.get(player.getUniqueId());
			long currentTime = System.currentTimeMillis();
			if ((lastEatTime == null) || (currentTime - lastEatTime.longValue() > 10000L))
			{
				if ((ctx.settings().commandments().enabled()) && (player.getHealth() != player.getAttribute(Attribute.MAX_HEALTH).getValue()))
				{
					if ((player.isOp()) || (ctx.permissionsManager().hasPermission(player, "gods.commandments")))
					{
						ctx.godManager().handleEat(player, godName, type.name());
					}
				}
				if (ctx.settings().quests().enabled())
				{
					ctx.questManager().handleEat(player.getName(), godName, type.name());
				}
				this.lastEatTimes.put(player.getUniqueId(), currentTime);
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
			String godName = ctx.holyBookManager().getGodForBible(book);
			if (godName != null)
			{
				Long lastReadTime = this.lastEatTimes.get(player.getUniqueId());
				long currentTime = System.currentTimeMillis();
				if ((lastReadTime == null) || (currentTime - lastReadTime.longValue() > 10000L))
				{
					ctx.questManager().handleReadBible(godName, player.getUniqueId());
					this.lastEatTimes.put(player.getUniqueId(), currentTime);
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
			if (event.getClickedBlock() != null && ctx.questManager().handleOpenChest(event.getPlayer().getUniqueId(), event.getClickedBlock().getLocation()))
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
				ctx.holyArtifactManager().handleActivate(player.getName(), player.getInventory().getItemInMainHand());
			}
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		ctx.godService().updateOnlineGods();
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
			if (ctx.holyArtifactManager().isHolyArtifact(event.getItem().getItemStack()))
			{
				if (ctx.holyArtifactManager().hasHolyArtifact(player.getName()))
				{
					event.setCancelled(true);
					return;
				}
			}
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		Player player = event.getPlayer();

		this.lastEatTimes.remove(player.getUniqueId());

		// The player still counts as online during this event
		if (ctx.plugin().isEnabled())
		{
			ctx.plugin().getServer().getScheduler().runTask(ctx.plugin(), () -> ctx.godService().updateOnlineGods());
		}
	}
}
