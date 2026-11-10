package com.dogonfire.gods.migration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Upgrades the config.yml written by Gods 0.9.x and by Gods before config versions (no version key, CamelCase
 * sections like Settings.Debug) to version 2. The values are copied into the bundled config.yml so the result keeps its
 * comments; settings that no longer do anything are left out.
 */
public final class ConfigV1ToV2 implements Migration
{
	/** Old key to new key, for values copied as-is. */
	static final Map<String, String> MOVED = Map.ofEntries(
			Map.entry("Settings.Debug", "general.debug"),
			Map.entry("Settings.Language", "general.language"),
			Map.entry("Settings.ServerName", "general.server-name"),
			Map.entry("Settings.Worlds", "general.worlds"),

			Map.entry("Settings.BroadcastNewGods", "gods.broadcast-new-gods"),
			Map.entry("Settings.DefaultPrivateReligions", "gods.private-by-default"),
			Map.entry("Settings.UseGodTitles", "gods.titles"),
			Map.entry("Settings.GodVerbosity", "gods.verbosity"),
			Map.entry("Settings.MoodFalloff", "gods.mood-falloff"),
			Map.entry("Settings.UseWhitelist", "gods.whitelist"),
			Map.entry("Settings.UseBlacklist", "gods.blacklist"),

			Map.entry("Settings.LeaveReligionOnDeath", "believers.leave-religion-on-death"),
			Map.entry("Settings.MinSecondsBetweenChangingGod", "believers.min-seconds-between-changing-god"),
			Map.entry("Settings.MaxBelieverPrayerTime", "believers.max-hours-without-prayer"),

			Map.entry("Settings.MinBelieversForPriest", "priests.min-believers"),
			Map.entry("Settings.MaxPriestsPrGod", "priests.max-per-god"),
			Map.entry("Settings.MaxPriestPrayerTime", "priests.max-hours-without-prayer"),
			Map.entry("Settings.OnlyPriestCanSetHome", "priests.only-priests-can-set-home"),
			Map.entry("Settings.PriestAssignCommand", "priests.assign-command"),
			Map.entry("Settings.PriestRemoveCommand", "priests.remove-command"),

			Map.entry("Prayers.Enabled", "prayers.enabled"),
			Map.entry("Settings.MinBelieverPrayerTime", "prayers.min-minutes-between-prayers"),
			Map.entry("Prayers.PrayerPowerForItem", "prayers.power-cost.item"),
			Map.entry("Prayers.PrayerPowerForHealth", "prayers.power-cost.health"),
			Map.entry("Prayers.PrayerPowerForBlessing", "prayers.power-cost.blessing"),
			Map.entry("Prayers.PrayerPowerForQuest", "prayers.power-cost.quest"),

			Map.entry("Sacrifices.Enabled", "sacrifices.enabled"),

			Map.entry("Commandments.Enabled", "commandments.enabled"),
			Map.entry("Commandments.BroadcastFoodEaten", "commandments.broadcast-food-eaten"),
			Map.entry("Commandments.BroadcastMobSlain", "commandments.broadcast-mob-slain"),

			Map.entry("Blessing.Enabled", "blessings.enabled"),
			Map.entry("Blessing.MinBlessingTime", "blessings.min-seconds-between-blessings"),
			Map.entry("Blessing.MaxBlessingTime", "blessings.blessed-seconds"),
			Map.entry("Blessing.Speed", "blessings.effects.speed"),
			Map.entry("Blessing.Regeneration", "blessings.effects.regeneration"),
			Map.entry("Blessing.Heal", "blessings.effects.heal"),
			Map.entry("Blessing.FastDigging", "blessings.effects.fast-digging"),
			Map.entry("Blessing.IncreaseDamage", "blessings.effects.increase-damage"),

			Map.entry("Cursing.Enabled", "curses.enabled"),
			Map.entry("Cursing.MinCursingTime", "curses.min-seconds-between-curses"),
			Map.entry("Cursing.MaxCursingTime", "curses.cursed-minutes"),
			Map.entry("Cursing.LightningCurse", "curses.lightning"),
			Map.entry("Cursing.MobCurse", "curses.mobs"),

			Map.entry("ItemBlessing.Enabled", "item-blessings.enabled"),
			Map.entry("ItemBlessing.MinItemBlessingTime", "item-blessings.min-minutes-between-blessings"),
			Map.entry("ItemBlessing.GodPowerForLevel1Items", "item-blessings.god-power-for-level-1-items"),
			Map.entry("ItemBlessing.GodPowerForLevel2Items", "item-blessings.god-power-for-level-2-items"),
			Map.entry("ItemBlessing.GodPowerForLevel3Items", "item-blessings.god-power-for-level-3-items"),

			Map.entry("HolyArtifacts.Enabled", "holy-artifacts.enabled"),
			Map.entry("HolyArtifacts.MinHolyArtifactBlessingTime", "holy-artifacts.min-minutes-between-blessings"),

			Map.entry("Quests.Enabled", "quests.enabled"),
			Map.entry("Quests.MinMinutesBetweenQuests", "quests.min-minutes-between-quests"),
			Map.entry("Quests.RequiredBelieversForQuests", "quests.required-believers"),
			Map.entry("Quests.SlayQuests", "quests.types.slay"),
			Map.entry("Quests.SacrificeQuests", "quests.types.sacrifice"),
			Map.entry("Quests.PilgrimageQuests", "quests.types.pilgrimage"),
			Map.entry("Quests.GiveItemsQuests", "quests.types.give-items"),

			Map.entry("HolyLand.Enabled", "holy-land.enabled"),
			Map.entry("HolyLand.DefaultPvP", "holy-land.default-pvp"),
			Map.entry("HolyLand.DefaultMobDamage", "holy-land.default-mob-damage"),
			Map.entry("HolyLand.Lightning", "holy-land.lightning"),
			Map.entry("HolyLand.AllowInteractionInNeutralLands", "holy-land.allow-interaction-in-neutral-lands"),
			Map.entry("HolyLand.DeleteAbandonedHolyLandsAfterDays", "holy-land.delete-abandoned-after-days"),

			Map.entry("Marriage.Enabled", "marriage.enabled"),
			Map.entry("Marriage.WeddingFireworks", "marriage.wedding-fireworks"),

			Map.entry("Bibles.Enabled", "bibles.enabled"),
			Map.entry("Bibles.GiveBibleWhenJoinReligion", "bibles.give-on-join"),

			Map.entry("ChatFormatting.Enabled", "chat.formatting"));

