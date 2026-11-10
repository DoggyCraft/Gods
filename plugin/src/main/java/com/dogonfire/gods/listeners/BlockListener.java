package com.dogonfire.gods.listeners;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.managers.AltarManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.LanguageManager;

public class BlockListener implements Listener
{
	/** Last time a player ate or read a holy book, to rate-limit those effects. */
	private final HashMap<UUID, Long> lastEatTimes = new HashMap<UUID, Long>();

	private final GodsContext ctx;

	public BlockListener(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	private boolean isHolyLandActiveIn(World world)
	{
		return ctx.settings().holyLand().enabled() && world != null && ctx.plugin().isEnabledInWorld(world);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void OnBlockPlace(BlockPlaceEvent event)
	{
		Player player = event.getPlayer();
		if (!isHolyLandActiveIn(player.getWorld()))
		{
			return;
		}
		if (player.isOp())
		{
			return;
		}

		if (!ctx.permissionsManager().hasPermission(player, "gods.holyland"))
		{
			ctx.plugin().logDebug(event.getPlayer().getName() + " does not have holyland permission");
			return;
		}

		if (ctx.holyLandManager().isNeutralLandLocation(event.getBlock().getLocation()))
		{
			player.sendMessage(ChatColor.RED + "You cannot build in neutral land");

			event.setCancelled(true);
			return;
		}

		String godName = ctx.holyLandManager().getGodAtHolyLandLocation(event.getBlock().getLocation());

		if (godName == null)
		{
			return;
		}

		String playerGod = ctx.believerService().getGodForBeliever(player.getUniqueId());

		if ((playerGod == null) || (!playerGod.equals(godName)))
		{
			player.sendMessage(ChatColor.RED + "You do not have access to the holy land of " + ChatColor.YELLOW + godName);

			event.setCancelled(true);
		}
	}

	@EventHandler
	public void OnEntityCombust(EntityCombustEvent event)
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
		UUID believerId = ctx.altarManager().getDroppedItemPlayer(event.getEntity().getEntityId());
		if (believerId == null)
		{
			return;
		}
		Player player = ctx.plugin().getServer().getPlayer(believerId);
		if (player == null)
		{
			return;
		}
		if ((!player.isOp()) && (!ctx.permissionsManager().hasPermission(player, "gods.altar.sacrifice")))
		{
			ctx.plugin().logDebug("Does not have gods.altar.sacrifice");
			return;
		}

		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());

		if (godName == null)
		{
			return;
		}

		if (ctx.questManager().handleSacrifice(player, godName, item.getItemStack().getType().name()))
		{
			return;
		}

		ctx.godManager().handleSacrifice(godName, player, item.getItemStack().getType());
	}

	/** The entity really behind some damage: the shooter of a projectile, the igniter of TNT. */
	private static Entity getResponsibleEntity(Entity damager)
	{
		if (damager instanceof Projectile)
		{
			ProjectileSource shooter = ((Projectile) damager).getShooter();

			return (shooter instanceof Entity) ? (Entity) shooter : null;
		}

		if (damager instanceof TNTPrimed)
		{
			return ((TNTPrimed) damager).getSource();
		}

		return damager;
	}

