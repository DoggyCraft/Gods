package com.dogonfire.gods.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.HolyArtifact;

//
// Artifacts are given as quest reward items. 
// Mostly vanity, but gives slight bonus when worn as a set. 
// Also enables holy power ability as a set
//
public class HolyArtifactManager
{
	private static HolyArtifactManager instance;

	public static HolyArtifactManager instance()
	{
		if (instance == null)
			instance = new HolyArtifactManager();
		return instance;
	}

	private FileConfiguration	holyArtifactsConfig		= null;
	private File				holyArtifactsConfigFile	= null;
	private Random				random					= new Random();

	private HolyArtifactManager()
	{
	}

	public Item createHolyArtifact(GodManager.GodType godType, String godName, Location location)
	{
		Material itemType = Material.AIR;
		String lorePage = "A Holy artifact given by " + godName;
		HolyPowerManager.HolyPower holyPower = HolyPowerManager.HolyPower.values()[this.random.nextInt(HolyPowerManager.HolyPower.values().length)];
		switch (godType)
		{
		case CREATURES:
			lorePage = ChatColor.DARK_PURPLE + "A Holy artifact of frost!";
			holyPower = HolyPowerManager.HolyPower.FREEZE;
			switch (this.random.nextInt(1))
			{
			case 0:
				itemType = Material.STICK;
			}
			break;
		case EVIL:
			lorePage = ChatColor.DARK_PURPLE + "A Holy artifact of LOVE!";
			holyPower = HolyPowerManager.HolyPower.HEALING;
			switch (this.random.nextInt(3))
			{
			case 0:
				itemType = Material.POPPY;
				break;
			case 1:
				itemType = Material.DIAMOND;
				break;
			case 2:
				itemType = Material.COOKIE;
			}
			break;
		case SEA:
			lorePage = ChatColor.DARK_PURPLE + "A Holy artifact of the PAAARTEY!";
			holyPower = HolyPowerManager.HolyPower.FIREWORK;
			switch (this.random.nextInt(3))
			{
			case 0:
				itemType = Material.GLASS_BOTTLE;
				break;
			case 1:
				itemType = Material.STICK;
				break;
			case 2:
				itemType = Material.CAKE;
			}
			break;
		case WISDOM:
			lorePage = ChatColor.DARK_PURPLE + "A Holy artifact of Nature";
			holyPower = HolyPowerManager.HolyPower.NATURE;
			switch (this.random.nextInt(5))
			{
			case 0:
				itemType = Material.STICK;
				break;
			case 1:
				itemType = Material.GOLDEN_HOE;
				break;
			case 2:
				itemType = Material.WHEAT_SEEDS;
				break;
			case 3:
				itemType = Material.PUMPKIN_SEEDS;
				break;
			case 4:
				itemType = Material.MELON_SEEDS;
			}
			break;
		case WEREWOLVES:
			lorePage = ChatColor.DARK_PURPLE + "A Holy artifact of the Wisdom";
			holyPower = HolyPowerManager.HolyPower.KNOWLEDGE;
			switch (this.random.nextInt(3))
			{
			case 0:
				itemType = Material.BOOK;
				break;
			case 1:
				itemType = Material.ENCHANTED_BOOK;
				break;
			case 2:
				itemType = Material.ENCHANTING_TABLE;
			}
			break;
		case MOON:
			lorePage = ChatColor.DARK_PURPLE + "A Holy artifact of the Moon";
			holyPower = HolyPowerManager.HolyPower.CALLMOON;
			switch (this.random.nextInt(2))
			{
			case 0:
				itemType = Material.BUCKET;
			}
			break;
		case NATURE:
			lorePage = ChatColor.DARK_PURPLE + "A Holy artifact of Nature";
			holyPower = HolyPowerManager.HolyPower.CALLSUN;
			switch (this.random.nextInt(2))
			{
			case 0:
				itemType = Material.BUCKET;
			}
			break;
		case PARTY:
			lorePage = ChatColor.DARK_PURPLE + "A Holy artifact of Thunder";
			holyPower = HolyPowerManager.HolyPower.LIGHTNING_STORM;
			switch (this.random.nextInt(2))
			{
			case 0:
				itemType = Material.STICK;
				break;
			case 1:
				itemType = Material.GOLDEN_AXE;
			}
			break;
		case WAR:
			lorePage = ChatColor.DARK_PURPLE + "A Holy artifact of Creatures";
			holyPower = HolyPowerManager.HolyPower.TAME;
			switch (this.random.nextInt(1))
			{
			case 0:
				itemType = Material.SHEARS;
				break;
			//case 1:
			//	itemType = Material.le;
			}
			break;
		case SUN:
			lorePage = ChatColor.DARK_PURPLE + "A Holy artifact of WAR";
			holyPower = HolyPowerManager.HolyPower.FIREBALL;
			switch (this.random.nextInt(2))
			{
			case 0:
				itemType = Material.GOLDEN_SWORD;
				break;
			case 1:
				itemType = Material.GOLDEN_AXE;
			}
			break;
		case FROST:
			lorePage = ChatColor.DARK_PURPLE + "An Unholy artifact of EVIL";
			holyPower = HolyPowerManager.HolyPower.FIREBALL;
			switch (this.random.nextInt(2))
			{
			case 0:
				itemType = Material.GOLDEN_SWORD;
				break;
			case 1:
				itemType = Material.GOLDEN_AXE;
			}
			break;
		case LOVE:
		case THUNDER:
		default:
			Gods.instance().log("createHolyArtifact() : Unknown godType " + godType);
			return null;
		}
		int powerValue = 1;

		String itemName = getHolyArtifactName(itemType, holyPower, godName, godType);

		ItemStack item = new ItemStack(itemType);

		ItemMeta itemMeta = null;

		try
		{
			itemMeta = item.getItemMeta();
			itemMeta.setDisplayName(ChatColor.GOLD + itemName);
		}
		catch (Exception ex)
		{
			Gods.instance().logDebug("createHolyArtifact(): Could not get or set item meta");
		}
		try
		{
			if (lorePage != null)
			{
				List<String> lorePages = new ArrayList<String>();
				lorePages.add(lorePage);
				lorePages.add(ChatColor.WHITE + "Damage: 12");
				lorePages.add(ChatColor.GREEN + HolyPowerManager.instance().describe(holyPower, powerValue));

				itemMeta.setLore(lorePages);
			}
		}
		catch (Exception ex)
		{
			Gods.instance().logDebug("createHolyArtifact(): Could not set meta lore pages");
			return null;
		}
		
		switch (this.random.nextInt(6))
		{
		case 0:
			itemMeta.addEnchant(Enchantment.SHARPNESS, 1 + this.random.nextInt(2), true);
			break;
		case 1:
			itemMeta.addEnchant(Enchantment.SMITE, 1 + this.random.nextInt(2), true);
			break;
		case 2:
			itemMeta.addEnchant(Enchantment.FIRE_ASPECT, 1 + this.random.nextInt(2), true);
			break;
		case 3:
			itemMeta.addEnchant(Enchantment.KNOCKBACK, 1 + this.random.nextInt(2), true);
			break;
		case 4:
			itemMeta.addEnchant(Enchantment.EFFICIENCY, 1 + this.random.nextInt(2), true);
			break;
		case 5:
			itemMeta.addEnchant(Enchantment.LOOTING, 1 + this.random.nextInt(2), true);
		}
		item.setItemMeta(itemMeta);

		Item artifactItem = location.getWorld().dropItem(location, item);

		resetHolyArtifactKills(artifactItem, godName, godType, 0, 0);

		return artifactItem;
	}

