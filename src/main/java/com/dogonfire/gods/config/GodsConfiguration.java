package com.dogonfire.gods.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.managers.AltarManager;
import com.dogonfire.gods.managers.ChatManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.GodManager.GodType;
import com.dogonfire.gods.managers.HolyArtifactManager;
import com.dogonfire.gods.managers.HolyBookManager;
import com.dogonfire.gods.managers.HolyLandManager;
import com.dogonfire.gods.managers.HolyPowerManager;
import com.dogonfire.gods.managers.MarriageManager;
import com.dogonfire.gods.managers.QuestManager;

public class GodsConfiguration
{
	private static GodsConfiguration instance;

	public static GodsConfiguration instance()
	{
		if (instance == null)
		{
			instance = new GodsConfiguration();
		}
		
		return instance;
	}

	private boolean			debug								= false;
	private boolean			downloadLanguageFile				= true;
	private boolean			useWhitelist						= false;
	private boolean			useBlacklist						= false;
	private boolean			questsEnabled						= false;
	private boolean			itemBlessingEnabled					= true;
	private boolean			blessingEnabled						= true;
	private boolean			enableDetoration					= false;
	private boolean			commandmentsEnabled					= true;
	private boolean			sacrificesEnabled					= true;
	private boolean			holyLandEnabled						= false;
	private boolean			biblesEnabled						= false;
	private boolean			giveBibleWhenJoinReligion			= false;
	private boolean			giveInstructionsWhenJoinReligion	= false;
	private boolean			holyArtifactsEnabled				= false;
	private boolean			propheciesEnabled					= false;
	private boolean			chatFormattingEnabled				= false;
	private boolean			useGodTitles						= true;
	private boolean			marriageEnabled						= false;
	private boolean			marriageFireworksEnabled			= false;
	private boolean			cursingEnabled						= true;
	private boolean			lightningCurseEnabled				= false;
	private boolean			mobCurseEnabled						= true;
	private boolean			fastDiggingBlessingEnabled			= false;
	private boolean			healBlessingEnabled					= false;
	private boolean			regenerationBlessingEnabled			= false;
	private boolean			speedBlessingEnabled				= false;
	private boolean			increaseDamageBlessingEnabled		= false;
	private boolean			globalQuestsEnabled					= false;
	private boolean			slayQuestsEnabled					= true;
	private boolean			sacrificeQuestsEnabled				= true;
	private boolean			pilgrimageQuestsEnabled				= false;
	private boolean			holyFeastQuestsEnabled				= false;
	private boolean			giveItemsQuestsEnabled				= false;
	private boolean			burnBiblesQuestsEnabled				= false;
	private boolean			crusadeQuestsEnabled				= false;
	private boolean			convertQuestsEnabled				= false;
	private boolean			buildTowerQuestsEnabled				= false;
	private boolean			holywarQuestsEnabled				= false;
	private boolean			slayDragonQuestsEnabled				= false;
	private boolean			broadcastNewGods					= true;
	private boolean			broadcastProphecyFullfillment		= true;
	private boolean			defaultPrivateReligions				= false;
	private boolean			leaveReligionOnDeath				= false;
	private boolean			onlyPriestCanSetHome				= false;
	private boolean			commandmentsBroadcastFoodEaten		= true;
	private boolean			commandmentsBroadcastMobSlain		= true;
	private boolean			holyLandDefaultPvP					= false;
	private boolean			holyLandDefaultMobDamage			= true;
	private boolean			holyLandLightning					= true;
	private boolean			powerLossOnDeath					= false;
	private boolean			allowMultipleGodsPrDivinePower		= false;
	private boolean			prayersEnabled						= true;
	private boolean			allowInteractionInNeutralLands		= true;
	private boolean			werewolfEnabled						= false;
	private int				minCursingTime						= 10;
	private int				maxCursingTime						= 10;
	private int				minBlessingTime						= 900;
	private int				maxBlessingTime						= 180;
	private int				minMinutesBetweenQuests				= 180;
	private int				globalQuestsPercentChance			= 10;
	private int				maxPriestsPrGod						= 3;
	private int				numberOfBelieversPrPriest			= 3;
	private int				maxInvitationTimeSeconds			= 60;
	private int				minItemBlessingTime					= 10;
	private int				minHolyArtifactBlessingTime			= 1440;
	private int				maxPriestPrayerTime					= 8;
	private int				maxBelieverPrayerTime				= 154;
	private int				minBelieverPrayerTime				= 30;
	private int				minGodPowerForItemBlessings			= 3;
	private int				godPowerForLevel3Items				= 100;
	private int				godPowerForLevel2Items				= 50;
	private int				godPowerForLevel1Items				= 10;
	private int				minBelieversForPriest				= 3;
	private int				minSecondsBetweenChangingGod		= 5 * 60;
	private int				requiredBelieversForQuests			= 1;
	private int				numberOfDaysForAbandonedHolyLands	= 14;
	private int				maxHolyArtifacts					= 50;
	private int				prayerPowerForItem					= 20;
	private int				prayerPowerForQuest					= 50;
	private int				prayerPowerForBlessing				= 10;
	private int				prayerPowerForHolyArtifact			= 100;
	private int				prayerPowerForHealth				= 10;
	private double			moodFalloff							= 0.03D;
	private double			godVerbosity						= 1.0D;
	private double			holyLandRadiusPrPower				= 1.25D;
	private String			languageIdentifier					= "english";
	private String			priestAssignCommand					= "";
	private String			priestRemoveCommand					= "";
	private String			serverName							= "Your Server";

	private Set<Material>	holylandBreakableBlockTypes			= new HashSet<Material>();

	private List<String>	worlds								= new ArrayList<String>();

	private GodsConfiguration()
	{
	}

	public final int getGlobalQuestsPercentChance()
	{
		return globalQuestsPercentChance;
	}

	public final int getGodPowerForLevel1Items()
	{
		return godPowerForLevel1Items;
	}

