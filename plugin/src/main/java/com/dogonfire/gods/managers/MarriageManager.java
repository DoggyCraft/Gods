package com.dogonfire.gods.managers;

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
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.storage.DataFile;
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

	private Random				random				= new Random();

	private Material			marriageTokenType	= Material.GOLD_NUGGET;

	private final Set<String>	invalidIds			= new HashSet<String>();

	private final GodsContext ctx;

	public MarriageManager(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	/** marriages.yml; see {@link DataFile} */
	private DataFile marriagesFile()
	{
		return ctx.storage().marriages();
	}

	private YamlConfiguration marriagesConfig()
	{
		return marriagesFile().yaml();
	}

	public void divorce(UUID believerId)
	{
		String partnerId = marriagesConfig().getString(believerId.toString() + ".Married.Partner");

		marriagesConfig().set(believerId.toString(), null);

		if (partnerId != null)
		{
			marriagesConfig().set(partnerId, null);

			Player partner = getOnlinePlayer(partnerId);
			if (partner != null)
			{
				// Tell the partner who divorced them
				Player divorcer = ctx.plugin().getServer().getPlayer(believerId);
				String divorcerName = divorcer != null ? divorcer.getDisplayName() : ctx.plugin().getServer().getOfflinePlayer(believerId).getName();

				ctx.plugin().sendInfo(partner.getUniqueId(), LanguageManager.LANGUAGESTRING.DivorcedYou, ChatColor.RED, divorcerName, "DIVORCED", 1);
			}
		}
		
		marriagesFile().save();
	}

	public UUID getGettingMarriedPartner(UUID playerId)
	{
		String partnerId = marriagesConfig().getString(playerId.toString() + ".GettingMarried.Partner");

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
				ctx.plugin().log("Ignoring marriages.yml entry '" + value + "': it is not a player UUID");
			}
			return null;
		}
	}

	private Player getOnlinePlayer(String playerId)
	{
		UUID id = parseId(playerId);

		return id != null ? ctx.plugin().getServer().getPlayer(id) : null;
	}

	public List<MarriedCouple> getMarriedCouples()
	{
		Set<String> list = marriagesConfig().getKeys(false);
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
				String lastLoveString = marriagesConfig().getString(playerId + ".Married.LastLove");

				try
				{
					lastLove = formatter.parse(lastLoveString);
				}
				catch (Exception ex)
				{
					ctx.plugin().log("Invalid lastlove format for " + playerId);
					lastLove = new Date();

					marriagesConfig().set(playerId.toString() + ".Married.LastLove", formatter.format(lastLove));
					marriagesConfig().set(partnerId.toString() + ".Married.LastLove", formatter.format(lastLove));

					marriagesFile().save();
				}

				couples.add(new MarriedCouple(playerId, partnerId, ctx.believerService().getGodForBeliever(playerId), lastLove));

				names.add(playerId);
			}
		}

		Collections.sort(couples, new MarriedCoupleComparator());

		return couples;
	}

	public UUID getPartnerId(UUID believerId)
	{
		String partner = marriagesConfig().getString(believerId.toString() + ".Married.Partner");

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

		return ctx.plugin().getServer().getOfflinePlayer(partnerId).getName();
	}

	public UUID getProposal(UUID believerId)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();
		Date offerDate = null;

		if (!marriagesConfig().contains(believerId + ".MarriageProposal"))
		{
			return null;
		}

		String offerDateString = marriagesConfig().getString(believerId + ".MarriageProposal.Time");
		try
		{
			offerDate = formatter.parse(offerDateString);
		}
		catch (Exception ex)
		{
			ctx.plugin().logDebug("Could no parse marriage proposal time: " + ex.getMessage());
			offerDate = new Date();
			offerDate.setTime(0L);
		}

		long diff = thisDate.getTime() - offerDate.getTime();
		long diffSeconds = diff / 1000L;

		if (diffSeconds > 30L)
		{
			ctx.plugin().logDebug("getProposal DiffSeconds is " + diffSeconds);

			marriagesConfig().set(believerId + ".MarriageProposal", null);

			marriagesFile().save();

			return null;
		}

		String partner = marriagesConfig().getString(believerId.toString() + ".MarriageProposal.Partner");

		return partner != null ? parseId(partner) : null;
	}

	public void handleAcceptProposal(UUID playerId1, UUID playerId2, String godName)
	{
		Player player1 = ctx.plugin().getServer().getPlayer(playerId1);
		Player player2 = ctx.plugin().getServer().getPlayer(playerId2);

		// The proposal can be accepted after the other player logged off
		marriagesConfig().set(playerId1 + ".MarriageProposal", null);
		marriagesConfig().set(playerId2 + ".MarriageProposal", null);
		if (player1 == null || player2 == null)
		{
			marriagesFile().save();
			return;
		}

		try
		{
			ctx.languageManager().setType(ctx.languageManager().getItemTypeName(this.marriageTokenType));
		}
		catch (Exception ex)
		{
			ctx.plugin().logDebug(ex.getMessage());
		}

		ctx.languageManager().setPlayerName(player2.getDisplayName());
		ctx.godManager().GodSay(godName, player1, LanguageManager.LANGUAGESTRING.GodToBelieverAcceptedMarriageProposal, 2 + this.random.nextInt(40));

		ctx.languageManager().setPlayerName(player1.getDisplayName());
		ctx.godManager().GodSay(godName, player2, LanguageManager.LANGUAGESTRING.GodToBelieverAcceptedYourMarriageProposal, 2 + this.random.nextInt(40));

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

		Player partner = ctx.plugin().getServer().getPlayer(gettingMarriedPartnerId);

		if (player == null || partner == null)
		{
			return;
		}

		marriagesConfig().set(player.getUniqueId().toString() + ".GettingMarried.HasPickupWeddingToken", true);

		// if (hasPickupWeddingRing(player.getUniqueId()))
		{
			String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());

			if (hasPickupWeddingRing(gettingMarriedPartnerId))
			{
				setMarried(player.getUniqueId(), gettingMarriedPartnerId);

				float godPower = ctx.godManager().getGodPower(godName);

				ctx.godManager().blessPlayer(godName, player.getUniqueId(), godPower);
				ctx.godManager().blessPlayer(godName, gettingMarriedPartnerId, godPower);

				ctx.languageManager().setPlayerName(partner.getDisplayName());
				ctx.godManager().GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverMarried, 10);

				ctx.languageManager().setPlayerName(player.getDisplayName());
				ctx.godManager().GodSay(godName, partner, LanguageManager.LANGUAGESTRING.GodToBelieverMarried, 10);

				ctx.plugin().log(player.getDisplayName() + " just married " + partner.getDisplayName() + " in the name of " + godName);
				ctx.plugin().getServer().broadcastMessage(ChatColor.WHITE + player.getDisplayName() + ChatColor.AQUA + " just married " + ChatColor.WHITE + partner.getDisplayName() + ChatColor.AQUA + " in the name of " + ChatColor.GOLD + godName + ChatColor.AQUA + "!");
				if ((ctx.settings().holyArtifacts().enabled()) && (ctx.settings().marriage().weddingFireworks()))
				{
					ctx.holyPowerManager().shootFirework(player, 16);
					ctx.holyPowerManager().shootFirework(partner, 16);
				}
			}
			else
			{
				try
				{
					ctx.languageManager().setType(ctx.languageManager().getItemTypeName(this.marriageTokenType));
				}
				catch (Exception ex)
				{
					ctx.plugin().logDebug(ex.getMessage());
				}
				ctx.languageManager().setPlayerName(partner.getDisplayName());
				ctx.godManager().GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverMarriageTokenPickup, 10);

				ctx.languageManager().setPlayerName(player.getDisplayName());
				ctx.godManager().GodSay(godName, partner, LanguageManager.LANGUAGESTRING.GodToBelieverMarriagePartnerTokenPickup, 10);
			}
		}

		marriagesFile().save();
	}

	public boolean hasPickupWeddingRing(UUID playerId)
	{
		if (playerId == null)
		{
			return false;
		}

		if (marriagesConfig().getString(playerId.toString() + ".GettingMarried.HasPickupWeddingToken") == null)
		{
			return false;
		}

		return true;
	}

	public void load()
	{

		marriagesFile().load();

		ctx.plugin().log("Loaded " + marriagesConfig().getKeys(false).size() + " marriages.");
	}

	public void love(UUID playerId)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();
		String partnerId = marriagesConfig().getString(playerId.toString() + ".Married.Partner");

		if (partnerId != null)
		{
			Player player = ctx.plugin().getServer().getPlayer(playerId);
			Player partner = getOnlinePlayer(partnerId);

			marriagesConfig().set(playerId.toString() + ".Married.LastLove", formatter.format(thisDate));
			marriagesConfig().set(partnerId + ".Married.LastLove", formatter.format(thisDate));

			if (player != null && partner != null)
			{
				ctx.plugin().getServer().getScheduler().scheduleSyncDelayedTask(ctx.plugin(), new TaskLove(ctx, player, partner), 1L);

				ctx.plugin().sendInfo(partner.getUniqueId(), LanguageManager.LANGUAGESTRING.MarrigeLovesYou, ChatColor.GREEN, player.getDisplayName(), ChatColor.DARK_RED + "LOVES", 1);
			}
		}

		marriagesFile().save();
	}

	public void proposeMarriage(UUID player1, UUID player2)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		marriagesConfig().set(player1.toString() + ".GettingMarried", null);
		marriagesConfig().set(player2.toString() + ".GettingMarried", null);
		marriagesConfig().set(player1.toString() + ".Married", null);
		marriagesConfig().set(player2.toString() + ".Married", null);

		marriagesConfig().set(player1.toString() + ".MarriageProposal.Time", formatter.format(thisDate));
		marriagesConfig().set(player1.toString() + ".MarriageProposal.Partner", player2.toString());
		marriagesConfig().set(player2.toString() + ".MarriageProposal.Time", formatter.format(thisDate));
		marriagesConfig().set(player2.toString() + ".MarriageProposal.Partner", player1.toString());

		marriagesFile().save();
	}

	public void setGettingMarried(UUID player1, UUID player2)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		marriagesConfig().set(player1.toString() + ".Married", null);
		marriagesConfig().set(player2.toString() + ".Married", null);
		marriagesConfig().set(player1.toString() + ".MarriageProposal", null);
		marriagesConfig().set(player2.toString() + ".MarriageProposal", null);

		marriagesConfig().set(player1.toString() + ".GettingMarried.Time", formatter.format(thisDate));
		marriagesConfig().set(player1.toString() + ".GettingMarried.Partner", player2.toString());
		marriagesConfig().set(player2.toString() + ".GettingMarried.Time", formatter.format(thisDate));
		marriagesConfig().set(player2.toString() + ".GettingMarried.Partner", player1.toString());

		marriagesFile().save();
	}

	public void setMarried(UUID player1, UUID player2)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		marriagesConfig().set(player1.toString() + ".GettingMarried", null);
		marriagesConfig().set(player2.toString() + ".GettingMarried", null);
		marriagesConfig().set(player1.toString() + ".MarriageProposal", null);
		marriagesConfig().set(player2.toString() + ".MarriageProposal", null);

		marriagesConfig().set(player1.toString() + ".Married.Time", formatter.format(thisDate));
		marriagesConfig().set(player1.toString() + ".Married.Partner", player2.toString());
		marriagesConfig().set(player2.toString() + ".Married.Time", formatter.format(thisDate));
		marriagesConfig().set(player2.toString() + ".Married.Partner", player1.toString());

		marriagesFile().save();
	}
}
