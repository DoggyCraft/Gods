package com.dogonfire.gods.storage;

import java.time.Instant;
import java.util.ArrayList;
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

import com.dogonfire.gods.migration.GodsV1ToV2;
import com.dogonfire.gods.migration.VersionedYaml;
import com.dogonfire.gods.model.ContestedLand;
import com.dogonfire.gods.model.God;
import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.model.GodHome;
import com.dogonfire.gods.model.GodType;
import com.dogonfire.gods.util.LegacyDates;

/**
 * gods.yml: one {@link God} per top-level key (the god's name), with the keys Gods has always written (see the fields
 * of {@link God}; times as {@code "HH:mm:ss dd-MM-yyyy"}, players as UUID strings).
 * <pre>
 * data-version: 2
 * &lt;god name&gt;: Created, DivineForce, Gender, PrivateAccess, Description, Home.World/X/Y/Z, Mood, Seed, Verbosity,
 *              Priests, PendingPriest, PendingPriestTime, Allies, Enemies, ContestedLand.World/X/Y/Z/Hash,
 *              BlessedPlayer, BlessedTime, CursedPlayer, CursedTime, EatFoodType, NotEatFoodType, NotSlayMobType,
 *              SlayMobType, SacrificeValues.&lt;material&gt;, PvP, MobDamage, MobSpawning, LastQuestTime,
 *              GenderChangedTime
 * </pre>
 * Version 1 (no {@code data-version}: Gods 0.9.x, and this branch before data versions) is upgraded by
 * {@link GodsV1ToV2}. The {@code !!java.util.UUID} tags older versions wrote are repaired before parsing
 * ({@link Storage#UUID_TAGS}). Values that can't be read (an unknown god type, a home without a world) are kept as
 * they are, like keys this version doesn't know.
 */
public final class GodRepository extends Repository
{
	public static final int				DATA_VERSION			= 2;
	public static final String			VERSION_KEY				= "data-version";

	private static final String			CREATED					= "Created";
	private static final String			DIVINE_FORCE			= "DivineForce";
	private static final String			GENDER					= "Gender";
	private static final String			PRIVATE_ACCESS			= "PrivateAccess";
	private static final String			DESCRIPTION				= "Description";
	private static final String			HOME					= "Home";
	private static final String			MOOD					= "Mood";
	private static final String			SEED					= "Seed";
	private static final String			VERBOSITY				= "Verbosity";
	private static final String			PRIESTS					= GodsV1ToV2.PRIESTS;
	private static final String			PENDING_PRIEST			= "PendingPriest";
	private static final String			PENDING_PRIEST_TIME		= "PendingPriestTime";
	private static final String			ALLIES					= "Allies";
	private static final String			ENEMIES					= "Enemies";
	private static final String			CONTESTED_LAND			= "ContestedLand";
	private static final String			BLESSED_PLAYER			= "BlessedPlayer";
	private static final String			BLESSED_TIME			= "BlessedTime";
	private static final String			CURSED_PLAYER			= "CursedPlayer";
	private static final String			CURSED_TIME				= "CursedTime";
	private static final String			HOLY_FOOD				= "EatFoodType";
	private static final String			UNHOLY_FOOD				= "NotEatFoodType";
	private static final String			HOLY_MOB				= "NotSlayMobType";
	private static final String			UNHOLY_MOB				= "SlayMobType";
	private static final String			SACRIFICE_VALUES		= "SacrificeValues";
	private static final String			PVP						= "PvP";
	private static final String			MOB_DAMAGE				= "MobDamage";
	private static final String			MOB_SPAWNING			= "MobSpawning";
	private static final String			LAST_QUEST_TIME			= "LastQuestTime";
	private static final String			GENDER_CHANGED_TIME		= "GenderChangedTime";

	/** Kept by older versions next to ContestedLand; cleared with it ({@link #clearContestedLand}) */
	private static final List<String>	LEGACY_CONTESTED		= List.of("ContestedKills", "ContestedDesecration");

