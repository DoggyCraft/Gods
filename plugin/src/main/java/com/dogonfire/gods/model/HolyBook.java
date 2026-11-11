package com.dogonfire.gods.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A god's holy book (bible): one section per god name in bibles.yml with {@code Title}, {@code Author} (the god, or the
 * priest who set it) and {@code Pages}. A plain data object: {@code HolyBookService} changes it.
 */
public final class HolyBook
{
	private final String		godName;
	private String				title;
	private String				author;
	private List<String>		pages;

	/** Keys of the section this version doesn't know, written back unchanged */
	private Map<String, Object>	extras	= new LinkedHashMap<String, Object>();

	public HolyBook(String godName)
	{
		this.godName = godName;
	}

	public String godName()
	{
		return this.godName;
	}

	/** Null if not stored */
	public String title()
	{
		return this.title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	/** Null if not stored */
	public String author()
	{
		return this.author;
	}

	public void setAuthor(String author)
	{
		this.author = author;
	}

	/** Null if not stored */
	public List<String> pages()
	{
		return this.pages;
	}

	/** The pages as a new list (empty if none are stored) */
	public List<String> pagesOrEmpty()
	{
		return (this.pages != null) ? new ArrayList<String>(this.pages) : new ArrayList<String>();
	}

	public void setPages(List<String> pages)
	{
		this.extras.remove("Pages");
		this.pages = (pages != null) ? new ArrayList<String>(pages) : null;
	}

	public Map<String, Object> extras()
	{
		return this.extras;
	}

	public void setExtras(Map<String, Object> extras)
	{
		this.extras = extras;
	}
}
