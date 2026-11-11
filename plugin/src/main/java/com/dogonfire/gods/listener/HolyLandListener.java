package com.dogonfire.gods.listener;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.projectiles.ProjectileSource;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.model.GodType;
import com.dogonfire.gods.model.HolyLand;
import com.dogonfire.gods.service.HolyLandContest;
import com.dogonfire.gods.service.HolyLandRules;
import com.dogonfire.gods.service.HolyLandService;

/**
 * Holy land and neutral land protection (building, breaking, interacting, PvP, mobs, explosions, teleports), the titles
 * shown when entering land, and kills and meals in contested holy lands. Registered only while holy land is enabled
 * (see {@code Gods#startFeatures}); the logic is in {@link HolyLandService}.
 */
public class HolyLandListener implements Listener
{
	private final GodsContext ctx;

	public HolyLandListener(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	private HolyLandService lands()
	{
		return ctx.holyLandService();
	}

	private boolean isActiveIn(World world)
	{
		return ctx.settings().holyLand().enabled() && world != null && ctx.plugin().isEnabledInWorld(world);
	}

	private static String nullToEmpty(String text)
	{
		return text != null ? text : "";
	}

	// ---------------------------------------------------------------------
	// Protection

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event)
	{
		Player player = event.getPlayer();
		if (!isActiveIn(player.getWorld()))
		{
			return;
		}
		if (player.isOp())
		{
			return;
		}

		if (!ctx.permissions().hasPermission(player, "gods.holyland"))
		{
			ctx.plugin().logDebug(event.getPlayer().getName() + " does not have holyland permission");
			return;
		}

		if (lands().isNeutralLandLocation(event.getBlock().getLocation()))
		{
			ctx.messages().send(player, "holy-land.neutral-no-build");

			event.setCancelled(true);
			return;
		}

		String godName = lands().getGodAtHolyLandLocation(event.getBlock().getLocation());

		if (godName == null)
		{
			return;
		}

		String playerGod = ctx.believerService().getGodForBeliever(player.getUniqueId());

		if ((playerGod == null) || (!playerGod.equals(godName)))
		{
			ctx.messages().send(player, "holy-land.no-access", Messages.god(godName));

			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event)
	{
		Player player = event.getPlayer();

		if (!isActiveIn(player.getWorld()))
		{
			return;
		}
		if (player.isOp())
		{
			return;
		}
		if (!ctx.permissions().hasPermission(player, "gods.holyland"))
		{
			ctx.plugin().logDebug(player.getName() + " does not have holyland permission");
			return;
		}

		Location location = event.getBlock().getLocation();

		if (lands().isNeutralLandLocation(location))
		{
			ctx.messages().send(player, "holy-land.neutral-no-break");
			event.setCancelled(true);
			return;
		}

		String godName = lands().getGodAtHolyLandLocation(location);
		if (godName == null)
		{
			return;
		}

		if (ctx.settings().holyLand().breakableBlocks().contains(event.getBlock().getType()))
		{
			return;
		}

		String attackerGod = ctx.believerService().getGodForBeliever(player.getUniqueId());

		if ((attackerGod == null) || (!attackerGod.equals(godName)))
		{
			ctx.messages().send(player, "holy-land.no-access", Messages.god(godName));
			event.setCancelled(true);
		}
	}

	/** Interacting with blocks in holy lands and neutral lands */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		if (!isActiveIn(player.getWorld()))
		{
			return;
		}

		if (!ctx.permissions().hasPermission(event.getPlayer(), "gods.holyland"))
		{
			ctx.plugin().logDebug(event.getPlayer().getName() + " does not have holyland permission");
			return;
		}
		if ((!player.isOp()) && (lands().isNeutralLandLocation(player.getLocation())))
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
		String blockGodName = lands().getGodAtHolyLandLocation(event.getClickedBlock().getLocation());
		if (blockGodName == null)
		{
			return;
		}
		if ((ctx.settings().holyLand().breakableBlocks().contains(event.getClickedBlock().getType())) || (ctx.altarService().isAltarBlock(event.getClickedBlock())))
		{
			return;
		}
		if (lands().isContestedLand(player.getLocation()))
		{
			ctx.messages().send(player, "holy-land.contested-no-access");
			event.setCancelled(true);
			return;
		}
		String playerGodName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		if (playerGodName == null)
		{
			ctx.messages().send(player, "holy-land.no-access-gold", Messages.god(blockGodName));
			event.setCancelled(true);
			return;
		}
		if (!playerGodName.equals(blockGodName))
		{
			if (ctx.relationService().hasAllianceRelation(blockGodName, playerGodName))
			{
				return;
			}
			if (player.isOp())
			{
				return;
			}
			ctx.messages().send(player, "holy-land.no-access-gold", Messages.god(blockGodName));
			event.setCancelled(true);
		}
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

	/** No fighting in neutral land; in holy land only the god's own believers, its enemies at war, and its mobs if allowed */
	@EventHandler(ignoreCancelled = true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
	{
		Location location = event.getEntity().getLocation();

		if (!isActiveIn(location.getWorld()))
		{
			return;
		}

		if (lands().isNeutralLandLocation(location))
		{
			event.setCancelled(true);
			return;
		}

		String godName = lands().getGodAtHolyLandLocation(location);
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
					ctx.blessingService().smite(godName, player, 3);
				}
				event.setCancelled(true);

				return;
			}
			if (!godName.equals(attackerGodName))
			{
				if ((ctx.settings().holyLand().lightning()) && ((event.getEntity() instanceof Player)))
				{
					ctx.blessingService().smite(godName, player, 3);
				}
				if (!ctx.relationService().hasWarRelation(godName, attackerGodName))
				{
					event.setCancelled(true);
				}
			}
			else if ((event.getEntity() instanceof Player victim) && godName.equals(ctx.believerService().getGodForBeliever(victim.getUniqueId()))
					&& (!ctx.godService().getGodPvP(godName)))
			{
				// Believers fighting each other in their own god's holy land; intruders can always be fought off
				event.setCancelled(true);
			}
		}
		else if ((attacker instanceof LivingEntity) && (!ctx.godService().getGodMobDamage(godName)))
		{
			event.setCancelled(true);
		}
	}

	/** Explosions don't destroy blocks in holy lands or neutral lands. */
	private void protectBlocksFromExplosion(List<Block> blocks)
	{
		if (blocks.isEmpty() || !isActiveIn(blocks.get(0).getWorld()))
		{
			return;
		}

		blocks.removeIf(block -> lands().isNeutralLandLocation(block.getLocation()) || lands().getGodAtHolyLandLocation(block.getLocation()) != null);
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

	/** No hostile mobs in neutral land; in holy land only what its god allows */
	@EventHandler(ignoreCancelled = true)
	public void onCreatureSpawn(CreatureSpawnEvent event)
	{
		// Only what the world spawns by itself: eggs, breeding, golems, commands and plugins are let in
		if (!HolyLandRules.isWorldSpawn(event.getSpawnReason()) || !isActiveIn(event.getLocation().getWorld()))
		{
			return;
		}

		if (lands().isNeutralLandLocation(event.getLocation()))
		{
			if (event.getEntity() instanceof Enemy)
			{
				ctx.plugin().logDebug("Prevented " + event.getEntityType() + " from spawning in Neutral land");
				event.setCancelled(true);
			}
			return;
		}

		String godName = lands().getGodAtHolyLandLocation(event.getLocation());
		if (godName != null)
		{
			if (ctx.godService().getDivineForceForGod(godName) == GodType.NATURE)
			{
				return;
			}

			if (event.getEntity().getType() == ctx.commandmentService().findHolyMobType(godName))
			{
				return;
			}

			if (event.getEntity().getType() == ctx.commandmentService().findUnholyMobType(godName))
			{
				ctx.plugin().logDebug("Prevented unholy mob " + event.getEntityType() + " from spawning in Holy Land of " + godName);
				event.setCancelled(true);
				return;
			}

			if (ctx.godService().getGodMobSpawning(godName))
			{
				return;
			}

			if (!lands().isMobTypeAllowedToSpawn(event.getEntityType()))
			{
				ctx.plugin().logDebug("Prevented banned " + event.getEntityType() + " from spawning in Holy Land of " + godName);
				event.setCancelled(true);
			}
		}
	}

	/** Players can't ender pearl or chorus fruit their way into another god's holy land. */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerTeleport(PlayerTeleportEvent event)
	{
		String cause = event.getCause().name();

		// CONSUMABLE_EFFECT is Paper's name for chorus fruit teleports
		if (!cause.equals("ENDER_PEARL") && !cause.equals("CHORUS_FRUIT") && !cause.equals("CONSUMABLE_EFFECT"))
		{
			return;
		}

		Player player = event.getPlayer();
		Location to = event.getTo();

		if (to == null || !isActiveIn(to.getWorld()))
		{
			return;
		}

		if (player.isOp() || !ctx.permissions().hasPermission(player, "gods.holyland"))
		{
			return;
		}

		String targetLandGodName = lands().getGodAtHolyLandLocation(to);

		if (targetLandGodName == null)
		{
			return;
		}

		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());

		if ((godName == null) || (!targetLandGodName.equals(godName)))
		{
			ctx.godSpeech().sendInfo(player.getUniqueId(), "holy-land.no-teleport", 1, Messages.god(targetLandGodName));
			event.setCancelled(true);
		}
	}

	// ---------------------------------------------------------------------
	// Attacks on holy lands

	/** Kills in a contested holy land count for the side of the killer. */
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event)
	{
		Player player = event.getEntity();

		if (!isActiveIn(player.getWorld()))
		{
			return;
		}

		Player killer = player.getKiller();
		if (killer == null)
		{
			return;
		}

		lands().handleContestKill(player.getLocation(), killer);
	}

	/**
	 * In a contested holy land, outsiders eating the god's unholy food desecrate it (enough desecration hands the land
	 * to the attackers), and the god's believers eating its holy food undo that.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerConsume(PlayerItemConsumeEvent event)
	{
		Player player = event.getPlayer();

		if (!isActiveIn(player.getWorld()))
		{
			return;
		}

		lands().handleContestMeal(player.getLocation(), player, event.getItem().getType());
	}

	// ---------------------------------------------------------------------
	// Entering land

	@EventHandler(ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event)
	{
		if (event.getTo() == null || lands().isSameChunk(event.getFrom(), event.getTo()))
		{
			return;
		}

		Player player = event.getPlayer();
		Location to = event.getTo();

		if (!isActiveIn(to.getWorld()))
		{
			return;
		}

		if (!ctx.permissions().hasPermission(player, "gods.holyland"))
		{
			return;
		}

		String godFrom = lands().getGodAtHolyLandLocationFrom(player.getUniqueId());

		if (lands().isNeutralLandLocation(to))
		{
			lands().setNeutralLandLocationFrom(player.getUniqueId());

			if (!HolyLandService.NEUTRAL_LAND_MARKER.equals(godFrom))
			{
				ctx.titles().send(player, 1 * 20, 3 * 20, 1 * 20, ctx.messages().legacy("holy-land.title.neutral"), ctx.messages().legacy("holy-land.title.neutral-subtitle"));
			}
			return;
		}

		String godTo = lands().getGodAtHolyLandLocationTo(player.getUniqueId(), to);

		HolyLand contest = godTo != null ? lands().getContest(to) : null;
		if (contest != null)
		{
			long minutesLeft = lands().getContestedTimeAtHolyLand(to);
			if (minutesLeft <= 0L)
			{
				// The defenders held out
				lands().resolveContestedLand(to, godTo, contest.attackingGodName(), true);
			}
			else
			{
				int attackerKillsNeeded = HolyLandContest.attackerKillsNeeded(contest);
				int defenderKillsNeeded = HolyLandContest.defenderKillsNeeded(contest);
				int desecration = contest.desecration();

				ctx.titles().send(player, 1 * 20, 4 * 20, 1 * 20, ctx.messages().legacy("holy-land.title.contested", Messages.god(godTo)), ctx.messages().legacy("holy-land.title.contested-subtitle", Messages.text("minutes", minutesLeft)));

				ctx.godSpeech().sendInfo(player.getUniqueId(), "holy-land.contested-kills", 20, Messages.text("minutes", minutesLeft), Messages.text("attacker_kills", attackerKillsNeeded), Messages.text("defender_kills", defenderKillsNeeded));
				ctx.godSpeech().sendInfo(player.getUniqueId(), "holy-land.contested-desecration", 20, Messages.god(godTo), Messages.text("desecrations", desecration));
				return;
			}
		}

		if ((godFrom == null) && (godTo == null))
		{
			return;
		}

		if ((godTo != null) && ((godFrom == null) || !godFrom.equals(godTo)))
		{
			String playerGod = ctx.believerService().getGodForBeliever(player.getUniqueId());
			if ((playerGod != null) && (playerGod.equals(godTo)))
			{
				ctx.titles().send(player, 1 * 20, 3 * 20, 1 * 20, ctx.messages().legacy("holy-land.title.holy-land", Messages.god(godTo)), ctx.messages().legacy("holy-land.title.holy-land-name", Messages.text("name", nullToEmpty(lands().getHolyLandName(to)))));
			}
			else
			{
				ctx.titles().send(player, 1 * 20, 3 * 20, 1 * 20, ctx.messages().legacy("holy-land.title.holy-land", Messages.god(godTo)), ctx.messages().legacy("holy-land.title.other-description", Messages.text("description", nullToEmpty(ctx.godService().getGodDescription(godTo)))));
			}
		}
		else if ((godFrom != null) && (godTo == null))
		{
			ctx.titles().send(player, 1 * 20, 3 * 20, 1 * 20, ctx.messages().legacy("holy-land.title.wilderness"), ctx.messages().legacy("holy-land.title.wilderness-subtitle"));
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		Location location = player.getLocation();

		if (!isActiveIn(location.getWorld()) || !ctx.permissions().hasPermission(player, "gods.holyland"))
		{
			return;
		}

		if (lands().isNeutralLandLocation(location))
		{
			lands().setNeutralLandLocationFrom(player.getUniqueId());
			ctx.titles().send(player, 1 * 20, 3 * 20, 1 * 20, ctx.messages().legacy("holy-land.title.neutral"), ctx.messages().legacy("holy-land.title.neutral-subtitle"));
			return;
		}

		String godName = lands().getGodAtHolyLandLocationTo(player.getUniqueId(), location);

		if (godName != null)
		{
			ctx.titles().send(player, 1 * 20, 3 * 20, 1 * 20, ctx.messages().legacy("holy-land.title.holy-land", Messages.god(godName)), ctx.messages().legacy("holy-land.title.description", Messages.text("description", nullToEmpty(ctx.godService().getGodDescription(godName)))));
		}
		else
		{
			ctx.titles().send(player, 1 * 20, 3 * 20, 1 * 20, ctx.messages().legacy("holy-land.title.wilderness"), ctx.messages().legacy("holy-land.title.wilderness-subtitle"));
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		lands().handleQuit(event.getPlayer().getUniqueId());
	}
}
