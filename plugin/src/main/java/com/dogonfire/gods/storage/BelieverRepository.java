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
import com.dogonfire.gods.util.LegacyDates;

/**
 * believers.yml: one {@link Believer} per player UUID, with the keys Gods has always written ({@code God},
 * {@code Prayers}, {@code LastPrayer}, ... times as {@code "HH:mm:ss dd-MM-yyyy"}).
 * <pre>
 * data-version: 2
 * &lt;uuid&gt;: God, Prayers, LastPrayer, Joined, PrayerPower, ReligionChat, Hunting, Invitation.God/Time,
 *         LastPriestOffer, ChangingGod, LastBlessingTime, LastCursingTime, LastItemBlessingTime,
 *         LastHolyArtifactBlessingTime
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

	private static final Set<String>	KNOWN						= Set.of(GOD, PRAYERS, LAST_PRAYER, JOINED, PRAYER_POWER, RELIGION_CHAT, HUNTING, INVITATION,
			LAST_PRIEST_OFFER, CHANGING_GOD, LAST_BLESSING, LAST_CURSING, LAST_ITEM_BLESSING, LAST_ARTIFACT_BLESSING);

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
			believer.setGodName(section.getString(GOD));
			believer.setPrayers(section.getInt(PRAYERS));
			believer.setLastPrayer(DATES.parse(section.getString(LAST_PRAYER)));
			believer.setJoined(DATES.parse(section.getString(JOINED)));
			believer.setPrayerPower(section.getInt(PRAYER_POWER));
			believer.setReligionChat(section.getBoolean(RELIGION_CHAT));
			believer.setHunting(section.getBoolean(HUNTING));
			believer.setInvitation(section.getString(INVITATION_GOD), DATES.parse(section.getString(INVITATION_TIME)));
			believer.setLastPriestOffer(DATES.parse(section.getString(LAST_PRIEST_OFFER)));
			believer.setChangingGod(DATES.parse(section.getString(CHANGING_GOD)));
			believer.setLastBlessing(DATES.parse(section.getString(LAST_BLESSING)));
			believer.setLastCursing(DATES.parse(section.getString(LAST_CURSING)));
			believer.setLastItemBlessing(DATES.parse(section.getString(LAST_ITEM_BLESSING)));
			believer.setLastHolyArtifactBlessing(DATES.parse(section.getString(LAST_ARTIFACT_BLESSING)));
			believer.setExtras(extraKeys(section, KNOWN));
			this.believers.put(id, believer);
		}

		logger().info("Loaded " + this.believers.size() + " believers.");
	}

	private static UUID parseId(String key)
	{
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
			}
			setTime(section, LAST_PRIEST_OFFER, believer.lastPriestOffer());
			setTime(section, CHANGING_GOD, believer.changingGod());
			setTime(section, LAST_BLESSING, believer.lastBlessing());
			setTime(section, LAST_CURSING, believer.lastCursing());
			setTime(section, LAST_ITEM_BLESSING, believer.lastItemBlessing());
			setTime(section, LAST_ARTIFACT_BLESSING, believer.lastHolyArtifactBlessing());
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
