package com.dogonfire.gods.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.api.event.PriestAssignEvent;
import com.dogonfire.gods.api.event.PriestRemoveEvent;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.model.DevotionLevel;
import com.dogonfire.gods.model.God;
import com.dogonfire.gods.storage.CleanupGuard;
import com.dogonfire.gods.storage.GodRepository;
import com.dogonfire.gods.util.LegacyDates;

/**
 * Priests: who they are, offering priesthood to a believer and their answer, assigning and removing them (with the
 * console commands config.yml sets for that), dropping priests who stopped praying, and a god reminding its priests of
 * its commandments. Reads ({@link #getPriestsForGod}, {@link #isPriest}) change nothing; only the update loop removes
 * priests ({@link #removeInactivePriests}). The rules are in {@link PriestRules}. Main thread only.
 */
public class PriestService
{
	private final Random			random	= new Random();
	private final GodsContext		ctx;
	private final GodRepository		repository;

	public PriestService(GodsContext ctx, GodRepository repository)
	{
		this.ctx = ctx;
		this.repository = repository;
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

	/**
	 * Makes the believer a priest of their god (firing a {@link PriestAssignEvent}) and clears the god's priest offer.
	 *
	 * @return false if they don't believe in the god or a listener cancelled it
	 */
	public boolean assignPriest(String godName, UUID playerId)
	{
		God existing = this.repository.find(godName);
		if (existing != null)
		{
			existing.setPendingPriest(null);
		}
		ctx.believerService().clearPendingPriest(playerId);

		Set<UUID> believers = ctx.believerService().getBelieversForGod(godName);
		if (believers.contains(playerId))
		{
			God god = this.repository.forChange(godName);
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
				if (ctx.fire(new PriestAssignEvent(god.name(), playerId)).isCancelled())
				{
					this.repository.markDirty();
					return false;
				}
				god.priests().add(playerId);
				dispatchPriestCommand(ctx.settings().priests().assignCommand(), playerId);
			}

			ctx.chatService().refresh(playerId);

			god.setPendingPriest(null);
			god.setPendingPriestTime(null);

			ctx.believerService().setLastPrayerDate(playerId);

			this.repository.markDirty();
			return true;
		}
		else
		{
			return false;
		}
	}

	/** The believer to offer priesthood to, or null; see {@link PriestRules#pick} */
	private UUID getNextBelieverForPriest(String godName)
	{
		Set<UUID> allBelievers = ctx.believerService().getBelieversForGod(godName);

		if (allBelievers == null || allBelievers.size() == 0)
		{
			ctx.plugin().logDebug("Did not find any priest candidates");
			return null;
		}

		UUID pendingPriest = getPendingPriest(godName);

		List<DevotionRules.PriestCandidate<UUID>> candidates = new ArrayList<>();
		for (UUID candidate : allBelievers)
		{
			Player player = ctx.plugin().getServer().getPlayer(candidate);
			if (player != null && PriestRules.isCandidate(isPriest(candidate), candidate.equals(pendingPriest), ctx.believerService().hasRecentPriestOffer(candidate),
					ctx.permissions().hasPermission(player, "gods.priest")))
			{
				// Only devotion that is on counts
				DevotionLevel level = ctx.devotionService().enabled() ? ctx.devotionService().getLevel(candidate) : DevotionLevel.FOLLOWER;
				candidates.add(new DevotionRules.PriestCandidate<>(candidate, level, ctx.believerService().getBelieverPower(candidate)));
			}
		}

		// The most devoted first, then the strongest; a believer of devotion.unlocks.priesthood is always asked first
		return DevotionRules.pickPriest(candidates, ctx.devotionService().preferredPriestLevel(), this.random);
	}

