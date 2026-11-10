package com.dogonfire.gods.managers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.GodsConfiguration;

public class LanguageManager
{
	public static enum LANGUAGESTRING {
		GodToBelieverPrayerRecentItemBlessing, GodToBelieverPrayerWhenNoItemNeed, GodToBelieverPrayerTooSoon, GodToBelieversClaimedHolyLand, EnterHolyLandInfoYourGod, EnterHolyLandInfoOtherGod, EnterWildernessInfo, EnterNeutralLandInfo, EnterContestedLandInfo, PrivateGodNoAccess, DivorcedYou, MarrigeLovesYou, MarrigeYouLove, HolylandSize, YouLeftReligion, AttackHelp, GodsHelp, AltarHelp, PrayForHelp, DescriptionHelp, FollowersHelp, InviteHelp, PrayAlterHelp, PrayToBlacklistedGodNotAllowed, BlessingsNotAllowed, CursesNotAllowed, CannotClaimMoreHolyland, TeleportIntoHolylandNotAllowed, AltarPrayingNotAllowed, YouEarnedPowerBySlayingHeathen, YouEarnedPowerBySlayingEnemy, PvPLostPower, ConfirmChangeToOtherReligion, CannotPraySoSoon, CannotChangeGodSoSoon, CannotBuildAltarToOtherGods, InfoHelp, YourPrayerPower, YouHealedBeings, RejectedJoinOffer, PrayedForBlessing, PrayedForQuest, PrayedForItem, PrayedForHealth, PrayedForHolyArtifact, NotEnoughPrayerPower, NowHunting, NotHunting, InvalidGodName, InvalidAltarSign, BlessingsHelp, CursesHelp, BuildAltarNotAllowed, CreateGodNotAllowed, SacrificeHelp, QuestTargetHelp, QuestTargetRange, BelieverPower, AttackingHolyLandsHelp, DefendingHolyLandsHelp, ContestedLandKillsStatus, ContestedLandDesecrationStatus, GodToBelieversDefendHolyLandSuccess, GodToBelieversDefendHolyLandFailed, GodToBelieversAttackHolyLandSuccess, GodToBelieversAttackHolyLandFailed, GodToBelieversAttackStarted, GodToBelieversAttackProgress, GodToBelieversAttackStatus, GodToBelieverQuestionHelp, GodToBelieverNoQuestion, GodToBelieversNewPlayerAccepted, GodToPriestPriestAccepted, GodToPriestBlessedPlayerSet, GodToPriestBlessedPlayerUnset, GodToPriestCursedPlayerSet, GodToPriestCursedPlayerUnset, GodToPriestEatFoodType, GodToPriestNotEatFoodType, GodToPriestSlayMobType, GodToPriestNotSlayMobType, GodToPriestUseBible, GodToPriestUseProphecies, GodToPriestBelieverKilledDeclareWarQuestion, GodToBelieversBlessedPlayerSet, GodToBelieversBlessedPlayerUnset, GodToBelieversCursedPlayerSet, GodToBelieversCursedPlayerUnset, GodToBelieverOfferPriest, GodToBelieverPraying, GodToBelieverMarriageProposal, GodToBelieverAcceptedMarriageProposal, GodToBelieverAcceptedYourMarriageProposal, GodToBelieverMarriageTokenPickup, GodToBelieverMarriagePartnerTokenPickup, GodToBelieverMarried, GodToBelieverMarriedCouple, GodToBelieversSlayDragonQuestStarted, GodToBelieversSlayDragonQuestProgress, GodToBelieversSlayDragonQuestStatus, GodToBelieversSlayDragonQuestCompleted, GodToBelieversSlayDragonQuestFailed, GodToBelieversSlayQuestStarted, GodToBelieversSlayQuestProgress, GodToBelieversSlayQuestStatus, GodToBelieversSlayQuestCompleted, GodToBelieversSlayQuestFailed, GodToBelieversConvertQuestStarted, GodToBelieversConvertQuestProgress, GodToBelieversConvertQuestStatus, GodToBelieversConvertQuestCompleted, GodToBelieversConvertQuestFailed, GodToBelieversBuildAltarsQuestStarted, GodToBelieversBuildAltarsQuestProgress, GodToBelieversBuildAltarsQuestStatus, GodToBelieversBuildAltarsQuestCompleted, GodToBelieversBuildAltarsQuestFailed, GodToBelieversBuildTowerQuestStarted, GodToBelieversBuildTowerQuestProgress, GodToBelieversBuildTowerQuestStatus, GodToBelieversBuildTowerQuestCompleted, GodToBelieversBuildTowerQuestFailed, GodToBelieversSacrificeQuestStarted, GodToBelieversSacrificeQuestProgress, GodToBelieversSacrificeQuestStatus, GodToBelieversSacrificeQuestCompleted, GodToBelieversSacrificeQuestFailed, GodToBelieversHolyFeastQuestStarted, GodToBelieversHolyFeastQuestProgress, GodToBelieversHolyFeastQuestStatus, GodToBelieversHolyFeastQuestCompleted, GodToBelieversHolyFeastQuestFailed, GodToBelieversHolyBattleQuestStarted, GodToBelieversHolyBattleQuestProgress, GodToBelieversHolyBattleQuestStatus, GodToBelieversHolyBattleQuestCompleted, GodToBelieversHolyBattleQuestFailed, GodToBelieversGiveItemsQuestStarted, GodToBelieversGiveBiblesQuestProgress, GodToBelieversGiveItemsQuestStatus, GodToBelieversGiveItemsQuestCompleted, GodToBelieversGiveItemsQuestFailed, GodToBelieversBurnBiblesQuestStarted, GodToBelieversBurnBiblesQuestProgress, GodToBelieversBurnBiblesQuestStatus, GodToBelieversBurnBiblesQuestCompleted, GodToBelieversBurnBiblesQuestFailed, GodToBelieversCrusadeQuestStarted, GodToBelieversCrusadeQuestProgress, GodToBelieversCrusadeQuestStatus, GodToBelieversCrusadeQuestCompleted, GodToBelieversCrusadeQuestFailed, GodToBelieversPVPRevengeQuestStarted, GodToBelieversPVPRevengeQuestProgress, GodToBelieversPVPRevengeQuestStatus, GodToBelieversPVPRevengeQuestCompleted, GodToBelieversPVPRevengeQuestFailed, GodToBelieversPilgrimageQuestStarted, GodToBelieversPilgrimageQuestProgress, GodToBelieversPilgrimageQuestStatus, GodToBelieversPilgrimageQuestCompleted, GodToBelieversPilgrimageQuestFailed, GodToBelieversClaimHolyLandQuestStarted, GodToBelieversClaimHolyLandQuestProgress, GodToBelieversClaimHolyLandQuestStatus, GodToBelieversClaimHolyLandQuestCompleted, GodToBelieversClaimHolyLandQuestFailed, GodToBelieversGetHolyArtifactQuestStarted, GodToBelieversGetHolyArtifactQuestProgress, GodToBelieversGetHolyArtifactQuestHelp, GodToBelieversGetHolyArtifactQuestRange, GodToBelieversGetHolyArtifactQuestStatus, GodToBelieversGetHolyArtifactQuestCompleted, GodToBelieversGetHolyArtifactOtherQuestFailed, GodToBelieversGetHolyArtifactQuestFailed, GodToBelieversHolywarQuestStarted, GodToBelieversHolywarQuestProgress, GodToBelieversHolywarQuestHelp, GodToBelieversHolywarQuestRange, GodToBelieversHolywarQuestStatus, GodToBelieversHolywarQuestCompleted, GodToBelieversHolywarOtherQuestFailed, GodToBelieversHolywarQuestFailed, GodToBelieversRemovedPriest, GodToBelieverPriestRejected, GodToBelieverAltarBuilt, GodToBelieversPlayerJoinedReligion, GodToBelieversPlayerLeftReligion, GodToBelieverRandomExaltedSpeech, GodToBelieverRandomPleasedSpeech, GodToBelieverRandomNeutralSpeech, GodToBelieverRandomDispleasedSpeech, GodToBelieverRandomAngrySpeech, GodToBelieversLostBeliever, GodToPlayerBlessed, GodToPlayerCursed, GodToPlayerInvite, GodToPlayerAcceptedInvitation, GodToBelieverCursedAngry, GodToBelieverGoodSacrifice, GodToBelieverMehSacrifice, GodToBelieverBadSacrifice, GodToBelieverHolyFoodSacrifice, GodToBelieversPlayerCursed, GodToBelieversPlayerBlessed, GodToBelieverItemBlessing, GodToBelieverHolyArtifactBlessing, GodToBelieverHealthBlessing, GodToBelieverSmiteBlessing, GodToBelieversEatFoodBlessing, GodToBelieversNotEatFoodCursing, GodToBelieversSlayMobBlessing, GodToBelieversNotSlayMobCursing, GodToBelieversPriestAccepted, GodToBelieversAltarDestroyed, GodToBelieversAltarDestroyedByPlayer, GodToBelieversWar, GodToBelieversAlliance, GodToBelieversWarCancelled, GodToBelieversAllianceCancelled, GodToBelieversSetHome, GodToBelieversSacrificeItemType, GodToBelieversAllPropheciesFulfilled, GodToBelieversProphecyFulfilled, GodToBelieversJustMarried, ProphecyHeaderBibleText, UnholyMobWillBeSlainFutureProphecyBibleText, UnholyMobWillBeSlainPastProphecyBibleText, StormProphecyEffectPastBibleText, StormProphecyEffectFutureBibleText, HolyFoodRainProphecyEffectBibleText, DarknessProphecyEffectBibleText, HeathenWillBeSlainProphecyEffectBibleText, BelieverWillLeaveReligionProphecyBibleText, DragonBossWillBeSlainPastProphecyBibleText, DragonBossWillBeSlainFutureProphecyBibleText, DragonBossProphecyEffectPastBibleText, DragonBossProphecyEffectFutureBibleText, DefaultBibleText1, DefaultBibleText2, DefaultBibleText3, DefaultBibleText4, DefaultBibleText5, DefaultBibleText6, DefaultBibleText7;
	}

