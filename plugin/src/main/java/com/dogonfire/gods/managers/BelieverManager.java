package com.dogonfire.gods.managers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.managers.LanguageManager.LANGUAGESTRING;
import com.dogonfire.gods.storage.DataFile;

public class BelieverManager
{
	private final Set<String>	invalidKeys			= new HashSet<String>();

	private final GodsContext ctx;

	public BelieverManager(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	/** believers.yml; see {@link DataFile} */
	private DataFile believersFile()
	{
		return ctx.storage().believers();
	}

	private YamlConfiguration believersConfig()
	{
		return believersFile().yaml();
	}

	public boolean addPrayer(UUID believerId, String godName)
	{
		String lastPrayer = believersConfig().getString(believerId + ".LastPrayer");

		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date lastPrayerDate = null;
		Date thisDate = new Date();
		try
		{
			lastPrayerDate = formatter.parse(lastPrayer);
		}
		catch (Exception ex)
		{
			lastPrayerDate = new Date();
			lastPrayerDate.setTime(0L);
		}

		int prayers = believersConfig().getInt(believerId + ".Prayers");
		String oldGod = believersConfig().getString(believerId + ".God");

		if ((oldGod != null) && !oldGod.equals(godName))
		{
			prayers = 0;
			lastPrayerDate.setTime(0L);
		}

		long diff = thisDate.getTime() - lastPrayerDate.getTime();

		long diffMinutes = diff / 60000L;
		if (diffMinutes < ctx.settings().prayers().minMinutesBetweenPrayers())
		{
			return false;
		}

		if (oldGod == null || !oldGod.equals(godName))
		{
			believersConfig().set(believerId + ".Joined", formatter.format(thisDate));
		}

		prayers++;

		believersConfig().set(believerId + ".LastPrayer", formatter.format(thisDate));
		believersConfig().set(believerId + ".God", godName);
		believersConfig().set(believerId + ".Prayers", Integer.valueOf(prayers));

		believersFile().markDirty();
		ctx.chatManager().refresh(believerId);

		return true;
	}

	public void believerLeave(String godName, UUID believerId)
	{
		if (godName == null || believerId == null)
		{
			return;
		}
		String believerGodName = believersConfig().getString(believerId + ".God");
		if (!godName.equals(believerGodName))
		{
			return;
		}
		believersConfig().set(believerId + ".God", null);

		believersFile().markDirty();
		ctx.chatManager().refresh(believerId);
	}

	void clearChangingGod(UUID believerId)
	{
		believersConfig().set(believerId + ".ChangingGod", null);

		believersFile().markDirty();
	}

	public void clearInvitation(UUID believerId)
	{
		believersConfig().set(believerId.toString() + ".Invitation", null);

		believersFile().markDirty();
	}

	public void clearPendingPriest(UUID believerId)
	{
		believersConfig().set(believerId + ".LastPriestOffer", null);
		believersFile().markDirty();
	}

	public void clearPrayerPower(UUID believerId)
	{
		believersConfig().set(believerId + ".PrayerPower", null);

		believersFile().markDirty();

		ctx.plugin().sendInfo(believerId, LANGUAGESTRING.YourPrayerPower, ChatColor.AQUA, 0, "", 10);
	}

	public float getBelieverPower(UUID believerId)
	{
		Date date = new Date();

		float time = 1.0F + 2.5E-008F * (date.getTime() - getLastPrayerTime(believerId).getTime());

		return getPrayers(believerId) / time;
	}

	public Set<String> getBelievers()
	{
		Set<String> allBelievers = believersConfig().getKeys(false);

		return allBelievers;
	}

	public Set<UUID> getBelieversForGod(String godName)
	{
		Set<String> allBelievers = believersConfig().getKeys(false);
		Set<UUID> believers = new HashSet<UUID>();

		for (String believer : allBelievers)
		{
			UUID believerId = parseBelieverId(believer);
			if (believerId == null)
			{
				continue;
			}

			String believerGod = getGodForBeliever(believerId);

			if ((believerGod != null) && (believerGod.equals(godName)))
			{
				believers.add(believerId);
			}
		}

		return believers;
	}

	boolean getChangingGod(UUID believerId)
	{
		String changingGodString = believersConfig().getString(believerId + ".ChangingGod");

		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date changingGodDate = null;
		boolean changing = false;
		Date thisDate = new Date();
		try
		{
			changingGodDate = formatter.parse(changingGodString);

			long diff = thisDate.getTime() - changingGodDate.getTime();
			long diffSeconds = diff / 1000L;

			changing = diffSeconds <= 10L;
		}
		catch (Exception ex)
		{
			changing = false;
		}
		return changing;
	}

	public String getGodForBeliever(UUID believerId)
	{
		return believersConfig().getString(believerId + ".God");
	}

	public String getInvitation(UUID believerId)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();
		Date offerDate = null;

		String offerDateString = believersConfig().getString(believerId.toString() + ".Invitation.Time");
		try
		{
			offerDate = formatter.parse(offerDateString);
		}
		catch (Exception ex)
		{
			offerDate = new Date();
			offerDate.setTime(0L);
		}
		long diff = thisDate.getTime() - offerDate.getTime();
		long diffSeconds = diff / 1000L;
		if (diffSeconds > 30L)
		{
			believersConfig().set(believerId.toString() + ".Invitation", null);

			believersFile().markDirty();

			return null;
		}
		return believersConfig().getString(believerId.toString() + ".Invitation.God");
	}

