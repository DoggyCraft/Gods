package com.dogonfire.gods.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.LanguageManager.LANGUAGESTRING;

public class ProphecyManager 
{
	private Gods plugin = null;
	private FileConfiguration prophecyConfig = null;
	private File prophecyConfigFile = null;

	static enum Prophecy 
	{
		None, Header, BelieverWillLeaveReligion, ReligionKills100Mobs, NewPriestWillBeSelected, HolyArtifactWillBeFound, RandomBelieverWillBeKilledByUnholyMob, RandomBelieverWillBeKilledByHeathen, UnholyMobWillBeSlainByBeliever, HeathenWillBeSlain, ReligionHarvested, DragonBossWillBeSlain;
	}

	static enum ProphecyEffect 
	{
		None, Rain, Storm, SkyWillDarken, HolyFoodRain, LongRain, LongNight, RainItems, SilverfishSwarm, DragonBoss, TitanBoss;
	}

	private static ProphecyManager instance;

	public static ProphecyManager instance()
	{
		if (instance == null)
			instance = new ProphecyManager();
		
		return instance;
	}
	
	ProphecyManager() 
	{
	}

	public void load() 
	{
		if (prophecyConfigFile == null) 
		{
			prophecyConfigFile = new File(this.plugin.getDataFolder(), "prophecies.yml");
		}

		prophecyConfig = YamlConfiguration
				.loadConfiguration(prophecyConfigFile);

		plugin.log("Loaded " + prophecyConfig.getKeys(false).size() + " prophecies.");
	}

	public void save() 
	{
		if ((prophecyConfig == null) || (prophecyConfigFile == null)) 
		{
			return;
		}

		try 
		{
			prophecyConfig.save(prophecyConfigFile);
		} 
		catch (Exception ex) 
		{
			plugin.log("Could not save config to " + prophecyConfigFile + ": " + ex.getMessage());
		}
	}

	public boolean hasProphecies(String godName) 
	{
		return getPropheciesForGod(godName) != null && getPropheciesForGod(godName).size() > 0;
	}

	public List<String> getProphecyPlayersForGod(String godName) 
	{
		return prophecyConfig.getStringList(godName + ".Players");
	}

	public List<String> getPropheciesForGod(String godName) 
	{
		return prophecyConfig.getStringList(godName + ".Prophecies");
	}

	public List<String> getProphecyEffectsForGod(String godName) 
	{
		return prophecyConfig.getStringList(godName + ".ProphecyEffects");
	}

	public void clearPropheciesForGod(String godName) 
	{
		this.prophecyConfig.set(godName + ".Prophecies", null);
		this.prophecyConfig.set(godName + ".ProphecyEffects", null);
		this.prophecyConfig.set(godName + ".CurrentProphecyIndex", null);

		save();
	}

	public Prophecy getCurrentProphecyForGod(String godName) 
	{
		int currentProphecyIndex = prophecyConfig.getInt(godName + ".CurrentProphecyIndex");

		List<String> prophecies = getPropheciesForGod(godName);

		String prophecyString = (String) prophecies.get(currentProphecyIndex);

		return Prophecy.valueOf(prophecyString);
	}

	public ProphecyEffect getCurrentProphecyEffectForGod(String godName) 
	{
		int currentProphecyIndex = prophecyConfig.getInt(godName + ".CurrentProphecyIndex");

		List<String> prophecyEffects = getProphecyEffectsForGod(godName);

		String prophecyEffectString = (String) prophecyEffects.get(currentProphecyIndex);

		return ProphecyEffect.valueOf(prophecyEffectString);
	}

	public void handleMobKill(String playerName, String godName, String mobType) 
	{
		if (!hasProphecies(godName)) 
		{
			plugin.logDebug("Generating prophecies for " + godName);
			generateProphecies(godName);
		}

		Prophecy currentProphecy = getCurrentProphecyForGod(godName);

		if (currentProphecy == Prophecy.UnholyMobWillBeSlainByBeliever) 
		{
			if (mobType.equals(GodManager.instance().getHolyMobTypeForGod(godName).name())) 
			{
				fullfillCurrentProphecy(godName, playerName);
			}
		}
	}

