package com.dogonfire.gods.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.model.Marriage;
import com.dogonfire.gods.model.MarriedCouple;
import com.dogonfire.gods.storage.MarriageRepository;
import com.dogonfire.gods.task.LoveTask;
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
				ctx.plugin().getServer().getScheduler().scheduleSyncDelayedTask(ctx.plugin(), new LoveTask(ctx, player, partner), 1L);

				ctx.godSpeech().sendInfo(partner.getUniqueId(), "marriage.loves-you", 1, Messages.displayName(player.getDisplayName()));
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

				ctx.godSpeech().sendInfo(partner.getUniqueId(), "marriage.divorced-you", 1, Messages.displayName(divorcerName));
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

		TagResolver token = Messages.type(ctx.messages().itemName(this.marriageTokenType));
		ctx.godSpeech().say(godName, player1, Voice.BELIEVER_ACCEPTED_MARRIAGE_PROPOSAL, 2, token, Messages.displayName(player2.getDisplayName()));
		ctx.godSpeech().say(godName, player2, Voice.BELIEVER_ACCEPTED_YOUR_MARRIAGE_PROPOSAL, 20, token, Messages.displayName(player1.getDisplayName()));

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

			float godPower = ctx.godService().getGodPower(godName);

			ctx.blessingService().blessPlayer(godName, player.getUniqueId(), godPower);
			ctx.blessingService().blessPlayer(godName, gettingMarriedPartnerId, godPower);

			ctx.godSpeech().say(godName, player, Voice.BELIEVER_MARRIED, 10, Messages.displayName(partner.getDisplayName()));
			ctx.godSpeech().say(godName, partner, Voice.BELIEVER_MARRIED, 10, Messages.displayName(player.getDisplayName()));

			ctx.plugin().log(player.getDisplayName() + " just married " + partner.getDisplayName() + " in the name of " + godName);
			ctx.messages().broadcast(ctx.plugin().getServer(), "broadcast.married", Messages.displayName(player.getDisplayName()), Messages.legacyText("partner", partner.getDisplayName()), Messages.god(godName));
			if ((ctx.settings().holyArtifacts().enabled()) && (ctx.settings().marriage().weddingFireworks()))
			{
				ctx.holyPowerService().shootFirework(player, 16);
				ctx.holyPowerService().shootFirework(partner, 16);
			}
		}
		else
		{
			TagResolver token = Messages.type(ctx.messages().itemName(this.marriageTokenType));
			ctx.godSpeech().say(godName, player, Voice.BELIEVER_MARRIAGE_TOKEN_PICKUP, 10, token, Messages.displayName(partner.getDisplayName()));
			ctx.godSpeech().say(godName, partner, Voice.BELIEVER_MARRIAGE_PARTNER_TOKEN_PICKUP, 10, token, Messages.displayName(player.getDisplayName()));
		}

		this.repository.save();
	}
}