	public Date getLastPrayerTime(UUID believerId)
	{
		String lastPrayerString = believersConfig().getString(believerId + ".LastPrayer");

		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date lastPrayerDate = null;
		try
		{
			lastPrayerDate = formatter.parse(lastPrayerString);
		}
		catch (Exception ex)
		{
			lastPrayerDate = new Date();
			lastPrayerDate.setTime(0L);
		}
		return lastPrayerDate;
	}

	public Set<UUID> getOnlineBelieversForGod(String godName)
	{
		Set<UUID> believers = new HashSet<UUID>();

		for (Player player : ctx.plugin().getServer().getOnlinePlayers())
		{
			String believerGod = getGodForBeliever(player.getUniqueId());
			if ((believerGod != null) && (believerGod.equals(godName)))
			{
				believers.add(player.getUniqueId());
			}
		}

		return believers;
	}

	/**
	 * Top-level keys in believers.yml are believer UUIDs. Very old files can
	 * still hold entries keyed by player name; those are skipped (and logged
	 * once) instead of breaking every lookup.
	 */
	private UUID parseBelieverId(String key)
	{
		try
		{
			return UUID.fromString(key);
		}
		catch (IllegalArgumentException ex)
		{
			if (this.invalidKeys.add(key))
			{
				ctx.plugin().log("Ignoring believers.yml entry '" + key + "': it is not a player UUID");
			}
			return null;
		}
	}

	public int getPrayerPower(UUID believerId)
	{
		int prayerPower = believersConfig().getInt(believerId + ".PrayerPower");

		return prayerPower;
	}

	public int getPrayers(UUID believerId)
	{
		int prayers = believersConfig().getInt(believerId + ".Prayers");

		return prayers;
	}

	public boolean getReligionChat(UUID believerId)
	{
		return believersConfig().getBoolean(believerId + ".ReligionChat");
	}

	public int getTimeUntilCanPray(UUID believerId)
	{
		String lastPrayerString = believersConfig().getString(believerId + ".LastPrayer");

		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date lastPrayerDate = null;
		Date thisDate = new Date();
		try
		{
			lastPrayerDate = formatter.parse(lastPrayerString);
		}
		catch (Exception ex)
		{
			lastPrayerDate = new Date();
			lastPrayerDate.setTime(0L);
		}
		long diff = ctx.settings().prayers().minMinutesBetweenPrayers() * 60 * 1000 + lastPrayerDate.getTime() - thisDate.getTime();
		long diffSeconds = diff / 1000L;
		int diffMinutes = (int) (diffSeconds / 60L);

		return diffMinutes;
	}

	public boolean hasRecentBlessing(UUID believerId)
	{
		String lastItemBlessingString = believersConfig().getString(believerId + ".LastBlessingTime");

		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date lastItemBlessingDate = null;
		Date thisDate = new Date();
		try
		{
			lastItemBlessingDate = formatter.parse(lastItemBlessingString);
		}
		catch (Exception ex)
		{
			lastItemBlessingDate = new Date();
			lastItemBlessingDate.setTime(0L);
		}
		long diff = thisDate.getTime() - lastItemBlessingDate.getTime();

		long diffSeconds = diff / 1000L;

		return diffSeconds < ctx.settings().blessings().minSecondsBetweenBlessings();
	}

