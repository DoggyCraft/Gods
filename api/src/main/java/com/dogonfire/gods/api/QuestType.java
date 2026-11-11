package com.dogonfire.gods.api;

/** The kinds of quest a god gives its believers. */
public enum QuestType
{
	/** Find the god's holy place (a chest) and open it; {@link QuestInfo#location()} is where it is */
	PILGRIMAGE,
	/** Slay a number of mobs of a kind; {@link QuestInfo#target()} is the mob type */
	SLAY,
	/** Sacrifice a number of items of a kind at an altar; {@link QuestInfo#target()} is the item type */
	ITEM_SACRIFICE,
	/** Get players without a god to read the god's holy book */
	GIVE_ITEMS,
	/** A kind of quest older Gods versions made, which can still be in the data files */
	OTHER
}
