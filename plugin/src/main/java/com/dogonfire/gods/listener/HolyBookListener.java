package com.dogonfire.gods.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.dogonfire.gods.GodsContext;

/**
 * Reading a holy book (it counts for a GIVEITEMS quest). Registered only while bibles are enabled
 * ({@code Gods.startFeatures}). Reading shares the {@link ActionCooldown} with meals.
 */
public class HolyBookListener implements Listener
{
	private final GodsContext		ctx;
	private final ActionCooldown	cooldown;

	public HolyBookListener(GodsContext ctx, ActionCooldown cooldown)
	{
		this.ctx = ctx;
		this.cooldown = cooldown;
	}

	/**
	 * Reading a holy book (right-click). Clicks in the air reach listeners already cancelled, so this handler doesn't
	 * ignore cancelled events; it only skips when using the item in hand was denied.
	 */
	@EventHandler(priority = EventPriority.HIGH)
	public void onHolyBookUse(PlayerInteractEvent event)
	{
		if (event.useItemInHand() == Event.Result.DENY || event.getHand() != EquipmentSlot.HAND)
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
			if (godName != null && this.cooldown.ready(player.getUniqueId(), event))
			{
				ctx.questService().handleReadBible(godName, player.getUniqueId());
			}
		}
	}
}
