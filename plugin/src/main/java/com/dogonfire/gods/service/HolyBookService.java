package com.dogonfire.gods.service;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.model.HolyBook;
import com.dogonfire.gods.storage.HolyBookRepository;

/**
 * Holy books (bibles): the text of each god's book, copies for believers (/g bible), copies to edit and setting a new
 * text (priests), and which god a book in a player's hand belongs to. Owns every change to bibles.yml
 * ({@link HolyBookRepository}); every change is written at once, as before. A god without a book gets one written from
 * its commandments the first time it is needed. Its data is only loaded while bibles are enabled. Main thread only.
 */
public class HolyBookService
{
	private final GodsContext			ctx;
	private final HolyBookRepository	repository;

	public HolyBookService(GodsContext ctx, HolyBookRepository repository)
	{
		this.ctx = ctx;
		this.repository = repository;
	}

	public void load()
	{
		this.repository.load();
	}

	public void clearBible(String godName)
	{
		this.repository.remove(godName);
		this.repository.save();
	}

	private static String defaultTitle(String godName)
	{
		return "Holy Book of " + godName;
	}

	/** The god's pages, writing the default book first if it has none */
	private List<String> pagesOrInit(String godName)
	{
		HolyBook book = this.repository.find(godName);
		if (book == null || book.pagesOrEmpty().isEmpty())
		{
			initBible(godName);
			book = this.repository.find(godName);
		}
		return book.pagesOrEmpty();
	}

	/** A signed copy of the god's holy book, tagged with the god; null if it can't be made */
	public ItemStack getBible(String godName)
	{
		List<String> pages = pagesOrInit(godName);

		try
		{
			HolyBookItem b = new HolyBookItem(ctx.plugin(), new ItemStack(Material.WRITTEN_BOOK, 1));

			String title = getBibleTitle(godName);
			b.setTitle(title == null ? defaultTitle(godName) : ChatColor.stripColor(title));
			b.setAuthor(godName);
			b.setPages(pages);
			b.setGodName(godName);

			return b.getItem();
		}
		catch (Exception ex)
		{
			ctx.plugin().logDebug("ERROR: Could not instance a bible for '" + godName + ": " + ex.getMessage());
		}
		return null;
	}

	/** The title of the god's holy book, or null if it has none */
	public String getBibleTitle(String godName)
	{
		HolyBook book = this.repository.find(godName);
		return (book != null) ? book.title() : null;
	}

	/** A book and quill with the god's pages; null if it can't be made */
	public ItemStack getEditBible(String godName)
	{
		List<String> pages = pagesOrInit(godName);

		try
		{
			// Writable books have no title or author
			HolyBookItem b = new HolyBookItem(ctx.plugin(), new ItemStack(Material.WRITABLE_BOOK, 1));
			b.setPages(pages);

			return b.getItem();
		}
		catch (Exception ex)
		{
			ctx.plugin().logDebug("ERROR: Could not instance a bible for '" + godName + ": " + ex.getMessage());
		}
		return null;
	}

	/**
	 * The God whose Holy Book this is, or null if it is not a Holy Book. Holy Books are tagged with their God; books
	 * from older versions are recognized by their author.
	 */
	public String getGodForBible(ItemStack book)
	{
		if (book == null || book.getType() != Material.WRITTEN_BOOK)
		{
			return null;
		}
		try
		{
			HolyBookItem bible = new HolyBookItem(ctx.plugin(), book);

			String godName = bible.getGodName();
			if (godName != null)
			{
				return (this.repository.find(godName) != null) ? godName : null;
			}

			String author = bible.getAuthor();
			if (author == null)
			{
				return null;
			}
			author = ChatColor.stripColor(author);
			return (this.repository.find(author) != null) ? author : null;
		}
		catch (Exception ex)
		{
			ctx.plugin().log("ERROR: Could not get bible for " + book + ": " + ex.getMessage());
		}
		return null;
	}

