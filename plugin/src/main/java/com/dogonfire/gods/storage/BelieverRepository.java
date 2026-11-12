package com.dogonfire.gods.storage;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.dogonfire.gods.migration.BelieversV1ToV2;
import com.dogonfire.gods.migration.VersionedYaml;
import com.dogonfire.gods.model.Believer;
import com.dogonfire.gods.model.DevotionLevel;
import com.dogonfire.gods.util.LegacyDates;

/**
 * believers.yml: one {@link Believer} per player UUID, with the keys Gods has always written ({@code God},
 * {@code Prayers}, {@code LastPrayer}, ... times as {@code "HH:mm:ss dd-MM-yyyy"}).
 * <pre>
 * data-version: 2
 * &lt;uuid&gt;: God, Prayers, LastPrayer, Joined, PrayerPower, ReligionChat, Hunting, Invitation.God/Time,
 *         LastPriestOffer, ChangingGod, LastBlessingTime, LastCursingTime, LastItemBlessingTime,
 *         LastHolyArtifactBlessingTime, Invitation.By, Devotion, DevotionLevel, DevotionSince, DevotionFaded
 * Unresolved.&lt;name&gt;: entries of very old versions, keyed by a player name the upgrade couldn't turn into a UUID
 * </pre>
 * Version 1 (no {@code data-version}) is upgraded by {@link BelieversV1ToV2}, which moves entries keyed by player
 * name to the player's UUID. Other entries that are not a UUID are logged, ignored and written back unchanged.
 */
public final class BelieverRepository extends Repository
{
	public static final int				DATA_VERSION				= 2;
	public static final String			VERSION_KEY					= "data-version";

	private static final String			GOD							= "God";
	private static final String			PRAYERS						= "Prayers";
	private static final String			LAST_PRAYER					= "LastPrayer";
	private static final String			JOINED						= "Joined";
	private static final String			PRAYER_POWER				= "PrayerPower";
	private static final String			RELIGION_CHAT				= "ReligionChat";
	private static final String			HUNTING						= "Hunting";
	private static final String			INVITATION					= "Invitation";
	private static final String			INVITATION_GOD				= "Invitation.God";
	private static final String			INVITATION_TIME				= "Invitation.Time";
	private static final String			LAST_PRIEST_OFFER			= "LastPriestOffer";
	private static final String			CHANGING_GOD				= "ChangingGod";
	private static final String			LAST_BLESSING				= "LastBlessingTime";
	private static final String			LAST_CURSING				= "LastCursingTime";
	private static final String			LAST_ITEM_BLESSING			= "LastItemBlessingTime";
	private static final String			LAST_ARTIFACT_BLESSING		= "LastHolyArtifactBlessingTime";
	private static final String			INVITATION_BY				= "Invitation.By";
	private static final String			DEVOTION					= "Devotion";
	private static final String			DEVOTION_LEVEL				= "DevotionLevel";
	private static final String			DEVOTION_SINCE				= "DevotionSince";
	private static final String			DEVOTION_FADED				= "DevotionFaded";

	private static final Set<String>	KNOWN						= Set.of(GOD, PRAYERS, LAST_PRAYER, JOINED, PRAYER_POWER, RELIGION_CHAT, HUNTING, INVITATION,
			LAST_PRIEST_OFFER, CHANGING_GOD, LAST_BLESSING, LAST_CURSING, LAST_ITEM_BLESSING, LAST_ARTIFACT_BLESSING, DEVOTION, DEVOTION_LEVEL, DEVOTION_SINCE,
			DEVOTION_FADED);

	private static final LegacyDates	DATES						= LegacyDates.SECONDS;

	private final Map<UUID, Believer>	believers					= new LinkedHashMap<UUID, Believer>();

	/** The data version and migrations of believers.yml */
	static VersionedYaml format(Logger logger, Function<String, UUID> playerIdByName)
	{
		return new VersionedYaml(VERSION_KEY, DATA_VERSION, 1, List.of(new BelieversV1ToV2(playerIdByName, logger)));
	}

	BelieverRepository(DataFile file, Logger logger)
	{
		super(file, logger);
	}

	/** The believer's entry, or null if they have none */
	public Believer find(UUID id)
	{
		return this.believers.get(id);
	}

	/** The believer's entry, created (empty) if they have none */
	public Believer getOrCreate(UUID id)
	{
		Believer believer = this.believers.get(id);
		if (believer == null)
		{
			believer = new Believer(id);
			// A new believer starts with no devotion; only believers from before devotion lack it (and are seeded)
			believer.setDevotion(0);
			this.believers.put(id, believer);
		}
		return believer;
	}

	public void remove(UUID id)
	{
		this.believers.remove(id);
	}

	public Collection<Believer> all()
	{
		return Collections.unmodifiableCollection(this.believers.values());
	}

	@Override
	public int size()
	{
		return this.believers.size();
	}

