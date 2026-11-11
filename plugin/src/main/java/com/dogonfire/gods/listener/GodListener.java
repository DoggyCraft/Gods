package com.dogonfire.gods.listener;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.dogonfire.gods.GodsContext;

/**
 * Gods: their commandments when a believer kills a mob or eats ({@code CommandmentService}), and which gods have a
 * believer online, on join and quit ({@code GodService}). Always registered, so it runs before
 * {@link QuestListener}, with which it shares the meal {@link ActionCooldown}.
 */
public class GodListener implements Listener
{
	private final GodsContext		ctx;
	private final ActionCooldown	cooldown;

	public GodListener(GodsContext ctx, ActionCooldown cooldown)
	{
		this.ctx = ctx;
		this.cooldown = cooldown;
	}

	/** Killing the mob the god wants slain, or the one it wants spared */
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

		if ((!player.isOp()) && (!ctx.permissions().hasPermission(player, "gods.commandments")))
		{
			return;
		}

		ctx.commandmentService().handleKilled(player, godName, event.getEntityType().name());
	}

	/** Eating the god's holy or unholy food, when not at full health */
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
			if ((ctx.settings().commandments().enabled()) && (player.getHealth() != player.getAttribute(Attribute.MAX_HEALTH).getValue()))
			{
				if ((player.isOp()) || (ctx.permissions().hasPermission(player, "gods.commandments")))
				{
					ctx.commandmentService().handleEat(player, godName, type.name());
				}
			}
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		ctx.godService().updateOnlineGods();
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		this.cooldown.forget(event.getPlayer().getUniqueId());

		// The player still counts as online during this event
		if (ctx.plugin().isEnabled())
		{
			ctx.plugin().getServer().getScheduler().runTask(ctx.plugin(), () -> ctx.godService().updateOnlineGods());
		}
	}
}
