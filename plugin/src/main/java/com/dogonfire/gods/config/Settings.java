package com.dogonfire.gods.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;

import com.dogonfire.gods.model.GodType;

/**
 * Everything in config.yml, read and checked once per (re)load by {@link SettingsLoader}; never changed afterwards.
 * Each record matches a section of config.yml (see plugin/src/main/resources/config.yml for the keys and what they do).
 * Every collection in it is unmodifiable.
 */
public record Settings(
		General general,
		GodSettings gods,
		Believers believers,
		Priests priests,
		Altars altars,
		Prayers prayers,
		Sacrifices sacrifices,
		Commandments commandments,
		Blessings blessings,
		Curses curses,
		ItemBlessings itemBlessings,
		HolyArtifacts holyArtifacts,
		Quests quests,
		HolyLand holyLand,
		Marriage marriage,
		Bibles bibles,
		Chat chat)
{
	/** @param worlds the worlds Gods is active in; empty for every world (also worlds loaded later) */
	public record General(boolean debug, String language, String serverName, List<String> worlds)
	{
		/** True if Gods is active in the world with this name */
		public boolean isActiveIn(String world)
		{
			return this.worlds.isEmpty() || this.worlds.contains(world);
		}
	}

	/**
	 * @param whitelist only gods in whitelist.yml are allowed
	 * @param blacklist gods in blacklist.yml are not allowed; never true together with {@code whitelist}
	 */
	public record GodSettings(boolean broadcastNewGods, boolean privateByDefault, boolean titles, double verbosity, double moodFalloff, boolean whitelist, boolean blacklist)
	{
	}

	public record Believers(boolean leaveReligionOnDeath, int minSecondsBetweenChangingGod, int maxHoursWithoutPrayer)
	{
	}

	/** @param assignCommand console command run when a player becomes a priest ($PlayerName is replaced), or "" */
	public record Priests(int minBelievers, int maxPerGod, int maxHoursWithoutPrayer, boolean onlyPriestsCanSetHome, int genderChangeCooldownHours, String assignCommand,
			String removeCommand)
	{
	}

	/**
	 * The blocks altars are built on, per god type, and the other way around.
	 *
	 * @param blockTypes      the blocks that make each god type (a god type without blocks may be missing)
	 * @param godTypesByBlock the god types each block makes
	 */
	public record Altars(Map<GodType, List<Material>> blockTypes, Map<Material, List<GodType>> godTypesByBlock)
	{
		public static Altars of(Map<GodType, List<Material>> blockTypes)
		{
			Map<Material, List<GodType>> byBlock = new HashMap<>();
			for (Map.Entry<GodType, List<Material>> entry : blockTypes.entrySet())
			{
				for (Material material : entry.getValue())
				{
					List<GodType> godTypes = byBlock.computeIfAbsent(material, m -> new ArrayList<>());
					if (!godTypes.contains(entry.getKey()))
					{
						godTypes.add(entry.getKey());
					}
				}
			}
			Map<Material, List<GodType>> frozen = new HashMap<>();
			byBlock.forEach((material, godTypes) -> frozen.put(material, List.copyOf(godTypes)));
			return new Altars(Collections.unmodifiableMap(blockTypes), Collections.unmodifiableMap(frozen));
		}

		/** The blocks that make this god type; empty when none do. */
		public List<Material> blocksFor(GodType godType)
		{
			return this.blockTypes.getOrDefault(godType, List.of());
		}

		/** The god types an altar on this block makes; empty when it isn't an altar block. */
		public List<GodType> godTypesFor(Material block)
		{
			return this.godTypesByBlock.getOrDefault(block, List.of());
		}
	}

	/** @param itemCost the prayer power /g pray item costs, and so on */
	public record Prayers(boolean enabled, int minMinutesBetweenPrayers, int itemCost, int healthCost, int blessingCost, int questCost)
	{
	}

	public record Sacrifices(boolean enabled)
	{
	}

	public record Commandments(boolean enabled, boolean broadcastFoodEaten, boolean broadcastMobSlain)
	{
	}

	/** The booleans after {@code blessedSeconds} say which blessing effects can be given. */
	public record Blessings(boolean enabled, int minSecondsBetweenBlessings, int blessedSeconds, boolean speed, boolean regeneration, boolean heal, boolean fastDigging, boolean increaseDamage)
	{
	}

	public record Curses(boolean enabled, int minSecondsBetweenCurses, int cursedMinutes, boolean lightning, boolean mobs)
	{
	}

	public record ItemBlessings(boolean enabled, int minMinutesBetweenBlessings, int minGodPower, int godPowerForLevel1Items, int godPowerForLevel2Items, int godPowerForLevel3Items)
	{
	}

	public record HolyArtifacts(boolean enabled, int minMinutesBetweenBlessings)
	{
	}

	/** @param rewardValues what each reward item is worth; never empty */
	public record Quests(boolean enabled, int minMinutesBetweenQuests, int requiredBelievers, boolean slay, boolean sacrifice, boolean pilgrimage, boolean giveItems, Map<Material, Integer> rewardValues)
	{
	}

	public record HolyLand(boolean enabled, boolean defaultPvp, boolean defaultMobDamage, boolean lightning, boolean allowInteractionInNeutralLands, int deleteAbandonedAfterDays, Set<Material> breakableBlocks)
	{
	}

	public record Marriage(boolean enabled, boolean weddingFireworks)
	{
	}

	public record Bibles(boolean enabled, boolean giveOnJoin)
	{
	}

	public record Chat(boolean formatting)
	{
	}
}