	public final int getGodPowerForLevel2Items()
	{
		return godPowerForLevel2Items;
	}

	public final int getGodPowerForLevel3Items()
	{
		return godPowerForLevel3Items;
	}

	public final double getGodVerbosity()
	{
		return godVerbosity;
	}

	public final Set<Material> getHolylandBreakableBlockTypes()
	{
		return holylandBreakableBlockTypes;
	}

	public final double getHolyLandRadiusPrPower()
	{
		return holyLandRadiusPrPower;
	}

	public final String getLanguageIdentifier()
	{
		return languageIdentifier;
	}

	public final int getMaxBelieverPrayerTime()
	{
		return maxBelieverPrayerTime;
	}

	public final int getMaxBlessingTime()
	{
		return maxBlessingTime;
	}

	public final int getMaxCursingTime()
	{
		return maxCursingTime;
	}

	public final int getMaxHolyArtifacts()
	{
		return maxHolyArtifacts;
	}

	public final int getMaxInvitationTimeSeconds()
	{
		return maxInvitationTimeSeconds;
	}

	public final int getMaxPriestPrayerTime()
	{
		return maxPriestPrayerTime;
	}

	public final int getMaxPriestsPrGod()
	{
		return maxPriestsPrGod;
	}

	public final int getMinBelieverPrayerTime()
	{
		return minBelieverPrayerTime;
	}

	public final int getMinBelieversForPriest()
	{
		return minBelieversForPriest;
	}

	public final int getMinBlessingTime()
	{
		return minBlessingTime;
	}

	public final int getMinCursingTime()
	{
		return minCursingTime;
	}

	public final int getMinGodPowerForItemBlessings()
	{
		return minGodPowerForItemBlessings;
	}

	public final int getMinHolyArtifactBlessingTime()
	{
		return minHolyArtifactBlessingTime;
	}

	public final int getMinItemBlessingTime()
	{
		return minItemBlessingTime;
	}

	public final int getMinMinutesBetweenQuests()
	{
		return minMinutesBetweenQuests;
	}

	public final int getMinSecondsBetweenChangingGod()
	{
		return minSecondsBetweenChangingGod;
	}

	public final double getMoodFalloff()
	{
		return moodFalloff;
	}

	public final int getNumberOfBelieversPrPriest()
	{
		return numberOfBelieversPrPriest;
	}

	public final int getNumberOfDaysForAbandonedHolyLands()
	{
		return numberOfDaysForAbandonedHolyLands;
	}

	public final int getPrayerPowerForBlessing()
	{
		return prayerPowerForBlessing;
	}

	public final int getPrayerPowerForHealth()
	{
		return prayerPowerForHealth;
	}

	public final int getPrayerPowerForHolyArtifact()
	{
		return prayerPowerForHolyArtifact;
	}

	public final int getPrayerPowerForItem()
	{
		return prayerPowerForItem;
	}

	public final int getPrayerPowerForQuest()
	{
		return prayerPowerForQuest;
	}

	public final String getPriestAssignCommand()
	{
		return priestAssignCommand;
	}

	public final String getPriestRemoveCommand()
	{
		return priestRemoveCommand;
	}

	public final int getRequiredBelieversForQuests()
	{
		return requiredBelieversForQuests;
	}

	public final String getServerName()
	{
		return serverName;
	}

	public final List<String> getWorlds()
	{
		return worlds;
	}

	public final boolean isAllowInteractionInNeutralLands()
	{
		return allowInteractionInNeutralLands;
	}

	public final boolean isAllowMultipleGodsPrDivinePower()
	{
		return allowMultipleGodsPrDivinePower;
	}

	public final boolean isBiblesEnabled()
	{
		return biblesEnabled;
	}
	
	public final boolean isGiveBibleWhenJoinReligion()
	{
		return giveBibleWhenJoinReligion;
	}
	
	public final boolean isGiveInstructionsWhenJoinReligion()
	{
		return giveInstructionsWhenJoinReligion;
	}

	public final boolean isBlessingEnabled()
	{
		return blessingEnabled;
	}

	public final boolean isBroadcastNewGods()
	{
		return broadcastNewGods;
	}

	public final boolean isBroadcastProphecyFullfillment()
	{
		return broadcastProphecyFullfillment;
	}

	public final boolean isBuildTowerQuestsEnabled()
	{
		return buildTowerQuestsEnabled;
	}

	public final boolean isBurnBiblesQuestsEnabled()
	{
		return burnBiblesQuestsEnabled;
	}

	public final boolean isChatFormattingEnabled()
	{
		return chatFormattingEnabled;
	}

	public final boolean isCommandmentsBroadcastFoodEaten()
	{
		return commandmentsBroadcastFoodEaten;
	}

	public final boolean isCommandmentsBroadcastMobSlain()
	{
		return commandmentsBroadcastMobSlain;
	}

	public final boolean isCommandmentsEnabled()
	{
		return commandmentsEnabled;
	}

	public final boolean isConvertQuestsEnabled()
	{
		return convertQuestsEnabled;
	}

	public final boolean isCrusadeQuestsEnabled()
	{
		return crusadeQuestsEnabled;
	}

	public final boolean isCursingEnabled()
	{
		return cursingEnabled;
	}

	public final boolean isDebug()
	{
		return debug;
	}

	public final boolean isDefaultPrivateReligions()
	{
		return defaultPrivateReligions;
	}

	public final boolean isDownloadLanguageFile()
	{
		return downloadLanguageFile;
	}

	public final boolean isEnableDetoration()
	{
		return enableDetoration;
	}

	public final boolean isFastDiggingBlessingEnabled()
	{
		return fastDiggingBlessingEnabled;
	}

	public final boolean isGiveItemsQuestsEnabled()
	{
		return giveItemsQuestsEnabled;
	}

	public final boolean isGlobalQuestsEnabled()
	{
		return globalQuestsEnabled;
	}