	public Item createHolyArtifact(String playerName, GodManager.GodType godType, String godName, Location location)
	{
		Item item = createHolyArtifact(godType, godName, location);

		return item;
	}

	public String getArtifactRankName(int kills)
	{
		if (kills < 5)
		{
			return "";
		}
		if (kills < 20)
		{
			return ChatColor.GREEN + "Shiny";
		}
		if (kills < 50)
		{
			return ChatColor.GREEN + "Honorable";
		}
		if (kills < 100)
		{
			return ChatColor.GREEN + "Holy";
		}
		if (kills < 150)
		{
			return ChatColor.GREEN + "Saintly";
		}
		if (kills < 200)
		{
			return ChatColor.BLUE + "Amazing";
		}
		if (kills < 300)
		{
			return ChatColor.BLUE + "Mega";
		}
		if (kills < 500)
		{
			return ChatColor.GREEN + "Awesome";
		}
		if (kills < 1000)
		{
			return ChatColor.DARK_PURPLE + "Epic";
		}
		return ChatColor.GOLD + "Legendary";
	}

	public String getHolyArtifactName(Material itemType, HolyPowerManager.HolyPower holyPower, String godName, GodManager.GodType godType)
	{
		String itemName = "Holy Artifact of " + godName;
		if (godType == null)
		{
			return itemName;
		}
		switch (godType)
		{
		case EVIL:
			switch (itemType)
			{
			case CHAINMAIL_BOOTS:
				itemName = "Love rose of " + godName;
				break;
			case LEATHER_BOOTS:
				itemName = "Jewel of " + godName;
				break;
			case SHEARS:
				itemName = "Friendship cookie of " + godName;
				break;
			default:
				break;
			}
			break;
		case SEA:
			switch (itemType)
			{
//			case STAINED_CLAY:
//				itemName = "Vodka of " + godName;
//				break;
			case MELON_STEM:
				itemName = "Partystick of " + godName;
				break;
			case SANDSTONE_STAIRS:
				itemName = "Partycake of " + godName;
				break;
			default:
				break;
			}
			break;
		case WISDOM:
			switch (itemType)
			{
//			case NETHER_FENCE:
//				itemName = "Earthdigger of " + godName;
//				break;
			case BAKED_POTATO:
				itemName = "Seed of " + godName;
				break;
//			case NETHER_STALK:
//				itemName = "Seed of " + godName;
//				break;
			case SLIME_BALL:
				itemName = "Melon seeds of " + godName;
				break;
//			case SKULL_ITEM:
//				itemName = "Pumpkin seeds of " + godName;
			default:
				break;
			}
			break;
		case WEREWOLVES:
			switch (itemType)
			{
//			case REDSTONE_COMPARATOR_OFF:
//				itemName = "Knowledge tome of " + godName;
//				break;
			case GLASS:
				itemName = "Enchantment table of " + godName;
				break;
//			case WALL_SIGN:
//				itemName = "Enchantment book of " + godName;
			default:
				break;
			}
			break;
		case MOON:
			switch (itemType)
			{
			default:
				break;
//			case RAW_CHICKEN:
//				itemName = "Moonbucket of " + godName;
			}
			break;
		case NATURE:
			switch (itemType)
			{
			default:
				break;
//			case RAW_CHICKEN: itemName = "Sunbucket of " + godName;
			}
			break;
		case PARTY:
			switch (itemType)
			{
			case MOSSY_COBBLESTONE:
				itemName = "Thunderaxe of " + godName;
				break;
			case MELON_STEM:
				itemName = "Thunderwand of " + godName;
				break;
			default:
				break;
			}
			break;
		case WAR:
			switch (itemType)
			{
//			case SIGN_POST:	itemName = "Shears of " + godName;	break;
			case WOODEN_AXE: itemName = "Leash of " + godName;
			default:
				break;
			}
			break;
		case SUN:
			switch (itemType)
			{
//			case MOB_SPAWNER: itemName = "Warsword of " + godName; break;
			case MOSSY_COBBLESTONE:
				itemName = "Waraxe of " + godName;
				break;
			default:
				break;
			}
			break;
		case FROST:
			switch (itemType)
			{
//			case MOB_SPAWNER: itemName = "Bloodsword of " + godName; break;
			case MOSSY_COBBLESTONE:
				itemName = "Bloodaxe of " + godName;
				break;
			default:
				break;
			}
			break;
		case CREATURES:
			switch (itemType)
			{
			case MELON_STEM:
				itemName = "Frostwand of " + godName;
				break;
			default:
				break;
			}
			break;
		default:
			break;
		}
		return itemName;
	}

