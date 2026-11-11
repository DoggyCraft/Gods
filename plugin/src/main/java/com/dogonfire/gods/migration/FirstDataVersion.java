package com.dogonfire.gods.migration;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Upgrades a data file whose format did not change to version 2, its first {@code data-version}: version 1 is every
 * file from before data versions (Gods 0.9.x and this branch before them). The keys stay as they are; what the
 * upgrade gives is the {@code <file>.v1.bak} backup {@link VersionedYaml} makes before the new repositories first
 * write the file (and drop what they clean up, e.g. quests.yml's obsolete quests).
 */
public final class FirstDataVersion implements Migration
{
	@Override
	public int fromVersion()
	{
		return 1;
	}

	@Override
	public YamlConfiguration migrate(YamlConfiguration old)
	{
		return old;
	}
}
