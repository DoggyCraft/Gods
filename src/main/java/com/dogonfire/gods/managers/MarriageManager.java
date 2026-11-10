package com.dogonfire.gods.managers;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.tasks.TaskLove;

public class MarriageManager
{
	public class MarriedCouple
	{
		public UUID		player1Id;
		public UUID		player2Id;
		public String	godName;
		public Date		lastLove;

		MarriedCouple(UUID player1Id, UUID player2Id, String godName, Date lastLove)
		{
			this.player1Id = player1Id;
			this.player2Id = player2Id;
			this.godName = godName;
			this.lastLove = lastLove;
		}
	}

	public class MarriedCoupleComparator implements Comparator<MarriageManager.MarriedCouple>
	{
		public MarriedCoupleComparator()
		{
		}

		@Override
		public int compare(MarriageManager.MarriedCouple object1, MarriageManager.MarriedCouple object2)
		{
			MarriageManager.MarriedCouple b1 = object1;
			MarriageManager.MarriedCouple b2 = object2;

			// Most recent love first
			return Long.compare(b2.lastLove.getTime(), b1.lastLove.getTime());
		}
	}

	private static MarriageManager instance;

	public static MarriageManager instance()
	{
		if (GodsConfiguration.instance().isMarriageEnabled() && instance == null)
		{
			instance = new MarriageManager();
		}
		
		return instance;
	}

	private FileConfiguration	marriagesConfig		= null;

	private File				marriagesConfigFile	= null;

	private Random				random				= new Random();

	private Material			marriageTokenType	= Material.GOLD_NUGGET;

	private final Set<String>	invalidIds			= new HashSet<String>();

	private MarriageManager()
	{
	}

	public void divorce(UUID believerId)
	{
		String partnerId = this.marriagesConfig.getString(believerId.toString() + ".Married.Partner");

		this.marriagesConfig.set(believerId.toString(), null);

		if (partnerId != null)
		{
			this.marriagesConfig.set(partnerId, null);

			Player partner = getOnlinePlayer(partnerId);
			if (partner != null)
			{
				// Tell the partner who divorced them
				Player divorcer = Gods.instance().getServer().getPlayer(believerId);
				String divorcerName = divorcer != null ? divorcer.getDisplayName() : Gods.instance().getServer().getOfflinePlayer(believerId).getName();

				Gods.instance().sendInfo(partner.getUniqueId(), LanguageManager.LANGUAGESTRING.DivorcedYou, ChatColor.RED, divorcerName, "DIVORCED", 1);
			}
		}
		
		save();
	}

	public UUID getGettingMarriedPartner(UUID playerId)
	{
		String partnerId = this.marriagesConfig.getString(playerId.toString() + ".GettingMarried.Partner");

		if (partnerId == null)
		{
			return null;
		}

		return parseId(partnerId);
	}

	/**
	 * marriages.yml is keyed by player UUID. Very old files can still hold
	 * entries keyed by player name; those are skipped (and logged once).
	 */
	private UUID parseId(String value)
	{
		try
		{
			return UUID.fromString(value);
		}
		catch (IllegalArgumentException ex)
		{
			if (this.invalidIds.add(value))
			{
				Gods.instance().log("Ignoring marriages.yml entry '" + value + "': it is not a player UUID");
			}
			return null;
		}
	}

	private Player getOnlinePlayer(String playerId)
	{
		UUID id = parseId(playerId);

		return id != null ? Gods.instance().getServer().getPlayer(id) : null;
	}

	public List<MarriedCouple> getMarriedCouples()
	{
		Set<String> list = this.marriagesConfig.getKeys(false);
		List<MarriedCouple> couples = new ArrayList<MarriedCouple>();
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date lastLove = null;

		List<UUID> names = new ArrayList<UUID>();

		for (String player : list)
		{
			UUID playerId = parseId(player);
			if (playerId == null)
			{
				continue;
			}

			UUID partnerId = getPartnerId(playerId);

			if (partnerId == null)
			{
				continue;
			}

			if (!names.contains(partnerId))
			{
				String lastLoveString = this.marriagesConfig.getString(playerId + ".Married.LastLove");

				try
				{
					lastLove = formatter.parse(lastLoveString);
				}
				catch (Exception ex)
				{
					Gods.instance().log("Invalid lastlove format for " + playerId);
					lastLove = new Date();

					this.marriagesConfig.set(playerId.toString() + ".Married.LastLove", formatter.format(lastLove));
					this.marriagesConfig.set(partnerId.toString() + ".Married.LastLove", formatter.format(lastLove));

					save();
				}

				couples.add(new MarriedCouple(playerId, partnerId, BelieverManager.instance().getGodForBeliever(playerId), lastLove));

				names.add(playerId);
			}
		}

		Collections.sort(couples, new MarriedCoupleComparator());

		return couples;
	}

	public UUID getPartnerId(UUID believerId)
	{
		String partner = this.marriagesConfig.getString(believerId.toString() + ".Married.Partner");

		if (partner == null)
		{
			return null;
		}

		return parseId(partner);
	}

