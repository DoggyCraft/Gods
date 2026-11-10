package com.dogonfire.gods.service;

import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.managers.LanguageManager.LANGUAGESTRING;
import com.dogonfire.gods.model.Believer;
import com.dogonfire.gods.storage.BelieverRepository;
import com.dogonfire.gods.util.LegacyDates;

/**
 * Believers: which god each player believes in, their prayers and prayer power, chat mode, invitations, priest offers
 * and blessing cooldowns. Owns every change to believers.yml ({@link BelieverRepository}); the time rules are in
 * {@link BelieverRules}. A change to a believer's god or chat mode refreshes the chat cache (prayer power reaches it
 * through its regular refresh). Main thread only.
 */
public class BelieverService
{
	private final GodsContext			ctx;
	private final BelieverRepository	repository;

	public BelieverService(GodsContext ctx, BelieverRepository repository)
	{
		this.ctx = ctx;
		this.repository = repository;
	}

	public void load()
	{
		this.repository.load();
	}

	private static Instant now()
	{
		return LegacyDates.SECONDS.now();
	}

	private Believer find(UUID believerId)
	{
		return this.repository.find(believerId);
	}

	private Believer getOrCreate(UUID believerId)
	{
		return this.repository.getOrCreate(believerId);
	}

	private void changed()
	{
		this.repository.markDirty();
	}

	// ---------------------------------------------------------------------
	// Gods and prayers

	public String getGodForBeliever(UUID believerId)
	{
		Believer believer = find(believerId);
		return believer != null ? believer.godName() : null;
	}

	/** Every player with an entry in believers.yml, believing in a god or not */
	public int getBelieverCount()
	{
		return this.repository.size();
	}

	public Set<UUID> getBelieversForGod(String godName)
	{
		Set<UUID> believers = new HashSet<UUID>();
		for (Believer believer : this.repository.all())
		{
			if (believer.godName() != null && believer.godName().equals(godName))
			{
				believers.add(believer.id());
			}
		}
		return believers;
	}

	public Set<UUID> getOnlineBelieversForGod(String godName)
	{
		Set<UUID> believers = new HashSet<UUID>();
		for (Player player : ctx.plugin().getServer().getOnlinePlayers())
		{
			String believerGod = getGodForBeliever(player.getUniqueId());
			if (believerGod != null && believerGod.equals(godName))
			{
				believers.add(player.getUniqueId());
			}
		}
		return believers;
	}

	/**
	 * A prayer at an altar: counts if enough time passed since the last one (or it is to a new god, which starts the
	 * count over and records when they joined). Returns false if it is too soon.
	 */
	public boolean addPrayer(UUID believerId, String godName)
	{
		return pray(believerId, godName, 1, true);
	}

	/** Like {@link #addPrayer}, but counts {@code incPrayers} prayers and doesn't record a new god's join time */
	public boolean increasePrayer(UUID believerId, String godName, int incPrayers)
	{
		return pray(believerId, godName, incPrayers, false);
	}

	private boolean pray(UUID believerId, String godName, int amount, boolean recordJoin)
	{
		Instant now = now();
		if (!BelieverRules.canPray(find(believerId), godName, now, ctx.settings().prayers().minMinutesBetweenPrayers()))
		{
			return false;
		}

		BelieverRules.recordPrayer(getOrCreate(believerId), godName, amount, now, recordJoin);
		changed();
		ctx.chatManager().refresh(believerId);
		return true;
	}

	/** One more prayer to {@code godName}, whenever the last one was */
	public boolean incPrayer(UUID believerId, String godName)
	{
		Believer believer = getOrCreate(believerId);
		believer.setGodName(godName);
		believer.setPrayers(believer.prayers() + 1);
		changed();
		ctx.chatManager().refresh(believerId);
		return true;
	}

	public void setLastPrayerDate(UUID believerId)
	{
		getOrCreate(believerId).setLastPrayer(now());
		changed();
	}

	public int getPrayers(UUID believerId)
	{
		Believer believer = find(believerId);
		return believer != null ? believer.prayers() : 0;
	}