	/** The minimum god power for item blessings. Gods 0.9.x saved it under the second name but read the first. */
	static final String	MIN_GOD_POWER		= "ItemBlessing.MinGodPowerForItemBlessings";
	static final String	OLD_MIN_GOD_POWER	= "ItemBlessing.MinGodPowerItemBlessings";

	static final String	ALTAR_BLOCK_TYPES	= "Altars.BlockTypes";
	static final String	REWARD_VALUES		= "Quests.RewardValues";
	static final String	BREAKABLE_BLOCKS	= "HolyLand.BreakableBlockTypes";

	/** Settings older versions wrote that do nothing any more. They are left out without a mention. */
	static final Set<String> OBSOLETE = Set.of(
			"Settings.DownloadLanguageFile",
			"Settings.NumberOfBelieversPrPriest",
			"Settings.AllowMultipleGodsPrDivinePower",
			"Quests.ConvertQuests",
			"Quests.HolywarQuests",
			"Quests.GlobalQuestsPercentChance",
			"Prophecies.Enabled",
			"HolyLand.RadiusPrPower");

	private final Function<String, InputStream>	resources;
	private final Logger						log;

	/** @param resources opens a file bundled in the plugin jar, like {@code Plugin#getResource} */
	public ConfigV1ToV2(Function<String, InputStream> resources, Logger log)
	{
		this.resources = resources;
		this.log = log;
	}

