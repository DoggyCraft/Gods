package com.dogonfire.gods.storage;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.dogonfire.gods.migration.HolyLandsV1ToV2;
import com.dogonfire.gods.migration.VersionedYaml;
import com.dogonfire.gods.model.HolyLand;
import com.dogonfire.gods.model.NeutralLand;
import com.dogonfire.gods.model.RegionKey;
import com.dogonfire.gods.util.LegacyDates;

/**
 * holyland.yml: the {@link HolyLand}s and {@link NeutralLand}s by {@link RegionKey}.
 * <pre>
 * data-version: 2
 * Holylands.&lt;world key&gt;.&lt;hash&gt;: GodName, Name, FirstPrayerTime, LastPrayerTime, World, AttackingGodName,
 *                                  ContestedTime, AttackerKills, DefenderKills, Desecration
 * NeutralLands.&lt;world key&gt;.&lt;hash&gt;: &lt;time set&gt;
 * </pre>
 * Version 1 (no {@code data-version}) is upgraded by {@link HolyLandsV1ToV2}.
 */
public final class HolyLandRepository extends Repository
{
	public static final int					DATA_VERSION		= 2;
	public static final String				VERSION_KEY			= "data-version";

	private static final String				HOLY_LANDS			= "Holylands";
	private static final String				NEUTRAL_LANDS		= "NeutralLands";

	private static final String				GOD_NAME			= "GodName";
	private static final String				NAME				= "Name";
	private static final String				FIRST_PRAYER_TIME	= "FirstPrayerTime";
	private static final String				LAST_PRAYER_TIME	= "LastPrayerTime";
	private static final String				WORLD				= "World";
	private static final String				ATTACKING_GOD_NAME	= "AttackingGodName";
	private static final String				CONTESTED_TIME		= "ContestedTime";
	private static final String				ATTACKER_KILLS		= "AttackerKills";
	private static final String				DEFENDER_KILLS		= "DefenderKills";
	private static final String				DESECRATION			= "Desecration";

	private static final Set<String>		KNOWN				= Set.of(GOD_NAME, NAME, FIRST_PRAYER_TIME, LAST_PRAYER_TIME, WORLD, ATTACKING_GOD_NAME, CONTESTED_TIME,
			ATTACKER_KILLS, DEFENDER_KILLS, DESECRATION);

	private static final LegacyDates		DATES				= LegacyDates.MINUTES;

	private final Map<RegionKey, HolyLand>		lands			= new LinkedHashMap<RegionKey, HolyLand>();
	private final Map<RegionKey, NeutralLand>	neutralLands	= new LinkedHashMap<RegionKey, NeutralLand>();
	// Entries under Holylands/NeutralLands whose key is not a region hash: "Holylands.<world>.<key>" -> value
	private final Map<String, Object>			unknownEntries	= new LinkedHashMap<String, Object>();
	// Neutral lands whose time could not be parsed: the value as it was, written back while the land has no time
	private final Map<RegionKey, Object>		rawNeutralTimes	= new LinkedHashMap<RegionKey, Object>();

	/** The data version and migrations of holyland.yml */
	static VersionedYaml format(Logger logger)
	{
		return new VersionedYaml(VERSION_KEY, DATA_VERSION, 1, List.of(new HolyLandsV1ToV2(logger)));
	}

	HolyLandRepository(DataFile file, Logger logger)
	{
		super(file, logger);
	}

	/** The holy land of a region, or null */
	public HolyLand find(RegionKey region)
	{
		return this.lands.get(region);
	}

	/** The holy land of a region, created (empty, unclaimed) if there is none */
	public HolyLand getOrCreate(RegionKey region)
	{
		return this.lands.computeIfAbsent(region, HolyLand::new);
	}

	public void remove(RegionKey region)
	{
		this.lands.remove(region);
	}

	public Collection<HolyLand> all()
	{
		return Collections.unmodifiableCollection(this.lands.values());
	}

	public NeutralLand findNeutral(RegionKey region)
	{
		return this.neutralLands.get(region);
	}

	public void putNeutral(NeutralLand land)
	{
		this.neutralLands.put(land.region(), land);
	}

	public void removeNeutral(RegionKey region)
	{
		this.neutralLands.remove(region);
	}

	public Collection<NeutralLand> allNeutral()
	{
		return Collections.unmodifiableCollection(this.neutralLands.values());
	}

	/** The number of holy lands */
	@Override
	public int size()
	{
		return this.lands.size();
	}

	@Override
	protected void clear()
	{
		this.lands.clear();
		this.neutralLands.clear();
		this.unknownEntries.clear();
		this.rawNeutralTimes.clear();
	}

