package com.dogonfire.gods.managers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.storage.DataFile;
import com.dogonfire.gods.HolyBook;

public class HolyBookManager
{
	private static HolyBookManager instance;

	public static HolyBookManager instance()
	{
		if (Gods.instance().settings().bibles().enabled() && instance == null)
			instance = new HolyBookManager();
		return instance;
	}

	private HolyBookManager()
	{
	}

	/** bibles.yml; see {@link DataFile} */
	private DataFile biblesFile()
	{
		return Gods.instance().storage().bibles();
	}

	private YamlConfiguration biblesConfig()
	{
		return biblesFile().yaml();
	}

	public void clearBible(String godName)
	{
		biblesConfig().set(godName, null);

		biblesFile().save();
	}

	private List<String> getPagesOrInit(String godName)
	{
		List<String> pages = biblesConfig().getStringList(godName + ".Pages");
		if (pages.size() == 0)
		{
			initBible(godName);
			pages = biblesConfig().getStringList(godName + ".Pages");
		}
		return pages;
	}

	private String getDefaultBibleTitle(String godName)
	{
		return "Holy Book of " + godName;
	}

	public ItemStack getBible(String godName)
	{
		List<String> pages = getPagesOrInit(godName);

		try
		{
			HolyBook b = new HolyBook(new ItemStack(Material.WRITTEN_BOOK, 1));

			String title = getBibleTitle(godName);
			b.setTitle(title == null ? getDefaultBibleTitle(godName) : ChatColor.stripColor(title));
			b.setAuthor(godName);
			b.setPages(pages);
			b.setGodName(godName);

			return b.getItem();
		}
		catch (Exception ex)
		{
			Gods.instance().logDebug("ERROR: Could not instance a bible for '" + godName + ": " + ex.getMessage());
		}
		return null;
	}

	public String getBibleTitle(String godName)
	{
		return biblesConfig().getString(godName + ".Title");
	}

	public ItemStack getEditBible(String godName)
	{
		List<String> pages = getPagesOrInit(godName);

		try
		{
			// Writable books have no title or author
			HolyBook b = new HolyBook(new ItemStack(Material.WRITABLE_BOOK, 1));
			b.setPages(pages);

			return b.getItem();
		}
		catch (Exception ex)
		{
			Gods.instance().logDebug("ERROR: Could not instance a bible for '" + godName + ": " + ex.getMessage());
		}
		return null;
	}

	/**
	 * The God whose Holy Book this is, or null if it is not a Holy Book.
	 * Holy Books are tagged with their God; books from older versions are
	 * recognized by their author.
	 */
	public String getGodForBible(ItemStack book)
	{
		if (book == null || book.getType() != Material.WRITTEN_BOOK)
		{
			return null;
		}
		try
		{
			HolyBook bible = new HolyBook(book);

			String godName = bible.getGodName();
			if (godName != null)
			{
				return biblesConfig().contains(godName) ? godName : null;
			}

			String author = bible.getAuthor();
			if (author == null)
			{
				return null;
			}
			author = ChatColor.stripColor(author);
			for (String god : biblesConfig().getKeys(false))
			{
				if (god.equals(author))
				{
					return god;
				}
			}
		}
		catch (Exception ex)
		{
			Gods.instance().log("ERROR: Could not get bible for " + book + ": " + ex.getMessage());
		}
		return null;
	}

	/**
	 * Puts a book in the player's main hand. The item they held moves to their
	 * inventory, or drops at their feet if the inventory is full.
	 */
	private void giveBook(Player player, ItemStack book)
	{
		PlayerInventory inventory = player.getInventory();
		ItemStack itemInHand = inventory.getItemInMainHand().clone();

		inventory.setItemInMainHand(book);

		if (itemInHand.getType().isAir() || itemInHand.getAmount() <= 0)
		{
			return;
		}
		for (ItemStack leftover : inventory.addItem(itemInHand).values())
		{
			player.getWorld().dropItemNaturally(player.getLocation(), leftover);
		}
	}

	public boolean giveBible(String godName, String playerName)
	{
		ItemStack bible = getBible(godName);
		if (bible == null)
		{
			return false;
		}
		Player player = Gods.instance().getServer().getPlayerExact(playerName);
		if (player == null)
		{
			Gods.instance().logDebug("ERROR: Could not give bible to offline player '" + playerName);
			return false;
		}
		giveBook(player, bible);

		return true;
	}

	public boolean giveEditBible(String godName, String playerName)
	{
		ItemStack bible = getEditBible(godName);
		if (bible == null)
		{
			return false;
		}
		Player player = Gods.instance().getServer().getPlayerExact(playerName);
		if (player == null)
		{
			Gods.instance().logDebug("ERROR: Could not give editable bible to offline player '" + playerName);
			return false;
		}
		giveBook(player, bible);

		return true;
	}

