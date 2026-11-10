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

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.AltarManager;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.HolyArtifactManager;
import com.dogonfire.gods.managers.HolyBookManager;
import com.dogonfire.gods.managers.HolyLandManager;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.managers.MarriageManager;
import com.dogonfire.gods.managers.PermissionsManager;
import com.dogonfire.gods.managers.QuestManager;

public class BlockListener implements Listener
{
	/** Last time a player ate or read a holy book, to rate-limit those effects. */
	private HashMap<UUID, Long> lastEatTimes = new HashMap<UUID, Long>();

	private static boolean isHolyLandActiveIn(World world)
	{
		return GodsConfiguration.instance().isHolyLandEnabled() && world != null && Gods.instance().isEnabledInWorld(world);
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

		if (!PermissionsManager.instance().hasPermission(player, "gods.holyland"))
		{
			Gods.instance().logDebug(event.getPlayer().getName() + " does not have holyland permission");
			return;
		}

		if (HolyLandManager.instance().isNeutralLandLocation(event.getBlock().getLocation()))
		{
			player.sendMessage(ChatColor.RED + "You cannot build in neutral land");

			event.setCancelled(true);
			return;
		}

		String godName = HolyLandManager.instance().getGodAtHolyLandLocation(event.getBlock().getLocation());

		if (godName == null)
		{
			return;
		}

		String playerGod = BelieverManager.instance().getGodForBeliever(player.getUniqueId());

		if ((playerGod == null) || (!playerGod.equals(godName)))
		{
			player.sendMessage(ChatColor.RED + "You do not have access to the holy land of " + ChatColor.YELLOW + godName);

			event.setCancelled(true);
		}
	}

	@EventHandler
	public void OnEntityCombust(EntityCombustEvent event)
	{
		if (!GodsConfiguration.instance().isSacrificesEnabled())
		{
			return;
		}
		if (!(event.getEntity() instanceof Item))
		{
			return;
		}
		Item item = (Item) event.getEntity();
		if (!Gods.instance().isEnabledInWorld(item.getWorld()))
		{
			return;
		}
		if (event.getEntity().getType() != EntityType.ITEM)
		{
			return;
		}
		UUID believerId = AltarManager.instance().getDroppedItemPlayer(event.getEntity().getEntityId());
		if (believerId == null)
		{
			return;
		}
		Player player = Gods.instance().getServer().getPlayer(believerId);
		if (player == null)
		{
			return;
		}
		if ((!player.isOp()) && (!PermissionsManager.instance().hasPermission(player, "gods.altar.sacrifice")))
		{
			Gods.instance().logDebug("Does not have gods.altar.sacrifice");
			return;
		}

		String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());

		if (godName == null)
		{
			return;
		}

		if (QuestManager.instance().handleSacrifice(player, godName, item.getItemStack().getType().name()))
		{
			return;
		}

		GodManager.instance().handleSacrifice(godName, player, item.getItemStack().getType());
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

		if (HolyLandManager.instance().isNeutralLandLocation(location))
		{
			event.setCancelled(true);
			return;
		}

		String godName = HolyLandManager.instance().getGodAtHolyLandLocation(location);
		if (godName == null)
		{
			return;
		}

		Entity attacker = getResponsibleEntity(event.getDamager());

