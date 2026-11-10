package com.dogonfire.gods.managers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.model.God;
import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.model.GodMood;
import com.dogonfire.gods.model.GodType;
import com.dogonfire.gods.model.MarriedCouple;
import com.dogonfire.gods.service.GodSpeech;
import com.dogonfire.gods.storage.GodRepository;
import com.dogonfire.gods.tasks.TaskGiveHolyArtifact;
import com.dogonfire.gods.tasks.TaskGiveItem;
import com.dogonfire.gods.tasks.TaskHealPlayer;
import com.dogonfire.gods.tasks.TaskSpawnHostileMobs;
import com.dogonfire.gods.util.LegacyDates;

public class GodManager
{
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

			float power1 = ctx.believerService().getBelieverPower(c1.believerId);
			float power2 = ctx.believerService().getBelieverPower(c2.believerId);

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

	private final GodsContext ctx;

	public GodManager(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	/** gods.yml; see {@link GodRepository} */
	private GodRepository gods()
	{
		return ctx.storage().gods();
	}

	/** The god with this name, or null */
	private God god(String godName)
	{
		return gods().find(godName);
	}

	/**
	 * The god to change, created if there is none (setting a value in gods.yml always did). Null for a null god name,
	 * so a lookup for a player without a god can't create a god called "null".
	 */
	private God godToChange(String godName, String what)
	{
		if (godName == null)
		{
			ctx.plugin().logDebug("Not writing " + what + " for a null god name");
			return null;
		}
		return gods().getOrCreate(godName);
	}

	private GodSpeech speech()
	{
		return ctx.godSpeech();
	}

	private static Instant now()
	{
		return LegacyDates.SECONDS.now();
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

			speech().say(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverAltarBuilt, 2 + this.random.nextInt(30));

			return true;
		}

		return false;
	}

	private boolean addBelief(Player player, String godName, boolean allowChangeGod)
	{
		String oldGodName = ctx.believerService().getGodForBeliever(player.getUniqueId());

		if (godName == null)
		{
			ctx.godSpeech().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.InvalidGodName, ChatColor.RED, 0, "", 1);
			return false;
		}

		if (oldGodName != null && !oldGodName.equals(godName))
		{
			if (!allowChangeGod)
			{
				ctx.believerService().setChangingGod(player.getUniqueId());

				ctx.godSpeech().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.ConfirmChangeToOtherReligion, ChatColor.YELLOW, 0, oldGodName, 1);
				return false;
			}

