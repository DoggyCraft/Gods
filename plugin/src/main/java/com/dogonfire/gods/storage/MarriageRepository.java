package com.dogonfire.gods.storage;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.dogonfire.gods.migration.FirstDataVersion;
import com.dogonfire.gods.migration.VersionedYaml;
import com.dogonfire.gods.model.Marriage;
import com.dogonfire.gods.util.LegacyDates;

/**
 * marriages.yml: one {@link Marriage} per player UUID, with the keys Gods has always written:
 * <pre>
 * &lt;uuid&gt;.MarriageProposal.Partner / Time
 * &lt;uuid&gt;.GettingMarried.Partner / Time / HasPickupWeddingToken
 * &lt;uuid&gt;.Married.Partner / Time / LastLove
 * </pre>
 * Times are {@code "HH:mm:ss dd-MM-yyyy"}, plus {@code data-version: 2}. Version 1 (no {@code data-version}) has the
 * same keys; its upgrade ({@link FirstDataVersion}) makes the {@code marriages.yml.v1.bak} backup before this
 * repository first rewrites it. Entries keyed by something other than a UUID, and sections whose partner isn't a UUID
 * (very old files), are logged and kept unchanged.
 */
public final class MarriageRepository extends Repository
{
	public static final int					DATA_VERSION	= 2;
	public static final String				VERSION_KEY		= "data-version";

	private static final String				PARTNER		= "Partner";
	private static final String				TIME		= "Time";
	private static final String				TOKEN		= "HasPickupWeddingToken";
	private static final String				LAST_LOVE	= "LastLove";

	private static final Set<String>		KNOWN		= Set.of(Marriage.PROPOSAL, Marriage.ENGAGEMENT, Marriage.WEDLOCK);

	private static final LegacyDates		DATES		= LegacyDates.SECONDS;

	private final Map<UUID, Marriage>		marriages	= new LinkedHashMap<UUID, Marriage>();
	// Times that could not be parsed, by player and "<section>.<key>": written back as they were while that part of
	// the marriage has no time
	private final Map<UUID, Map<String, Object>>	rawTimes	= new LinkedHashMap<UUID, Map<String, Object>>();

	/** The data version and migrations of marriages.yml */
	static VersionedYaml format()
	{
		return new VersionedYaml(VERSION_KEY, DATA_VERSION, 1, List.of(new FirstDataVersion()));
	}

	MarriageRepository(DataFile file, Logger logger)
	{
		super(file, logger);
	}

	/** The player's entry, or null */
	public Marriage find(UUID playerId)
	{
		return this.marriages.get(playerId);
	}

	/** The player's entry, created (empty) if there is none */
	public Marriage getOrCreate(UUID playerId)
	{
		return this.marriages.computeIfAbsent(playerId, Marriage::new);
	}

	public void remove(UUID playerId)
	{
		this.marriages.remove(playerId);
	}

	public Collection<Marriage> all()
	{
		return Collections.unmodifiableCollection(this.marriages.values());
	}

	@Override
	public int size()
	{
		return this.marriages.size();
	}

	@Override
	protected void clear()
	{
		this.marriages.clear();
		this.rawTimes.clear();
	}

	@Override
	protected void read(YamlConfiguration yaml)
	{
		for (String key : yaml.getKeys(false))
		{
			if (key.equals(VERSION_KEY))
			{
				continue;
			}
			UUID playerId = parseId(key);
			ConfigurationSection section = yaml.getConfigurationSection(key);
			if (playerId == null || section == null)
			{
				if (playerId == null)
				{
					logger().info("Ignoring marriages.yml entry '" + key + "': it is not a player UUID");
				}
				keepUnknown(key, yaml.get(key));
				continue;
			}

			Marriage marriage = new Marriage(playerId);
			Map<String, Object> extras = extraKeys(section, KNOWN);

			ConfigurationSection proposal = section.getConfigurationSection(Marriage.PROPOSAL);
			if (proposal != null && readablePartner(key, proposal))
			{
				marriage.setProposal(new Marriage.Proposal(partner(proposal), time(playerId, proposal, TIME)));
			}
			ConfigurationSection engagement = section.getConfigurationSection(Marriage.ENGAGEMENT);
			if (engagement != null && readablePartner(key, engagement))
			{
				marriage.setEngagement(new Marriage.Engagement(partner(engagement), time(playerId, engagement, TIME), engagement.contains(TOKEN)));
			}
			ConfigurationSection wedlock = section.getConfigurationSection(Marriage.WEDLOCK);
			if (wedlock != null && readablePartner(key, wedlock))
			{
				marriage.setWedlock(new Marriage.Wedlock(partner(wedlock), time(playerId, wedlock, TIME), time(playerId, wedlock, LAST_LOVE)));
			}

			// Sections that couldn't be read stay as they are (the setters above dropped readable ones from extras)
			for (String name : KNOWN)
			{
				ConfigurationSection unread = section.getConfigurationSection(name);
				if (unread != null && !readablePartner(null, unread))
				{
					extras.put(name, toPlain(unread));
				}
				else if (unread == null && section.contains(name))
				{
					extras.put(name, toPlain(section.get(name)));
				}
			}
			marriage.setExtras(extras);
			this.marriages.put(playerId, marriage);
		}

		logger().info("Loaded " + this.marriages.size() + " marriages.");
	}