	private void initBible(String godName)
	{
		Gods.instance().logDebug("Creating bible for '" + godName + "' ...");

		List<String> pages = new ArrayList<String>();

		biblesConfig().set(godName + ".Title", getDefaultBibleTitle(godName));

		biblesConfig().set(godName + ".Author", godName);

		LanguageManager.instance().setPlayerName(godName);

		pages.add(LanguageManager.instance().getLanguageStringForBook(godName, LanguageManager.LANGUAGESTRING.DefaultBibleText1));
		try
		{
			LanguageManager.instance().setType(LanguageManager.instance().getMobTypeName(GodManager.instance().getUnholyMobTypeForGod(godName)));
			pages.add(LanguageManager.instance().getLanguageStringForBook(godName, LanguageManager.LANGUAGESTRING.DefaultBibleText2));

			LanguageManager.instance().setType(LanguageManager.instance().getItemTypeName(GodManager.instance().getHolyFoodTypeForGod(godName)));

			pages.add(LanguageManager.instance().getLanguageStringForBook(godName, LanguageManager.LANGUAGESTRING.DefaultBibleText3));

			LanguageManager.instance().setType(LanguageManager.instance().getItemTypeName(GodManager.instance().getUnholyFoodTypeForGod(godName)));

			pages.add(LanguageManager.instance().getLanguageStringForBook(godName, LanguageManager.LANGUAGESTRING.DefaultBibleText4));

			LanguageManager.instance().setType(LanguageManager.instance().getMobTypeName(GodManager.instance().getHolyMobTypeForGod(godName)));
			pages.add(LanguageManager.instance().getLanguageStringForBook(godName, LanguageManager.LANGUAGESTRING.DefaultBibleText5));

			LanguageManager.instance().setType(LanguageManager.instance().getMobTypeName(GodManager.instance().getHolyMobTypeForGod(godName)));
			pages.add(LanguageManager.instance().getLanguageStringForBook(godName, LanguageManager.LANGUAGESTRING.DefaultBibleText6));

			LanguageManager.instance().setType(LanguageManager.instance().getMobTypeName(GodManager.instance().getHolyMobTypeForGod(godName)));
			pages.add(LanguageManager.instance().getLanguageStringForBook(godName, LanguageManager.LANGUAGESTRING.DefaultBibleText7));

			biblesConfig().set(godName + ".Pages", pages);
		}
		catch (Exception ex)
		{
			Gods.instance().logDebug(ex.getMessage());
		}

		biblesFile().save();

		// if (Gods.instance().propheciesEnabled)
		// {
		// }
	}

	public void load()
	{
		biblesFile().load();

		Gods.instance().log("Loaded " + biblesConfig().getKeys(false).size() + " bibles.");
	}

	/**
	 * Makes the book in the priest's main hand the Holy Book of their God. A
	 * signed book keeps its title. A book and quill keeps the current title,
	 * and the priest gets a signed copy of the new Holy Book in its place.
	 */
	public boolean setBible(String godName, String priestName)
	{
		Player player = Gods.instance().getServer().getPlayerExact(priestName);
		if (player == null)
		{
			return false;
		}
		ItemStack item = player.getInventory().getItemInMainHand();
		if ((item.getType() != Material.WRITTEN_BOOK) && (item.getType() != Material.WRITABLE_BOOK))
		{
			return false;
		}

		HolyBook book;
		try
		{
			book = new HolyBook(item);
		}
		catch (Exception ex)
		{
			Gods.instance().logDebug("ERROR: Could not set a bible for '" + godName + ": " + ex.getMessage());
			return false;
		}

		List<String> pages = book.getPages();
		if (pages.isEmpty())
		{
			return false;
		}

		String title = null;
		if (item.getType() == Material.WRITTEN_BOOK && book.getTitle() != null)
		{
			title = ChatColor.stripColor(book.getTitle());
		}
		if (title == null || title.isBlank())
		{
			String oldTitle = getBibleTitle(godName);
			title = oldTitle == null ? getDefaultBibleTitle(godName) : ChatColor.stripColor(oldTitle);
		}

		biblesConfig().set(godName + ".Title", title);
		biblesConfig().set(godName + ".Author", player.getName());
		biblesConfig().set(godName + ".Pages", pages);

		biblesFile().save();

		if (item.getType() == Material.WRITABLE_BOOK)
		{
			ItemStack bible = getBible(godName);
			if (bible != null)
			{
				player.getInventory().setItemInMainHand(bible);
			}
		}
		else
		{
			book.setGodName(godName);
		}

		return true;
	}
}