	public final boolean isHealBlessingEnabled()
	{
		return healBlessingEnabled;
	}

	public final boolean isHolyArtifactsEnabled()
	{
		return holyArtifactsEnabled;
	}

	public final boolean isHolyFeastQuestsEnabled()
	{
		return holyFeastQuestsEnabled;
	}

	public final boolean isHolyLandDefaultMobDamage()
	{
		return holyLandDefaultMobDamage;
	}

	public final boolean isHolyLandDefaultPvP()
	{
		return holyLandDefaultPvP;
	}

	public final boolean isHolyLandEnabled()
	{
		return holyLandEnabled;
	}

	public final boolean isHolyLandLightning()
	{
		return holyLandLightning;
	}

	public final boolean isHolywarQuestsEnabled()
	{
		return holywarQuestsEnabled;
	}

	public final boolean isIncreaseDamageBlessingEnabled()
	{
		return increaseDamageBlessingEnabled;
	}

	public final boolean isItemBlessingEnabled()
	{
		return itemBlessingEnabled;
	}

	public final boolean isLeaveReligionOnDeath()
	{
		return leaveReligionOnDeath;
	}

	public final boolean isLightningCurseEnabled()
	{
		return lightningCurseEnabled;
	}

	public final boolean isMarriageEnabled()
	{
		return marriageEnabled;
	}

	public final boolean isMarriageFireworksEnabled()
	{
		return marriageFireworksEnabled;
	}

	public final boolean isMobCurseEnabled()
	{
		return mobCurseEnabled;
	}

	public final boolean isOnlyPriestCanSetHome()
	{
		return onlyPriestCanSetHome;
	}

	public final boolean isPilgrimageQuestsEnabled()
	{
		return pilgrimageQuestsEnabled;
	}

	public final boolean isPowerLossOnDeath()
	{
		return powerLossOnDeath;
	}

	public final boolean isPrayersEnabled()
	{
		return prayersEnabled;
	}

	public final boolean isPropheciesEnabled()
	{
		return propheciesEnabled;
	}

	public final boolean isQuestsEnabled()
	{
		return questsEnabled;
	}

	public final boolean isRegenerationBlessingEnabled()
	{
		return regenerationBlessingEnabled;
	}

	public final boolean isSacrificeQuestsEnabled()
	{
		return sacrificeQuestsEnabled;
	}

	public final boolean isSacrificesEnabled()
	{
		return sacrificesEnabled;
	}

	public final boolean isSlayDragonQuestsEnabled()
	{
		return slayDragonQuestsEnabled;
	}

	public final boolean isSlayQuestsEnabled()
	{
		return slayQuestsEnabled;
	}

	public final boolean isSpeedBlessingEnabled()
	{
		return speedBlessingEnabled;
	}

	public final boolean isUseBlacklist()
	{
		return useBlacklist;
	}

	public final boolean isUseGodTitles()
	{
		return useGodTitles;
	}

	public final boolean isUseWhitelist()
	{
		return useWhitelist;
	}

	public final boolean isWerewolfEnabled()
	{
		return werewolfEnabled;
	}

