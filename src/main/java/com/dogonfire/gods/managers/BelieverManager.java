package com.dogonfire.gods.managers;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.LanguageManager.LANGUAGESTRING;

public class BelieverManager
{
	private static BelieverManager instance;

	public static BelieverManager instance()
	{
		if (instance == null)
			instance = new BelieverManager();
		return instance;
	}

	private static final long	SAVE_INTERVAL_MILLIS	= 180000L;
	private static final long	SAVE_INTERVAL_TICKS		= SAVE_INTERVAL_MILLIS / 50L;

	private FileConfiguration	believersConfig		= null;

	private File				believersConfigFile	= null;
	private long				lastSaveTime;
	private boolean				dirty				= false;
	private BukkitTask			flushTask			= null;
	private final Set<String>	invalidKeys			= new HashSet<String>();

	private BelieverManager()
	{
	}

	public boolean addPrayer(UUID believerId, String godName)
	{
		String lastPrayer = this.believersConfig.getString(believerId + ".LastPrayer");

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

		int prayers = this.believersConfig.getInt(believerId + ".Prayers");
		String oldGod = this.believersConfig.getString(believerId + ".God");

		if ((oldGod != null) && !oldGod.equals(godName))
		{
			prayers = 0;
			lastPrayerDate.setTime(0L);
		}

		long diff = thisDate.getTime() - lastPrayerDate.getTime();

		long diffMinutes = diff / 60000L;
		if (diffMinutes < GodsConfiguration.instance().getMinBelieverPrayerTime())
		{
			return false;
		}

		if (oldGod == null || !oldGod.equals(godName))
		{
			this.believersConfig.set(believerId + ".Joined", formatter.format(thisDate));
		}

		prayers++;

		this.believersConfig.set(believerId + ".LastPrayer", formatter.format(thisDate));
		this.believersConfig.set(believerId + ".God", godName);
		this.believersConfig.set(believerId + ".Prayers", Integer.valueOf(prayers));

		saveTimed();
		ChatManager.instance().refresh(believerId);

		return true;
	}

	public void believerLeave(String godName, UUID believerId)
	{
		if (godName == null || believerId == null)
		{
			return;
		}
		String believerGodName = this.believersConfig.getString(believerId + ".God");
		if (!godName.equals(believerGodName))
		{
			return;
		}
		this.believersConfig.set(believerId + ".God", null);

		saveTimed();
		ChatManager.instance().refresh(believerId);
	}

	void clearChangingGod(UUID believerId)
	{
		this.believersConfig.set(believerId + ".ChangingGod", null);

		saveTimed();
	}

	public void clearInvitation(UUID believerId)
	{
		this.believersConfig.set(believerId.toString() + ".Invitation", null);

		saveTimed();
	}

	public void clearPendingPriest(UUID believerId)
	{
		this.believersConfig.set(believerId + ".LastPriestOffer", null);
		saveTimed();
	}

	public void clearPrayerPower(UUID believerId)
	{
		this.believersConfig.set(believerId + ".PrayerPower", null);

		saveTimed();

		Gods.instance().sendInfo(believerId, LANGUAGESTRING.YourPrayerPower, ChatColor.AQUA, 0, "", 10);
	}

	public float getBelieverPower(UUID believerId)
	{
		Date date = new Date();

		float time = 1.0F + 2.5E-008F * (date.getTime() - getLastPrayerTime(believerId).getTime());

		return getPrayers(believerId) / time;
	}

	public Set<String> getBelievers()
	{
		Set<String> allBelievers = this.believersConfig.getKeys(false);

		return allBelievers;
	}

	public Set<UUID> getBelieversForGod(String godName)
	{
		Set<String> allBelievers = this.believersConfig.getKeys(false);
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
		String changingGodString = this.believersConfig.getString(believerId + ".ChangingGod");

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
		return this.believersConfig.getString(believerId + ".God");
	}

	public String getInvitation(UUID believerId)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();
		Date offerDate = null;

		String offerDateString = this.believersConfig.getString(believerId.toString() + ".Invitation.Time");
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
			this.believersConfig.set(believerId.toString() + ".Invitation", null);

			saveTimed();