		if (attacker instanceof Player)
		{
			Player player = (Player) attacker;

			String attackerGodName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());
			if (attackerGodName == null)
			{
				if ((GodsConfiguration.instance().isHolyLandLightning()) && ((event.getEntity() instanceof Player)))
				{
					GodManager.instance().strikePlayerWithLightning(player.getUniqueId(), 3);
				}
				event.setCancelled(true);

				return;
			}
			if (!godName.equals(attackerGodName))
			{
				if ((GodsConfiguration.instance().isHolyLandLightning()) && ((event.getEntity() instanceof Player)))
				{
					GodManager.instance().strikePlayerWithLightning(player.getUniqueId(), 3);
				}
				if (!GodManager.instance().hasWarRelation(godName, attackerGodName))
				{
					event.setCancelled(true);
				}
			}
			else if ((event.getEntity() instanceof Player) && (!GodManager.instance().getGodPvP(godName)))
			{
				// Believers fighting each other in their own god's holy land
				event.setCancelled(true);
			}
		}
		else if ((attacker instanceof LivingEntity) && (!GodManager.instance().getGodMobDamage(godName)))
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

		if (!Gods.instance().isEnabledInWorld(player.getWorld()))
		{
			return;
		}

		String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());

		if (godName == null)
		{
			return;
		}

		if (GodsConfiguration.instance().isQuestsEnabled())
		{
			QuestManager.instance().handleKilledMob(godName, event.getEntityType().name());
		}

		if ((!player.isOp()) && (!PermissionsManager.instance().hasPermission(player, "gods.commandments")))
		{
			return;
		}

		GodManager.instance().handleKilled(player, godName, event.getEntityType().name());
	}

	/** Explosions don't destroy blocks in holy lands or neutral lands. */
	private static void protectBlocksFromExplosion(List<Block> blocks)
	{
		if (blocks.isEmpty() || !isHolyLandActiveIn(blocks.get(0).getWorld()))
		{
			return;
		}

		blocks.removeIf(block -> HolyLandManager.instance().isNeutralLandLocation(block.getLocation()) || HolyLandManager.instance().getGodAtHolyLandLocation(block.getLocation()) != null);
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

		if (!Gods.instance().isEnabledInWorld(player.getWorld()))
		{
			return;
		}

		String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());
		Material type = event.getItem().getType();
		if (godName != null)
		{
			Long lastEatTime = this.lastEatTimes.get(player.getUniqueId());
			long currentTime = System.currentTimeMillis();
			if ((lastEatTime == null) || (currentTime - lastEatTime.longValue() > 10000L))
			{
				if ((GodsConfiguration.instance().isCommandmentsEnabled()) && (player.getHealth() != player.getAttribute(Attribute.MAX_HEALTH).getValue()))
				{
					if ((player.isOp()) || (PermissionsManager.instance().hasPermission(player, "gods.commandments")))
					{
						GodManager.instance().handleEat(player, godName, type.name());
					}
				}
				if (GodsConfiguration.instance().isQuestsEnabled())
				{
					QuestManager.instance().handleEat(player.getName(), godName, type.name());
				}
				this.lastEatTimes.put(player.getUniqueId(), currentTime);
			}
		}
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event)
	{
		Player player = event.getEntity();
		if (!Gods.instance().isEnabledInWorld(player.getWorld()))
		{
			return;
		}
		String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());

		if (godName != null)
		{
			GodManager.GodType godType = GodManager.instance().getDivineForceForGod(godName);

			GodManager.instance().handleKilledPlayer(player.getUniqueId(), godName, godType);
		}

		Player killer = player.getKiller();
		if (killer == null)
		{
			return;
		}

		String killerGodName = BelieverManager.instance().getGodForBeliever(killer.getUniqueId());
		if (killerGodName == null)
		{
			return;
		}

		LanguageManager.LANGUAGESTRING message = null;

		if (godName == null)
		{
			// War gods reward slaying heathens
			if (GodManager.instance().getDivineForceForGod(killerGodName) == GodManager.GodType.WAR)
			{
				message = LanguageManager.LANGUAGESTRING.YouEarnedPowerBySlayingHeathen;
			}
		}
		else
		{
			List<String> warRelations = GodManager.instance().getWarRelations(killerGodName);
			if ((warRelations != null) && (warRelations.contains(godName)))
			{
				message = LanguageManager.LANGUAGESTRING.YouEarnedPowerBySlayingEnemy;
			}
		}

		if (message != null)
		{
			double powerBefore = BelieverManager.instance().getBelieverPower(killer.getUniqueId());
			BelieverManager.instance().increasePrayer(killer.getUniqueId(), killerGodName, 2);
			BelieverManager.instance().increasePrayerPower(killer.getUniqueId(), 2);
			double powerAfter = BelieverManager.instance().getBelieverPower(killer.getUniqueId());

			Gods.instance().sendInfo(killer.getUniqueId(), message, ChatColor.AQUA, (int) (powerAfter - powerBefore), killerGodName, 20);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void OnPlayerDropItem(PlayerDropItemEvent event)
	{
		Player player = event.getPlayer();
		if (!Gods.instance().isEnabledInWorld(player.getWorld()))
		{
			return;
		}
		if ((!player.isOp()) && (!PermissionsManager.instance().hasPermission(player, "gods.altar.sacrifice")))
		{
			Gods.instance().logDebug("OnPlayerDropItem(): Does not have gods.altar.sacrifice");
			return;
		}
		if (player.getGameMode() == GameMode.CREATIVE)
		{
			return;
		}
		AltarManager.instance().addDroppedItem(event.getItemDrop().getEntityId(), player.getUniqueId());
	}

	/**
	 * Reading a holy book (right-click). Clicks in
	 * the air reach listeners already cancelled, so this handler doesn't ignore
	 * cancelled events; it only skips when using the item in hand was denied.
	 */
	@EventHandler(priority = EventPriority.HIGH)
	public void onHolyBookUse(PlayerInteractEvent event)
	{
		if (!GodsConfiguration.instance().isBiblesEnabled() || event.useItemInHand() == Event.Result.DENY || event.getHand() != EquipmentSlot.HAND)
		{
			return;
		}

		Player player = event.getPlayer();
		if (!Gods.instance().isEnabledInWorld(player.getWorld()))
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
			String godName = HolyBookManager.instance().getGodForBible(book);
			if (godName != null)
			{
				Long lastReadTime = this.lastEatTimes.get(player.getUniqueId());
				long currentTime = System.currentTimeMillis();
				if ((lastReadTime == null) || (currentTime - lastReadTime.longValue() > 10000L))
				{
					QuestManager.instance().handleReadBible(godName, player.getUniqueId());
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
		if (!Gods.instance().isEnabledInWorld(player.getWorld()))
		{
			return;
		}

		if (GodsConfiguration.instance().isQuestsEnabled())
		{
			if (event.getClickedBlock() != null && QuestManager.instance().handleOpenChest(event.getPlayer().getUniqueId(), event.getClickedBlock().getLocation()))
			{
				event.setCancelled(true);
			}
		}

		if (!GodsConfiguration.instance().isHolyLandEnabled())
		{
			return;
		}

		if (!PermissionsManager.instance().hasPermission(event.getPlayer(), "gods.holyland"))
		{
			Gods.instance().logDebug(event.getPlayer().getName() + " does not have holyland permission");
			return;
		}
		if ((!player.isOp()) && (HolyLandManager.instance().isNeutralLandLocation(player.getLocation())))
		{
			if (!GodsConfiguration.instance().isAllowInteractionInNeutralLands())
			{
				event.setCancelled(true);
			}
			return;
		}
		if (event.getClickedBlock() == null)
		{
			return;
		}
		String blockGodName = HolyLandManager.instance().getGodAtHolyLandLocation(event.getClickedBlock().getLocation());
		if (blockGodName == null)
		{
			return;
		}
		if ((GodsConfiguration.instance().getHolylandBreakableBlockTypes().contains(event.getClickedBlock().getType())) || (AltarManager.instance().isAltarBlock(event.getClickedBlock())))
		{
			return;
		}
		if (HolyLandManager.instance().isContestedLand(player.getLocation()))
		{
			player.sendMessage(ChatColor.RED + "This Holy Land is contested! Win the battle before you can access this Holy Land!");
			event.setCancelled(true);
			return;
		}
		String playerGodName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());
		if (playerGodName == null)
		{
			player.sendMessage(ChatColor.RED + "You do not have access to the holy land of " + ChatColor.GOLD + blockGodName);
			event.setCancelled(true);
			return;
		}
		if (!playerGodName.equals(blockGodName))
		{
			if (GodManager.instance().hasAllianceRelation(blockGodName, playerGodName))
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
		if (!Gods.instance().isEnabledInWorld(player.getWorld()))
		{
			return;
		}

		String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());

		if ((event.getAction() == Action.RIGHT_CLICK_AIR) || (event.getAction() == Action.RIGHT_CLICK_BLOCK))
		{
			Material type = player.getInventory().getItemInMainHand().getType();
			if ((event.getHand() == EquipmentSlot.HAND) && (type != Material.AIR) && (godName != null) && (GodsConfiguration.instance().isHolyArtifactsEnabled()))
			{
				HolyArtifactManager.instance().handleActivate(player.getName(), player.getInventory().getItemInMainHand());
			}
		}

		if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
		{
			return;
		}

		Block block = event.getClickedBlock();

		if (!AltarManager.instance().isAltarSign(block))
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

		if (GodsConfiguration.instance().isCursingEnabled())
		{
			Player cursedPlayer = AltarManager.instance().getCursedPlayerFromAltar(block, lines);

			if (cursedPlayer != null)
			{
				if ((godName != null) && GodManager.instance().isPriest(player.getUniqueId()))
				{
					Player oldCursedPlayer = GodManager.instance().getCursedPlayerForGod(godName);
					if ((oldCursedPlayer != null) && oldCursedPlayer == cursedPlayer)
					{
						GodManager.instance().setCursedPlayerForGod(godName, null);

						LanguageManager.instance().setPlayerName(cursedPlayer.getDisplayName());

						GodManager.instance().GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToPriestCursedPlayerUnset, 2);
						GodManager.instance().GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversCursedPlayerUnset, cursedPlayer.getUniqueId());
					}
					else
					{
						GodManager.instance().setCursedPlayerForGod(godName, cursedPlayer.getUniqueId());

						LanguageManager.instance().setPlayerName(cursedPlayer.getDisplayName());

						GodManager.instance().GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToPriestCursedPlayerSet, 2);
						GodManager.instance().GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversCursedPlayerSet, player.getUniqueId());

						Gods.instance().log(player.getName() + " asked " + godName + " for curses on " + cursedPlayer.getName());
					}
				}
				else
				{
					Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.CursesNotAllowed, ChatColor.DARK_RED, 0, "", 1);
				}

				return;
			}
		}
		if (GodsConfiguration.instance().isBlessingEnabled())
		{
			Player blessedPlayer = AltarManager.instance().getBlessedPlayerFromAltarSign(block, lines);
			if (blessedPlayer != null)
			{
				if ((godName != null) && GodManager.instance().isPriest(player.getUniqueId()))
				{
					UUID oldBlessedPlayer = GodManager.instance().getBlessedPlayerForGod(godName);
					if ((oldBlessedPlayer != null) && (oldBlessedPlayer.equals(blessedPlayer.getUniqueId())))
					{
						GodManager.instance().setBlessedPlayerForGod(godName, null);

						LanguageManager.instance().setPlayerName(blessedPlayer.getDisplayName());

						GodManager.instance().GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToPriestBlessedPlayerUnset, 2);

						GodManager.instance().GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversBlessedPlayerUnset, player.getUniqueId());
					}
					else
					{
						GodManager.instance().setBlessedPlayerForGod(godName, blessedPlayer.getUniqueId());

						LanguageManager.instance().setPlayerName(blessedPlayer.getDisplayName());

						GodManager.instance().GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToPriestBlessedPlayerSet, 2);

						GodManager.instance().GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversBlessedPlayerSet, player.getUniqueId());

						Gods.instance().log(player.getName() + " asked " + godName + " for blessings on " + blessedPlayer.getName());
					}
				}
				else
				{
					Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.BlessingsNotAllowed, ChatColor.DARK_RED, 0, "", 1);
				}
				return;
			}
		}

		if ((!player.isOp()) && (!PermissionsManager.instance().hasPermission(player, "gods.altar.pray")))
		{
			Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.AltarPrayingNotAllowed, ChatColor.DARK_RED, 0, "", 1);
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
			Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.InvalidGodName, ChatColor.DARK_RED, 0, "", 1);
			return;
		}

		altarGodName = GodManager.instance().formatGodName(altarGodName);

		if ((Gods.instance().isBlacklistedGod(altarGodName)) || (!Gods.instance().isWhitelistedGod(altarGodName)))
		{
			Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.PrayToBlacklistedGodNotAllowed, ChatColor.DARK_RED, 0, "", 1);
			return;
		}

		if (!GodManager.instance().hasGodAccess(player.getUniqueId(), altarGodName))
		{
			Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.PrivateGodNoAccess, ChatColor.DARK_RED, 0, "", 1);
			return;
		}

		if (GodManager.instance().handleAltarPray(block.getLocation(), player, altarGodName))
		{
			Gods.instance().log(player.getDisplayName() + " prayed to " + altarGodName + " at an altar");
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		GodManager.instance().updateOnlineGods();
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerPickupItem(EntityPickupItemEvent event)
	{
		if (!(event.getEntity() instanceof Player)) {
			return;
		}

		Player player = (Player) event.getEntity();

		if (!Gods.instance().isEnabledInWorld(player.getWorld()))
		{
			return;
		}

		if (GodsConfiguration.instance().isHolyArtifactsEnabled())
		{
			if (HolyArtifactManager.instance().isHolyArtifact(event.getItem().getItemStack()))
			{
				if (HolyArtifactManager.instance().hasHolyArtifact(player.getName()))
				{
					event.setCancelled(true);
					return;
				}
			}
		}

		if (GodsConfiguration.instance().isMarriageEnabled())
		{
			MarriageManager.instance().handlePickupItem(player, event.getItem(), event.getItem().getLocation());
		}

	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		Player player = event.getPlayer();

		this.lastEatTimes.remove(player.getUniqueId());

		if (GodsConfiguration.instance().isHolyLandEnabled())
		{
			HolyLandManager.instance().handleQuit(player.getUniqueId());
		}

		// The player still counts as online during this event
		if (Gods.instance().isEnabled())
		{
			Gods.instance().getServer().getScheduler().runTask(Gods.instance(), () -> GodManager.instance().updateOnlineGods());
		}
	}

	/** Turns a sign written on an altar block into a praying, cursing or blessing altar. */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void OnSignChange(SignChangeEvent event)
	{
		Player player = event.getPlayer();
		Block block = event.getBlock();

		if (!Gods.instance().isEnabledInWorld(block.getWorld()))
		{
			return;
		}

		if (!AltarManager.instance().isAltarSign(block))
		{
			return;
		}

		// An existing altar sign is not rewritten (its editor can still be opened by other plugins)
		if (AltarManager.instance().isFormattedAltarSign(block))
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

		if ((GodsConfiguration.instance().isCursingEnabled()) && (AltarManager.instance().isCursingAltar(block, event.getLines())))
		{
			created = AltarManager.instance().handleNewCursingAltar(event);
		}
		else if ((GodsConfiguration.instance().isBlessingEnabled()) && (AltarManager.instance().isBlessingAltar(block, event.getLines())))
		{
			created = AltarManager.instance().handleNewBlessingAltar(event);
		}
		else
		{
			created = AltarManager.instance().handleNewPrayingAltar(event);
		}

		if (created)
		{
			AltarManager.instance().waxAltarSignLater(block);
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
