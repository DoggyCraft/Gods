package com.dogonfire.gods.listener;

import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
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
import com.dogonfire.gods.config.Theme;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.service.AltarClicks;
import com.dogonfire.gods.service.AltarGeometry;
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

	/**
	 * Right-clicking altar signs: praying, cursing, blessing. Right-clicking the altar block or its light prays as
	 * clicking its praying sign does, unless the click has a vanilla use there ({@link AltarClicks#praysOnClick}).
	 */
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return;
		}

		if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
		{
			return;
		}

		Block block = event.getClickedBlock();

		if (ctx.altarService().isAltarSign(block))
		{
			// Right-clicking a sign opens the sign editor (1.20+); altar signs must keep their text
			event.setUseInteractedBlock(Event.Result.DENY);

			// The event fires once per hand; only act once
			if (event.getHand() == EquipmentSlot.HAND)
			{
				useAltarSign(player, block);
			}
			return;
		}

		// A finished altar whose light is gone does nothing until it is lit again: say so
		if (AltarService.isAltarSignType(block) && block.getState() instanceof Sign sign && ctx.altarService().readTaggedAltar(sign) != null)
		{
			if (event.getHand() == EquipmentSlot.HAND)
			{
				ctx.godSpeech().sendInfo(player.getUniqueId(), "altar.no-light", 1);
			}
			return;
		}

		prayAtAltar(event, player, block);
	}

	/**
	 * A right click on an altar block or its light: prays with the altar's praying sign. Both hands' events are
	 * decided by the main hand's item, so the off hand doesn't place or use anything after the main hand prayed.
	 * Clicks that don't pray are left alone.
	 */
	private void prayAtAltar(PlayerInteractEvent event, Player player, Block block)
	{
		// Another plugin (a protection plugin) refused the click
		if (event.useInteractedBlock() == Event.Result.DENY)
		{
			return;
		}
		AltarClicks.Part part = ctx.altarService().altarPart(block);
		if (part == AltarClicks.Part.NONE)
		{
			return;
		}
		boolean ownUse = part == AltarClicks.Part.BLOCK && block.getType().isInteractable();
		String held = player.getInventory().getItemInMainHand().getType().name();
		if (!AltarClicks.praysOnClick(part, true, player.isSneaking(), ownUse, block.getType().name(), held))
		{
			return;
		}
		Block sign = ctx.altarService().findPrayingSign(block);
		if (sign == null)
		{
			return;
		}
		event.setUseInteractedBlock(Event.Result.DENY);
		event.setUseItemInHand(Event.Result.DENY);
		if (event.getHand() == EquipmentSlot.HAND)
		{
			useAltarSign(player, sign);
		}
	}

	/** Using an altar sign (clicked, or the praying sign of a clicked altar): praying, cursing, blessing */
	private void useAltarSign(Player player, Block block)
	{
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());

		// By its tags, or by the text of an altar from before tags (which gets tagged and styled now)
		AltarSigns.AltarSign altar = ctx.altarService().readAltarSign(block);
		if (altar == null)
		{
			return;
		}
		altar = ctx.altarService().refreshAltarSign(block, altar, player);
		if (altar.tagged())
		{
			// Known from now on (an altar from before tags, or one the registry hasn't found yet)
			ctx.altarRegistry().register(block, altar, null);
		}

		if (ctx.settings().curses().enabled())
		{
			Player cursedPlayer = altar.kind() == AltarSigns.Kind.CURSES ? ctx.altarService().getAltarSignPlayer(altar) : null;

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
			Player blessedPlayer = altar.kind() == AltarSigns.Kind.BLESSINGS ? ctx.altarService().getAltarSignPlayer(altar) : null;
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

		// A curse or blessing sign whose player is offline (or with cursing/blessing off) does nothing
		if (altar.kind() != AltarSigns.Kind.PRAYING)
		{
			return;
		}

		ctx.prayerService().prayAtAltarSign(player, block, altar.god());
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

		// Altars are written on the front of a wall sign (its back faces the altar block), on either side of a hanging
		// sign (both sides get the altar's text)
		if (event.getSide() != Side.FRONT && AltarService.mount(block) != AltarGeometry.Mount.WALL_HANGING)
		{
			return;
		}

		AltarSigns.AltarSign created;

		if ((ctx.settings().curses().enabled()) && (ctx.altarService().isCursingAltar(block, event.getLines())))
		{
			created = mayBuildHere(player, block) ? ctx.altarService().handleNewCursingAltar(event) : null;
		}
		else if ((ctx.settings().blessings().enabled()) && (ctx.altarService().isBlessingAltar(block, event.getLines())))
		{
			created = mayBuildHere(player, block) ? ctx.altarService().handleNewBlessingAltar(event) : null;
		}
		else
		{
			// A praying altar is a sign with the god's name on it, and maybe "God" or "Goddess" on another line; leave
			// other signs alone
			int written = AltarSigns.writtenLines(event.getLines());
			if (written != 1 && (written != 2 || ctx.altarService().readNewPrayingAltar(event.getLines()).problem() == AltarSigns.Problem.INVALID_SIGN))
			{
				return;
			}
			created = mayBuildHere(player, block) ? ctx.altarService().handleNewPrayingAltar(event) : null;
		}

		if (created != null)
		{
			ctx.altarService().finishAltarSignLater(block, created, player.getUniqueId());
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

	/**
	 * True if a new altar may stand here as far as land claims go (altars.require-claim with GriefPrevention); tells the
	 * player why not otherwise
	 */
	private boolean mayBuildHere(Player player, Block sign)
	{
		if (ctx.altarService().mayBuildAltarHere(player, sign))
		{
			return true;
		}
		ctx.godSpeech().sendInfo(player.getUniqueId(), "altar.not-in-claim", 1);
		return false;
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
			// What the quest asked for
			ctx.effectService().sacrifice(godName, item, Theme.EffectMood.PLEASED);
			return;
		}

		ctx.commandmentService().handleSacrifice(godName, player, item);
	}
}