	@Override
	protected void read(YamlConfiguration yaml)
	{
		for (String key : yaml.getKeys(false))
		{
			if (key.equals(HOLY_LANDS))
			{
				forEachRegion(yaml.getConfigurationSection(key), HOLY_LANDS, (region, value) -> {
					if (!(value instanceof ConfigurationSection section))
					{
						return false;
					}
					this.lands.put(region, readLand(region, section));
					return true;
				});
			}
			else if (key.equals(NEUTRAL_LANDS))
			{
				forEachRegion(yaml.getConfigurationSection(key), NEUTRAL_LANDS, (region, value) -> {
					if (value instanceof ConfigurationSection)
					{
						return false;
					}
					Instant since = DATES.parse(value != null ? value.toString() : null);
					if (since == null && value != null && !value.toString().isEmpty())
					{
						this.rawNeutralTimes.put(region, value);
					}
					this.neutralLands.put(region, new NeutralLand(region, since));
					return true;
				});
			}
			else
			{
				// data-version, and whatever a newer version added
				keepUnknown(key, yaml.get(key));
			}
		}

		logger().info("Loaded " + this.lands.size() + " holy land entries.");
	}

	private interface RegionReader
	{
		/** Returns false if the value is not what was expected; it is then kept as it is */
		boolean read(RegionKey region, Object value);
	}

	private void forEachRegion(ConfigurationSection lands, String sectionName, RegionReader reader)
	{
		if (lands == null)
		{
			return;
		}
		for (String world : lands.getKeys(false))
		{
			ConfigurationSection worldLands = lands.getConfigurationSection(world);
			if (worldLands == null)
			{
				this.unknownEntries.put(sectionName + "." + world, toPlain(lands.get(world)));
				continue;
			}
			for (String hash : worldLands.getKeys(false))
			{
				Object value = worldLands.get(hash);
				RegionKey region = parseRegion(world, hash);
				if (region == null || !reader.read(region, value))
				{
					this.unknownEntries.put(sectionName + "." + world + "." + hash, toPlain(value));
				}
			}
		}
	}

	private static RegionKey parseRegion(String world, String hash)
	{
		try
		{
			return new RegionKey(world, Long.parseLong(hash));
		}
		catch (NumberFormatException ex)
		{
			return null;
		}
	}

	private static HolyLand readLand(RegionKey region, ConfigurationSection section)
	{
		HolyLand land = new HolyLand(region);
		// Times that can't be parsed are kept in the extras and written back as they were
		Map<String, Object> extras = extraKeys(section, KNOWN);
		land.setGodName(section.getString(GOD_NAME));
		land.setName(section.getString(NAME));
		land.setFirstPrayerTime(readTime(section, FIRST_PRAYER_TIME, DATES, extras));
		land.setLastPrayerTime(readTime(section, LAST_PRAYER_TIME, DATES, extras));
		land.setWorldName(section.getString(WORLD));
		land.setAttackingGodName(section.getString(ATTACKING_GOD_NAME));
		land.setContestedTime(readTime(section, CONTESTED_TIME, DATES, extras));
		land.setAttackerKills(section.getInt(ATTACKER_KILLS));
		land.setDefenderKills(section.getInt(DEFENDER_KILLS));
		land.setDesecration(section.getInt(DESECRATION));
		land.setExtras(extras);
		return land;
	}

	@Override
	protected void write(YamlConfiguration yaml)
	{
		for (HolyLand land : this.lands.values())
		{
			ConfigurationSection section = yaml.createSection(path(HOLY_LANDS, land.region()));
			section.set(GOD_NAME, land.godName());
			section.set(NAME, land.name());
			section.set(FIRST_PRAYER_TIME, DATES.format(land.firstPrayerTime()));
			section.set(LAST_PRAYER_TIME, DATES.format(land.lastPrayerTime()));
			section.set(WORLD, land.worldName());
			if (land.attackingGodName() != null)
			{
				section.set(ATTACKING_GOD_NAME, land.attackingGodName());
				section.set(CONTESTED_TIME, DATES.format(land.contestedTime()));
				section.set(ATTACKER_KILLS, land.attackerKills());
				section.set(DEFENDER_KILLS, land.defenderKills());
				section.set(DESECRATION, land.desecration());
			}
			writeExtras(section, land.extras());
		}
		for (NeutralLand land : this.neutralLands.values())
		{
			String since = DATES.format(land.since());
			Object raw = this.rawNeutralTimes.get(land.region());
			yaml.set(path(NEUTRAL_LANDS, land.region()), since != null ? since : raw != null ? raw : "");
		}
		for (Map.Entry<String, Object> entry : this.unknownEntries.entrySet())
		{
			if (!yaml.contains(entry.getKey()))
			{
				yaml.set(entry.getKey(), entry.getValue());
			}
		}
	}

	private static String path(String section, RegionKey region)
	{
		return section + "." + region.world() + "." + region.hash();
	}
}