	@EventHandler(ignoreCancelled = true)
	public void OnEntityDamageByEntity(EntityDamageByEntityEvent event)
	{
		Location location = event.getEntity().getLocation();

		if (!isHolyLandActiveIn(location.getWorld()))
		{
			return;
		}

		if (ctx.holyLandManager().isNeutralLandLocation(location))
		{
			event.setCancelled(true);
			return;
		}

		String godName = ctx.holyLandManager().getGodAtHolyLandLocation(location);
		if (godName == null)
		{
			return;
		}

		Entity attacker = getResponsibleEntity(event.getDamager());

		if (attacker instanceof Player)
		{
			Player player = (Player) attacker;

			String attackerGodName = ctx.believerService().getGodForBeliever(player.getUniqueId());
			if (attackerGodName == null)
			{
				if ((ctx.settings().holyLand().lightning()) && ((event.getEntity() instanceof Player)))
				{
					ctx.godManager().strikePlayerWithLightning(player.getUniqueId(), 3);
				}
				event.setCancelled(true);

				return;
			}
			if (!godName.equals(attackerGodName))
			{
				if ((ctx.settings().holyLand().lightning()) && ((event.getEntity() instanceof Player)))
				{
					ctx.godManager().strikePlayerWithLightning(player.getUniqueId(), 3);
				}
				if (!ctx.godManager().hasWarRelation(godName, attackerGodName))
				{
					event.setCancelled(true);
				}
			}
			else if ((event.getEntity() instanceof Player victim) && godName.equals(ctx.believerService().getGodForBeliever(victim.getUniqueId()))
					&& (!ctx.godManager().getGodPvP(godName)))
			{
				// Believers fighting each other in their own god's holy land; intruders can always be fought off
				event.setCancelled(true);
			}
		}
		else if ((attacker instanceof LivingEntity) && (!ctx.godManager().getGodMobDamage(godName)))
		{
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void OnEntityDeath(EntityDeathEvent event)
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

	/** Explosions don't destroy blocks in holy lands or neutral lands. */
	private void protectBlocksFromExplosion(List<Block> blocks)
	{
		if (blocks.isEmpty() || !isHolyLandActiveIn(blocks.get(0).getWorld()))
		{
			return;
		}

		blocks.removeIf(block -> ctx.holyLandManager().isNeutralLandLocation(block.getLocation()) || ctx.holyLandManager().getGodAtHolyLandLocation(block.getLocation()) != null);
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event)
	{
		protectBlocksFromExplosion(event.blockList());
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockExplode(BlockExplodeEvent event)
	{
		protectBlocksFromExplosion(event.blockList());
	}

	@EventHandler(ignoreCancelled = true)
	public void OnPlayerConsume(PlayerItemConsumeEvent event)
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
			GodManager.GodType godType = ctx.godManager().getDivineForceForGod(godName);

			ctx.godManager().handleKilledPlayer(player.getUniqueId(), godName, godType);
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
			if (ctx.godManager().getDivineForceForGod(killerGodName) == GodManager.GodType.WAR)
			{
				message = LanguageManager.LANGUAGESTRING.YouEarnedPowerBySlayingHeathen;
			}
		}
		else
		{
			List<String> warRelations = ctx.godManager().getWarRelations(killerGodName);
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

			ctx.plugin().sendInfo(killer.getUniqueId(), message, ChatColor.AQUA, (int) (powerAfter - powerBefore), killerGodName, 20);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void OnPlayerDropItem(PlayerDropItemEvent event)
	{
		Player player = event.getPlayer();
		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return;
		}
		if ((!player.isOp()) && (!ctx.permissionsManager().hasPermission(player, "gods.altar.sacrifice")))
		{
			ctx.plugin().logDebug("OnPlayerDropItem(): Does not have gods.altar.sacrifice");
			return;
		}
		if (player.getGameMode() == GameMode.CREATIVE)
		{
			return;
		}
		ctx.altarManager().addDroppedItem(event.getItemDrop().getEntityId(), player.getUniqueId());
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

	/** Quest chests and holy land access. */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event)
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

		if (!ctx.settings().holyLand().enabled())
		{
			return;
		}

		if (!ctx.permissionsManager().hasPermission(event.getPlayer(), "gods.holyland"))
		{
			ctx.plugin().logDebug(event.getPlayer().getName() + " does not have holyland permission");
			return;
		}
		if ((!player.isOp()) && (ctx.holyLandManager().isNeutralLandLocation(player.getLocation())))
		{
			if (!ctx.settings().holyLand().allowInteractionInNeutralLands())
			{
				event.setCancelled(true);
			}
			return;
		}
		if (event.getClickedBlock() == null)
		{
			return;
		}
		String blockGodName = ctx.holyLandManager().getGodAtHolyLandLocation(event.getClickedBlock().getLocation());
		if (blockGodName == null)
		{
			return;
		}
		if ((ctx.settings().holyLand().breakableBlocks().contains(event.getClickedBlock().getType())) || (ctx.altarManager().isAltarBlock(event.getClickedBlock())))
		{
			return;
		}
		if (ctx.holyLandManager().isContestedLand(player.getLocation()))
		{
			player.sendMessage(ChatColor.RED + "This Holy Land is contested! Win the battle before you can access this Holy Land!");
			event.setCancelled(true);
			return;
		}
		String playerGodName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		if (playerGodName == null)
		{
			player.sendMessage(ChatColor.RED + "You do not have access to the holy land of " + ChatColor.GOLD + blockGodName);
			event.setCancelled(true);
			return;
		}
		if (!playerGodName.equals(blockGodName))
		{
			if (ctx.godManager().hasAllianceRelation(blockGodName, playerGodName))
			{
				return;
			}
			if (player.isOp())
			{
				return;
			}
			player.sendMessage(ChatColor.RED + "You do not have access to the holy land of " + ChatColor.GOLD + blockGodName);
			event.setCancelled(true);
		}
	}

	/** Holy artifacts, and right-clicking altar signs (praying, cursing, blessing). */
	@EventHandler
	public void OnPlayerInteract(PlayerInteractEvent event)
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

		if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
		{
			return;
		}

		Block block = event.getClickedBlock();

		if (!ctx.altarManager().isAltarSign(block))
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

		String[] lines = AltarManager.getSignLines(block);
		if (lines == null)
		{
			return;
		}

		if (ctx.settings().curses().enabled())
		{
			Player cursedPlayer = ctx.altarManager().getCursedPlayerFromAltar(block, lines);

			if (cursedPlayer != null)
			{
				if ((godName != null) && ctx.godManager().isPriest(player.getUniqueId()))
				{
					Player oldCursedPlayer = ctx.godManager().getCursedPlayerForGod(godName);
					if ((oldCursedPlayer != null) && oldCursedPlayer == cursedPlayer)
					{
						ctx.godManager().setCursedPlayerForGod(godName, null);

						ctx.languageManager().setPlayerName(cursedPlayer.getDisplayName());

						ctx.godManager().GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToPriestCursedPlayerUnset, 2);
						ctx.godManager().GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversCursedPlayerUnset, cursedPlayer.getUniqueId());
					}
					else
					{
						ctx.godManager().setCursedPlayerForGod(godName, cursedPlayer.getUniqueId());

						ctx.languageManager().setPlayerName(cursedPlayer.getDisplayName());

						ctx.godManager().GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToPriestCursedPlayerSet, 2);
						ctx.godManager().GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversCursedPlayerSet, player.getUniqueId());

						ctx.plugin().log(player.getName() + " asked " + godName + " for curses on " + cursedPlayer.getName());
					}
				}
				else
				{
					ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.CursesNotAllowed, ChatColor.DARK_RED, 0, "", 1);
				}

				return;
			}
		}
		if (ctx.settings().blessings().enabled())
		{
			Player blessedPlayer = ctx.altarManager().getBlessedPlayerFromAltarSign(block, lines);
			if (blessedPlayer != null)
			{
				if ((godName != null) && ctx.godManager().isPriest(player.getUniqueId()))
				{
					UUID oldBlessedPlayer = ctx.godManager().getBlessedPlayerForGod(godName);
					if ((oldBlessedPlayer != null) && (oldBlessedPlayer.equals(blessedPlayer.getUniqueId())))
					{
						ctx.godManager().setBlessedPlayerForGod(godName, null);

						ctx.languageManager().setPlayerName(blessedPlayer.getDisplayName());

						ctx.godManager().GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToPriestBlessedPlayerUnset, 2);

						ctx.godManager().GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversBlessedPlayerUnset, player.getUniqueId());
					}
					else
					{
						ctx.godManager().setBlessedPlayerForGod(godName, blessedPlayer.getUniqueId());

						ctx.languageManager().setPlayerName(blessedPlayer.getDisplayName());

						ctx.godManager().GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToPriestBlessedPlayerSet, 2);

						ctx.godManager().GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversBlessedPlayerSet, player.getUniqueId());

						ctx.plugin().log(player.getName() + " asked " + godName + " for blessings on " + blessedPlayer.getName());
					}
				}
				else
				{
					ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.BlessingsNotAllowed, ChatColor.DARK_RED, 0, "", 1);
				}
				return;
			}
		}

		// Only a finished "Altar / of / <god>" sign is a praying altar. A curse or blessing sign whose player
		// is offline (or with cursing/blessing off), or any other sign on an altar block, does nothing
		String firstLine = ChatColor.stripColor(lines[0]).trim();
		if (!firstLine.equals("Altar") || !ctx.altarManager().isFormattedAltarSign(block))
		{
			return;
		}

		if ((!player.isOp()) && (!ctx.permissionsManager().hasPermission(player, "gods.altar.pray")))
		{
			ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.AltarPrayingNotAllowed, ChatColor.DARK_RED, 0, "", 1);
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
			ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.InvalidGodName, ChatColor.DARK_RED, 0, "", 1);
			return;
		}

		altarGodName = ctx.godManager().formatGodName(altarGodName);

		if ((ctx.plugin().isBlacklistedGod(altarGodName)) || (!ctx.plugin().isWhitelistedGod(altarGodName)))
		{
			ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.PrayToBlacklistedGodNotAllowed, ChatColor.DARK_RED, 0, "", 1);
			return;
		}

		if (!ctx.godManager().hasGodAccess(player.getUniqueId(), altarGodName))
		{
			ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.PrivateGodNoAccess, ChatColor.DARK_RED, 0, "", 1);
			return;
		}

		if (ctx.godManager().handleAltarPray(block.getLocation(), player, altarGodName))
		{
			ctx.plugin().log(player.getDisplayName() + " prayed to " + altarGodName + " at an altar");
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		ctx.godManager().updateOnlineGods();
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerPickupItem(EntityPickupItemEvent event)
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

		if (ctx.settings().marriage().enabled())
		{
			ctx.marriageManager().handlePickupItem(player, event.getItem(), event.getItem().getLocation());
		}

	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		Player player = event.getPlayer();

		this.lastEatTimes.remove(player.getUniqueId());

		if (ctx.settings().holyLand().enabled())
		{
			ctx.holyLandManager().handleQuit(player.getUniqueId());
		}

		// The player still counts as online during this event
		if (ctx.plugin().isEnabled())
		{
			ctx.plugin().getServer().getScheduler().runTask(ctx.plugin(), () -> ctx.godManager().updateOnlineGods());
		}
	}

	/** Turns a sign written on an altar block into a praying, cursing or blessing altar. */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void OnSignChange(SignChangeEvent event)
	{
		Player player = event.getPlayer();
		Block block = event.getBlock();

		if (!ctx.plugin().isEnabledInWorld(block.getWorld()))
		{
			return;
		}

		if (!ctx.altarManager().isAltarSign(block))
		{
			return;
		}

		// An existing altar sign is not rewritten (its editor can still be opened by other plugins)
		if (ctx.altarManager().isFormattedAltarSign(block))
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

		if ((ctx.settings().curses().enabled()) && (ctx.altarManager().isCursingAltar(block, event.getLines())))
		{
			created = ctx.altarManager().handleNewCursingAltar(event);
		}
		else if ((ctx.settings().blessings().enabled()) && (ctx.altarManager().isBlessingAltar(block, event.getLines())))
		{
			created = ctx.altarManager().handleNewBlessingAltar(event);
		}
		else
		{
			// A praying altar is a sign with only the god's name on it; leave other signs alone
			int writtenLines = 0;
			for (String text : event.getLines())
			{
				if ((text != null) && !ChatColor.stripColor(text).isBlank())
				{
					writtenLines++;
				}
			}
			if (writtenLines != 1)
			{
				return;
			}
			created = ctx.altarManager().handleNewPrayingAltar(event);
		}

		if (created)
		{
			ctx.altarManager().waxAltarSignLater(block);
		}
		else
		{
			ItemStack sign = AltarManager.getSignItem(block.getType());
			event.setCancelled(true);
			block.setType(Material.AIR);
			if (player.getGameMode() != GameMode.CREATIVE)
			{
				block.getWorld().dropItemNaturally(block.getLocation(), sign);
			}
		}
	}
}