	/** When the believer last prayed; 1970 if never */
	public Date getLastPrayerTime(UUID believerId)
	{
		Believer believer = find(believerId);
		return new Date(BelieverRules.millis(believer != null ? believer.lastPrayer() : null));
	}

	private Instant lastPrayer(UUID believerId)
	{
		Believer believer = find(believerId);
		return believer != null ? believer.lastPrayer() : null;
	}

	public float getBelieverPower(UUID believerId)
	{
		return BelieverRules.believerPower(getPrayers(believerId), lastPrayer(believerId), Instant.now());
	}

	public int getTimeUntilCanPray(UUID believerId)
	{
		return BelieverRules.minutesUntilCanPray(lastPrayer(believerId), Instant.now(), ctx.settings().prayers().minMinutesBetweenPrayers());
	}

	public boolean hasRecentPrayer(UUID believerId)
	{
		return BelieverRules.hasRecentPrayer(lastPrayer(believerId), Instant.now(), ctx.settings().prayers().minMinutesBetweenPrayers());
	}

	/** The believer leaves {@code godName} (if that is their god); the rest of their entry stays */
	public void believerLeave(String godName, UUID believerId)
	{
		if (godName == null || believerId == null)
		{
			return;
		}
		Believer believer = find(believerId);
		if (believer == null || !godName.equals(believer.godName()))
		{
			return;
		}
		believer.setGodName(null);
		changed();
		ctx.chatManager().refresh(believerId);
	}

	/** Removes the believer's whole entry, if they believe in {@code godName} or in no god */
	public void removeBeliever(String godName, UUID believerId)
	{
		String believerGodName = getGodForBeliever(believerId);
		if (believerGodName != null && !believerGodName.equals(godName))
		{
			return;
		}

		clearPrayerPower(believerId);
		this.repository.remove(believerId);

		ctx.plugin().log(godName + " lost " + believerId + " as believer");

		changed();
		ctx.chatManager().refresh(believerId);
	}

	// ---------------------------------------------------------------------
	// Changing god

	/** The believer prayed at another god's altar and was asked to confirm */
	public void setChangingGod(UUID believerId)
	{
		getOrCreate(believerId).setChangingGod(now());
		changed();
	}

	/** Whether the believer asked to change god in the last few seconds (so praying again confirms it) */
	public boolean getChangingGod(UUID believerId)
	{
		Believer believer = find(believerId);
		return BelieverRules.isChangingGod(believer != null ? believer.changingGod() : null, Instant.now());
	}

	public void clearChangingGod(UUID believerId)
	{
		Believer believer = find(believerId);
		if (believer != null)
		{
			believer.setChangingGod(null);
		}
		changed();
	}

	/** Whether the believer joined their god too recently to change again */
	public boolean hasRecentGodChange(UUID believerId)
	{
		Believer believer = find(believerId);
		return BelieverRules.joinedRecently(believer != null ? believer.joined() : null, Instant.now(), ctx.settings().believers().minSecondsBetweenChangingGod());
	}

	// ---------------------------------------------------------------------
	// Prayer power

	public int getPrayerPower(UUID believerId)
	{
		Believer believer = find(believerId);
		return believer != null ? believer.prayerPower() : 0;
	}

	/** Adds {@code powerChange} (may be negative; never below 0) and tells the believer */
	public void increasePrayerPower(UUID believerId, int powerChange)
	{
		Believer believer = getOrCreate(believerId);
		int prayerPower = Math.max(0, believer.prayerPower() + powerChange);
		believer.setPrayerPower(prayerPower);
		changed();

		ctx.godSpeech().sendInfo(believerId, LANGUAGESTRING.YourPrayerPower, ChatColor.AQUA, prayerPower, "", 10);
	}

	public void clearPrayerPower(UUID believerId)
	{
		Believer believer = find(believerId);
		if (believer != null)
		{
			believer.setPrayerPower(0);
		}
		changed();

		ctx.godSpeech().sendInfo(believerId, LANGUAGESTRING.YourPrayerPower, ChatColor.AQUA, 0, "", 10);
	}

	// ---------------------------------------------------------------------
	// Chat and hunting

	public boolean getReligionChat(UUID believerId)
	{
		Believer believer = find(believerId);
		return believer != null && believer.religionChat();
	}