	public void handleBelieverLeave(String playerName, String godName) 
	{
		if (!hasProphecies(godName)) 
		{
			plugin.logDebug("Generating prophecies for " + godName);
			generateProphecies(godName);
		}

		Prophecy currentProphecy = getCurrentProphecyForGod(godName);

		if (currentProphecy == Prophecy.BelieverWillLeaveReligion) 
		{
			fullfillCurrentProphecy(godName, playerName);
		}
	}

	private String describeFulfilledProphecy(String godName, Prophecy prophecy, ProphecyEffect effect, String playerName) 
	{
		String prophecyText = "MISSINGPROPHECY";

		//plugin.getLanguageManager().setType(plugin.getLanguageManager().getMobTypeName(plugin.getGodManager().getUnholyMobTypeForGod(godName)));
		LanguageManager.instance().setPlayerName(playerName);
		
		switch (prophecy) 
		{
			case Header: prophecyText = ChatColor.GOLD + "The Prophecies of " + godName + ChatColor.BLACK + "                                                      These are things I foreseen will come to happen in " + GodsConfiguration.instance().getServerName() + "."; break;
			case BelieverWillLeaveReligion: prophecyText = ChatColor.GOLD + "The Prophecies of " + godName + ChatColor.BLACK + "                                             These are things I foreseen will come to happen in " + GodsConfiguration.instance().getServerName() + ".";
			case DragonBossWillBeSlain : prophecyText = LanguageManager.instance().getLanguageStringForBook(godName, LANGUAGESTRING.DragonBossWillBeSlainPastProphecyBibleText); break;
			case UnholyMobWillBeSlainByBeliever : prophecyText = LanguageManager.instance().getLanguageStringForBook(godName, LANGUAGESTRING.UnholyMobWillBeSlainPastProphecyBibleText); break;
			case HeathenWillBeSlain: prophecyText = ChatColor.GOLD + "The Prophecies of " + godName + ChatColor.BLACK + "                                             These are things I foreseen will come to happen in " + GodsConfiguration.instance().getServerName() + ".";
			case ReligionHarvested: prophecyText = "The evil " + GodManager.instance().getHolyFoodTypeForGod(godName) + "was slain by the great " + playerName; break;
			case NewPriestWillBeSelected: prophecyText = playerName + "became our new leader of to guide our people"; break;
			case None: prophecyText = "And a champion will find a holy artifact"; break;
			case RandomBelieverWillBeKilledByHeathen: prophecyText = "And our believer  will be killed by the evil " + GodManager.instance().getHolyFoodTypeForGod(godName) + "!";
			case RandomBelieverWillBeKilledByUnholyMob: prophecyText = "And a will be killed by a sneaky heathen!";
			case ReligionKills100Mobs: prophecyText = "The foolish " + playerName + "was slain";
			case HolyArtifactWillBeFound: break;
			default: break;
		}
		
		switch (effect) 
		{
			case None : prophecyText += ""; break;
			case DragonBoss: prophecyText += LanguageManager.instance().getLanguageStringForBook(godName, LANGUAGESTRING.DragonBossProphecyEffectPastBibleText); break;
			case LongRain: prophecyText = " and the sky will thunder!"; break;
			case RainItems: prophecyText = " and the skies will weap rain with for 3 days!"; break;
			case Rain: prophecyText = " and the skies will be filled with " + GodManager.instance().getHolyFoodTypeForGod(godName) + "!";
			case Storm: prophecyText +=  LanguageManager.instance().getLanguageStringForBook(godName, LANGUAGESTRING.StormProphecyEffectPastBibleText); break;
			case SilverfishSwarm: prophecyText = " and silverfish will appear, roaming the lands!"; break;
			case SkyWillDarken: prophecyText = " and the skies will darken and sun dissapear!"; break;
			default: break;
		}
		
		return ChatColor.GOLD + prophecyText;
	}

