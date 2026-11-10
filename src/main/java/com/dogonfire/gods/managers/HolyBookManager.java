package com.dogonfire.gods.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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

	public ItemStack getBible(String godName)
	{
		List<String> pages = this.biblesConfig.getStringList(godName + ".Pages");
		if (pages.size() == 0)
		{
			initBible(godName);
			pages = this.biblesConfig.getStringList(godName + ".Pages");
		}
		
		ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
		
		try
		{
			HolyBook b = new HolyBook(book);

			b.setTitle(this.biblesConfig.getString(godName + ".Title"));

			b.setAuthor(godName);
			b.setPages(pages);

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
		List<String> pages = this.biblesConfig.getStringList(godName + ".Pages");
		if (pages.size() == 0)
		{
			initBible(godName);
			pages = this.biblesConfig.getStringList(godName + ".Pages");
		}
		ItemStack book = new ItemStack(Material.WRITABLE_BOOK);
		try
		{
			HolyBook b = new HolyBook(book);

			b.setTitle(this.biblesConfig.getString(godName + ".Title"));
			b.setAuthor(godName);
			for (int i = 0; i < pages.size(); i++)
			{
				pages.set(i, pages.get(i));
			}
			b.setPages(pages);

			return b.getItem();
		}
		catch (Exception ex)
		{
			Gods.instance().logDebug("ERROR: Could not instance a bible for '" + godName + ": " + ex.getMessage());
		}
		return null;
	}

	public String getGodForBible(ItemStack book)
	{
		try
		{
			HolyBook bible = new HolyBook(book);
			for (String god : this.biblesConfig.getKeys(false))
			{
				if (god.equals(bible.getAuthor()))
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

	public boolean giveBible(String godName, String playerName)
	{
		ItemStack bible = getBible(godName);
		if (bible == null)
		{
			return false;
		}
		Player player = Gods.instance().getServer().getPlayer(playerName);
		if (player == null)
		{
			Gods.instance().logDebug("ERROR: Could not give bible to offline player '" + playerName);
			return false;
		}
		int amount = player.getPlayer().getInventory().getItemInMainHand().getAmount();
		ItemStack[] itemStack = { player.getPlayer().getInventory().getItemInMainHand() };
		itemStack[0].setAmount(amount);
		player.getInventory().addItem(itemStack);

		player.getInventory().setItemInMainHand(bible);

		return true;
	}

	public boolean giveEditBible(String godName, String playerName)
	{
		ItemStack bible = getEditBible(godName);
		if (bible == null)
		{
			return false;
		}
		Player player = Gods.instance().getServer().getPlayer(playerName);
		if (player == null)
		{
			Gods.instance().logDebug("ERROR: Could not give editable bible to offline player '" + playerName);
			return false;
		}
		int amount = player.getPlayer().getInventory().getItemInMainHand().getAmount();
		ItemStack[] itemStack = { player.getPlayer().getInventory().getItemInMainHand() };
		itemStack[0].setAmount(amount);
		player.getInventory().addItem(itemStack);

		player.getInventory().setItemInMainHand(bible);

		return true;
	}
		
	public void handleQuestCompleted(String godName, QuestManager.QUESTTYPE type, String playerName)
	{
	}

	private void initBible(String godName)
	{
		Gods.instance().logDebug("Creating bible for '" + godName + "' ...");

		List<String> pages = new ArrayList<String>();

		this.biblesConfig.set(godName + ".Title", "Holy Book of " + godName);

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

	public boolean setBible(String godName, String priestName)
	{
		Player player = Gods.instance().getServer().getPlayer(priestName);
		if (player == null)
		{
			return false;
		}
		ItemStack item = player.getInventory().getItemInMainHand();
		if ((item == null) || ((item.getType() != Material.WRITTEN_BOOK) && (item.getType() != Material.WRITABLE_BOOK)))
		{
			return false;
		}
		setBible(godName, player.getName(), item);

		item.setType(Material.WRITTEN_BOOK);

		return true;
	}

	private void setBible(String godName, String priestName, ItemStack book)
	{
		HolyBook b = null;
		try
		{
			b = new HolyBook(book);
		}
		catch (Exception ex)
		{
			Gods.instance().logDebug("ERROR: Could not set a bible for '" + godName + ": " + ex.getMessage());
		}
		this.biblesConfig.set(godName + ".Title", b.getTitle());
		this.biblesConfig.set(godName + ".Author", priestName);
		this.biblesConfig.set(godName + ".Pages", b.getPages());

		save();
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
