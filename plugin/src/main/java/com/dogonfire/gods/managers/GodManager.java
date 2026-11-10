package com.dogonfire.gods.managers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.storage.DataFile;
import com.dogonfire.gods.tasks.TaskGiveHolyArtifact;
import com.dogonfire.gods.tasks.TaskGiveItem;
import com.dogonfire.gods.tasks.TaskGodSpeak;
import com.dogonfire.gods.tasks.TaskHealPlayer;
import com.dogonfire.gods.tasks.TaskSpawnHostileMobs;

public class GodManager
{
	public static enum GodGender {
		None, Male, Female;
	}

	public static enum GodMood {
		EXALTED, PLEASED, NEUTRAL, DISPLEASED, ANGRY;
	}

	public static enum GodRelation {
		LOVERS, MARRIED, ENEMIES, FRIENDS, BFF, ROOMMATES;
	}

	public static enum GodType {
		FROST, LOVE, EVIL, SEA, MOON, SUN, THUNDER, PARTY, WAR, WEREWOLVES, CREATURES, WISDOM, NATURE;
	}

	public class NewPriestComparator implements Comparator<Object>
	{
		public NewPriestComparator()
		{
		}

		@Override
		public int compare(Object object1, Object object2)
		{
			GodManager.PriestCandidate c1 = (GodManager.PriestCandidate) object1;
			GodManager.PriestCandidate c2 = (GodManager.PriestCandidate) object2;

			float power1 = ctx.believerManager().getBelieverPower(c1.believerId);
			float power2 = ctx.believerManager().getBelieverPower(c2.believerId);

			return Float.compare(power2, power1);
		}
	}

	public class PriestCandidate
	{
		public UUID believerId;

		PriestCandidate(UUID believerId)
		{
			this.believerId = believerId;
		}
	}

	private Random				random			= new Random();
	private List<String>		onlineGods		= new ArrayList<String>();
	private String				pattern			= "HH:mm:ss dd-MM-yyyy";
	DateFormat					formatter		= new SimpleDateFormat(this.pattern);

	private final GodsContext ctx;

	public GodManager(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	/** gods.yml; see {@link DataFile} */
	private DataFile godsFile()
	{
		return ctx.storage().gods();
	}

	private YamlConfiguration godsConfig()
	{
		return godsFile().yaml();
	}

	/**
	 * Sets godName + key in gods.yml. Does nothing for a null god name, so a lookup for a player without a god can't
	 * create a god called "null".
	 */
	private void setGodValue(String godName, String key, Object value)
	{
		if (godName == null)
		{
			ctx.plugin().logDebug("Not writing " + key + " for a null god name");
			return;
		}
		godsConfig().set(godName + key, value);
	}

	/**
	 * Runs the console command configured in Settings.PriestAssignCommand or Settings.PriestRemoveCommand, with
	 * $PlayerName replaced by the player's name. Blank commands are skipped.
	 */
	private void dispatchPriestCommand(String command, UUID playerId)
	{
		if (command == null || command.trim().isEmpty())
		{
			return;
		}

		command = command.trim();
		if (command.startsWith("/"))
		{
			command = command.substring(1);
		}

		if (command.contains("$PlayerName"))
		{
			String playerName = ctx.plugin().getServer().getOfflinePlayer(playerId).getName();
			if (playerName == null)
			{
				ctx.plugin().log("Not running priest command '" + command + "': no name known for " + playerId);
				return;
			}
			command = command.replace("$PlayerName", playerName);
		}

		ctx.plugin().getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
	}

	public boolean addAltar(Player player, String godName, Location location)
	{
		if (addBeliefByAltar(player, godName, location, true))
		{
			ctx.languageManager().setPlayerName(player.getName());

			GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverAltarBuilt, 2 + this.random.nextInt(30));

			return true;
		}

		return false;
	}

	private boolean addBelief(Player player, String godName, boolean allowChangeGod)
	{
		String oldGodName = ctx.believerManager().getGodForBeliever(player.getUniqueId());

		if (godName == null)
		{
			ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.InvalidGodName, ChatColor.RED, 0, "", 1);
			return false;
		}

		if (oldGodName != null && !oldGodName.equals(godName))
		{
			if (!allowChangeGod)
			{
				ctx.believerManager().setChangingGod(player.getUniqueId());

				ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.ConfirmChangeToOtherReligion, ChatColor.YELLOW, 0, oldGodName, 1);
				return false;
			}

