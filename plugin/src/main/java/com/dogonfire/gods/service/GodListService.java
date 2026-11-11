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
	// Whether whitelist.yml was read at least once; a reload that can't read it keeps the list read before
	private boolean						whitelistUsable;

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
			boolean read = this.whitelist.load();
			if (read)
			{
				this.whitelistUsable = true;
			}
			else if (this.whitelistUsable)
			{
				ctx.plugin().getLogger().warning("whitelist.yml could not be read; using the whitelist as it was before the reload.");
			}
			else
			{
				// An empty whitelist would block every god
				ctx.plugin().getLogger().warning("whitelist.yml could not be read, so the whitelist is OFF until it is fixed (and Gods is reloaded).");
			}

			if (read && this.whitelist.size() == 0)
			{
				this.whitelist.put(EXAMPLE_GOD, Map.of("MinPower", 0));
				this.whitelist.save();
			}

			ctx.plugin().log("Using whitelist");
		}

		if (ctx.settings().gods().blacklist())
		{
			boolean read = this.blacklist.load();

			if (read && this.blacklist.size() == 0)
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

	/**
	 * Whether this god may be prayed to as far as the whitelist goes: always while it is off, or while whitelist.yml
	 * has never been read successfully (it can't be read)
	 */
	public boolean isWhitelistedGod(String godName)
	{
		return !ctx.settings().gods().whitelist() || !this.whitelistUsable || this.whitelist.contains(godName);
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