	private static final Set<String>	KNOWN					= Set.of(CREATED, DIVINE_FORCE, GENDER, PRIVATE_ACCESS, DESCRIPTION, HOME, MOOD, SEED, VERBOSITY,
			PRIESTS, PENDING_PRIEST, PENDING_PRIEST_TIME, ALLIES, ENEMIES, CONTESTED_LAND, BLESSED_PLAYER, BLESSED_TIME, CURSED_PLAYER, CURSED_TIME,
			HOLY_FOOD, UNHOLY_FOOD, HOLY_MOB, UNHOLY_MOB, SACRIFICE_VALUES, PVP, MOB_DAMAGE, MOB_SPAWNING, LAST_QUEST_TIME, GENDER_CHANGED_TIME);

	private static final LegacyDates	DATES					= LegacyDates.SECONDS;

	private final Map<String, God>		gods					= new LinkedHashMap<String, God>();

	/** The data version and migrations of gods.yml */
	static VersionedYaml format(Logger logger, Function<String, UUID> playerIdByName)
	{
		return new VersionedYaml(VERSION_KEY, DATA_VERSION, 1, List.of(new GodsV1ToV2(playerIdByName, logger)));
	}

	GodRepository(DataFile file, Logger logger)
	{
		super(file, logger);
	}

	/** The god with exactly this name, or null */
	public God find(String name)
	{
		return name != null ? this.gods.get(name) : null;
	}

	/** The god with exactly this name, created (empty) if there is none */
	public God getOrCreate(String name)
	{
		return this.gods.computeIfAbsent(name, God::new);
	}

	/**
	 * The god to change, created if there is none (setting a value in gods.yml always created the god), or null for a
	 * null name, so a lookup for a player without a god can't create a god called "null"
	 */
	public God forChange(String name)
	{
		return name != null ? getOrCreate(name) : null;
	}

	public void remove(String name)
	{
		this.gods.remove(name);
	}

	/** Every god's name, in file order; a copy, so gods can be removed while going through it */
	public List<String> names()
	{
		return new ArrayList<String>(this.gods.keySet());
	}

	public Collection<God> all()
	{
		return Collections.unmodifiableCollection(this.gods.values());
	}

	@Override
	public int size()
	{
		return this.gods.size();
	}

	/** Forgets the holy land the god attacks, and what older versions kept about it */
	public static void clearContestedLand(God god)
	{
		god.setContestedLand(null);
		god.extras().remove(CONTESTED_LAND);
		for (String key : LEGACY_CONTESTED)
		{
			god.extras().remove(key);
		}
	}

	@Override
	protected void clear()
	{
		this.gods.clear();
	}

	@Override
	protected void read(YamlConfiguration yaml)
	{
		for (String key : yaml.getKeys(false))
		{
			ConfigurationSection section = yaml.getConfigurationSection(key);
			if (key.equals(VERSION_KEY) || section == null)
			{
				if (!key.equals(VERSION_KEY))
				{
					logger().info("Ignoring gods.yml entry '" + key + "': it is not a god");
				}
				keepUnknown(key, yaml.get(key));
				continue;
			}
			this.gods.put(key, readGod(key, section));
		}

		logger().info("Loaded " + this.gods.size() + " gods.");
	}

