package com.dogonfire.gods.managers;

import java.io.File;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.storage.DataFile;

/**
 * Holy lands: 128x128 block regions claimed by a god when a believer builds or
 * prays at an altar there. Stored in holyland.yml as
 * <pre>
 * Holylands.&lt;world&gt;.&lt;region hash&gt;.GodName / Name / FirstPrayerTime / LastPrayerTime / World / AttackingGodName / ContestedTime / AttackerKills / DefenderKills / Desecration
 * NeutralLands.&lt;world&gt;.&lt;region hash&gt;: &lt;time set&gt;
 * </pre>
 * Gods before 1.0 stored them under "Holyland.&lt;hash&gt;" and "NeutralLand.&lt;hash&gt;"
 * with a broken hash (many regions, in every world, shared one key), so that
 * data can't be converted; see {@link #load()}.
 */
public class HolyLandManager implements Listener
{
	private static final String		HOLY_LANDS				= "Holylands";
	private static final String		NEUTRAL_LANDS			= "NeutralLands";
	private static final String		LEGACY_HOLY_LANDS		= "Holyland";
	private static final String		LEGACY_NEUTRAL_LANDS	= "NeutralLand";
	private static final String		NEUTRAL_LAND_MARKER		= "NeutralLand";

	/** Holy land regions are 2^7 = 128 blocks wide. */
	private static final int		REGION_SHIFT			= 7;

	/** How long an attack on a holy land lasts before the defenders win. */
	private static final long		CONTEST_MINUTES			= 60L;
	// Kills (or unholy meals) one side needs to win a contested holy land
	private static final int		ATTACKER_KILLS_TO_WIN	= 10;
	private static final int		DEFENDER_KILLS_TO_WIN	= 5;
	private static final int		DESECRATION_TO_WIN		= 30;

	private HashMap<UUID, String>	fromLocations			= new HashMap<UUID, String>();

	private String					pattern					= "HH:mm dd-MM-yyyy";
	private DateFormat				formatter				= new SimpleDateFormat(this.pattern);
	private Random 					random 					= new Random();

	private final GodsContext ctx;