	public void loadSettings()
	{
		FileConfiguration config = Gods.instance().getConfig();

		this.debug = config.getBoolean("Settings.Debug", false);
		this.downloadLanguageFile = config.getBoolean("Settings.DownloadLanguageFile", true);
		this.languageIdentifier = config.getString("Settings.Language", "english");

		List<String> worldNames = config.getStringList("Settings.Worlds");
		if ((worldNames == null) || (worldNames.size() == 0))
		{
			Gods.instance().log("No worlds found in config file.");
			for (World world : Gods.instance().getServer().getWorlds())
			{
				if (!this.worlds.contains(world.getName())) {
					this.worlds.add(world.getName());
				}
				Gods.instance().log("Enabed in world '" + world.getName() + "'");
			}
			config.set("Settings.Worlds", this.worlds);
			Gods.instance().saveConfig();
		}
		else
		{
			for (String worldName : worldNames)
			{
				if (!this.worlds.contains(worldName)) {
					this.worlds.add(worldName);
				}
				Gods.instance().log("Enabled in '" + worldName + "'");
			}
			if (worldNames.size() == 0)
			{
				Gods.instance().log("WARNING: No worlds are set in config file. Gods are disabled on this server!");
			}
		}
		this.biblesEnabled = config.getBoolean("Bibles.Enabled", true);
		if (this.biblesEnabled)
		{
			HolyBookManager.instance().load();
		}
		
		this.marriageEnabled = config.getBoolean("Marriage.Enabled", true);		
		if (this.marriageEnabled)
		{
			this.marriageFireworksEnabled = config.getBoolean("Marriage.WeddingFireworks", true);
			MarriageManager.instance().load();
		}
		
		this.holyArtifactsEnabled = config.getBoolean("HolyArtifacts.Enabled", true);	
		if (this.holyArtifactsEnabled)
		{
			HolyPowerManager.instance();
			HolyArtifactManager.instance().load();
		}

		this.prayersEnabled = config.getBoolean("Prayers.Enabled", true);

		// if (this.propheciesEnabled)
		// {
		// this.prophecyManager = new ProphecyManager(this);
		// this.prophecyManager.load();
		//
		// //this.bossManager = new BossManager(this);
		// }

		this.chatFormattingEnabled = config.getBoolean("ChatFormatting.Enabled", false);

		if (this.chatFormattingEnabled)
			ChatManager.instance().load();

		this.holyLandEnabled = config.getBoolean("HolyLand.Enabled", false);

		if (this.holyLandEnabled)
			HolyLandManager.instance().load();

		this.holyLandRadiusPrPower = config.getDouble("HolyLand.RadiusPrPower", 1.25D);
		this.holyLandDefaultPvP = config.getBoolean("HolyLand.DefaultPvP", false);
		this.holyLandDefaultMobDamage = config.getBoolean("HolyLand.DefaultMobDamage", true);
		this.holyLandLightning = config.getBoolean("HolyLand.Lightning", false);
		this.allowInteractionInNeutralLands = config.getBoolean("HolyLand.AllowInteractionInNeutralLands", true);
		this.numberOfDaysForAbandonedHolyLands = config.getInt("HolyLand.DeleteAbandonedHolyLandsAfterDays", 14);

		List<String> blockList = config.getStringList("HolyLand.BreakableBlockTypes");
		if (blockList != null && blockList.size() > 0)
		{
			for (String blockType : blockList)
			{
				try
				{
					Gods.instance().logDebug("adding breakable block type " + blockType);
					this.holylandBreakableBlockTypes.add(Material.getMaterial(blockType));
				}
				catch (Exception ex)
				{
					Gods.instance().log("ERROR parsing HolyLand.BreakableBlockTypes blocktype '" + blockType + "' in config");
				}
			}
		}
		else
		{
			Gods.instance().log("No HolyLand.BreakableBlockTypes section found in config.");
			Gods.instance().log("Adding '" + Material.SMOOTH_STONE.name() + "' to BreakableBlockTypes");
			this.holylandBreakableBlockTypes.add(Material.SMOOTH_STONE);
		}

		this.sacrificesEnabled = config.getBoolean("Sacrifices.Enabled", true);

		this.commandmentsEnabled = config.getBoolean("Commandments.Enabled", true);
		this.commandmentsBroadcastFoodEaten = config.getBoolean("Commandments.BroadcastFoodEaten", true);
		this.commandmentsBroadcastMobSlain = config.getBoolean("Commandments.BroadcastMobSlain", true);

		this.questsEnabled = config.getBoolean("Quests.Enabled", false);
		this.minMinutesBetweenQuests = config.getInt("Quests.MinMinutesBetweenQuests", 180);

		this.globalQuestsPercentChance = config.getInt("Quests.GlobalQuestsPercentChance", 1);

		this.slayQuestsEnabled = config.getBoolean("Quests.SlayQuests", true);
		this.sacrificeQuestsEnabled = config.getBoolean("Quests.SacrificeQuests", true);
		this.pilgrimageQuestsEnabled = config.getBoolean("Quests.PilgrimageQuests", true);

		ConfigurationSection configSection = config.getConfigurationSection("Quests.RewardValues");
		if (configSection != null)
		{
			for (String rewardItem : configSection.getKeys(false))
			{
				try
				{
					Gods.instance().logDebug("Setting value for reward item " + rewardItem + " to " + config.getInt(new StringBuilder().append("Quests.RewardValues.").append(rewardItem).toString()));

					QuestManager.instance().setItemRewardValue(Material.getMaterial(rewardItem), config.getInt("Quests.RewardValues." + rewardItem));
				}
				catch (Exception ex)
				{
					Gods.instance().log("ERROR parsing Quests.RewardValues value '" + rewardItem + "' in config");
				}
			}
		}
		else
		{
			QuestManager.instance().resetItemRewardValues();
		}

		this.blessingEnabled = config.getBoolean("Blessing.Enabled", true);
		this.speedBlessingEnabled = config.getBoolean("Blessing.Speed", true);
		this.regenerationBlessingEnabled = config.getBoolean("Blessing.Regeneration", true);
		this.healBlessingEnabled = config.getBoolean("Blessing.Heal", true);
		this.fastDiggingBlessingEnabled = config.getBoolean("Blessing.FastDigging", true);
		this.increaseDamageBlessingEnabled = config.getBoolean("Blessing.IncreaseDamage", true);
		this.minBlessingTime = config.getInt("Blessing.MinBlessingTime", 600);
		this.maxBlessingTime = config.getInt("Blessing.MaxBlessingTime", 180);

		this.cursingEnabled = config.getBoolean("Cursing.Enabled", true);
		this.lightningCurseEnabled = config.getBoolean("Cursing.LightningCurse", true);
		this.mobCurseEnabled = config.getBoolean("Cursing.MobCurse", true);
		this.maxCursingTime = config.getInt("Cursing.MaxCursingTime", 10);
		this.minCursingTime = config.getInt("Cursing.MinCursingTime", 5);

		this.itemBlessingEnabled = config.getBoolean("ItemBlessing.Enabled", true);
		this.minItemBlessingTime = config.getInt("ItemBlessing.MinItemBlessingTime", 10);
		this.minGodPowerForItemBlessings = config.getInt("ItemBlessing.MinGodPowerForItemBlessings", 3);
		this.godPowerForLevel1Items = config.getInt("ItemBlessing.GodPowerForLevel1Items", 10);
		this.godPowerForLevel2Items = config.getInt("ItemBlessing.GodPowerForLevel2Items", 50);
		this.godPowerForLevel3Items = config.getInt("ItemBlessing.GodPowerForLevel3Items", 100);
		this.onlyPriestCanSetHome = config.getBoolean("Settings.OnlyPriestCanSetHome", false);
		this.leaveReligionOnDeath = config.getBoolean("Settings.LeaveReligionOnDeath", false);
		this.maxPriestPrayerTime = config.getInt("Settings.MaxPriestPrayerTime", 72);
		this.maxBelieverPrayerTime = config.getInt("Settings.MaxBelieverPrayerTime", 154);
		this.minBelieverPrayerTime = config.getInt("Settings.MinBelieverPrayerTime", 30);
		this.minBelieversForPriest = config.getInt("Settings.MinBelieversForPriest", 3);
		this.minSecondsBetweenChangingGod = config.getInt("Settings.MinSecondsBetweenChangingGod", 5 * 60);
		this.maxPriestsPrGod = config.getInt("Settings.MaxPriestsPrGod", 1);
		this.defaultPrivateReligions = config.getBoolean("Settings.DefaultPrivateReligions", false);

		this.broadcastNewGods = config.getBoolean("Settings.BroadcastNewGods", true);
		this.useWhitelist = config.getBoolean("Settings.UseWhitelist", false);
		this.useBlacklist = config.getBoolean("Settings.UseBlacklist", false);
		this.godVerbosity = config.getDouble("Settings.GodVerbosity", 1.0D);
		this.serverName = config.getString("Settings.ServerName", "Your Server");

		this.priestAssignCommand = config.getString("Settings.PriestAssignCommand", "");
		this.priestRemoveCommand = config.getString("Settings.PriestRemoveCommand", "");
		this.allowMultipleGodsPrDivinePower = config.getBoolean("Settings.AllowMultipleGodsPrDivinePower", false);
		this.moodFalloff = config.getDouble("Settings.MoodFalloff", 0.03D);

		configSection = config.getConfigurationSection("Altars.BlockTypes");

		if (configSection != null)
		{
			for (String godType : configSection.getKeys(false))
			{
				try
				{
					for (String blockMaterial : config.getStringList("Altars.BlockTypes." + godType))
					{
						Gods.instance().log("Setting block type " + blockMaterial + " for God type " + godType);
						AltarManager.instance().setAltarBlockTypeForGodType(GodType.valueOf(godType), Material.getMaterial(blockMaterial));
					}
				}
				catch (Exception ex)
				{
					Gods.instance().log("ERROR parsing Altars.BlockType value '" + godType + "' in config");
				}
			}
		}
		else
		{
			Gods.instance().log("No altar blocktypes found in config. Setting defaults.");
			AltarManager.instance().resetAltarBlockTypes();

			for (GodType godType : GodManager.GodType.values())
			{
				config.set("Altars.BlockTypes." + godType.name(), AltarManager.instance().getAltarBlockTypesFromGodType(godType));
			}
			saveSettings();
		}
	}