	public void setReligionChat(UUID believerId, boolean enabled)
	{
		if (enabled)
		{
			getOrCreate(believerId).setReligionChat(true);
		}
		else
		{
			Believer believer = find(believerId);
			if (believer != null)
			{
				believer.setReligionChat(false);
			}
		}
		changed();
		ctx.chatManager().refresh(believerId);
	}

	public boolean isHunting(UUID believerId)
	{
		Believer believer = find(believerId);
		return believer != null && believer.hunting();
	}

	public void setHunting(UUID believerId, boolean hunting)
	{
		ctx.plugin().logDebug("Setting hunting string to '" + hunting + "' for " + believerId);

		getOrCreate(believerId).setHunting(hunting);
		changed();
	}

	// ---------------------------------------------------------------------
	// Invitations and priest offers

	public void setInvitation(UUID believerId, String godName)
	{
		getOrCreate(believerId).setInvitation(godName, now());
		changed();
	}

	/** The god that invited the player in the last {@link BelieverRules#INVITATION_SECONDS} seconds, or null */
	public String getInvitation(UUID believerId)
	{
		Believer believer = find(believerId);
		if (BelieverRules.invitationExpired(believer != null ? believer.invitationTime() : null, Instant.now()))
		{
			clearInvitation(believerId);
			return null;
		}
		return believer.invitationGod();
	}

	public void clearInvitation(UUID believerId)
	{
		Believer believer = find(believerId);
		if (believer != null)
		{
			believer.setInvitation(null, null);
		}
		changed();
	}

	public void setPendingPriest(UUID believerId)
	{
		getOrCreate(believerId).setLastPriestOffer(now());
		changed();
	}

	public void clearPendingPriest(UUID believerId)
	{
		Believer believer = find(believerId);
		if (believer != null)
		{
			believer.setLastPriestOffer(null);
		}
		changed();
	}

	/** Whether the believer was offered priesthood in the last {@link BelieverRules#PRIEST_OFFER_MINUTES} minutes */
	public boolean hasRecentPriestOffer(UUID believerId)
	{
		Believer believer = find(believerId);
		return BelieverRules.minutesSince(believer != null ? believer.lastPriestOffer() : null, Instant.now()) <= BelieverRules.PRIEST_OFFER_MINUTES;
	}

	// ---------------------------------------------------------------------
	// Blessing and cursing cooldowns

	public void setBlessingTime(UUID believerId)
	{
		getOrCreate(believerId).setLastBlessing(now());
		changed();
	}

	public boolean hasRecentBlessing(UUID believerId)
	{
		Believer believer = find(believerId);
		return BelieverRules.secondsSince(believer != null ? believer.lastBlessing() : null, Instant.now()) < ctx.settings().blessings().minSecondsBetweenBlessings();
	}

	public void setCursingTime(UUID believerId)
	{
		getOrCreate(believerId).setLastCursing(now());
		changed();
	}

	public boolean hasRecentCursing(UUID believerId)
	{
		Believer believer = find(believerId);
		return BelieverRules.secondsSince(believer != null ? believer.lastCursing() : null, Instant.now()) < ctx.settings().curses().minSecondsBetweenCurses();
	}

	public void setItemBlessingTime(UUID believerId)
	{
		getOrCreate(believerId).setLastItemBlessing(now());
		changed();
	}

	public boolean hasRecentItemBlessing(UUID believerId)
	{
		Believer believer = find(believerId);
		return BelieverRules.minutesSince(believer != null ? believer.lastItemBlessing() : null, Instant.now()) < ctx.settings().itemBlessings().minMinutesBetweenBlessings();
	}

	public void setHolyArtifactBlessingTime(UUID believerId)
	{
		getOrCreate(believerId).setLastHolyArtifactBlessing(now());
		changed();
	}

	public boolean hasRecentHolyArtifactBlessing(UUID believerId)
	{
		Believer believer = find(believerId);
		return BelieverRules.minutesSince(believer != null ? believer.lastHolyArtifactBlessing() : null, Instant.now()) < ctx.settings().holyArtifacts().minMinutesBetweenBlessings();
	}
}