	/**
	 * Puts a book in the player's main hand. The item they held moves to their inventory, or drops at their feet if the
	 * inventory is full.
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
		Player player = ctx.plugin().getServer().getPlayerExact(playerName);
		if (player == null)
		{
			ctx.plugin().logDebug("ERROR: Could not give bible to offline player '" + playerName);
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
		Player player = ctx.plugin().getServer().getPlayerExact(playerName);
		if (player == null)
		{
			ctx.plugin().logDebug("ERROR: Could not give editable bible to offline player '" + playerName);
			return false;
		}
		giveBook(player, bible);

		return true;
	}

	/** Writes the god's default book from its commandments */
	private void initBible(String godName)
	{
		ctx.plugin().logDebug("Creating bible for '" + godName + "' ...");

		HolyBook book = this.repository.getOrCreate(godName);
		List<String> pages = new ArrayList<String>();

		book.setTitle(defaultTitle(godName));
		book.setAuthor(godName);

		LanguageManager language = ctx.languageManager();
		CommandmentService commandments = ctx.commandmentService();

		language.setPlayerName(godName);

		pages.add(language.getLanguageStringForBook(godName, LanguageManager.LANGUAGESTRING.DefaultBibleText1));
		try
		{
			language.setType(language.getMobTypeName(commandments.getUnholyMobTypeForGod(godName)));
			pages.add(language.getLanguageStringForBook(godName, LanguageManager.LANGUAGESTRING.DefaultBibleText2));

			language.setType(language.getItemTypeName(commandments.getHolyFoodTypeForGod(godName)));
			pages.add(language.getLanguageStringForBook(godName, LanguageManager.LANGUAGESTRING.DefaultBibleText3));

			language.setType(language.getItemTypeName(commandments.getUnholyFoodTypeForGod(godName)));
			pages.add(language.getLanguageStringForBook(godName, LanguageManager.LANGUAGESTRING.DefaultBibleText4));

			language.setType(language.getMobTypeName(commandments.getHolyMobTypeForGod(godName)));
			pages.add(language.getLanguageStringForBook(godName, LanguageManager.LANGUAGESTRING.DefaultBibleText5));

			language.setType(language.getMobTypeName(commandments.getHolyMobTypeForGod(godName)));
			pages.add(language.getLanguageStringForBook(godName, LanguageManager.LANGUAGESTRING.DefaultBibleText6));

			language.setType(language.getMobTypeName(commandments.getHolyMobTypeForGod(godName)));
			pages.add(language.getLanguageStringForBook(godName, LanguageManager.LANGUAGESTRING.DefaultBibleText7));

			book.setPages(pages);
		}
		catch (Exception ex)
		{
			ctx.plugin().logDebug(ex.getMessage());
		}

		this.repository.save();
	}

	/**
	 * Makes the book in the priest's main hand the Holy Book of their God. A signed book keeps its title. A book and
	 * quill keeps the current title, and the priest gets a signed copy of the new Holy Book in its place.
	 */
	public boolean setBible(String godName, String priestName)
	{
		Player player = ctx.plugin().getServer().getPlayerExact(priestName);
		if (player == null)
		{
			return false;
		}
		ItemStack item = player.getInventory().getItemInMainHand();
		if ((item.getType() != Material.WRITTEN_BOOK) && (item.getType() != Material.WRITABLE_BOOK))
		{
			return false;
		}

		HolyBookItem book;
		try
		{
			book = new HolyBookItem(ctx.plugin(), item);
		}
		catch (Exception ex)
		{
			ctx.plugin().logDebug("ERROR: Could not set a bible for '" + godName + ": " + ex.getMessage());
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
			title = oldTitle == null ? defaultTitle(godName) : ChatColor.stripColor(oldTitle);
		}

		HolyBook bible = this.repository.getOrCreate(godName);
		bible.setTitle(title);
		bible.setAuthor(player.getName());
		bible.setPages(pages);

		this.repository.save();

		if (item.getType() == Material.WRITABLE_BOOK)
		{
			ItemStack copy = getBible(godName);
			if (copy != null)
			{
				player.getInventory().setItemInMainHand(copy);
			}
		}
		else
		{
			book.setGodName(godName);
		}

		return true;
	}
}
