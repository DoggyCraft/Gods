package com.dogonfire.gods.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;

import com.dogonfire.gods.model.DevotionLevel;
import com.dogonfire.gods.model.GodMood;
import com.dogonfire.gods.model.GodRank;
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
		Chat chat,
		Effects effects,
		AltarTexts altarTexts,
		Npc npc,
		Ranks ranks,
		Devotion devotion)
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

	/**
	 * Believers ({@code believers}). A believer who stops praying loses prayer power and leaves their religion when the
	 * decay has brought it to 0 (see {@code BelieverRules.decay}).
	 *
	 * @param decayAfterDays     days without a prayer before a follower's prayer power starts to decay
	 * @param decayMultipliers   how many times {@code decayAfterDays} the quiet period is for each devotion level
	 * @param decayPerDay        prayer power lost on each day of decay
	 * @param decayPercentPerDay percent of their prayer power (rounded up) lost on each day of decay, before
	 *                           {@code decayPerDay}
	 */
	public record Believers(boolean leaveReligionOnDeath, int minSecondsBetweenChangingGod, int decayAfterDays, Map<DevotionLevel, Double> decayMultipliers, int decayPerDay,
			int decayPercentPerDay)
	{
		/** How many times decay-after-days a believer of this level waits before their prayer power decays (1 if unset) */
		public double multiplier(DevotionLevel level)
		{
			return this.decayMultipliers.getOrDefault(level != null ? level : DevotionLevel.FOLLOWER, 1.0D);
		}

		/** Whether prayer power decays at all (else believers never leave for not praying) */
		public boolean decays()
		{
			return this.decayPerDay > 0 || this.decayPercentPerDay > 0;
		}
	}

	/** @param assignCommand console command run when a player becomes a priest ($PlayerName is replaced), or "" */
	public record Priests(int minBelievers, int maxPerGod, int maxHoursWithoutPrayer, int genderChangeCooldownHours, String assignCommand, String removeCommand)
	{
	}

	/**
	 * The blocks altars are built on, per god type, and the other way around.
	 *
	 * @param blockTypes      the blocks that make each god type (a god type without blocks may be missing)
	 * @param godTypesByBlock the god types each block makes
	 * @param requireClaim    new altars only inside a GriefPrevention claim the builder may build in (only while
	 *                        GriefPrevention is installed)
	 */
	public record Altars(Map<GodType, List<Material>> blockTypes, Map<Material, List<GodType>> godTypesByBlock, boolean requireClaim)
	{
		/** These altar blocks, with new altars only in claims (the default) */
		public static Altars of(Map<GodType, List<Material>> blockTypes)
		{
			return of(blockTypes, true);
		}

		public static Altars of(Map<GodType, List<Material>> blockTypes, boolean requireClaim)
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
			return new Altars(Collections.unmodifiableMap(blockTypes), Collections.unmodifiableMap(frozen), requireClaim);
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

	/**
	 * @param protection whether holy and neutral land protect themselves ({@code holy-land.protection}); see
	 *                   {@code HolyLandRules.protects}
	 */
	public record HolyLand(boolean enabled, boolean defaultPvp, boolean defaultMobDamage, boolean lightning, boolean allowInteractionInNeutralLands, int deleteAbandonedAfterDays, Set<Material> breakableBlocks,
			HolyLandProtection protection)
	{
	}

	/** Who protects holy and neutral land ({@code holy-land.protection}) */
	public enum HolyLandProtection
	{
		/** Gods, unless GriefPrevention or WorldGuard is on the server: then their claims and regions protect it */
		AUTO,
		/** Gods, always */
		ON,
		/** Nobody: the land is still claimed and shown, but other plugins protect it (or nothing does) */
		OFF;

		/** {@code auto}, {@code on} or {@code off} (any case; YAML's true and false for an unquoted on and off), or null */
		public static HolyLandProtection parse(Object value)
		{
			if (value instanceof Boolean on)
			{
				return on ? ON : OFF;
			}
			if (value instanceof String text)
			{
				return switch (text.trim().toLowerCase(java.util.Locale.ROOT))
				{
					case "auto" -> AUTO;
					case "on", "true" -> ON;
					case "off", "false" -> OFF;
					default -> null;
				};
			}
			return null;
		}
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

	/**
	 * Short themed effects (particles, sounds and display entities) for nearby players only.
	 *
	 * @param enabled            all effects; off means none at all
	 * @param viewDistanceBlocks only players this close see and hear them
	 */
	public record Effects(boolean enabled, boolean prayer, boolean sacrifice, boolean blessings, int viewDistanceBlocks)
	{
		/** Effects when a believer prays at an altar */
		public boolean prayerOn()
		{
			return this.enabled && this.prayer;
		}

		/** Effects for sacrifices */
		public boolean sacrificeOn()
		{
			return this.enabled && this.sacrifice;
		}

		/** Effects on blessed and cursed players */
		public boolean blessingsOn()
		{
			return this.enabled && this.blessings;
		}
	}

	/**
	 * The floating text above altars (in the altars section of config.yml).
	 *
	 * @param floatingText       which praying altars show their god's name and title
	 * @param viewDistanceBlocks how far players see it
	 */
	public record AltarTexts(FloatingText floatingText, int viewDistanceBlocks)
	{
		/** Whether any altar shows floating text */
		public boolean enabled()
		{
			return this.floatingText != FloatingText.OFF;
		}
	}

	/** {@code altars.floating-text}: all praying altars, only gods' main altars, or none */
	public enum FloatingText
	{
		ALL, MAIN, OFF
	}

	/**
	 * The god standing as an NPC at its main altar (the npc section of config.yml). It appears once the god has the
	 * {@link GodRank#DESCENDED} rank (see {@link Ranks}).
	 *
	 * @param enabled  whether gods appear at their main altars at all
	 * @param provider who makes the NPC; {@code auto} picks one (see {@code integration/npc/NpcProviders})
	 */
	public record Npc(boolean enabled, NpcProvider provider)
	{
	}

	/**
	 * The ranks a religion grows through (the ranks section of config.yml; see {@code service/RankRules}).
	 *
	 * @param conditions     what a god needs for each rank above {@link GodRank#SPIRIT} (every rank has an entry; one
	 *                       without conditions is {@link RankConditions#NONE})
	 * @param dropGraceHours how long a god may have fewer believers than its rank needs before it falls
	 * @param unlocks        the lowest rank each feature needs
	 * @param aura           the effect believers get in their god's holy land, from {@link Unlocks#aura()} on
	 */
	public record Ranks(Map<GodRank, RankConditions> conditions, int dropGraceHours, Unlocks unlocks, Aura aura)
	{
		/** What a god needs for this rank; none for {@link GodRank#SPIRIT} */
		public RankConditions of(GodRank rank)
		{
			return this.conditions.getOrDefault(rank, RankConditions.NONE);
		}
	}

	/**
	 * What a god needs to rise to one rank ({@code ranks.<rank>}); 0, false or null means no such condition. Each holds
	 * for this rank only: config.yml is expected to ask more of higher ranks.
	 *
	 * @param minBelievers        believers (a structural condition: below it for the grace period, the god falls)
	 * @param minPower            power (its believers' prayers)
	 * @param requiresPriest      a priest (structural: without one the god falls at once)
	 * @param minMood             at least this mood, or null
	 * @param requiresMainAltar   a main altar
	 * @param hoursAfterPriest    it has had a priest this long ({@code PriestSince})
	 * @param hoursAtPreviousRank it has had the rank below this long ({@code RankSince})
	 * @param minPrayers          prayers at its altars in its whole life
	 * @param minSacrifices       sacrifices it was pleased with (ignored while sacrifices are off)
	 * @param minQuests           completed quests (ignored while quests are off)
	 * @param minAltars           praying altars standing (altars.yml)
	 * @param minHolyLands        holy lands, one per altar that claimed land (ignored while holy land is off; {@code min-holy-land-regions})
	 * @param minConverts         players who joined it through an invitation
	 */
	public record RankConditions(int minBelievers, int minPower, boolean requiresPriest, GodMood minMood, boolean requiresMainAltar, int hoursAfterPriest,
			int hoursAtPreviousRank, int minPrayers, int minSacrifices, int minQuests, int minAltars, int minHolyLands, int minConverts)
	{
		public static final RankConditions NONE = new RankConditions(0, 0, false, null, false, 0, 0, 0, 0, 0, 0, 0, 0);
	}

	/**
	 * The lowest rank each feature needs ({@code ranks.unlocks}).
	 *
	 * @param bibles               the religion's holy book (/g bible, /g editbible, /g setbible, a copy on joining)
	 * @param quests               quests (given by the god or prayed for)
	 * @param holyArtifacts        holy artifacts (given by the god, /g holyartifact)
	 * @param relations            alliances and wars (/g ally, /g war)
	 * @param extraPriest          one more priest than priests.max-per-god
	 * @param aura                 the holy land aura ({@link Aura})
	 * @param holyLandRadiusChunks how many chunks around its altar's chunk each altar's holy land reaches, per rank (0: the
	 *                             altar's own chunks, 1: 3x3 chunks, ...; every rank has an entry)
	 */
	public record Unlocks(GodRank bibles, GodRank quests, GodRank holyArtifacts, GodRank relations, GodRank extraPriest, GodRank aura,
			Map<GodRank, Integer> holyLandRadiusChunks)
	{
		/** How many chunks around its altar's chunk each altar's holy land reaches for a god of this rank */
		public int holyLandRadiusChunks(GodRank rank)
		{
			return this.holyLandRadiusChunks.getOrDefault(rank, 0);
		}
	}

	/**
	 * Believers in their god's holy land get a gentle effect, refreshed on the god's turn ({@code ranks.aura}).
	 *
	 * @param enabled   whether there is an aura at all
	 * @param effect    the potion effect's id (e.g. {@code regeneration}), looked up on the server
	 * @param amplifier 0 for level I, 1 for level II
	 * @param seconds   how long it lasts each time
	 */
	public record Aura(boolean enabled, String effect, int amplifier, int seconds)
	{
	}

	/**
	 * Believers' devotion to their god (the devotion section of config.yml; see {@code service/DevotionRules}).
	 *
	 * @param enabled                    whether believers earn devotion and levels unlock anything; off, nothing needs a
	 *                                   level
	 * @param points                     what each deed earns
	 * @param levels                     the points each level needs (every level has an entry, 0 for
	 *                                   {@link DevotionLevel#FOLLOWER}, never less than the level below)
	 * @param commandmentCooldownMinutes eating the holy food and slaying the unholy mob earn points at most this often
	 * @param fadeAfterDays              devotion starts fading after this many days without a prayer
	 * @param fadePercentPerDay          then it loses this percent of its points per day; 0: it never fades
	 * @param keepLevelPercent           a level is kept until the points fall below this percent of what it needs
	 * @param keptOnChangingGodPercent   the percent of their points a believer keeps when they leave or change god
	 * @param unlocks                    the lowest level each unlock needs
	 * @param prayForDiscountPercent     how much less /g prayfor costs from {@link DevotionUnlocks#prayForDiscount()} on
	 * @param blessingChanceMultiplier   how many times likelier the god's blessings are from
	 *                                   {@link DevotionUnlocks#moreBlessings()} on
	 * @param chatTitle                  the title ({@link DevotionUnlocks#title()}) shows in religion chat
	 */
	public record Devotion(boolean enabled, DevotionPoints points, Map<DevotionLevel, Integer> levels, int commandmentCooldownMinutes, int fadeAfterDays,
			int fadePercentPerDay, int keepLevelPercent, int keptOnChangingGodPercent, DevotionUnlocks unlocks, int prayForDiscountPercent,
			int blessingChanceMultiplier, boolean chatTitle)
	{
		/** The points a level needs (0 for a follower) */
		public int threshold(DevotionLevel level)
		{
			return this.levels.getOrDefault(level, 0);
		}
	}

	/**
	 * The devotion points each deed earns ({@code devotion.points})
	 *
	 * @param prayer    a prayer at an altar
	 * @param sacrifice a sacrifice the god wanted
	 * @param quest     taking part in a completed quest
	 * @param convert   inviting a player who joins
	 * @param holyFood  eating the god's holy food
	 * @param unholyMob slaying the god's unholy mob
	 */
	public record DevotionPoints(int prayer, int sacrifice, int quest, int convert, int holyFood, int unholyMob)
	{
	}

	/**
	 * The lowest devotion level each unlock needs ({@code devotion.unlocks})
	 *
	 * @param prayForDiscount /g prayfor costs less ({@link Devotion#prayForDiscountPercent()})
	 * @param holyArtifacts   the believer may be given a holy artifact (by the god, or /g holyartifact)
	 * @param moreBlessings   the god's blessings are likelier ({@link Devotion#blessingChanceMultiplier()})
	 * @param praise          the god praises the believer by name to its other believers
	 * @param title           the level's name is the believer's title (%gods_devotion_title%, religion chat)
	 * @param priesthood      the god prefers the believer as its priest, and asks them at once
	 */
	public record DevotionUnlocks(DevotionLevel prayForDiscount, DevotionLevel holyArtifacts, DevotionLevel moreBlessings, DevotionLevel praise, DevotionLevel title,
			DevotionLevel priesthood)
	{
	}

	/** {@code npc.provider}: who makes the god's NPC */
	public enum NpcProvider
	{
		/** Citizens if installed, else FancyNpcs if installed, else the built-in one where the server has mannequins */
		AUTO,
		/** The vanilla mannequin (Minecraft 1.21.9+) */
		BUILTIN,
		CITIZENS,
		FANCYNPCS,
		/** No NPC */
		OFF
	}
}
