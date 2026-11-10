package com.dogonfire.gods.managers;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.GodsConfiguration;

public class HolyLandManager implements Listener
{
	private static HolyLandManager instance;

	public static HolyLandManager instance()
	{
		if (instance == null)
			instance = new HolyLandManager();
		return instance;
	}

	private FileConfiguration		landConfig		= null;
	private File					landConfigFile	= null;
	private HashMap<UUID, String>	fromLocations	= new HashMap<UUID, String>();
	private HashMap<Location, Long>	hashedLocations	= new HashMap<Location, Long>();

	private String					pattern			= "HH:mm dd-MM-yyyy";
	private DateFormat				formatter		= new SimpleDateFormat(this.pattern);
	private Random 					random 			= new Random();

	private HolyLandManager()
	{
	}

	public void clearContestedLand(Location location)
	{
		Date thisDate = new Date();

		long hash = hashLocation(location);

		this.landConfig.set("Holyland." + hash + ".AttackingGodName", null);
		this.landConfig.set("Holyland." + hash + ".ContestedTime", this.formatter.format(thisDate));

		save();
	}

	public void clearNeutralLand(Location location)
	{
		long hash = hashLocation(location);

		this.landConfig.set("NeutralLand." + hash, null);

		save();
	}

	/*
	public boolean deleteGodAtHolyLandLocation(Location location)
	{
		Location clampedLocation = new Location(null, location.getBlockX(), 0.0D, location.getBlockZ());

		long holylandHash = hashLocation(clampedLocation);

		String godName = this.landConfig.getString("HolyLands." + holylandHash + ".GodName");
		if (godName != null)
		{
			this.landConfig.set("HolyLands." + holylandHash + ".GodName", null);

			save();

			return true;
		}
		return false;
	}*/

	public Set<String> getBelievers()
	{
		Set<String> allBelievers = this.landConfig.getKeys(false);

		return allBelievers;
	}

	public long getContestedTimeAtHolyLand(Location location)
	{
		Date thisDate = new Date();
		Date contestedDate = null;
		String contestedTime = this.landConfig.getString("Holyland." + hashLocation(location) + ".ContestedTime");
		try
		{
			contestedDate = this.formatter.parse(contestedTime);
		}
		catch (Exception ex)
		{
			contestedDate = new Date();
			contestedDate.setTime(0L);
		}
		long diff = thisDate.getTime() - contestedDate.getTime();
		long diffMinutes = diff / 60000L % 60L;

		return diffMinutes;
	}

	public String getGodAtHolyLandLocation(Location location)
	{
		String godName = this.landConfig.getString("Holyland." + hashLocation(location) + ".GodName");
		if (godName != null)
		{
			return godName;
		}
		return null;
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
		String name = this.landConfig.getString("Holyland." + hashLocation(location) + ".Name");
		if (name != null)
		{
			return name;
		}
		return null;
	}

	public void setHolyLandName(Location location, String name)
	{
		this.landConfig.set("Holyland." + hashLocation(location) + ".Name", name);
		
		save();
	}	

