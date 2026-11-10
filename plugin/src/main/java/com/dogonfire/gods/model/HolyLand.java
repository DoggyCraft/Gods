package com.dogonfire.gods.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A region claimed by a god when a believer built or prayed at an altar there, and the state of an attack on it by
 * another god (a priest started it). In holyland.yml it is {@code Holylands.<world key>.<hash>} with the keys noted on
 * the fields; times are {@code "HH:mm dd-MM-yyyy"}. A plain data object: {@code HolyLandService} changes it.
 */
public final class HolyLand
{
	private final RegionKey		region;

	private String				godName;			// GodName
	private String				name;				// Name, generated from the biome
	private Instant				firstPrayerTime;	// FirstPrayerTime
	private Instant				lastPrayerTime;		// LastPrayerTime: abandoned lands are removed after some days
	private String				worldName;			// World: the real world name

	// The attack, while there is one
	private String				attackingGodName;	// AttackingGodName
	private Instant				contestedTime;		// ContestedTime: when the attack started
	private int					attackerKills;		// AttackerKills
	private int					defenderKills;		// DefenderKills
	private int					desecration;		// Desecration: unholy meals by outsiders

	/** Keys of the entry this version doesn't know, written back unchanged */
	private Map<String, Object>	extras				= new LinkedHashMap<String, Object>();

	public HolyLand(RegionKey region)
	{
		this.region = region;
	}

	public RegionKey region()
	{
		return this.region;
	}

	public String godName()
	{
		return this.godName;
	}

	public void setGodName(String godName)
	{
		this.godName = godName;
	}

	public String name()
	{
		return this.name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public Instant firstPrayerTime()
	{
		return this.firstPrayerTime;
	}

	public void setFirstPrayerTime(Instant firstPrayerTime)
	{
		this.firstPrayerTime = firstPrayerTime;
	}

	public Instant lastPrayerTime()
	{
		return this.lastPrayerTime;
	}

	public void setLastPrayerTime(Instant lastPrayerTime)
	{
		this.lastPrayerTime = lastPrayerTime;
	}

	public String worldName()
	{
		return this.worldName;
	}

	public void setWorldName(String worldName)
	{
		this.worldName = worldName;
	}

	public String attackingGodName()
	{
		return this.attackingGodName;
	}

	public void setAttackingGodName(String attackingGodName)
	{
		this.attackingGodName = attackingGodName;
	}

	public Instant contestedTime()
	{
		return this.contestedTime;
	}

	public void setContestedTime(Instant contestedTime)
	{
		this.contestedTime = contestedTime;
	}

	public int attackerKills()
	{
		return this.attackerKills;
	}

	public void setAttackerKills(int attackerKills)
	{
		this.attackerKills = attackerKills;
	}

	public int defenderKills()
	{
		return this.defenderKills;
	}

	public void setDefenderKills(int defenderKills)
	{
		this.defenderKills = defenderKills;
	}

	public int desecration()
	{
		return this.desecration;
	}

	public void setDesecration(int desecration)
	{
		this.desecration = desecration;
	}

	/** Ends the attack: forgets the attacker, its start time and the counters */
	public void clearContest()
	{
		this.attackingGodName = null;
		this.contestedTime = null;
		this.attackerKills = 0;
		this.defenderKills = 0;
		this.desecration = 0;
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