	public HolyLandManager(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	/** holyland.yml; see {@link DataFile} */
	private DataFile landFile()
	{
		return ctx.storage().holyLands();
	}

	private YamlConfiguration landConfig()
	{
		return landFile().yaml();
	}

	// ---------------------------------------------------------------------
	// Keys

	/** A 128x128 region of a world as one number: region X in the high 32 bits, region Z in the low 32 bits. */
	public long hashLocation(Location location)
	{
		long regionX = location.getBlockX() >> REGION_SHIFT;
		long regionZ = location.getBlockZ() >> REGION_SHIFT;

		return (regionX << 32) | (regionZ & 0xFFFFFFFFL);
	}

	/** World names may contain '.', the YAML path separator. */
	private static String worldKey(World world)
	{
		return world.getName().replace('.', '_');
	}

	private String landPath(Location location)
	{
		return HOLY_LANDS + "." + worldKey(location.getWorld()) + "." + hashLocation(location);
	}

	private String neutralLandPath(Location location)
	{
		return NEUTRAL_LANDS + "." + worldKey(location.getWorld()) + "." + hashLocation(location);
	}

	/** The paths ("Holylands.&lt;world&gt;.&lt;hash&gt;") of every holy land. */
	private List<String> getAllLandPaths()
	{
		List<String> paths = new ArrayList<String>();

		ConfigurationSection lands = landConfig().getConfigurationSection(HOLY_LANDS);
		if (lands == null)
		{
			return paths;
		}

		for (String world : lands.getKeys(false))
		{
			ConfigurationSection worldLands = lands.getConfigurationSection(world);
			if (worldLands == null)
			{
				continue;
			}

			for (String hash : worldLands.getKeys(false))
			{
				paths.add(HOLY_LANDS + "." + world + "." + hash);
			}
		}

		return paths;
	}

	// ---------------------------------------------------------------------
	// Loading and saving

	public void load()
	{
		landFile().load();
		if (!landFile().isReadOnly())
		{
			dropLegacyLands();
		}

		ctx.plugin().log("Loaded " + getAllLandPaths().size() + " holy land entries.");
	}

	/**
	 * The old format can't be converted: its hash put many regions (from every
	 * world) under the same key, so which land was where is lost. Keep a backup
	 * of the file and drop the old entries; gods claim their land again when
	 * believers build or pray at their altars.
	 */
	private void dropLegacyLands()
	{
		if (!landConfig().contains(LEGACY_HOLY_LANDS) && !landConfig().contains(LEGACY_NEUTRAL_LANDS))
		{
			return;
		}

		File backup = new File(landFile().path().getParent().toFile(), "holyland.yml.v1.bak");
		if (backup.exists())
		{
			backup = new File(landFile().path().getParent().toFile(), "holyland.yml.v1." + System.currentTimeMillis() + ".bak");
		}

		try
		{
			Files.copy(landFile().path(), backup.toPath());
		}
		catch (Exception ex)
		{
			ctx.plugin().getLogger().severe("Could not back up holyland.yml to " + backup.getName() + ": " + ex.getMessage() + ". Leaving the old holy lands in place.");
			return;
		}

		landConfig().set(LEGACY_HOLY_LANDS, null);
		landConfig().set(LEGACY_NEUTRAL_LANDS, null);

		landFile().save();

		ctx.plugin().getLogger().warning("holyland.yml was in the old format, whose land positions were broken and can't be converted.");
		ctx.plugin().getLogger().warning("All holy lands and neutral lands were reset; the old file is kept as " + backup.getName() + ".");
		ctx.plugin().getLogger().warning("Gods claim holy land again when believers build or pray at their altars. Set neutral lands again with /g setsafe.");
	}

	// ---------------------------------------------------------------------
	// Holy lands

	public String getGodAtHolyLandLocation(Location location)
	{
		return landConfig().getString(landPath(location) + ".GodName");
	}

	public String getGodAtHolyLandLocationFrom(UUID believerId)
	{
		return this.fromLocations.get(believerId);
	}

	public String getGodAtHolyLandLocationTo(UUID believerId, Location location)
	{
		String godName = getGodAtHolyLandLocation(location);

		this.fromLocations.put(believerId, godName);

		return godName;
	}

	public String getHolyLandName(Location location)
	{
		return landConfig().getString(landPath(location) + ".Name");
	}

	public void setHolyLandName(Location location, String name)
	{
		landConfig().set(landPath(location) + ".Name", name);

		landFile().save();
	}

	private static String nullToEmpty(String text)
	{
		return text != null ? text : "";
	}

	private String generateHolyLandName(Biome type)
	{
		String[] first = new String[] {"Upper ", "Lower ", "High ", ""};
		String[] second = new String[] {"Flower", "Grass", "Cold"};
		String[] third = new String[] {"place", "field", "shire"};

		// Biome is no longer an enum, so this can't be a switch
		if (type == Biome.SUNFLOWER_PLAINS || type == Biome.PLAINS)
		{
			first = new String[] {"Upper ", "Lower ", "High ", ""};
			second = new String[] {"Flower", "Grass", "Cold"};
			third = new String[] {"place", "field", "shire"};
		}
		else if (type == Biome.DARK_FOREST || type == Biome.BIRCH_FOREST)
		{
			first = new String[] {"Upper ", "Lower ", "High ", ""};
			second = new String[] {"Fire", "Dank", "Cold"};
			third = new String[] {"forest", "wood", ""};
		}
		else if (type == Biome.WINDSWEPT_HILLS)
		{
			first = new String[] {"Upper ", "Lower ", "High ", ""};
			second = new String[] {"Rock", "High", "Sky"};
			third = new String[] {"hill", "mountain"};
		}

		return first[random.nextInt(first.length)] + second[random.nextInt(second.length)] + third[random.nextInt(third.length)];
	}

	public void handleQuit(UUID playerId)
	{
		this.fromLocations.remove(playerId);
	}

	public boolean isMobTypeAllowedToSpawn(EntityType mobType)
	{
		if ((mobType == EntityType.BAT) ||
			(mobType == EntityType.SQUID) ||
			(mobType == EntityType.CHICKEN) ||
			(mobType == EntityType.PIG) ||
			(mobType == EntityType.COW) ||
			(mobType == EntityType.OCELOT) ||
			(mobType == EntityType.SHEEP) ||
			(mobType == EntityType.VILLAGER) ||
			(mobType == EntityType.MOOSHROOM) ||
			(mobType == EntityType.IRON_GOLEM))
		{
			return true;
		}
		return false;
	}

	public boolean isSameChunk(Location one, Location two)
	{
		if ((one.getBlockX() >> REGION_SHIFT) != (two.getBlockX() >> REGION_SHIFT))
		{
			return false;
		}
		if ((one.getBlockZ() >> REGION_SHIFT) != (two.getBlockZ() >> REGION_SHIFT))
		{
			return false;
		}
		if (one.getWorld() != two.getWorld())
		{
			return false;
		}
		return true;
	}

	public void setHolyLand(Location location, String godName)
	{
		String path = landPath(location);

		Date thisDate = new Date();
		if (landConfig().getString(path + ".FirstPrayerTime") == null)
		{
			landConfig().set(path + ".FirstPrayerTime", this.formatter.format(thisDate));
		}
		landConfig().set(path + ".GodName", godName);
		landConfig().set(path + ".LastPrayerTime", this.formatter.format(thisDate));
		landConfig().set(path + ".World", location.getWorld().getName());

		landFile().save();
	}

	private void unsetHolyLand(String path)
	{
		landConfig().set(path, null);
	}

	/** Removes holy lands nobody has prayed in for HolyLand.DaysForAbandonedHolyLands days. */
	public void removeAbandonedLands()
	{
		long timeBefore = System.currentTimeMillis();

		Date thisDate = new Date();
		long maxMinutes = ctx.settings().holyLand().deleteAbandonedAfterDays() * 24L * 60L;
		List<String> paths = getAllLandPaths();
		int removed = 0;

		for (String path : paths)
		{
			Date lastPrayerDate = null;
			try
			{
				lastPrayerDate = this.formatter.parse(landConfig().getString(path + ".LastPrayerTime"));
			}
			catch (Exception ex)
			{
				// No valid prayer time: treated as abandoned
			}

			if (lastPrayerDate == null || (thisDate.getTime() - lastPrayerDate.getTime()) / 60000L > maxMinutes)
			{
				unsetHolyLand(path);
				removed++;
			}
		}

		if (removed > 0)
		{
			landFile().save();
		}

		long timeAfter = System.currentTimeMillis();

		ctx.plugin().logDebug("Traversed " + paths.size() + " Holy lands in " + (timeAfter - timeBefore) + " ms, removed " + removed + " abandoned");
	}

	public void removeHolyLandsForGod(String godName)
	{
		boolean changed = false;

		for (String path : getAllLandPaths())
		{
			if (godName != null && godName.equals(landConfig().getString(path + ".GodName")))
			{
				unsetHolyLand(path);
				changed = true;
			}
		}

		if (changed)
		{
			landFile().save();
		}
	}

	public int getClaimedChunksForGod(String godName)
	{
		int chunks = 0;

		for (String path : getAllLandPaths())
		{
			if (godName != null && godName.equals(landConfig().getString(path + ".GodName")))
			{
				chunks++;
			}
		}

		return chunks;
	}

	public int getMaximumChunksForGod(String godName)
	{
		return (int) (ctx.godManager().getGodPower(godName) / 10 + 3);
	}

	/** A believer built an altar here: claim the land for the god if it's free. */
	public void addAltar(Player player, String godName, Location location)
	{
		if(getGodAtHolyLandLocation(location) != null)
		{
			return;
		}

		if(getClaimedChunksForGod(godName) >= getMaximumChunksForGod(godName))
		{
			ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.CannotClaimMoreHolyland, ChatColor.RED, 0, "", 1);
			return;
		}

		setHolyLand(location, godName);
		setHolyLandName(location, generateHolyLandName(location.getBlock().getBiome()));

		int claimed = getClaimedChunksForGod(godName);
		int max = getMaximumChunksForGod(godName);

		ctx.languageManager().setPlayerName(player.getName());
		ctx.godManager().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversClaimedHolyLand, 1);
		ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.HolylandSize, ChatColor.AQUA, claimed, Integer.toString(max), 20*5);
	}

	/**
	 * A believer prayed at an altar here: keep the god's land from being
	 * abandoned, or claim the land for the god if it's free.
	 */
	public void addPrayer(Player player, String godName, Location location)
	{
		String landGodName = getGodAtHolyLandLocation(location);

		if (landGodName != null)
		{
			if (landGodName.equals(godName))
			{
				landConfig().set(landPath(location) + ".LastPrayerTime", this.formatter.format(new Date()));
				landFile().save();
			}
			return;
		}

		if(getClaimedChunksForGod(godName) >= getMaximumChunksForGod(godName))
		{
			return;
		}

		setHolyLand(location, godName);

		int claimedAfter = getClaimedChunksForGod(godName);
		int max = getMaximumChunksForGod(godName);

		ctx.languageManager().setPlayerName(player.getName());
		ctx.godManager().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversClaimedHolyLand, 1);
		ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.HolylandSize, ChatColor.AQUA, claimedAfter, Integer.toString(max), 20*5);
	}

	// ---------------------------------------------------------------------
	// Neutral lands

	public boolean isNeutralLandLocation(Location location)
	{
		return landConfig().contains(neutralLandPath(location));
	}

	public void setNeutralLand(Location location)
	{
		landConfig().set(neutralLandPath(location), this.formatter.format(new Date()));

		landFile().save();
	}

	public void clearNeutralLand(Location location)
	{
		landConfig().set(neutralLandPath(location), null);

		landFile().save();
	}

	public void setNeutralLandLocationFrom(UUID believerId)
	{
		this.fromLocations.put(believerId, NEUTRAL_LAND_MARKER);
	}

	// ---------------------------------------------------------------------
	// Contested lands (a priest started an attack on another god's holy land)

	public boolean isContestedLand(Location location)
	{
		return getAttackingGod(location) != null;
	}

	private String getAttackingGod(Location location)
	{
		return landConfig().getString(landPath(location) + ".AttackingGodName");
	}

	public void setContestedLand(Location location, String attackingGodName)
	{
		String path = landPath(location);

		landConfig().set(path + ".AttackingGodName", attackingGodName);
		landConfig().set(path + ".ContestedTime", this.formatter.format(new Date()));

		landFile().save();
	}

	public void clearContestedLand(Location location)
	{
		String path = landPath(location);

		landConfig().set(path + ".AttackingGodName", null);
		landConfig().set(path + ".ContestedTime", null);
		landConfig().set(path + ".AttackerKills", null);
		landConfig().set(path + ".DefenderKills", null);
		landConfig().set(path + ".Desecration", null);

		landFile().save();
	}

	/** Minutes left of the attack on this holy land; 0 when the defenders have held out. */
	public long getContestedTimeAtHolyLand(Location location)
	{
		Date contestedDate;
		try
		{
			contestedDate = this.formatter.parse(landConfig().getString(landPath(location) + ".ContestedTime"));
		}
		catch (Exception ex)
		{
			return 0L;
		}

		long elapsedMinutes = (System.currentTimeMillis() - contestedDate.getTime()) / 60000L;

		return Math.max(0L, CONTEST_MINUTES - elapsedMinutes);
	}

	/**
	 * True if godName is attacking the holy land at location right now. An attack whose time ran out is
	 * settled here (the defenders win), so an attack nobody visits still ends.
	 */
	public boolean isAttackingHolyLand(String godName, Location location)
	{
		if (location == null || location.getWorld() == null || !godName.equals(getAttackingGod(location)))
		{
			return false;
		}
		if (getContestedTimeAtHolyLand(location) <= 0L)
		{
			resolveContestedLand(location, getGodAtHolyLandLocation(location), godName, true);
			return false;
		}
		return true;
	}

	/** True if some god is attacking the holy land at location right now. */
	public boolean isUnderAttack(Location location)
	{
		String attackingGodName = getAttackingGod(location);
		return attackingGodName != null && isAttackingHolyLand(attackingGodName, location);
	}

	/** Adds n to a counter of the contested land at location and returns the new value. */
	private int addToContestedCounter(Location location, String counter, int n)
	{
		String path = landPath(location) + "." + counter;
		int value = Math.max(0, landConfig().getInt(path) + n);
		landConfig().set(path, value);
		landFile().save();
		return value;
	}

	private void resolveContestedLand(Location location, String defendingGodName, String attackingGodName, boolean defenderWins)
	{
		if (defendingGodName != null && attackingGodName != null)
		{
			if (defenderWins)
			{
				ctx.godManager().godSayToBelievers(defendingGodName, LanguageManager.LANGUAGESTRING.GodToBelieversDefendHolyLandSuccess, 1);
				ctx.godManager().godSayToBelievers(attackingGodName, LanguageManager.LANGUAGESTRING.GodToBelieversAttackHolyLandFailed, 1);
			}
			else
			{
				setHolyLand(location, attackingGodName);
				ctx.godManager().godSayToBelievers(defendingGodName, LanguageManager.LANGUAGESTRING.GodToBelieversDefendHolyLandFailed, 1);
				ctx.godManager().godSayToBelievers(attackingGodName, LanguageManager.LANGUAGESTRING.GodToBelieversAttackHolyLandSuccess, 1);
			}
		}

		clearContestedLand(location);

		// Only the attacker's attack ends; the defender may be attacking a land of its own
		if (attackingGodName != null)
		{
			ctx.godManager().clearContestedHolyLandForGod(attackingGodName);
		}
	}

	// ---------------------------------------------------------------------
	// Events

	private boolean isActiveIn(World world)
	{
		return ctx.settings().holyLand().enabled() && world != null && ctx.plugin().isEnabledInWorld(world);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void OnBlockBreak(BlockBreakEvent event)
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
		if (!ctx.permissionsManager().hasPermission(player, "gods.holyland"))
		{
			ctx.plugin().logDebug(player.getName() + " does not have holyland permission");
			return;
		}

		Location location = event.getBlock().getLocation();

		if (isNeutralLandLocation(location))
		{
			player.sendMessage(ChatColor.RED + "You cannot break blocks in neutral land");
			event.setCancelled(true);
			return;
		}

		String godName = getGodAtHolyLandLocation(location);
		if (godName == null)
		{
			return;
		}

		if (ctx.settings().holyLand().breakableBlocks().contains(event.getBlock().getType()))
		{
			return;
		}

		String attackerGod = ctx.believerManager().getGodForBeliever(player.getUniqueId());

		if ((attackerGod == null) || (!attackerGod.equals(godName)))
		{
			player.sendMessage(ChatColor.RED + "You do not have access to the holy land of " + ChatColor.YELLOW + godName);
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void OnCreatureSpawn(CreatureSpawnEvent event)
	{
		if (!isActiveIn(event.getLocation().getWorld()))
		{
			return;
		}

		if (isNeutralLandLocation(event.getLocation()))
		{
			if (event.getEntity() instanceof Enemy)
			{
				ctx.plugin().logDebug("Prevented " + event.getEntityType() + " from spawning in Neutral land");
				event.setCancelled(true);
			}
			return;
		}

		String godName = getGodAtHolyLandLocation(event.getLocation());
		if (godName != null)
		{
			if (ctx.godManager().getDivineForceForGod(godName) == GodManager.GodType.NATURE)
			{
				return;
			}

			if (event.getEntity().getType() == ctx.godManager().getHolyMobTypeForGod(godName))
			{
				return;
			}

			if (event.getEntity().getType() == ctx.godManager().getUnholyMobTypeForGod(godName))
			{
				ctx.plugin().logDebug("Prevented unholy mob " + event.getEntityType() + " from spawning in Holy Land of " + godName);
				event.setCancelled(true);
				return;
			}

			if (ctx.godManager().getGodMobSpawning(godName))
			{
				return;
			}

			if (!isMobTypeAllowedToSpawn(event.getEntityType()))
			{
				ctx.plugin().logDebug("Prevented banned " + event.getEntityType() + " from spawning in Holy Land of " + godName);
				event.setCancelled(true);
			}
		}
	}

	/** Kills in a contested holy land count for the side of the killer. */
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event)
	{
		Player player = event.getEntity();

		if (!isActiveIn(player.getWorld()))
		{
			return;
		}

		Location location = player.getLocation();
		String attackingGodName = getAttackingGod(location);
		if (attackingGodName == null)
		{
			return;
		}

		Player killer = player.getKiller();
		if (killer == null)
		{
			return;
		}

		String defendingGodName = getGodAtHolyLandLocation(location);
		String killerGodName = ctx.believerManager().getGodForBeliever(killer.getUniqueId());

		if (killerGodName == null || defendingGodName == null)
		{
			return;
		}

		if (killerGodName.equals(attackingGodName))
		{
			if (addToContestedCounter(location, "AttackerKills", 1) >= ATTACKER_KILLS_TO_WIN)
			{
				resolveContestedLand(location, defendingGodName, attackingGodName, false);
			}
		}
		else if (killerGodName.equals(defendingGodName))
		{
			if (addToContestedCounter(location, "DefenderKills", 1) >= DEFENDER_KILLS_TO_WIN)
			{
				resolveContestedLand(location, defendingGodName, attackingGodName, true);
			}
		}
	}

	/**
	 * In a contested holy land, outsiders eating the god's unholy food desecrate
	 * it (enough desecration hands the land to the attackers), and the god's
	 * believers eating its holy food undo that.
	 */
	@EventHandler(ignoreCancelled = true)
	public void OnPlayerConsume(PlayerItemConsumeEvent event)
	{
		Player player = event.getPlayer();

		if (!isActiveIn(player.getWorld()))
		{
			return;
		}

		Location location = player.getLocation();
		String attackingGodName = getAttackingGod(location);
		if (attackingGodName == null)
		{
			return;
		}

		String holylandGodName = getGodAtHolyLandLocation(location);
		if (holylandGodName == null)
		{
			return;
		}

		String playerGodName = ctx.believerManager().getGodForBeliever(player.getUniqueId());
		Material food = event.getItem().getType();

		if (!holylandGodName.equals(playerGodName))
		{
			if (food != ctx.godManager().getUnholyFoodTypeForGod(holylandGodName))
			{
				return;
			}

			if (addToContestedCounter(location, "Desecration", 1) >= DESECRATION_TO_WIN)
			{
				resolveContestedLand(location, holylandGodName, attackingGodName, false);
			}
		}
		else
		{
			if (food != ctx.godManager().getHolyFoodTypeForGod(holylandGodName))
			{
				return;
			}

			addToContestedCounter(location, "Desecration", -1);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event)
	{
		if (event.getTo() == null || isSameChunk(event.getFrom(), event.getTo()))
		{
			return;
		}

		Player player = event.getPlayer();
		Location to = event.getTo();

		if (!isActiveIn(to.getWorld()))
		{
			return;
		}

		if (!ctx.permissionsManager().hasPermission(player, "gods.holyland"))
		{
			return;
		}

		String godFrom = getGodAtHolyLandLocationFrom(player.getUniqueId());

		if (isNeutralLandLocation(to))
		{
			setNeutralLandLocationFrom(player.getUniqueId());

			if (!NEUTRAL_LAND_MARKER.equals(godFrom))
			{
				TitleManager.sendTitle(player, 1*20, 3*20, 1*20, ChatColor.WHITE + "Neutral Land", ChatColor.GREEN + "You are safe against mobs and PvP");
			}
			return;
		}

		String godTo = getGodAtHolyLandLocationTo(player.getUniqueId(), to);

		if (godTo != null && isContestedLand(to))
		{
			long minutesLeft = getContestedTimeAtHolyLand(to);
			if (minutesLeft <= 0L)
			{
				// The defenders held out
				resolveContestedLand(to, godTo, getAttackingGod(to), true);
			}
			else
			{
				String path = landPath(to);
				int attackerKillsNeeded = Math.max(0, ATTACKER_KILLS_TO_WIN - landConfig().getInt(path + ".AttackerKills"));
				int defenderKillsNeeded = Math.max(0, DEFENDER_KILLS_TO_WIN - landConfig().getInt(path + ".DefenderKills"));
				int desecration = landConfig().getInt(path + ".Desecration");

				TitleManager.sendTitle(player, 1*20, 4*20, 1*20, ChatColor.GOLD + "Holyland of " + godTo, ChatColor.RED + "Contested " + minutesLeft + " min");

				ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.ContestedLandKillsStatus, ChatColor.AQUA, String.valueOf(minutesLeft), attackerKillsNeeded, defenderKillsNeeded, 20);
				ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.ContestedLandDesecrationStatus, ChatColor.AQUA, godTo, 0, desecration, 20);
				return;
			}
		}

		if ((godFrom == null) && (godTo == null))
		{
			return;
		}

		if ((godTo != null) && ((godFrom == null) || !godFrom.equals(godTo)))
		{
			String playerGod = ctx.believerManager().getGodForBeliever(player.getUniqueId());
			if ((playerGod != null) && (playerGod.equals(godTo)))
			{
				TitleManager.sendTitle(player, 1*20, 3*20, 1*20, ChatColor.GOLD + "Holyland of " + godTo, ChatColor.WHITE + nullToEmpty(this.getHolyLandName(to)));
			}
			else
			{
				TitleManager.sendTitle(player, 1*20, 3*20, 1*20, ChatColor.GOLD + "Holyland of " + godTo, nullToEmpty(ctx.godManager().getGodDescription(godTo)));
			}
		}
		else if ((godFrom != null) && (godTo == null))
		{
			TitleManager.sendTitle(player, 1*20, 3*20, 1*20, ChatColor.DARK_GREEN + "Wilderness", "");
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

		if (player.isOp() || !ctx.permissionsManager().hasPermission(player, "gods.holyland"))
		{
			return;
		}

		String targetLandGodName = getGodAtHolyLandLocation(to);

		if (targetLandGodName == null)
		{
			return;
		}

		String godName = ctx.believerManager().getGodForBeliever(player.getUniqueId());

		if ((godName == null) || (!targetLandGodName.equals(godName)))
		{
			ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.TeleportIntoHolylandNotAllowed, ChatColor.DARK_RED, 0, targetLandGodName, 1);
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		Location location = player.getLocation();

		if (!isActiveIn(location.getWorld()) || !ctx.permissionsManager().hasPermission(player, "gods.holyland"))
		{
			return;
		}

		if (isNeutralLandLocation(location))
		{
			setNeutralLandLocationFrom(player.getUniqueId());
			TitleManager.sendTitle(player, 1*20, 3*20, 1*20, ChatColor.WHITE + "Neutral Land", ChatColor.GREEN + "You are safe against mobs and PvP");
			return;
		}

		String godName = getGodAtHolyLandLocationTo(player.getUniqueId(), location);

		if (godName != null)
		{
			TitleManager.sendTitle(player, 1*20, 3*20, 1*20, ChatColor.GOLD + "Holyland of " + godName, ChatColor.WHITE + nullToEmpty(ctx.godManager().getGodDescription(godName)));
		}
		else
		{
			TitleManager.sendTitle(player, 1*20, 3*20, 1*20, ChatColor.DARK_GREEN + "Wilderness", "");
		}
	}
}