			return null;
		}
		return this.believersConfig.getString(believerId.toString() + ".Invitation.God");
	}

	public Date getLastPrayerTime(UUID believerId)
	{
		String lastPrayerString = this.believersConfig.getString(believerId + ".LastPrayer");

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

		for (Player player : Gods.instance().getServer().getOnlinePlayers())
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
				Gods.instance().log("Ignoring believers.yml entry '" + key + "': it is not a player UUID");
			}
			return null;
		}
	}

	public int getPrayerPower(UUID believerId)
	{
		int prayerPower = this.believersConfig.getInt(believerId + ".PrayerPower");

		return prayerPower;
	}

	public int getPrayers(UUID believerId)
	{
		int prayers = this.believersConfig.getInt(believerId + ".Prayers");

		return prayers;
	}

	public boolean getReligionChat(UUID believerId)
	{
		return this.believersConfig.getBoolean(believerId + ".ReligionChat");
	}

	public int getTimeUntilCanPray(UUID believerId)
	{
		String lastPrayerString = this.believersConfig.getString(believerId + ".LastPrayer");

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
		long diff = GodsConfiguration.instance().getMinBelieverPrayerTime() * 60 * 1000 + lastPrayerDate.getTime() - thisDate.getTime();
		long diffSeconds = diff / 1000L;
		int diffMinutes = (int) (diffSeconds / 60L);

		return diffMinutes;
	}

	public boolean hasRecentBlessing(UUID believerId)
	{
		String lastItemBlessingString = this.believersConfig.getString(believerId + ".LastBlessingTime");

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

		return diffSeconds < GodsConfiguration.instance().getMinBlessingTime();
	}

	public boolean hasRecentCursing(UUID believerId)
	{
		String lastItemBlessingString = this.believersConfig.getString(believerId + ".LastCursingTime");

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

		return diffSeconds < GodsConfiguration.instance().getMinCursingTime();
	}

	boolean hasRecentGodChange(UUID believerId)
	{
		String joinedGodString = this.believersConfig.getString(believerId + ".Joined");

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

			joinedRecently = diffSeconds <= GodsConfiguration.instance().getMinSecondsBetweenChangingGod();
		}
		catch (Exception ex)
		{
			joinedRecently = false;
		}

		return joinedRecently;
	}

	public boolean hasRecentHolyArtifactBlessing(UUID believerId)
	{
		String lastItemBlessingString = this.believersConfig.getString(believerId + ".LastHolyArtifactBlessingTime");

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

		return diffMinutes < GodsConfiguration.instance().getMinHolyArtifactBlessingTime();
	}

	public boolean hasRecentItemBlessing(UUID believerId)
	{
		String lastItemBlessingString = this.believersConfig.getString(believerId + ".LastItemBlessingTime");

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

		return diffMinutes < GodsConfiguration.instance().getMinItemBlessingTime();
	}

	public boolean hasRecentPrayer(UUID believerId)
	{
		String lastPrayerString = this.believersConfig.getString(believerId + ".LastPrayer");

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

		return diffMinutes <= GodsConfiguration.instance().getMinBelieverPrayerTime();
	}

	public boolean hasRecentPriestOffer(UUID believerId)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();
		Date offerDate = null;

		String offerDateString = this.believersConfig.getString(believerId + ".LastPriestOffer");
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
		int prayers = this.believersConfig.getInt(believerId + ".Prayers");

		prayers++;

		this.believersConfig.set(believerId + ".God", godName);
		this.believersConfig.set(believerId + ".Prayers", Integer.valueOf(prayers));

		saveTimed();
		ChatManager.instance().refresh(believerId);

		return true;
	}

	public boolean increasePrayer(UUID believerId, String godName, int incPrayers)
	{
		String lastPrayer = this.believersConfig.getString(believerId + ".LastPrayer");

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
		int prayers = this.believersConfig.getInt(believerId + ".Prayers");
		String oldGod = this.believersConfig.getString(believerId + ".God");
		if ((oldGod != null) && (!oldGod.equals(godName)))
		{
			prayers = 0;
			lastPrayerDate.setTime(0L);
		}
		long diff = thisDate.getTime() - lastPrayerDate.getTime();

		long diffMinutes = diff / 60000L;
		if (diffMinutes < GodsConfiguration.instance().getMinBelieverPrayerTime())
		{
			return false;
		}
		prayers += incPrayers;

		this.believersConfig.set(believerId + ".LastPrayer", formatter.format(thisDate));
		this.believersConfig.set(believerId + ".God", godName);
		this.believersConfig.set(believerId + ".Prayers", Integer.valueOf(prayers));

		saveTimed();
		ChatManager.instance().refresh(believerId);

		return true;
	}

	public void increasePrayerPower(UUID believerId, int powerChange)
	{
		int prayerPower = this.believersConfig.getInt(believerId + ".PrayerPower");

		prayerPower += powerChange;

		if (prayerPower < 0)
		{
			prayerPower = 0;
		}

		this.believersConfig.set(believerId + ".PrayerPower", prayerPower);

		saveTimed();

		Gods.instance().sendInfo(believerId, LANGUAGESTRING.YourPrayerPower, ChatColor.AQUA, prayerPower, "", 10);
	}

	public boolean isHunting(UUID believerId)
	{
		boolean hunting = false;
		try
		{
			hunting = this.believersConfig.getBoolean(believerId + ".Hunting");
		}
		catch (Exception ex)
		{
			setHunting(believerId, false);
		}
		return hunting;
	}

	public void load()
	{
		if (this.believersConfigFile == null)
		{
			this.believersConfigFile = new File(Gods.instance().getDataFolder(), "believers.yml");
		}
		this.believersConfig = YamlConfiguration.loadConfiguration(this.believersConfigFile);
		this.dirty = false;
		this.invalidKeys.clear();

		Gods.instance().log("Loaded " + this.believersConfig.getKeys(false).size() + " believers.");

		// saveTimed() writes at most every few minutes; this writes what it skipped
		if (this.flushTask != null)
		{
			this.flushTask.cancel();
		}
		this.flushTask = Gods.instance().getServer().getScheduler().runTaskTimer(Gods.instance(), this::saveIfDirty, SAVE_INTERVAL_TICKS, SAVE_INTERVAL_TICKS);
	}

	public void removeBeliever(String godName, UUID believerId)
	{
		String believerGodName = this.believersConfig.getString(believerId + ".God");

		if (believerGodName != null && !believerGodName.equals(godName))
		{
			return;
		}

		clearPrayerPower(believerId);

		this.believersConfig.set(believerId.toString(), null);

		Gods.instance().log(godName + " lost " + believerId + " as believer");

		saveTimed();
		ChatManager.instance().refresh(believerId);
	}

	public void save()
	{
		this.lastSaveTime = System.currentTimeMillis();
		if ((this.believersConfig == null) || (this.believersConfigFile == null))
		{
			return;
		}
		this.dirty = false;
		try
		{
			this.believersConfig.save(this.believersConfigFile);
		}
		catch (Exception ex)
		{
			Gods.instance().log("Could not save config to " + this.believersConfigFile.getName() + ": " + ex.getMessage());
		}
	}

	/** Writes believers.yml if there are changes saveTimed() has not written yet. */
	public void saveIfDirty()
	{
		if (this.dirty)
		{
			save();
		}
	}

	/** Marks the data as changed and writes it at most every few minutes. */
	public void saveTimed()
	{
		this.dirty = true;

		if (System.currentTimeMillis() - this.lastSaveTime < SAVE_INTERVAL_MILLIS)
		{
			return;
		}
		save();
	}

	public void setBlessingTime(UUID believerId)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		this.believersConfig.set(believerId + ".LastBlessingTime", formatter.format(thisDate));

		saveTimed();
	}

	void setChangingGod(UUID believerId)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		this.believersConfig.set(believerId + ".ChangingGod", formatter.format(thisDate));
		saveTimed();
	}

	public void setCursingTime(UUID believerId)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		this.believersConfig.set(believerId + ".LastCursingTime", formatter.format(thisDate));

		saveTimed();
	}

	public void setHolyArtifactBlessingTime(UUID believerId)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		this.believersConfig.set(believerId + ".LastHolyArtifactBlessingTime", formatter.format(thisDate));

		saveTimed();
	}

	public void setHunting(UUID believerId, boolean hunting)
	{
		Gods.instance().logDebug("Setting hunting string to '" + hunting + "' for " + believerId);

		this.believersConfig.set(believerId + ".Hunting", Boolean.valueOf(hunting));

		saveTimed();
	}

	public void setInvitation(UUID believerId, String godName)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		this.believersConfig.set(believerId.toString() + ".Invitation.Time", formatter.format(thisDate));
		this.believersConfig.set(believerId.toString() + ".Invitation.God", godName);

		saveTimed();
	}

	public void setItemBlessingTime(UUID believerId)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		this.believersConfig.set(believerId + ".LastItemBlessingTime", formatter.format(thisDate));

		saveTimed();
	}

	public void setLastPrayerDate(UUID believerId)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		this.believersConfig.set(believerId + ".LastPrayer", formatter.format(thisDate));

		saveTimed();
	}

	public void setPendingPriest(UUID believerId)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		this.believersConfig.set(believerId + ".LastPriestOffer", formatter.format(thisDate));
		saveTimed();
	}

	public void setReligionChat(UUID believerId, boolean enabled)
	{
		if (enabled)
		{
			this.believersConfig.set(believerId + ".ReligionChat", Boolean.valueOf(true));
		}
		else
		{
			this.believersConfig.set(believerId + ".ReligionChat", null);
		}
		saveTimed();
		ChatManager.instance().refresh(believerId);
	}
}