	private String describeUnfulfilledProphecy(String godName, Prophecy prophecy, ProphecyEffect effect) 
	{
		String prophecyText = "MISSINGPROPHECY";

		//plugin.getLanguageManager().setType(plugin.getLanguageManager().getMobTypeName(plugin.getGodManager().getUnholyMobTypeForGod(godName)));
		LanguageManager.instance().setPlayerName(godName);
		
		switch (prophecy) 
		{
			case Header: prophecyText = LanguageManager.instance().getLanguageStringForBook(godName, LANGUAGESTRING.ProphecyHeaderBibleText); break;
			case BelieverWillLeaveReligion : prophecyText = LanguageManager.instance().getLanguageStringForBook(godName, LANGUAGESTRING.BelieverWillLeaveReligionProphecyBibleText); break;
			case DragonBossWillBeSlain : prophecyText = LanguageManager.instance().getLanguageStringForBook(godName, LANGUAGESTRING.DragonBossWillBeSlainFutureProphecyBibleText); break;
			case UnholyMobWillBeSlainByBeliever : prophecyText = LanguageManager.instance().getLanguageStringForBook(godName, LANGUAGESTRING.UnholyMobWillBeSlainFutureProphecyBibleText); break;
			case HeathenWillBeSlain: prophecyText = ChatColor.GOLD + "The Prophecies of " + godName + ChatColor.BLACK + "                                             These are things I foreseen will come to happen in " + GodsConfiguration.instance().getServerName() + "."; break;
			case ReligionHarvested: prophecyText = "A champion will slay the nasty "+ GodManager.instance().getHolyFoodTypeForGod(godName); break;
			case NewPriestWillBeSelected: prophecyText = "A new leader of " + godName + " will guide our people"; break;
			case None: prophecyText = "A champion will find a holy artifact"; break;
			case RandomBelieverWillBeKilledByHeathen: prophecyText = "And our believer  will be killed by the evil $Type!"; break;
			case RandomBelieverWillBeKilledByUnholyMob: prophecyText = "A brother will be killed by a sneaky heathen!"; break;
			case ReligionKills100Mobs: prophecyText = "A heathen will be slain"; break;
			case HolyArtifactWillBeFound: prophecyText = "A champion will find a holy artifact"; break;
			default: break;
		}
		
		switch (effect) 
		{
			case DragonBoss: prophecyText += LanguageManager.instance().getLanguageStringForBook(godName, LANGUAGESTRING.DragonBossProphecyEffectFutureBibleText); break;
			case LongRain: prophecyText += " and the sky will thunder!"; break;
			case None: prophecyText += ""; break;
			case RainItems: prophecyText += " and the skies will weap rain with for 3 days!"; break;
			case Rain: prophecyText += " and the skies was be filled with $Type!"; break;
			case Storm: prophecyText += LanguageManager.instance().getLanguageStringForBook(godName, LANGUAGESTRING.StormProphecyEffectFutureBibleText); break;
			case HolyFoodRain: prophecyText +=  " and it rained with $Type"; break;
			case LongNight: prophecyText +=  " and day will turn into night"; break;
			case SilverfishSwarm: prophecyText +=  ""; break;
			case SkyWillDarken: prophecyText +=  ""; break;
			default: break;
		}
		
		return prophecyText;
	}

	public void generateProphecies(String godName) 
	{
		List<String> prophecies = new ArrayList<String>();
		List<String> prophecyEffects = new ArrayList<String>();
		List<String> players = new ArrayList<String>();

		// Make sure bible has a text
		HolyBookManager.instance().getBible(godName);
		
		players.add("NoOne");
		prophecies.add(Prophecy.Header.name());
		prophecyEffects.add(ProphecyEffect.None.name());

		//for (int p = 0; p < numberOfProphecies; p++) 
		//{
		prophecies.add(Prophecy.UnholyMobWillBeSlainByBeliever.name());
		prophecyEffects.add(ProphecyEffect.Storm.name());
		//}

		prophecies.add(Prophecy.UnholyMobWillBeSlainByBeliever.name());
		prophecyEffects.add(ProphecyEffect.DragonBoss.name());

		//prophecies.add(Prophecy.DragonBossWillBeSlain.name());
		//prophecyEffects.add(ProphecyEffect.Storm.name());

		prophecyConfig.set(godName + ".Prophecies", prophecies);
		prophecyConfig.set(godName + ".ProphecyEffects", prophecyEffects);
		prophecyConfig.set(godName + ".Players", players);
		prophecyConfig.set(godName + ".CurrentProphecyIndex", Integer.valueOf(1));

		save();

		updatePropheciesInBible(godName);
	}