			if (ctx.believerService().hasRecentGodChange(player.getUniqueId()))
			{
				ctx.godSpeech().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.CannotChangeGodSoSoon, ChatColor.RED, 0, "", 1);
				return false;
			}

			ctx.believerService().clearChangingGod(player.getUniqueId());
		}

		if (!ctx.believerService().addPrayer(player.getUniqueId(), godName))
		{
			int timeUntilCanPray = ctx.believerService().getTimeUntilCanPray(player.getUniqueId());

			ctx.godSpeech().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.CannotPraySoSoon, ChatColor.RED, timeUntilCanPray, "", 1);
			return false;
		}

		if (oldGodName != null && !oldGodName.equals(godName))
		{
			if (isPriestForGod(player.getUniqueId(), oldGodName))
			{
				removePriest(oldGodName, player.getUniqueId());
			}

			ctx.languageManager().setPlayerName(player.getName());

			speech().sayToBelievers(oldGodName, LanguageManager.LANGUAGESTRING.GodToBelieversPlayerLeftReligion, 2 + this.random.nextInt(20));

			ctx.godSpeech().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.YouLeftReligion, ChatColor.RED, 0, oldGodName, 20);

			speech().sayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPlayerJoinedReligion, player.getUniqueId());

			ctx.believerService().clearPrayerPower(player.getUniqueId());
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

			ctx.believerService().increasePrayerPower(player.getUniqueId(), 1);
		}

		if (oldGodName == null || !oldGodName.equals(godName))
		{
			if (ctx.settings().marriage().enabled())
			{
				ctx.marriageService().divorce(player.getUniqueId());
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
		for (UUID playerId : ctx.believerService().getBelieversForGod(godName))
		{
			Player player = ctx.plugin().getServer().getPlayer(playerId);

			if (player == null)
			{
				continue;
			}

			ctx.believerService().incPrayer(player.getUniqueId(), godName);

			List<ItemStack> rewards = ctx.questManager().getRewardsForQuestCompletion(godName);

			for (ItemStack items : rewards)
			{
				giveItem(godName, player, items.getType(), false);
			}
		}
	}

	private boolean addBeliefByAltar(Player player, String godName, Location prayerLocation, boolean allowChangeGod)
	{
		if (!ctx.godService().godExist(godName))
		{
			if (!player.isOp() && (!ctx.permissionsManager().hasPermission(player, "gods.god.create")))
			{
				ctx.godSpeech().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.CreateGodNotAllowed, ChatColor.RED, 0, "", 20);
				return false;
			}

			Block altarBlock = ctx.altarService().getAltarBlockFromSign(prayerLocation.getBlock());

			GodGender godGender = ctx.altarService().getGodGenderFromAltarBlock(altarBlock);

			ctx.plugin().logDebug("Altar is " + altarBlock.getType().name());

			GodType godType = ctx.altarService().getGodTypeForAltarBlockType(altarBlock.getType());

			ctx.plugin().logDebug("God divine force is " + godType);

			ctx.godService().createGod(godName, player.getLocation(), godGender, godType);

			if (ctx.settings().gods().broadcastNewGods())
			{
				ctx.plugin().getServer().broadcastMessage(ChatColor.WHITE + player.getName() + ChatColor.AQUA + " started to believe in the " + ctx.languageManager().getGodGenderName(ctx.godService().getGenderForGod(godName)) + " " + ChatColor.GOLD + godName);
			}

			ctx.plugin().log(player.getName() + " created new god " + godName);
		}

		return addBelief(player, godName, allowChangeGod);
	}

	public boolean assignPriest(String godName, UUID playerId)
	{
		God existing = god(godName);
		if (existing != null)
		{
			existing.setPendingPriest(null);
		}
		ctx.believerService().clearPendingPriest(playerId);

		Set<UUID> believers = ctx.believerService().getBelieversForGod(godName);
		if (believers.contains(playerId))
		{
			God god = godToChange(godName, "Priests");
			if (god == null)
			{
				return false;
			}

			if (god.priests().contains(playerId))
			{
				ctx.plugin().log(playerId.toString() + " is already a priest of " + godName);
			}
			else
			{
				god.priests().add(playerId);
				dispatchPriestCommand(ctx.settings().priests().assignCommand(), playerId);
			}

			ctx.chatManager().refresh(playerId);

			god.setPendingPriest(null);
			god.setPendingPriestTime(null);

			ctx.believerService().setLastPrayerDate(playerId);

			gods().markDirty();
			return true;
		}
		else
		{
			return false;
		}
	}

	public void believerAccept(UUID believerId)
	{
		String godName = ctx.believerService().getGodForBeliever(believerId);

		Player player = ctx.plugin().getServer().getPlayer(believerId);
		if (player == null)
		{
			ctx.plugin().logDebug("believerAccept(): player is null for " + believerId);
			return;
		}

		ctx.languageManager().setPlayerName(player.getName());
		if (ctx.settings().marriage().enabled())
		{
			UUID pendingMarriagePartner = ctx.marriageService().getProposal(believerId);

			if (pendingMarriagePartner != null)
			{
				ctx.plugin().log(player.getName() + " accepted the proposal to marry " + pendingMarriagePartner);

				ctx.marriageService().handleAcceptProposal(believerId, pendingMarriagePartner, godName);

				return;
			}
		}
	
		String pendingGodInvitation = ctx.believerService().getInvitation(believerId);
		if (pendingGodInvitation != null)
		{
			ctx.plugin().logDebug("pendingGodInvitation is " + pendingGodInvitation);
			if (addBelief(player, pendingGodInvitation, true))
			{
				ctx.believerService().clearInvitation(believerId);

				ctx.plugin().log(player.getName() + " accepted the invitation to join " + pendingGodInvitation);

				speech().say(pendingGodInvitation, player, LanguageManager.LANGUAGESTRING.GodToPlayerAcceptedInvitation, 2 + this.random.nextInt(40));
				speech().sayToBelieversExcept(pendingGodInvitation, LanguageManager.LANGUAGESTRING.GodToBelieversNewPlayerAccepted, player.getUniqueId());
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
				gods().markDirty();

				ctx.plugin().log(player.getName() + " accepted the offer from " + godName + " to be priest");

				ctx.godSpeech().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.InviteHelp, ChatColor.AQUA, ChatColor.WHITE + "/gods invite <playername>", ChatColor.WHITE + "/gods invite <playername>", 100);
				ctx.godSpeech().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.FollowersHelp, ChatColor.AQUA, ChatColor.WHITE + "/gods followers", ChatColor.WHITE + "/gods followers", 200);
				ctx.godSpeech().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.DescriptionHelp, ChatColor.AQUA, ChatColor.WHITE + "/gods desc", ChatColor.WHITE + "/gods desc", 300);

				if (ctx.settings().holyArtifacts().enabled())
				{
					ctx.godSpeech().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.AttackHelp, ChatColor.AQUA, ChatColor.WHITE + "/gods startattack", ChatColor.WHITE + "/gods startattack", 300);
				}
				try
				{
					speech().say(godName, player, LanguageManager.LANGUAGESTRING.GodToPriestPriestAccepted, 2 + this.random.nextInt(40));
					speech().sayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPriestAccepted, player.getUniqueId());
				}
				catch (Exception ex)
				{
					ctx.plugin().log("ERROR: Could not say GodToPriestPriestAccepted text! " + ex.getMessage());
				}
				return;
			}
		}

		ctx.plugin().logDebug(player.getDisplayName() + " did not have anything to accepted from " + godName);
		speech().say(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverNoQuestion, 2 + this.random.nextInt(20));
	}

	public boolean believerLeaveGod(UUID believerId)
	{
		String godName = ctx.believerService().getGodForBeliever(believerId);
		if (godName == null)
		{
			return false;
		}

		if (isPriestForGod(believerId, godName))
		{
			removePriest(godName, believerId);
		}
		ctx.believerService().believerLeave(godName, believerId);

		ctx.languageManager().setPlayerName(ctx.plugin().getServer().getPlayer(believerId).getDisplayName());

		if (ctx.settings().marriage().enabled())
		{
			ctx.marriageService().divorce(believerId);
		}

		ctx.believerService().clearPrayerPower(believerId);

		speech().sayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPlayerLeftReligion, 2 + this.random.nextInt(20));

		return true;
	}

	public void believerReject(UUID believerId)
	{
		String godName = ctx.believerService().getGodForBeliever(believerId);
		Player player = ctx.plugin().getServer().getPlayer(believerId);
		if (player == null)
		{
			return;
		}

		ctx.languageManager().setPlayerName(player.getName());

		String pendingGodInvitation = ctx.believerService().getInvitation(believerId);
		if (pendingGodInvitation != null)
		{
			ctx.believerService().clearInvitation(believerId);

			ctx.plugin().log(player.getName() + " rejected the invitation to join " + pendingGodInvitation);

			ctx.godSpeech().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.RejectedJoinOffer, ChatColor.RED, 0, pendingGodInvitation, 20);

			return;
		}

		if (godName == null)
		{
			return;
		}

		UUID pendingPriest = getPendingPriest(godName);

		if (pendingPriest == null)
		{
			speech().say(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverNoQuestion, 2 + this.random.nextInt(20));
			return;
		}

		if (pendingPriest.equals(believerId))
		{
			god(godName).setPendingPriest(null);

			ctx.believerService().clearPendingPriest(believerId);

			ctx.plugin().log(player.getName() + " rejected the offer from " + godName + " to be priest");

			speech().say(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverPriestRejected, 2 + this.random.nextInt(20));

			gods().markDirty();
		}
	}

	public boolean blessPlayer(String godName, UUID playerId, float godPower)
	{
		Player player = ctx.plugin().getServer().getPlayer(playerId);

		if (player == null)
		{
			return false;
		}

		if (ctx.believerService().hasRecentBlessing(playerId))
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

		ctx.believerService().setBlessingTime(player.getUniqueId());

		return true;
	}

	public void blessPlayerWithHolyArtifact(String godName, Player player)
	{
		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return;
		}
		giveHolyArtifact(godName, ctx.godService().getDivineForceForGod(godName), player, true);
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

	public boolean cursePlayer(String godName, UUID playerId, float godPower)
	{
		Player player = ctx.plugin().getServer().getPlayer(playerId);

		if (player == null)
		{
			return false;
		}

		if (ctx.believerService().hasRecentCursing(playerId))
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

		ctx.believerService().setCursingTime(player.getUniqueId());

		return true;
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

	private Material getAxeBlessing(String godName)
	{
		if (ctx.godService().getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel3Items())
		{
			return Material.DIAMOND_AXE;
		}
		if (ctx.godService().getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel2Items())
		{
			return Material.IRON_AXE;
		}
		if (ctx.godService().getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel1Items())
		{
			return Material.STONE_AXE;
		}
		return Material.WOODEN_AXE;
	}

	public UUID getBlessedPlayerForGod(String godName)
	{
		God god = god(godName);
		if (god == null || god.blessedTime() == null)
		{
			return null;
		}

		// Blessing.MaxBlessingTime is in seconds (Cursing.MaxCursingTime is in minutes)
		long diffSeconds = (System.currentTimeMillis() - god.blessedTime().toEpochMilli()) / 1000L;
		UUID blessedPlayer = god.blessedPlayer();
		if (blessedPlayer == null || diffSeconds > ctx.settings().blessings().blessedSeconds())
		{
			setBlessedPlayerForGod(godName, null);

			return null;
		}
		return blessedPlayer;
	}

	public Player getCursedPlayerForGod(String godName)
	{
		God god = god(godName);
		if (god == null || god.cursedTime() == null)
		{
			return null;
		}

		// Cursing.MaxCursingTime is in minutes (Blessing.MaxBlessingTime is in seconds)
		long diffMinutes = (System.currentTimeMillis() - god.cursedTime().toEpochMilli()) / 60000L;
		UUID cursedPlayer = god.cursedPlayer();
		if (cursedPlayer == null || diffMinutes > ctx.settings().curses().cursedMinutes())
		{
			setCursedPlayerForGod(godName, null);

			return null;
		}

		return ctx.plugin().getServer().getPlayer(cursedPlayer);
	}

	public Material getHolyFoodTypeForGod(String godName)
	{
		God god = godToChange(godName, "EatFoodType");
		String foodTypeString = god != null ? god.holyFood() : null;
		
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

			if (god != null)
			{
				god.setHolyFood(foodTypeString);
			}

			gods().markDirty();
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

	private Material getFoodBlessing(String godName)
	{
		return getHolyFoodTypeForGod(godName);
	}

	public double getHealthNeed(String godName, Player player)
	{
		return player.getAttribute(Attribute.MAX_HEALTH).getValue() - player.getHealth();
	}

	private Material getHoeBlessing(String godName)
	{
		if (ctx.godService().getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel3Items())
		{
			return Material.DIAMOND_HOE;
		}
		if (ctx.godService().getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel2Items())
		{
			return Material.IRON_HOE;
		}
		if (ctx.godService().getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel1Items())
		{
			return Material.STONE_HOE;
		}
		return Material.WOODEN_HOE;
	}

	public EntityType getHolyMobTypeForGod(String godName)
	{
		God god = godToChange(godName, "NotSlayMobType");
		EntityType mobType = parseEntityType(god != null ? god.holyMob() : null);
		if (mobType == null)
		{
			String unholyMobTypeString = getUnholyMobTypeForGod(godName).name();
			String mobTypeString;
			do
			{
				mobTypeString = generateHolyMobTypeForGod();
			}
			while (mobTypeString.equals(unholyMobTypeString));

			if (god != null)
			{
				god.setHolyMob(mobTypeString);
			}
			gods().markDirty();

			mobType = EntityType.valueOf(mobTypeString);
		}
		return mobType;
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

	private UUID getNextBelieverForPriest(String godName)
	{
		Set<UUID> allBelievers = ctx.believerService().getBelieversForGod(godName);

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
						if (!ctx.believerService().hasRecentPriestOffer(candidate))
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
		God god = godToChange(godName, "NotEatFoodType");
		String foodTypeString = god != null ? god.unholyFood() : null;
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
			
			if (god != null)
			{
				god.setUnholyFood(foodTypeString);
			}

			gods().markDirty();
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

	public UUID getPendingPriest(String godName)
	{
		God god = god(godName);
		UUID believerId = god != null ? god.pendingPriest() : null;

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
		if (ctx.godService().getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel3Items())
		{
			return Material.DIAMOND_PICKAXE;
		}
		if (ctx.godService().getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel2Items())
		{
			return Material.IRON_PICKAXE;
		}
		if (ctx.godService().getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel1Items())
		{
			return Material.STONE_PICKAXE;
		}
		return Material.WOODEN_PICKAXE;
	}

	public List<UUID> getPriestsForGod(String godName)
	{
		God god = god(godName);
		List<UUID> list = new ArrayList<UUID>();
		if (god == null)
		{
			return list;
		}

		for (UUID believerId : new ArrayList<UUID>(god.priests()))
		{
			long diffHours = (System.currentTimeMillis() - ctx.believerService().getLastPrayerTime(believerId).getTime()) / 3600000L;
			if (diffHours > ctx.settings().priests().maxHoursWithoutPrayer())
			{
				String priestName = ctx.plugin().getServer().getOfflinePlayer(believerId).getName();
				ctx.languageManager().setPlayerName(priestName != null ? priestName : "our priest");
				speech().sayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversRemovedPriest, 2 + this.random.nextInt(40));
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

		God god = god(godName);
		if ((god == null) || (god.sacrificeValues().size() == 0))
		{
			return null;
		}
		Map<String, Float> values = god.sacrificeValues();
		for (int i = 0; i < values.size(); i++)
		{
			itemName = (String) values.keySet().toArray()[this.random.nextInt(values.size())];

			value = Integer.valueOf(values.get(itemName).intValue());
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
		Random materialRandom = new Random(ctx.godService().getSeedForGod(godName));
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
		God god = god(godName);
		if (god != null && !god.sacrificeValues().isEmpty())
		{
			for (String itemType : god.sacrificeValues().keySet())
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
				if (god.sacrificeValues().get(itemType) <= 0.0D)
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
		God god = god(godName);
		Float value = god != null ? god.sacrificeValues().get(type.name()) : null;
		return value != null ? value : 0;
	}

	/** Puts a sacrifice value; null removes it */
	private void setSacrificeValueForGod(String godName, Material type, Float value)
	{
		if (value == null)
		{
			God god = god(godName);
			if (god != null)
			{
				god.sacrificeValues().remove(type.name());
			}
			return;
		}
		God god = godToChange(godName, "SacrificeValues");
		if (god != null)
		{
			god.sacrificeValues().put(type.name(), value);
		}
	}

	private Material getSpadeBlessing(String godName)
	{
		if (ctx.godService().getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel3Items())
		{
			return Material.DIAMOND_SHOVEL;
		}
		if (ctx.godService().getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel2Items())
		{
			return Material.IRON_SHOVEL;
		}
		if (ctx.godService().getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel1Items())
		{
			return Material.STONE_SHOVEL;
		}
		return Material.WOODEN_SHOVEL;
	}

	private Material getSwordBlessing(String godName)
	{
		if (ctx.godService().getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel3Items())
		{
			return Material.DIAMOND_SWORD;
		}
		if (ctx.godService().getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel2Items())
		{
			return Material.IRON_SWORD;
		}
		if (ctx.godService().getGodPower(godName) > ctx.settings().itemBlessings().godPowerForLevel1Items())
		{
			return Material.STONE_SWORD;
		}
		return Material.WOODEN_SWORD;
	}

	public EntityType getUnholyMobTypeForGod(String godName)
	{
		God god = godToChange(godName, "SlayMobType");
		EntityType mobType = parseEntityType(god != null ? god.unholyMob() : null);
		if (mobType == null)
		{
			String mobTypeString = generateUnholyMobTypeForGod();

			if (god != null)
			{
				god.setUnholyMob(mobTypeString);
			}
			gods().markDirty();

			mobType = EntityType.valueOf(mobTypeString);
		}
		return mobType;
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

				speech().sayToBeliever(godName, believerId, LanguageManager.LANGUAGESTRING.GodToBelieversSacrificeItemType);

				return true;
			}
		}
		return false;
	}

	public boolean handleAltarPray(Location location, Player player, String godName)
	{
		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return false;
		}

		if (addBeliefByAltar(player, godName, location, ctx.believerService().getChangingGod(player.getUniqueId())))
		{
			Block altarBlock = ctx.altarService().getAltarBlockFromSign(player.getWorld().getBlockAt(location));

			if (ctx.godService().getGenderForGod(godName) == GodGender.None)
			{
				GodGender godGender = ctx.altarService().getGodGenderFromAltarBlock(altarBlock);

				ctx.plugin().logDebug("God did not have a gender, setting gender to " + godGender);

				ctx.godService().setGenderForGod(godName, godGender);
			}
			if (ctx.godService().getDivineForceForGod(godName) == null)
			{
				GodType godType = ctx.altarService().getGodTypeForAltarBlockType(altarBlock.getType());

				ctx.plugin().logDebug("God did not have a divine force, setting divine force to " + godType);

				ctx.godService().setDivineForceForGod(godName, godType);
			}

			ctx.moodService().addMoodForGod(godName, ctx.moodService().getPleasedModifierForGod(godName));

			if ((ctx.settings().holyLand().enabled()) && (ctx.permissionsManager().hasPermission(player, "gods.holyland")))
			{
				ctx.holyLandService().addPrayer(player, godName, altarBlock.getLocation());
			}

			ctx.questManager().handlePrayer(godName, player.getUniqueId());

			ctx.languageManager().setPlayerName(player.getName());

			speech().say(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverPraying, 2 + this.random.nextInt(10));
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
			ctx.moodService().addMoodForGod(godName, ctx.moodService().getPleasedModifierForGod(godName));

			if (blessPlayer(godName, player.getUniqueId(), ctx.godService().getGodPower(godName)))
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
					speech().sayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversEatFoodBlessing, 2 + this.random.nextInt(20));
				}
				else
				{
					speech().sayToBeliever(godName, player.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieversEatFoodBlessing);
				}
			}
		}

		if (foodType.equals(notEatFoodType.name()))
		{
			ctx.moodService().addMoodForGod(godName, ctx.moodService().getAngryModifierForGod(godName));
			
			if (cursePlayer(godName, player.getUniqueId(), ctx.godService().getGodPower(godName)))
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
					speech().sayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversNotEatFoodCursing, 2 + this.random.nextInt(10));
				}
				else
				{
					speech().sayToBeliever(godName, player.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieversNotEatFoodCursing);
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
			if (blessPlayer(godName, player.getUniqueId(), ctx.godService().getGodPower(godName)))
			{
				ctx.moodService().addMoodForGod(godName, ctx.moodService().getPleasedModifierForGod(godName));

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
					speech().sayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSlayMobBlessing, 2 + this.random.nextInt(20));
				}
				else
				{
					speech().sayToBeliever(godName, player.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieversSlayMobBlessing);
				}
			}
		}

		if ((holyMobType != null) && mobType.equals(holyMobType.name()))
		{
			if (cursePlayer(godName, player.getUniqueId(), ctx.godService().getGodPower(godName)))
			{
				ctx.moodService().addMoodForGod(godName, ctx.moodService().getAngryModifierForGod(godName));

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
					speech().sayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversNotSlayMobCursing, 2 + this.random.nextInt(10));
				}
				else
				{
					speech().sayToBeliever(godName, player.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieversNotSlayMobCursing);
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
			ctx.believerService().believerLeave(godName, playerId);
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

		int godPower = (int) ctx.godService().getGodPower(godName);

		ctx.plugin().log(believer.getDisplayName() + " sacrificed " + type.name() + " to " + godName);

		Material eatFoodType = getHolyFoodTypeForGod(godName);

		if (type == eatFoodType)
		{
			ctx.moodService().addMoodForGod(godName, ctx.moodService().getAngryModifierForGod(godName));
			cursePlayer(godName, believer.getUniqueId(), ctx.godService().getGodPower(godName));

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
				speech().sayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieverHolyFoodSacrifice, 2 + this.random.nextInt(10));
			}
			else
			{
				speech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverHolyFoodSacrifice);
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
			ctx.moodService().addMoodForGod(godName, ctx.moodService().getPleasedModifierForGod(godName));
			ctx.believerService().addPrayer(believer.getUniqueId(), godName);

			blessPlayer(godName, believer.getUniqueId(), godPower);
			speech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverGoodSacrifice);

			ctx.believerService().increasePrayerPower(believer.getUniqueId(), 1);
		}
		else if (value >= -5.0F)
		{
			speech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverMehSacrifice);
		}
		else
		{
			ctx.moodService().addMoodForGod(godName, ctx.moodService().getAngryModifierForGod(godName));
			strikePlayerWithLightning(believer.getUniqueId(), 1 + this.random.nextInt(3));
			speech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverBadSacrifice);
		}

		value -= 1.0F;

		setSacrificeValueForGod(godName, type, Float.valueOf(value));

		gods().markDirty();
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

	public void healPlayer(String godName, Player player)
	{
		ctx.plugin().getServer().getScheduler().runTaskLater(ctx.plugin(), new TaskHealPlayer(ctx, godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverHealthBlessing), 2L);
	}

	public boolean isPriest(UUID believerId)
	{
		if (believerId == null)
		{
			return false;
		}

		List<String> gods = ctx.godService().getAllGods();

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

	private boolean manageBelieverForAngryGod(String godName, Player believer)
	{
		if (!ctx.plugin().isEnabledInWorld(believer.getWorld()))
		{
			return false;
		}

		int godPower = 1 + (int) ctx.godService().getGodPower(godName);

		if (this.random.nextInt(1 + 1000 / godPower) == 0)
		{
			if (ctx.believerService().hasRecentPrayer(believer.getUniqueId()))
			{
				return false;
			}

			if (cursePlayer(godName, believer.getUniqueId(), godPower))
			{
				ctx.languageManager().setPlayerName(believer.getDisplayName());

				speech().say(godName, believer, LanguageManager.LANGUAGESTRING.GodToBelieverCursedAngry, 2 + this.random.nextInt(10));

				return true;
			}
		}

		if (this.random.nextInt(1 + 1000 / ctx.godService().getVerbosityForGod(godName)) == 0)
		{
			if ((ctx.believerService().hasRecentPrayer(believer.getUniqueId())) && (this.random.nextInt(2) == 0))
			{
				return false;
			}
			speech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverRandomAngrySpeech);
			return true;
		}

		if (this.random.nextInt(1 + 600 / ctx.godService().getVerbosityForGod(godName)) == 0)
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
		if (this.random.nextInt(1 + 1000 / ctx.godService().getVerbosityForGod(godName)) == 0)
		{
			if ((ctx.believerService().hasRecentPrayer(believer.getUniqueId())) && (this.random.nextInt(2) == 0))
			{
				return false;
			}
			speech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverRandomDispleasedSpeech);
			return true;
		}
		if (this.random.nextInt(1 + 600 / ctx.godService().getVerbosityForGod(godName)) == 0)
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
			if (!ctx.believerService().hasRecentItemBlessing(believer.getUniqueId()))
			{
				if (ctx.settings().itemBlessings().enabled())
				{
					float power = ctx.godService().getGodPower(godName);

					if (power >= ctx.settings().itemBlessings().minGodPower() && this.random.nextInt((int) (1.0F + 50.0F / power)) == 0)
					{
						double healing = getHealthNeed(godName, believer);

						if ((healing > 1.0D) && (this.random.nextInt(3) == 0))
						{
							healPlayer(godName, believer);

							ctx.believerService().setItemBlessingTime(believer.getUniqueId());

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

							ctx.believerService().setItemBlessingTime(believer.getUniqueId());

							return true;
						}
					}
				}
			}

			if (ctx.settings().holyArtifacts().enabled())
			{
				if (!ctx.believerService().hasRecentHolyArtifactBlessing(believer.getUniqueId()))
				{
					float power = ctx.godService().getGodPower(godName);

					if ((power >= ctx.settings().itemBlessings().minGodPower()) && (this.random.nextInt((int) (1.0F + 100.0F / power)) == 0))
					{
						blessPlayerWithHolyArtifact(godName, believer);

						ctx.languageManager().setPlayerName(believer.getDisplayName());
						ctx.believerService().setHolyArtifactBlessingTime(believer.getUniqueId());

						return true;
					}
				}
			}
		}

		if (!ctx.believerService().hasRecentItemBlessing(believer.getUniqueId()))
		{
			if (blessPlayer(godName, believer.getUniqueId(), ctx.godService().getGodPower(godName)))
			{
				ctx.languageManager().setPlayerName(believer.getDisplayName());

				speech().say(godName, believer, LanguageManager.LANGUAGESTRING.GodToPlayerBlessed, 2 + this.random.nextInt(10));

				speech().sayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPlayerBlessed, believer.getUniqueId());

				return true;
			}
		}

		if (ctx.settings().marriage().enabled() && this.random.nextInt(501) == 0)
		{
			List<MarriedCouple> marriedCouples = ctx.marriageService().getMarriedCouples();
			if (marriedCouples.size() > 0)
			{
				MarriedCouple couple = marriedCouples.get(this.random.nextInt(marriedCouples.size()));

				ctx.languageManager().setPlayerName(ctx.plugin().getServer().getOfflinePlayer(couple.player1Id()).getName() + " and " + ctx.plugin().getServer().getOfflinePlayer(couple.player2Id()).getName());
				speech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverMarriedCouple);
				return true;
			}
		}

		if (this.random.nextInt(1 + 1000 / ctx.godService().getVerbosityForGod(godName)) == 0)
		{
			if ((ctx.believerService().hasRecentPrayer(believer.getUniqueId())) && (this.random.nextInt(2) == 0))
			{
				return false;
			}
			speech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverRandomExaltedSpeech);
			return true;
		}

		if (this.random.nextInt(1 + 600 / ctx.godService().getVerbosityForGod(godName)) == 0)
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
			List<MarriedCouple> marriedCouples = ctx.marriageService().getMarriedCouples();
			if (marriedCouples.size() > 0)
			{
				MarriedCouple couple = marriedCouples.get(this.random.nextInt(marriedCouples.size()));

				ctx.languageManager().setPlayerName(ctx.plugin().getServer().getOfflinePlayer(couple.player1Id()).getName() + " and " + ctx.plugin().getServer().getOfflinePlayer(couple.player2Id()).getName());
				speech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverMarriedCouple);
				return true;
			}
		}
		if (this.random.nextInt(1 + 1000 / ctx.godService().getVerbosityForGod(godName)) == 0)
		{
			if ((ctx.believerService().hasRecentPrayer(believer.getUniqueId())) && (this.random.nextInt(2) == 0))
			{
				return false;
			}
			speech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverRandomNeutralSpeech);
			return true;
		}

		if (this.random.nextInt(1 + 600 / ctx.godService().getVerbosityForGod(godName)) == 0)
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
			if (!ctx.believerService().hasRecentItemBlessing(believer.getUniqueId()))
			{
				if (ctx.settings().itemBlessings().enabled())
				{
					float power = ctx.godService().getGodPower(godName);
					if ((power >= ctx.settings().itemBlessings().minGodPower()) && (this.random.nextInt((int) (1.0F + 100.0F / power)) == 0))
					{
						double healing = getHealthNeed(godName, believer);
						if ((healing > 1.0D) && (this.random.nextInt(2) == 0))
						{
							healPlayer(godName, believer);

							ctx.believerService().setItemBlessingTime(believer.getUniqueId());

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
							ctx.believerService().setItemBlessingTime(believer.getUniqueId());

							return true;
						}
					}
				}
			}
		}

		if ((ctx.settings().marriage().enabled()) && (this.random.nextInt(501) == 0))
		{
			List<MarriedCouple> marriedCouples = ctx.marriageService().getMarriedCouples();

			if (marriedCouples.size() > 0)
			{
				MarriedCouple couple = marriedCouples.get(this.random.nextInt(marriedCouples.size()));

				ctx.languageManager().setPlayerName(ctx.plugin().getServer().getOfflinePlayer(couple.player1Id()).getName() + " and " + ctx.plugin().getServer().getOfflinePlayer(couple.player2Id()).getName());
				speech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverMarriedCouple);
				return true;
			}
		}

		if (this.random.nextInt(1 + 1000 / ctx.godService().getVerbosityForGod(godName)) == 0)
		{
			if ((ctx.believerService().hasRecentPrayer(believer.getUniqueId())) && (this.random.nextInt(2) == 0))
			{
				return false;
			}

			speech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverRandomPleasedSpeech);

			return true;
		}

		if (this.random.nextInt(1 + 600 / ctx.godService().getVerbosityForGod(godName)) == 0)
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
		Set<UUID> believers = ctx.believerService().getOnlineBelieversForGod(godName);
		Set<UUID> managedBelievers = new HashSet<UUID>();
		if (believers.size() == 0)
		{
			return;
		}

		GodMood godMood = ctx.moodService().getMoodForGod(godName);

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

		int godPower = 1 + (int) ctx.godService().getGodPower(godName);

		if (this.random.nextInt(1 + 100 / godPower) == 0)
		{
			Player player = ctx.plugin().getServer().getPlayer(blessedPlayer);

			if ((player == null) || (!ctx.permissionsManager().hasPermission(player, "gods.blessings")))
			{
				return;
			}

			if (blessPlayer(godName, player.getUniqueId(), ctx.godService().getGodPower(godName)))
			{
				ctx.languageManager().setPlayerName(player.getName());

				speech().say(godName, player, LanguageManager.LANGUAGESTRING.GodToPlayerBlessed, 2 + this.random.nextInt(10));

				speech().sayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPlayerBlessed, player.getUniqueId());
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

		int godPower = 1 + (int) ctx.godService().getGodPower(godName);

		if (this.random.nextInt(1 + 100 / godPower) == 0)
		{
			if (!ctx.permissionsManager().hasPermission(cursedPlayer, "gods.curses"))
			{
				return;
			}

			if (cursePlayer(godName, cursedPlayer.getUniqueId(), godPower))
			{
				ctx.languageManager().setPlayerName(cursedPlayer.getDisplayName());

				speech().say(godName, cursedPlayer, LanguageManager.LANGUAGESTRING.GodToPlayerCursed, 2 + this.random.nextInt(10));

				speech().sayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPlayerCursed, cursedPlayer.getUniqueId());
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
		
		ctx.holyLandService().removeAbandonedLands();
	}
	
	private void manageLostBelievers(String godName)
	{
		if (this.random.nextInt(100) > 0)
		{
			return;
		}

		Set<UUID> believers = ctx.believerService().getBelieversForGod(godName);
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
				long timeDiff = System.currentTimeMillis() - ctx.believerService().getLastPrayerTime(believerId).getTime();

				if (timeDiff > 3600000L * ctx.settings().believers().maxHoursWithoutPrayer())
				{
					String believerName = ctx.plugin().getServer().getOfflinePlayer(believerId).getName();
					if (believerName != null)
					{
						ctx.languageManager().setPlayerName(believerName);

						speech().sayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversLostBeliever, 2 + this.random.nextInt(100));
					}

					ctx.believerService().removeBeliever(godName, believerId);
				}
			}

			managedBelievers.add(believerId);
		}
	}

	public boolean managePriests(String godName)
	{
		int numberOfBelievers = ctx.believerService().getBelieversForGod(godName).size();

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

					speech().sayWithQuestion(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverOfferPriest, 2);

					return true;
				}
			}
		}

		for (UUID priestId : priestNames)
		{
			if (this.random.nextInt(1 + 1000 / ctx.godService().getVerbosityForGod(godName)) == 0)
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
								speech().sayToPriests(godName, LanguageManager.LANGUAGESTRING.GodToPriestEatFoodType);
								break;
							case 1:
								ctx.languageManager().setType(ctx.languageManager().getItemTypeName(getUnholyFoodTypeForGod(godName)));
								speech().sayToPriests(godName, LanguageManager.LANGUAGESTRING.GodToPriestNotEatFoodType);
								break;
							case 2:
								ctx.languageManager().setType(ctx.languageManager().getMobTypeName(getUnholyMobTypeForGod(godName)));
								speech().sayToPriests(godName, LanguageManager.LANGUAGESTRING.GodToPriestSlayMobType);
								break;
							default:
								ctx.languageManager().setType(ctx.languageManager().getMobTypeName(getHolyMobTypeForGod(godName)));
								speech().sayToPriests(godName, LanguageManager.LANGUAGESTRING.GodToPriestNotSlayMobType);
							}
						}
						else
						{
							ctx.languageManager().setType(ctx.holyBookManager().getBibleTitle(godName));
							speech().sayToPriests(godName, LanguageManager.LANGUAGESTRING.GodToPriestUseBible);
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

		int numberOfBelievers = ctx.believerService().getOnlineBelieversForGod(godName).size();

		if (!ctx.questManager().hasQuest(godName))
		{
			if (numberOfBelievers < ctx.settings().quests().requiredBelievers() || ctx.godService().getMinutesSinceLastQuest(godName) < ctx.settings().quests().minMinutesBetweenQuests())
			{
				return;
			}

			ctx.questManager().generateQuest(godName);
		}
		else if (ctx.questManager().hasExpiredQuest(godName))
		{
			ctx.moodService().addMoodForGod(godName, ctx.moodService().getAngryModifierForGod(godName));

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

		ctx.altarService().clearDroppedItems();
	}

	private void manageSacrifices(String godName)
	{
		if (!ctx.settings().sacrifices().enabled())
		{
			return;
		}

		int godPower = 1 + (int) ctx.godService().getGodPower(godName);
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

		setSacrificeValueForGod(godName, type, Float.valueOf(value));

		gods().markDirty();

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
				setSacrificeValueForGod(godName, type, null);
			}
			else
			{
				setSacrificeValueForGod(godName, type, Float.valueOf(value));
			}
			gods().save();
		}
	}

	public void removePriest(String godName, UUID playerId)
	{
		God god = god(godName);

		if (god != null && god.priests().remove(playerId))
		{
			dispatchPriestCommand(ctx.settings().priests().removeCommand(), playerId);
		}

		ctx.chatManager().refresh(playerId);

		gods().markDirty();

		ctx.plugin().log(godName + " removed " + ctx.plugin().getServer().getOfflinePlayer(playerId).getName() + " as priest");
	}

	/**
	 * Makes the player the god's blessed player from now on, or clears it when believerId is null.
	 */
	public void setBlessedPlayerForGod(String godName, UUID believerId)
	{
		God god = believerId == null ? god(godName) : godToChange(godName, "BlessedPlayer");
		if (god != null)
		{
			god.setBlessed(believerId, believerId == null ? null : now());
		}

		gods().markDirty();
	}

	/**
	 * Makes the player the god's cursed player from now on, or clears it when believerId is null.
	 */
	public void setCursedPlayerForGod(String godName, UUID believerId)
	{
		God god = believerId == null ? god(godName) : godToChange(godName, "CursedPlayer");
		if (god != null)
		{
			god.setCursed(believerId, believerId == null ? null : now());
		}

		gods().markDirty();
	}

	public boolean setPendingPriest(String godName, UUID believerId)
	{
		if (believerId == null)
		{
			return false;
		}

		God god = godToChange(godName, "PendingPriest");
		if (god == null)
		{
			return false;
		}

		// Offer the priesthood at most once every 3 minutes
		Instant lastPriestTime = god.pendingPriestTime();
		if (lastPriestTime != null)
		{
			long diffMinutes = (System.currentTimeMillis() - lastPriestTime.toEpochMilli()) / 60000L;
			if (diffMinutes < 3L)
			{
				return false;
			}
		}

		god.setPendingPriest(believerId);
		god.setPendingPriestTime(now());

		gods().markDirty();

		ctx.believerService().setPendingPriest(believerId);

		return true;
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

	public void update()
	{
		if (this.random.nextInt(50) == 0)
		{
			ctx.plugin().logDebug("Processing dead offline Gods...");

			long timeBefore = System.currentTimeMillis();

			List<String> godNames = ctx.godService().getOfflineGods();
			for (String offlineGodName : godNames)
			{
				if (ctx.godService().isDeadGod(offlineGodName))
				{
					ctx.plugin().log("Removed dead offline God '" + offlineGodName + "'");
				}
			}
			long timeAfter = System.currentTimeMillis();

			ctx.plugin().logDebug("Processed " + godNames.size() + " offline Gods in " + (timeAfter - timeBefore) + " ms");
		}

		List<String> godNames = ctx.godService().getOnlineGods();

		long timeBefore = System.currentTimeMillis();

		if (godNames.size() == 0)
		{
			return;
		}
		String godName = (String) godNames.toArray()[this.random.nextInt(godNames.size())];

		ctx.plugin().logDebug("Processing God '" + godName + "'");

		ctx.moodService().applyFalloff(godName);

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

}
