package com.dogonfire.gods.config;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.EntityType;

import com.dogonfire.gods.model.GodRank;
import com.dogonfire.gods.model.NpcSkin;

/**
 * What one god type is about (a section of themes.yml, merged by {@link ThemesLoader}): the mobs and foods its gods pick
 * their commandments from, what they may want sacrificed, what their slay quests ask for, the blocks their altars are
 * built on and how their altar signs look. Only names this server has are in it. Every list is unmodifiable.
 *
 * @param holyMobs       a god forbids killing one of these (picked once and kept)
 * @param unholyMobs     a god wants one of these slain (picked once and kept)
 * @param holyFoods      a god wants one of these eaten (picked once and kept)
 * @param unholyFoods    a god forbids eating one of these (picked once and kept)
 * @param sacrificeNeeds what a god may come to want sacrificed
 * @param slayTargets    what slay quests ask for, besides the god's unholy mob
 * @param altarBlocks    the blocks an altar to this god type is built on (config.yml's altars.block-types may replace
 *                       them, see {@link Themes#altars()})
 * @param sign           how altar signs of these gods look
 * @param effects        how the effects of these gods look and sound (prayers, sacrifices, blessings, curses)
 * @param npcSkins       the skins of these gods' NPCs at their main altars, per gender (a god's own skin, set with
 *                       /g godskin, comes first; see {@code GodNpcRules.skin} for the whole order)
 * @param rankNames      what these gods' ranks are called
 */
public record Theme(
		List<EntityType> holyMobs,
		List<EntityType> unholyMobs,
		List<Material> holyFoods,
		List<Material> unholyFoods,
		List<Material> sacrificeNeeds,
		List<EntityType> slayTargets,
		List<Material> altarBlocks,
		SignStyle sign,
		Effects effects,
		NpcSkins npcSkins,
		RankNames rankNames)
{
	/**
	 * What a theme's ranks are called ({@code ranks} in themes.yml): a name for every rank.
	 *
	 * @param names the name of each rank (every rank has one)
	 */
	public record RankNames(Map<GodRank, String> names)
	{
		/** The name of a rank */
		public String of(GodRank rank)
		{
			String name = this.names.get(rank);
			return name != null ? name : fallback(rank);
		}

		/** A rank's key with a capital, for a rank no section names */
		public static String fallback(GodRank rank)
		{
			String key = rank.key();
			return key.substring(0, 1).toUpperCase(Locale.ROOT) + key.substring(1);
		}
	}

	/**
	 * The skins of a theme's god NPCs ({@code npc.skin} in themes.yml), one per gender, each a texture Mojang signed or
	 * null. Which one a god wears is {@code GodNpcRules.skin}'s.
	 *
	 * @param god     for gods (Male, and None: gods from before genders)
	 * @param goddess for goddesses (Female)
	 * @param deity   for deities (NonBinary)
	 */
	public record NpcSkins(NpcSkin god, NpcSkin goddess, NpcSkin deity)
	{
		public static final NpcSkins NONE = new NpcSkins(null, null, null);

		/** Whether it has no skin at all */
		public boolean isEmpty()
		{
			return this.god == null && this.goddess == null && this.deity == null;
		}
	}

	/**
	 * How an altar sign looks once Gods has made it.
	 *
	 * @param messages the texts, {@code altar.sign.<messages>.*} in messages.yml (falling back to
	 *                 {@code altar.sign.default.*})
	 * @param dye      the sign's dye color
	 * @param glowing  whether the text glows
	 */
	public record SignStyle(String messages, DyeColor dye, boolean glowing)
	{
		public static final SignStyle PLAIN = new SignStyle("default", DyeColor.BLACK, false);
	}

	/** Which of a theme's effect styles an effect uses: the god's mood, or whether it accepts what it gets */
	public enum EffectMood
	{
		PLEASED, NEUTRAL, ANGRY;

		/** Its section under effects: in themes.yml */
		public String key()
		{
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	/**
	 * The effects of a god type's gods: a style per {@link EffectMood}, and a symbol that floats up at prayers.
	 *
	 * @param symbol  the text of the symbol (like ❄), empty for none
	 */
	public record Effects(String symbol, EffectStyle pleased, EffectStyle neutral, EffectStyle angry)
	{
		public static final Effects NONE = new Effects("", EffectStyle.NONE, EffectStyle.NONE, EffectStyle.NONE);

		public EffectStyle of(EffectMood mood)
		{
			return switch (mood)
			{
				case PLEASED -> this.pleased;
				case NEUTRAL -> this.neutral;
				case ANGRY -> this.angry;
			};
		}
	}

	/**
	 * One effect style: what an effect picks its particles, sound and colors from (each list may be empty).
	 *
	 * @param particles particles this server has, and that Gods can show (no block or item particles)
	 * @param sounds    sound ids ({@code minecraft:block.amethyst_block.chime})
	 * @param colors    colors for the particles that take one (dust) and the symbol
	 */
	public record EffectStyle(List<Particle> particles, List<String> sounds, List<Color> colors)
	{
		public static final EffectStyle NONE = new EffectStyle(List.of(), List.of(), List.of());
	}
}
