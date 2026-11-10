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
import com.dogonfire.gods.managers.HolyBookManager;
import com.dogonfire.gods.managers.HolyLandManager;
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

	/**
	 * Keys older versions wrote to config.yml for options that do nothing. They are removed on save.
	 */
	private static final String[]	OBSOLETE_KEYS						= { "Settings.DownloadLanguageFile", "Settings.NumberOfBelieversPrPriest", "Settings.AllowMultipleGodsPrDivinePower", "Quests.ConvertQuests", "Quests.HolywarQuests", "Quests.GlobalQuestsPercentChance", "Prophecies", "HolyLand.RadiusPrPower", "ItemBlessing.MinGodPowerItemBlessings" };

	private boolean					debug								= false;
	private boolean					useWhitelist						= false;
	private boolean					useBlacklist						= false;
	private boolean					questsEnabled						= false;
	private boolean					itemBlessingEnabled					= true;
	private boolean					blessingEnabled						= true;
	private boolean					commandmentsEnabled					= true;
	private boolean					sacrificesEnabled					= true;
	private boolean					holyLandEnabled						= false;
	private boolean					biblesEnabled						= false;
	private boolean					giveBibleWhenJoinReligion			= false;
	private boolean					holyArtifactsEnabled				= false;
	private boolean					chatFormattingEnabled				= false;
	private boolean					useGodTitles						= true;
	private boolean					marriageEnabled						= false;
	private boolean					marriageFireworksEnabled			= false;
	private boolean					cursingEnabled						= true;
	private boolean					lightningCurseEnabled				= false;
	private boolean					mobCurseEnabled						= true;
	private boolean					fastDiggingBlessingEnabled			= false;
	private boolean					healBlessingEnabled					= false;
	private boolean					regenerationBlessingEnabled			= false;
	private boolean					speedBlessingEnabled				= false;
	private boolean					increaseDamageBlessingEnabled		= false;
	private boolean					slayQuestsEnabled					= true;
	private boolean					sacrificeQuestsEnabled				= true;
	private boolean					pilgrimageQuestsEnabled				= false;
	private boolean					giveItemsQuestsEnabled				= false;
	private boolean					broadcastNewGods					= true;
	private boolean					defaultPrivateReligions				= false;
	private boolean					leaveReligionOnDeath				= false;
	private boolean					onlyPriestCanSetHome				= false;
	private boolean					commandmentsBroadcastFoodEaten		= true;
	private boolean					commandmentsBroadcastMobSlain		= true;
	private boolean					holyLandDefaultPvP					= false;
	private boolean					holyLandDefaultMobDamage			= true;
	private boolean					holyLandLightning					= true;
	private boolean					prayersEnabled						= true;
	private boolean					allowInteractionInNeutralLands		= true;
	private int						minCursingTime						= 5;
	private int						maxCursingTime						= 10;
	private int						minBlessingTime						= 600;
	private int						maxBlessingTime						= 180;
	private int						minMinutesBetweenQuests				= 180;
	private int						maxPriestsPrGod						= 1;
	private int						minItemBlessingTime					= 10;
	private int						minHolyArtifactBlessingTime			= 1440;
	private int						maxPriestPrayerTime					= 72;
	private int						maxBelieverPrayerTime				= 154;
	private int						minBelieverPrayerTime				= 30;
	private int						minGodPowerForItemBlessings			= 3;
	private int						godPowerForLevel3Items				= 100;
	private int						godPowerForLevel2Items				= 50;
	private int						godPowerForLevel1Items				= 10;
	private int						minBelieversForPriest				= 3;
	private int						minSecondsBetweenChangingGod		= 5 * 60;
	private int						requiredBelieversForQuests			= 1;
	private int						numberOfDaysForAbandonedHolyLands	= 14;
	private int						prayerPowerForItem					= 20;
	private int						prayerPowerForQuest					= 50;
	private int						prayerPowerForBlessing				= 10;
	private int						prayerPowerForHealth				= 10;
	private double					moodFalloff							= 0.03D;
	private double					godVerbosity						= 1.0D;
	private String					languageIdentifier					= "english";
	private String					priestAssignCommand					= "";
	private String					priestRemoveCommand					= "";
	private String					serverName							= "Your Server";

	private Set<Material>			holylandBreakableBlockTypes			= new HashSet<Material>();

	private List<String>			worlds								= new ArrayList<String>();

	private GodsConfiguration()
	{
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

	public final String getLanguageIdentifier()
	{
		return languageIdentifier;
	}

	public final int getMaxBelieverPrayerTime()
	{
		return maxBelieverPrayerTime;
	}

	/**
	 * @return how many seconds a player stays the blessed player of a god after a priest names them
	 */
	public final int getMaxBlessingTime()
	{
		return maxBlessingTime;
	}

	/**
	 * @return how many minutes a player stays the cursed player of a god after a priest names them
	 */
	public final int getMaxCursingTime()
	{
		return maxCursingTime;
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

	/**
	 * @return the minimum number of seconds between two blessings of the same player
	 */
	public final int getMinBlessingTime()
	{
		return minBlessingTime;
	}

	/**
	 * @return the minimum number of seconds between two curses of the same player
	 */
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

	public final int getPrayerPowerForItem()
	{
		return prayerPowerForItem;
	}

	public final int getPrayerPowerForQuest()
	{
		return prayerPowerForQuest;
	}

	/**
	 * @return the console command to run when a player becomes a priest, with $PlayerName for the player's name, or ""
	 */
	public final String getPriestAssignCommand()
	{
		return priestAssignCommand;
	}

	/**
	 * @return the console command to run when a priest is removed, with $PlayerName for the player's name, or ""
	 */
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

	public final boolean isBiblesEnabled()
	{
		return biblesEnabled;
	}

	public final boolean isGiveBibleWhenJoinReligion()
	{
		return giveBibleWhenJoinReligion;
	}

	public final boolean isBlessingEnabled()
	{
		return blessingEnabled;
	}

	public final boolean isBroadcastNewGods()
	{
		return broadcastNewGods;
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

	public final boolean isFastDiggingBlessingEnabled()
	{
		return fastDiggingBlessingEnabled;
	}

	public final boolean isGiveItemsQuestsEnabled()
	{
		return giveItemsQuestsEnabled;
	}

	public final boolean isHealBlessingEnabled()
	{
		return healBlessingEnabled;
	}

	public final boolean isHolyArtifactsEnabled()
	{
		return holyArtifactsEnabled;
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

	public final boolean isPrayersEnabled()
	{
		return prayersEnabled;
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

	public void loadSettings()
	{
		FileConfiguration config = Gods.instance().getConfig();

		this.debug = config.getBoolean("Settings.Debug", false);
		this.languageIdentifier = config.getString("Settings.Language", "english");

		this.worlds.clear();
		List<String> worldNames = config.getStringList("Settings.Worlds");
		if (worldNames.isEmpty())
		{
			Gods.instance().log("No worlds found in config file.");
			for (World world : Gods.instance().getServer().getWorlds())
			{
				if (!this.worlds.contains(world.getName()))
				{
					this.worlds.add(world.getName());
				}
				Gods.instance().log("Enabled in world '" + world.getName() + "'");
			}
			config.set("Settings.Worlds", this.worlds);
			Gods.instance().saveConfig();
		}
		else
		{
			for (String worldName : worldNames)
			{
				if (!this.worlds.contains(worldName))
				{
					this.worlds.add(worldName);
				}
				Gods.instance().log("Enabled in '" + worldName + "'");
			}
		}

		this.biblesEnabled = config.getBoolean("Bibles.Enabled", true);
		this.giveBibleWhenJoinReligion = config.getBoolean("Bibles.GiveBibleWhenJoinReligion", false);
		if (this.biblesEnabled)
		{
			HolyBookManager.instance().load();
		}

		this.marriageEnabled = config.getBoolean("Marriage.Enabled", true);
		this.marriageFireworksEnabled = config.getBoolean("Marriage.WeddingFireworks", true);
		if (this.marriageEnabled)
		{
			MarriageManager.instance().load();
		}

		this.holyArtifactsEnabled = config.getBoolean("HolyArtifacts.Enabled", true);
		this.minHolyArtifactBlessingTime = config.getInt("HolyArtifacts.MinHolyArtifactBlessingTime", 1440);

		this.prayersEnabled = config.getBoolean("Prayers.Enabled", true);
		this.prayerPowerForItem = config.getInt("Prayers.PrayerPowerForItem", 20);
		this.prayerPowerForQuest = config.getInt("Prayers.PrayerPowerForQuest", 50);
		this.prayerPowerForBlessing = config.getInt("Prayers.PrayerPowerForBlessing", 10);
		this.prayerPowerForHealth = config.getInt("Prayers.PrayerPowerForHealth", 10);

		this.chatFormattingEnabled = config.getBoolean("ChatFormatting.Enabled", false);

		if (this.chatFormattingEnabled)
		{
			ChatManager.instance().load();
		}

		this.holyLandEnabled = config.getBoolean("HolyLand.Enabled", false);

		if (this.holyLandEnabled)
		{
			HolyLandManager.instance().load();
		}

		this.holyLandDefaultPvP = config.getBoolean("HolyLand.DefaultPvP", false);
		this.holyLandDefaultMobDamage = config.getBoolean("HolyLand.DefaultMobDamage", true);
		this.holyLandLightning = config.getBoolean("HolyLand.Lightning", false);
		this.allowInteractionInNeutralLands = config.getBoolean("HolyLand.AllowInteractionInNeutralLands", true);
		this.numberOfDaysForAbandonedHolyLands = config.getInt("HolyLand.DeleteAbandonedHolyLandsAfterDays", 14);

		this.holylandBreakableBlockTypes.clear();
		List<String> blockList = config.getStringList("HolyLand.BreakableBlockTypes");
		if (!blockList.isEmpty())
		{
			for (String blockType : blockList)
			{
				Material material = Material.getMaterial(blockType);
				if (material == null)
				{
					Gods.instance().log("WARNING: Unknown block type '" + blockType + "' in HolyLand.BreakableBlockTypes in config.yml. Ignoring it.");
					continue;
				}
				Gods.instance().logDebug("adding breakable block type " + blockType);
				this.holylandBreakableBlockTypes.add(material);
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
		this.requiredBelieversForQuests = config.getInt("Quests.RequiredBelieversForQuests", 1);

		this.slayQuestsEnabled = config.getBoolean("Quests.SlayQuests", true);
		this.sacrificeQuestsEnabled = config.getBoolean("Quests.SacrificeQuests", true);
		this.pilgrimageQuestsEnabled = config.getBoolean("Quests.PilgrimageQuests", true);
		this.giveItemsQuestsEnabled = config.getBoolean("Quests.GiveItemsQuests", false);

		QuestManager.instance().clearItemRewardValues();
		ConfigurationSection configSection = config.getConfigurationSection("Quests.RewardValues");
		if (configSection != null)
		{
			for (String rewardItem : configSection.getKeys(false))
			{
				Material material = Material.getMaterial(rewardItem);
				if (material == null)
				{
					Gods.instance().log("WARNING: Unknown item type '" + rewardItem + "' in Quests.RewardValues in config.yml. Ignoring it.");
					continue;
				}
				int value = configSection.getInt(rewardItem);
				Gods.instance().logDebug("Setting value for reward item " + rewardItem + " to " + value);
				QuestManager.instance().setItemRewardValue(material, value);
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
		// Versions before 1.0 saved this as MinGodPowerItemBlessings but read MinGodPowerForItemBlessings
		this.minGodPowerForItemBlessings = config.getInt("ItemBlessing.MinGodPowerForItemBlessings", config.getInt("ItemBlessing.MinGodPowerItemBlessings", 3));
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
		this.useGodTitles = config.getBoolean("Settings.UseGodTitles", true);
		this.godVerbosity = config.getDouble("Settings.GodVerbosity", 1.0D);
		this.serverName = config.getString("Settings.ServerName", "Your Server");

		this.priestAssignCommand = config.getString("Settings.PriestAssignCommand", "");
		this.priestRemoveCommand = config.getString("Settings.PriestRemoveCommand", "");
		this.moodFalloff = config.getDouble("Settings.MoodFalloff", 0.03D);

		configSection = config.getConfigurationSection("Altars.BlockTypes");

		AltarManager.instance().clearAltarBlockTypes();
		if (configSection != null)
		{
			for (String godType : configSection.getKeys(false))
			{
				GodType type;
				try
				{
					type = GodType.valueOf(godType);
				}
				catch (IllegalArgumentException ex)
				{
					Gods.instance().log("WARNING: Unknown god type '" + godType + "' in Altars.BlockTypes in config.yml. Ignoring it.");
					continue;
				}

				for (String blockMaterial : configSection.getStringList(godType))
				{
					Material material = Material.getMaterial(blockMaterial);
					if (material == null)
					{
						Gods.instance().log("WARNING: Unknown block type '" + blockMaterial + "' for god type " + godType + " in Altars.BlockTypes in config.yml. Ignoring it.");
						continue;
					}
					Gods.instance().log("Setting block type " + blockMaterial + " for God type " + godType);
					AltarManager.instance().setAltarBlockTypeForGodType(type, material);
				}
			}
		}
		else
		{
			Gods.instance().log("No altar blocktypes found in config. Setting defaults.");
			AltarManager.instance().resetAltarBlockTypes();
			saveSettings();
		}
	}

	public void saveSettings()
	{
		FileConfiguration config = Gods.instance().getConfig();

		for (String key : OBSOLETE_KEYS)
		{
			config.set(key, null);
		}

		config.set("Settings.Debug", this.debug);
		config.set("Settings.Language", this.languageIdentifier);
		config.set("Settings.Worlds", this.worlds);
		config.set("Settings.UseWhitelist", this.useWhitelist);
		config.set("Settings.UseBlacklist", this.useBlacklist);
		config.set("Settings.UseGodTitles", this.useGodTitles);
		config.set("Settings.BroadcastNewGods", this.broadcastNewGods);
		config.set("Settings.MaxPriestPrayerTime", this.maxPriestPrayerTime);
		config.set("Settings.MaxBelieverPrayerTime", this.maxBelieverPrayerTime);
		config.set("Settings.MinBelieverPrayerTime", this.minBelieverPrayerTime);
		config.set("Settings.MinBelieversForPriest", this.minBelieversForPriest);
		config.set("Settings.MinSecondsBetweenChangingGod", this.minSecondsBetweenChangingGod);
		config.set("Settings.MaxPriestsPrGod", this.maxPriestsPrGod);
		config.set("Settings.GodVerbosity", this.godVerbosity);
		config.set("Settings.ServerName", this.serverName);
		config.set("Settings.PriestAssignCommand", this.priestAssignCommand);
		config.set("Settings.PriestRemoveCommand", this.priestRemoveCommand);
		config.set("Settings.OnlyPriestCanSetHome", this.onlyPriestCanSetHome);
		config.set("Settings.LeaveReligionOnDeath", this.leaveReligionOnDeath);
		config.set("Settings.MoodFalloff", this.moodFalloff);
		config.set("Settings.DefaultPrivateReligions", this.defaultPrivateReligions);
		for (GodManager.GodType godType : GodManager.GodType.values())
		{
			config.set("Altars.BlockTypes." + godType.name(), AltarManager.instance().getAltarBlockTypesFromGodType(godType));
		}
		config.set("ItemBlessing.Enabled", this.itemBlessingEnabled);
		config.set("ItemBlessing.MinGodPowerForItemBlessings", this.minGodPowerForItemBlessings);
		config.set("ItemBlessing.GodPowerForLevel1Items", this.godPowerForLevel1Items);
		config.set("ItemBlessing.MinItemBlessingTime", this.minItemBlessingTime);
		config.set("ItemBlessing.GodPowerForLevel2Items", this.godPowerForLevel2Items);
		config.set("ItemBlessing.GodPowerForLevel3Items", this.godPowerForLevel3Items);

		config.set("Blessing.Enabled", this.blessingEnabled);
		config.set("Blessing.Speed", this.speedBlessingEnabled);
		config.set("Blessing.Heal", this.healBlessingEnabled);
		config.set("Blessing.Regeneration", this.regenerationBlessingEnabled);
		config.set("Blessing.IncreaseDamage", this.increaseDamageBlessingEnabled);
		config.set("Blessing.FastDigging", this.fastDiggingBlessingEnabled);
		config.set("Blessing.MaxBlessingTime", this.maxBlessingTime);
		config.set("Blessing.MinBlessingTime", this.minBlessingTime);

		config.set("Cursing.Enabled", this.cursingEnabled);
		config.set("Cursing.LightningCurse", this.lightningCurseEnabled);
		config.set("Cursing.MobCurse", this.mobCurseEnabled);
		config.set("Cursing.MaxCursingTime", this.maxCursingTime);
		config.set("Cursing.MinCursingTime", this.minCursingTime);

		config.set("Quests.Enabled", this.questsEnabled);
		config.set("Quests.MinMinutesBetweenQuests", this.minMinutesBetweenQuests);
		config.set("Quests.RequiredBelieversForQuests", this.requiredBelieversForQuests);
		config.set("Quests.SlayQuests", this.slayQuestsEnabled);
		config.set("Quests.SacrificeQuests", this.sacrificeQuestsEnabled);
		config.set("Quests.GiveItemsQuests", this.giveItemsQuestsEnabled);
		config.set("Quests.PilgrimageQuests", this.pilgrimageQuestsEnabled);

		config.set("Sacrifices.Enabled", this.sacrificesEnabled);

		config.set("Commandments.Enabled", this.commandmentsEnabled);
		config.set("Commandments.BroadcastFoodEaten", this.commandmentsBroadcastFoodEaten);
		config.set("Commandments.BroadcastMobSlain", this.commandmentsBroadcastMobSlain);

		config.set("HolyLand.Enabled", this.holyLandEnabled);
		config.set("HolyLand.DefaultPvP", this.holyLandDefaultPvP);
		config.set("HolyLand.DefaultMobDamage", this.holyLandDefaultMobDamage);
		config.set("HolyLand.Lightning", this.holyLandLightning);
		config.set("HolyLand.AllowInteractionInNeutralLands", this.allowInteractionInNeutralLands);
		config.set("HolyLand.DeleteAbandonedHolyLandsAfterDays", this.numberOfDaysForAbandonedHolyLands);

		List<String> blockTypes = new ArrayList<String>();
		for (Material blockType : this.holylandBreakableBlockTypes)
		{
			blockTypes.add(blockType.name());
		}
		config.set("HolyLand.BreakableBlockTypes", blockTypes);

		config.set("ChatFormatting.Enabled", this.chatFormattingEnabled);

		config.set("Bibles.Enabled", this.biblesEnabled);
		config.set("Bibles.GiveBibleWhenJoinReligion", this.giveBibleWhenJoinReligion);

		config.set("HolyArtifacts.Enabled", this.holyArtifactsEnabled);
		config.set("HolyArtifacts.MinHolyArtifactBlessingTime", this.minHolyArtifactBlessingTime);

		config.set("Marriage.Enabled", this.marriageEnabled);
		config.set("Marriage.WeddingFireworks", this.marriageFireworksEnabled);

		config.set("Prayers.Enabled", this.prayersEnabled);
		config.set("Prayers.PrayerPowerForItem", this.prayerPowerForItem);
		config.set("Prayers.PrayerPowerForQuest", this.prayerPowerForQuest);
		config.set("Prayers.PrayerPowerForBlessing", this.prayerPowerForBlessing);
		config.set("Prayers.PrayerPowerForHealth", this.prayerPowerForHealth);

		Gods.instance().saveConfig();
	}

	public final void setUseBlacklist(boolean useBlacklist)
	{
		this.useBlacklist = useBlacklist;
	}

	public final void setUseWhitelist(boolean useWhitelist)
	{
		this.useWhitelist = useWhitelist;
	}
}