	public void saveSettings()
	{
		FileConfiguration config = Gods.instance().getConfig();
		config.set("Settings.Debug", Boolean.valueOf(this.debug));
		config.set("Settings.DownloadLanguageFile", Boolean.valueOf(this.downloadLanguageFile));
		config.set("Settings.Language", this.languageIdentifier);
		config.set("Settings.Worlds", this.worlds);
		config.set("Settings.UseWhitelist", Boolean.valueOf(this.useWhitelist));
		config.set("Settings.UseBlacklist", Boolean.valueOf(this.useBlacklist));
		config.set("Settings.BroadcastNewGods", Boolean.valueOf(this.broadcastNewGods));
		config.set("Settings.MaxPriestPrayerTime", Integer.valueOf(this.maxPriestPrayerTime));
		config.set("Settings.MaxBelieverPrayerTime", Integer.valueOf(this.maxBelieverPrayerTime));
		config.set("Settings.MinBelieverPrayerTime", Integer.valueOf(this.minBelieverPrayerTime));
		config.set("Settings.MinBelieversForPriest", Integer.valueOf(this.minBelieversForPriest));
		config.set("Settings.MaxPriestsPrGod", Integer.valueOf(this.maxPriestsPrGod));
		config.set("Settings.NumberOfBelieversPrPriest", Integer.valueOf(this.numberOfBelieversPrPriest));
		config.set("Settings.GodVerbosity", Double.valueOf(this.godVerbosity));
		config.set("Settings.ServerName", this.serverName);
		config.set("Settings.PriestAssignCommand", this.priestAssignCommand);
		config.set("Settings.PriestRemoveCommand", this.priestRemoveCommand);
		config.set("Settings.OnlyPriestCanSetHome", Boolean.valueOf(this.onlyPriestCanSetHome));
		config.set("Settings.LeaveReligionOnDeath", Boolean.valueOf(this.leaveReligionOnDeath));
		config.set("Settings.AllowMultipleGodsPrDivinePower", Boolean.valueOf(this.allowMultipleGodsPrDivinePower));
		config.set("Settings.MoodFalloff", Double.valueOf(this.moodFalloff));
		config.set("Settings.DefaultPrivateReligions", Boolean.valueOf(this.defaultPrivateReligions));
		for (GodManager.GodType godType : GodManager.GodType.values())
		{
			config.set("Altars.BlockTypes." + godType.name(), AltarManager.instance().getAltarBlockTypesFromGodType(godType));
		}
		config.set("ItemBlessing.Enabled", Boolean.valueOf(this.itemBlessingEnabled));
		config.set("ItemBlessing.MinGodPowerItemBlessings", Integer.valueOf(this.minGodPowerForItemBlessings));
		config.set("ItemBlessing.GodPowerForLevel1Items", Integer.valueOf(this.godPowerForLevel1Items));
		config.set("ItemBlessing.MinItemBlessingTime", Integer.valueOf(this.minItemBlessingTime));
		config.set("ItemBlessing.GodPowerForLevel2Items", Integer.valueOf(this.godPowerForLevel2Items));
		config.set("ItemBlessing.GodPowerForLevel3Items", Integer.valueOf(this.godPowerForLevel3Items));

		config.set("Blessing.Enabled", Boolean.valueOf(this.blessingEnabled));
		config.set("Blessing.Speed", Boolean.valueOf(this.speedBlessingEnabled));
		config.set("Blessing.Heal", Boolean.valueOf(this.healBlessingEnabled));
		config.set("Blessing.Regeneration", Boolean.valueOf(this.regenerationBlessingEnabled));
		config.set("Blessing.IncreaseDamage", Boolean.valueOf(this.increaseDamageBlessingEnabled));
		config.set("Blessing.FastDigging", Boolean.valueOf(this.fastDiggingBlessingEnabled));
		config.set("Blessing.MaxBlessingTime", Integer.valueOf(this.maxBlessingTime));
		config.set("Blessing.MinBlessingTime", Integer.valueOf(this.minBlessingTime));

		config.set("Cursing.Enabled", Boolean.valueOf(this.cursingEnabled));
		config.set("Cursing.LightningCurse", Boolean.valueOf(this.lightningCurseEnabled));
		config.set("Cursing.MobCurse", Boolean.valueOf(this.mobCurseEnabled));
		config.set("Cursing.MaxCursingTime", Integer.valueOf(this.maxCursingTime));
		config.set("Cursing.MinCursingTime", Integer.valueOf(this.minCursingTime));

		config.set("Quests.Enabled", Boolean.valueOf(this.questsEnabled));
		config.set("Quests.MinMinutesBetweenQuests", Integer.valueOf(this.minMinutesBetweenQuests));
		config.set("Quests.GlobalQuestsPercentChance", Integer.valueOf(this.globalQuestsPercentChance));
		config.set("Quests.SlayQuests", Boolean.valueOf(this.slayQuestsEnabled));
		config.set("Quests.SacrificeQuests", Boolean.valueOf(this.sacrificeQuestsEnabled));
		config.set("Quests.ConvertQuests", Boolean.valueOf(this.convertQuestsEnabled));
		config.set("Quests.GiveItemsQuests", Boolean.valueOf(this.giveItemsQuestsEnabled));
		config.set("Quests.PilgrimageQuests", Boolean.valueOf(this.pilgrimageQuestsEnabled));
		config.set("Quests.HolywarQuests", Boolean.valueOf(this.holywarQuestsEnabled));

		config.set("Sacrifices.Enabled", Boolean.valueOf(this.sacrificesEnabled));

		config.set("Commandments.Enabled", Boolean.valueOf(this.commandmentsEnabled));
		config.set("Commandments.BroadcastFoodEaten", Boolean.valueOf(this.commandmentsBroadcastFoodEaten));
		config.set("Commandments.BroadcastMobSlain", Boolean.valueOf(this.commandmentsBroadcastMobSlain));

		config.set("HolyLand.Enabled", Boolean.valueOf(this.holyLandEnabled));
		config.set("HolyLand.RadiusPrPower", Double.valueOf(this.holyLandRadiusPrPower));
		config.set("HolyLand.DefaultPvP", Boolean.valueOf(this.holyLandDefaultPvP));
		config.set("HolyLand.DefaultMobDamage", Boolean.valueOf(this.holyLandDefaultMobDamage));
		config.set("HolyLand.Lightning", Boolean.valueOf(this.holyLandLightning));
		config.set("HolyLand.AllowInteractionInNeutralLands", Boolean.valueOf(this.allowInteractionInNeutralLands));
		config.set("HolyLand.DeleteAbandonedHolyLandsAfterDays", Integer.valueOf(this.numberOfDaysForAbandonedHolyLands));

		List<String> blockTypes = new ArrayList<String>();
		for (Material blockType : this.holylandBreakableBlockTypes)
		{
			if(blockType!=null)
			{
				blockTypes.add(blockType.name());
			}
		}
		config.set("HolyLand.BreakableBlockTypes", blockTypes);

		config.set("ChatFormatting.Enabled", this.chatFormattingEnabled);

		config.set("Bibles.Enabled", this.biblesEnabled);

		config.set("Prophecies.Enabled", this.propheciesEnabled);

		config.set("HolyArtifacts.Enabled", this.holyArtifactsEnabled);

		config.set("Marriage.Enabled", this.marriageEnabled);
		config.set("Marriage.WeddingFireworks", this.marriageFireworksEnabled);

		config.set("Prayers.Enabled", Boolean.valueOf(this.prayersEnabled));

		Gods.instance().saveConfig();
	}

