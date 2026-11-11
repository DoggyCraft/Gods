package com.dogonfire.gods.model;

/** What an altar sign is for; the key is its {@code gods:altar} tag and its {@code Kind} in altars.yml */
public enum AltarKind
{
	PRAYING("praying"), CURSES("curses"), BLESSINGS("blessings");

	private final String key;

	AltarKind(String key)
	{
		this.key = key;
	}

	public String key()
	{
		return this.key;
	}

	/** The kind with this key, or null */
	public static AltarKind byKey(String key)
	{
		for (AltarKind kind : values())
		{
			if (kind.key.equals(key))
			{
				return kind;
			}
		}
		return null;
	}
}