	public String getPartnerName(UUID playerId)
	{
		UUID partnerId = getPartnerId(playerId);

		if (partnerId == null)
		{
			return null;
		}

		return Gods.instance().getServer().getOfflinePlayer(partnerId).getName();
	}

	public UUID getProposal(UUID believerId)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();
		Date offerDate = null;

		if (!this.marriagesConfig.contains(believerId + ".MarriageProposal"))
		{
			return null;
		}

		String offerDateString = this.marriagesConfig.getString(believerId + ".MarriageProposal.Time");
		try
		{
			offerDate = formatter.parse(offerDateString);
		}
		catch (Exception ex)
		{
			Gods.instance().logDebug("Could no parse marriage proposal time: " + ex.getMessage());
			offerDate = new Date();
			offerDate.setTime(0L);
		}

		long diff = thisDate.getTime() - offerDate.getTime();
		long diffSeconds = diff / 1000L;

		if (diffSeconds > 30L)
		{
			Gods.instance().logDebug("getProposal DiffSeconds is " + diffSeconds);

			this.marriagesConfig.set(believerId + ".MarriageProposal", null);

			save();

			return null;
		}

		String partner = this.marriagesConfig.getString(believerId.toString() + ".MarriageProposal.Partner");

		return partner != null ? parseId(partner) : null;
	}

	public void handleAcceptProposal(UUID playerId1, UUID playerId2, String godName)
	{
		Player player1 = Gods.instance().getServer().getPlayer(playerId1);
		Player player2 = Gods.instance().getServer().getPlayer(playerId2);

		try
		{
			LanguageManager.instance().setType(LanguageManager.instance().getItemTypeName(this.marriageTokenType));
		}
		catch (Exception ex)
		{
			Gods.instance().logDebug(ex.getStackTrace().toString());
		}

		LanguageManager.instance().setPlayerName(player2.getDisplayName());
		GodManager.instance().GodSay(godName, player1, LanguageManager.LANGUAGESTRING.GodToBelieverAcceptedMarriageProposal, 2 + this.random.nextInt(40));

		LanguageManager.instance().setPlayerName(player1.getDisplayName());
		GodManager.instance().GodSay(godName, player2, LanguageManager.LANGUAGESTRING.GodToBelieverAcceptedYourMarriageProposal, 2 + this.random.nextInt(40));

		setGettingMarried(playerId1, playerId2);
	}

	public void handlePickupItem(Player player, Item item, Location location)
	{
		if (item.getItemStack().getType() != this.marriageTokenType)
		{
			return;
		}

		{
			UUID partnerId = getPartnerId(player.getUniqueId());

			// Already married
			if (partnerId != null)
			{
				return;
			}
		}

		UUID gettingMarriedPartnerId = getGettingMarriedPartner(player.getUniqueId());

		if (gettingMarriedPartnerId == null)
		{
			return;
		}

		Player partner = Gods.instance().getServer().getPlayer(gettingMarriedPartnerId);

		if (player == null || partner == null)
		{
			return;
		}

		this.marriagesConfig.set(player.getUniqueId().toString() + ".GettingMarried.HasPickupWeddingToken", true);

		// if (hasPickupWeddingRing(player.getUniqueId()))
		{
			String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());

			if (hasPickupWeddingRing(gettingMarriedPartnerId))
			{
				setMarried(player.getUniqueId(), gettingMarriedPartnerId);

				float godPower = GodManager.instance().getGodPower(godName);

				GodManager.instance().blessPlayer(godName, player.getUniqueId(), godPower);
				GodManager.instance().blessPlayer(godName, gettingMarriedPartnerId, godPower);

				LanguageManager.instance().setPlayerName(partner.getDisplayName());
				GodManager.instance().GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverMarried, 10);

				LanguageManager.instance().setPlayerName(player.getDisplayName());
				GodManager.instance().GodSay(godName, partner, LanguageManager.LANGUAGESTRING.GodToBelieverMarried, 10);

				Gods.instance().log(player.getDisplayName() + " just married " + partner.getDisplayName() + " in the name of " + godName);
				Gods.instance().getServer().broadcastMessage(ChatColor.WHITE + player.getDisplayName() + ChatColor.AQUA + " just married " + ChatColor.WHITE + partner.getDisplayName() + ChatColor.AQUA + " in the name of " + ChatColor.GOLD + godName + ChatColor.AQUA + "!");
				if ((GodsConfiguration.instance().isHolyArtifactsEnabled()) && (GodsConfiguration.instance().isMarriageFireworksEnabled()))
				{
					HolyPowerManager.instance().shootFirework(player, 16);
					HolyPowerManager.instance().shootFirework(partner, 16);
				}
			}
			else
			{
				try
				{
					LanguageManager.instance().setType(LanguageManager.instance().getItemTypeName(this.marriageTokenType));
				}
				catch (Exception ex)
				{
					Gods.instance().logDebug(ex.getStackTrace().toString());
				}
				LanguageManager.instance().setPlayerName(partner.getDisplayName());
				GodManager.instance().GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverMarriageTokenPickup, 10);

				LanguageManager.instance().setPlayerName(player.getDisplayName());
				GodManager.instance().GodSay(godName, partner, LanguageManager.LANGUAGESTRING.GodToBelieverMarriagePartnerTokenPickup, 10);
			}
		}

		save();
	}

	public boolean hasPickupWeddingRing(UUID playerId)
	{
		if (playerId == null)
		{
			return false;
		}

		if (this.marriagesConfig.getString(playerId.toString() + ".GettingMarried.HasPickupWeddingToken") == null)
		{
			return false;
		}

		return true;
	}

	public void load()
	{
		if (this.marriagesConfigFile == null)
		{
			this.marriagesConfigFile = new File(Gods.instance().getDataFolder(), "marriages.yml");
		}

		this.marriagesConfig = YamlConfiguration.loadConfiguration(this.marriagesConfigFile);

		Gods.instance().log("Loaded " + this.marriagesConfig.getKeys(false).size() + " marriages.");
	}

	public void love(UUID playerId)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();
		String partnerId = this.marriagesConfig.getString(playerId.toString() + ".Married.Partner");

		if (partnerId != null)
		{
			Player player = Gods.instance().getServer().getPlayer(playerId);
			Player partner = getOnlinePlayer(partnerId);

			this.marriagesConfig.set(playerId.toString() + ".Married.LastLove", formatter.format(thisDate));
			this.marriagesConfig.set(partnerId + ".Married.LastLove", formatter.format(thisDate));

			if (player != null && partner != null)
			{
				Gods.instance().getServer().getScheduler().scheduleSyncDelayedTask(Gods.instance(), new TaskLove(player, partner), 1L);

				Gods.instance().sendInfo(partner.getUniqueId(), LanguageManager.LANGUAGESTRING.MarrigeLovesYou, ChatColor.GREEN, player.getDisplayName(), ChatColor.DARK_RED + "LOVES", 1);
			}
		}

		save();
	}

	public void proposeMarriage(UUID player1, UUID player2)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		this.marriagesConfig.set(player1.toString() + ".GettingMarried", null);
		this.marriagesConfig.set(player2.toString() + ".GettingMarried", null);
		this.marriagesConfig.set(player1.toString() + ".Married", null);
		this.marriagesConfig.set(player2.toString() + ".Married", null);

		this.marriagesConfig.set(player1.toString() + ".MarriageProposal.Time", formatter.format(thisDate));
		this.marriagesConfig.set(player1.toString() + ".MarriageProposal.Partner", player2.toString());
		this.marriagesConfig.set(player2.toString() + ".MarriageProposal.Time", formatter.format(thisDate));
		this.marriagesConfig.set(player2.toString() + ".MarriageProposal.Partner", player1.toString());

		save();
	}

	public void save()
	{
		if ((this.marriagesConfig == null) || (this.marriagesConfigFile == null))
		{
			return;
		}
		try
		{
			this.marriagesConfig.save(this.marriagesConfigFile);
		}
		catch (Exception ex)
		{
			Gods.instance().log("Could not save config to " + this.marriagesConfigFile.getName() + ": " + ex.getMessage());
		}
	}

	public void setGettingMarried(UUID player1, UUID player2)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		this.marriagesConfig.set(player1.toString() + ".Married", null);
		this.marriagesConfig.set(player2.toString() + ".Married", null);
		this.marriagesConfig.set(player1.toString() + ".MarriageProposal", null);
		this.marriagesConfig.set(player2.toString() + ".MarriageProposal", null);

		this.marriagesConfig.set(player1.toString() + ".GettingMarried.Time", formatter.format(thisDate));
		this.marriagesConfig.set(player1.toString() + ".GettingMarried.Partner", player2.toString());
		this.marriagesConfig.set(player2.toString() + ".GettingMarried.Time", formatter.format(thisDate));
		this.marriagesConfig.set(player2.toString() + ".GettingMarried.Partner", player1.toString());

		save();
	}

	public void setMarried(UUID player1, UUID player2)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		this.marriagesConfig.set(player1.toString() + ".GettingMarried", null);
		this.marriagesConfig.set(player2.toString() + ".GettingMarried", null);
		this.marriagesConfig.set(player1.toString() + ".MarriageProposal", null);
		this.marriagesConfig.set(player2.toString() + ".MarriageProposal", null);

		this.marriagesConfig.set(player1.toString() + ".Married.Time", formatter.format(thisDate));
		this.marriagesConfig.set(player1.toString() + ".Married.Partner", player2.toString());
		this.marriagesConfig.set(player2.toString() + ".Married.Time", formatter.format(thisDate));
		this.marriagesConfig.set(player2.toString() + ".Married.Partner", player1.toString());

		save();
	}
}