	public int getHolyArtifactUsed(String playerName)
	{
		return this.holyArtifactsConfig.getInt(playerName + ".Used");
	}

	public int getNumberOfHolyArtifacts()
	{
		ConfigurationSection configSection = this.holyArtifactsConfig.getConfigurationSection("Player");
		if (configSection != null)
		{
			Gods.instance().logDebug("NumberOfArtifacts = " + configSection.getKeys(false).size());
			return configSection.getKeys(false).size();
		}
		return 0;
	}

	public void handleActivate(String playerName, ItemStack item)
	{
		if (!isHolyArtifact(item))
		{
			return;
		}
		HolyPowerManager.HolyPower holyPower = HolyPowerManager.instance().getHolyPowerFromDescription(item.getItemMeta().getLore().get(2).substring(2));

		Player player = Gods.instance().getServer().getPlayer(playerName);
		if (player == null)
		{
			return;
		}
		HolyPowerManager.instance().activatePower(player, holyPower, 1);
		

		// item.setDurability((short) (item.getDurability() - 1));
		ItemMeta im = item.getItemMeta();

		Damageable damage = (Damageable) im;
		damage.setDamage(damage.getDamage() - 1);
		
		item.setItemMeta(im);
	}

	public float handleDamage(String playerName, Entity targetEntity, ItemStack itemInHand, String godName)
	{
		if (itemInHand.getAmount() == 0)
		{
			return 1.0F;
		}
		HolyArtifact item = new HolyArtifact(itemInHand);
		if ((targetEntity.getType() == EntityType.GIANT) || (targetEntity.getType() == EntityType.ENDER_DRAGON))
		{
			if (!item.isHolyArtifact())
			{
				return 0.0F;
			}
			return 1.0F;
		}
		if (!item.isHolyArtifact())
		{
			return 1.0F;
		}
		Gods.instance().logDebug("Handling Holy Artifact damage for " + playerName);

		// itemInHand.setDurability((short) -2);
		ItemMeta im = itemInHand.getItemMeta();

		Damageable damage = (Damageable) im;
		damage.setDamage(damage.getDamage() - 2);
		
		itemInHand.setItemMeta(im);

		int used = getHolyArtifactUsed(playerName);

		Gods.instance().logDebug("Holy Artifact doing " + (1.0F + used / 25.0F) + " damage");

		return 1.0F + used / 25.0F;
	}