	private God readGod(String name, ConfigurationSection section)
	{
		God god = new God(name);
		Map<String, Object> extras = extraKeys(section, KNOWN);

		String created = section.getString(CREATED);
		god.setCreated(created != null ? orEpoch(readTime(section, CREATED, DATES, extras)) : null);

		god.setType(parseEnum(GodType.class, section, DIVINE_FORCE, extras));
		god.setGender(parseEnum(GodGender.class, section, GENDER, extras));
		god.setPrivateAccess(section.getBoolean(PRIVATE_ACCESS));
		god.setDescription(section.getString(DESCRIPTION));

		if (section.contains(HOME + ".World"))
		{
			god.setHome(new GodHome(section.getString(HOME + ".World"), section.getDouble(HOME + ".X"), section.getDouble(HOME + ".Y"), section.getDouble(HOME + ".Z")));
		}
		else if (section.contains(HOME))
		{
			extras.put(HOME, toPlain(section.get(HOME)));
		}

		god.setMood((float) section.getDouble(MOOD));
		god.setSeed(section.getLong(SEED));
		// 0 is generated again; a negative one would break the random draws that divide by it
		god.setVerbosity(Math.max(0, section.getInt(VERBOSITY)));

		List<String> unresolved = new ArrayList<String>(section.getStringList(GodsV1ToV2.UNRESOLVED_PRIESTS));
		for (String priest : section.getStringList(PRIESTS))
		{
			UUID priestId = parseUUID(priest);
			if (priestId == null)
			{
				// Kept, not lost: a priest that isn't a UUID can't be one, but the admin can fix it
				logger().info("Ignoring priest '" + priest + "' of " + name + ": it is not a player UUID; it is kept under " + GodsV1ToV2.UNRESOLVED_PRIESTS);
				if (!unresolved.contains(priest))
				{
					unresolved.add(priest);
				}
				continue;
			}
			god.priests().add(priestId);
		}
		if (!unresolved.isEmpty())
		{
			extras.put(GodsV1ToV2.UNRESOLVED_PRIESTS, unresolved);
		}
		god.setPendingPriest(parseUUID(section.getString(PENDING_PRIEST)));
		god.setPendingPriestTime(readTime(section, PENDING_PRIEST_TIME, DATES, extras));

		god.allies().addAll(section.getStringList(ALLIES));
		god.enemies().addAll(section.getStringList(ENEMIES));

		if (section.contains(CONTESTED_LAND + ".World"))
		{
			god.setContestedLand(new ContestedLand(section.getString(CONTESTED_LAND + ".World"), section.getInt(CONTESTED_LAND + ".X"), section.getInt(CONTESTED_LAND + ".Y"),
					section.getInt(CONTESTED_LAND + ".Z"), section.getLong(CONTESTED_LAND + ".Hash")));
		}
		else if (section.contains(CONTESTED_LAND))
		{
			extras.put(CONTESTED_LAND, toPlain(section.get(CONTESTED_LAND)));
		}

		god.setBlessed(parseUUID(section.getString(BLESSED_PLAYER)), readTime(section, BLESSED_TIME, DATES, extras));
		god.setCursed(parseUUID(section.getString(CURSED_PLAYER)), readTime(section, CURSED_TIME, DATES, extras));

		god.setHolyFood(section.getString(HOLY_FOOD));
		god.setUnholyFood(section.getString(UNHOLY_FOOD));
		god.setHolyMob(section.getString(HOLY_MOB));
		god.setUnholyMob(section.getString(UNHOLY_MOB));

		ConfigurationSection sacrifices = section.getConfigurationSection(SACRIFICE_VALUES);
		if (sacrifices != null)
		{
			for (String item : sacrifices.getKeys(false))
			{
				god.sacrificeValues().put(item, (float) sacrifices.getDouble(item));
			}
		}

		god.setPvp(readBoolean(section, PVP, extras));
		god.setMobDamage(readBoolean(section, MOB_DAMAGE, extras));
		god.setMobSpawning(readBoolean(section, MOB_SPAWNING, extras));

		god.setLastQuestTime(readTime(section, LAST_QUEST_TIME, DATES, extras));
		god.setGenderChangedTime(readTime(section, GENDER_CHANGED_TIME, DATES, extras));

		god.setExtras(extras);
		return god;
	}

	/** A time that is there but can't be read still counts as set */
	private static Instant orEpoch(Instant time)
	{
		return time != null ? time : Instant.EPOCH;
	}

	/** The value, or null if it is missing; one that isn't a known name is kept in {@code extras} */
	private static <E extends Enum<E>> E parseEnum(Class<E> type, ConfigurationSection section, String key, Map<String, Object> extras)
	{
		String text = section.getString(key);
		if (text == null)
		{
			return null;
		}
		try
		{
			return Enum.valueOf(type, text);
		}
		catch (IllegalArgumentException ex)
		{
			extras.put(key, section.get(key));
			return null;
		}
	}

