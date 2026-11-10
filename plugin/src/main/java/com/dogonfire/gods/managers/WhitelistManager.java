package com.dogonfire.gods.managers;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.storage.DataFile;

/**
 * whitelist.yml (only these gods are allowed) and blacklist.yml (these gods are not), each read only while it is
 * switched on in config.yml.
 */
public class WhitelistManager
{
	private final GodsContext ctx;

	public WhitelistManager(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	private DataFile whitelistFile()
	{
		return ctx.storage().whitelist();
	}

	private DataFile blacklistFile()
	{
		return ctx.storage().blacklist();
	}

	public float getMinGodPower(String godName)
	{
		int power = whitelistFile().yaml().getInt(godName + ".MinPower");

		return power;
	}

	public boolean isBlacklistedGod(String godName)
	{
		String name = blacklistFile().yaml().getString(godName);

		return name != null;
	}

	public boolean isWhitelistedGod(String godName)
	{
		String name = whitelistFile().yaml().getString(godName);

		return name != null;
	}

	public void load()
	{
		if (ctx.settings().gods().whitelist())
		{
			DataFile whitelist = whitelistFile();
			whitelist.load();

			ctx.plugin().log("Loaded " + whitelist.yaml().getKeys(false).size() + " whitelisted Gods.");

			if (whitelist.yaml().getKeys(false).size() == 0)
			{
				whitelist.yaml().set("TheExampleGodName.MinPower", Integer.valueOf(0));

				whitelist.save();
			}

			ctx.plugin().log("Using whitelist");
		}

		if (ctx.settings().gods().blacklist())
		{
			DataFile blacklist = blacklistFile();
			blacklist.load();

			ctx.plugin().log("Loaded " + blacklist.yaml().getKeys(false).size() + " blacklisted Gods.");
			if (blacklist.yaml().getKeys(false).size() == 0)
			{
				blacklist.yaml().set("TheExampleGodName", "");

				blacklist.save();
			}

			ctx.plugin().log("Using blacklist");
		}
	}
}