	public UUID getPendingPriest(String godName)
	{
		God god = this.repository.find(godName);
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

	/**
	 * The god's priests who still pray, in the order they were appointed. Changes nothing: priests who stopped praying
	 * are left out here and removed by {@link #removeInactivePriests} (the update loop). While believers.yml can't be
	 * read, nobody's prayers are known, so every stored priest counts.
	 */
	public List<UUID> getPriestsForGod(String godName)
	{
		God god = this.repository.find(godName);
		List<UUID> list = new ArrayList<UUID>();
		if (god == null)
		{
			return list;
		}

		boolean prayersKnown = !ctx.storage().believers().isReadOnly();
		for (UUID believerId : god.priests())
		{
			if (!prayersKnown || !isInactive(believerId))
			{
				list.add(believerId);
			}
		}
		return list;
	}

	private boolean isInactive(UUID believerId)
	{
		return PriestRules.isInactive(ctx.believerService().getLastPrayerTime(believerId).getTime(), System.currentTimeMillis(), ctx.settings().priests().maxHoursWithoutPrayer());
	}

	/**
	 * The update loop: removes the god's priests who stopped praying (with the remove command and a word to the
	 * believers). Nothing is removed while {@link com.dogonfire.gods.storage.Storage#cleanupVerdict()} says the data
	 * can't be trusted for that (a file could not be read, no believers loaded, just after a (re)load).
	 */
	public void removeInactivePriests(String godName)
	{
		God god = this.repository.find(godName);
		if (god == null || ctx.storage().cleanupVerdict() != CleanupGuard.Verdict.ALLOWED)
		{
			return;
		}

		for (UUID believerId : new ArrayList<UUID>(god.priests()))
		{
			if (isInactive(believerId))
			{
				String priestName = ctx.plugin().getServer().getOfflinePlayer(believerId).getName();
				ctx.godSpeech().sayToBelievers(godName, Voice.BELIEVERS_REMOVED_PRIEST, 2 + this.random.nextInt(40), priestName != null ? Messages.player(priestName) : Messages.component("player", ctx.messages().component("god.our-priest")));
				removePriest(godName, believerId);
			}
		}
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

	/** The god's turn in the update loop: drops priests who stopped praying, looks for new ones, reminds them */
	public boolean managePriests(String godName)
	{
		removeInactivePriests(godName);

		int numberOfBelievers = ctx.believerService().getBelieversForGod(godName).size();

		List<UUID> priestNames = getPriestsForGod(godName);

		if (priestNames == null)
		{
			priestNames = new ArrayList<UUID>();
		}

		if (!PriestRules.hasEnoughBelievers(numberOfBelievers, priestNames.size(), ctx.settings().priests().minBelievers()))
		{
			return false;
		}

		// priests.max-per-god, and one more from the rank config.yml's ranks.unlocks.extra-priest names
		if (PriestRules.wantsMorePriests(priestNames.size(), RankRules.maxPriests(ctx.settings().priests().maxPerGod(), ctx.rankService().getRank(godName),
				ctx.settings().ranks().unlocks().extraPriest())))
		{
			UUID believerId = getNextBelieverForPriest(godName);
			// A believer of the level devotion.unlocks.priesthood names is asked on this turn; others now and then
			boolean devoted = believerId != null && ctx.devotionService().grants(believerId, ctx.settings().devotion().unlocks().priesthood());
			if (devoted || this.random.nextInt(3) == 0)
			{
				ctx.plugin().logDebug(godName + " has too few priests. Finding one...");

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

				if (offerPriesthood(godName, player))
				{
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

					// Remind the priests of a commandment, or of the holy book when bibles are on
					int r = ctx.settings().bibles().enabled() ? this.random.nextInt(2) : 0;

					try
					{
						if (r == 0)
						{
							switch (this.random.nextInt(4))
							{
							case 0:
								ctx.godSpeech().sayToPriests(godName, Voice.PRIEST_EAT_FOOD_TYPE, Messages.displayName(player.getDisplayName()), Messages.type(ctx.messages().itemName(ctx.commandmentService().getHolyFoodTypeForGod(godName))));
								break;
							case 1:
								ctx.godSpeech().sayToPriests(godName, Voice.PRIEST_NOT_EAT_FOOD_TYPE, Messages.displayName(player.getDisplayName()), Messages.type(ctx.messages().itemName(ctx.commandmentService().getUnholyFoodTypeForGod(godName))));
								break;
							case 2:
								ctx.godSpeech().sayToPriests(godName, Voice.PRIEST_SLAY_MOB_TYPE, Messages.displayName(player.getDisplayName()), Messages.type(ctx.messages().mobName(ctx.commandmentService().getUnholyMobTypeForGod(godName))));
								break;
							default:
								ctx.godSpeech().sayToPriests(godName, Voice.PRIEST_NOT_SLAY_MOB_TYPE, Messages.displayName(player.getDisplayName()), Messages.type(ctx.messages().mobName(ctx.commandmentService().getHolyMobTypeForGod(godName))));
							}
						}
						else
						{
							String bibleTitle = ctx.holyBookService().getBibleTitle(godName);
							if (bibleTitle == null)
							{
								// The god has no holy book yet: nothing to remind of (as before)
								return false;
							}
							ctx.godSpeech().sayToPriests(godName, Voice.PRIEST_USE_BIBLE, Messages.displayName(player.getDisplayName()), Messages.type(bibleTitle));
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

	/**
	 * The god offers the believer to become its priest; they answer with /g yes or /g no. At most one offer every 3
	 * minutes per god.
	 *
	 * @return false if the god offered priesthood too recently
	 */
	public boolean offerPriesthood(String godName, Player player)
	{
		if (!setPendingPriest(godName, player.getUniqueId()))
		{
			return false;
		}
		ctx.plugin().log(godName + " offered " + player.getName() + " to be priest");

		ctx.godSpeech().sayWithQuestion(godName, player, Voice.BELIEVER_OFFER_PRIEST, 2, Messages.player(player.getName()));
		return true;
	}

	/** Removes the priest (firing a {@link PriestRemoveEvent} if they were one) */
	public void removePriest(String godName, UUID playerId)
	{
		God god = this.repository.find(godName);

		if (god != null && god.priests().remove(playerId))
		{
			dispatchPriestCommand(ctx.settings().priests().removeCommand(), playerId);
			ctx.fire(new PriestRemoveEvent(god.name(), playerId));
		}

		ctx.chatService().refresh(playerId);

		this.repository.markDirty();

		ctx.plugin().log(godName + " removed " + ctx.plugin().getServer().getOfflinePlayer(playerId).getName() + " as priest");
	}

	/** The believer offered priesthood said no */
	public void clearPendingPriest(String godName)
	{
		God god = this.repository.find(godName);
		if (god != null)
		{
			god.setPendingPriest(null);
		}

		this.repository.markDirty();
	}

	public boolean setPendingPriest(String godName, UUID believerId)
	{
		if (believerId == null)
		{
			return false;
		}

		God god = this.repository.forChange(godName);
		if (god == null)
		{
			return false;
		}

		// Offer the priesthood at most once every 3 minutes
		Instant lastPriestTime = god.pendingPriestTime();
		if (!PriestRules.canOfferAgain(lastPriestTime != null ? lastPriestTime.toEpochMilli() : null, System.currentTimeMillis()))
		{
			return false;
		}

		god.setPendingPriest(believerId);
		god.setPendingPriestTime(LegacyDates.SECONDS.now());

		this.repository.markDirty();

		ctx.believerService().setPendingPriest(believerId);

		return true;
	}
}
