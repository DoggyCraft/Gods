package com.dogonfire.gods.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.HolyBook;
import com.dogonfire.gods.config.GodsConfiguration;


public class InstructionBookManager
{
	private static InstructionBookManager instance;

	public static InstructionBookManager instance()
	{
		if (GodsConfiguration.instance().isBiblesEnabled() && instance == null)
			instance = new InstructionBookManager();
		return instance;
	}

	private FileConfiguration	instructionsConfig		= null;
	private File				instructionsConfigFile	= null;

	private InstructionBookManager()
	{
	}

	public ItemStack getInstructionBook(String godName)
	{
		List<String> pages = this.instructionsConfig.getStringList(godName + ".Pages");
		if (pages.size() == 0)
		{
			initBible(godName);
			pages = this.instructionsConfig.getStringList(godName + ".Pages");
		}
		
		ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
		
		try
		{
			HolyBook b = new HolyBook(book);

			b.setTitle(this.instructionsConfig.getString(godName + ".Title"));

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

	public boolean giveInstructionBook(String godName, String playerName)
	{
		ItemStack bible = getInstructionBook(godName);
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
		
	public boolean giveInstructions(String playerName)
	{
		ItemStack instructions = null;//getInstructions(godName);
		if (instructions == null)
		{
			return false;
		}
		
		Player player = Gods.instance().getServer().getPlayer(playerName);
		
		if (player == null)
		{
			Gods.instance().logDebug("ERROR: Could not give instructions to offline player '" + playerName);
			return false;
		}
		int amount = player.getPlayer().getInventory().getItemInMainHand().getAmount();
		ItemStack[] itemStack = { player.getPlayer().getInventory().getItemInMainHand() };
		itemStack[0].setAmount(amount);
		player.getInventory().addItem(itemStack);

		player.getInventory().setItemInMainHand(instructions);

		return true;
	}

	public void handleQuestCompleted(String godName, QuestManager.QUESTTYPE type, String playerName)
	{
	}

	private void initBible(String godName)
	{
		Gods.instance().logDebug("Creating bible for '" + godName + "' ...");

		List<String> pages = new ArrayList<String>();

		this.instructionsConfig.set(godName + ".Title", "Holy Book of " + godName);

		this.instructionsConfig.set(godName + ".Author", godName);

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

			this.instructionsConfig.set(godName + ".Pages", pages);
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
		if (this.instructionsConfigFile == null)
		{
			this.instructionsConfigFile = new File(Gods.instance().getDataFolder(), "bibles.yml");
		}
		this.instructionsConfig = YamlConfiguration.loadConfiguration(this.instructionsConfigFile);

		Gods.instance().log("Loaded " + this.instructionsConfig.getKeys(false).size() + " bibles.");
	}

	public void save()
	{
		if ((this.instructionsConfig == null) || (this.instructionsConfigFile == null))
		{
			return;
		}
		try
		{
			this.instructionsConfig.save(this.instructionsConfigFile);
		}
		catch (Exception ex)
		{
			Gods.instance().log("Could not save config to " + this.instructionsConfigFile.getName() + ": " + ex.getMessage());
		}
	}

	public boolean setBible(String godName, String priestName)
	{
		Player player = Gods.instance().getServer().getPlayer(priestName);
		return setBible(godName, player.getUniqueId());
	}

	public boolean setBible(String godName, UUID priestId)
	{
		Player player = Gods.instance().getServer().getPlayer(priestId);
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
		this.instructionsConfig.set(godName + ".Title", b.getTitle());
		this.instructionsConfig.set(godName + ".Author", priestName);
		this.instructionsConfig.set(godName + ".Pages", b.getPages());

		save();
	}

	public void setProphecyPages(String godName, List<String> prophecyPages)
	{
		List<String> pages = this.instructionsConfig.getStringList(godName + ".Pages");
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
		this.instructionsConfig.set(godName + ".Pages", newPages);

		save();
	}
}
