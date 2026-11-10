package com.dogonfire.gods.service;

import java.util.Map;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.storage.GodListRepository;

/**
 * The god whitelist (whitelist.yml: only these gods can be prayed to, each with a minimum power) and blacklist
 * (blacklist.yml: these gods can't), each used and loaded only while it is switched on in config.yml. A new, empty
 * list gets an example entry so admins see the format.
 */
public class GodListService
{
	private static final String			EXAMPLE_GOD	= "TheExampleGodName";

	private final GodsContext			ctx;
	private final GodListRepository		whitelist;
	private final GodListRepository		blacklist;

	public GodListService(GodsContext ctx, GodListRepository whitelist, GodListRepository blacklist)
	{
		this.ctx = ctx;
		this.whitelist = whitelist;
		this.blacklist = blacklist;
	}

	public void load()
	{
		if (ctx.settings().gods().whitelist())
		{
			this.whitelist.load();

			if (this.whitelist.size() == 0)
			{
				this.whitelist.put(EXAMPLE_GOD, Map.of("MinPower", 0));
				this.whitelist.save();
			}

			ctx.plugin().log("Using whitelist");
		}

		if (ctx.settings().gods().blacklist())
		{
			this.blacklist.load();

			if (this.blacklist.size() == 0)
			{
				this.blacklist.put(EXAMPLE_GOD, "");
				this.blacklist.save();
			}

			ctx.plugin().log("Using blacklist");
		}
	}

	/** Whether the blacklist is on and has this god */
	public boolean isBlacklistedGod(String godName)
	{
		return ctx.settings().gods().blacklist() && this.blacklist.contains(godName);
	}

	/** Whether this god may be prayed to as far as the whitelist goes: always while it is off */
	public boolean isWhitelistedGod(String godName)
	{
		return !ctx.settings().gods().whitelist() || this.whitelist.contains(godName);
	}

	/** Whether this god may be prayed to: not blacklisted, and whitelisted if the whitelist is on */
	public boolean isAllowedGod(String godName)
	{
		return !isBlacklistedGod(godName) && isWhitelistedGod(godName);
	}

	/** The power the whitelist gives this god as a minimum (only while the whitelist is on) */
	public float getMinGodPower(String godName)
	{
		return this.whitelist.minPower(godName);
	}
}