	public void sendTitleForHolyLand(Player player, String godName, Location location)
	{
		TitleManager.sendTitle(player, 1*20, 3*20, 1*20, ChatColor.GOLD + "Holyland of " + godName, ChatColor.WHITE + this.getHolyLandName(location));
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

	public String getNearestBeliever(Location location)
	{
		Set<String> allBelievers = this.landConfig.getKeys(false);
		double minLength = 999999.0D;
		Player minPlayer = null;
		for (String believer : allBelievers)
		{
			Player player = Gods.instance().getServer().getPlayer(UUID.fromString(believer));
			if ((player != null) && (player.getWorld() == location.getWorld()))
			{
				double length = player.getLocation().subtract(location).length();
				if (length < minLength)
				{
					minLength = length;
					minPlayer = player;
				}
			}
		}
		if (minPlayer == null)
		{
			return null;
		}
		return minPlayer.getName();
	}

	public void handleQuit(UUID playerId)
	{
		this.fromLocations.remove(playerId);
	}

	public long hashLocation(Location location)
	{
		if (this.hashedLocations.containsKey(location))
		{
			return this.hashedLocations.get(location).longValue();
		}
		
		//Gods.instance().log("setHolyLand getBlockX=" + location.getBlockX() + " getBlockZ=" + location.getBlockZ());

		int chunkX = location.getBlockX() >> 7;
		int chunkZ = location.getBlockZ() >> 7;

		//Gods.instance().log("setHolyLand chunkX=" + chunkX + " chunkZ=" + chunkZ);
		
		long x = chunkX << 32;
		long z = chunkZ & 0xFFFFFFFF;

		//Gods.instance().log("setHolyLand x=" + x + " z=" + z);
		
		long hash = x | z;

		//Gods.instance().log("setHolyLand hash=" + hash);

		this.hashedLocations.put(location, hash);

		return hash;
	}

	public boolean isContestedLand(Location location)
	{
		return this.landConfig.getString("Holyland." + hashLocation(location) + ".AttackingGodName") != null;
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

	public boolean isNeutralLandLocation(Location location)
	{
		return this.landConfig.getString("NeutralLand." + hashLocation(location)) != null;
	}

	public boolean isSameChunk(Location one, Location two)
	{
		if ((one.getBlockX() >> 7) != (two.getBlockX() >> 7))
		{
			return false;
		}
		if ((one.getBlockZ() >> 7) != (two.getBlockZ() >> 7))
		{
			return false;
		}
		if (one.getWorld() != two.getWorld())
		{
			return false;
		}
		return true;
	}

	public void load()
	{
		if (this.landConfigFile == null)
		{
			this.landConfigFile = new File(Gods.instance().getDataFolder(), "holyland.yml");
		}
		this.landConfig = YamlConfiguration.loadConfiguration(this.landConfigFile);

		Gods.instance().log("Loaded " + this.landConfig.getKeys(false).size() + " holy land entries.");
	}

	@EventHandler
	public void OnBlockBreak(BlockBreakEvent event)
	{
		Player player = event.getPlayer();
		if (player != null)
		{
			if (!Gods.instance().isEnabledInWorld(player.getWorld()))
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
		}
		if (event.getBlock() == null)
		{
			return;
		}
		if (isNeutralLandLocation(event.getBlock().getLocation()))
		{
			if (player != null)
			{
				player.sendMessage(ChatColor.RED + "You cannot break blocks in neutral land");
			}
			event.setCancelled(true);
			return;
		}
		String godName = getGodAtHolyLandLocation(event.getBlock().getLocation());
		Player attacker = event.getPlayer();
		String attackerGod = null;
		if (godName == null)
		{
			return;
		}

		if (attacker != null)
		{
			attackerGod = BelieverManager.instance().getGodForBeliever(attacker.getUniqueId());
		}

		if ((attackerGod == null) || (!attackerGod.equals(godName)))
		{
			if (attacker != null)
			{
				attacker.sendMessage(ChatColor.RED + "You do not have access to the holy land of " + ChatColor.YELLOW + godName);
			}
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void OnCreatureSpawn(CreatureSpawnEvent event)
	{
		if (!Gods.instance().isEnabledInWorld(event.getLocation().getWorld()))
		{
			return;
		}
		
		if (isNeutralLandLocation(event.getLocation()))
		{
			Gods.instance().logDebug("Prevented " + event.getEntityType() + " from spawning in Neutral land");
			return;
		}
		
		String godName = getGodAtHolyLandLocation(event.getLocation());
		if (godName != null)
		{
			if (GodManager.instance().getDivineForceForGod(godName) == GodManager.GodType.NATURE)
			{
				return;
			}
			
			if (event.getEntity().getType() == GodManager.instance().getHolyMobTypeForGod(godName))
			{
				return;
			}
			
			if (event.getEntity().getType() == GodManager.instance().getUnholyMobTypeForGod(godName))
			{
				Gods.instance().logDebug("Prevented unholy mob " + event.getEntityType() + " from spawning in Holy Land of " + godName);
				event.setCancelled(true);
				return;
			}

			if (GodManager.instance().getGodMobSpawning(godName))
			{
				return;
			}
					
			if (!isMobTypeAllowedToSpawn(event.getEntityType()))
			{
				Gods.instance().logDebug("Prevented banned " + event.getEntityType() + " from spawning in Holy Land of " + godName);
				event.setCancelled(true);
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
		
		if (!isContestedLand(player.getLocation()))
		{
			return;
		}
		
		String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());

		Player killer = event.getEntity().getKiller();
		String killerGodName = BelieverManager.instance().getGodForBeliever(killer.getUniqueId());
		
		if (GodManager.instance().increaseContestedHolyLandKillsForGod(killerGodName, 1))
		{
			resolveContestedLand(player.getLocation(), godName, killerGodName, false);
		}
	}
	
	@EventHandler
	public void OnPlayerConsume(PlayerItemConsumeEvent event)
	{
		Player player = event.getPlayer();

		if (!Gods.instance().isEnabledInWorld(player.getWorld()))
		{
			return;
		}
		
		if (!isContestedLand(player.getLocation()))
		{
			return;
		}
		
		String playerGodName = BelieverManager.instance().getGodForBeliever(event.getPlayer().getUniqueId());
		String holylandGodName = getGodAtHolyLandLocation(player.getLocation());
		Material food = player.getInventory().getItemInMainHand().getType();
		Material holyFood = GodManager.instance().getHolyFoodTypeForGod(holylandGodName);
		Material unholyFood = GodManager.instance().getUnholyFoodTypeForGod(holylandGodName);
		
		if(!playerGodName.equals(holylandGodName))
		{
			if(!food.equals(unholyFood))
			{				
				return;
			}
			
			if (GodManager.instance().increaseContestedHolyLandDesecrationForGod(holylandGodName, 1))
			{
				return;
			}
			
			if (GodManager.instance().increaseContestedHolyLandDesecrationForGod(holylandGodName, 1))
			{
				int desecration = GodManager.instance().getContestedHolyLandDesecrationForGod(holylandGodName);
				resolveContestedLand(player.getLocation(), holylandGodName, playerGodName, false);
				//TODO: Send as title to all believers Gods.instance().sendInfoToBelievers(player.getUniqueId(), LanguageManager.LANGUAGESTRING.ContestedLandDesecrationStatus, ChatColor.AQUA, holylandGodName, 0, desecration, 20);
			}			
		}
		else
		{
			if(!food.equals(holyFood))
			{				
				return;
			}
			
			if (GodManager.instance().increaseContestedHolyLandDesecrationForGod(holylandGodName, -1))
			{
				return;
			}
			
			if (GodManager.instance().increaseContestedHolyLandDesecrationForGod(holylandGodName, -1))
			{
				int desecration = GodManager.instance().getContestedHolyLandDesecrationForGod(holylandGodName);
				resolveContestedLand(player.getLocation(), holylandGodName, playerGodName, false);
				//TODO: Send as title to all believers  Gods.instance().sendInfoToBelievers(player.getUniqueId(), LanguageManager.LANGUAGESTRING.ContestedLandDesecrationStatus, ChatColor.AQUA, holylandGodName, 0, desecration, 20);
			}			
		}
		
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event)
	{
		if (!GodsConfiguration.instance().isHolyLandEnabled())
		{
			return;
		}
		
		if (isSameChunk(event.getFrom(), event.getTo()))
		{
			return;
		}
		
		Player player = event.getPlayer();
		if (!Gods.instance().isEnabledInWorld(player.getWorld()))
		{
			return;
		}
		
		if (!PermissionsManager.instance().hasPermission(player, "gods.holyland"))
		{
			return;
		}
		
		Location to = event.getTo();

		String godFrom = null;
		String godTo = null;

		godFrom = getGodAtHolyLandLocationFrom(player.getUniqueId());
		if (isNeutralLandLocation(to))
		{
			godTo = "NeutralLand";
			setNeutralLandLocationFrom(player.getUniqueId());

			TitleManager.sendTitle(player, 1*20, 3*20, 1*20, ChatColor.WHITE + "Neutral Land", ChatColor.GREEN + "You are safe against mobs and PvP");

			//Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.EnterNeutralLandInfo, ChatColor.YELLOW, "You are safe against mobs and PvP", "You are safe against mobs and PvP", 1);
			return;
		}
		
		if (isContestedLand(to))
		{
			long time = getContestedTimeAtHolyLand(to);
			if (time <= 0L)
			{
				godTo = getGodAtHolyLandLocationTo(player.getUniqueId(), to);
				resolveContestedLand(to, godTo, godFrom, true);
			}
			else
			{
				int defenderKillsNeeded = 5;
				int attackerKillsNeeded = 10;
				int desecration = GodManager.instance().getContestedHolyLandDesecrationForGod(godTo);

				TitleManager.sendTitle(player, 1*20, 4*20, 1*20, ChatColor.GOLD + "Holyland of " + godTo, ChatColor.RED + "Contested " + time +" min");
				
				//Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.EnterContestedLandInfo, ChatColor.RED, (int) time, godTo, 1);
				Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.ContestedLandKillsStatus, ChatColor.AQUA, String.valueOf(time), attackerKillsNeeded, defenderKillsNeeded, 20);
				Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.ContestedLandDesecrationStatus, ChatColor.AQUA, godTo, 0, desecration, 20);
			}
		}
		else
		{
			godTo = getGodAtHolyLandLocationTo(player.getUniqueId(), to);
		}
		
		if ((godFrom == null) && (godTo == null))
		{
			Gods.instance().log("Wilderness");
			return;
		}
		
		if ((godTo != null) && ((godFrom == null) || !godFrom.equals(godTo)))
		{
			String playerGod = BelieverManager.instance().getGodForBeliever(player.getUniqueId());
			if ((playerGod != null) && (playerGod.equals(godTo)))
			{
				TitleManager.sendTitle(player, 1*20, 3*20, 1*20, ChatColor.GOLD + "Holyland of " + godTo, ChatColor.WHITE + this.getHolyLandName(to));
			}
			else
			{
				TitleManager.sendTitle(player, 1*20, 3*20, 1*20, ChatColor.GOLD + "Holyland of " + godTo, GodManager.instance().getGodDescription(godTo));
			}
		}
		else if ((godFrom != null) && (godTo == null))
		{
			TitleManager.sendTitle(player, 1*20, 3*20, 1*20, ChatColor.DARK_GREEN + "Wilderness", ChatColor.WHITE + "");
		}
	}

	
	
	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event)
	{
		if (!Gods.instance().isEnabledInWorld(event.getPlayer().getWorld()))
		{
			return;
		}

		String godName = BelieverManager.instance().getGodForBeliever(event.getPlayer().getUniqueId());

		String targetLandGodName = getGodAtHolyLandLocation(event.getTo());

		if (event.getPlayer().isOp())
		{
			return;
		}

		if (targetLandGodName != null)
		{
			if ((godName == null) || (!targetLandGodName.equals(godName)))
			{
				Gods.instance().sendInfo(event.getPlayer().getUniqueId(), LanguageManager.LANGUAGESTRING.TeleportIntoHolylandNotAllowed, ChatColor.DARK_RED, 0, targetLandGodName, 1);
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		String godName = getGodAtHolyLandLocation(event.getPlayer().getLocation());

		if (isNeutralLandLocation(event.getPlayer().getLocation()))
		{
			TitleManager.sendTitle(event.getPlayer(), 1*20, 3*20, 1*20, ChatColor.WHITE + "Neutral Land", ChatColor.GREEN + "You are safe against mobs and PvP");
		}		
		else if (godName != null)
		{
			TitleManager.sendTitle(event.getPlayer(), 1*20, 3*20, 1*20, ChatColor.GOLD + "Holyland of " + godName, ChatColor.WHITE + GodManager.instance().getGodDescription(godName));
		}
		else
		{
			TitleManager.sendTitle(event.getPlayer(), 1*20, 3*20, 1*20, ChatColor.DARK_GREEN + "Wilderness", ChatColor.WHITE + "");
		}	
	}
	
	public void removeAbandonedLands()
	{
		long timeBefore = System.currentTimeMillis();

		Date thisDate = new Date();
		for (String holylandHash : this.landConfig.getConfigurationSection("Holyland").getKeys(false))
		{
			//if (this.landConfig.getString("NeutralLand." + holylandHash) != null)
			{
				String lastPrayerString = this.landConfig.getString("HolyLand." + holylandHash + ".LastPrayerTime");

				String pattern = "HH:mm dd-MM-yyyy";

				DateFormat formatter = new SimpleDateFormat(pattern);

				Date lastPrayerDate = null;
				try
				{
					lastPrayerDate = formatter.parse(lastPrayerString);
				}
				catch (Exception ex)
				{
					unsetHolyLand(Long.parseLong(holylandHash));
				}
				
				if(lastPrayerDate == null)
				{
					unsetHolyLand(Long.parseLong(holylandHash));
				}
				else
				{
					long diff = thisDate.getTime() - lastPrayerDate.getTime();
					long diffMinutes = diff / 60000L;
					if (diffMinutes > GodsConfiguration.instance().getNumberOfDaysForAbandonedHolyLands() * 24 * 60)
					{
						unsetHolyLand(Long.parseLong(holylandHash));
					}
				}
			}
		}
		long timeAfter = System.currentTimeMillis();

		Gods.instance().logDebug("Traversed " + this.landConfig.getKeys(false).size() + " Holy lands in " + (timeAfter - timeBefore) + " ms");
	}

	private void resolveContestedLand(Location location, String godName1, String godName2, boolean firstIsWinner)
	{
		if (firstIsWinner)
		{
			setHolyLand(location, godName1);
			GodManager.instance().godSayToBelievers(godName1, LanguageManager.LANGUAGESTRING.GodToBelieversDefendHolyLandSuccess, 1);
			GodManager.instance().godSayToBelievers(godName2, LanguageManager.LANGUAGESTRING.GodToBelieversAttackHolyLandFailed, 1);
		}
		else
		{
			setHolyLand(location, godName2);
			GodManager.instance().godSayToBelievers(godName1, LanguageManager.LANGUAGESTRING.GodToBelieversDefendHolyLandFailed, 1);
			GodManager.instance().godSayToBelievers(godName2, LanguageManager.LANGUAGESTRING.GodToBelieversAttackHolyLandSuccess, 1);
		}
		clearContestedLand(location);

		GodManager.instance().clearContestedHolyLandForGod(godName1);
		GodManager.instance().clearContestedHolyLandForGod(godName2);
	}

	public void save()
	{
		if ((this.landConfig == null) || (this.landConfigFile == null))
		{
			return;
		}
		try
		{
			this.landConfig.save(this.landConfigFile);
		}
		catch (Exception ex)
		{
			Gods.instance().log("Could not save config to " + this.landConfigFile + ": " + ex.getMessage());
		}
	}

	public void setContestedLand(Location location, String attackingGodName)
	{
		this.landConfig.set(hashLocation(location) + ".AttackingGodName", attackingGodName);

		save();
	}

	public void removeHolyLandsForGod(String godName)
	{
		for (String hash : this.landConfig.getConfigurationSection("Holyland").getKeys(false))
		{
			String holylandGodName = this.landConfig.getString("Holyland." + hash + ".GodName");
			
			if(holylandGodName.equals(godName))
			{
				unsetHolyLand(Long.parseLong(hash));
			}
		}
	}
	
	public void setHolyLand(Location location, String godName)
	{
		long hash = hashLocation(location);
		
		Date thisDate = new Date();
		if (this.landConfig.getString("Holyland." + hash + ".FirstPrayerTime") == null)
		{
			this.landConfig.set("Holyland." + hash + ".FirstPrayerTime", this.formatter.format(thisDate));
		}
		this.landConfig.set("Holyland." + hash + ".GodName", godName);
		this.landConfig.set("Holyland." + hash + ".LastPrayerTime", this.formatter.format(thisDate));
		this.landConfig.set("Holyland." + hash + ".World", location.getWorld().getName());

		save();
	}

	public void unsetHolyLand(Location location)
	{
		long hash = hashLocation(location);
		unsetHolyLand(hash);
	}

	public void unsetHolyLand(long hash)
	{
		this.landConfig.set("Holyland." + hash, null);
		
		save();
	}

	public void setNeutralLand(Location location)
	{
		Date thisDate = new Date();

		long hash = hashLocation(location);

		this.landConfig.set("NeutralLand." + hash, this.formatter.format(thisDate));

		save();
	}

	public void setNeutralLandLocationFrom(UUID believerId)
	{
		this.fromLocations.put(believerId, "NeutralLand");
	}

	public int getClaimedChunksForGod(String godName)
	{
		int chunks = 0;
		
		ConfigurationSection section = this.landConfig.getConfigurationSection("Holyland");
		
		if(section==null)
		{
			return 0;
		}
		
		for(String hash : section.getKeys(false))
		{
			if(this.landConfig.getString("Holyland." + hash + ".GodName").equals(godName))
			{
				chunks++;
			}
		}
		
		return chunks;
	}

	public int getMaximumChunksForGod(String godName)
	{
		return (int) (GodManager.instance().getGodPower(godName) / 10 + 3);		
	}
	
	public void addAltar(Player player, String godName, Location location)
	{
		if(getGodAtHolyLandLocation(location) != null)
		{
			return;		
		}

		if(getClaimedChunksForGod(godName) >= getMaximumChunksForGod(godName))
		{
			Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.CannotClaimMoreHolyland, ChatColor.RED, 0, "", 1);
			return;
		}
		
		setHolyLand(location, godName);
		setHolyLandName(location, generateHolyLandName(location.getWorld().getBiome(location.getBlockX(), location.getBlockZ())));

		int claimed = getClaimedChunksForGod(godName);
		int max = getMaximumChunksForGod(godName);
		
		LanguageManager.instance().setPlayerName(player.getName());
		GodManager.instance().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversClaimedHolyLand, 1);
		Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.HolylandSize, ChatColor.AQUA, claimed, Integer.toString(max), 20*5);
	}
	
	public void addPrayer(Player player, String godName, Location location)
	{
		if(getGodAtHolyLandLocation(location) != null)
		{
			return;		
		}

		if(getClaimedChunksForGod(godName) >= getMaximumChunksForGod(godName))
		{
			return;
		}
		
		int claimedBefore = getClaimedChunksForGod(godName);

		setHolyLand(location, godName);

		int claimedAfter = getClaimedChunksForGod(godName);
		int max = getMaximumChunksForGod(godName);

		if(claimedBefore!=claimedAfter)
		{
			LanguageManager.instance().setPlayerName(player.getName());
			GodManager.instance().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversClaimedHolyLand, 1);
			Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.HolylandSize, ChatColor.AQUA, claimedAfter, Integer.toString(max), 20*5);
		}
	}

	/*
	public void setPrayingHotspot(String believerName, String godName, Location location)
	{
		Location clampedLocation = new Location(location.getWorld(), location.getBlockX(), 0.0D, location.getBlockZ());

		long hash = hashLocation(clampedLocation);

		Date thisDate = new Date();
		if (this.landConfig.getString("Holyland." + hash + ".FirstPrayerTime") == null)
		{
			this.landConfig.set("Holyland." + hash + ".FirstPrayerTime", this.formatter.format(thisDate));
		}
		this.landConfig.set("Holyland." + hash + ".GodName", godName);
		this.landConfig.set("Holyland." + hash + ".LastPrayerTime", this.formatter.format(thisDate));
		this.landConfig.set("Holyland." + hash + ".World", clampedLocation.getWorld().getName());

		save();
	}
	*/
}