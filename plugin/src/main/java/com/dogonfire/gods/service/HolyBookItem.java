package com.dogonfire.gods.service;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.WritableBookMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * A written book or a book and quill, as a holy book. Only written books have a title and an author. A holy book is
 * recognized by its {@code gods:holy_book_god} tag; books from before the tag only by their author
 * ({@link HolyBookService#getGodForBible}).
 */
final class HolyBookItem
{
	// Longest title a written book can have
	private static final int	MAX_TITLE_LENGTH	= 32;

	private final ItemStack		s;
	private final NamespacedKey	godKey;

	HolyBookItem(Plugin plugin, ItemStack itemStack) throws Exception
	{
		if ((itemStack.getType() != Material.WRITTEN_BOOK) && (itemStack.getType() != Material.WRITABLE_BOOK))
		{
			throw new Exception("HolyBookItem: the item is not Material.WRITTEN_BOOK or Material.WRITABLE_BOOK");
		}
		this.s = itemStack;
		this.godKey = new NamespacedKey(plugin, "holy_book_god");
	}

	public String getAuthor()
	{
		ItemMeta meta = this.s.getItemMeta();
		return meta instanceof BookMeta bookMeta ? bookMeta.getAuthor() : null;
	}

	/**
	 * The God this book is the Holy Book of, or null if it is not tagged as one.
	 */
	public String getGodName()
	{
		return this.s.getItemMeta().getPersistentDataContainer().get(this.godKey, PersistentDataType.STRING);
	}

	public ItemStack getItem()
	{
		return this.s;
	}

	public List<String> getPages()
	{
		return ((WritableBookMeta) this.s.getItemMeta()).getPages();
	}

	public String getTitle()
	{
		ItemMeta meta = this.s.getItemMeta();
		return meta instanceof BookMeta bookMeta ? bookMeta.getTitle() : null;
	}

	public void setAuthor(String author)
	{
		if (this.s.getItemMeta() instanceof BookMeta bookMeta)
		{
			bookMeta.setAuthor(ChatColor.GOLD + author);
			this.s.setItemMeta(bookMeta);
		}
	}

	/**
	 * Tags this book as the Holy Book of a God.
	 */
	public void setGodName(String godName)
	{
		ItemMeta meta = this.s.getItemMeta();
		meta.getPersistentDataContainer().set(this.godKey, PersistentDataType.STRING, godName);
		this.s.setItemMeta(meta);
	}

	public void setPages(List<String> pages)
	{
		WritableBookMeta bookMeta = (WritableBookMeta) this.s.getItemMeta();
		bookMeta.setPages(pages);
		this.s.setItemMeta(bookMeta);
	}

	public void setTitle(String name)
	{
		if (this.s.getItemMeta() instanceof BookMeta bookMeta)
		{
			String title = ChatColor.GOLD + name;
			if (title.length() > MAX_TITLE_LENGTH)
			{
				title = name.length() > MAX_TITLE_LENGTH ? name.substring(0, MAX_TITLE_LENGTH) : name;
			}
			bookMeta.setTitle(title);
			this.s.setItemMeta(bookMeta);
		}
	}
}