	public final void setAllowInteractionInNeutralLands(boolean allowInteractionInNeutralLands)
	{
		this.allowInteractionInNeutralLands = allowInteractionInNeutralLands;
	}

	public final void setAllowMultipleGodsPrDivinePower(boolean allowMultipleGodsPrDivinePower)
	{
		this.allowMultipleGodsPrDivinePower = allowMultipleGodsPrDivinePower;
	}

	public final void setBiblesEnabled(boolean biblesEnabled)
	{
		this.biblesEnabled = biblesEnabled;
	}

	public final void setBlessingEnabled(boolean blessingEnabled)
	{
		this.blessingEnabled = blessingEnabled;
	}

	public final void setBroadcastNewGods(boolean broadcastNewGods)
	{
		this.broadcastNewGods = broadcastNewGods;
	}

	public final void setBroadcastProphecyFullfillment(boolean broadcastProphecyFullfillment)
	{
		this.broadcastProphecyFullfillment = broadcastProphecyFullfillment;
	}

	public final void setBuildTowerQuestsEnabled(boolean buildTowerQuestsEnabled)
	{
		this.buildTowerQuestsEnabled = buildTowerQuestsEnabled;
	}

	public final void setBurnBiblesQuestsEnabled(boolean burnBiblesQuestsEnabled)
	{
		this.burnBiblesQuestsEnabled = burnBiblesQuestsEnabled;
	}

	public final void setChatFormattingEnabled(boolean chatFormattingEnabled)
	{
		this.chatFormattingEnabled = chatFormattingEnabled;
	}

	public final void setCommandmentsBroadcastFoodEaten(boolean commandmentsBroadcastFoodEaten)
	{
		this.commandmentsBroadcastFoodEaten = commandmentsBroadcastFoodEaten;
	}

	public final void setCommandmentsBroadcastMobSlain(boolean commandmentsBroadcastMobSlain)
	{
		this.commandmentsBroadcastMobSlain = commandmentsBroadcastMobSlain;
	}

	public final void setCommandmentsEnabled(boolean commandmentsEnabled)
	{
		this.commandmentsEnabled = commandmentsEnabled;
	}

	public final void setConvertQuestsEnabled(boolean convertQuestsEnabled)
	{
		this.convertQuestsEnabled = convertQuestsEnabled;
	}

	public final void setCrusadeQuestsEnabled(boolean crusadeQuestsEnabled)
	{
		this.crusadeQuestsEnabled = crusadeQuestsEnabled;
	}

