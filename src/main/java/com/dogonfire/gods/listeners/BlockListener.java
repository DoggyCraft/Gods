package com.dogonfire.gods.listeners;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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
import org.bukkit.inventory.ItemStack;

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
	private HashMap<String, Long> lastEatTimes = new HashMap<String, Long>();

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void OnBlockPlace(BlockPlaceEvent event)
	{
		if (!GodsConfiguration.instance().isHolyLandEnabled())
		{
			return;
		}
		Player player = event.getPlayer();
		if ((player == null) || (!Gods.instance().isEnabledInWorld(player.getWorld())))
		{
			return;
		}
		if (player.isOp())
		{
			return;
		}
		if (event.getBlock() == null)
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
		String playerGod = null;
		
		if (godName == null)
		{
			return;
		}
		
		if (player != null)
		{
			playerGod = BelieverManager.instance().getGodForBeliever(player.getUniqueId());
		}
		
		if ((playerGod == null) || (!playerGod.equals(godName)))
		{
			player.sendMessage(ChatColor.RED + "You do not have access to the holy land of " + ChatColor.YELLOW + godName);

			event.setCancelled(true);
			return;
		}
	
	}

	@EventHandler
	public void OnEntityCombust(EntityCombustEvent event)
	{
		if (!GodsConfiguration.instance().isSacrificesEnabled())
		{
			return;
		}
		if (event.getEntity() == null)
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

	@EventHandler
	public void OnEntityDamageByEntity(EntityDamageByEntityEvent event)
	{
		if (event.getDamager() == null)
		{
			return;
		}
		if (!Gods.instance().isEnabledInWorld(event.getDamager().getWorld()))
		{
			return;
		}
		if (GodsConfiguration.instance().isQuestsEnabled())
		{
			if ((event.getDamager() instanceof Player))
			{
				Player attackerPlayer = (Player) event.getDamager();
				if ((event.getEntity() instanceof Player))
				{
					Player victimPlayer = (Player) event.getEntity();

					String attackerGodName = BelieverManager.instance().getGodForBeliever(attackerPlayer.getUniqueId());
					String victimGodName = BelieverManager.instance().getGodForBeliever(victimPlayer.getUniqueId());
					if ((attackerGodName != null) && (victimGodName != null))
					{
						if (GodManager.instance().hasWarRelation(attackerGodName, victimGodName))
						{
							Gods.instance().logDebug("hasWarRelation");

							event.setCancelled(false);
						}
					}
				}
			}
		}
		if (!GodsConfiguration.instance().isHolyLandEnabled())
		{
			return;
		}
		if (HolyLandManager.instance().isNeutralLandLocation(event.getEntity().getLocation()))
		{
			event.setCancelled(true);
		}
		String godName = HolyLandManager.instance().getGodAtHolyLandLocation(event.getEntity().getLocation());
		if (godName != null)
		{
			if ((event.getDamager() instanceof Player))
			{
				Player player = (Player) event.getDamager();

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
				else if ((event.getEntity() instanceof Player))
				{
					GodManager.instance().getGodPvP(godName);
				}
			}
			else if (((event.getDamager() instanceof LivingEntity)) && (!GodManager.instance().getGodMobDamage(godName)))
			{
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler
	public void OnEntityDeath(EntityDeathEvent event)
	{
		if (!(event.getEntity().getKiller() instanceof Player))
		{
			return;
		}

		Player player = event.getEntity().getKiller();

		if (!Gods.instance().isEnabledInWorld(player.getWorld()))
		{
			return;
		}

		String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());

		if (godName == null)
		{
			return;
		}

		// if (Gods.instance().propheciesEnabled)
		// {
		// Gods.instance().getProphecyManager().handleMobKill(player.getName(),
		// godName,
		// event.getEntityType().name());
		// }

		if (GodsConfiguration.instance().isQuestsEnabled())
		{
			QuestManager.instance().handleKilledMob(godName, event.getEntityType().name());
		}

		if (GodsConfiguration.instance().isHolyArtifactsEnabled())
		{
			HolyArtifactManager.instance().handleDeath(event.getEntity().getKiller().getName(), godName, event.getEntity().getKiller().getInventory().getItemInMainHand());
		}

		if ((!player.isOp()) && (!PermissionsManager.instance().hasPermission(player, "gods.commandments")))
		{
			return;
		}

		GodManager.instance().handleKilled(player, godName, event.getEntityType().name());
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event)
	{
		if (!GodsConfiguration.instance().isHolyLandEnabled())
		{
			return;
		}
		String targetLandGodName = HolyLandManager.instance().getGodAtHolyLandLocation(event.getLocation());
		if (targetLandGodName != null)
		{
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void OnPlayerConsume(PlayerItemConsumeEvent event)
	{
		Player player = event.getPlayer();

		String godName = BelieverManager.instance().getGodForBeliever(event.getPlayer().getUniqueId());
		Material type = player.getInventory().getItemInMainHand().getType();
		if (godName != null)
		{
			Long lastEatTime = this.lastEatTimes.get(player.getName());
			Long currentTime = Long.valueOf(System.currentTimeMillis());
			if ((lastEatTime == null) || (currentTime.longValue() - lastEatTime.longValue() > 10000L))
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
				this.lastEatTimes.put(player.getName(), currentTime);
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
		GodManager.GodType godType = GodManager.instance().getDivineForceForGod(godName);

		GodManager.instance().handleKilledPlayer(player.getUniqueId(), godName, godType);
		QuestManager.instance().handleKilledPlayer(player.getUniqueId(), godName);

		double powerAfter = 0.0D;
		double powerBefore = 0.0D;
		if ((event.getEntity().getKiller() != null) && ((event.getEntity().getKiller() instanceof Player)))
		{
			Player killer = event.getEntity().getKiller();
			String killerGodName = BelieverManager.instance().getGodForBeliever(killer.getUniqueId());
			if (killerGodName != null)
			{
				if (godName == null)
				{
					if (GodManager.instance().getDivineForceForGod(killerGodName) == GodManager.GodType.WAR)
					{
						powerBefore = BelieverManager.instance().getBelieverPower(player.getUniqueId());
						BelieverManager.instance().increasePrayer(killer.getUniqueId(), killerGodName, 2);
						BelieverManager.instance().increasePrayerPower(killer.getUniqueId(), 2);
						powerAfter = BelieverManager.instance().getBelieverPower(player.getUniqueId());

						Gods.instance().sendInfo(killer.getUniqueId(), LanguageManager.LANGUAGESTRING.YouEarnedPowerBySlayingHeathen, ChatColor.AQUA, (int) (powerAfter - powerBefore), killerGodName, 20);
					}
				}
				else
				{
					List<String> warRelations = GodManager.instance().getWarRelations(killerGodName);
					if (warRelations != null)
					{
						if (warRelations.contains(godName))
						{
							powerBefore = BelieverManager.instance().getBelieverPower(player.getUniqueId());
							BelieverManager.instance().increasePrayer(killer.getUniqueId(), killerGodName, 2);
							BelieverManager.instance().increasePrayerPower(killer.getUniqueId(), 2);
							powerAfter = BelieverManager.instance().getBelieverPower(player.getUniqueId());

							Gods.instance().sendInfo(killer.getUniqueId(), LanguageManager.LANGUAGESTRING.YouEarnedPowerBySlayingEnemy, ChatColor.AQUA, (int) (powerAfter - powerBefore), killerGodName, 20);
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void OnPlayerDropItem(PlayerDropItemEvent event)
	{
		Player player = event.getPlayer();
		if ((player == null) || !Gods.instance().isEnabledInWorld(player.getWorld()))
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
		if (GodsConfiguration.instance().isHolyArtifactsEnabled())
		{
			HolyArtifactManager.instance().handleDrop(player.getName(), event.getItemDrop(), event.getItemDrop().getLocation());
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		if ((player == null) || (!Gods.instance().isEnabledInWorld(player.getWorld())))
		{
			return;
		}

		if (GodsConfiguration.instance().isQuestsEnabled())
		{
/*
			if (event.getAction() == Action.PHYSICAL)
			{
				if (QuestManager.instance().handlePressurePlate(player.getUniqueId(), event.getClickedBlock()))
				{
					event.setCancelled(true);
				}
			}
*/
			if (event.getClickedBlock() != null && QuestManager.instance().handleOpenChest(event.getPlayer().getUniqueId(), event.getClickedBlock().getLocation()))
			{
				event.setCancelled(true);
			}
		}

		if (GodsConfiguration.instance().isBiblesEnabled())
		{
			if ((event.getAction().equals(Action.RIGHT_CLICK_AIR)) || (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)))
			{
				if ((event.getItem() != null) && (player.getInventory().getItemInMainHand() != null))
				{
					ItemStack book = player.getInventory().getItemInMainHand();
					if (book.getType() == Material.WRITTEN_BOOK)
					{
						String godName = HolyBookManager.instance().getGodForBible(book);
						if (godName != null)
						{
							Long lastEatTime = this.lastEatTimes.get(player.getName());
							Long currentTime = Long.valueOf(System.currentTimeMillis());
							if ((lastEatTime == null) || (currentTime.longValue() - lastEatTime.longValue() > 10000L))
							{
								GodManager.instance().handleReadBible(godName, player);
								QuestManager.instance().handleReadBible(godName, player.getUniqueId());
								this.lastEatTimes.put(player.getName(), currentTime);
							}
						}
					}
				}
			}
			else if (((event.getAction().equals(Action.LEFT_CLICK_AIR)) || (event.getAction().equals(Action.LEFT_CLICK_BLOCK))) && ((event.getItem() != null) && (player.getInventory().getItemInMainHand() != null)))
			{
				ItemStack book = player.getInventory().getItemInMainHand();
				if (book.getType() == Material.WRITTEN_BOOK)
				{
					String godName = HolyBookManager.instance().getGodForBible(book);
					if (godName != null)
					{
						GodManager.instance().handleBibleMelee(godName, player);
						QuestManager.instance().handleBibleMelee(godName, player.getUniqueId());
					}
				}
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
			return;
		}
	}

	@EventHandler
	public void OnPlayerInteract(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		String godName = null;
		if ((player == null) || (!Gods.instance().isEnabledInWorld(player.getWorld())))
		{
			return;
		}
		if ((event.getAction().equals(Action.RIGHT_CLICK_AIR)) || (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)))
		{
			Material type = player.getInventory().getItemInMainHand().getType();
			if ((type != null) && (type != Material.AIR))
			{
				godName = BelieverManager.instance().getGodForBeliever(event.getPlayer().getUniqueId());
				if (godName != null)
				{
					if (GodsConfiguration.instance().isHolyArtifactsEnabled())
					{
						HolyArtifactManager.instance().handleActivate(player.getName(), player.getInventory().getItemInMainHand());
					}
				}
			}
		}
		if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
		{
			if (!AltarManager.instance().isAltarSign(event.getClickedBlock()))
			{
				return;
			}

			BlockState state = event.getClickedBlock().getState();

			Sign sign = (Sign) state;
			if (GodsConfiguration.instance().isCursingEnabled())
			{
				Player cursedPlayer = AltarManager.instance().getCursedPlayerFromAltar(event.getClickedBlock(), sign.getLines());

				if (cursedPlayer != null)
				{
					if (GodManager.instance().isPriest(player.getUniqueId()))
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

							Gods.instance().log(player.getName() + " asked " + godName + " for curses on " + cursedPlayer);
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
				Player blessedPlayer = AltarManager.instance().getBlessedPlayerFromAltarSign(event.getClickedBlock(), sign.getLines());
				if (blessedPlayer != null)
				{
					if (GodManager.instance().isPriest(player.getUniqueId()))
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

							Gods.instance().log(player.getName() + " asked " + godName + " for blessings on " + blessedPlayer);
						}
					}
					else
					{
						Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.BlessingsNotAllowed, ChatColor.DARK_RED, 0, "", 1);
					}
					return;
				}
			}

			if ((!event.getPlayer().isOp()) && (!PermissionsManager.instance().hasPermission(event.getPlayer(), "gods.altar.pray")))
			{
				Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.AltarPrayingNotAllowed, ChatColor.DARK_RED, 0, "", 1);
				return;
			}

			Block block = event.getClickedBlock();

			if (!AltarManager.instance().isPrayingAltar(block))
			{
				return;
			}

			godName = sign.getLine(2);

			if (godName == null)
			{
				return;
			}

			godName = godName.trim();

			if (godName.length() <= 1)
			{
				Gods.instance().sendInfo(event.getPlayer().getUniqueId(), LanguageManager.LANGUAGESTRING.InvalidGodName, ChatColor.DARK_RED, 0, "", 1);
				return;
			}

			godName = GodManager.instance().formatGodName(godName);

			if ((Gods.instance().isBlacklistedGod(godName)) || (!Gods.instance().isWhitelistedGod(godName)))
			{
				Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.PrayToBlacklistedGodNotAllowed, ChatColor.DARK_RED, 0, "", 1);
				return;
			}

			if (!GodManager.instance().hasGodAccess(player.getUniqueId(), godName))
			{
				Gods.instance().sendInfo(event.getPlayer().getUniqueId(), LanguageManager.LANGUAGESTRING.PrivateGodNoAccess, ChatColor.DARK_RED, 0, "", 1);
				return;
			}

			if (GodManager.instance().handleAltarPray(block.getLocation(), event.getPlayer(), godName))
			{
				Gods.instance().log(event.getPlayer().getDisplayName() + " prayed to " + godName + " at an altar");
			}
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		GodManager.instance().updateOnlineGods();
	}

	@EventHandler
	public void onPlayerPickupItem(EntityPickupItemEvent event)
	{
		if (!(event.getEntity() instanceof Player)) {
			return;
		}

		Player player = (Player) event.getEntity();

		if (player == null || !Gods.instance().isEnabledInWorld(player.getWorld()))
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

		if (GodsConfiguration.instance().isQuestsEnabled())
		{
			QuestManager.instance().handlePickupItem(player.getName(), event.getItem(), event.getItem().getLocation());
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		Player player = event.getPlayer();

		this.lastEatTimes.remove(player.getName());
		if (!GodsConfiguration.instance().isHolyLandEnabled())
		{
			return;
		}
		HolyLandManager.instance().handleQuit(player.getUniqueId());

		GodManager.instance().updateOnlineGods();
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void OnSignChange(SignChangeEvent event)
	{
		Player player = event.getPlayer();

		if ((player == null) || (!Gods.instance().isEnabledInWorld(player.getWorld())))
		{
			return;
		}

		if ((GodsConfiguration.instance().isCursingEnabled()) && (AltarManager.instance().isCursingAltar(event.getBlock(), event.getLines())))
		{
			if (!AltarManager.instance().handleNewCursingAltar(event))
			{
				event.setCancelled(true);
				event.getBlock().setType(Material.AIR);
				event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(Material.OAK_SIGN, 1));
			}
			return;
		}

		if ((GodsConfiguration.instance().isBlessingEnabled()) && (AltarManager.instance().isBlessingAltar(event.getBlock(), event.getLines())))
		{
			if (!AltarManager.instance().handleNewBlessingAltar(event))
			{
				event.setCancelled(true);
				event.getBlock().setType(Material.AIR);
				event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(Material.OAK_SIGN, 1));
			}
			return;
		}

		if (AltarManager.instance().isPrayingAltar(event.getBlock()))
		{
			if (!AltarManager.instance().handleNewPrayingAltar(event))
			{
				ItemStack sign = new ItemStack(Material.OAK_SIGN);
				event.setCancelled(true);
				event.getBlock().setType(Material.AIR);
				event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), sign);
			}
			return;
		}

		if (AltarManager.instance().isRitualAltar(event.getBlock(), event.getLines()))
		{
			if (!AltarManager.instance().handleNewRitualAltar(event))
			{
				event.setCancelled(true);
				event.getBlock().setType(Material.AIR);
				event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(Material.OAK_SIGN, 1));
			}
			return;
		}
	}
}