	@Override
	public int fromVersion()
	{
		return 1;
	}

	@Override
	public YamlConfiguration migrate(YamlConfiguration old) throws MigrationException
	{
		YamlConfiguration config = bundled("config.yml");

		for (Map.Entry<String, String> moved : MOVED.entrySet())
		{
			if (old.isSet(moved.getKey()) && !old.isConfigurationSection(moved.getKey()))
			{
				config.set(moved.getValue(), old.get(moved.getKey()));
			}
		}

		if (old.isSet(MIN_GOD_POWER))
		{
			config.set("item-blessings.min-god-power", old.get(MIN_GOD_POWER));
		}
		else if (old.isSet(OLD_MIN_GOD_POWER))
		{
			config.set("item-blessings.min-god-power", old.get(OLD_MIN_GOD_POWER));
		}

		// Gods without these sections (or with empty ones) used the built-in defaults, which the bundled config has
		ConfigurationSection altars = old.getConfigurationSection(ALTAR_BLOCK_TYPES);
		if (altars != null && !altars.getKeys(false).isEmpty())
		{
			clearSection(config, "altars.block-types");
			for (String godType : altars.getKeys(false))
			{
				config.set("altars.block-types." + godType.toLowerCase(Locale.ROOT), lowerCase(altars.getStringList(godType)));
			}
		}

		ConfigurationSection rewards = old.getConfigurationSection(REWARD_VALUES);
		if (rewards != null && !rewards.getKeys(false).isEmpty())
		{
			clearSection(config, "quests.reward-values");
			for (String item : rewards.getKeys(false))
			{
				config.set("quests.reward-values." + item.toLowerCase(Locale.ROOT), rewards.get(item));
			}
		}

		List<String> breakable = old.getStringList(BREAKABLE_BLOCKS);
		if (!breakable.isEmpty())
		{
			config.set("holy-land.breakable-blocks", lowerCase(breakable));
		}

		logLeftOut(old);
		return config;
	}

	/** Mentions settings in the old file that were neither moved nor known to be obsolete, in case of a typo. */
	private void logLeftOut(YamlConfiguration old)
	{
		List<String> leftOut = new ArrayList<>();
		for (String key : old.getKeys(true))
		{
			if (old.isConfigurationSection(key) || MOVED.containsKey(key) || OBSOLETE.contains(key) || key.equals(MIN_GOD_POWER) || key.equals(OLD_MIN_GOD_POWER) || key.equals(BREAKABLE_BLOCKS) || key.startsWith(ALTAR_BLOCK_TYPES + ".") || key.startsWith(REWARD_VALUES + "."))
			{
				continue;
			}
			leftOut.add(key);
		}
		if (!leftOut.isEmpty())
		{
			this.log.warning("These settings in the old config.yml are unknown to Gods and were left out: " + String.join(", ", leftOut));
		}
	}

	/** Empties a section of the bundled config but keeps the section itself, so its comment stays. */
	private static void clearSection(YamlConfiguration config, String path)
	{
		ConfigurationSection section = config.getConfigurationSection(path);
		if (section != null)
		{
			for (String key : section.getKeys(false))
			{
				section.set(key, null);
			}
		}
	}

	private static List<String> lowerCase(List<String> names)
	{
		return names.stream().map(name -> name.trim().toLowerCase(Locale.ROOT)).toList();
	}

	private YamlConfiguration bundled(String name) throws MigrationException
	{
		try (InputStream in = this.resources.apply(name))
		{
			if (in == null)
			{
				throw new MigrationException(name + " is missing from the plugin jar");
			}
			return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
		}
		catch (IOException e)
		{
			throw new MigrationException("could not read the bundled " + name + ": " + e.getMessage());
		}
	}
}
