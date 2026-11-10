package com.dogonfire.gods.managers;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.storage.DataFile;

/**
 * whitelist.yml (only these gods are allowed) and blacklist.yml (these gods are not), each read only while it is
 * switched on in config.yml.
 */
public class WhitelistManager
{
	private static WhitelistManager instance;

	public static WhitelistManager instance()
	{
		if (instance == null)
			instance = new WhitelistManager();
		return instance;
	}

	private WhitelistManager()
	{
	}

	private DataFile whitelistFile()
	{
		return Gods.instance().storage().whitelist();
	}

	private DataFile blacklistFile()
	{
		return Gods.instance().storage().blacklist();
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
		if (Gods.instance().settings().gods().whitelist())
		{
			DataFile whitelist = whitelistFile();
			whitelist.load();

			Gods.instance().log("Loaded " + whitelist.yaml().getKeys(false).size() + " whitelisted Gods.");

			if (whitelist.yaml().getKeys(false).size() == 0)
			{
				whitelist.yaml().set("TheExampleGodName.MinPower", Integer.valueOf(0));

				whitelist.save();
			}

			Gods.instance().log("Using whitelist");
		}

		if (Gods.instance().settings().gods().blacklist())
		{
			DataFile blacklist = blacklistFile();
			blacklist.load();

			Gods.instance().log("Loaded " + blacklist.yaml().getKeys(false).size() + " blacklisted Gods.");
			if (blacklist.yaml().getKeys(false).size() == 0)
			{
				blacklist.yaml().set("TheExampleGodName", "");

				blacklist.save();
			}

			Gods.instance().log("Using blacklist");
		}
	}
}