	public boolean hasRecentCursing(UUID believerId)
	{
		String lastItemBlessingString = believersConfig().getString(believerId + ".LastCursingTime");

		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date lastItemBlessingDate = null;
		Date thisDate = new Date();
		try
		{
			lastItemBlessingDate = formatter.parse(lastItemBlessingString);
		}
		catch (Exception ex)
		{
			lastItemBlessingDate = new Date();
			lastItemBlessingDate.setTime(0L);
		}
		long diff = thisDate.getTime() - lastItemBlessingDate.getTime();
		long diffSeconds = diff / 1000L;

		return diffSeconds < ctx.settings().curses().minSecondsBetweenCurses();
	}

	boolean hasRecentGodChange(UUID believerId)
	{
		String joinedGodString = believersConfig().getString(believerId + ".Joined");

		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date joinedGodDate = null;
		Date thisDate = new Date();
		boolean joinedRecently;

		try
		{
			joinedGodDate = formatter.parse(joinedGodString);

			long diff = thisDate.getTime() - joinedGodDate.getTime();
			long diffSeconds = diff / 1000L;

			joinedRecently = diffSeconds <= ctx.settings().believers().minSecondsBetweenChangingGod();
		}
		catch (Exception ex)
		{
			joinedRecently = false;
		}

		return joinedRecently;
	}

	public boolean hasRecentHolyArtifactBlessing(UUID believerId)
	{
		String lastItemBlessingString = believersConfig().getString(believerId + ".LastHolyArtifactBlessingTime");

		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date lastItemBlessingDate = null;
		Date thisDate = new Date();
		try
		{
			lastItemBlessingDate = formatter.parse(lastItemBlessingString);
		}
		catch (Exception ex)
		{
			lastItemBlessingDate = new Date();
			lastItemBlessingDate.setTime(0L);
		}
		long diff = thisDate.getTime() - lastItemBlessingDate.getTime();
		long diffMinutes = diff / 60000L;

		return diffMinutes < ctx.settings().holyArtifacts().minMinutesBetweenBlessings();
	}

	public boolean hasRecentItemBlessing(UUID believerId)
	{
		String lastItemBlessingString = believersConfig().getString(believerId + ".LastItemBlessingTime");

		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date lastItemBlessingDate = null;
		Date thisDate = new Date();
		try
		{
			lastItemBlessingDate = formatter.parse(lastItemBlessingString);
		}
		catch (Exception ex)
		{
			lastItemBlessingDate = new Date();
			lastItemBlessingDate.setTime(0L);
		}
		long diff = thisDate.getTime() - lastItemBlessingDate.getTime();
		long diffMinutes = diff / 60000L;

		return diffMinutes < ctx.settings().itemBlessings().minMinutesBetweenBlessings();
	}

	public boolean hasRecentPrayer(UUID believerId)
	{
		String lastPrayerString = believersConfig().getString(believerId + ".LastPrayer");

		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date lastPrayerDate = null;
		Date thisDate = new Date();
		try
		{
			lastPrayerDate = formatter.parse(lastPrayerString);
		}
		catch (Exception ex)
		{
			lastPrayerDate = new Date();
			lastPrayerDate.setTime(0L);
		}
		long diff = thisDate.getTime() - lastPrayerDate.getTime();
		long diffMinutes = diff / 60000L;

		return diffMinutes <= ctx.settings().prayers().minMinutesBetweenPrayers();
	}

	public boolean hasRecentPriestOffer(UUID believerId)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();
		Date offerDate = null;

		String offerDateString = believersConfig().getString(believerId + ".LastPriestOffer");
		try
		{
			offerDate = formatter.parse(offerDateString);
		}
		catch (Exception ex)
		{
			offerDate = new Date();
			offerDate.setTime(0L);
		}
		long diff = thisDate.getTime() - offerDate.getTime();
		long diffMinutes = diff / 60000L;