	/** A missing partner is fine (it reads as none); one that isn't a UUID is logged (once, when key is given) */
	private boolean readablePartner(String key, ConfigurationSection section)
	{
		String partner = section.getString(PARTNER);
		if (partner == null || parseId(partner) != null)
		{
			return true;
		}
		if (key != null)
		{
			logger().info("Ignoring the " + section.getName() + " partner '" + partner + "' of " + key + " in marriages.yml: it is not a player UUID");
		}
		return false;
	}

	private static UUID partner(ConfigurationSection section)
	{
		String partner = section.getString(PARTNER);
		return partner != null ? parseId(partner) : null;
	}

	/** The time, or null; one that can't be parsed is remembered as it was ({@link #rawTimes}) */
	private Instant time(UUID playerId, ConfigurationSection section, String key)
	{
		Object raw = section.get(key);
		if (raw == null)
		{
			return null;
		}
		Instant time = DATES.parse(raw.toString());
		if (time == null)
		{
			this.rawTimes.computeIfAbsent(playerId, id -> new LinkedHashMap<String, Object>()).put(section.getName() + "." + key, toPlain(raw));
		}
		return time;
	}

	/** Writes the time, or, if there is none, the text that could not be parsed when it was read */
	private void setTime(ConfigurationSection out, UUID playerId, String key, Instant time)
	{
		if (time != null)
		{
			out.set(key, DATES.format(time));
			return;
		}
		Map<String, Object> raw = this.rawTimes.get(playerId);
		if (raw != null && raw.get(out.getName() + "." + key) != null)
		{
			out.set(key, raw.get(out.getName() + "." + key));
		}
	}

	private static UUID parseId(String value)
	{
		try
		{
			return UUID.fromString(value);
		}
		catch (IllegalArgumentException ex)
		{
			return null;
		}
	}

	@Override
	protected void write(YamlConfiguration yaml)
	{
		for (Marriage marriage : this.marriages.values())
		{
			ConfigurationSection section = yaml.createSection(marriage.playerId().toString());

			Marriage.Proposal proposal = marriage.proposal();
			if (proposal != null)
			{
				ConfigurationSection out = section.createSection(Marriage.PROPOSAL);
				setTime(out, marriage.playerId(), TIME, proposal.time());
				out.set(PARTNER, idString(proposal.partner()));
			}
			Marriage.Engagement engagement = marriage.engagement();
			if (engagement != null)
			{
				ConfigurationSection out = section.createSection(Marriage.ENGAGEMENT);
				setTime(out, marriage.playerId(), TIME, engagement.since());
				out.set(PARTNER, idString(engagement.partner()));
				if (engagement.tokenPickedUp())
				{
					out.set(TOKEN, true);
				}
			}
			Marriage.Wedlock wedlock = marriage.wedlock();
			if (wedlock != null)
			{
				ConfigurationSection out = section.createSection(Marriage.WEDLOCK);
				setTime(out, marriage.playerId(), TIME, wedlock.since());
				out.set(PARTNER, idString(wedlock.partner()));
				setTime(out, marriage.playerId(), LAST_LOVE, wedlock.lastLove());
			}
			writeExtras(section, marriage.extras());
		}
	}

	private static String idString(UUID id)
	{
		return id != null ? id.toString() : null;
	}
}
