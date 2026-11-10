package com.dogonfire.gods.service;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.model.GodType;

/**
 * Believing: praying at an altar (which creates the god the first time), joining a god, changing to another one, a
 * believer's answers (/g yes, /g no) to invitations and priest offers, leaving a god, dying, and believers who stopped
 * praying. Prayers, prayer power and invitations themselves are {@link BelieverService}'s. Main thread only.
 */
public class PrayerService
{
	private final Random			random	= new Random();
	private final GodsContext		ctx;

	public PrayerService(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	public boolean addAltar(Player player, String godName, Location location)
	{
		if (addBeliefByAltar(player, godName, location, true))
		{
			ctx.languageManager().setPlayerName(player.getName());

			ctx.godSpeech().say(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverAltarBuilt, 2 + this.random.nextInt(30));

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
			if (ctx.priestService().isPriestForGod(player.getUniqueId(), oldGodName))
			{
				ctx.priestService().removePriest(oldGodName, player.getUniqueId());
			}

			ctx.languageManager().setPlayerName(player.getName());

			ctx.godSpeech().sayToBelievers(oldGodName, LanguageManager.LANGUAGESTRING.GodToBelieversPlayerLeftReligion, 2 + this.random.nextInt(20));

			ctx.godSpeech().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.YouLeftReligion, ChatColor.RED, 0, oldGodName, 20);

			ctx.godSpeech().sayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPlayerJoinedReligion, player.getUniqueId());

			ctx.believerService().clearPrayerPower(player.getUniqueId());
		}
		else
		{
			Material foodType = ctx.commandmentService().getHolyFoodTypeForGod(godName);

			try
			{
				ctx.languageManager().setType(ctx.languageManager().getItemTypeName(foodType));
			}
			catch (Exception ex)
			{
				ctx.plugin().logDebug(ex.getMessage());
			}

			ctx.blessingService().giveItem(godName, player, foodType, false);

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
				ctx.blessingService().giveItem(godName, player, items.getType(), false);
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

				ctx.godSpeech().say(pendingGodInvitation, player, LanguageManager.LANGUAGESTRING.GodToPlayerAcceptedInvitation, 2 + this.random.nextInt(40));
				ctx.godSpeech().sayToBelieversExcept(pendingGodInvitation, LanguageManager.LANGUAGESTRING.GodToBelieversNewPlayerAccepted, player.getUniqueId());
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

		UUID pendingPriest = ctx.priestService().getPendingPriest(godName);

		if (pendingPriest != null)
		{
			if (pendingPriest.equals(believerId))
			{
				ctx.priestService().assignPriest(godName, believerId);

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
					ctx.godSpeech().say(godName, player, LanguageManager.LANGUAGESTRING.GodToPriestPriestAccepted, 2 + this.random.nextInt(40));
					ctx.godSpeech().sayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPriestAccepted, player.getUniqueId());
				}
				catch (Exception ex)
				{
					ctx.plugin().log("ERROR: Could not say GodToPriestPriestAccepted text! " + ex.getMessage());
				}
				return;
			}
		}

		ctx.plugin().logDebug(player.getDisplayName() + " did not have anything to accepted from " + godName);
		ctx.godSpeech().say(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverNoQuestion, 2 + this.random.nextInt(20));
	}

	public boolean believerLeaveGod(UUID believerId)
	{
		String godName = ctx.believerService().getGodForBeliever(believerId);
		if (godName == null)
		{
			return false;
		}

		if (ctx.priestService().isPriestForGod(believerId, godName))
		{
			ctx.priestService().removePriest(godName, believerId);
		}
		ctx.believerService().believerLeave(godName, believerId);

		ctx.languageManager().setPlayerName(ctx.plugin().getServer().getPlayer(believerId).getDisplayName());

		if (ctx.settings().marriage().enabled())
		{
			ctx.marriageService().divorce(believerId);
		}

		ctx.believerService().clearPrayerPower(believerId);

		ctx.godSpeech().sayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPlayerLeftReligion, 2 + this.random.nextInt(20));

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

		UUID pendingPriest = ctx.priestService().getPendingPriest(godName);

		if (pendingPriest == null)
		{
			ctx.godSpeech().say(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverNoQuestion, 2 + this.random.nextInt(20));
			return;
		}

		if (pendingPriest.equals(believerId))
		{
			ctx.priestService().clearPendingPriest(godName);

			ctx.believerService().clearPendingPriest(believerId);

			ctx.plugin().log(player.getName() + " rejected the offer from " + godName + " to be priest");

			ctx.godSpeech().say(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverPriestRejected, 2 + this.random.nextInt(20));
		}
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

			ctx.godSpeech().say(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverPraying, 2 + this.random.nextInt(10));
			location.getWorld().playEffect(location, Effect.MOBSPAWNER_FLAMES, 25);

			return true;
		}

		return false;
	}

	public void handleKilledPlayer(UUID playerId, String godName, GodType godType)
	{
		if (godName == null || godType == null)
		{
			return;
		}
		if (ctx.settings().believers().leaveReligionOnDeath())
		{
			if (ctx.priestService().isPriestForGod(playerId, godName))
			{
				ctx.priestService().removePriest(godName, playerId);
			}
			ctx.believerService().believerLeave(godName, playerId);
		}
	}

	public void removeLostBelievers(String godName)
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

						ctx.godSpeech().sayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversLostBeliever, 2 + this.random.nextInt(100));
					}

					ctx.believerService().removeBeliever(godName, believerId);
				}
			}

			managedBelievers.add(believerId);
		}
	}
}
