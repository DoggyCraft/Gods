package com.dogonfire.gods.service;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.api.event.BelieverJoinGodEvent;
import com.dogonfire.gods.api.event.BelieverLeaveGodEvent;
import com.dogonfire.gods.api.event.BelieverPrayEvent;
import com.dogonfire.gods.api.event.GodCreateEvent;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.message.Messages;
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

			ctx.godSpeech().say(godName, player, Voice.BELIEVER_ALTAR_BUILT, 2 + this.random.nextInt(30), Messages.player(player.getName()));

			return true;
		}

		return false;
	}

	private boolean addBelief(Player player, String godName, boolean allowChangeGod, BelieverJoinGodEvent.Reason reason)
	{
		String oldGodName = ctx.believerService().getGodForBeliever(player.getUniqueId());

		if (godName == null)
		{
			ctx.godSpeech().sendInfo(player.getUniqueId(), "altar.invalid-god-name", 1);
			return false;
		}

		if (oldGodName != null && !oldGodName.equals(godName))
		{
			if (!allowChangeGod)
			{
				ctx.believerService().setChangingGod(player.getUniqueId());

				ctx.godSpeech().sendInfo(player.getUniqueId(), "prayer.confirm-change", 1, Messages.god(oldGodName));
				return false;
			}

			if (ctx.believerService().hasRecentGodChange(player.getUniqueId()))
			{
				ctx.godSpeech().sendInfo(player.getUniqueId(), "prayer.change-too-soon", 1);
				return false;
			}

			ctx.believerService().clearChangingGod(player.getUniqueId());
		}

		// Joining can be cancelled; if they can't pray yet, addPrayer says so as before
		boolean joining = oldGodName == null || !oldGodName.equals(godName);
		if (joining && ctx.believerService().canPray(player.getUniqueId(), godName) && ctx.fire(new BelieverJoinGodEvent(godName, player, oldGodName, reason)).isCancelled())
		{
			return false;
		}

		if (!ctx.believerService().addPrayer(player.getUniqueId(), godName))
		{
			int timeUntilCanPray = ctx.believerService().getTimeUntilCanPray(player.getUniqueId());

			ctx.godSpeech().sendInfo(player.getUniqueId(), "prayer.too-soon", 1, Messages.text("minutes", timeUntilCanPray));
			return false;
		}

		if (oldGodName != null && !oldGodName.equals(godName))
		{
			if (ctx.priestService().isPriestForGod(player.getUniqueId(), oldGodName))
			{
				ctx.priestService().removePriest(oldGodName, player.getUniqueId());
			}
			ctx.fire(new BelieverLeaveGodEvent(oldGodName, player.getUniqueId(), BelieverLeaveGodEvent.Reason.CHANGED_GOD));


			ctx.godSpeech().sayToBelievers(oldGodName, Voice.BELIEVERS_PLAYER_LEFT_RELIGION, 2 + this.random.nextInt(20), Messages.player(player.getName()));

			ctx.godSpeech().sendInfo(player.getUniqueId(), "prayer.left-religion", 20, Messages.god(oldGodName));

			ctx.godSpeech().sayToBelieversExcept(godName, Voice.BELIEVERS_PLAYER_JOINED_RELIGION, player.getUniqueId(), Messages.player(player.getName()));

			ctx.believerService().clearPrayerPower(player.getUniqueId());
		}
		else
		{
			Material foodType = ctx.commandmentService().getHolyFoodTypeForGod(godName);


			ctx.blessingService().giveItem(godName, player, foodType, false);

			ctx.believerService().increasePrayerPower(player.getUniqueId(), 1);
		}

		if (oldGodName == null || !oldGodName.equals(godName))
		{
			if (ctx.settings().marriage().enabled())
			{
				ctx.marriageService().divorce(player.getUniqueId());
			}
			
			ctx.questService().handleJoinReligion(player, godName);

			if (ctx.settings().bibles().enabled() && ctx.settings().bibles().giveOnJoin())
			{
				ctx.holyBookService().giveBible(godName, player);
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

			List<ItemStack> rewards = ctx.questService().getRewardsForQuestCompletion(godName);

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
			if (!player.isOp() && (!ctx.permissions().hasPermission(player, "gods.god.create")))
			{
				ctx.godSpeech().sendInfo(player.getUniqueId(), "altar.create-god-not-allowed", 20);
				return false;
			}

			Block altarBlock = ctx.altarService().getAltarBlockFromSign(prayerLocation.getBlock());

			GodGender godGender = ctx.altarService().getGodGenderFromAltarBlock(altarBlock);

			ctx.plugin().logDebug("Altar is " + altarBlock.getType().name());

			GodType godType = ctx.altarService().getGodTypeForAltarBlockType(altarBlock.getType());

			ctx.plugin().logDebug("God divine force is " + godType);

			if (ctx.fire(new GodCreateEvent(godName, player, ApiSnapshots.godType(godType), ApiSnapshots.gender(godGender), player.getLocation())).isCancelled())
			{
				return false;
			}

			ctx.godService().createGod(godName, player.getLocation(), godGender, godType);

			if (ctx.settings().gods().broadcastNewGods())
			{
				ctx.messages().broadcast(ctx.plugin().getServer(), "broadcast.new-god", Messages.player(player.getName()), Messages.god(godName), Messages.component("gender", ctx.messages().genderName(ctx.godService().getGenderForGod(godName))));
			}

			ctx.plugin().log(player.getName() + " created new god " + godName);
		}

		return addBelief(player, godName, allowChangeGod, BelieverJoinGodEvent.Reason.ALTAR);
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

		if (godName != null && ctx.settings().marriage().enabled())
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
			if (addBelief(player, pendingGodInvitation, true, BelieverJoinGodEvent.Reason.INVITATION))
			{
				ctx.believerService().clearInvitation(believerId);

				ctx.plugin().log(player.getName() + " accepted the invitation to join " + pendingGodInvitation);

				ctx.godSpeech().say(pendingGodInvitation, player, Voice.PLAYER_ACCEPTED_INVITATION, 2 + this.random.nextInt(40), Messages.player(player.getName()));
				ctx.godSpeech().sayToBelieversExcept(pendingGodInvitation, Voice.BELIEVERS_NEW_PLAYER_ACCEPTED, player.getUniqueId(), Messages.player(player.getName()));
			}
			else
			{
				ctx.plugin().log(player.getName() + " could NOT accept the invitation to join " + pendingGodInvitation);
			}
			return;
		}

		if (godName == null)
		{
			ctx.messages().send(player, "command.yes.nothing");
			return;
		}

		UUID pendingPriest = ctx.priestService().getPendingPriest(godName);

		if (pendingPriest != null)
		{
			if (pendingPriest.equals(believerId))
			{
				if (!ctx.priestService().assignPriest(godName, believerId))
				{
					ctx.plugin().log(player.getName() + " accepted the offer from " + godName + " to be priest, but did not become one");
					return;
				}

				ctx.plugin().log(player.getName() + " accepted the offer from " + godName + " to be priest");

				ctx.godSpeech().sendInfo(player.getUniqueId(), "priest.invite-hint", 100);
				ctx.godSpeech().sendInfo(player.getUniqueId(), "priest.followers-hint", 200);
				ctx.godSpeech().sendInfo(player.getUniqueId(), "priest.description-hint", 300);

				if (ctx.settings().holyArtifacts().enabled())
				{
					ctx.godSpeech().sendInfo(player.getUniqueId(), "holy-land.attack-hint", 300);
				}
				try
				{
					ctx.godSpeech().say(godName, player, Voice.PRIEST_PRIEST_ACCEPTED, 2 + this.random.nextInt(40), Messages.player(player.getName()));
					ctx.godSpeech().sayToBelieversExcept(godName, Voice.BELIEVERS_PRIEST_ACCEPTED, player.getUniqueId(), Messages.player(player.getName()));
				}
				catch (Exception ex)
				{
					ctx.plugin().log("ERROR: Could not say GodToPriestPriestAccepted text! " + ex.getMessage());
				}
				return;
			}
		}

		ctx.plugin().logDebug(player.getDisplayName() + " did not have anything to accepted from " + godName);
		ctx.godSpeech().say(godName, player, Voice.BELIEVER_NO_QUESTION, 2 + this.random.nextInt(20), Messages.player(player.getName()));
	}

	/** The god invites the player to join it; they answer with /g yes or /g no */
	public void invite(String godName, Player invited)
	{
		ctx.believerService().setInvitation(invited.getUniqueId(), godName);
		ctx.plugin().log(godName + " invited to " + invited.getName() + " to join the religion");
		ctx.godSpeech().say(godName, invited, Voice.PLAYER_INVITE, 10, Messages.player(invited.getName()));
		ctx.godSpeech().sendInfo(invited.getUniqueId(), "god.question-hint", 40);
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
		ctx.believerService().believerLeave(godName, believerId, BelieverLeaveGodEvent.Reason.LEFT);


		if (ctx.settings().marriage().enabled())
		{
			ctx.marriageService().divorce(believerId);
		}

		ctx.believerService().clearPrayerPower(believerId);

		ctx.godSpeech().sayToBelievers(godName, Voice.BELIEVERS_PLAYER_LEFT_RELIGION, 2 + this.random.nextInt(20), Messages.displayName(ctx.plugin().getServer().getPlayer(believerId).getDisplayName()));

		return true;
	}

	/**
	 * A priest kicks a believer (online or not) out of the religion: like leaving it, they stop being a priest and are
	 * divorced, and their entry goes. Does nothing if they believe in another god.
	 */
	public void kick(String godName, UUID believerId)
	{
		if (!godName.equals(ctx.believerService().getGodForBeliever(believerId)))
		{
			return;
		}
		if (ctx.priestService().isPriestForGod(believerId, godName))
		{
			ctx.priestService().removePriest(godName, believerId);
		}
		if (ctx.settings().marriage().enabled())
		{
			ctx.marriageService().divorce(believerId);
		}
		ctx.believerService().removeBeliever(godName, believerId, BelieverLeaveGodEvent.Reason.KICKED);
	}

	public void believerReject(UUID believerId)
	{
		String godName = ctx.believerService().getGodForBeliever(believerId);
		Player player = ctx.plugin().getServer().getPlayer(believerId);
		if (player == null)
		{
			return;
		}


		String pendingGodInvitation = ctx.believerService().getInvitation(believerId);
		if (pendingGodInvitation != null)
		{
			ctx.believerService().clearInvitation(believerId);

			ctx.plugin().log(player.getName() + " rejected the invitation to join " + pendingGodInvitation);

			ctx.godSpeech().sendInfo(player.getUniqueId(), "prayer.rejected-invitation", 20, Messages.god(pendingGodInvitation));

			return;
		}

		if (godName == null)
		{
			ctx.messages().send(player, "command.no.nothing");
			return;
		}

		UUID pendingPriest = ctx.priestService().getPendingPriest(godName);

		if (pendingPriest == null)
		{
			ctx.godSpeech().say(godName, player, Voice.BELIEVER_NO_QUESTION, 2 + this.random.nextInt(20), Messages.player(player.getName()));
			return;
		}

		if (pendingPriest.equals(believerId))
		{
			ctx.priestService().clearPendingPriest(godName);

			ctx.believerService().clearPendingPriest(believerId);

			ctx.plugin().log(player.getName() + " rejected the offer from " + godName + " to be priest");

			ctx.godSpeech().say(godName, player, Voice.BELIEVER_PRIEST_REJECTED, 2 + this.random.nextInt(20), Messages.player(player.getName()));
		}
	}

	public boolean handleAltarPray(Location location, Player player, String godName)
	{
		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return false;
		}

		if (godName != null && ctx.fire(new BelieverPrayEvent(godName, player, location)).isCancelled())
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

			if ((ctx.settings().holyLand().enabled()) && (ctx.permissions().hasPermission(player, "gods.holyland")))
			{
				ctx.holyLandService().addPrayer(player, godName, altarBlock.getLocation());
			}

			ctx.questService().handlePrayer(godName, player.getUniqueId());


			ctx.godSpeech().say(godName, player, Voice.BELIEVER_PRAYING, 2 + this.random.nextInt(10), Messages.player(player.getName()));
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
			ctx.believerService().believerLeave(godName, playerId, BelieverLeaveGodEvent.Reason.DIED);
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
			if (!managedBelievers.contains(believerId) && !ctx.believerService().hasUnreadableLastPrayer(believerId))
			{
				long timeDiff = System.currentTimeMillis() - ctx.believerService().getLastPrayerTime(believerId).getTime();

				if (timeDiff > 3600000L * ctx.settings().believers().maxHoursWithoutPrayer())
				{
					String believerName = ctx.plugin().getServer().getOfflinePlayer(believerId).getName();
					if (believerName != null)
					{

						ctx.godSpeech().sayToBelievers(godName, Voice.BELIEVERS_LOST_BELIEVER, 2 + this.random.nextInt(100), Messages.player(believerName));
					}

					ctx.believerService().removeBeliever(godName, believerId, BelieverLeaveGodEvent.Reason.INACTIVE);
				}
			}

			managedBelievers.add(believerId);
		}
	}
}
