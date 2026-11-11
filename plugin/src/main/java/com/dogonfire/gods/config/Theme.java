package com.dogonfire.gods.config;

import java.util.List;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

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
 */
public record Theme(
		List<EntityType> holyMobs,
		List<EntityType> unholyMobs,
		List<Material> holyFoods,
		List<Material> unholyFoods,
		List<Material> sacrificeNeeds,
		List<EntityType> slayTargets,
		List<Material> altarBlocks,
		SignStyle sign)
{
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
}
