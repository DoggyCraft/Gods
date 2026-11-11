package com.dogonfire.gods.listener;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.service.AltarService;
import com.dogonfire.gods.service.AltarSigns;

/**
 * Altars: writing a new altar sign, right-clicking an altar sign (praying, or a priest setting the god's cursed or
 * blessed player), and sacrifices (an item dropped by a believer that burns). Always registered; the logic is in
 * {@link AltarService}, {@code PrayerService} (prayers), {@code BlessingService} (blessed and cursed players),
 * {@code CommandmentService} (sacrifices) and QuestManager.
 */
public class AltarListener implements Listener
{
	private final GodsContext ctx;

	public AltarListener(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	/** Right-clicking altar signs: praying, cursing, blessing */
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return;
		}

		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());

		if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
		{
			return;
		}

		Block block = event.getClickedBlock();

		if (!ctx.altarService().isAltarSign(block))
		{
			return;
		}

		// Right-clicking a sign opens the sign editor (1.20+); altar signs must keep their text
		event.setUseInteractedBlock(Event.Result.DENY);

		// The event fires once per hand; only act once
		if (event.getHand() != EquipmentSlot.HAND)
		{
			return;
		}

		String[] lines = AltarService.getSignLines(block);
		if (lines == null)
		{
			return;
		}

		if (ctx.settings().curses().enabled())
		{
			Player cursedPlayer = ctx.altarService().getCursedPlayerFromAltar(block, lines);

			if (cursedPlayer != null)
			{
				if ((godName != null) && ctx.priestService().isPriest(player.getUniqueId()))
				{
					Player oldCursedPlayer = ctx.blessingService().getCursedPlayerForGod(godName);
					if ((oldCursedPlayer != null) && oldCursedPlayer == cursedPlayer)
					{
						ctx.blessingService().setCursedPlayerForGod(godName, null);


						ctx.godSpeech().say(godName, player, Voice.PRIEST_CURSED_PLAYER_UNSET, 2, Messages.displayName(cursedPlayer.getDisplayName()));
						ctx.godSpeech().sayToBelieversExcept(godName, Voice.BELIEVERS_CURSED_PLAYER_UNSET, cursedPlayer.getUniqueId(), Messages.displayName(cursedPlayer.getDisplayName()));
					}
					else
					{
						ctx.blessingService().setCursedPlayerForGod(godName, cursedPlayer.getUniqueId());


						ctx.godSpeech().say(godName, player, Voice.PRIEST_CURSED_PLAYER_SET, 2, Messages.displayName(cursedPlayer.getDisplayName()));
						ctx.godSpeech().sayToBelieversExcept(godName, Voice.BELIEVERS_CURSED_PLAYER_SET, player.getUniqueId(), Messages.displayName(cursedPlayer.getDisplayName()));

						ctx.plugin().log(player.getName() + " asked " + godName + " for curses on " + cursedPlayer.getName());
					}
				}
				else
				{
					ctx.godSpeech().sendInfo(player.getUniqueId(), "altar.curse-not-allowed", 1);
				}

				return;
			}
		}
		if (ctx.settings().blessings().enabled())
		{
			Player blessedPlayer = ctx.altarService().getBlessedPlayerFromAltarSign(block, lines);
			if (blessedPlayer != null)
			{
				if ((godName != null) && ctx.priestService().isPriest(player.getUniqueId()))
				{
					UUID oldBlessedPlayer = ctx.blessingService().getBlessedPlayerForGod(godName);
					if ((oldBlessedPlayer != null) && (oldBlessedPlayer.equals(blessedPlayer.getUniqueId())))
					{
						ctx.blessingService().setBlessedPlayerForGod(godName, null);


						ctx.godSpeech().say(godName, player, Voice.PRIEST_BLESSED_PLAYER_UNSET, 2, Messages.displayName(blessedPlayer.getDisplayName()));

						ctx.godSpeech().sayToBelieversExcept(godName, Voice.BELIEVERS_BLESSED_PLAYER_UNSET, player.getUniqueId(), Messages.displayName(blessedPlayer.getDisplayName()));
					}
					else
					{
						ctx.blessingService().setBlessedPlayerForGod(godName, blessedPlayer.getUniqueId());


						ctx.godSpeech().say(godName, player, Voice.PRIEST_BLESSED_PLAYER_SET, 2, Messages.displayName(blessedPlayer.getDisplayName()));

						ctx.godSpeech().sayToBelieversExcept(godName, Voice.BELIEVERS_BLESSED_PLAYER_SET, player.getUniqueId(), Messages.displayName(blessedPlayer.getDisplayName()));

						ctx.plugin().log(player.getName() + " asked " + godName + " for blessings on " + blessedPlayer.getName());
					}
				}
				else
				{
					ctx.godSpeech().sendInfo(player.getUniqueId(), "altar.blessing-not-allowed", 1);
				}
				return;
			}
		}

		// Only a finished "Altar / of / <god>" sign is a praying altar. A curse or blessing sign whose player
		// is offline (or with cursing/blessing off), or any other sign on an altar block, does nothing
		String firstLine = ChatColor.stripColor(lines[0]).trim();
		if (!firstLine.equals("Altar") || !ctx.altarService().isFormattedAltarSign(block))
		{
			return;
		}

		if ((!player.isOp()) && (!ctx.permissions().hasPermission(player, "gods.altar.pray")))
		{
			ctx.godSpeech().sendInfo(player.getUniqueId(), "altar.praying-not-allowed", 1);
			return;
		}

		// The god of a praying altar is written on its third line
		String altarGodName = lines[2];

		if (altarGodName == null)
		{
			return;
		}

		altarGodName = ChatColor.stripColor(altarGodName).trim();

		if (altarGodName.length() <= 1)
		{
			ctx.godSpeech().sendInfo(player.getUniqueId(), "altar.invalid-god-name", 1);
			return;
		}

		altarGodName = ctx.godService().formatGodName(altarGodName);

		if (!ctx.godListService().isAllowedGod(altarGodName))
		{
			ctx.godSpeech().sendInfo(player.getUniqueId(), "altar.god-not-allowed", 1);
			return;
		}

		if (!ctx.godService().hasGodAccess(player.getUniqueId(), altarGodName))
		{
			ctx.godSpeech().sendInfo(player.getUniqueId(), "altar.no-access", 1);
			return;
		}

		if (ctx.prayerService().handleAltarPray(block.getLocation(), player, altarGodName))
		{
			ctx.plugin().log(player.getDisplayName() + " prayed to " + altarGodName + " at an altar");
		}
	}

	/** Turns a sign written on an altar block into a praying, cursing or blessing altar. */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onSignChange(SignChangeEvent event)
	{
		Player player = event.getPlayer();
		Block block = event.getBlock();

		if (!ctx.plugin().isEnabledInWorld(block.getWorld()))
		{
			return;
		}

		if (!ctx.altarService().isAltarSign(block))
		{
			return;
		}

		// An existing altar sign is not rewritten (its editor can still be opened by other plugins)
		if (ctx.altarService().isFormattedAltarSign(block))
		{
			event.setCancelled(true);
			return;
		}

		// Altars are written on the front of the sign
		if (event.getSide() != Side.FRONT)
		{
			return;
		}

		boolean created;

		if ((ctx.settings().curses().enabled()) && (ctx.altarService().isCursingAltar(block, event.getLines())))
		{
			created = ctx.altarService().handleNewCursingAltar(event);
		}
		else if ((ctx.settings().blessings().enabled()) && (ctx.altarService().isBlessingAltar(block, event.getLines())))
		{
			created = ctx.altarService().handleNewBlessingAltar(event);
		}
		else
		{
			// A praying altar is a sign with only the god's name on it; leave other signs alone
			if (AltarSigns.writtenLines(event.getLines()) != 1)
			{
				return;
			}
			created = ctx.altarService().handleNewPrayingAltar(event);
		}

		if (created)
		{
			ctx.altarService().waxAltarSignLater(block);
		}
		else
		{
			ItemStack sign = AltarService.getSignItem(block.getType());
			event.setCancelled(true);
			block.setType(Material.AIR);
			if (player.getGameMode() != GameMode.CREATIVE)
			{
				block.getWorld().dropItemNaturally(block.getLocation(), sign);
			}
		}
	}

	/** Remembers who dropped an item, so it counts as their sacrifice if it burns */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerDropItem(PlayerDropItemEvent event)
	{
		Player player = event.getPlayer();
		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return;
		}
		if ((!player.isOp()) && (!ctx.permissions().hasPermission(player, "gods.altar.sacrifice")))
		{
			ctx.plugin().logDebug("OnPlayerDropItem(): Does not have gods.altar.sacrifice");
			return;
		}
		if (player.getGameMode() == GameMode.CREATIVE)
		{
			return;
		}
		ctx.altarService().addDroppedItem(event.getItemDrop(), player.getUniqueId());
	}

	/**
	 * A dropped item catching fire (in fire or in lava): a sacrifice to the god of whoever dropped it (or part of a
	 * sacrifice quest). It counts once, though it catches fire again every tick it burns.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onEntityCombust(EntityCombustEvent event)
	{
		if (!ctx.settings().sacrifices().enabled())
		{
			return;
		}
		if (!(event.getEntity() instanceof Item))
		{
			return;
		}
		Item item = (Item) event.getEntity();
		if (!ctx.plugin().isEnabledInWorld(item.getWorld()))
		{
			return;
		}
		if (event.getEntity().getType() != EntityType.ITEM)
		{
			return;
		}
		UUID believerId = ctx.altarService().takeDroppedItemPlayer(item);
		if (believerId == null)
		{
			return;
		}
		Player player = ctx.plugin().getServer().getPlayer(believerId);
		if (player == null)
		{
			return;
		}
		if ((!player.isOp()) && (!ctx.permissions().hasPermission(player, "gods.altar.sacrifice")))
		{
			ctx.plugin().logDebug("Does not have gods.altar.sacrifice");
			return;
		}

		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());

		if (godName == null)
		{
			return;
		}

		if (ctx.questService().handleSacrifice(player, godName, item.getItemStack().getType().name()))
		{
			return;
		}

		ctx.commandmentService().handleSacrifice(godName, player, item.getItemStack().getType());
	}
}
