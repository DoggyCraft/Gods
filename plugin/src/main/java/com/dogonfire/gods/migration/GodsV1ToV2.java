package com.dogonfire.gods.migration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Upgrades gods.yml to version 2, the first with a {@code data-version}. The keys stay the same; what changes is how
 * priests are kept and what is no longer stored:
 * <ul>
 * <li>Gods 0.9.x kept one priest by name under {@code PriestName}; it is added to {@code Priests}.</li>
 * <li>{@code Priests} held player names; each becomes the player's UUID, looked up among the players the server has
 * seen (case-insensitive). A name the server doesn't know is moved to {@code UnresolvedPriests} (logged), so it is
 * not lost; the placeholder {@code none} and duplicates are dropped.</li>
 * <li>{@code LanguageFileName} is dropped: a god's language file is chosen from config.yml's language and the god's
 * type and gender.</li>
 * </ul>
 * Before data versions, Gods did this on every load; files from this branch before data versions only get the version.
 */
public final class GodsV1ToV2 implements Migration
{
	public static final String				PRIESTS				= "Priests";
	public static final String				LEGACY_PRIEST_NAME	= "PriestName";
	public static final String				LANGUAGE_FILE_NAME	= "LanguageFileName";
	/** Priests kept by a name the server didn't know; not priests, but kept so they can be fixed by hand */
	public static final String				UNRESOLVED_PRIESTS	= "UnresolvedPriests";

	private final Function<String, UUID>	playerIdByName;
	private final Logger					log;

	/** @param playerIdByName the UUID of a player the server knows, by name (any case), or null */
	public GodsV1ToV2(Function<String, UUID> playerIdByName, Logger log)
	{
		this.playerIdByName = playerIdByName;
		this.log = log;
	}

	@Override
	public int fromVersion()
	{
		return 1;
	}

	@Override
	public YamlConfiguration migrate(YamlConfiguration old)
	{
		for (String godName : old.getKeys(false))
		{
			ConfigurationSection god = old.getConfigurationSection(godName);
			if (god == null)
			{
				continue;
			}

			List<String> priests = god.getStringList(PRIESTS);
			boolean priestsChanged = false;

			String priestName = god.getString(LEGACY_PRIEST_NAME);
			if (priestName != null)
			{
				if (!priests.contains(priestName))
				{
					priests.add(priestName);
				}
				god.set(LEGACY_PRIEST_NAME, null);
				priestsChanged = true;
			}

			List<String> priestIds = new ArrayList<String>();
			List<String> unresolved = new ArrayList<String>(god.getStringList(UNRESOLVED_PRIESTS));
			for (String priest : priests)
			{
				UUID priestId = parseUUID(priest);
				if (priestId == null && priest != null && !priest.equals("none"))
				{
					priestId = this.playerIdByName.apply(priest);
					if (priestId == null)
					{
						this.log.warning("Kept the unknown priest '" + priest + "' of " + godName + " under " + UNRESOLVED_PRIESTS + ": the server doesn't know a player of that name");
						if (!unresolved.contains(priest))
						{
							unresolved.add(priest);
						}
					}
				}
				if (priestId == null || priestIds.contains(priestId.toString()))
				{
					priestsChanged = true;
					continue;
				}
				if (!priestId.toString().equals(priest))
				{
					priestsChanged = true;
				}
				priestIds.add(priestId.toString());
			}
			if (priestsChanged)
			{
				god.set(PRIESTS, priestIds);
			}
			if (!unresolved.isEmpty())
			{
				god.set(UNRESOLVED_PRIESTS, unresolved);
			}

			god.set(LANGUAGE_FILE_NAME, null);
		}
		return old;
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
}