		return diffMinutes <= 60L;
	}

	public boolean incPrayer(UUID believerId, String godName)
	{
		int prayers = believersConfig().getInt(believerId + ".Prayers");

		prayers++;

		believersConfig().set(believerId + ".God", godName);
		believersConfig().set(believerId + ".Prayers", Integer.valueOf(prayers));

		believersFile().markDirty();
		ctx.chatManager().refresh(believerId);

		return true;
	}

	public boolean increasePrayer(UUID believerId, String godName, int incPrayers)
	{
		String lastPrayer = believersConfig().getString(believerId + ".LastPrayer");

		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date lastPrayerDate = null;
		Date thisDate = new Date();
		try
		{
			lastPrayerDate = formatter.parse(lastPrayer);
		}
		catch (Exception ex)
		{
			lastPrayerDate = new Date();
			lastPrayerDate.setTime(0L);
		}
		int prayers = believersConfig().getInt(believerId + ".Prayers");
		String oldGod = believersConfig().getString(believerId + ".God");
		if ((oldGod != null) && (!oldGod.equals(godName)))
		{
			prayers = 0;
			lastPrayerDate.setTime(0L);
		}
		long diff = thisDate.getTime() - lastPrayerDate.getTime();

		long diffMinutes = diff / 60000L;
		if (diffMinutes < ctx.settings().prayers().minMinutesBetweenPrayers())
		{
			return false;
		}
		prayers += incPrayers;

		believersConfig().set(believerId + ".LastPrayer", formatter.format(thisDate));
		believersConfig().set(believerId + ".God", godName);
		believersConfig().set(believerId + ".Prayers", Integer.valueOf(prayers));

		believersFile().markDirty();
		ctx.chatManager().refresh(believerId);

		return true;
	}

	public void increasePrayerPower(UUID believerId, int powerChange)
	{
		int prayerPower = believersConfig().getInt(believerId + ".PrayerPower");

		prayerPower += powerChange;

		if (prayerPower < 0)
		{
			prayerPower = 0;
		}

		believersConfig().set(believerId + ".PrayerPower", prayerPower);

		believersFile().markDirty();

		ctx.plugin().sendInfo(believerId, LANGUAGESTRING.YourPrayerPower, ChatColor.AQUA, prayerPower, "", 10);
	}

	public boolean isHunting(UUID believerId)
	{
		boolean hunting = false;
		try
		{
			hunting = believersConfig().getBoolean(believerId + ".Hunting");
		}
		catch (Exception ex)
		{
			setHunting(believerId, false);
		}
		return hunting;
	}

	public void load()
	{
		believersFile().load();
		this.invalidKeys.clear();

		ctx.plugin().log("Loaded " + believersConfig().getKeys(false).size() + " believers.");
	}

	public void removeBeliever(String godName, UUID believerId)
	{
		String believerGodName = believersConfig().getString(believerId + ".God");

		if (believerGodName != null && !believerGodName.equals(godName))
		{
			return;
		}

		clearPrayerPower(believerId);

		believersConfig().set(believerId.toString(), null);

		ctx.plugin().log(godName + " lost " + believerId + " as believer");

		believersFile().markDirty();
		ctx.chatManager().refresh(believerId);
	}

	public void setBlessingTime(UUID believerId)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		believersConfig().set(believerId + ".LastBlessingTime", formatter.format(thisDate));

		believersFile().markDirty();
	}

	void setChangingGod(UUID believerId)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		believersConfig().set(believerId + ".ChangingGod", formatter.format(thisDate));
		believersFile().markDirty();
	}

	public void setCursingTime(UUID believerId)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		believersConfig().set(believerId + ".LastCursingTime", formatter.format(thisDate));

		believersFile().markDirty();
	}

	public void setHolyArtifactBlessingTime(UUID believerId)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		believersConfig().set(believerId + ".LastHolyArtifactBlessingTime", formatter.format(thisDate));

		believersFile().markDirty();
	}

	public void setHunting(UUID believerId, boolean hunting)
	{
		ctx.plugin().logDebug("Setting hunting string to '" + hunting + "' for " + believerId);

		believersConfig().set(believerId + ".Hunting", Boolean.valueOf(hunting));

		believersFile().markDirty();
	}

	public void setInvitation(UUID believerId, String godName)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		believersConfig().set(believerId.toString() + ".Invitation.Time", formatter.format(thisDate));
		believersConfig().set(believerId.toString() + ".Invitation.God", godName);

		believersFile().markDirty();
	}

	public void setItemBlessingTime(UUID believerId)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		believersConfig().set(believerId + ".LastItemBlessingTime", formatter.format(thisDate));

		believersFile().markDirty();
	}

	public void setLastPrayerDate(UUID believerId)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		believersConfig().set(believerId + ".LastPrayer", formatter.format(thisDate));

		believersFile().markDirty();
	}

	public void setPendingPriest(UUID believerId)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		believersConfig().set(believerId + ".LastPriestOffer", formatter.format(thisDate));
		believersFile().markDirty();
	}

	public void setReligionChat(UUID believerId, boolean enabled)
	{
		if (enabled)
		{
			believersConfig().set(believerId + ".ReligionChat", Boolean.valueOf(true));
		}
		else
		{
			believersConfig().set(believerId + ".ReligionChat", null);
		}
		believersFile().markDirty();
		ctx.chatManager().refresh(believerId);
	}
}