	public void handleDeath(String killerName, String godName, ItemStack itemInHand)
	{
		if (itemInHand.getAmount() > 0)
		{
			HolyArtifact item = new HolyArtifact(itemInHand);

			item.isHolyArtifact();
		}
	}

	public void handleDrop(String playerName, Item item, Location pickupLocation)
	{
		if (!isHolyArtifact(item.getItemStack()))
		{
			return;
		}
		Gods.instance().logDebug(playerName + " dropped up a Holy artifact");

		save();
	}

	public boolean hasHolyArtifact(String playerName)
	{
		return this.holyArtifactsConfig.getString("Player." + playerName + ".PowerType") != null;
	}

	public boolean hasHolyArtifactBlessing(String playerName)
	{
		return this.holyArtifactsConfig.get("Player." + playerName + ".PowerType") != null;
	}

	public boolean isHolyArtifact(ItemStack item)
	{
		if (!item.hasItemMeta())
		{
			return false;
		}
		if (!item.getItemMeta().hasLore())
		{
			return false;
		}
		ItemMeta itemMeta = item.getItemMeta();
		if (itemMeta.getLore().size() < 3)
		{
			return false;
		}
		return true;
	}

	public boolean isNewItemRank(int oldKills)
	{
		if (oldKills == 5)
		{
			return true;
		}
		if (oldKills == 20)
		{
			return true;
		}
		if (oldKills == 50)
		{
			return true;
		}
		if (oldKills == 100)
		{
			return true;
		}
		if (oldKills == 150)
		{
			return true;
		}
		if (oldKills == 200)
		{
			return true;
		}
		if (oldKills == 300)
		{
			return true;
		}
		if (oldKills == 500)
		{
			return true;
		}
		if (oldKills == 1000)
		{
			return true;
		}
		return false;
	}

	public void load()
	{
		if (this.holyArtifactsConfigFile == null)
		{
			this.holyArtifactsConfigFile = new File(Gods.instance().getDataFolder(), "holyartifacts.yml");
		}
		this.holyArtifactsConfig = YamlConfiguration.loadConfiguration(this.holyArtifactsConfigFile);

		Gods.instance().log("Loaded " + this.holyArtifactsConfig.getKeys(false).size() + " holy artifacts.");
	}

	public void resetHolyArtifactKills(Item item, String godName, GodManager.GodType godType, int oldKills, int kills)
	{
		ItemMeta itemMeta = item.getItemStack().getItemMeta();
		List<String> lore = itemMeta.getLore();

		lore.set(1, ChatColor.WHITE + "Kills: " + kills);

		itemMeta.setLore(lore);

		item.getItemStack().setItemMeta(itemMeta);

		this.holyArtifactsConfig.set("Location." + item.getEntityId() + ".Kills", Integer.valueOf(kills));

		save();
	}

	public void save()
	{
		if ((this.holyArtifactsConfig == null) || (this.holyArtifactsConfigFile == null))
		{
			return;
		}
		try
		{
			this.holyArtifactsConfig.save(this.holyArtifactsConfigFile);
		}
		catch (Exception ex)
		{
			Gods.instance().log("Could not save config to " + this.holyArtifactsConfigFile.getName() + ": " + ex.getMessage());
		}
	}

	public void updateStats(ItemStack item, String playerName, String godName, GodManager.GodType godType, int oldUses, int uses)
	{
		ItemMeta itemMeta = item.getItemMeta();
		List<String> lore = itemMeta.getLore();

		lore.set(1, ChatColor.WHITE + "Used " + uses + " times");

		itemMeta.setLore(lore);

		item.setItemMeta(itemMeta);

		HolyPowerManager.HolyPower holyPower = HolyPowerManager.HolyPower.valueOf(this.holyArtifactsConfig.getString("Player." + playerName + ".PowerType"));
		if ((uses != oldUses) && (isNewItemRank(oldUses)))
		{
			String artifactName = getArtifactRankName(uses) + " " + ChatColor.GOLD + getHolyArtifactName(item.getType(), holyPower, godName, godType);

			itemMeta.setDisplayName(artifactName);
			item.setItemMeta(itemMeta);

			Gods.instance().getServer().broadcastMessage(ChatColor.AQUA + playerName + "'s " + ChatColor.WHITE + getHolyArtifactName(item.getType(), holyPower, godName, godType) + ChatColor.AQUA + " is now " + getArtifactRankName(uses));
		}
	}
}
