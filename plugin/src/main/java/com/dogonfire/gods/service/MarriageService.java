package com.dogonfire.gods.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.model.Marriage;
import com.dogonfire.gods.model.MarriedCouple;
import com.dogonfire.gods.storage.MarriageRepository;
import com.dogonfire.gods.tasks.TaskLove;
import com.dogonfire.gods.util.LegacyDates;

/**
 * Marriages: proposals (/g marry, accepted by praying), weddings (both partners pick up a wedding token, a gold
 * nugget), love (/g love) and divorce. Owns every change to marriages.yml ({@link MarriageRepository}); every change is
 * written at once, as before. Its data is only loaded while marriage is enabled. Main thread only.
 */
public class MarriageService
{
	/** A proposal expires after this many seconds */
	public static final long			PROPOSAL_SECONDS	= 30L;

	private static final LegacyDates	DATES				= LegacyDates.SECONDS;

	private final GodsContext			ctx;
	private final MarriageRepository	repository;
	private final Random				random				= new Random();
	private final Material				marriageTokenType	= Material.GOLD_NUGGET;

	public MarriageService(GodsContext ctx, MarriageRepository repository)
	{
		this.ctx = ctx;
		this.repository = repository;
	}

	public void load()
	{
		this.repository.load();
	}

	private Marriage find(UUID playerId)
	{
		return this.repository.find(playerId);
	}

	// ---------------------------------------------------------------------
	// Married couples

	public UUID getPartnerId(UUID believerId)
	{
		Marriage marriage = find(believerId);
		return marriage != null && marriage.wedlock() != null ? marriage.wedlock().partner() : null;
	}

	public String getPartnerName(UUID playerId)
	{
		UUID partnerId = getPartnerId(playerId);

		if (partnerId == null)
		{
			return null;
		}

		return ctx.plugin().getServer().getOfflinePlayer(partnerId).getName();
	}

	/** Every married couple once, the most recently loving first */
	public List<MarriedCouple> getMarriedCouples()
	{
		List<MarriedCouple> couples = new ArrayList<MarriedCouple>();
		Set<UUID> listed = new HashSet<UUID>();

		for (Marriage marriage : new ArrayList<Marriage>(this.repository.all()))
		{
			UUID playerId = marriage.playerId();
			UUID partnerId = getPartnerId(playerId);

			if (partnerId == null || listed.contains(partnerId))
			{
				continue;
			}

			Instant lastLove = marriage.wedlock().lastLove();
			if (lastLove == null)
			{
				ctx.plugin().log("Invalid lastlove format for " + playerId);
				lastLove = DATES.now();

				setLastLove(playerId, lastLove);
				setLastLove(partnerId, lastLove);

				this.repository.save();
			}

			couples.add(new MarriedCouple(playerId, partnerId, ctx.believerService().getGodForBeliever(playerId), Date.from(lastLove)));
			listed.add(playerId);
		}

		// Most recent love first
		couples.sort(Comparator.comparing(MarriedCouple::lastLove).reversed());

		return couples;
	}

	/** Sets when the player last loved their partner (an entry without a marriage gets one with only that time, as before) */
	private void setLastLove(UUID playerId, Instant lastLove)
	{
		Marriage marriage = this.repository.getOrCreate(playerId);
		Marriage.Wedlock wedlock = marriage.wedlock();
		marriage.setWedlock(wedlock != null ? new Marriage.Wedlock(wedlock.partner(), wedlock.since(), lastLove) : new Marriage.Wedlock(null, null, lastLove));
	}

	/** /g love: the partner is told, and both get hearts if they are online */
	public void love(UUID playerId)
	{
		UUID partnerId = getPartnerId(playerId);

		if (partnerId != null)
		{
			Player player = ctx.plugin().getServer().getPlayer(playerId);
			Player partner = ctx.plugin().getServer().getPlayer(partnerId);

			Instant now = DATES.now();
			setLastLove(playerId, now);
			setLastLove(partnerId, now);

			if (player != null && partner != null)
			{
				ctx.plugin().getServer().getScheduler().scheduleSyncDelayedTask(ctx.plugin(), new TaskLove(ctx, player, partner), 1L);

				ctx.plugin().sendInfo(partner.getUniqueId(), LanguageManager.LANGUAGESTRING.MarrigeLovesYou, ChatColor.GREEN, player.getDisplayName(), ChatColor.DARK_RED + "LOVES", 1);
			}
		}

		this.repository.save();
	}

