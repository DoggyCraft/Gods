package com.dogonfire.gods.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.model.God;
import com.dogonfire.gods.storage.GodRepository;
import com.dogonfire.gods.util.LegacyDates;

/**
 * Priests: who they are, offering priesthood to a believer and their answer, assigning and removing them (with the
 * console commands config.yml sets for that), dropping priests who stopped praying, and a god reminding its priests of
 * its commandments. The rules are in {@link PriestRules}. Main thread only.
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

		Map<UUID, Float> candidates = new HashMap<UUID, Float>();
		for (UUID candidate : allBelievers)
		{
			Player player = ctx.plugin().getServer().getPlayer(candidate);
			if (player != null && PriestRules.isCandidate(isPriest(candidate), candidate.equals(pendingPriest), ctx.believerService().hasRecentPriestOffer(candidate),
					ctx.permissions().hasPermission(player, "gods.priest")))
			{
				candidates.put(candidate, ctx.believerService().getBelieverPower(candidate));
			}
		}

		List<UUID> byPower = new ArrayList<UUID>(candidates.keySet());
		byPower.sort((c1, c2) -> Float.compare(candidates.get(c2), candidates.get(c1)));

		return PriestRules.pick(byPower, this.random);
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

	public List<UUID> getPriestsForGod(String godName)
	{
		God god = this.repository.find(godName);
		List<UUID> list = new ArrayList<UUID>();
		if (god == null)
		{
			return list;
		}

		for (UUID believerId : new ArrayList<UUID>(god.priests()))
		{
			if (PriestRules.isInactive(ctx.believerService().getLastPrayerTime(believerId).getTime(), System.currentTimeMillis(), ctx.settings().priests().maxHoursWithoutPrayer()))
			{
				String priestName = ctx.plugin().getServer().getOfflinePlayer(believerId).getName();
				ctx.languageManager().setPlayerName(priestName != null ? priestName : "our priest");
				ctx.godSpeech().sayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversRemovedPriest, 2 + this.random.nextInt(40));
				removePriest(godName, believerId);
			}
			else
			{
				list.add(believerId);
			}
		}
		return list;
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

	public boolean managePriests(String godName)
	{
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

		if (PriestRules.wantsMorePriests(priestNames.size(), ctx.settings().priests().maxPerGod()))
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

					ctx.godSpeech().sayWithQuestion(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverOfferPriest, 2);

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
								ctx.languageManager().setType(ctx.languageManager().getItemTypeName(ctx.commandmentService().getHolyFoodTypeForGod(godName)));
								ctx.godSpeech().sayToPriests(godName, LanguageManager.LANGUAGESTRING.GodToPriestEatFoodType);
								break;
							case 1:
								ctx.languageManager().setType(ctx.languageManager().getItemTypeName(ctx.commandmentService().getUnholyFoodTypeForGod(godName)));
								ctx.godSpeech().sayToPriests(godName, LanguageManager.LANGUAGESTRING.GodToPriestNotEatFoodType);
								break;
							case 2:
								ctx.languageManager().setType(ctx.languageManager().getMobTypeName(ctx.commandmentService().getUnholyMobTypeForGod(godName)));
								ctx.godSpeech().sayToPriests(godName, LanguageManager.LANGUAGESTRING.GodToPriestSlayMobType);
								break;
							default:
								ctx.languageManager().setType(ctx.languageManager().getMobTypeName(ctx.commandmentService().getHolyMobTypeForGod(godName)));
								ctx.godSpeech().sayToPriests(godName, LanguageManager.LANGUAGESTRING.GodToPriestNotSlayMobType);
							}
						}
						else
						{
							ctx.languageManager().setType(ctx.holyBookService().getBibleTitle(godName));
							ctx.godSpeech().sayToPriests(godName, LanguageManager.LANGUAGESTRING.GodToPriestUseBible);
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

	public void removePriest(String godName, UUID playerId)
	{
		God god = this.repository.find(godName);

		if (god != null && god.priests().remove(playerId))
		{
			dispatchPriestCommand(ctx.settings().priests().removeCommand(), playerId);
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