	private static LanguageManager instance;

	public static LanguageManager instance()
	{
		if (instance == null)
			instance = new LanguageManager();
		return instance;
	}

	private String								generalLanguageFileName	= null;
	private HashMap<String, FileConfiguration>	languageConfigs			= new HashMap<String, FileConfiguration>();
	private Random								random					= new Random();
	private int									amount;
	private String								playerName;
	private String								type;

	private LanguageManager()
	{
	}

	private void downloadLanguageFile(String fileName) throws IOException
	{
		BufferedInputStream in = new BufferedInputStream(new URL("https://raw.githubusercontent.com/DogOnFire/Gods/master/lang/" + fileName).openStream());

		FileOutputStream fos = new FileOutputStream(Gods.instance().getDataFolder() + "/lang/" + fileName);

		BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);

		byte[] data = new byte[1024];

		int x = 0;
		while ((x = in.read(data, 0, 1024)) >= 0)
		{
			bout.write(data, 0, x);
		}
		bout.close();

		in.close();
	}

	public int getAmount()
	{
		return this.amount;
	}

	public String getGodGenderName(GodManager.GodGender gender)
	{
		return this.languageConfigs.get(this.generalLanguageFileName).getString("GodGender." + gender.name());
	}

	public String getGodMoodName(GodManager.GodMood mood)
	{
		return this.languageConfigs.get(this.generalLanguageFileName).getString("GodMood." + mood.name());
	}

	public String getGodTypeName(GodManager.GodType type, String gender)
	{
		String typeName = this.languageConfigs.get(this.generalLanguageFileName).getString("GodTypes." + type.name());
		if (typeName == null)
		{
			typeName = "$Gender of Nothing";
		}
		return typeName.replace("$Gender", gender);
	}

	public String getInfoString(LANGUAGESTRING languageString, ChatColor defaultColor)
	{
		String text = this.languageConfigs.get(this.generalLanguageFileName).getString("Info." + languageString.name());

		if (text == null)
		{
			Gods.instance().logDebug("WARNING: No language string in " + this.generalLanguageFileName + " for the info type '" + languageString.name() + "'");
			return languageString.name() + " MISSING in " + this.generalLanguageFileName;
		}
		return parseString(text, defaultColor);
	}

	public String getItemTypeName(Material material)
	{
		FileConfiguration configuration = this.languageConfigs.get(this.generalLanguageFileName);
		if (configuration == null)
		{
			return null;
		}
		String itemTypeName = configuration.getString("Items." + material.name());
		if (itemTypeName == null)
		{
			Gods.instance().logDebug("WARNING: No language string in " + this.generalLanguageFileName + " for the item '" + material.name() + "'");
			return material.name();
		}
		return itemTypeName;
	}

	private String getLanguageFileForGod(String godName)
	{
		return GodManager.instance().getLanguageFileForGod(godName);
	}

	public String getLanguageString(String godName, LANGUAGESTRING type)
	{
		FileConfiguration languageConfig = this.languageConfigs.get(getLanguageFileForGod(godName));
		if (languageConfig == null)
		{
			GodManager.GodType godType = GodManager.instance().getDivineForceForGod(godName);

			Gods.instance().log("No languageConfig found for " + godName + " of type " + godType.name() + "!");
			return "MISSING LANGUAGEFILE " + type.name();
		}

		List<String> strings = languageConfig.getStringList(type.name());

		if (strings == null || strings.size() == 0)
		{
			Gods.instance().log("No language strings found for " + godName + "," + type.name() + "!");
			return type.name() + " MISSING in " + getLanguageFileForGod(godName);
		}

		String text = (String) strings.toArray()[this.random.nextInt(strings.size())];

		return parseString(text);
	}

	public String getLanguageStringForBook(String godName, LANGUAGESTRING type)
	{
		List<String> strings = this.languageConfigs.get(getLanguageFileForGod(godName)).getStringList(type.name());
		if (strings.size() == 0)
		{
			Gods.instance().log("No language strings found for " + type.name() + "!");
			return type.name() + " MISSING in " + getLanguageFileForGod(godName);
		}
		String text = (String) strings.toArray()[this.random.nextInt(strings.size())];

		return parseStringForBook(text);
	}

	public String getMobTypeName(EntityType type)
	{
		String mobTypeName = this.languageConfigs.get(this.generalLanguageFileName).getString("Mobs." + type.name());
		if (mobTypeName == null)
		{
			Gods.instance().logDebug("WARNING: No language string in " + this.generalLanguageFileName + " for the mob type '" + type.name() + "'");
			return type.name();
		}
		return mobTypeName;
	}

	public String getPlayerName()
	{
		return this.playerName;
	}

	public String getPriestAssignCommand(UUID playerName)
	{
		return "";
	}

	public String getPriestRemoveCommand(UUID playerName)
	{
		return "";
	}

	public String getType()
	{
		return this.type;
	}

	public void load()
	{
		this.generalLanguageFileName = (GodsConfiguration.instance().getLanguageIdentifier() + "_general.yml");

		Gods.instance().logDebug("generalFileName is " + this.generalLanguageFileName);
		Gods.instance().logDebug("GodsConfiguration.instance().language is " + GodsConfiguration.instance().getLanguageIdentifier());

		File directory = new File(Gods.instance().getDataFolder() + "/lang");
		if (!directory.exists())
		{
			System.out.println("Creating language file directory '/lang'...");

			boolean result = directory.mkdir();
			if (result)
			{
				Gods.instance().logDebug("Directory created");
			}
			else
			{
				Gods.instance().logDebug("Directory FAILED!");
				return;
			}
		}
		if (!loadLanguageFile(this.generalLanguageFileName))
		{
			Gods.instance().log("Could not load " + this.generalLanguageFileName + " from the /lang folder!");
			if (GodsConfiguration.instance().isDownloadLanguageFile())
			{
				Gods.instance().log("Downloading " + this.generalLanguageFileName + " from DogOnFire...");
				try
				{
					downloadLanguageFile(this.generalLanguageFileName);
				}
				catch (Exception ex)
				{
					Gods.instance().log("Could not download " + this.generalLanguageFileName + " language file from DogOnFire: " + ex.getMessage());
					return;
				}
				if (loadLanguageFile(this.generalLanguageFileName))
				{
					Gods.instance().logDebug(this.generalLanguageFileName + " loaded.");
				}
			}
			else
			{
				Gods.instance().log("Will NOT download from DogOnFire. Please place a valid language file in your /lang folder!");
			}
		}
		
		for (GodManager.GodType godType : GodManager.GodType.values())
		{
			for (GodManager.GodGender godGender : GodManager.GodGender.values())
			{
				if (godGender != GodManager.GodGender.None)
				{
					String fileName = GodsConfiguration.instance().getLanguageIdentifier() + "_" + godType.name().toLowerCase() + "_" + godGender.name().toLowerCase() + ".yml";
					if (!loadLanguageFile(fileName))
					{
						Gods.instance().log("Could not load language file " + fileName + " from the /lang folder!");
						Gods.instance().log("Downloading english files from doggycraft.dk...");
						try
						{
							downloadLanguageFile(fileName);
						}
						catch (Exception ex)
						{
							Gods.instance().log("Could not download language file " + fileName + " from bukkit: " + ex.getMessage());
							continue;
						}
						loadLanguageFile(fileName);
					}
					Gods.instance().log("Loaded " + fileName + ".");
				}
			}
		}
	}

	private boolean loadLanguageFile(String fileName)
	{
		File languageConfigFile = new File(Gods.instance().getDataFolder() + "/lang/" + fileName);
		if (!languageConfigFile.exists())
		{
			Gods.instance().log("Could not load " + Gods.instance().getDataFolder() + "/lang/" + fileName);
			return false;
		}
		FileConfiguration languageConfig = YamlConfiguration.loadConfiguration(languageConfigFile);

		this.languageConfigs.put(fileName, languageConfig);

		Gods.instance().log("Loaded " + languageConfig.getString("Version.Name") + " by " + languageConfig.getString("Version.Author") + " version " + languageConfig.getString("Version.Version"));

		return true;
	}

	public String parseString(String id)
	{
		String string = id;
		if (string.contains("$ServerName"))
		{
			string = string.replace("$ServerName", ChatColor.GOLD + GodsConfiguration.instance().getServerName() + ChatColor.WHITE + ChatColor.BOLD);
		}
		if (string.contains("$PlayerName"))
		{
			string = string.replace("$PlayerName", ChatColor.GOLD + this.playerName + ChatColor.WHITE + ChatColor.BOLD);
		}
		if (string.contains("$Amount"))
		{
			string = string.replace("$Amount", ChatColor.GOLD + String.valueOf(this.amount) + ChatColor.WHITE + ChatColor.BOLD);
		}
		if (string.contains("$Type"))
		{
			string = string.replace("$Type", ChatColor.GOLD + this.type + ChatColor.WHITE + ChatColor.BOLD);
		}
		return string;
	}

	public String parseString(String id, ChatColor defaultColor)
	{
		String string = id;
		if (string.contains("$ServerName"))
		{
			string = string.replace("$ServerName", ChatColor.GOLD + GodsConfiguration.instance().getServerName() + defaultColor);
		}
		if (string.contains("$PlayerName"))
		{
			string = string.replace("$PlayerName", ChatColor.GOLD + this.playerName + defaultColor);
		}
		if (string.contains("$Amount"))
		{
			string = string.replace("$Amount", ChatColor.GOLD + String.valueOf(this.amount) + defaultColor);
		}
		if (string.contains("$Type"))
		{
			string = string.replace("$Type", ChatColor.GOLD + this.type + defaultColor);
		}
		return string;
	}

	public String parseStringForBook(String id)
	{
		String string = id;
		if (string.contains("$ServerName"))
		{
			string = string.replace("$ServerName", GodsConfiguration.instance().getServerName());
		}
		if (string.contains("$PlayerName"))
		{
			string = string.replace("$PlayerName", this.playerName);
		}
		if (string.contains("$Amount"))
		{
			string = string.replace("$Amount", String.valueOf(this.amount));
		}
		if (string.contains("$Type"))
		{
			string = string.replace("$Type", this.type);
		}
		return string;
	}

	public void setAmount(int a)
	{
		this.amount = a;
	}

	public boolean setDefault()
	{
		return true;
	}

	public void setPlayerName(String name)
	{
		if (name == null)
		{
			Gods.instance().logDebug("WARNING: Setting null playername");
		}
		this.playerName = name;
	}

	public void setType(String t) throws Exception
	{
		if (t == null)
		{
			Gods.instance().logDebug("WARNING: Setting null type");
			throw new Exception("WARNING: Setting null type");
		}
		this.type = t;
	}
}