	public final void setCursingEnabled(boolean cursingEnabled)
	{
		this.cursingEnabled = cursingEnabled;
	}

	public final void setDebug(boolean debug)
	{
		this.debug = debug;
	}

	public final void setDefaultPrivateReligions(boolean defaultPrivateReligions)
	{
		this.defaultPrivateReligions = defaultPrivateReligions;
	}

	public final void setDownloadLanguageFile(boolean downloadLanguageFile)
	{
		this.downloadLanguageFile = downloadLanguageFile;
	}

	public final void setEnableDetoration(boolean enableDetoration)
	{
		this.enableDetoration = enableDetoration;
	}

	public final void setFastDiggingBlessingEnabled(boolean fastDiggingBlessingEnabled)
	{
		this.fastDiggingBlessingEnabled = fastDiggingBlessingEnabled;
	}

	public final void setGiveItemsQuestsEnabled(boolean giveItemsQuestsEnabled)
	{
		this.giveItemsQuestsEnabled = giveItemsQuestsEnabled;
	}

	public final void setGlobalQuestsEnabled(boolean globalQuestsEnabled)
	{
		this.globalQuestsEnabled = globalQuestsEnabled;
	}

	public final void setGlobalQuestsPercentChance(int globalQuestsPercentChance)
	{
		this.globalQuestsPercentChance = globalQuestsPercentChance;
	}

	public final void setGodPowerForLevel1Items(int godPowerForLevel1Items)
	{
		this.godPowerForLevel1Items = godPowerForLevel1Items;
	}

	public final void setGodPowerForLevel2Items(int godPowerForLevel2Items)
	{
		this.godPowerForLevel2Items = godPowerForLevel2Items;
	}

	public final void setGodPowerForLevel3Items(int godPowerForLevel3Items)
	{
		this.godPowerForLevel3Items = godPowerForLevel3Items;
	}

	public final void setGodVerbosity(double godVerbosity)
	{
		this.godVerbosity = godVerbosity;
	}

	public final void setHealBlessingEnabled(boolean healBlessingEnabled)
	{
		this.healBlessingEnabled = healBlessingEnabled;
	}

	public final void setHolyArtifactsEnabled(boolean holyArtifactsEnabled)
	{
		this.holyArtifactsEnabled = holyArtifactsEnabled;
	}

	public final void setHolyFeastQuestsEnabled(boolean holyFeastQuestsEnabled)
	{
		this.holyFeastQuestsEnabled = holyFeastQuestsEnabled;
	}

	public final void setHolylandBreakableBlockTypes(Set<Material> holylandBreakableBlockTypes)
	{
		this.holylandBreakableBlockTypes = holylandBreakableBlockTypes;
	}

	public final void setHolyLandDefaultMobDamage(boolean holyLandDefaultMobDamage)
	{
		this.holyLandDefaultMobDamage = holyLandDefaultMobDamage;
	}

	public final void setHolyLandDefaultPvP(boolean holyLandDefaultPvP)
	{
		this.holyLandDefaultPvP = holyLandDefaultPvP;
	}

	public final void setHolyLandEnabled(boolean holyLandEnabled)
	{
		this.holyLandEnabled = holyLandEnabled;
	}

	public final void setHolyLandLightning(boolean holyLandLightning)
	{
		this.holyLandLightning = holyLandLightning;
	}

	public final void setHolyLandRadiusPrPower(double holyLandRadiusPrPower)
	{
		this.holyLandRadiusPrPower = holyLandRadiusPrPower;
	}

	public final void setHolywarQuestsEnabled(boolean holywarQuestsEnabled)
	{
		this.holywarQuestsEnabled = holywarQuestsEnabled;
	}

	public final void setIncreaseDamageBlessingEnabled(boolean increaseDamageBlessingEnabled)
	{
		this.increaseDamageBlessingEnabled = increaseDamageBlessingEnabled;
	}

	public final void setItemBlessingEnabled(boolean itemBlessingEnabled)
	{
		this.itemBlessingEnabled = itemBlessingEnabled;
	}

	public final void setLanguageIdentifier(String languageIdentifier)
	{
		this.languageIdentifier = languageIdentifier;
	}

	public final void setLeaveReligionOnDeath(boolean leaveReligionOnDeath)
	{
		this.leaveReligionOnDeath = leaveReligionOnDeath;
	}

	public final void setLightningCurseEnabled(boolean lightningCurseEnabled)
	{
		this.lightningCurseEnabled = lightningCurseEnabled;
	}

	public final void setMarriageEnabled(boolean marriageEnabled)
	{
		this.marriageEnabled = marriageEnabled;
	}

	public final void setMarriageFireworksEnabled(boolean marriageFireworksEnabled)
	{
		this.marriageFireworksEnabled = marriageFireworksEnabled;
	}

	public final void setMaxBelieverPrayerTime(int maxBelieverPrayerTime)
	{
		this.maxBelieverPrayerTime = maxBelieverPrayerTime;
	}

	public final void setMaxBlessingTime(int maxBlessingTime)
	{
		this.maxBlessingTime = maxBlessingTime;
	}

	public final void setMaxCursingTime(int maxCursingTime)
	{
		this.maxCursingTime = maxCursingTime;
	}

	public final void setMaxHolyArtifacts(int maxHolyArtifacts)
	{
		this.maxHolyArtifacts = maxHolyArtifacts;
	}

	public final void setMaxInvitationTimeSeconds(int maxInvitationTimeSeconds)
	{
		this.maxInvitationTimeSeconds = maxInvitationTimeSeconds;
	}

	public final void setMaxPriestPrayerTime(int maxPriestPrayerTime)
	{
		this.maxPriestPrayerTime = maxPriestPrayerTime;
	}

	public final void setMaxPriestsPrGod(int maxPriestsPrGod)
	{
		this.maxPriestsPrGod = maxPriestsPrGod;
	}

	public final void setMinBelieverPrayerTime(int minBelieverPrayerTime)
	{
		this.minBelieverPrayerTime = minBelieverPrayerTime;
	}

