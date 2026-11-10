package com.dogonfire.gods.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.dogonfire.gods.Gods;

@SuppressWarnings("unused")
public class HolyLawManager implements Listener
{
	/*
	enum HolyLawActionType
	{
		KillMob,
		KillPlayer,
		EatFood,
		SacrificeItem,
		SacrificePlayer,
		PlaceBlock,
		BreakBlock,
	}
	
	enum HolyLawRuleType
	{
		Holy,
		Unholy				
	}
	
	class HolyLaw
	{
		int id;
		HolyLawType lawtype;  	// 0: None, 1:Holy, 2:Unholy
		int time;     			// 0: None, 1:Day, 2:Night, 3:Midnight
		int actiontype;     	// 1: MobKilled, 2:Killed, 3:Sacrifice
		EntityType entityType;  // 
		ItemType foodType;  // 
		Biome biome;
		
		public String Description()
		{
			return "Thou shall not kill spiders at midnight!";			
		}
	}
	
	
	private Gods							plugin;
	private FileConfiguration				holyLawsConfig		= null;
	private File							holyLawsConfigFile	= null;
	private Random							random				= new Random();

	private List<EntityType>				mobCandidates       = new ArrayList<EntityType>();
	private List<ItemType>					foodCandidates      = new ArrayList<ItemType>();
	private List<Biome>						biomeCandidates     = new ArrayList<Biome>();

	private HashMap<String,HolyLaw>			blockPlacementLaws	= new HashMap<String,HolyLaw>();
	private HashMap<String,HolyLaw>			blockBreakLaws		= new HashMap<String,HolyLaw>();
	private HashMap<String,List<HolyLaw>>	killLaws			= new HashMap<String,List<HolyLaw>>();
	private HashMap<String,List<HolyLaw>>	sacrificeLaws		= new HashMap<String,List<HolyLaw>>();
	
	private static HolyLawManager instance;

	public static HolyLawManager instance()
	{
		if (instance == null)
			instance = new HolyLawManager();
		
		return instance;
	}

	HolyLawManager()
	{		
		mobCandidates.add(EntityType.BAT);
		mobCandidates.add(EntityType.CHICKEN);
		mobCandidates.add(EntityType.COW);
		mobCandidates.add(EntityType.PIG);
		mobCandidates.add(EntityType.SHEEP);
		mobCandidates.add(EntityType.HORSE);
		mobCandidates.add(EntityType.RABBIT);
		mobCandidates.add(EntityType.SKELETON);
		mobCandidates.add(EntityType.SQUID);
		mobCandidates.add(EntityType.SPIDER);
		mobCandidates.add(EntityType.ZOMBIE);

		foodCandidates.add(ItemType.EGG);
		foodCandidates.add(ItemType.COOKED_PORKCHOP);
		foodCandidates.add(ItemType.COOKED_BEEF);
		foodCandidates.add(ItemType.BREAD);
		foodCandidates.add(ItemType.BAKED_POTATO);
		
		biomeCandidates.add(Biome.SWAMPLAND);
		biomeCandidates.add(Biome.PLAINS);
		biomeCandidates.add(Biome.DESERT);
		biomeCandidates.add(Biome.BIRCH_FOREST);
		biomeCandidates.add(Biome.FOREST);	
	}

	public void load()
	{
		if (this.holyLawsConfigFile == null)
		{
			this.holyLawsConfigFile = new File(this.plugin.getDataFolder(), "holylaws.yml");
		}
		this.holyLawsConfig = YamlConfiguration.loadConfiguration(this.holyLawsConfigFile);

		this.plugin.log("Loaded " + this.holyLawsConfig.getKeys(false).size() + " holy laws.");
	}

	public void save()
	{
		if ((this.holyLawsConfig == null) || (this.holyLawsConfigFile == null))
		{
			return;
		}
		try
		{
			this.holyLawsConfig.save(this.holyLawsConfigFile);
		}
		catch (Exception ex)
		{
			this.plugin.log("Could not save config to " + this.holyLawsConfigFile.getName() + ": " + ex.getMessage());
		}
	}
	
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void OnBlockPlace(BlockPlaceEvent event)
	{
		String godName = BelieverManager.instance().getGodForBeliever(event.getPlayer().getUniqueId());
		
		if(godName==null)
		{
			return;
		}
		
		if(!blockPlacementLaws.containsKey(godName))
		{
			return;
		}		
	}
	
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void OnBlockBreak(BlockBreakEvent event)
	{
		String godName = BelieverManager.instance().getGodForBeliever(event.getPlayer().getUniqueId());
		
		if(godName==null)
		{
			return;
		}

		/*
		if(!blockBreakLaws.containsKey(godName))
		{
			return;
		}
		
		HolyLaw holyLaw = blockBreakLaws.get(godName);
		
		if(holyLaw.lawtype == HolyLawType.Holy)
		{
			if(holyLaw.biome!=null)
			{
				Biome biome = event.getPlayer().getLocation().getWorld().getBiome(event.getPlayer().getLocation().getBlockX(), event.getPlayer().getLocation().getBlockZ());
				//handleBlockPlaced(event.getPlayer(), godName, event.getBlock().getType(), biome);

				return;
			}
			
			if(holyLaw.time!=0)
			{
			

				return;
			}

			return;
		}
		
		if(holyLaw.lawtype == HolyLawType.Unholy)
		{
			if(holyLaw.biome!=null)
			{			
				return;
			}
			
			if(holyLaw.time!=0)
			{
			

				return;
			}
			
			return;
		}		
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void OnPlayerDeath(PlayerDeathEvent event)
	{
	}
	
	@EventHandler
	public void OnEntityDeath(EntityDeathEvent event)
	{
		if (!(event.getEntity().getKiller() instanceof Player))
		{
			return;
		}
	}
	
	public HolyLaw getPendingHolyLaw(String godName)
	{
		HolyLaw holyLaw = new HolyLaw();
		
		// Time out and return null
		this.holyLawsConfig.get("PendingLaws." + godName + ".Id");
		this.holyLawsConfig.get("PendingLaws." + godName + ".ActionType");
		this.holyLawsConfig.get("PendingLaws." + godName + ".Biome");
		this.holyLawsConfig.get("PendingLaws." + godName + ".EntityType");
		this.holyLawsConfig.get("PendingLaws." + godName + ".FoodType");
		this.holyLawsConfig.get("PendingLaws." + godName + ".LawType");
		this.holyLawsConfig.get("PendingLaws." + godName + ".Time");
				
		return holyLaw;
	}

	void acceptPendingLaw(String godName)
	{
		Set<String> keys;
		
		// Good, i will take this into consideration		
		HolyLaw currentHolyLaw = getPendingHolyLaw(godName);
				
		this.holyLawsConfig.set("Laws." + godName + "." + currentHolyLaw.id + ".ActionType", currentHolyLaw.actiontype);
		this.holyLawsConfig.set("Laws." + godName + "." + currentHolyLaw.id + ".Biome", currentHolyLaw.biome);
		this.holyLawsConfig.set("Laws." + godName + "." + currentHolyLaw.id + ".EntityType", currentHolyLaw.entityType);
		this.holyLawsConfig.set("Laws." + godName + "." + currentHolyLaw.id + ".FoodType", currentHolyLaw.foodType);
		this.holyLawsConfig.set("Laws." + godName + "." + currentHolyLaw.id + ".LawType", currentHolyLaw.lawtype);
		this.holyLawsConfig.set("Laws." + godName + "." + currentHolyLaw.id + ".Time", currentHolyLaw.time);
						
		this.holyLawsConfig.set("CurrentLawQuestions." + godName, null);
		
		save();
	}

	public void rejectPendingLawQuestion(String godName)
	{
		// Good, i will take this into consideration
		HolyLaw currentHolyLaw = getPendingHolyLaw(godName);	
						
		this.holyLawsConfig.set("CurrentLawQuestion." + godName, null);
		
		save();
	}
	
	public String getPendingLawQuestion(String godName)
	{
		return this.holyLawsConfig.getString("CurrentLawQuestion." + godName + ".Question.Text");		
	}
	
	private Set<HolyLaw> getHolyLaws(String godName)
	{
		Set<HolyLaw> laws = new HashSet<HolyLaw>(); 
				
		for(String id : holyLawsConfig.getConfigurationSection(godName).getKeys(false))
		{
			ItemType itemType = ItemType.fromID(holyLawsConfig.getInt(godName + "." + id + ".ItemType"));
			EntityType entityType = EntityType.fromId(holyLawsConfig.getInt(godName + "." + id + ".EntityType"));
			Biome biomeType = Biome.valueOf(holyLawsConfig.getString(godName + "." + id + ".Biome"));
					
			laws.add(new HolyLaw(id));		
		}
		
		return laws;
	}
	
	// NOTE: Law action must be relatively rare occuring, such as breaking diamond blocks. Not wood blocks.
	public String generateNewLawQuestionForGod(String godName)
	{
		// How do you feel about bacon?
		// I dont care
		// It's evil!
		// Best food ever 2018
		
		// About killing players?
		// Holy!
		// Only at night!
		//   When is it holy?
		//		At midnight!
		//      In the morning!
		
		// About sacrificing players
		// Own/Others
		
		// About killing wolves?
		// They are nasty creatures! They must be ridden of
		
		// About building in xxx-lands?
		
		// About mining (diamond ore) in xxx-lands?
		// No! They are holy! Must be protected
		// No problem
		
		// First check existing law types
		for(HolyLaw holyLaw : getHolyLaws(godName))
		{
			HolyLaw holyLaw = blockBreakLaws.get(godName);
						
		}
		
		int r = random.nextInt(HolyLawActionType.values().length);
		HolyLawActionType lawActionType = HolyLawActionType.values()[r];
		String question = "I got nothing.";
		
		HolyLaw newLaw = new HolyLaw(lawActionType);
		
		switch(lawActionType)
		{
			case KillMob : question = "How do you feel about eating " + itemType + "?"; break; 	
			case KillPlayer : question = "How do you feel about eating " + itemType + "?"; break; 	
			case SacrificeItem : question = "How do you feel about eating " + itemType + "?"; break; 	
			case SacrificePlayer : question = "How do you feel about eating " + itemType + "?"; break; 	
		}
		
		setPendingLaw(godtName, newLaw);
		
		// Alternate between holy and unholy question to ensure that they are in equal numbers		
		return null;
	}
	
	public String generateRefinementLawQuestionForGod(String godName)
	{
		
		return null;
	}

	public List<String> generateNewLawAnswers(String godName)
	{
		List<String> answers = new ArrayList<String>();
		
		answers.add("Holy! There is nothing greater!");
		answers.add("Unholy blasfemy!");
		answers.add("Meh");		
		
		return answers;
	}
	
	public List<String> generateRefinementLawAnswers(String godName)
	{
		List<String> answers = new ArrayList<String>();
		
		answers.add("Perfect.");
		answers.add("No, this law is fulfilled. It must be removed.");  // 
		answers.add("Yes, but only when done in a " + biomeString);
		answers.add("Yes, but only when done at " + timeString);		
		
		return answers;
	}

	public void setPendingLawHoly(String godName)
	{
		
	}
	
	public void setPendingLawUnholy(String godName)
	{
		
	}
	
	public void setPendingLawTime(String godName)
	{		
		
	}

	public void setPendingLawBiome(String godName)
	{		
		
	}

	public void skipPendingLaw(String godName)
	{
		
	}

	public void deleteLaw(String godName)
	{
		
	}

	public List<String> getHolyMobsLawsForGod(String godName)
	{
		List<String> mobList = new ArrayList<String>();
		
		for(EntityType type : mobCandidates)
		{
			if(this.holyLawsConfig.getString("Laws." + godName + ".Holy.MobKill." + type)!=null)
			{
				mobList.add(type.toString());
			}
		}
		
		return mobList;		
	}

	public List<String> getUnholyMobsLawsForGod(String godName)
	{
		List<String> mobList = new ArrayList<String>();
		
		for(EntityType type : mobCandidates)
		{
			if(this.holyLawsConfig.getString("Laws." + godName + ".Unholy.MobKill." + type)!=null)
			{
				mobList.add(type.toString());
			}
		}
		
		return mobList;		
	}

	public void setHolyMob(String godName, EntityType mobType)
	{
		this.holyLawsConfig.set("Laws." + godName + ".Holy.MobKill." + mobType + ".StartTime", 0);		
		this.holyLawsConfig.set("Laws." + godName + ".Holy.MobKill." + mobType + ".EndTime", 24000);		
	}

	public boolean isHolyMobKill(String godName, EntityType mobType, int time)
	{
		if(this.holyLawsConfig.getString(godName + ".Holy.MobKill." + mobType) != null)		
		{
			return false;			
		}
		
		int startTime = this.holyLawsConfig.getInt("Laws." + godName + ".Holy.MobKill." + mobType + ".StartTime");		
		int endTime = this.holyLawsConfig.getInt("Laws." + godName + ".Holy.MobKill." + mobType + ".EndTime");		
	
		if(startTime == 0 && endTime == 0)
		{						
			return true;
		}
		
		String biomeType = this.holyLawsConfig.getString("Laws." + godName + ".Holy.MobKill." + mobType + ".Biome");		

		if(biomeType == null)
		{						
			return true;
		}

		return time > startTime && time < endTime; 		
	}
	
	public Biome getHolyMobBiome(String godName, EntityType mobType)
	{	
		return Biome.valueOf(this.holyLawsConfig.getString("Laws." + godName + ".Holy.MobKill." + mobType + ".Biome"));		
	}

	public String getHolyMobTime(String godName, EntityType mobType)
	{	
		return this.holyLawsConfig.getString("Laws." + godName + ".Holy.MobKill." + mobType + ".Time");		
	}

	public boolean isUnholyMobKill(String godName, EntityType mobType)
	{
		return this.holyLawsConfig.getString(godName + ".Unholy.MobKill." + mobType.toString()) != null;		
	}

	public void setHolyPlayerKill(String godName)
	{
		this.holyLawsConfig.set("Laws." + godName + ".Holy.PlayerKill.StartTime", 0);		
		this.holyLawsConfig.set("Laws." + godName + ".Holy.PlayerKill.EndTime", 24000);		
		this.holyLawsConfig.set("Laws." + godName + ".Holy.PlayerKill.Biome", Biome.BIRCH_FOREST);		
	}

	public boolean isHolyPlayerKill(String godName, UUID playerId, int time)
	{
		if(this.holyLawsConfig.getString(godName + ".Holy.PlayerKill." + playerId) != null)		
		{
			return false;			
		}
		
		int startTime = this.holyLawsConfig.getInt("Laws." + godName + ".Holy.PlayerKill." + playerId + ".StartTime");		
		int endTime = this.holyLawsConfig.getInt("Laws." + godName + ".Holy.PlayerKill." + playerId + ".EndTime");		
	
		if(startTime == 0 && endTime == 0)
		{						
			return true;
		}
		
		String biomeType = this.holyLawsConfig.getString(godName + ".Holy.PlayerKill." + playerId + ".Biome");		

		if(biomeType == null)
		{						
			return true;
		}

		return time > startTime && time < endTime; 		
	}

	public boolean isUnholyPlayerKill(String godName, UUID playerId, int time)
	{
		if(this.holyLawsConfig.getString(godName + ".Unholy.PlayerKill." + playerId) != null)		
		{
			return false;			
		}
		
		int startTime = this.holyLawsConfig.getInt("Laws." + godName + ".Unholy.PlayerKill." + playerId + ".StartTime");		
		int endTime = this.holyLawsConfig.getInt("Laws." + godName + ".Unholy.PlayerKill." + playerId + ".EndTime");		
	
		if(startTime == 0 && endTime == 0)
		{						
			return true;
		}
		
		String biomeType = this.holyLawsConfig.getString("Laws." + godName + ".Unholy.PlayerKill." + playerId + ".Biome");		

		if(biomeType == null)
		{						
			return true;
		}

		return time > startTime && time < endTime; 				
	}

	public List<String> getHolyFoodLawsForGod(String godName)
	{
		List<String> foodList = new ArrayList<String>();
		
		for(ItemType type : foodCandidates)
		{
			if(this.holyLawsConfig.getString("Laws." + godName + ".Holy.Food." + type.toString())!=null)
			{
				foodList.add(type.toString());
			}
		}
		
		return foodList;		
	}
	
	public boolean isHolyFood(String godName, EntityType foodType, int time)
	{
		if(this.holyLawsConfig.getString(godName + ".Holy.Food." + foodType) != null)		
		{
			return false;			
		}
		
		int startTime = this.holyLawsConfig.getInt("Laws." + godName + ".Holy.Food." + foodType + ".StartTime");		
		int endTime = this.holyLawsConfig.getInt("Laws." + godName + ".Holy.Food." + foodType + ".EndTime");		
	
		if(startTime == 0 && endTime == 0)
		{						
			return true;
		}
		
		String biomeType = this.holyLawsConfig.getString("Laws." + godName + ".Holy.Food." + foodType + ".Biome");		

		if(biomeType == null)
		{						
			return true;
		}

		return time > startTime && time < endTime; 		
	}

	public boolean isUnholyFood(String godName, EntityType foodType, int time)
	{
		if(this.holyLawsConfig.getString(godName + ".Unholy.Food." + foodType) != null)		
		{
			return false;			
		}
		
		int startTime = this.holyLawsConfig.getInt("Laws." + godName + ".Unholy.Food." + foodType + ".StartTime");		
		int endTime = this.holyLawsConfig.getInt("Laws." + godName + ".Unholy.Food." + foodType + ".EndTime");		
	
		if(startTime == 0 && endTime == 0)
		{						
			return true;
		}
		
		String biomeType = this.holyLawsConfig.getString("Laws." + godName + ".Unholy.Food." + foodType + ".Biome");		

		if(biomeType == null)
		{						
			return true;
		}

		return time > startTime && time < endTime; 		
	}
	*/
}
