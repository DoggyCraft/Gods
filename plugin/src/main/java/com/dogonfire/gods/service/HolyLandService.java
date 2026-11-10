package com.dogonfire.gods.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.model.HolyLand;
import com.dogonfire.gods.model.NeutralLand;
import com.dogonfire.gods.model.RegionKey;
import com.dogonfire.gods.storage.HolyLandRepository;
import com.dogonfire.gods.util.LegacyDates;

/**
 * Holy lands: 128x128 block regions ({@link RegionKey}) claimed by a god when a believer builds or prays at an altar
 * there, neutral lands set by admins, and attacks on holy lands (the rules are in {@link HolyLandContest}). Owns every
 * change to holyland.yml ({@link HolyLandRepository}); every change is written at once, as before. The events are
 * handled by {@code listener/HolyLandListener}. Its data is only loaded while holy land is enabled. Main thread only.
 */
public class HolyLandService
{
	/** {@link #getGodAtHolyLandLocationFrom} for a player last seen in neutral land */
	public static final String			NEUTRAL_LAND_MARKER	= "NeutralLand";

	private static final LegacyDates	DATES				= LegacyDates.MINUTES;

	private final GodsContext			ctx;
	private final HolyLandRepository	repository;

	// The god of the land each player was last seen in (for the titles shown when entering land)
	private final Map<UUID, String>		fromLocations		= new HashMap<UUID, String>();
	private final Random				random				= new Random();

	public HolyLandService(GodsContext ctx, HolyLandRepository repository)
	{
		this.ctx = ctx;
		this.repository = repository;
	}

	public void load()
	{
		this.repository.load();
	}

	// ---------------------------------------------------------------------
	// Regions

	/** The region of a location */
	public static RegionKey regionOf(Location location)
	{
		return RegionKey.of(location.getWorld().getName(), location.getBlockX(), location.getBlockZ());
	}

	/** The region hash of a location (without its world); gods.yml keeps it for a god's attack */
	public long hashLocation(Location location)
	{
		return RegionKey.hash(location.getBlockX(), location.getBlockZ());
	}

	/** Whether two locations are in the same region of the same world */
	public boolean isSameChunk(Location one, Location two)
	{
		return RegionKey.sameRegion(one.getBlockX(), one.getBlockZ(), two.getBlockX(), two.getBlockZ()) && one.getWorld() == two.getWorld();
	}

	private HolyLand find(Location location)
	{
		return this.repository.find(regionOf(location));
	}

	// ---------------------------------------------------------------------
	// Holy lands

	public String getGodAtHolyLandLocation(Location location)
	{
		HolyLand land = find(location);
		return land != null ? land.godName() : null;
	}

	public String getGodAtHolyLandLocationFrom(UUID believerId)
	{
		return this.fromLocations.get(believerId);
	}

	/** The god of the land at {@code location}, remembered as where the player is now */
	public String getGodAtHolyLandLocationTo(UUID believerId, Location location)
	{
		String godName = getGodAtHolyLandLocation(location);

		this.fromLocations.put(believerId, godName);

		return godName;
	}

	public void setNeutralLandLocationFrom(UUID believerId)
	{
		this.fromLocations.put(believerId, NEUTRAL_LAND_MARKER);
	}

	public void handleQuit(UUID playerId)
	{
		this.fromLocations.remove(playerId);
	}

	public String getHolyLandName(Location location)
	{
		HolyLand land = find(location);
		return land != null ? land.name() : null;
	}