	public final void setMinBelieversForPriest(int minBelieversForPriest)
	{
		this.minBelieversForPriest = minBelieversForPriest;
	}

	public final void setMinBlessingTime(int minBlessingTime)
	{
		this.minBlessingTime = minBlessingTime;
	}

	public final void setMinCursingTime(int minCursingTime)
	{
		this.minCursingTime = minCursingTime;
	}

	public final void setMinGodPowerForItemBlessings(int minGodPowerForItemBlessings)
	{
		this.minGodPowerForItemBlessings = minGodPowerForItemBlessings;
	}

	public final void setMinHolyArtifactBlessingTime(int minHolyArtifactBlessingTime)
	{
		this.minHolyArtifactBlessingTime = minHolyArtifactBlessingTime;
	}

	public final void setMinItemBlessingTime(int minItemBlessingTime)
	{
		this.minItemBlessingTime = minItemBlessingTime;
	}

	public final void setMinMinutesBetweenQuests(int minMinutesBetweenQuests)
	{
		this.minMinutesBetweenQuests = minMinutesBetweenQuests;
	}

	public final void setMinSecondsBetweenChangingGod(int minSecondsBetweenChangingGod)
	{
		this.minSecondsBetweenChangingGod = minSecondsBetweenChangingGod;
	}

	public final void setMobCurseEnabled(boolean mobCurseEnabled)
	{
		this.mobCurseEnabled = mobCurseEnabled;
	}

	public final void setMoodFalloff(double moodFalloff)
	{
		this.moodFalloff = moodFalloff;
	}

	public final void setNumberOfBelieversPrPriest(int numberOfBelieversPrPriest)
	{
		this.numberOfBelieversPrPriest = numberOfBelieversPrPriest;
	}

	public final void setNumberOfDaysForAbandonedHolyLands(int numberOfDaysForAbandonedHolyLands)
	{
		this.numberOfDaysForAbandonedHolyLands = numberOfDaysForAbandonedHolyLands;
	}

	public final void setOnlyPriestCanSetHome(boolean onlyPriestCanSetHome)
	{
		this.onlyPriestCanSetHome = onlyPriestCanSetHome;
	}

	public final void setPilgrimageQuestsEnabled(boolean pilgrimageQuestsEnabled)
	{
		this.pilgrimageQuestsEnabled = pilgrimageQuestsEnabled;
	}

	public final void setPowerLossOnDeath(boolean powerLossOnDeath)
	{
		this.powerLossOnDeath = powerLossOnDeath;
	}

	public final void setPrayerPowerForBlessing(int prayerPowerForBlessing)
	{
		this.prayerPowerForBlessing = prayerPowerForBlessing;
	}

	public final void setPrayerPowerForHealth(int prayerPowerForHealth)
	{
		this.prayerPowerForHealth = prayerPowerForHealth;
	}

	public final void setPrayerPowerForHolyArtifact(int prayerPowerForHolyArtifact)
	{
		this.prayerPowerForHolyArtifact = prayerPowerForHolyArtifact;
	}

	public final void setPrayerPowerForItem(int prayerPowerForItem)
	{
		this.prayerPowerForItem = prayerPowerForItem;
	}

	public final void setPrayerPowerForQuest(int prayerPowerForQuest)
	{
		this.prayerPowerForQuest = prayerPowerForQuest;
	}

	public final void setPrayersEnabled(boolean prayersEnabled)
	{
		this.prayersEnabled = prayersEnabled;
	}

	public final void setPriestAssignCommand(String priestAssignCommand)
	{
		this.priestAssignCommand = priestAssignCommand;
	}

	public final void setPriestRemoveCommand(String priestRemoveCommand)
	{
		this.priestRemoveCommand = priestRemoveCommand;
	}

	public final void setPropheciesEnabled(boolean propheciesEnabled)
	{
		this.propheciesEnabled = propheciesEnabled;
	}

	public final void setQuestsEnabled(boolean questsEnabled)
	{
		this.questsEnabled = questsEnabled;
	}

	public final void setRegenerationBlessingEnabled(boolean regenerationBlessingEnabled)
	{
		this.regenerationBlessingEnabled = regenerationBlessingEnabled;
	}

	public final void setRequiredBelieversForQuests(int requiredBelieversForQuests)
	{
		this.requiredBelieversForQuests = requiredBelieversForQuests;
	}

	public final void setSacrificeQuestsEnabled(boolean sacrificeQuestsEnabled)
	{
		this.sacrificeQuestsEnabled = sacrificeQuestsEnabled;
	}

	public final void setSacrificesEnabled(boolean sacrificesEnabled)
	{
		this.sacrificesEnabled = sacrificesEnabled;
	}

	public final void setServerName(String serverName)
	{
		this.serverName = serverName;
	}

	public final void setSlayDragonQuestsEnabled(boolean slayDragonQuestsEnabled)
	{
		this.slayDragonQuestsEnabled = slayDragonQuestsEnabled;
	}

	public final void setSlayQuestsEnabled(boolean slayQuestsEnabled)
	{
		this.slayQuestsEnabled = slayQuestsEnabled;
	}

	public final void setSpeedBlessingEnabled(boolean speedBlessingEnabled)
	{
		this.speedBlessingEnabled = speedBlessingEnabled;
	}

	public final void setUseBlacklist(boolean useBlacklist)
	{
		this.useBlacklist = useBlacklist;
	}

	public final void setUseGodTitles(boolean useGodTitles)
	{
		this.useGodTitles = useGodTitles;
	}

	public final void setUseWhitelist(boolean useWhitelist)
	{
		this.useWhitelist = useWhitelist;
	}

	public final void setWerewolfEnabled(boolean werewolfEnabled)
	{
		this.werewolfEnabled = werewolfEnabled;
	}

	public final void setWorlds(List<String> worlds)
	{
		this.worlds = worlds;
	}
}