package com.dogonfire.gods.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.HolyBook;
import com.dogonfire.gods.config.GodsConfiguration;

public class HolyBookManager
{
	private static HolyBookManager instance;

	public static HolyBookManager instance()
	{
		if (GodsConfiguration.instance().isBiblesEnabled() && instance == null)
			instance = new HolyBookManager();
		return instance;
	}

	private FileConfiguration	biblesConfig		= null;
	private File				biblesConfigFile	= null;

	private HolyBookManager()
	{
	}

	public void clearBible(String godName)
	{
		this.biblesConfig.set(godName, null);

		save();
	}

	private List<String> getPagesOrInit(String godName)
	{
		List<String> pages = this.biblesConfig.getStringList(godName + ".Pages");
		if (pages.size() == 0)
		{
			initBible(godName);
			pages = this.biblesConfig.getStringList(godName + ".Pages");
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
		return this.biblesConfig.getString(godName + ".Title");
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
				return this.biblesConfig.contains(godName) ? godName : null;
			}

			String author = bible.getAuthor();
			if (author == null)
			{
				return null;
			}
			author = ChatColor.stripColor(author);
			for (String god : this.biblesConfig.getKeys(false))
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

	/**
	 * Nothing happens to bibles when a quest is completed. Kept for the
	 * existing QuestManager call.
	 */
	public void handleQuestCompleted(String godName, QuestManager.QUESTTYPE type, String playerName)
	{
	}

	private void initBible(String godName)
	{
		Gods.instance().logDebug("Creating bible for '" + godName + "' ...");

		List<String> pages = new ArrayList<String>();

		this.biblesConfig.set(godName + ".Title", getDefaultBibleTitle(godName));

		this.biblesConfig.set(godName + ".Author", godName);

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

			this.biblesConfig.set(godName + ".Pages", pages);
		}
		catch (Exception ex)
		{
			Gods.instance().logDebug(ex.getStackTrace().toString());
		}

		save();

		// if (Gods.instance().propheciesEnabled)
		// {
		// Gods.instance().getProphecyManager().generateProphecies(godName);
		// }
	}

	public void load()
	{
		if (this.biblesConfigFile == null)
		{
			this.biblesConfigFile = new File(Gods.instance().getDataFolder(), "bibles.yml");
		}
		this.biblesConfig = YamlConfiguration.loadConfiguration(this.biblesConfigFile);

		Gods.instance().log("Loaded " + this.biblesConfig.getKeys(false).size() + " bibles.");
	}

	public void save()
	{
		if ((this.biblesConfig == null) || (this.biblesConfigFile == null))
		{
			return;
		}
		try
		{
			this.biblesConfig.save(this.biblesConfigFile);
		}
		catch (Exception ex)
		{
			Gods.instance().log("Could not save config to " + this.biblesConfigFile.getName() + ": " + ex.getMessage());
		}
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

		this.biblesConfig.set(godName + ".Title", title);
		this.biblesConfig.set(godName + ".Author", player.getName());
		this.biblesConfig.set(godName + ".Pages", pages);

		save();

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

	public void setProphecyPages(String godName, List<String> prophecyPages)
	{
		List<String> pages = this.biblesConfig.getStringList(godName + ".Pages");
		List<String> newPages = new ArrayList<String>();
		for (String page : pages)
		{
			if (page.contains("Prophecies of " + godName))
			{
				break;
			}
			newPages.add(page);
		}
		for (String page : prophecyPages)
		{
			newPages.add(page);
		}
		this.biblesConfig.set(godName + ".Pages", newPages);

		save();
	}
}
