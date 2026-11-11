package com.dogonfire.gods.model;

/**
 * The kinds of quest a god can give. Only PILGRIMAGE, SLAY, ITEMSACRIFICE and GIVEITEMS are generated now, but every
 * constant may be stored in quests.yml (Gods 0.9.x made the others), so they all stay. Stored as {@code Type} in the
 * form {@link #toString()} gives ({@code "Pilgrimage"}); read back in any case.
 */
public enum QuestType
{
	NONE, FIREWORKPARTY, HUMANSACRIFICE, MOBSACRIFICE, ITEMSACRIFICE, GIVEROSE, KILLBOSS, HARVEST, CONVERT, GETHOLYARTIFACT, DELIVERITEM, SLAY, BUILDALTARS, BUILDTOWER, HOLYFEAST, COLLECTBIBLES, BURNBIBLES, GIVEITEMS, PILGRIMAGE, HOLYWAR, CLAIMHOLYLAND, PVPREVENGE, SPREADLOVE, SHOWHOLYARTIFACT, CRAFTITEMS, SLAYDRAGON, CRUSADE;

	/** The quest type stored as {@code name} (any case), or {@link #NONE} if there is none of that name */
	public static QuestType parse(String name)
	{
		if (name == null)
		{
			return NONE;
		}
		try
		{
			return valueOf(name.toUpperCase());
		}
		catch (IllegalArgumentException ex)
		{
			return NONE;
		}
	}

	/** As written to quests.yml: {@code "Pilgrimage"} */
	@Override
	public String toString()
	{
		String output = name();
		return output.charAt(0) + output.substring(1).toLowerCase();
	}
}