	public void fullfillCurrentProphecy(String godName, String playerName) 
	{
		List<String> prophecies = getPropheciesForGod(godName);

		// Tell everyone
		if(GodsConfiguration.instance().isBroadcastProphecyFullfillment())
		{
			LanguageManager.instance().setPlayerName(playerName);
			//plugin.getLanguageManager().setType(godName);
			plugin.getServer().broadcastMessage(LanguageManager.instance().getLanguageString(godName, LanguageManager.LANGUAGESTRING.GodToBelieversProphecyFulfilled));
		}
		
		// Launch the effect
		ProphecyEffect prophecyEffect = getCurrentProphecyEffectForGod(godName);
		launchProphecyEffect(godName, prophecyEffect);
		
		// Add the champion 
		List<String> champions = getProphecyPlayersForGod(godName);		
		champions.add(playerName);		
		prophecyConfig.set(godName + ".Players", champions);		
		
		// Increment
		int currentProphecyIndex = prophecyConfig.getInt(godName + ".CurrentProphecyIndex");

		currentProphecyIndex++;

		prophecyConfig.set(godName + ".CurrentProphecyIndex", Integer.valueOf(currentProphecyIndex));

		// Check for being the last one
		if (currentProphecyIndex >= prophecies.size()) 
		{
			LanguageManager.instance().setPlayerName(HolyBookManager.instance().getBibleTitle(godName));
			GodManager.instance().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversAllPropheciesFulfilled, 10);
			clearPropheciesForGod(godName);
			return;
		}

		save();

		updatePropheciesInBible(godName);
	}

	public void updatePropheciesInBible(String godName) 
	{
		int currentProphecyIndex = prophecyConfig.getInt(godName + ".CurrentProphecyIndex");
		
		List<String> prophecies = getPropheciesForGod(godName);
		List<String> players = getProphecyPlayersForGod(godName);
		List<String> prophecyEffects = getProphecyEffectsForGod(godName);
		List<String> prophecyPages = new ArrayList<String>();

		for (int p = 0; p < currentProphecyIndex; p++) 
		{
			Prophecy fulfilledProphecy = Prophecy.valueOf((String) prophecies.get(p));
			ProphecyEffect fulfilledProphecyEffect = ProphecyEffect.valueOf((String) prophecyEffects.get(p));
			String fulfilledPlayerName = (String) players.get(p);

			prophecyPages.add(describeFulfilledProphecy(godName, fulfilledProphecy, fulfilledProphecyEffect, fulfilledPlayerName));
		}

		if (currentProphecyIndex > -1) 
		{
			for (int p = currentProphecyIndex; p < prophecies.size(); p++) 
			{
				Prophecy unfulfilledProphecy = Prophecy.valueOf((String) prophecies.get(p));
				ProphecyEffect unfulfilledProphecyEffect = ProphecyEffect.valueOf((String) prophecyEffects.get(p));

				prophecyPages.add(describeUnfulfilledProphecy(godName, unfulfilledProphecy, unfulfilledProphecyEffect));
			}
		}

		HolyBookManager.instance().setProphecyPages(godName, prophecyPages);
	}
	
	public void launchProphecyEffect(String godName, ProphecyEffect prophecyEffect)
	{
		switch(prophecyEffect)
		{
			case Storm : 
				{
					plugin.logDebug("Setting storm...");
					for(World world : plugin.getServer().getWorlds())
					{ 
						world.setStorm(true);
						world.setWeatherDuration(20*60*20);
					}  					
				} break;		
			
			case DragonBoss : 
			{
				plugin.logDebug("Spawning dragon boss...");
				Location location = new Location(plugin.getServer().getWorlds().get(0), 0,0,0);
				//plugin.getBossManager().createNewBoss(BossType.DRAGON, godName, location);
			} break;		

			case TitanBoss : 
			{
				Location location = new Location(plugin.getServer().getWorlds().get(0), 0,0,0);
				//plugin.getBossManager().createNewBoss(BossType.TITAN, godName, location);
			} break;	
			default: break;	
		}
		
	}
}