	@Override
	protected void clear()
	{
		this.believers.clear();
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
			if (key.equals(BelieversV1ToV2.UNRESOLVED))
			{
				keepUnknown(key, yaml.get(key));
				continue;
			}
			UUID id = parseId(key);
			ConfigurationSection section = yaml.getConfigurationSection(key);
			if (id == null || section == null)
			{
				if (id == null)
				{
					logger().info("Ignoring believers.yml entry '" + key + "': it is not a player UUID");
				}
				keepUnknown(key, yaml.get(key));
				continue;
			}

			Believer believer = new Believer(id);
			// Times that can't be parsed are kept in the extras and written back as they were
			Map<String, Object> extras = extraKeys(section, KNOWN);
			believer.setGodName(section.getString(GOD));
			believer.setPrayers(section.getInt(PRAYERS));
			believer.setLastPrayer(readTime(section, LAST_PRAYER, DATES, extras));
			believer.setJoined(readTime(section, JOINED, DATES, extras));
			believer.setPrayerPower(section.getInt(PRAYER_POWER));
			believer.setReligionChat(section.getBoolean(RELIGION_CHAT));
			believer.setHunting(section.getBoolean(HUNTING));
			believer.setInvitation(section.getString(INVITATION_GOD), readTime(section, INVITATION_TIME, DATES, extras));
			believer.setLastPriestOffer(readTime(section, LAST_PRIEST_OFFER, DATES, extras));
			believer.setChangingGod(readTime(section, CHANGING_GOD, DATES, extras));
			believer.setLastBlessing(readTime(section, LAST_BLESSING, DATES, extras));
			believer.setLastCursing(readTime(section, LAST_CURSING, DATES, extras));
			believer.setLastItemBlessing(readTime(section, LAST_ITEM_BLESSING, DATES, extras));
			believer.setLastHolyArtifactBlessing(readTime(section, LAST_ARTIFACT_BLESSING, DATES, extras));
			believer.setInvitedBy(parseId(section.getString(INVITATION_BY)));
			readDevotion(section, believer, extras);
			believer.setExtras(extras);
			this.believers.put(id, believer);
		}

		logger().info("Loaded " + this.believers.size() + " believers.");
	}

	/**
	 * Devotion: a believer without {@code Devotion} is from before devotion (seeded once by the devotion service). A
	 * value that can't be read (not a number, an unknown level) is kept in the extras as it was.
	 */
	private void readDevotion(ConfigurationSection section, Believer believer, Map<String, Object> extras)
	{
		Object points = section.get(DEVOTION);
		if (points instanceof Number number)
		{
			believer.setDevotion(Math.max(0, number.intValue()));
		}
		else if (points != null)
		{
			extras.put(DEVOTION, toPlain(points));
			logger().info("Ignoring the devotion '" + points + "' of believer " + believer.id() + " in believers.yml: it is not a number");
		}
		String level = section.getString(DEVOTION_LEVEL);
		if (level != null)
		{
			believer.setDevotionLevel(DevotionLevel.byKey(level));
			if (believer.devotionLevel() == null)
			{
				extras.put(DEVOTION_LEVEL, level);
			}
		}
		believer.setDevotionSince(readTime(section, DEVOTION_SINCE, DATES, extras));
		believer.setDevotionFaded(readTime(section, DEVOTION_FADED, DATES, extras));
	}

	private static void writeDevotion(ConfigurationSection section, Believer believer)
	{
		if (believer.devotion() != null)
		{
			section.set(DEVOTION, believer.devotion());
		}
		if (believer.devotionLevel() != null && believer.devotionLevel() != DevotionLevel.FOLLOWER)
		{
			section.set(DEVOTION_LEVEL, believer.devotionLevel().key());
		}
		setTime(section, DEVOTION_SINCE, believer.devotionSince());
		setTime(section, DEVOTION_FADED, believer.devotionFaded());
	}

	private static UUID parseId(String key)
	{
		if (key == null)
		{
			return null;
		}
		try
		{
			return UUID.fromString(key);
		}
		catch (IllegalArgumentException ex)
		{
			return null;
		}
	}

	@Override
	protected void write(YamlConfiguration yaml)
	{
		for (Believer believer : this.believers.values())
		{
			ConfigurationSection section = yaml.createSection(believer.id().toString());
			section.set(GOD, believer.godName());
			if (believer.prayers() != 0)
			{
				section.set(PRAYERS, believer.prayers());
			}
			setTime(section, LAST_PRAYER, believer.lastPrayer());
			setTime(section, JOINED, believer.joined());
			if (believer.prayerPower() != 0)
			{
				section.set(PRAYER_POWER, believer.prayerPower());
			}
			if (believer.religionChat())
			{
				section.set(RELIGION_CHAT, true);
			}
			if (believer.hunting())
			{
				section.set(HUNTING, true);
			}
			if (believer.invitationGod() != null || believer.invitationTime() != null)
			{
				setTime(section, INVITATION_TIME, believer.invitationTime());
				section.set(INVITATION_GOD, believer.invitationGod());
				if (believer.invitedBy() != null)
				{
					section.set(INVITATION_BY, believer.invitedBy().toString());
				}
			}
			setTime(section, LAST_PRIEST_OFFER, believer.lastPriestOffer());
			setTime(section, CHANGING_GOD, believer.changingGod());
			setTime(section, LAST_BLESSING, believer.lastBlessing());
			setTime(section, LAST_CURSING, believer.lastCursing());
			setTime(section, LAST_ITEM_BLESSING, believer.lastItemBlessing());
			setTime(section, LAST_ARTIFACT_BLESSING, believer.lastHolyArtifactBlessing());
			writeDevotion(section, believer);
			writeExtras(section, believer.extras());
		}
	}

	private static void setTime(ConfigurationSection section, String key, Instant time)
	{
		if (time != null)
		{
			section.set(key, DATES.format(time));
		}
	}
}