	/** Removes both partners' entries; the partner is told if online */
	public void divorce(UUID believerId)
	{
		UUID partnerId = getPartnerId(believerId);

		this.repository.remove(believerId);

		if (partnerId != null)
		{
			this.repository.remove(partnerId);

			Player partner = ctx.plugin().getServer().getPlayer(partnerId);
			if (partner != null)
			{
				// Tell the partner who divorced them
				Player divorcer = ctx.plugin().getServer().getPlayer(believerId);
				String divorcerName = divorcer != null ? divorcer.getDisplayName() : ctx.plugin().getServer().getOfflinePlayer(believerId).getName();

				ctx.plugin().sendInfo(partner.getUniqueId(), LanguageManager.LANGUAGESTRING.DivorcedYou, ChatColor.RED, divorcerName, "DIVORCED", 1);
			}
		}

		this.repository.save();
	}

	// ---------------------------------------------------------------------
	// Proposals and weddings

	/** /g marry: both players get a proposal from the other, replacing any engagement or marriage */
	public void proposeMarriage(UUID player1, UUID player2)
	{
		Instant now = DATES.now();

		Marriage first = this.repository.getOrCreate(player1);
		Marriage second = this.repository.getOrCreate(player2);
		for (Marriage marriage : List.of(first, second))
		{
			marriage.setEngagement(null);
			marriage.setWedlock(null);
		}
		first.setProposal(new Marriage.Proposal(player2, now));
		second.setProposal(new Marriage.Proposal(player1, now));

		this.repository.save();
	}

	/**
	 * The player who proposed to {@code believerId} in the last {@link #PROPOSAL_SECONDS} seconds, or null. An expired
	 * proposal is removed.
	 */
	public UUID getProposal(UUID believerId)
	{
		Marriage marriage = find(believerId);
		if (marriage == null || marriage.proposal() == null)
		{
			return null;
		}

		Instant time = marriage.proposal().time();
		if (time == null)
		{
			ctx.plugin().logDebug("Could no parse marriage proposal time");
		}

		long diffSeconds = BelieverRules.secondsSince(time, Instant.now());
		if (diffSeconds > PROPOSAL_SECONDS)
		{
			ctx.plugin().logDebug("getProposal DiffSeconds is " + diffSeconds);

			marriage.setProposal(null);

			this.repository.save();

			return null;
		}

		return marriage.proposal().partner();
	}

	/** A player accepted a proposal by praying: both are now getting married, if both are online */
	public void handleAcceptProposal(UUID playerId1, UUID playerId2, String godName)
	{
		Player player1 = ctx.plugin().getServer().getPlayer(playerId1);
		Player player2 = ctx.plugin().getServer().getPlayer(playerId2);

		// The proposal can be accepted after the other player logged off
		clearProposal(playerId1);
		clearProposal(playerId2);
		if (player1 == null || player2 == null)
		{
			this.repository.save();
			return;
		}

		try
		{
			ctx.languageManager().setType(ctx.languageManager().getItemTypeName(this.marriageTokenType));
		}
		catch (Exception ex)
		{
			ctx.plugin().logDebug(ex.getMessage());
		}

		ctx.languageManager().setPlayerName(player2.getDisplayName());
		ctx.godManager().GodSay(godName, player1, LanguageManager.LANGUAGESTRING.GodToBelieverAcceptedMarriageProposal, 2 + this.random.nextInt(40));

		ctx.languageManager().setPlayerName(player1.getDisplayName());
		ctx.godManager().GodSay(godName, player2, LanguageManager.LANGUAGESTRING.GodToBelieverAcceptedYourMarriageProposal, 2 + this.random.nextInt(40));

		setGettingMarried(playerId1, playerId2);
	}

	private void clearProposal(UUID playerId)
	{
		Marriage marriage = find(playerId);
		if (marriage != null)
		{
			marriage.setProposal(null);
		}
	}

	public UUID getGettingMarriedPartner(UUID playerId)
	{
		Marriage marriage = find(playerId);
		return marriage != null && marriage.engagement() != null ? marriage.engagement().partner() : null;
	}

	public boolean hasPickupWeddingRing(UUID playerId)
	{
		if (playerId == null)
		{
			return false;
		}
		Marriage marriage = find(playerId);
		return marriage != null && marriage.engagement() != null && marriage.engagement().tokenPickedUp();
	}