			if (ctx.believerManager().hasRecentGodChange(player.getUniqueId()))
			{
				ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.CannotChangeGodSoSoon, ChatColor.RED, 0, "", 1);
				return false;
			}

			ctx.believerManager().clearChangingGod(player.getUniqueId());
		}

		if (!ctx.believerManager().addPrayer(player.getUniqueId(), godName))
		{
			int timeUntilCanPray = ctx.believerManager().getTimeUntilCanPray(player.getUniqueId());

			ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.CannotPraySoSoon, ChatColor.RED, timeUntilCanPray, "", 1);
			return false;
		}

		if (oldGodName != null && !oldGodName.equals(godName))
		{
			if (isPriestForGod(player.getUniqueId(), oldGodName))
			{
				removePriest(oldGodName, player.getUniqueId());
			}

			ctx.languageManager().setPlayerName(player.getName());

			godSayToBelievers(oldGodName, LanguageManager.LANGUAGESTRING.GodToBelieversPlayerLeftReligion, 2 + this.random.nextInt(20));

			ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.YouLeftReligion, ChatColor.RED, 0, oldGodName, 20);

			GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPlayerJoinedReligion, player.getUniqueId());

			ctx.believerManager().clearPrayerPower(player.getUniqueId());
		}
		else
		{
			Material foodType = getHolyFoodTypeForGod(godName);

			try
			{
				ctx.languageManager().setType(ctx.languageManager().getItemTypeName(foodType));
			}
			catch (Exception ex)
			{
				ctx.plugin().logDebug(ex.getMessage());
			}

			giveItem(godName, player, foodType, false);

			ctx.believerManager().increasePrayerPower(player.getUniqueId(), 1);
		}

		if (oldGodName == null || !oldGodName.equals(godName))
		{
			if (ctx.settings().marriage().enabled())
			{
				ctx.marriageManager().divorce(player.getUniqueId());
			}
			
			ctx.questManager().handleJoinReligion(player.getName(), godName);

			if (ctx.settings().bibles().enabled() && ctx.settings().bibles().giveOnJoin())
			{
				ctx.holyBookManager().giveBible(godName, player.getName());
			}
		}

		return true;
	}

	public void addBeliefAndRewardBelievers(String godName)
	{
		for (UUID playerId : ctx.believerManager().getBelieversForGod(godName))
		{
			Player player = ctx.plugin().getServer().getPlayer(playerId);

			if (player == null)
			{
				continue;
			}

			ctx.believerManager().incPrayer(player.getUniqueId(), godName);

			List<ItemStack> rewards = ctx.questManager().getRewardsForQuestCompletion(godName);

			for (ItemStack items : rewards)
			{
				giveItem(godName, player, items.getType(), false);
			}
		}
	}

	private boolean addBeliefByAltar(Player player, String godName, Location prayerLocation, boolean allowChangeGod)
	{
		if (!godExist(godName))
		{
			if (!player.isOp() && (!ctx.permissionsManager().hasPermission(player, "gods.god.create")))
			{
				ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.CreateGodNotAllowed, ChatColor.RED, 0, "", 20);
				return false;
			}

			Block altarBlock = ctx.altarManager().getAltarBlockFromSign(prayerLocation.getBlock());

			GodGender godGender = ctx.altarManager().getGodGenderFromAltarBlock(altarBlock);

			ctx.plugin().logDebug("Altar is " + altarBlock.getType().name());

			GodType godType = ctx.altarManager().getGodTypeForAltarBlockType(altarBlock.getType());

			ctx.plugin().logDebug("God divine force is " + godType);

			createGod(godName, player.getLocation(), godGender, godType);

			if (ctx.settings().gods().broadcastNewGods())
			{
				ctx.plugin().getServer().broadcastMessage(ChatColor.WHITE + player.getName() + ChatColor.AQUA + " started to believe in the " + ctx.languageManager().getGodGenderName(getGenderForGod(godName)) + " " + ChatColor.GOLD + godName);
			}

			ctx.plugin().log(player.getName() + " created new god " + godName);
		}

		return addBelief(player, godName, allowChangeGod);
	}

	public void addMoodForGod(String godName, float mood)
	{
		float godMood = (float) godsConfig().getDouble(godName + ".Mood");

		godMood += mood;
		if (godMood > 100.0F)
		{
			godMood = 100.0F;
		}
		else if (godMood < -100.0F)
		{
			godMood = -100.0F;
		}
		setGodValue(godName, ".Mood", Float.valueOf(godMood));

		godsFile().markDirty();
	}

	public boolean assignPriest(String godName, UUID playerId)
	{
		setGodValue(godName, ".PendingPriest", null);
		ctx.believerManager().clearPendingPriest(playerId);

		Set<UUID> believers = ctx.believerManager().getBelieversForGod(godName);
		if (believers.contains(playerId))
		{
			List<String> priests = godsConfig().getStringList(godName + ".Priests");

			if (priests.contains(playerId.toString()))
			{
				ctx.plugin().log(playerId.toString() + " is already a priest of " + godName);
			}
			else
			{
				priests.add(playerId.toString());
				dispatchPriestCommand(ctx.settings().priests().assignCommand(), playerId);
			}

			setGodValue(godName, ".Priests", priests);
			ctx.chatManager().refresh(playerId);

			setGodValue(godName, ".PendingPriest", null);
			setGodValue(godName, ".PendingPriestTime", null);

			ctx.believerManager().setLastPrayerDate(playerId);

			godsFile().markDirty();
			return true;
		}
		else
		{
			return false;
		}
	}

	public void believerAccept(UUID believerId)
	{
		String godName = ctx.believerManager().getGodForBeliever(believerId);

		Player player = ctx.plugin().getServer().getPlayer(believerId);
		if (player == null)
		{
			ctx.plugin().logDebug("believerAccept(): player is null for " + believerId);
			return;
		}

		ctx.languageManager().setPlayerName(player.getName());
		if (ctx.settings().marriage().enabled())
		{
			UUID pendingMarriagePartner = ctx.marriageManager().getProposal(believerId);

			if (pendingMarriagePartner != null)
			{
				ctx.plugin().log(player.getName() + " accepted the proposal to marry " + pendingMarriagePartner);

				ctx.marriageManager().handleAcceptProposal(believerId, pendingMarriagePartner, godName);

				return;
			}
		}
	
		String pendingGodInvitation = ctx.believerManager().getInvitation(believerId);
		if (pendingGodInvitation != null)
		{
			ctx.plugin().logDebug("pendingGodInvitation is " + pendingGodInvitation);
			if (addBelief(player, pendingGodInvitation, true))
			{
				ctx.believerManager().clearInvitation(believerId);

				ctx.plugin().log(player.getName() + " accepted the invitation to join " + pendingGodInvitation);

				GodSay(pendingGodInvitation, player, LanguageManager.LANGUAGESTRING.GodToPlayerAcceptedInvitation, 2 + this.random.nextInt(40));
				GodSayToBelieversExcept(pendingGodInvitation, LanguageManager.LANGUAGESTRING.GodToBelieversNewPlayerAccepted, player.getUniqueId());
			}
			else
			{
				ctx.plugin().log(player.getName() + " could NOT accept the invitation to join " + pendingGodInvitation);
			}
			return;
		}

		if (godName == null)
		{
			ctx.plugin().logDebug(player.getName() + " has no god and nothing to accept");
			return;
		}

		UUID pendingPriest = getPendingPriest(godName);

		if (pendingPriest != null)
		{
			if (pendingPriest.equals(believerId))
			{
				assignPriest(godName, believerId);
				godsFile().markDirty();

				ctx.plugin().log(player.getName() + " accepted the offer from " + godName + " to be priest");

				ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.InviteHelp, ChatColor.AQUA, ChatColor.WHITE + "/gods invite <playername>", ChatColor.WHITE + "/gods invite <playername>", 100);
				ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.FollowersHelp, ChatColor.AQUA, ChatColor.WHITE + "/gods followers", ChatColor.WHITE + "/gods followers", 200);
				ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.DescriptionHelp, ChatColor.AQUA, ChatColor.WHITE + "/gods desc", ChatColor.WHITE + "/gods desc", 300);

				if (ctx.settings().holyArtifacts().enabled())
				{
					ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.AttackHelp, ChatColor.AQUA, ChatColor.WHITE + "/gods startattack", ChatColor.WHITE + "/gods startattack", 300);
				}
				try
				{
					GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToPriestPriestAccepted, 2 + this.random.nextInt(40));
					GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPriestAccepted, player.getUniqueId());
				}
				catch (Exception ex)
				{
					ctx.plugin().log("ERROR: Could not say GodToPriestPriestAccepted text! " + ex.getMessage());
				}
				return;
			}
		}

		ctx.plugin().logDebug(player.getDisplayName() + " did not have anything to accepted from " + godName);
		GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverNoQuestion, 2 + this.random.nextInt(20));
	}

	public boolean believerLeaveGod(UUID believerId)
	{
		String godName = ctx.believerManager().getGodForBeliever(believerId);
		if (godName == null)
		{
			return false;
		}

		if (isPriestForGod(believerId, godName))
		{
			removePriest(godName, believerId);
		}
		ctx.believerManager().believerLeave(godName, believerId);

		ctx.languageManager().setPlayerName(ctx.plugin().getServer().getPlayer(believerId).getDisplayName());

		if (ctx.settings().marriage().enabled())
		{
			ctx.marriageManager().divorce(believerId);
		}

		ctx.believerManager().clearPrayerPower(believerId);

		godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPlayerLeftReligion, 2 + this.random.nextInt(20));

		return true;
	}

	public void believerReject(UUID believerId)
	{
		String godName = ctx.believerManager().getGodForBeliever(believerId);
		Player player = ctx.plugin().getServer().getPlayer(believerId);
		if (player == null)
		{
			return;
		}

		ctx.languageManager().setPlayerName(player.getName());

		String pendingGodInvitation = ctx.believerManager().getInvitation(believerId);
		if (pendingGodInvitation != null)
		{
			ctx.believerManager().clearInvitation(believerId);

			ctx.plugin().log(player.getName() + " rejected the invitation to join " + pendingGodInvitation);

			ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.RejectedJoinOffer, ChatColor.RED, 0, pendingGodInvitation, 20);

			return;
		}

		if (godName == null)
		{
			return;
		}

		UUID pendingPriest = getPendingPriest(godName);

		if (pendingPriest == null)
		{
			GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverNoQuestion, 2 + this.random.nextInt(20));
			return;
		}

		if (pendingPriest.equals(believerId))
		{
			setGodValue(godName, ".PendingPriest", null);

			ctx.believerManager().clearPendingPriest(believerId);

			ctx.plugin().log(player.getName() + " rejected the offer from " + godName + " to be priest");

			GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverPriestRejected, 2 + this.random.nextInt(20));

			godsFile().markDirty();
		}
	}

	public boolean blessPlayer(String godName, UUID playerId, float godPower)
	{
		Player player = ctx.plugin().getServer().getPlayer(playerId);

		if (player == null)
		{
			return false;
		}

		if (ctx.believerManager().hasRecentBlessing(playerId))
		{
			return false;
		}

		int blessingType = 0;
		int t = 0;

		float blessingPower = 1.0F + godPower / 100.0F;

		do
		{
			blessingType = this.random.nextInt(5);
			t++;
		}
		while ((t < 50) && (((blessingType == 0) && (!ctx.settings().blessings().fastDigging())) || ((blessingType == 1) && (!ctx.settings().blessings().heal())) || ((blessingType == 2) && (!ctx.settings().blessings().regeneration()))
				|| ((blessingType == 3) && (!ctx.settings().blessings().speed())) || ((blessingType == 4) && (!ctx.settings().blessings().increaseDamage()))));

		switch (blessingType)
		{
		case 0:
			player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, (int) (300.0F * blessingPower), 1));
			break;
		case 1:
			player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, (int) (300.0F * blessingPower), 1));
			break;
		case 2:
			player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, (int) (300.0F * blessingPower), 1));
			break;
		case 3:
			player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (300.0F * blessingPower), 1));
			break;
		case 4:
			player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, (int) (300.0F * blessingPower), 1));
		}

		ctx.believerManager().setBlessingTime(player.getUniqueId());

		return true;
	}

	public void blessPlayerWithHolyArtifact(String godName, Player player)
	{
		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return;
		}
		giveHolyArtifact(godName, getDivineForceForGod(godName), player, true);
	}

	public ItemStack blessPlayerWithItem(String godName, Player player)
	{
		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return null;
		}
		ItemStack item = getItemNeed(godName, player);
		if (item != null)
		{
			giveItem(godName, player, item.getType(), true);
		}
		return item;
	}

	public void clearContestedHolyLandForGod(String godName)
	{
		setGodValue(godName, ".ContestedLand", null);
		setGodValue(godName, ".ContestedKills", null);
		setGodValue(godName, ".ContestedDesecration", null);

		godsFile().markDirty();
	}

	public void createGod(String godName, Location location, GodGender godGender, GodType godType)
	{
		Date thisDate = new Date();

		DateFormat formatter = new SimpleDateFormat(this.pattern);

		setHomeForGod(godName, location);
		setGenderForGod(godName, godGender);
		setDivineForceForGod(godName, godType);
		setPrivateAccess(godName, ctx.settings().gods().privateByDefault());

		setGodValue(godName, ".Created", formatter.format(thisDate));

		godsFile().markDirty();
	}

	public boolean cursePlayer(String godName, UUID playerId, float godPower)
	{
		Player player = ctx.plugin().getServer().getPlayer(playerId);

		if (player == null)
		{
			return false;
		}

		if (ctx.believerManager().hasRecentCursing(playerId))
		{
			return false;
		}

		int curseType = 0;
		int t = 0;

		do
		{
			curseType = this.random.nextInt(7);
			t++;
		}
		while ((t < 50) && (((curseType == 5) && (!ctx.settings().curses().lightning())) || ((curseType == 6) && (!ctx.settings().curses().mobs()))));

		float cursePower = 1.0F + godPower / 100.0F;

		switch (curseType)
		{
		case 0:
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int) (200.0F * cursePower), 1));
			break;
		case 1:
			player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, (int) (200.0F * cursePower), 1));
			break;
		case 2:
			player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, (int) (200.0F * cursePower), 1));
			break;
		case 3:
			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int) (200.0F * cursePower), 1));
			break;
		case 4:
			player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, (int) (200.0F * cursePower), 1));
			break;
		case 5:
			strikePlayerWithLightning(playerId, 1);
			break;
		case 6:
			strikePlayerWithMobs(godName, playerId, godPower);
		}

		ctx.believerManager().setCursingTime(player.getUniqueId());

		return true;
	}

	public String formatGodName(String godName)
	{
		return godName.substring(0, 1).toUpperCase() + godName.substring(1).toLowerCase();
	}

	private String generateHolyMobTypeForGod()
	{
		EntityType[] holyMobTypes = new EntityType[] { EntityType.CHICKEN, EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.RABBIT, EntityType.HORSE, EntityType.BAT };

		return holyMobTypes[this.random.nextInt(holyMobTypes.length)].name();
	}

	private String generateUnholyMobTypeForGod()
	{
		EntityType[] unholyMobTypes = new EntityType[] { EntityType.CHICKEN, EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.RABBIT, EntityType.HORSE, EntityType.BAT, EntityType.WOLF, EntityType.SKELETON, EntityType.ZOMBIE };

		return unholyMobTypes[this.random.nextInt(unholyMobTypes.length)].name();
	}

	/**
	 * @return the entity type with this name, or null if the name is missing or no longer exists (e.g. PIG_ZOMBIE)
	 */
	private EntityType parseEntityType(String mobTypeString)
	{
		if (mobTypeString == null)
		{
			return null;
		}
		try
		{
			return EntityType.valueOf(mobTypeString.toUpperCase());
		}
		catch (IllegalArgumentException ex)
		{
			return null;
		}
	}

	/**
	 * @return the UUID in this string, or null if it is missing or not a UUID (e.g. a player name from Gods 0.9.x)
	 */
	private static UUID parseUUID(String uuidString)
	{
		if (uuidString == null)
		{
			return null;
		}
		try
		{
			return UUID.fromString(uuidString);
		}
		catch (IllegalArgumentException ex)
		{
			return null;
		}
	}

	public Set<String> getAllGods()
	{
		Set<String> gods = godsConfig().getKeys(false);

		return gods;
	}

	public List<String> getAllianceRelations(String godName)
	{
		return godsConfig().getStringList(godName + ".Allies");
	}

	public float getAngryModifierForGod(String godName)
	{
		return -1.0F;
	}

	private Material getAxeBlessing(String godName)
	{
		if (getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel3Items())
		{
			return Material.DIAMOND_AXE;
		}
		if (getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel2Items())
		{
			return Material.IRON_AXE;
		}
		if (getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel1Items())
		{
			return Material.STONE_AXE;
		}
		return Material.WOODEN_AXE;
	}

	public UUID getBlessedPlayerForGod(String godName)
	{
		Date lastBlessedDate = getLastBlessedTimeForGod(godName);
		if (lastBlessedDate == null)
		{
			return null;
		}
		Date thisDate = new Date();

		// Blessing.MaxBlessingTime is in seconds (Cursing.MaxCursingTime is in minutes)
		long diffSeconds = (thisDate.getTime() - lastBlessedDate.getTime()) / 1000L;
		UUID blessedPlayer = parseUUID(godsConfig().getString(godName + ".BlessedPlayer"));
		if (blessedPlayer == null || diffSeconds > ctx.settings().blessings().blessedSeconds())
		{
			setBlessedPlayerForGod(godName, null);

			return null;
		}
		return blessedPlayer;
	}

	public ChatColor getColorForGod(String godName)
	{
		GodType godType = getDivineForceForGod(godName);

		return getColorForGodType(godType);
	}

	public ChatColor getColorForGodType(GodType godType)
	{
		ChatColor color = ChatColor.WHITE;
		if (godType == null)
		{
			return ChatColor.WHITE;
		}
		switch (godType)
		{
		case THUNDER:
			color = ChatColor.DARK_GRAY;
			break;
		case EVIL:
			color = ChatColor.RED;
			break;
		case WISDOM:
			color = ChatColor.DARK_GREEN;
			break;
		case FROST:
			color = ChatColor.BLACK;
			break;
		case SUN:
			color = ChatColor.DARK_RED;
			break;
		case SEA:
			color = ChatColor.DARK_AQUA;
			break;
		case LOVE:
			color = ChatColor.BLUE;
			break;
		case MOON:
			color = ChatColor.GRAY;
			break;
		case WAR:
			color = ChatColor.GREEN;
			break;
		case NATURE:
			color = ChatColor.YELLOW;
			break;
		case CREATURES:
			color = ChatColor.DARK_BLUE;
			break;
		case WEREWOLVES:
			color = ChatColor.GRAY;
			break;
		default:
			break;
		}
		return color;
	}

	public Location getContestedHolyLandAttackLocationForGod(String godName)
	{
		int x = godsConfig().getInt(godName + ".ContestedLand" + ".X");
		int y = godsConfig().getInt(godName + ".ContestedLand" + ".Y");
		int z = godsConfig().getInt(godName + ".ContestedLand" + ".Z");
		String worldName = godsConfig().getString(godName + ".ContestedLand" + ".World");

		return new Location(ctx.plugin().getServer().getWorld(worldName), x, y, z);
	}

	/**
	 * The region hash of the holy land godName is attacking, or null. An attack whose land no longer has
	 * godName as attacker (it was settled, abandoned or taken over, or comes from an old data file) is cleared.
	 */
	public Long getContestedHolyLandForGod(String godName)
	{
		if (godName == null || !godsConfig().contains(godName + ".ContestedLand.World"))
		{
			return null;
		}
		Location location = getContestedHolyLandAttackLocationForGod(godName);
		if (!ctx.holyLandManager().isAttackingHolyLand(godName, location))
		{
			if (godsConfig().contains(godName + ".ContestedLand"))
			{
				clearContestedHolyLandForGod(godName);
			}
			return null;
		}
		return ctx.holyLandManager().hashLocation(location);
	}

	public Player getCursedPlayerForGod(String godName)
	{
		Date lastCursedDate = getLastCursingTimeForGod(godName);
		if (lastCursedDate == null)
		{
			return null;
		}
		Date thisDate = new Date();

		// Cursing.MaxCursingTime is in minutes (Blessing.MaxBlessingTime is in seconds)
		long diffMinutes = (thisDate.getTime() - lastCursedDate.getTime()) / 60000L;
		UUID cursedPlayer = parseUUID(godsConfig().getString(godName + ".CursedPlayer"));
		if (cursedPlayer == null || diffMinutes > ctx.settings().curses().cursedMinutes())
		{
			setCursedPlayerForGod(godName, null);

			return null;
		}

		return ctx.plugin().getServer().getPlayer(cursedPlayer);
	}

	/**
	 * @return the god's type, or null for a null name or a god that doesn't exist. A god with a missing or unknown
	 *         type gets a random one.
	 */
	public GodType getDivineForceForGod(String godName)
	{
		if (godName == null || !godsConfig().isConfigurationSection(godName))
		{
			return null;
		}

		String typeName = godsConfig().getString(godName + ".DivineForce");
		try
		{
			return GodType.valueOf(typeName);
		}
		catch (Exception ex)
		{
			ctx.plugin().log("Could not parse GodType " + typeName + " for the god '" + godName + "'. Assigning a random GodType.");
			GodType type;
			do
			{
				type = GodType.values()[this.random.nextInt(GodType.values().length)];
			}
			while (type == GodType.WEREWOLVES);
			setDivineForceForGod(godName, type);
			return type;
		}
	}

	public Material getHolyFoodTypeForGod(String godName)
	{
		String foodTypeString = godsConfig().getString(godName + ".EatFoodType");
		
		Material foodType = Material.AIR;
		
		if (foodTypeString == null)
		{
			int r1 = this.random.nextInt(7);
			switch (r1)
			{
			case 0:
				foodType = Material.APPLE;
				break;
			case 1:
				foodType = Material.BREAD;
				break;
			case 2:
				foodType = Material.COOKED_SALMON;
				break;
			case 3:
				foodType = Material.MELON_SLICE;
				break;
			case 4:
				foodType = Material.COOKED_BEEF;
				break;
			case 5:
				foodType = Material.COOKED_PORKCHOP;
				break;
			case 6:
			default: 
				foodType = Material.CARROT;
					
			}
			foodTypeString = foodType.name();

			setGodValue(godName, ".EatFoodType", foodTypeString);

			godsFile().markDirty();
		}
		else
		{
			foodType = Material.getMaterial(foodTypeString);
		}
		
		if(foodType == null)
		{
			foodType = Material.CARROT;
		}
		
		return foodType;
	}

	public float getExactMoodForGod(String godName)
	{
		return (float) godsConfig().getDouble(godName + ".Mood");
	}

	public float getFalloffModifierForGod(String godName)
	{
		Random moodRandom = new Random(getSeedForGod(godName));

		float baseFalloff = (1 + moodRandom.nextInt(40)) / 20.0F;

		double falloffValue = -ctx.settings().gods().moodFalloff() * (1.0F + baseFalloff * ctx.believerManager().getOnlineBelieversForGod(godName).size()) * (1.0D + Math.sin(System.currentTimeMillis() / 1500000.0F));

		ctx.plugin().logDebug(godName + " mood falloff is " + falloffValue);

		return (float) falloffValue;
	}

	private Material getFoodBlessing(String godName)
	{
		return getHolyFoodTypeForGod(godName);
	}

	public GodGender getGenderForGod(String godName)
	{
		String genderString = godsConfig().getString(godName + ".Gender");
		GodGender godGender = GodGender.None;

		if (genderString != null)
		{
			try
			{
				godGender = GodGender.valueOf(genderString);
			}
			catch (Exception ex)
			{
				godGender = GodGender.None;
			}
		}

		return godGender;
	}

	public String getGodDescription(String godName)
	{
		String description = godsConfig().getString(godName + ".Description");
		if (description == null)
		{
			description = new String("No description :/");
		}
		return description;
	}

	public boolean getGodMobDamage(String godName)
	{
		return godsConfig().getBoolean(godName + ".MobDamage", ctx.settings().holyLand().defaultMobDamage());
	}

	public boolean getGodMobSpawning(String godName)
	{
		return godsConfig().getBoolean(godName + ".MobSpawning");
	}

	public float getGodPower(String godName)
	{
		float godPower = 0.0F;
		int minGodPower = 0;

		String name = godsConfig().getString(godName);

		if (name == null)
		{
			return 0.0F;
		}

		Set<UUID> believers = ctx.believerManager().getBelieversForGod(godName);

		if (ctx.settings().gods().whitelist())
		{
			minGodPower = (int) ctx.whitelistManager().getMinGodPower(godName);
		}

		for (UUID believerId : believers)
		{
			float believerPower = ctx.believerManager().getBelieverPower(believerId);

			godPower += believerPower;
		}
		if (godPower < minGodPower)
		{
			godPower = minGodPower;
		}
		return godPower;
	}

	public boolean getGodPvP(String godName)
	{
		return godsConfig().getBoolean(godName + ".PvP", ctx.settings().holyLand().defaultPvp());
	}

	public double getHealthNeed(String godName, Player player)
	{
		return player.getAttribute(Attribute.MAX_HEALTH).getValue() - player.getHealth();
	}

	private Material getHoeBlessing(String godName)
	{
		if (getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel3Items())
		{
			return Material.DIAMOND_HOE;
		}
		if (getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel2Items())
		{
			return Material.IRON_HOE;
		}
		if (getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel1Items())
		{
			return Material.STONE_HOE;
		}
		return Material.WOODEN_HOE;
	}

	public EntityType getHolyMobTypeForGod(String godName)
	{
		EntityType mobType = parseEntityType(godsConfig().getString(godName + ".NotSlayMobType"));
		if (mobType == null)
		{
			String unholyMobTypeString = getUnholyMobTypeForGod(godName).name();
			String mobTypeString;
			do
			{
				mobTypeString = generateHolyMobTypeForGod();
			}
			while (mobTypeString.equals(unholyMobTypeString));

			setGodValue(godName, ".NotSlayMobType", mobTypeString);
			godsFile().markDirty();

			mobType = EntityType.valueOf(mobTypeString);
		}
		return mobType;
	}

	public Location getHomeForGod(String godName)
	{
		Location location = new Location(null, 0.0D, 0.0D, 0.0D);

		String worldName = godsConfig().getString(godName + ".Home.World");
		if (worldName == null)
		{
			return null;
		}
		location.setWorld(ctx.plugin().getServer().getWorld(worldName));

		location.setX(godsConfig().getDouble(godName + ".Home.X"));
		location.setY(godsConfig().getDouble(godName + ".Home.Y"));
		location.setZ(godsConfig().getDouble(godName + ".Home.Z"));

		return location;
	}

	private ItemStack getItemNeed(String godName, Player player)
	{
		if (!hasFood(player, godName))
		{
			return new ItemStack(getFoodBlessing(godName));
		}
		if (!hasPickAxe(player))
		{
			return new ItemStack(getPickAxeBlessing(godName));
		}
		if (!hasSword(player))
		{
			return new ItemStack(getSwordBlessing(godName));
		}
		if (!hasSpade(player))
		{
			return new ItemStack(getSpadeBlessing(godName));
		}
		if (!hasAxe(player))
		{
			return new ItemStack(getAxeBlessing(godName));
		}
		if (!hasHoe(player))
		{
			return new ItemStack(getHoeBlessing(godName));
		}
		return null;
	}

	/**
	 * @return the language file the god speaks from, e.g. english_sun_male.yml, built from Settings.Language and the
	 *         god's type and gender. A god without a gender always gets the same one, picked from its seed.
	 */
	public String getLanguageFileForGod(String godName)
	{
		GodType godType = getDivineForceForGod(godName);
		GodGender godGender;
		if (godType == null)
		{
			// No such god
			godType = GodType.SUN;
			godGender = GodGender.Male;
		}
		else
		{
			godGender = getGenderForGod(godName);
			if (godGender == GodGender.None)
			{
				godGender = (getSeedForGod(godName) & 1L) == 0L ? GodGender.Female : GodGender.Male;
			}
		}

		return ctx.settings().general().language() + "_" + godType.name().toLowerCase() + "_" + godGender.name().toLowerCase() + ".yml";
	}

	public Date getLastBlessedTimeForGod(String godName)
	{
		String lastBlessedString = godsConfig().getString(godName + ".BlessedTime");
		if (lastBlessedString == null)
		{
			return null;
		}
		DateFormat formatter = new SimpleDateFormat(this.pattern);
		Date lastBlessedDate = null;
		try
		{
			lastBlessedDate = formatter.parse(lastBlessedString);
		}
		catch (Exception ex)
		{
			lastBlessedDate = new Date();
			lastBlessedDate.setTime(0L);
		}
		return lastBlessedDate;
	}

	public Date getLastCursingTimeForGod(String godName)
	{
		String lastCursedString = godsConfig().getString(godName + ".CursedTime");
		if (lastCursedString == null)
		{
			return null;
		}
		DateFormat formatter = new SimpleDateFormat(this.pattern);
		Date lastCursedDate = null;
		try
		{
			lastCursedDate = formatter.parse(lastCursedString);
		}
		catch (Exception ex)
		{
			lastCursedDate = new Date();
			lastCursedDate.setTime(0L);
		}
		return lastCursedDate;
	}

	/**
	 * @return minutes since the god's last quest started, or Long.MAX_VALUE if it never had one
	 */
	public long getMinutesSinceLastQuest(String godName)
	{
		String lastQuestDateString = godsConfig().getString(godName + ".LastQuestTime");
		if (lastQuestDateString == null)
		{
			return Long.MAX_VALUE;
		}

		Date questDate;
		try
		{
			questDate = formatter.parse(lastQuestDateString);
		}
		catch (Exception ex)
		{
			return Long.MAX_VALUE;
		}

		return (System.currentTimeMillis() - questDate.getTime()) / 60000L;
	}

	public GodMood getMoodForGod(String godName)
	{
		float godMood = (float) godsConfig().getDouble(godName + ".Mood");
		if (godMood < -70.0F)
		{
			return GodMood.ANGRY;
		}
		if (godMood < -20.0F)
		{
			return GodMood.DISPLEASED;
		}
		if (godMood < 20.0F)
		{
			return GodMood.NEUTRAL;
		}
		if (godMood < 70.0F)
		{
			return GodMood.PLEASED;
		}
		return GodMood.EXALTED;
	}

	private UUID getNextBelieverForPriest(String godName)
	{
		Set<UUID> allBelievers = ctx.believerManager().getBelieversForGod(godName);

		List<PriestCandidate> candidates = new ArrayList<PriestCandidate>();

		if (allBelievers == null || allBelievers.size() == 0)
		{
			ctx.plugin().logDebug("Did not find any priest candidates");
			return null;
		}

		UUID pendingPriest = getPendingPriest(godName);

		for (UUID candidate : allBelievers)
		{
			Player player = ctx.plugin().getServer().getPlayer(candidate);
			if (player != null)
			{
				if (!isPriest(candidate))
				{
					if ((pendingPriest == null) || (!pendingPriest.equals(candidate)))
					{
						if (!ctx.believerManager().hasRecentPriestOffer(candidate))
						{
							if (ctx.permissionsManager().hasPermission(player, "gods.priest"))
							{
								candidates.add(new PriestCandidate(candidate));
							}
						}
					}
				}
			}
		}

		if (candidates.size() == 0)
		{
			return null;
		}

		Collections.sort(candidates, new NewPriestComparator());

		PriestCandidate finalCandidate = null;
		if (candidates.size() > 2)
		{
			finalCandidate = (PriestCandidate) candidates.toArray()[this.random.nextInt(3)];
		}
		else
		{
			finalCandidate = (PriestCandidate) candidates.toArray()[0];
		}

		return finalCandidate.believerId;
	}

	public Material getUnholyFoodTypeForGod(String godName)
	{
		String foodTypeString = godsConfig().getString(godName + ".NotEatFoodType");
		Material foodType = Material.AIR;
		if (foodTypeString == null)
		{
			do
			{
				int r1 = this.random.nextInt(7);
				switch (r1)
				{
				case 0:
					foodType = Material.APPLE;
					break;
				case 1:
					foodType = Material.BREAD;
					break;
				case 2:
					foodType = Material.COOKED_SALMON;
					break;
				case 3:
					foodType = Material.MELON_SLICE;
					break;
				case 4:
					foodType = Material.COOKED_BEEF;
					break;
				case 5:
					foodType = Material.COOKED_PORKCHOP;
					break;
				default:	
				case 6:
					foodType = Material.CARROT;
				}
				foodTypeString = foodType.name();
			}
			while (foodTypeString.equals(getHolyFoodTypeForGod(godName).name()));
			
			setGodValue(godName, ".NotEatFoodType", foodTypeString);

			godsFile().markDirty();
		}
		else
		{
			foodType = Material.getMaterial(foodTypeString);
		}
		
		if(foodType == null)
		{
			foodType = Material.CARROT;
		}

		return foodType;
	}

	public List<String> getOfflineGods()
	{
		Set<String> allGods = godsConfig().getKeys(false);
		List<String> offlineGods = new ArrayList<String>();
		for (String godName : allGods)
		{
			if (godName == null)
			{
				continue;
			}
			
			if (!this.onlineGods.contains(godName))
			{
				offlineGods.add(godName);
			}
		}
		return offlineGods;
	}

	public List<String> getOnlineGods()
	{
		return this.onlineGods;
	}

	public UUID getPendingPriest(String godName)
	{
		UUID believerId = parseUUID(godsConfig().getString(godName + ".PendingPriest"));

		if (believerId == null)
		{
			return null;
		}

		Player player = ctx.plugin().getServer().getPlayer(believerId);

		if (player == null)
		{
			return null;
		}

		return player.getUniqueId();
	}

	private Material getPickAxeBlessing(String godName)
	{
		if (getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel3Items())
		{
			return Material.DIAMOND_PICKAXE;
		}
		if (getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel2Items())
		{
			return Material.IRON_PICKAXE;
		}
		if (getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel1Items())
		{
			return Material.STONE_PICKAXE;
		}
		return Material.WOODEN_PICKAXE;
	}

	public float getPleasedModifierForGod(String godName)
	{
		Random moodRandom = new Random(getSeedForGod(godName));

		return 5 + moodRandom.nextInt(10);
	}

	public List<UUID> getPriestsForGod(String godName)
	{
		List<String> names = godsConfig().getStringList(godName + ".Priests");
		List<UUID> list = new ArrayList<UUID>();

		for (String name : names)
		{
			// load() converts the player names Gods 0.9.x stored here; skip any it couldn't
			UUID believerId = parseUUID(name);
			if (believerId == null)
			{
				continue;
			}

			long diffHours = (System.currentTimeMillis() - ctx.believerManager().getLastPrayerTime(believerId).getTime()) / 3600000L;
			if (diffHours > ctx.settings().priests().maxHoursWithoutPrayer())
			{
				String priestName = ctx.plugin().getServer().getOfflinePlayer(believerId).getName();
				ctx.languageManager().setPlayerName(priestName != null ? priestName : "our priest");
				godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversRemovedPriest, 2 + this.random.nextInt(40));
				removePriest(godName, believerId);
			}
			else
			{
				list.add(believerId);
			}
		}
		return list;
	}

	public Material getSacrificeItemTypeForGod(String godName)
	{
		String itemName = "";
		Integer value = Integer.valueOf(0);
		String sacrificeItemName = null;

		ConfigurationSection configSection = godsConfig().getConfigurationSection(godName + ".SacrificeValues");
		if ((configSection == null) || (configSection.getKeys(false).size() == 0))
		{
			return null;
		}
		for (int i = 0; i < configSection.getKeys(false).size(); i++)
		{
			itemName = (String) configSection.getKeys(false).toArray()[this.random.nextInt(configSection.getKeys(false).size())];

			value = Integer.valueOf(godsConfig().getInt(godName + ".SacrificeValues." + itemName));
			if (value.intValue() > 10)
			{
				sacrificeItemName = itemName;
			}
		}
		if (sacrificeItemName != null)
		{
			return Material.getMaterial(sacrificeItemName);
		}
		return null;
	}

	private Material getSacrificeNeedForGod(String godName)
	{
		Random materialRandom = new Random(getSeedForGod(godName));
		List<Integer> materials = new ArrayList<Integer>();

		for (int n = 0; n < 5; n++)
		{
			materials.add(materialRandom.nextInt(24));
		}

		int typeIndex = 0;
		Material type = Material.AIR;

		do
		{
			typeIndex = materials.get(this.random.nextInt(materials.size())).intValue();

			switch (typeIndex)
			{
			case 0:
				type = Material.POPPY;
				break;
			case 1:
				type = Material.COOKED_PORKCHOP;
				break;
			case 2:
				type = getUnholyFoodTypeForGod(godName);
				break;
			case 3:
				type = Material.RABBIT_HIDE;
				break;
			case 4:
				type = Material.RABBIT_FOOT;
				break;
			case 5:
				type = Material.CACTUS;
				break;
			case 6:
				type = Material.BREAD;
				break;
			case 7:
				type = Material.CARROT;
				break;
			case 8:
				type = Material.IRON_PICKAXE;
				break;
			case 9:
				type = Material.IRON_INGOT;
				break;
			case 10:
				type = Material.GOLD_INGOT;
				break;
			case 11:
				type = Material.APPLE;
				break;
			case 12:
				type = Material.BOOK;
				break;
			case 13:
				type = Material.CAKE;
				break;
			case 14:
				type = Material.MELON_SLICE;
				break;
			case 15:
				type = Material.COOKIE;
				break;
			case 16:
				type = Material.PUMPKIN;
				break;
			case 17:
				type = Material.SUGAR_CANE;
				break;
			case 18:
				type = Material.EGG;
				break;
			case 19:
				type = Material.WHEAT;
				break;
			case 20:
				type = Material.NETHERRACK;
				break;
			case 21:
				type = Material.POTATO;
				break;
			case 22:
				type = Material.BONE;
				break;
			case 23:
			default:	
				type = Material.FEATHER;
			}
		}
		while (type == getHolyFoodTypeForGod(godName) || type == Material.AIR);

		return type;
	}

	private Material getSacrificeUnwantedForGod(String godName)
	{
		List<Material> unwantedItems = new ArrayList<Material>();
		ConfigurationSection configSection = godsConfig().getConfigurationSection(godName + ".SacrificeValues.");
		if (configSection != null)
		{
			for (String itemType : configSection.getKeys(false))
			{
				Material item = null;
				try
				{
					item = Material.valueOf(itemType);
				}
				catch (Exception ex)
				{
					continue;
				}
				if (godsConfig().getDouble(godName + ".SacrificeValues." + itemType) <= 0.0D)
				{
					unwantedItems.add(item);
				}
			}
		}
		else
		{
			return null;
		}
		if (unwantedItems.size() == 0)
		{
			return null;
		}
		return unwantedItems.get(this.random.nextInt(unwantedItems.size()));
	}

	private float getSacrificeValueForGod(String godName, Material type)
	{
		if(!godsConfig().isSet(godName + ".SacrificeValues." + type.name()))
		{
			return 0;
		}
						
		return (float)godsConfig().getDouble(godName + ".SacrificeValues." + type.name());
	}

	public long getSeedForGod(String godName)
	{
		long seed = godsConfig().getLong(godName + ".Seed");
		if (seed == 0L)
		{
			seed = this.random.nextLong();
			setGodValue(godName, ".Seed", Long.valueOf(seed));

			godsFile().markDirty();
		}
		return seed;
	}

	private Material getSpadeBlessing(String godName)
	{
		if (getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel3Items())
		{
			return Material.DIAMOND_SHOVEL;
		}
		if (getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel2Items())
		{
			return Material.IRON_SHOVEL;
		}
		if (getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel1Items())
		{
			return Material.STONE_SHOVEL;
		}
		return Material.WOODEN_SHOVEL;
	}

	private Material getSwordBlessing(String godName)
	{
		if (getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel3Items())
		{
			return Material.DIAMOND_SWORD;
		}
		if (getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel2Items())
		{
			return Material.IRON_SWORD;
		}
		if (getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel1Items())
		{
			return Material.STONE_SWORD;
		}
		return Material.WOODEN_SWORD;
	}

	public String getTitleForGod(String godName)
	{
		if (!ctx.settings().gods().titles())
		{
			return "";
		}
		GodType godType = this.getDivineForceForGod(godName);
		if (godType == null)
		{
			return "";
		}
		return ctx.languageManager().getGodTypeName(godType, ctx.languageManager().getGodGenderName(this.getGenderForGod(godName)));
	}

	public Set<String> getTopGods()
	{
		Set<String> topGods = godsConfig().getKeys(false);

		return topGods;
	}

	public EntityType getUnholyMobTypeForGod(String godName)
	{
		EntityType mobType = parseEntityType(godsConfig().getString(godName + ".SlayMobType"));
		if (mobType == null)
		{
			String mobTypeString = generateUnholyMobTypeForGod();

			setGodValue(godName, ".SlayMobType", mobTypeString);
			godsFile().markDirty();

			mobType = EntityType.valueOf(mobTypeString);
		}
		return mobType;
	}

	private int getVerbosityForGod(String godName)
	{
		int verbosity = godsConfig().getInt(godName + ".Verbosity");
		if (verbosity == 0)
		{
			verbosity = 1 + this.random.nextInt(50);

			setGodValue(godName, ".Verbosity", Integer.valueOf(verbosity));

			godsFile().save();
		}
		Random moodRandom = new Random(getSeedForGod(godName));

		double variation = 1.0D + 1.0D * Math.sin(moodRandom.nextFloat() + System.currentTimeMillis() / 3600000.0F);

		double godVerbosity = getGodPower(godName) / 100.0F + verbosity;

		return (int) (1.0D + variation * (ctx.settings().gods().verbosity() * godVerbosity));
	}

	public List<String> getWarRelations(String godName)
	{
		return godsConfig().getStringList(godName + ".Enemies");
	}

	public void giveHolyArtifact(String godName, GodType godType, Player player, boolean speak)
	{
		// A second artifact would only drop at their feet: the pickup is refused while they carry one
		if (ctx.holyArtifactManager().hasHolyArtifact(player))
		{
			return;
		}
		ctx.plugin().getServer().getScheduler().runTaskLater(ctx.plugin(), new TaskGiveHolyArtifact(ctx, godName, godType, player, speak), 2L);
	}

	public void giveItem(String godName, Player player, Material material, boolean speak)
	{
		ctx.plugin().getServer().getScheduler().runTaskLater(ctx.plugin(), new TaskGiveItem(ctx, godName, player, material, speak), 2L);
	}

	public boolean godExist(String godName)
	{
		String name = godsConfig().getString(formatGodName(godName) + ".Created");
		if (name == null)
		{
			return false;
		}
		return true;
	}

	public void GodSay(String godName, Player player, LanguageManager.LANGUAGESTRING message, int delay)
	{
		String playerNameString = ctx.languageManager().getPlayerName();
		String typeNameString = ctx.languageManager().getType();
		int amount = ctx.languageManager().getAmount();

		if (player == null)
		{
			ctx.plugin().logDebug("GodSay(): Player is null!");
			return;
		}

		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return;
		}

		ctx.plugin().logDebug(godName + " to " + player.getName() + ": " + ctx.languageManager().getLanguageString(godName, message));

		if (!ctx.permissionsManager().hasPermission(player, "gods.listen"))
		{
			return;
		}

		ctx.plugin().getServer().getScheduler().runTaskLater(ctx.plugin(), new TaskGodSpeak(ctx, godName, player.getUniqueId(), playerNameString, typeNameString, amount, message), delay);
	}
	
	private boolean godSayNeededSacrificeToBeliever(String godName, UUID believerId)
	{
		if (ctx.settings().sacrifices().enabled())
		{
			Material itemType = getSacrificeItemTypeForGod(godName);
			if (itemType != null)
			{
				String itemName = ctx.languageManager().getItemTypeName(itemType);
				try
				{
					ctx.languageManager().setType(itemName);
				}
				catch (Exception ex)
				{
					ctx.plugin().logDebug(ex.getMessage());
				}

				godSayToBeliever(godName, believerId, LanguageManager.LANGUAGESTRING.GodToBelieversSacrificeItemType);

				return true;
			}
		}
		return false;
	}

	public void godSayToBeliever(String godName, UUID playerId, LanguageManager.LANGUAGESTRING message)
	{
		godSayToBeliever(godName, playerId, message, 2 + this.random.nextInt(10));
	}

	public void godSayToBeliever(String godName, UUID playerId, LanguageManager.LANGUAGESTRING message, int delay)
	{
		Player player = ctx.plugin().getServer().getPlayer(playerId);

		if (player == null)
		{
			ctx.plugin().logDebug("GodSayToBeliever player is null");
			return;
		}
		GodSay(godName, player, message, delay);
	}

	public void godSayToBelievers(String godName, LanguageManager.LANGUAGESTRING message, int delay)
	{
		for (UUID playerId : ctx.believerManager().getBelieversForGod(godName))
		{
			Player player = ctx.plugin().getServer().getPlayer(playerId);
			if (player != null)
			{
				GodSay(godName, player, message, delay);
			}
		}
	}

	public void GodSayToBelieversExcept(String godName, LanguageManager.LANGUAGESTRING message, UUID exceptPlayer)
	{
		for (UUID playerId : ctx.believerManager().getBelieversForGod(godName))
		{
			Player player = ctx.plugin().getServer().getPlayer(playerId);

			if (player != null && !player.getUniqueId().equals(exceptPlayer))
			{
				GodSay(godName, player, message, 2 + this.random.nextInt(20));
			}
		}
	}

	public void GodSayToPriest(String godName, LanguageManager.LANGUAGESTRING message)
	{
		List<UUID> priests = getPriestsForGod(godName);
		if (priests == null)
		{
			return;
		}

		for (UUID priest : priests)
		{
			Player player = ctx.plugin().getServer().getPlayer(priest);
			if (player != null)
			{
				GodSay(godName, player, message, 2 + this.random.nextInt(30));
			}
		}
	}

	public void GodSayWithQuestion(String godName, Player player, LanguageManager.LANGUAGESTRING message, int delay)
	{
		String playerNameString = ctx.languageManager().getPlayerName();
		String typeNameString = ctx.languageManager().getType();
		int amount = ctx.languageManager().getAmount();

		if (player == null)
		{
			ctx.plugin().logDebug("GodSay(): Player is null!");
			return;
		}
		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return;
		}
		ctx.plugin().logDebug(godName + " to " + player.getName() + ": " + ctx.languageManager().getLanguageString(godName, message));
		if (!ctx.permissionsManager().hasPermission(player, "gods.listen"))
		{
			return;
		}

		ctx.plugin().getServer().getScheduler().runTaskLater(ctx.plugin(), new TaskGodSpeak(ctx, godName, player.getUniqueId(), playerNameString, typeNameString, amount, message), delay);

		ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverQuestionHelp, ChatColor.AQUA, ChatColor.WHITE + "/gods yes or /gods no", ChatColor.WHITE + "/gods yes or /gods no", delay + 80);
	}

	public boolean handleAltarPray(Location location, Player player, String godName)
	{
		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return false;
		}

		if (addBeliefByAltar(player, godName, location, ctx.believerManager().getChangingGod(player.getUniqueId())))
		{
			Block altarBlock = ctx.altarManager().getAltarBlockFromSign(player.getWorld().getBlockAt(location));

			if (this.getGenderForGod(godName) == GodGender.None)
			{
				GodGender godGender = ctx.altarManager().getGodGenderFromAltarBlock(altarBlock);

				ctx.plugin().logDebug("God did not have a gender, setting gender to " + godGender);

				this.setGenderForGod(godName, godGender);
			}
			if (this.getDivineForceForGod(godName) == null)
			{
				GodType godType = ctx.altarManager().getGodTypeForAltarBlockType(altarBlock.getType());

				ctx.plugin().logDebug("God did not have a divine force, setting divine force to " + godType);

				this.setDivineForceForGod(godName, godType);
			}

			addMoodForGod(godName, getPleasedModifierForGod(godName));

			if ((ctx.settings().holyLand().enabled()) && (ctx.permissionsManager().hasPermission(player, "gods.holyland")))
			{
				ctx.holyLandManager().addPrayer(player, godName, altarBlock.getLocation());
			}

			ctx.questManager().handlePrayer(godName, player.getUniqueId());

			ctx.languageManager().setPlayerName(player.getName());

			GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverPraying, 2 + this.random.nextInt(10));
			location.getWorld().playEffect(location, Effect.MOBSPAWNER_FLAMES, 25);

			return true;
		}

		return false;
	}

	public void handleEat(Player player, String godName, String foodType)
	{
		Material eatFoodType = getHolyFoodTypeForGod(godName);
		Material notEatFoodType = getUnholyFoodTypeForGod(godName);

		if (foodType.equals(eatFoodType.name()))
		{
			addMoodForGod(godName, getPleasedModifierForGod(godName));

			if (blessPlayer(godName, player.getUniqueId(), getGodPower(godName)))
			{
				try
				{
					ctx.languageManager().setType(ctx.languageManager().getItemTypeName(eatFoodType));
				}
				catch (Exception ex)
				{
					ctx.plugin().logDebug(ex.getMessage());
				}
				
				ctx.languageManager().setPlayerName(player.getDisplayName());
				
				if (ctx.settings().commandments().broadcastFoodEaten())
				{
					godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversEatFoodBlessing, 2 + this.random.nextInt(20));
				}
				else
				{
					godSayToBeliever(godName, player.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieversEatFoodBlessing);
				}
			}
		}

		if (foodType.equals(notEatFoodType.name()))
		{
			addMoodForGod(godName, getAngryModifierForGod(godName));
			
			if (cursePlayer(godName, player.getUniqueId(), getGodPower(godName)))
			{
				try
				{
					ctx.languageManager().setType(ctx.languageManager().getItemTypeName(notEatFoodType));
				}
				catch (Exception ex)
				{
					ctx.plugin().logDebug(ex.getMessage());
				}

				ctx.languageManager().setPlayerName(player.getDisplayName().toUpperCase());

				if (ctx.settings().commandments().broadcastFoodEaten())
				{
					godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversNotEatFoodCursing, 2 + this.random.nextInt(10));
				}
				else
				{
					godSayToBeliever(godName, player.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieversNotEatFoodCursing);
				}
			}
		}
	}

	public void handleKilled(Player player, String godName, String mobType)
	{
		if ((!ctx.settings().commandments().enabled()) || (mobType == null))
		{
			return;
		}
		EntityType holyMobType = getHolyMobTypeForGod(godName);
		EntityType unholyMobType = getUnholyMobTypeForGod(godName);
		if ((unholyMobType != null) && (mobType.equals(unholyMobType.name())))
		{
			if (blessPlayer(godName, player.getUniqueId(), getGodPower(godName)))
			{
				addMoodForGod(godName, getPleasedModifierForGod(godName));

				ctx.languageManager().setPlayerName(player.getDisplayName());
				try
				{
					ctx.languageManager().setType(ctx.languageManager().getMobTypeName(unholyMobType));
				}
				catch (Exception ex)
				{
					ctx.plugin().logDebug(ex.getMessage());
				}
				if (ctx.settings().commandments().broadcastMobSlain())
				{
					godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSlayMobBlessing, 2 + this.random.nextInt(20));
				}
				else
				{
					godSayToBeliever(godName, player.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieversSlayMobBlessing);
				}
			}
		}

		if ((holyMobType != null) && mobType.equals(holyMobType.name()))
		{
			if (cursePlayer(godName, player.getUniqueId(), getGodPower(godName)))
			{
				addMoodForGod(godName, getAngryModifierForGod(godName));

				ctx.languageManager().setPlayerName(player.getDisplayName().toUpperCase());
				try
				{
					ctx.languageManager().setType(ctx.languageManager().getMobTypeName(holyMobType));
				}
				catch (Exception ex)
				{
					ctx.plugin().logDebug(ex.getMessage());
				}
				
				if (ctx.settings().commandments().broadcastMobSlain())
				{
					godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversNotSlayMobCursing, 2 + this.random.nextInt(10));
				}
				else
				{
					godSayToBeliever(godName, player.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieversNotSlayMobCursing);
				}
			}
		}
	}

	public void handleKilledPlayer(UUID playerId, String godName, GodType godType)
	{
		if (godName == null || godType == null)
		{
			return;
		}
		if (ctx.settings().believers().leaveReligionOnDeath())
		{
			if (isPriestForGod(playerId, godName))
			{
				removePriest(godName, playerId);
			}
			ctx.believerManager().believerLeave(godName, playerId);
		}
	}

	public void handleSacrifice(String godName, Player believer, Material type)
	{
		if (believer == null)
		{
			return;
		}

		if (!ctx.plugin().isEnabledInWorld(believer.getWorld()))
		{
			return;
		}

		if (godName == null)
		{
			return;
		}

		int godPower = (int) this.getGodPower(godName);

		ctx.plugin().log(believer.getDisplayName() + " sacrificed " + type.name() + " to " + godName);

		Material eatFoodType = getHolyFoodTypeForGod(godName);

		if (type == eatFoodType)
		{
			addMoodForGod(godName, getAngryModifierForGod(godName));
			cursePlayer(godName, believer.getUniqueId(), getGodPower(godName));

			try
			{
				ctx.languageManager().setType(ctx.languageManager().getItemTypeName(eatFoodType));
			}
			catch (Exception ex)
			{
				ctx.plugin().logDebug(ex.getMessage());
			}

			ctx.languageManager().setPlayerName(believer.getDisplayName());

			if (ctx.settings().commandments().broadcastFoodEaten())
			{
				godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieverHolyFoodSacrifice, 2 + this.random.nextInt(10));
			}
			else
			{
				godSayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverHolyFoodSacrifice);
			}

			strikePlayerWithLightning(believer.getUniqueId(), 1 + this.random.nextInt(3));

			return;
		}

		float value = getSacrificeValueForGod(godName, type);

		ctx.languageManager().setPlayerName(believer.getDisplayName());

		try
		{
			ctx.languageManager().setType(ctx.languageManager().getItemTypeName(type));
		}
		catch (Exception ex)
		{
			ctx.plugin().logDebug(ex.getMessage());
		}

		if (value > 10.0F)
		{
			addMoodForGod(godName, getPleasedModifierForGod(godName));
			ctx.believerManager().addPrayer(believer.getUniqueId(), godName);

			blessPlayer(godName, believer.getUniqueId(), godPower);
			godSayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverGoodSacrifice);

			ctx.believerManager().increasePrayerPower(believer.getUniqueId(), 1);
		}
		else if (value >= -5.0F)
		{
			godSayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverMehSacrifice);
		}
		else
		{
			addMoodForGod(godName, getAngryModifierForGod(godName));
			strikePlayerWithLightning(believer.getUniqueId(), 1 + this.random.nextInt(3));
			godSayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverBadSacrifice);
		}

		value -= 1.0F;

		setGodValue(godName, ".SacrificeValues." + type.name(), Float.valueOf(value));

		godsFile().markDirty();
	}

	public boolean hasAllianceRelation(String godName, String otherGodName)
	{
		return getAllianceRelations(godName).contains(otherGodName);
	}

	private boolean hasAxe(Player player)
	{
		PlayerInventory inventory = player.getInventory();
		if (inventory.contains(Material.WOODEN_AXE))
		{
			return true;
		}
		if (inventory.contains(Material.STONE_AXE))
		{
			return true;
		}
		if (inventory.contains(Material.IRON_AXE))
		{
			return true;
		}
		if (inventory.contains(Material.DIAMOND_AXE))
		{
			return true;
		}
		return false;
	}

	private boolean hasFood(Player player, String godName)
	{
		PlayerInventory inventory = player.getInventory();
		if (inventory.contains(this.getHolyFoodTypeForGod(godName)))
		{
			return true;
		}
		return false;
	}

	public boolean hasGodAccess(UUID believerId, String godName)
	{
		if (!isPrivateAccess(godName))
		{
			return true;
		}

		String currentGodName = ctx.believerManager().getGodForBeliever(believerId);

		if ((currentGodName == null) || (!currentGodName.equals(godName)))
		{
			return false;
		}
		return true;
	}

	private boolean hasHoe(Player player)
	{
		PlayerInventory inventory = player.getInventory();
		if (inventory.contains(Material.WOODEN_HOE))
		{
			return true;
		}
		if (inventory.contains(Material.STONE_HOE))
		{
			return true;
		}
		if (inventory.contains(Material.IRON_HOE))
		{
			return true;
		}
		if (inventory.contains(Material.DIAMOND_HOE))
		{
			return true;
		}
		return false;
	}

	private boolean hasPickAxe(Player player)
	{
		PlayerInventory inventory = player.getInventory();
		if (inventory.contains(Material.WOODEN_PICKAXE))
		{
			return true;
		}
		if (inventory.contains(Material.STONE_PICKAXE))
		{
			return true;
		}
		if (inventory.contains(Material.IRON_PICKAXE))
		{
			return true;
		}
		if (inventory.contains(Material.DIAMOND_PICKAXE))
		{
			return true;
		}
		return false;
	}

	private boolean hasSpade(Player player)
	{
		PlayerInventory inventory = player.getInventory();
		if (inventory.contains(Material.WOODEN_SHOVEL))
		{
			return true;
		}
		if (inventory.contains(Material.STONE_SHOVEL))
		{
			return true;
		}
		if (inventory.contains(Material.IRON_SHOVEL))
		{
			return true;
		}
		if (inventory.contains(Material.DIAMOND_SHOVEL))
		{
			return true;
		}
		return false;
	}

	private boolean hasSword(Player player)
	{
		PlayerInventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getSize(); i++)
		{
			ItemStack stack = inventory.getItem(i);
			if ((stack != null) && ((stack.getType().equals(Material.WOODEN_SWORD)) || (stack.getType().equals(Material.STONE_SWORD)) || (stack.getType().equals(Material.IRON_SWORD)) || (stack.getType().equals(Material.DIAMOND_SWORD))) && (stack.getAmount() != 0))
			{
				return true;
			}
		}
		return false;
	}

	public boolean hasWarRelation(String godName, String otherGodName)
	{
		return getWarRelations(godName).contains(otherGodName);
	}

	public void healPlayer(String godName, Player player)
	{
		ctx.plugin().getServer().getScheduler().runTaskLater(ctx.plugin(), new TaskHealPlayer(ctx, godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverHealthBlessing), 2L);
	}

	public boolean isDeadGod(String godName)
	{
		if ((ctx.believerManager().getBelieversForGod(godName).size() == 0) && (this.getGodPower(godName) < 1.0F))
		{
			removeGod(godName);

			return true;
		}
		return false;
	}

	public boolean isPriest(UUID believerId)
	{
		if (believerId == null)
		{
			return false;
		}

		Set<String> gods = getAllGods();

		for (String godName : gods)
		{
			List<UUID> list = getPriestsForGod(godName);

			if (list != null && list.contains(believerId))
			{
				return true;
			}
		}
		return false;
	}

	public boolean isPriestForGod(UUID believerId, String godName)
	{
		if (believerId == null)
		{
			return false;
		}

		List<UUID> priests = getPriestsForGod(godName);

		if (priests != null && priests.contains(believerId))
		{
			return true;
		}
		return false;
	}

	public boolean isPrivateAccess(String godName)
	{
		Boolean access = Boolean.valueOf(godsConfig().getBoolean(godName + ".PrivateAccess"));
		if (access != null)
		{
			return access.booleanValue();
		}
		return false;
	}

	/**
	 * Reads gods.yml and upgrades what older versions wrote (priests by name, LanguageFileName). If the file can't be
	 * read, Storage keeps it untouched and nothing here changes it.
	 */
	public void load()
	{
		godsFile().load();
		if (godsFile().isReadOnly())
		{
			return;
		}

		boolean changed = false;

		Map<String, UUID> playerIdsByName = null;

		for (String godName : godsConfig().getKeys(false))
		{
			List<String> priests = godsConfig().getStringList(godName + ".Priests");
			boolean priestsChanged = false;

			// Gods 0.9.x kept a single priest by name in PriestName
			String priestName = godsConfig().getString(godName + ".PriestName");
			if (priestName != null)
			{
				if (!priests.contains(priestName))
				{
					priests.add(priestName);
				}
				godsConfig().set(godName + ".PriestName", null);
				priestsChanged = true;
			}

			// Priests used to be stored by player name. Convert them to UUIDs, or drop them if the player is unknown.
			List<String> priestIds = new ArrayList<String>();
			for (String priest : priests)
			{
				UUID priestId = parseUUID(priest);
				if (priestId == null && priest != null && !priest.equals("none"))
				{
					if (playerIdsByName == null)
					{
						playerIdsByName = getKnownPlayerIdsByName();
					}
					priestId = playerIdsByName.get(priest.toLowerCase());
					if (priestId == null)
					{
						ctx.plugin().log("Removed unknown priest '" + priest + "' from " + godName);
					}
				}
				if (priestId == null || priestIds.contains(priestId.toString()))
				{
					priestsChanged = true;
					continue;
				}
				if (!priestId.toString().equals(priest))
				{
					priestsChanged = true;
				}
				priestIds.add(priestId.toString());
			}
			if (priestsChanged)
			{
				godsConfig().set(godName + ".Priests", priestIds);
				changed = true;
			}

			// The language file is now chosen from Settings.Language and the god's type and gender
			if (godsConfig().contains(godName + ".LanguageFileName"))
			{
				godsConfig().set(godName + ".LanguageFileName", null);
				changed = true;
			}
		}

		ctx.plugin().log("Loaded " + godsConfig().getKeys(false).size() + " gods.");

		if (changed)
		{
			godsFile().save();
		}
	}

	/**
	 * @return the UUIDs of every player who has been on this server, by lower case name
	 */
	private Map<String, UUID> getKnownPlayerIdsByName()
	{
		Map<String, UUID> playerIds = new HashMap<String, UUID>();
		for (OfflinePlayer player : ctx.plugin().getServer().getOfflinePlayers())
		{
			if (player.getName() != null)
			{
				playerIds.put(player.getName().toLowerCase(), player.getUniqueId());
			}
		}
		return playerIds;
	}

	private boolean manageBelieverForAngryGod(String godName, Player believer)
	{
		if (!ctx.plugin().isEnabledInWorld(believer.getWorld()))
		{
			return false;
		}

		int godPower = 1 + (int) this.getGodPower(godName);

		if (this.random.nextInt(1 + 1000 / godPower) == 0)
		{
			if (ctx.believerManager().hasRecentPrayer(believer.getUniqueId()))
			{
				return false;
			}

			if (cursePlayer(godName, believer.getUniqueId(), godPower))
			{
				ctx.languageManager().setPlayerName(believer.getDisplayName());

				GodSay(godName, believer, LanguageManager.LANGUAGESTRING.GodToBelieverCursedAngry, 2 + this.random.nextInt(10));

				return true;
			}
		}

		if (this.random.nextInt(1 + 1000 / getVerbosityForGod(godName)) == 0)
		{
			if ((ctx.believerManager().hasRecentPrayer(believer.getUniqueId())) && (this.random.nextInt(2) == 0))
			{
				return false;
			}
			godSayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverRandomAngrySpeech);
			return true;
		}

		if (this.random.nextInt(1 + 600 / getVerbosityForGod(godName)) == 0)
		{
			if (godSayNeededSacrificeToBeliever(godName, believer.getUniqueId()))
			{
				return true;
			}
		}
		return false;
	}

	private boolean manageBelieverForDispleasedGod(String godName, Player believer)
	{
		if (believer == null)
		{
			return false;
		}
		if (!ctx.plugin().isEnabledInWorld(believer.getWorld()))
		{
			return false;
		}
		if (this.random.nextInt(1 + 1000 / getVerbosityForGod(godName)) == 0)
		{
			if ((ctx.believerManager().hasRecentPrayer(believer.getUniqueId())) && (this.random.nextInt(2) == 0))
			{
				return false;
			}
			godSayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverRandomDispleasedSpeech);
			return true;
		}
		if (this.random.nextInt(1 + 600 / getVerbosityForGod(godName)) == 0)
		{
			if (godSayNeededSacrificeToBeliever(godName, believer.getUniqueId()))
			{
				return true;
			}
		}
		return false;
	}

	private boolean manageBelieverForExaltedGod(String godName, Player believer)
	{
		if (believer == null)
		{
			return false;
		}

		if (!ctx.plugin().isEnabledInWorld(believer.getWorld()))
		{
			return false;
		}

		if ((believer.getGameMode() != GameMode.CREATIVE) && ctx.permissionsManager().hasPermission(believer, "gods.itemblessings"))
		{
			if (!ctx.believerManager().hasRecentItemBlessing(believer.getUniqueId()))
			{
				if (ctx.settings().itemBlessings().enabled())
				{
					float power = getGodPower(godName);

					if (power >= ctx.settings().itemBlessings().minGodPower() && this.random.nextInt((int) (1.0F + 50.0F / power)) == 0)
					{
						double healing = getHealthNeed(godName, believer);

						if ((healing > 1.0D) && (this.random.nextInt(3) == 0))
						{
							healPlayer(godName, believer);

							ctx.believerManager().setItemBlessingTime(believer.getUniqueId());

							return true;
						}

						ItemStack blessedItem = blessPlayerWithItem(godName, believer);

						if (blessedItem != null)
						{
							ctx.languageManager().setPlayerName(believer.getDisplayName());
							try
							{
								ctx.languageManager().setType(ctx.languageManager().getItemTypeName(blessedItem.getType()));
							}
							catch (Exception ex)
							{
								ctx.plugin().logDebug(ex.getMessage());
							}

							ctx.believerManager().setItemBlessingTime(believer.getUniqueId());

							return true;
						}
					}
				}
			}

			if (ctx.settings().holyArtifacts().enabled())
			{
				if (!ctx.believerManager().hasRecentHolyArtifactBlessing(believer.getUniqueId()))
				{
					float power = getGodPower(godName);

					if ((power >= ctx.settings().itemBlessings().minGodPower()) && (this.random.nextInt((int) (1.0F + 100.0F / power)) == 0))
					{
						blessPlayerWithHolyArtifact(godName, believer);

						ctx.languageManager().setPlayerName(believer.getDisplayName());
						ctx.believerManager().setHolyArtifactBlessingTime(believer.getUniqueId());

						return true;
					}
				}
			}
		}

		if (!ctx.believerManager().hasRecentItemBlessing(believer.getUniqueId()))
		{
			if (blessPlayer(godName, believer.getUniqueId(), getGodPower(godName)))
			{
				ctx.languageManager().setPlayerName(believer.getDisplayName());

				GodSay(godName, believer, LanguageManager.LANGUAGESTRING.GodToPlayerBlessed, 2 + this.random.nextInt(10));

				GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPlayerBlessed, believer.getUniqueId());

				return true;
			}
		}

		if (ctx.settings().marriage().enabled() && this.random.nextInt(501) == 0)
		{
			List<MarriageManager.MarriedCouple> marriedCouples = ctx.marriageManager().getMarriedCouples();
			if (marriedCouples.size() > 0)
			{
				MarriageManager.MarriedCouple couple = marriedCouples.get(this.random.nextInt(marriedCouples.size()));

				ctx.languageManager().setPlayerName(ctx.plugin().getServer().getOfflinePlayer(couple.player1Id).getName() + " and " + ctx.plugin().getServer().getOfflinePlayer(couple.player2Id).getName());
				godSayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverMarriedCouple);
				return true;
			}
		}

		if (this.random.nextInt(1 + 1000 / getVerbosityForGod(godName)) == 0)
		{
			if ((ctx.believerManager().hasRecentPrayer(believer.getUniqueId())) && (this.random.nextInt(2) == 0))
			{
				return false;
			}
			godSayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverRandomExaltedSpeech);
			return true;
		}

		if (this.random.nextInt(1 + 600 / getVerbosityForGod(godName)) == 0)
		{
			if (godSayNeededSacrificeToBeliever(godName, believer.getUniqueId()))
			{
				return true;
			}
		}
		return false;
	}

	private boolean manageBelieverForNeutralGod(String godName, Player believer)
	{
		if (believer == null)
		{
			return false;
		}

		if (!ctx.plugin().isEnabledInWorld(believer.getWorld()))
		{
			return false;
		}
		if ((ctx.settings().marriage().enabled()) && (this.random.nextInt(501) == 0))
		{
			List<MarriageManager.MarriedCouple> marriedCouples = ctx.marriageManager().getMarriedCouples();
			if (marriedCouples.size() > 0)
			{
				MarriageManager.MarriedCouple couple = marriedCouples.get(this.random.nextInt(marriedCouples.size()));

				ctx.languageManager().setPlayerName(ctx.plugin().getServer().getOfflinePlayer(couple.player1Id).getName() + " and " + ctx.plugin().getServer().getOfflinePlayer(couple.player2Id).getName());
				godSayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverMarriedCouple);
				return true;
			}
		}
		if (this.random.nextInt(1 + 1000 / getVerbosityForGod(godName)) == 0)
		{
			if ((ctx.believerManager().hasRecentPrayer(believer.getUniqueId())) && (this.random.nextInt(2) == 0))
			{
				return false;
			}
			godSayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverRandomNeutralSpeech);
			return true;
		}

		if (this.random.nextInt(1 + 600 / getVerbosityForGod(godName)) == 0)
		{
			if (godSayNeededSacrificeToBeliever(godName, believer.getUniqueId()))
			{
				return true;
			}
		}
		return false;
	}

	private boolean manageBelieverForPleasedGod(String godName, Player believer)
	{
		if (believer == null)
		{
			return false;
		}

		if (!ctx.plugin().isEnabledInWorld(believer.getWorld()))
		{
			return false;
		}

		if (believer.getGameMode() != GameMode.CREATIVE && ctx.permissionsManager().hasPermission(believer, "gods.itemblessings"))
		{
			if (!ctx.believerManager().hasRecentItemBlessing(believer.getUniqueId()))
			{
				if (ctx.settings().itemBlessings().enabled())
				{
					float power = getGodPower(godName);
					if ((power >= ctx.settings().itemBlessings().minGodPower()) && (this.random.nextInt((int) (1.0F + 100.0F / power)) == 0))
					{
						double healing = getHealthNeed(godName, believer);
						if ((healing > 1.0D) && (this.random.nextInt(2) == 0))
						{
							healPlayer(godName, believer);

							ctx.believerManager().setItemBlessingTime(believer.getUniqueId());

							return true;
						}

						ItemStack blessedItem = blessPlayerWithItem(godName, believer);

						if (blessedItem != null)
						{
							ctx.languageManager().setPlayerName(believer.getDisplayName());
							try
							{
								ctx.languageManager().setType(ctx.languageManager().getItemTypeName(blessedItem.getType()));
							}
							catch (Exception ex)
							{
								ctx.plugin().logDebug(ex.getMessage());
							}
							ctx.believerManager().setItemBlessingTime(believer.getUniqueId());

							return true;
						}
					}
				}
			}
		}

		if ((ctx.settings().marriage().enabled()) && (this.random.nextInt(501) == 0))
		{
			List<MarriageManager.MarriedCouple> marriedCouples = ctx.marriageManager().getMarriedCouples();

			if (marriedCouples.size() > 0)
			{
				MarriageManager.MarriedCouple couple = marriedCouples.get(this.random.nextInt(marriedCouples.size()));

				ctx.languageManager().setPlayerName(ctx.plugin().getServer().getOfflinePlayer(couple.player1Id).getName() + " and " + ctx.plugin().getServer().getOfflinePlayer(couple.player2Id).getName());
				godSayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverMarriedCouple);
				return true;
			}
		}

		if (this.random.nextInt(1 + 1000 / getVerbosityForGod(godName)) == 0)
		{
			if ((ctx.believerManager().hasRecentPrayer(believer.getUniqueId())) && (this.random.nextInt(2) == 0))
			{
				return false;
			}

			godSayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverRandomPleasedSpeech);

			return true;
		}

		if (this.random.nextInt(1 + 600 / getVerbosityForGod(godName)) == 0)
		{
			if (godSayNeededSacrificeToBeliever(godName, believer.getUniqueId()))
			{
				return true;
			}
		}
		return false;
	}

	private void manageBelievers(String godName)
	{
		Set<UUID> believers = ctx.believerManager().getOnlineBelieversForGod(godName);
		Set<UUID> managedBelievers = new HashSet<UUID>();
		if (believers.size() == 0)
		{
			return;
		}

		GodMood godMood = getMoodForGod(godName);

		List<UUID> priests = getPriestsForGod(godName);
		for (int n = 0; n < 10; n++)
		{
			UUID believerId = (UUID) believers.toArray()[this.random.nextInt(believers.size())];

			if (!managedBelievers.contains(believerId))
			{
				String priestName = null;
				if (!priests.isEmpty())
				{
					priestName = ctx.plugin().getServer().getOfflinePlayer(priests.get(this.random.nextInt(priests.size()))).getName();
				}
				ctx.languageManager().setPlayerName(priestName != null ? priestName : "our priest");

				Player believer = ctx.plugin().getServer().getPlayer(believerId);

				switch (godMood)
				{
					case EXALTED: manageBelieverForExaltedGod(godName, believer); break;
					case PLEASED: manageBelieverForPleasedGod(godName, believer); break;
					case NEUTRAL: manageBelieverForNeutralGod(godName, believer); break;
					case DISPLEASED: manageBelieverForDispleasedGod(godName, believer); break;
					case ANGRY: manageBelieverForAngryGod(godName, believer);
				}

				managedBelievers.add(believerId);
			}
		}
	}

	private void manageBlessings(String godName)
	{
		if (!ctx.settings().blessings().enabled())
		{
			return;
		}
		UUID blessedPlayer = getBlessedPlayerForGod(godName);
		if (blessedPlayer == null)
		{
			return;
		}

		int godPower = 1 + (int) getGodPower(godName);

		if (this.random.nextInt(1 + 100 / godPower) == 0)
		{
			Player player = ctx.plugin().getServer().getPlayer(blessedPlayer);

			if ((player == null) || (!ctx.permissionsManager().hasPermission(player, "gods.blessings")))
			{
				return;
			}

			if (blessPlayer(godName, player.getUniqueId(), getGodPower(godName)))
			{
				ctx.languageManager().setPlayerName(player.getName());

				GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToPlayerBlessed, 2 + this.random.nextInt(10));

				GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPlayerBlessed, player.getUniqueId());
			}
		}
	}

	private void manageCurses(String godName)
	{
		if (!ctx.settings().curses().enabled())
		{
			return;
		}

		Player cursedPlayer = getCursedPlayerForGod(godName);

		if (cursedPlayer == null)
		{
			return;
		}

		int godPower = 1 + (int) this.getGodPower(godName);

		if (this.random.nextInt(1 + 100 / godPower) == 0)
		{
			if (!ctx.permissionsManager().hasPermission(cursedPlayer, "gods.curses"))
			{
				return;
			}

			if (cursePlayer(godName, cursedPlayer.getUniqueId(), godPower))
			{
				ctx.languageManager().setPlayerName(cursedPlayer.getDisplayName());

				GodSay(godName, cursedPlayer, LanguageManager.LANGUAGESTRING.GodToPlayerCursed, 2 + this.random.nextInt(10));

				GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPlayerCursed, cursedPlayer.getUniqueId());
			}
		}
	}

	private void manageHolyLands()
	{
		if (!ctx.settings().holyLand().enabled())
		{
			return;
		}
		if (this.random.nextInt(1000) > 0)
		{
			return;
		}
		
		// Holy powers effects
		
		// Mobs: Spawn types of mob in random radius
		// Nature: Grow/Spawn type of sapling/vine/flower in random radius
		// Battle: Spawn swords, bows & armour in random block
		// Love: Spawn luck/food/Health/Gold/
		// Sea: Spawn fish in sea within radius
		
		ctx.holyLandManager().removeAbandonedLands();
	}
	
	private void manageLostBelievers(String godName)
	{
		if (this.random.nextInt(100) > 0)
		{
			return;
		}

		Set<UUID> believers = ctx.believerManager().getBelieversForGod(godName);
		Set<UUID> managedBelievers = new HashSet<UUID>();

		if (believers.size() == 0)
		{
			return;
		}

		ctx.plugin().logDebug("Managing lost believers for " + godName);

		for (int n = 0; n < 5; n++)
		{
			UUID believerId = (UUID) believers.toArray()[this.random.nextInt(believers.size())];
			if (!managedBelievers.contains(believerId))
			{
				long timeDiff = System.currentTimeMillis() - ctx.believerManager().getLastPrayerTime(believerId).getTime();

				if (timeDiff > 3600000L * ctx.settings().believers().maxHoursWithoutPrayer())
				{
					String believerName = ctx.plugin().getServer().getOfflinePlayer(believerId).getName();
					if (believerName != null)
					{
						ctx.languageManager().setPlayerName(believerName);

						godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversLostBeliever, 2 + this.random.nextInt(100));
					}

					ctx.believerManager().removeBeliever(godName, believerId);
				}
			}

			managedBelievers.add(believerId);
		}
	}

	private void manageMood(String godName)
	{
		if (ctx.believerManager().getOnlineBelieversForGod(godName).size() == 0)
		{
			return;
		}
		this.addMoodForGod(godName, this.getFalloffModifierForGod(godName));
	}

	public boolean managePriests(String godName)
	{
		int numberOfBelievers = ctx.believerManager().getBelieversForGod(godName).size();

		List<UUID> priestNames = getPriestsForGod(godName);

		if (priestNames == null)
		{
			priestNames = new ArrayList<UUID>();
		}

		if (numberOfBelievers < ctx.settings().priests().minBelievers() + 6 * priestNames.size())
		{
			return false;
		}

		if (priestNames.size() < ctx.settings().priests().maxPerGod())
		{
			if (this.random.nextInt(3) == 0)
			{
				ctx.plugin().logDebug(godName + " has too few priests. Finding one...");

				UUID believerId = getNextBelieverForPriest(godName);
				if (believerId == null)
				{
					ctx.plugin().logDebug(godName + " could not find a candidate for a priest");
					return false;
				}

				Player player = ctx.plugin().getServer().getPlayer(believerId);

				if (player == null)
				{
					return false;
				}

				if (setPendingPriest(godName, believerId))
				{
					ctx.plugin().log(godName + " offered " + player.getName() + " to be priest");
					ctx.languageManager().setPlayerName(player.getName());

					GodSayWithQuestion(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverOfferPriest, 2);

					return true;
				}
			}
		}

		for (UUID priestId : priestNames)
		{
			if (this.random.nextInt(1 + 1000 / getVerbosityForGod(godName)) == 0)
			{
				Player player = ctx.plugin().getServer().getPlayer(priestId);

				if (player != null)
				{
					ctx.languageManager().setPlayerName(player.getDisplayName());

					// Remind the priests of a commandment, or of the holy book when bibles are on
					int r = ctx.settings().bibles().enabled() ? this.random.nextInt(2) : 0;

					try
					{
						if (r == 0)
						{
							switch (this.random.nextInt(4))
							{
							case 0:
								ctx.languageManager().setType(ctx.languageManager().getItemTypeName(getHolyFoodTypeForGod(godName)));
								GodSayToPriest(godName, LanguageManager.LANGUAGESTRING.GodToPriestEatFoodType);
								break;
							case 1:
								ctx.languageManager().setType(ctx.languageManager().getItemTypeName(getUnholyFoodTypeForGod(godName)));
								GodSayToPriest(godName, LanguageManager.LANGUAGESTRING.GodToPriestNotEatFoodType);
								break;
							case 2:
								ctx.languageManager().setType(ctx.languageManager().getMobTypeName(getUnholyMobTypeForGod(godName)));
								GodSayToPriest(godName, LanguageManager.LANGUAGESTRING.GodToPriestSlayMobType);
								break;
							default:
								ctx.languageManager().setType(ctx.languageManager().getMobTypeName(getHolyMobTypeForGod(godName)));
								GodSayToPriest(godName, LanguageManager.LANGUAGESTRING.GodToPriestNotSlayMobType);
							}
						}
						else
						{
							ctx.languageManager().setType(ctx.holyBookManager().getBibleTitle(godName));
							GodSayToPriest(godName, LanguageManager.LANGUAGESTRING.GodToPriestUseBible);
						}
						return true;
					}
					catch (Exception ex)
					{
						ctx.plugin().logDebug(ex.getMessage());
					}
				}
			}
		}
		return false;
	}

	private void manageQuests(String godName)
	{
		if (!ctx.settings().quests().enabled())
		{
			return;
		}

		int numberOfBelievers = ctx.believerManager().getOnlineBelieversForGod(godName).size();

		if (!ctx.questManager().hasQuest(godName))
		{
			if (numberOfBelievers < ctx.settings().quests().requiredBelievers() || this.getMinutesSinceLastQuest(godName) < ctx.settings().quests().minMinutesBetweenQuests())
			{
				return;
			}

			ctx.questManager().generateQuest(godName);
		}
		else if (ctx.questManager().hasExpiredQuest(godName))
		{
			addMoodForGod(godName, getAngryModifierForGod(godName));

			ctx.questManager().godSayFailed(godName);

			ctx.questManager().removeFailedQuestForGod(godName);
		}
		else if (random.nextInt(5) == 0)
		{
			ctx.questManager().godSayStatus(godName);
		}
	}

	private void manageSacrifices()
	{
		if (!ctx.settings().sacrifices().enabled())
		{
			return;
		}

		if (this.random.nextInt(10) > 0)
		{
			return;
		}

		ctx.altarManager().clearDroppedItems();
	}

	private void manageSacrifices(String godName)
	{
		if (!ctx.settings().sacrifices().enabled())
		{
			return;
		}

		int godPower = 1 + (int) this.getGodPower(godName);
		if (this.random.nextInt(20 + (int) (70.0F / godPower)) > 0)
		{
			return;
		}
		
		Material type = getSacrificeNeedForGod(godName);

		float value = getSacrificeValueForGod(godName, type);

		value += 1 + this.random.nextInt(3);
		if (value > 64.0F)
		{
			value = 64.0F;
		}
		else if (value < -64.0F)
		{
			value = -64.0F;
		}
		ctx.plugin().logDebug("Increasing wanted " + type.name() + " sacrifice need for " + godName + " to " + value);

		setGodValue(godName, ".SacrificeValues." + type.name(), Float.valueOf(value));

		godsFile().markDirty();

		type = getSacrificeUnwantedForGod(godName);
		if (type != null)
		{
			value = 0.25F * getSacrificeValueForGod(godName, type);
			if (value > -0.5D)
			{
				value = 0.0F;
			}
			ctx.plugin().logDebug("Reducing unwanted " + type.name() + " sacrifice need for " + godName + " to " + value);
			if (value == 0.0F)
			{
				setGodValue(godName, ".SacrificeValues." + type.name(), null);
			}
			else
			{
				setGodValue(godName, ".SacrificeValues." + type.name(), Float.valueOf(value));
			}
			godsFile().save();
		}
	}

	public void removeGod(String godName)
	{
		if (godName == null)
		{
			return;
		}

		for (String otherGodName : getAllGods())
		{
			removeRelation(otherGodName, ".Allies", godName);
			removeRelation(otherGodName, ".Enemies", godName);
		}

		godsConfig().set(godName, null);

		if (ctx.settings().bibles().enabled())
		{
			ctx.holyBookManager().clearBible(godName);
		}

		if (ctx.settings().holyLand().enabled())
		{
			ctx.holyLandManager().removeHolyLandsForGod(godName);
		}

		godsFile().save();
	}

	public void removePriest(String godName, UUID playerId)
	{
		List<String> priests = godsConfig().getStringList(godName + ".Priests");

		if (priests.remove(playerId.toString()))
		{
			dispatchPriestCommand(ctx.settings().priests().removeCommand(), playerId);
		}

		setGodValue(godName, ".Priests", priests);
		ctx.chatManager().refresh(playerId);

		godsFile().markDirty();

		ctx.plugin().log(godName + " removed " + ctx.plugin().getServer().getOfflinePlayer(playerId).getName() + " as priest");
	}

	/**
	 * Removes otherGodName from the relation list (".Allies" or ".Enemies") of godName.
	 */
	private void removeRelation(String godName, String relation, String otherGodName)
	{
		List<String> gods = godsConfig().getStringList(godName + relation);
		if (gods.remove(otherGodName))
		{
			setGodValue(godName, relation, gods);
		}
	}

	public void sendInfoToBelievers(String godName, LanguageManager.LANGUAGESTRING message, ChatColor color, int delay)
	{
		for (UUID playerId : ctx.believerManager().getBelieversForGod(godName))
		{
			Player player = ctx.plugin().getServer().getPlayer(playerId);

			if (player != null)
			{
				ctx.plugin().sendInfo(playerId, message, color, 0, "", 10);
			}
		}
	}

	public void sendInfoToBelievers(String godName, LanguageManager.LANGUAGESTRING message, ChatColor color, String name, int amount1, int amount2, int delay)
	{
		for (UUID playerId : ctx.believerManager().getBelieversForGod(godName))
		{
			Player player = ctx.plugin().getServer().getPlayer(playerId);
			if (player != null)
			{
				ctx.plugin().sendInfo(playerId, message, color, name, amount1, amount2, 10);
			}
		}
	}

	/**
	 * Makes the player the god's blessed player from now on, or clears it when believerId is null.
	 */
	public void setBlessedPlayerForGod(String godName, UUID believerId)
	{
		// Stored as a string: a UUID object is written with a !!java.util.UUID tag Bukkit can't load
		setGodValue(godName, ".BlessedPlayer", believerId == null ? null : believerId.toString());
		setGodValue(godName, ".BlessedTime", believerId == null ? null : formatter.format(new Date()));

		godsFile().markDirty();
	}

	public void setContestedHolyLandForGod(String godName, Location contestedLand)
	{
		setGodValue(godName, ".ContestedLand.Hash", Long.valueOf(ctx.holyLandManager().hashLocation(contestedLand)));

		setGodValue(godName, ".ContestedLand" + ".X", Integer.valueOf(contestedLand.getBlockX()));
		setGodValue(godName, ".ContestedLand" + ".Y", Integer.valueOf(contestedLand.getBlockY()));
		setGodValue(godName, ".ContestedLand" + ".Z", Integer.valueOf(contestedLand.getBlockZ()));
		setGodValue(godName, ".ContestedLand" + ".World", contestedLand.getWorld().getName());

		ctx.holyLandManager().setContestedLand(contestedLand, godName);

		godsFile().markDirty();
	}

	/**
	 * Makes the player the god's cursed player from now on, or clears it when believerId is null.
	 */
	public void setCursedPlayerForGod(String godName, UUID believerId)
	{
		// Stored as a string: a UUID object is written with a !!java.util.UUID tag Bukkit can't load
		setGodValue(godName, ".CursedPlayer", believerId == null ? null : believerId.toString());
		setGodValue(godName, ".CursedTime", believerId == null ? null : formatter.format(new Date()));

		godsFile().markDirty();
	}

	public void setDivineForceForGod(String godName, GodType divineForce)
	{
		setGodValue(godName, ".DivineForce", divineForce.name().toUpperCase());

		godsFile().save();
	}

	public void setGenderForGod(String godName, GodGender godGender)
	{
		setGodValue(godName, ".Gender", godGender.name());

		godsFile().markDirty();
	}

	public void setGodDescription(String godName, String description)
	{
		setGodValue(godName, ".Description", description);

		godsFile().markDirty();
	}

	public void setGodPvP(String godName, boolean pvp)
	{
		setGodValue(godName, ".PvP", Boolean.valueOf(pvp));

		godsFile().markDirty();
	}

	public void setHomeForGod(String godName, Location location)
	{
		setGodValue(godName, ".Home.X", Double.valueOf(location.getX()));
		setGodValue(godName, ".Home.Y", Double.valueOf(location.getY()));
		setGodValue(godName, ".Home.Z", Double.valueOf(location.getZ()));
		setGodValue(godName, ".Home.World", location.getWorld().getName());

		godsFile().markDirty();
	}

	public boolean setPendingPriest(String godName, UUID believerId)
	{
		if (believerId == null)
		{
			return false;
		}

		// Offer the priesthood at most once every 3 minutes
		String lastPriestTime = godsConfig().getString(godName + ".PendingPriestTime");
		if (lastPriestTime != null)
		{
			try
			{
				long diffMinutes = (System.currentTimeMillis() - formatter.parse(lastPriestTime).getTime()) / 60000L;
				if (diffMinutes < 3L)
				{
					return false;
				}
			}
			catch (Exception ex)
			{
				ctx.plugin().logDebug("Could not parse PendingPriestTime '" + lastPriestTime + "' for " + godName);
			}
		}

		setGodValue(godName, ".PendingPriest", believerId.toString());
		setGodValue(godName, ".PendingPriestTime", formatter.format(new Date()));

		godsFile().markDirty();

		ctx.believerManager().setPendingPriest(believerId);

		return true;
	}

	public void setPrivateAccess(String godName, boolean privateAccess)
	{
		setGodValue(godName, ".PrivateAccess", Boolean.valueOf(privateAccess));

		godsFile().markDirty();
	}

	public void setTimeSinceLastQuest(String godName)
	{
		setGodValue(godName, ".LastQuestTime", formatter.format(new Date()));

		godsFile().markDirty();
	}

	public void spawnHostileMobs(String godName, Player player, EntityType mobType, int numberOfMobs)
	{
		ctx.plugin().getServer().getScheduler().runTaskLater(ctx.plugin(), new TaskSpawnHostileMobs(ctx, godName, player, mobType, numberOfMobs), 2L);
	}

	public boolean strikePlayerWithLightning(UUID playerId, int damage)
	{
		Player player = ctx.plugin().getServer().getPlayer(playerId);

		if (player != null)
		{
			if (damage <= 0)
			{
				player.getWorld().strikeLightningEffect(player.getLocation());
			}
			else
			{
				LightningStrike strike = player.getWorld().strikeLightning(player.getLocation());
				player.damage(damage - 1, strike);
			}
		}
		return true;
	}

	public boolean strikePlayerWithMobs(String godName, UUID playerId, float godPower)
	{
		Player player = ctx.plugin().getServer().getPlayer(playerId);

		if (player == null)
		{
			ctx.plugin().logDebug("player is null");
			return false;
		}

		EntityType mobType;

		switch (this.random.nextInt(5))
		{
		case 0:
			mobType = EntityType.SKELETON;
			break;
		case 1:
			mobType = EntityType.ZOMBIE;
			break;
		case 2:
			mobType = EntityType.ZOMBIFIED_PIGLIN;
			break;
		case 3:
			mobType = EntityType.SPIDER;
			break;
		default:
			mobType = EntityType.WOLF;
			break;
		}
		int numberOfMobs = 1 + (int) (godPower / 10.0F);

		spawnHostileMobs(godName, player, mobType, numberOfMobs);

		return true;
	}

	/**
	 * Adds otherGodName to the relation list (".Allies" or ".Enemies") of godName.
	 */
	private void addRelation(String godName, String relation, String otherGodName)
	{
		List<String> gods = godsConfig().getStringList(godName + relation);
		if (!gods.contains(otherGodName))
		{
			gods.add(otherGodName);
			setGodValue(godName, relation, gods);
		}
	}

	/**
	 * Makes the two gods allies (ending any war between them), or ends their alliance if they are allies.
	 *
	 * @return true if they are allies now
	 */
	public boolean toggleAllianceRelationForGod(String godName, String allyGodName)
	{
		if (!hasAllianceRelation(godName, allyGodName))
		{
			addRelation(godName, ".Allies", allyGodName);
			addRelation(allyGodName, ".Allies", godName);
			removeRelation(godName, ".Enemies", allyGodName);
			removeRelation(allyGodName, ".Enemies", godName);
			godsFile().markDirty();

			return true;
		}

		removeRelation(godName, ".Allies", allyGodName);
		removeRelation(allyGodName, ".Allies", godName);
		godsFile().save();

		return false;
	}

	/**
	 * Puts the two gods at war (ending any alliance between them), or ends their war if they are at war.
	 *
	 * @return true if they are at war now
	 */
	public boolean toggleWarRelationForGod(String godName, String enemyGodName)
	{
		if (!hasWarRelation(godName, enemyGodName))
		{
			addRelation(godName, ".Enemies", enemyGodName);
			addRelation(enemyGodName, ".Enemies", godName);
			removeRelation(godName, ".Allies", enemyGodName);
			removeRelation(enemyGodName, ".Allies", godName);
			godsFile().markDirty();

			return true;
		}

		removeRelation(godName, ".Enemies", enemyGodName);
		removeRelation(enemyGodName, ".Enemies", godName);
		godsFile().save();

		return false;
	}

	public void update()
	{
		if (this.random.nextInt(50) == 0)
		{
			ctx.plugin().logDebug("Processing dead offline Gods...");

			long timeBefore = System.currentTimeMillis();

			List<String> godNames = getOfflineGods();
			for (String offlineGodName : godNames)
			{
				if (isDeadGod(offlineGodName))
				{
					ctx.plugin().log("Removed dead offline God '" + offlineGodName + "'");
				}
			}
			long timeAfter = System.currentTimeMillis();

			ctx.plugin().logDebug("Processed " + godNames.size() + " offline Gods in " + (timeAfter - timeBefore) + " ms");
		}

		List<String> godNames = getOnlineGods();

		long timeBefore = System.currentTimeMillis();

		if (godNames.size() == 0)
		{
			return;
		}
		String godName = (String) godNames.toArray()[this.random.nextInt(godNames.size())];

		ctx.plugin().logDebug("Processing God '" + godName + "'");

		manageMood(godName);

		boolean godTalk = managePriests(godName);

		manageLostBelievers(godName);

		if (!godTalk)
		{
			manageBelievers(godName);
		}

		if (!godTalk)
		{
			manageQuests(godName);
		}

		manageBlessings(godName);

		manageCurses(godName);

		manageSacrifices(godName);

		manageSacrifices();

		manageHolyLands();

		long timeAfter = System.currentTimeMillis();

		ctx.plugin().logDebug("Processed 1 Online God in " + (timeAfter - timeBefore) + " ms");
	}

	public void updateOnlineGods()
	{
		this.onlineGods.clear();
		for (Player player : ctx.plugin().getServer().getOnlinePlayers())
		{
			String godName = ctx.believerManager().getGodForBeliever(player.getUniqueId());
			if (godName != null)
			{
				if (!this.onlineGods.contains(godName))
				{
					this.onlineGods.add(godName);
				}
			}
		}
	}
}