	/** True/false, or null if it is missing; a value that isn't a boolean is kept in {@code extras} */
	private static Boolean readBoolean(ConfigurationSection section, String key, Map<String, Object> extras)
	{
		if (!section.contains(key))
		{
			return null;
		}
		if (section.isBoolean(key))
		{
			return section.getBoolean(key);
		}
		extras.put(key, toPlain(section.get(key)));
		return null;
	}

	private static UUID parseUUID(String text)
	{
		if (text == null)
		{
			return null;
		}
		try
		{
			return UUID.fromString(text);
		}
		catch (IllegalArgumentException ex)
		{
			return null;
		}
	}

	@Override
	protected void write(YamlConfiguration yaml)
	{
		for (God god : this.gods.values())
		{
			ConfigurationSection section = yaml.createSection(god.name());
			if (!(Instant.EPOCH.equals(god.created()) && god.extras().containsKey(CREATED)))
			{
				// A Created that couldn't be parsed (EPOCH in memory) is written back as it was, from the extras
				setTime(section, CREATED, god.created());
			}
			if (god.type() != null)
			{
				section.set(DIVINE_FORCE, god.type().name());
			}
			if (god.gender() != null)
			{
				section.set(GENDER, god.gender().name());
			}
			section.set(PRIVATE_ACCESS, god.privateAccess());
			section.set(DESCRIPTION, god.description());
			if (god.home() != null)
			{
				section.set(HOME + ".X", god.home().x());
				section.set(HOME + ".Y", god.home().y());
				section.set(HOME + ".Z", god.home().z());
				section.set(HOME + ".World", god.home().world());
			}
			if (god.mood() != 0.0F)
			{
				section.set(MOOD, god.mood());
			}
			if (god.seed() != 0L)
			{
				section.set(SEED, god.seed());
			}
			if (god.verbosity() != 0)
			{
				section.set(VERBOSITY, god.verbosity());
			}
			if (!god.priests().isEmpty())
			{
				section.set(PRIESTS, god.priests().stream().map(UUID::toString).toList());
			}
			if (god.pendingPriest() != null)
			{
				section.set(PENDING_PRIEST, god.pendingPriest().toString());
			}
			setTime(section, PENDING_PRIEST_TIME, god.pendingPriestTime());
			if (!god.allies().isEmpty())
			{
				section.set(ALLIES, new ArrayList<String>(god.allies()));
			}
			if (!god.enemies().isEmpty())
			{
				section.set(ENEMIES, new ArrayList<String>(god.enemies()));
			}
			ContestedLand contested = god.contestedLand();
			if (contested != null)
			{
				section.set(CONTESTED_LAND + ".Hash", contested.hash());
				section.set(CONTESTED_LAND + ".X", contested.x());
				section.set(CONTESTED_LAND + ".Y", contested.y());
				section.set(CONTESTED_LAND + ".Z", contested.z());
				section.set(CONTESTED_LAND + ".World", contested.world());
			}
			if (god.blessedPlayer() != null)
			{
				section.set(BLESSED_PLAYER, god.blessedPlayer().toString());
			}
			setTime(section, BLESSED_TIME, god.blessedTime());
			if (god.cursedPlayer() != null)
			{
				section.set(CURSED_PLAYER, god.cursedPlayer().toString());
			}
			setTime(section, CURSED_TIME, god.cursedTime());
			section.set(HOLY_FOOD, god.holyFood());
			section.set(UNHOLY_FOOD, god.unholyFood());
			section.set(HOLY_MOB, god.holyMob());
			section.set(UNHOLY_MOB, god.unholyMob());
			for (Map.Entry<String, Float> sacrifice : god.sacrificeValues().entrySet())
			{
				section.set(SACRIFICE_VALUES + "." + sacrifice.getKey(), sacrifice.getValue());
			}
			section.set(PVP, god.pvp());
			section.set(MOB_DAMAGE, god.mobDamage());
			section.set(MOB_SPAWNING, god.mobSpawning());
			setTime(section, LAST_QUEST_TIME, god.lastQuestTime());
			setTime(section, GENDER_CHANGED_TIME, god.genderChangedTime());
			writeExtras(section, god.extras());
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