	public void setHolyLandName(Location location, String name)
	{
		this.repository.getOrCreate(regionOf(location)).setName(name);

		this.repository.save();
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

	/** The mobs that may spawn in a holy land whose god doesn't allow mob spawning */
	public boolean isMobTypeAllowedToSpawn(EntityType mobType)
	{
		return (mobType == EntityType.BAT) ||
			(mobType == EntityType.SQUID) ||
			(mobType == EntityType.CHICKEN) ||
			(mobType == EntityType.PIG) ||
			(mobType == EntityType.COW) ||
			(mobType == EntityType.OCELOT) ||
			(mobType == EntityType.SHEEP) ||
			(mobType == EntityType.VILLAGER) ||
			(mobType == EntityType.MOOSHROOM) ||
			(mobType == EntityType.IRON_GOLEM);
	}

	/** Gives the land to {@code godName} (keeping when it was first prayed in) and counts it as prayed in now */
	public void setHolyLand(Location location, String godName)
	{
		HolyLand land = this.repository.getOrCreate(regionOf(location));

		Instant now = DATES.now();
		if (land.firstPrayerTime() == null)
		{
			land.setFirstPrayerTime(now);
		}
		land.setGodName(godName);
		land.setLastPrayerTime(now);
		land.setWorldName(location.getWorld().getName());

		this.repository.save();
	}

	/** Removes holy lands nobody has prayed in for the configured number of days */
	public void removeAbandonedLands()
	{
		long timeBefore = System.currentTimeMillis();

		long now = System.currentTimeMillis();
		long maxMinutes = ctx.settings().holyLand().deleteAbandonedAfterDays() * 24L * 60L;
		List<HolyLand> lands = new ArrayList<HolyLand>(this.repository.all());
		int removed = 0;

		for (HolyLand land : lands)
		{
			// No valid prayer time: treated as abandoned
			Instant lastPrayer = land.lastPrayerTime();
			if (lastPrayer == null || (now - lastPrayer.toEpochMilli()) / 60000L > maxMinutes)
			{
				this.repository.remove(land.region());
				removed++;
			}
		}

		if (removed > 0)
		{
			this.repository.save();
		}

		long timeAfter = System.currentTimeMillis();

		ctx.plugin().logDebug("Traversed " + lands.size() + " Holy lands in " + (timeAfter - timeBefore) + " ms, removed " + removed + " abandoned");
	}

	public void removeHolyLandsForGod(String godName)
	{
		boolean changed = false;

		for (HolyLand land : new ArrayList<HolyLand>(this.repository.all()))
		{
			if (godName != null && godName.equals(land.godName()))
			{
				this.repository.remove(land.region());
				changed = true;
			}
		}

		if (changed)
		{
			this.repository.save();
		}
	}

	public int getClaimedChunksForGod(String godName)
	{
		int chunks = 0;

		for (HolyLand land : this.repository.all())
		{
			if (godName != null && godName.equals(land.godName()))
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
		if (getGodAtHolyLandLocation(location) != null)
		{
			return;
		}

		if (getClaimedChunksForGod(godName) >= getMaximumChunksForGod(godName))
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
		ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.HolylandSize, ChatColor.AQUA, claimed, Integer.toString(max), 20 * 5);
	}

	/**
	 * A believer prayed at an altar here: keep the god's land from being abandoned, or claim the land for the god if
	 * it's free.
	 */
	public void addPrayer(Player player, String godName, Location location)
	{
		HolyLand land = find(location);
		String landGodName = land != null ? land.godName() : null;

		if (landGodName != null)
		{
			if (landGodName.equals(godName))
			{
				land.setLastPrayerTime(DATES.now());
				this.repository.save();
			}
			return;
		}

		if (getClaimedChunksForGod(godName) >= getMaximumChunksForGod(godName))
		{
			return;
		}

		setHolyLand(location, godName);

		int claimedAfter = getClaimedChunksForGod(godName);
		int max = getMaximumChunksForGod(godName);

		ctx.languageManager().setPlayerName(player.getName());
		ctx.godManager().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversClaimedHolyLand, 1);
		ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.HolylandSize, ChatColor.AQUA, claimedAfter, Integer.toString(max), 20 * 5);
	}

	// ---------------------------------------------------------------------
	// Neutral lands

	public boolean isNeutralLandLocation(Location location)
	{
		return this.repository.findNeutral(regionOf(location)) != null;
	}

	public void setNeutralLand(Location location)
	{
		this.repository.putNeutral(new NeutralLand(regionOf(location), DATES.now()));

		this.repository.save();
	}

	public void clearNeutralLand(Location location)
	{
		this.repository.removeNeutral(regionOf(location));

		this.repository.save();
	}

	// ---------------------------------------------------------------------
	// Contested lands (a priest started an attack on another god's holy land)

	public boolean isContestedLand(Location location)
	{
		return getAttackingGod(location) != null;
	}

	/** The god attacking the holy land at location, or null */
	public String getAttackingGod(Location location)
	{
		HolyLand land = find(location);
		return land != null ? land.attackingGodName() : null;
	}

	public void setContestedLand(Location location, String attackingGodName)
	{
		HolyLand land = this.repository.getOrCreate(regionOf(location));

		land.setAttackingGodName(attackingGodName);
		land.setContestedTime(DATES.now());

		this.repository.save();
	}

	public void clearContestedLand(Location location)
	{
		HolyLand land = find(location);
		if (land != null)
		{
			land.clearContest();
		}

		this.repository.save();
	}

	/** Minutes left of the attack on this holy land; 0 when the defenders have held out. */
	public long getContestedTimeAtHolyLand(Location location)
	{
		HolyLand land = find(location);
		return HolyLandContest.minutesLeft(land != null ? land.contestedTime() : null, Instant.now());
	}

	/**
	 * True if godName is attacking the holy land at location right now. An attack whose time ran out is settled here
	 * (the defenders win), so an attack nobody visits still ends.
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

	/** The status of the attack on the land at location, for the title shown when entering it; null if none */
	public HolyLand getContest(Location location)
	{
		HolyLand land = find(location);
		return land != null && land.attackingGodName() != null ? land : null;
	}

	/** A player was killed by {@code killer} in a contested holy land: the kill counts for the killer's side */
	public void handleContestKill(Location location, Player killer)
	{
		HolyLand land = getContest(location);
		if (land == null)
		{
			return;
		}

		String defendingGodName = land.godName();
		String killerGodName = ctx.believerService().getGodForBeliever(killer.getUniqueId());
		if (killerGodName == null || defendingGodName == null)
		{
			return;
		}

		String attackingGodName = land.attackingGodName();
		if (!killerGodName.equals(attackingGodName) && !killerGodName.equals(defendingGodName))
		{
			return;
		}

		HolyLandContest.Outcome outcome = HolyLandContest.recordKill(land, killerGodName);
		this.repository.save();
		settle(location, defendingGodName, attackingGodName, outcome);
	}

	/**
	 * A player ate in a contested holy land: outsiders eating the god's unholy food desecrate it, the god's believers
	 * eating its holy food undo that.
	 */
	public void handleContestMeal(Location location, Player player, Material food)
	{
		HolyLand land = getContest(location);
		if (land == null || land.godName() == null)
		{
			return;
		}

		String holyLandGodName = land.godName();
		String attackingGodName = land.attackingGodName();
		String playerGodName = ctx.believerService().getGodForBeliever(player.getUniqueId());

		boolean unholyFood = food == ctx.godManager().getUnholyFoodTypeForGod(holyLandGodName);
		boolean holyFood = food == ctx.godManager().getHolyFoodTypeForGod(holyLandGodName);
		HolyLandContest.Outcome outcome = HolyLandContest.recordMeal(land, playerGodName, unholyFood, holyFood);
		if (outcome == null)
		{
			return;
		}
		this.repository.save();
		settle(location, holyLandGodName, attackingGodName, outcome);
	}

	private void settle(Location location, String defendingGodName, String attackingGodName, HolyLandContest.Outcome outcome)
	{
		if (outcome == HolyLandContest.Outcome.ATTACKERS_WIN)
		{
			resolveContestedLand(location, defendingGodName, attackingGodName, false);
		}
		else if (outcome == HolyLandContest.Outcome.DEFENDERS_WIN)
		{
			resolveContestedLand(location, defendingGodName, attackingGodName, true);
		}
	}

	/** Ends the attack on the land at location: the land goes to the attackers unless the defenders won */
	public void resolveContestedLand(Location location, String defendingGodName, String attackingGodName, boolean defenderWins)
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
}
