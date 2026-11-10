package com.dogonfire.gods.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.dogonfire.gods.Gods;

//
// Holy artifacts are items a God gives to a believer. Right-clicking one
// activates its holy power. An artifact keeps its state (power, uses, name)
// in its own persistent data, so it needs no data file.
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

	private record ArtifactItem(Material material, String name)
	{
	}

	private record ArtifactKind(String lore, HolyPowerManager.HolyPower power, ArtifactItem... items)
	{
	}

	private final Random		random	= new Random();

	private final NamespacedKey	powerKey;
	private final NamespacedKey	powerValueKey;
	private final NamespacedKey	usesKey;
	private final NamespacedKey	nameKey;

	private HolyArtifactManager()
	{
		this.powerKey = new NamespacedKey(Gods.instance(), "holy_power");
		this.powerValueKey = new NamespacedKey(Gods.instance(), "holy_power_value");
		this.usesKey = new NamespacedKey(Gods.instance(), "holy_artifact_uses");
		this.nameKey = new NamespacedKey(Gods.instance(), "holy_artifact_name");
	}

	private static ArtifactKind getArtifactKind(GodManager.GodType godType)
	{
		return switch (godType)
		{
		case FROST -> new ArtifactKind("A Holy artifact of Frost", HolyPowerManager.HolyPower.FREEZE, new ArtifactItem(Material.STICK, "Frostwand"), new ArtifactItem(Material.IRON_SWORD, "Frostblade"));
		case LOVE -> new ArtifactKind("A Holy artifact of Love", HolyPowerManager.HolyPower.HEALING, new ArtifactItem(Material.DIAMOND, "Love jewel"), new ArtifactItem(Material.AMETHYST_SHARD, "Heartstone"));
		case EVIL -> new ArtifactKind("An Unholy artifact of Evil", HolyPowerManager.HolyPower.LIGHTNING, new ArtifactItem(Material.IRON_SWORD, "Bloodsword"), new ArtifactItem(Material.IRON_AXE, "Bloodaxe"));
		case SEA -> new ArtifactKind("A Holy artifact of the Sea", HolyPowerManager.HolyPower.ICE, new ArtifactItem(Material.HEART_OF_THE_SEA, "Heart of the sea"), new ArtifactItem(Material.PRISMARINE_SHARD, "Tidestone"));
		case MOON -> new ArtifactKind("A Holy artifact of the Moon", HolyPowerManager.HolyPower.CALLMOON, new ArtifactItem(Material.CLOCK, "Moon dial"), new ArtifactItem(Material.PHANTOM_MEMBRANE, "Night veil"));
		case SUN -> new ArtifactKind("A Holy artifact of the Sun", HolyPowerManager.HolyPower.CALLSUN, new ArtifactItem(Material.BLAZE_ROD, "Sunstaff"), new ArtifactItem(Material.GOLDEN_SWORD, "Sunblade"));
		case THUNDER -> new ArtifactKind("A Holy artifact of Thunder", HolyPowerManager.HolyPower.LIGHTNING_STORM, new ArtifactItem(Material.STICK, "Thunderwand"), new ArtifactItem(Material.GOLDEN_AXE, "Thunderaxe"));
		case PARTY -> new ArtifactKind("A Holy artifact of the PAAARTEY!", HolyPowerManager.HolyPower.FIREWORK, new ArtifactItem(Material.STICK, "Partystick"), new ArtifactItem(Material.FIREWORK_STAR, "Partystar"));
		case WAR -> new ArtifactKind("A Holy artifact of War", HolyPowerManager.HolyPower.FIREBALL, new ArtifactItem(Material.IRON_SWORD, "Warsword"), new ArtifactItem(Material.IRON_AXE, "Waraxe"));
		case WEREWOLVES -> new ArtifactKind("A Holy artifact of the Pack", HolyPowerManager.HolyPower.TAME, new ArtifactItem(Material.RABBIT_FOOT, "Wolfpaw"), new ArtifactItem(Material.IRON_SWORD, "Fangblade"));
		case CREATURES -> new ArtifactKind("A Holy artifact of Creatures", HolyPowerManager.HolyPower.TAME, new ArtifactItem(Material.STICK, "Beastwand"), new ArtifactItem(Material.FEATHER, "Beast feather"));
		case WISDOM -> new ArtifactKind("A Holy artifact of Wisdom", HolyPowerManager.HolyPower.KNOWLEDGE, new ArtifactItem(Material.BOOK, "Knowledge tome"), new ArtifactItem(Material.FEATHER, "Quill"));
		case NATURE -> new ArtifactKind("A Holy artifact of Nature", HolyPowerManager.HolyPower.NATURE, new ArtifactItem(Material.GOLDEN_HOE, "Earthdigger"), new ArtifactItem(Material.STICK, "Druid staff"));
		};
	}

	public Item createHolyArtifact(GodManager.GodType godType, String godName, Location location)
	{
		if (godType == null)
		{
			Gods.instance().log("createHolyArtifact(): " + godName + " has no god type");
			return null;
		}

		ArtifactKind kind = getArtifactKind(godType);
		ArtifactItem artifactItem = kind.items()[this.random.nextInt(kind.items().length)];
		HolyPowerManager.HolyPower holyPower = kind.power();
		int powerValue = 1;
		String itemName = artifactItem.name() + " of " + godName;

		ItemStack item = new ItemStack(artifactItem.material());
		ItemMeta itemMeta = item.getItemMeta();
		if (itemMeta == null)
		{
			Gods.instance().logDebug("createHolyArtifact(): Could not get item meta for " + artifactItem.material());
			return null;
		}

		itemMeta.setDisplayName(ChatColor.GOLD + itemName);

		List<String> lore = new ArrayList<String>();
		lore.add(ChatColor.DARK_PURPLE + kind.lore());
		lore.add(getUsesLine(0));
		lore.add(ChatColor.GREEN + HolyPowerManager.instance().describe(holyPower, powerValue));
		itemMeta.setLore(lore);

		switch (this.random.nextInt(6))
		{
		case 0 -> itemMeta.addEnchant(Enchantment.SHARPNESS, 1 + this.random.nextInt(2), true);
		case 1 -> itemMeta.addEnchant(Enchantment.SMITE, 1 + this.random.nextInt(2), true);
		case 2 -> itemMeta.addEnchant(Enchantment.FIRE_ASPECT, 1 + this.random.nextInt(2), true);
		case 3 -> itemMeta.addEnchant(Enchantment.KNOCKBACK, 1 + this.random.nextInt(2), true);
		case 4 -> itemMeta.addEnchant(Enchantment.EFFICIENCY, 1 + this.random.nextInt(2), true);
		default -> itemMeta.addEnchant(Enchantment.LOOTING, 1 + this.random.nextInt(2), true);
		}

		PersistentDataContainer data = itemMeta.getPersistentDataContainer();
		data.set(this.powerKey, PersistentDataType.STRING, holyPower.name());
		data.set(this.powerValueKey, PersistentDataType.INTEGER, powerValue);
		data.set(this.usesKey, PersistentDataType.INTEGER, 0);
		data.set(this.nameKey, PersistentDataType.STRING, itemName);

		item.setItemMeta(itemMeta);

		return location.getWorld().dropItem(location, item);
	}

	/**
	 * Same as {@link #createHolyArtifact(GodManager.GodType, String, Location)};
	 * the player name is not used.
	 */
	public Item createHolyArtifact(String playerName, GodManager.GodType godType, String godName, Location location)
	{
		return createHolyArtifact(godType, godName, location);
	}

	private static String getUsesLine(int uses)
	{
		return ChatColor.WHITE + "Used " + uses + (uses == 1 ? " time" : " times");
	}

	private static String getArtifactRankName(int uses)
	{
		if (uses < 5)
		{
			return "";
		}
		if (uses < 20)
		{
			return ChatColor.GREEN + "Shiny";
		}
		if (uses < 50)
		{
			return ChatColor.GREEN + "Honorable";
		}
		if (uses < 100)
		{
			return ChatColor.GREEN + "Holy";
		}
		if (uses < 150)
		{
			return ChatColor.GREEN + "Saintly";
		}
		if (uses < 200)
		{
			return ChatColor.BLUE + "Amazing";
		}
		if (uses < 300)
		{
			return ChatColor.BLUE + "Mega";
		}
		if (uses < 500)
		{
			return ChatColor.BLUE + "Awesome";
		}
		if (uses < 1000)
		{
			return ChatColor.DARK_PURPLE + "Epic";
		}
		return ChatColor.GOLD + "Legendary";
	}

	/**
	 * Activates the holy power of the artifact a player right-clicked with.
	 * Each use counts towards the artifact's rank, and wears it like any other
	 * use of a tool.
	 */
	public void handleActivate(String playerName, ItemStack item)
	{
		if (!isHolyArtifact(item))
		{
			return;
		}
		Player player = Gods.instance().getServer().getPlayerExact(playerName);
		HolyPowerManager.PowerDescription holyPower = getHolyPower(item);
		if (player == null || holyPower == null)
		{
			return;
		}

		HolyPowerManager.instance().activatePower(player, holyPower.power(), holyPower.value());

		ItemMeta itemMeta = item.getItemMeta();
		PersistentDataContainer data = itemMeta.getPersistentDataContainer();

		// Tag artifacts from before the persistent data, so they keep working
		// if their lore is changed
		if (!data.has(this.powerKey, PersistentDataType.STRING))
		{
			data.set(this.powerKey, PersistentDataType.STRING, holyPower.power().name());
			data.set(this.powerValueKey, PersistentDataType.INTEGER, holyPower.value());
			data.set(this.nameKey, PersistentDataType.STRING, ChatColor.stripColor(itemMeta.getDisplayName()));
		}

		int oldUses = data.getOrDefault(this.usesKey, PersistentDataType.INTEGER, 0);
		int uses = oldUses + 1;
		data.set(this.usesKey, PersistentDataType.INTEGER, uses);

		List<String> lore = itemMeta.getLore();
		if (lore != null && lore.size() >= 2)
		{
			lore.set(1, getUsesLine(uses));
			itemMeta.setLore(lore);
		}

		String rankName = getArtifactRankName(uses);
		if (!rankName.equals(getArtifactRankName(oldUses)))
		{
			String baseName = data.getOrDefault(this.nameKey, PersistentDataType.STRING, ChatColor.stripColor(itemMeta.getDisplayName()));
			itemMeta.setDisplayName(rankName + " " + ChatColor.GOLD + baseName);
			Gods.instance().getServer().broadcastMessage(ChatColor.AQUA + player.getName() + "'s " + ChatColor.WHITE + baseName + ChatColor.AQUA + " is now " + rankName);
		}

		boolean broken = false;
		if (itemMeta instanceof Damageable damageable && !itemMeta.isUnbreakable())
		{
			int maxDamage = damageable.hasMaxDamage() ? damageable.getMaxDamage() : item.getType().getMaxDurability();
			if (maxDamage > 0)
			{
				int damage = damageable.getDamage() + 1;
				if (damage >= maxDamage)
				{
					broken = true;
				}
				else
				{
					damageable.setDamage(damage);
				}
			}
		}

		item.setItemMeta(itemMeta);

		if (broken)
		{
			item.setAmount(item.getAmount() - 1);
			player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.0F);
		}
	}

	/**
	 * Nothing to do: artifacts keep their state on the item. Kept for the
	 * existing listener call.
	 */
	public void handleDeath(String killerName, String godName, ItemStack itemInHand)
	{
	}

	/**
	 * Nothing to do: artifacts keep their state on the item. Kept for the
	 * existing listener call.
	 */
	public void handleDrop(String playerName, Item item, Location pickupLocation)
	{
	}

	/**
	 * True if the online player with this exact name carries a Holy artifact.
	 */
	public boolean hasHolyArtifact(String playerName)
	{
		Player player = Gods.instance().getServer().getPlayerExact(playerName);
		return player != null && hasHolyArtifact(player);
	}

	/**
	 * True if the player carries a Holy artifact anywhere in their inventory.
	 */
	public boolean hasHolyArtifact(Player player)
	{
		for (ItemStack item : player.getInventory().getContents())
		{
			if (isHolyArtifact(item))
			{
				return true;
			}
		}
		return false;
	}

	public boolean isHolyArtifact(ItemStack item)
	{
		if (item == null || !item.hasItemMeta())
		{
			return false;
		}
		ItemMeta itemMeta = item.getItemMeta();
		if (itemMeta.getPersistentDataContainer().has(this.powerKey, PersistentDataType.STRING))
		{
			return true;
		}
		return getLegacyHolyPower(itemMeta) != null;
	}

	/**
	 * The power of an artifact, from its persistent data or, for artifacts
	 * made by older versions, from its lore. Null if the item is no artifact.
	 */
	private HolyPowerManager.PowerDescription getHolyPower(ItemStack item)
	{
		ItemMeta itemMeta = item.getItemMeta();
		PersistentDataContainer data = itemMeta.getPersistentDataContainer();
		String powerName = data.get(this.powerKey, PersistentDataType.STRING);
		if (powerName == null)
		{
			return getLegacyHolyPower(itemMeta);
		}
		try
		{
			HolyPowerManager.HolyPower power = HolyPowerManager.HolyPower.valueOf(powerName);
			return new HolyPowerManager.PowerDescription(power, data.getOrDefault(this.powerValueKey, PersistentDataType.INTEGER, 1));
		}
		catch (IllegalArgumentException ex)
		{
			Gods.instance().logDebug("Unknown holy power '" + powerName + "' on a Holy artifact");
			return null;
		}
	}

	/**
	 * Reads the power of an artifact made before artifacts were tagged: its
	 * lore starts with the artifact line and its third line describes a
	 * holy power. Null for any other item.
	 */
	private HolyPowerManager.PowerDescription getLegacyHolyPower(ItemMeta itemMeta)
	{
		if (!itemMeta.hasLore())
		{
			return null;
		}
		List<String> lore = itemMeta.getLore();
		if (lore.size() < 3)
		{
			return null;
		}
		String firstLine = ChatColor.stripColor(lore.get(0));
		if (!firstLine.startsWith("A Holy artifact") && !firstLine.startsWith("An Unholy artifact"))
		{
			return null;
		}
		return HolyPowerManager.instance().getHolyPowerFromDescription(ChatColor.stripColor(lore.get(2)));
	}

	/**
	 * Nothing to load: artifacts keep their state on the item.
	 */
	public void load()
	{
	}
}