	public void setGettingMarried(UUID player1, UUID player2)
	{
		Instant now = DATES.now();

		Marriage first = this.repository.getOrCreate(player1);
		Marriage second = this.repository.getOrCreate(player2);
		for (Marriage marriage : List.of(first, second))
		{
			marriage.setWedlock(null);
			marriage.setProposal(null);
		}
		first.setEngagement(new Marriage.Engagement(player2, now, false));
		second.setEngagement(new Marriage.Engagement(player1, now, false));

		this.repository.save();
	}

	public void setMarried(UUID player1, UUID player2)
	{
		Instant now = DATES.now();

		Marriage first = this.repository.getOrCreate(player1);
		Marriage second = this.repository.getOrCreate(player2);
		for (Marriage marriage : List.of(first, second))
		{
			marriage.setEngagement(null);
			marriage.setProposal(null);
		}
		// LastLove isn't set: the first /g marriages sets it to then, as before
		first.setWedlock(new Marriage.Wedlock(player2, now, keptLastLove(first)));
		second.setWedlock(new Marriage.Wedlock(player1, now, keptLastLove(second)));

		this.repository.save();
	}

	private static Instant keptLastLove(Marriage marriage)
	{
		return marriage.wedlock() != null ? marriage.wedlock().lastLove() : null;
	}

	/** An engaged player picked up a wedding token: when both partners have one, they are married */
	public void handlePickupItem(Player player, Item item)
	{
		if (item.getItemStack().getType() != this.marriageTokenType)
		{
			return;
		}

		// Already married
		if (getPartnerId(player.getUniqueId()) != null)
		{
			return;
		}

		UUID gettingMarriedPartnerId = getGettingMarriedPartner(player.getUniqueId());

		if (gettingMarriedPartnerId == null)
		{
			return;
		}

		Player partner = ctx.plugin().getServer().getPlayer(gettingMarriedPartnerId);

		if (partner == null)
		{
			return;
		}

		Marriage marriage = this.repository.getOrCreate(player.getUniqueId());
		Marriage.Engagement engagement = marriage.engagement();
		marriage.setEngagement(new Marriage.Engagement(engagement.partner(), engagement.since(), true));

		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());

		if (hasPickupWeddingRing(gettingMarriedPartnerId))
		{
			setMarried(player.getUniqueId(), gettingMarriedPartnerId);

			float godPower = ctx.godManager().getGodPower(godName);

			ctx.godManager().blessPlayer(godName, player.getUniqueId(), godPower);
			ctx.godManager().blessPlayer(godName, gettingMarriedPartnerId, godPower);

			ctx.languageManager().setPlayerName(partner.getDisplayName());
			ctx.godManager().GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverMarried, 10);

			ctx.languageManager().setPlayerName(player.getDisplayName());
			ctx.godManager().GodSay(godName, partner, LanguageManager.LANGUAGESTRING.GodToBelieverMarried, 10);

			ctx.plugin().log(player.getDisplayName() + " just married " + partner.getDisplayName() + " in the name of " + godName);
			ctx.plugin().getServer().broadcastMessage(ChatColor.WHITE + player.getDisplayName() + ChatColor.AQUA + " just married " + ChatColor.WHITE + partner.getDisplayName() + ChatColor.AQUA + " in the name of " + ChatColor.GOLD + godName + ChatColor.AQUA + "!");
			if ((ctx.settings().holyArtifacts().enabled()) && (ctx.settings().marriage().weddingFireworks()))
			{
				ctx.holyPowerManager().shootFirework(player, 16);
				ctx.holyPowerManager().shootFirework(partner, 16);
			}
		}
		else
		{
			try
			{
				ctx.languageManager().setType(ctx.languageManager().getItemTypeName(this.marriageTokenType));
			}
			catch (Exception ex)
			{
				ctx.plugin().logDebug(ex.getMessage());
			}
			ctx.languageManager().setPlayerName(partner.getDisplayName());
			ctx.godManager().GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverMarriageTokenPickup, 10);

			ctx.languageManager().setPlayerName(player.getDisplayName());
			ctx.godManager().GodSay(godName, partner, LanguageManager.LANGUAGESTRING.GodToBelieverMarriagePartnerTokenPickup, 10);
		}

		this.repository.save();
	}
}
