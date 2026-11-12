package com.dogonfire.gods.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One player's entry in believers.yml: the god they believe in (none after leaving it), their prayers and prayer
 * power, devotion, chat mode, a pending invitation and the times of their last prayer, blessings and offers. Every player who
 * ever prayed, was invited or offered priesthood has one. Times are {@code null} when never set (or unreadable). A
 * plain data object: {@code BelieverService} changes it and marks the repository dirty.
 */
public final class Believer
{
	private final UUID			id;

	private String				godName;						// God
	private int					prayers;						// Prayers
	private Instant				lastPrayer;						// LastPrayer
	private Instant				joined;							// Joined: when they started praying to this god
	private int					prayerPower;					// PrayerPower
	private Instant				prayerPowerDecayed;				// PrayerPowerDecayed: how far the decay of their prayer power has been counted
	private boolean				religionChat;					// ReligionChat
	private boolean				hunting;						// Hunting
	private String				invitationGod;					// Invitation.God
	private Instant				invitationTime;					// Invitation.Time
	private Instant				lastPriestOffer;				// LastPriestOffer
	private Instant				changingGod;					// ChangingGod: when they last tried to change god
	private Instant				lastBlessing;					// LastBlessingTime
	private Instant				lastCursing;					// LastCursingTime
	private Instant				lastItemBlessing;				// LastItemBlessingTime
	private Instant				lastHolyArtifactBlessing;		// LastHolyArtifactBlessingTime
	private UUID				invitedBy;						// Invitation.By: who invited them, if Gods knows
	private Integer				devotion;						// Devotion: their devotion points; null for a believer from before devotion
	private DevotionLevel		devotionLevel;					// DevotionLevel: null reads as FOLLOWER
	private Instant				devotionSince;					// DevotionSince: when they reached their level
	private Instant				devotionFaded;					// DevotionFaded: how far fading has been counted

	/** Keys of the entry this version doesn't know, written back unchanged */
	private Map<String, Object>	extras	= new LinkedHashMap<String, Object>();

	public Believer(UUID id)
	{
		this.id = id;
	}

	public UUID id()
	{
		return this.id;
	}

	public String godName()
	{
		return this.godName;
	}

	public void setGodName(String godName)
	{
		this.godName = godName;
	}

	public int prayers()
	{
		return this.prayers;
	}

	public void setPrayers(int prayers)
	{
		this.prayers = prayers;
	}

	public Instant lastPrayer()
	{
		return this.lastPrayer;
	}

	public void setLastPrayer(Instant lastPrayer)
	{
		this.lastPrayer = lastPrayer;
	}

	public Instant joined()
	{
		return this.joined;
	}

	public void setJoined(Instant joined)
	{
		this.joined = joined;
	}

	public int prayerPower()
	{
		return this.prayerPower;
	}

	public void setPrayerPower(int prayerPower)
	{
		this.prayerPower = prayerPower;
	}

	/** Up to when the decay of their prayer power has been counted; null if it never decayed */
	public Instant prayerPowerDecayed()
	{
		return this.prayerPowerDecayed;
	}

	public void setPrayerPowerDecayed(Instant prayerPowerDecayed)
	{
		this.prayerPowerDecayed = prayerPowerDecayed;
	}

	public boolean religionChat()
	{
		return this.religionChat;
	}

	public void setReligionChat(boolean religionChat)
	{
		this.religionChat = religionChat;
	}

	public boolean hunting()
	{
		return this.hunting;
	}

	public void setHunting(boolean hunting)
	{
		this.hunting = hunting;
	}

	public String invitationGod()
	{
		return this.invitationGod;
	}

	public Instant invitationTime()
	{
		return this.invitationTime;
	}

	/** Both null to clear the invitation */
	public void setInvitation(String godName, Instant time)
	{
		this.invitationGod = godName;
		this.invitationTime = time;
	}

	public Instant lastPriestOffer()
	{
		return this.lastPriestOffer;
	}

	public void setLastPriestOffer(Instant lastPriestOffer)
	{
		this.lastPriestOffer = lastPriestOffer;
	}

	public Instant changingGod()
	{
		return this.changingGod;
	}

	public void setChangingGod(Instant changingGod)
	{
		this.changingGod = changingGod;
	}

	public Instant lastBlessing()
	{
		return this.lastBlessing;
	}

	public void setLastBlessing(Instant lastBlessing)
	{
		this.lastBlessing = lastBlessing;
	}

	public Instant lastCursing()
	{
		return this.lastCursing;
	}

	public void setLastCursing(Instant lastCursing)
	{
		this.lastCursing = lastCursing;
	}

	public Instant lastItemBlessing()
	{
		return this.lastItemBlessing;
	}

	public void setLastItemBlessing(Instant lastItemBlessing)
	{
		this.lastItemBlessing = lastItemBlessing;
	}

	public Instant lastHolyArtifactBlessing()
	{
		return this.lastHolyArtifactBlessing;
	}

	public void setLastHolyArtifactBlessing(Instant lastHolyArtifactBlessing)
	{
		this.lastHolyArtifactBlessing = lastHolyArtifactBlessing;
	}

	/** Who sent their pending invitation, or null if it came from the god itself (the API) or isn't known */
	public UUID invitedBy()
	{
		return this.invitedBy;
	}

	public void setInvitedBy(UUID invitedBy)
	{
		this.invitedBy = invitedBy;
	}

	/** Their devotion points; null for a believer from before devotion, who is seeded once */
	public Integer devotion()
	{
		return this.devotion;
	}

	public void setDevotion(Integer devotion)
	{
		this.devotion = devotion;
	}

	/** Their devotion level; null when never set (a follower) */
	public DevotionLevel devotionLevel()
	{
		return this.devotionLevel;
	}

	public void setDevotionLevel(DevotionLevel devotionLevel)
	{
		this.devotionLevel = devotionLevel;
	}

	public Instant devotionSince()
	{
		return this.devotionSince;
	}

	public void setDevotionSince(Instant devotionSince)
	{
		this.devotionSince = devotionSince;
	}

	/** Up to when their devotion has faded; null if it never did */
	public Instant devotionFaded()
	{
		return this.devotionFaded;
	}

	public void setDevotionFaded(Instant devotionFaded)
	{
		this.devotionFaded = devotionFaded;
	}

	public Map<String, Object> extras()
	{
		return this.extras;
	}

	public void setExtras(Map<String, Object> extras)
	{
		this.extras = extras;
	}
}
