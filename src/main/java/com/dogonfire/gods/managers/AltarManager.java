package com.dogonfire.gods.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.GodManager.GodType;

public class AltarManager
{
	private static AltarManager instance;

	public static AltarManager instance()
	{
		if (instance == null)
			instance = new AltarManager();
		return instance;
	}

	private Random							random			= new Random();
	private Map<Integer, UUID>			droppedItems	= new HashMap<Integer, UUID>();
	private Map<Material, List<GodType>>	altarBlockTypes	= new HashMap<Material, List<GodType>>();

	private AltarManager()
	{
	}

	public void addDroppedItem(int entityID, UUID playerId)
	{
		this.droppedItems.put(Integer.valueOf(entityID), playerId);
	}

	public void clearDroppedItems()
	{
		Gods.instance().logDebug("Cleared " + this.droppedItems.size() + " dropped items");
		this.droppedItems.clear();
	}

	/** Any wooden wall sign (oak, spruce, ..., bamboo, crimson, warped) can be an altar sign. */
	public static boolean isWallSign(Block block)
	{
		return block != null && Tag.WALL_SIGNS.isTagged(block.getType());
	}

	/** The sign item that a wall sign block drops, e.g. SPRUCE_SIGN for SPRUCE_WALL_SIGN. */
	public static ItemStack getSignItem(Material wallSignType)
	{
		Material item = Material.getMaterial(wallSignType.name().replace("_WALL_SIGN", "_SIGN"));

		return new ItemStack((item != null && item.isItem()) ? item : Material.OAK_SIGN, 1);
	}

	/** The lines on the front of a sign block (the side altars are written on), or null if it is not a sign. */
	public static String[] getSignLines(Block block)
	{
		if (block == null || !(block.getState() instanceof Sign))
		{
			return null;
		}

		return ((Sign) block.getState()).getSide(Side.FRONT).getLines();
	}

	/**
	 * Waxes the sign of a new altar one tick later (after the text from the
	 * sign editor has been applied), so players can't open the sign editor on it.
	 */
	public void waxAltarSignLater(Block signBlock)
	{
		Gods.instance().getServer().getScheduler().runTask(Gods.instance(), () -> {
			if (signBlock.getState() instanceof Sign)
			{
				Sign sign = (Sign) signBlock.getState();
				sign.setWaxed(true);
				sign.update();
			}
		});
	}

	public Block getAltarBlockFromSign(Block block)
	{
		if (!isWallSign(block))
		{
			return null;
		}
		BlockData bd = block.getBlockData();
		if (!(bd instanceof Directional)) {
			return null;
		}
		Directional directional = (Directional) bd;

		Block altarBlock = block.getRelative(directional.getFacing().getOppositeFace());

		Gods.instance().logDebug("getAltarBlockFromSign(): AltarBlock block is " + altarBlock.getType().name());
		if (getGodTypeForAltarBlockType(altarBlock.getType()) == null)
		{
			return null;
		}
		if (!isAltarTorch(altarBlock.getRelative(BlockFace.UP).getType()))
		{
			return null;
		}
		return altarBlock;
	}

	private static boolean isAltarTorch(Material type)
	{
		return type == Material.TORCH || type == Material.REDSTONE_TORCH;
	}

	public List<String> getAltarBlockTypesFromGodType(GodManager.GodType godType)
	{
		List<String> list = new ArrayList<String>();
		for (Material blockMaterial : this.altarBlockTypes.keySet())
		{
			if (this.altarBlockTypes.containsKey(blockMaterial) && this.altarBlockTypes.get(blockMaterial).contains(godType))
			{
				list.add(blockMaterial.name());
			}
		}
		return list;
	}

	public Player getBlessedPlayerFromAltarSign(Block block, String[] lines)
	{
		return getPlayerFromAltarSign(block, lines, "blessings");
	}

	public Player getCursedPlayerFromAltar(Block block, String[] lines)
	{
		return getPlayerFromAltarSign(block, lines, "curses");
	}

	/** The online player named on line 3 of a sign whose first line is the given keyword. */
	private Player getPlayerFromAltarSign(Block block, String[] lines, String keyword)
	{
		if (!isWallSign(block) || lines == null || lines.length < 3 || lines[0] == null)
		{
			return null;
		}

		if (!ChatColor.stripColor(lines[0]).trim().equalsIgnoreCase(keyword))
		{
			return null;
		}

		String playerName = lines[2];
		if ((playerName == null) || (playerName.trim().length() < 1))
		{
			return null;
		}

		// Exact name: getPlayer(String) would also match name prefixes
		return Gods.instance().getServer().getPlayerExact(ChatColor.stripColor(playerName).trim());
	}

	public UUID getDroppedItemPlayer(int entityID)
	{
		return this.droppedItems.get(entityID);
	}

	public GodManager.GodGender getGodGenderFromAltarBlock(Block block)
	{
		if (block.getRelative(BlockFace.UP).getType().equals(Material.REDSTONE_TORCH))
		{
			return GodManager.GodGender.Female;
		}
		return GodManager.GodGender.Male;
	}

	public GodManager.GodType getGodTypeForAltarBlockType(Material altarBlockType)
	{
		List<GodManager.GodType> godTypes = this.altarBlockTypes.get(altarBlockType);
		if ((godTypes == null) || (godTypes.size() == 0))
		{
			Gods.instance().logDebug("No god types available for block type " + altarBlockType.name() + "!");
			return null;
		}
		return godTypes.get(this.random.nextInt(godTypes.size()));
	}

	public boolean handleNewBlessingAltar(SignChangeEvent event)
	{
		Player player = event.getPlayer();

		if (!player.isOp() && !PermissionsManager.instance().hasPermission(player, "gods.altar.build"))
		{
			return false;
		}
		event.setLine(0, "Blessings");
		event.setLine(1, "On");

		event.setLine(3, "");

		Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.BlessingsHelp, ChatColor.AQUA, 0, "", 10);

		return true;
	}

	public boolean handleNewCursingAltar(SignChangeEvent event)
	{
		Player player = event.getPlayer();
		if ((!player.isOp()) && (!PermissionsManager.instance().hasPermission(player, "gods.altar.build")))
		{
			Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.BuildAltarNotAllowed, ChatColor.DARK_RED, 0, "", 10);
			return false;
		}
		
		event.setLine(0, "Curses");
		event.setLine(1, "On");
		event.setLine(3, "");

		Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.CursesHelp, ChatColor.AQUA, 0, "", 20);

		return true;
	}

	public boolean handleNewPrayingAltar(SignChangeEvent event)
	{
		Player player = event.getPlayer();
		if ((!player.isOp()) && (!PermissionsManager.instance().hasPermission(player, "gods.altar.build")))
		{
			Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.BuildAltarNotAllowed, ChatColor.DARK_RED, 0, "", 10);
			return false;
		}

		// The god's name is the one line the player wrote on the sign
		String godName = "";
		int line = 0;
		int otherline = 0;
		while ((godName.isEmpty()) && (line < 4))
		{
			String text = event.getLine(line++);
			godName = (text != null) ? text : "";
		}

		line--;

		while (otherline < 4)
		{
			if (otherline == line)
			{
				otherline++;
			}
			else
			{
				String text = event.getLine(otherline);

				if ((text != null) && (text.length() > 0))
				{
					Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.InvalidAltarSign, ChatColor.DARK_RED, 0, "", 10);
					return false;
				}
				otherline++;
			}
		}

		if (godName.length() <= 1)
		{
			Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.InvalidGodName, ChatColor.DARK_RED, 0, "", 20);
			return false;
		}

		godName = godName.trim();
		godName = GodManager.instance().formatGodName(godName);

		if ((godName.length() <= 1) || (godName.contains(" ")))
		{
			Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.InvalidGodName, ChatColor.DARK_RED, 0, "", 20);
			return false;
		}

		if ((Gods.instance().isBlacklistedGod(godName)) || (!Gods.instance().isWhitelistedGod(godName)))
		{
			Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.PrayToBlacklistedGodNotAllowed, ChatColor.DARK_RED, 0, "", 1);
			return false;
		}

		if (!GodManager.instance().hasGodAccess(player.getUniqueId(), godName))
		{
			Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.PrivateGodNoAccess, ChatColor.DARK_RED, 0, "", 1);
			return false;
		}

		String currentGodName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());
		if (currentGodName != null && !currentGodName.equals(godName))
		{
			Gods.instance().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.CannotBuildAltarToOtherGods, ChatColor.DARK_RED, 0, godName, 1);
			return false;
		}

		Block altarBlock = getAltarBlockFromSign(player.getWorld().getBlockAt(event.getBlock().getLocation()));

		if (altarBlock == null)
		{
			return false;
		}
		
		if (GodManager.instance().addAltar(event.getPlayer(), godName, event.getBlock().getLocation()))
		{
			if ((GodsConfiguration.instance().isHolyLandEnabled()) && (PermissionsManager.instance().hasPermission(player, "gods.holyland")))
			{
				HolyLandManager.instance().addAltar(player, godName, altarBlock.getLocation());
			}
			
			QuestManager.instance().handleBuiltPrayingAltar(godName);

			event.setLine(0, "Altar");
			event.setLine(1, "of");
			event.setLine(2, godName);
			event.setLine(3, "");

			Gods.instance().sendInfo(event.getPlayer().getUniqueId(), LanguageManager.LANGUAGESTRING.PrayAlterHelp, ChatColor.AQUA, 0, godName, 80);
		}
		else
		{
			event.setLine(0, "");
			event.setLine(1, "");
			event.setLine(2, "");
			event.setLine(3, "");

			return false;
		}

		return true;
	}

	public boolean isAltarBlock(Block block)
	{
		if ((block == null) || (getGodTypeForAltarBlockType(block.getType()) == null))
		{
			return false;
		}
		if (!isAltarTorch(block.getRelative(BlockFace.UP).getType()))
		{
			return false;
		}

		for (BlockFace face : new BlockFace[] { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST })
		{
			if (isWallSign(block.getRelative(face)))
			{
				return true;
			}
		}

		return false;
	}

	/** A wall sign on an altar block (a block with an altar type and a torch on top). */
	public boolean isAltarSign(Block block)
	{
		return getAltarBlockFromSign(block) != null;
	}

	/** True if the front of this sign already shows a finished altar ("Altar of ...", "Curses On ...", "Blessings On ..."). */
	public boolean isFormattedAltarSign(Block block)
	{
		String[] lines = getSignLines(block);
		if (lines == null || lines.length < 2)
		{
			return false;
		}

		String first = ChatColor.stripColor(lines[0]).trim();
		String second = ChatColor.stripColor(lines[1]).trim();

		return (first.equals("Altar") && second.equals("of")) || ((first.equals("Curses") || first.equals("Blessings")) && second.equals("On"));
	}

	public boolean isBlessingAltar(Block block, String[] lines)
	{
		if (!isAltarSign(block))
		{
			return false;
		}
		return getBlessedPlayerFromAltarSign(block, lines) != null;
	}

	public boolean isCursingAltar(Block block, String[] lines)
	{
		if (!isAltarSign(block))
		{
			return false;
		}
		return getCursedPlayerFromAltar(block, lines) != null;
	}

	public boolean isPrayingAltar(Block block)
	{
		return isAltarSign(block);
	}

	public void resetAltarBlockTypes()
	{
		this.altarBlockTypes.clear();

		{
			ArrayList<GodManager.GodType> list = new ArrayList<GodType>();
			list.add(GodManager.GodType.MOON);
			this.altarBlockTypes.put(Material.END_STONE, list);
		}

		{
			ArrayList<GodType> list = new ArrayList<GodType>();
			list.add(GodManager.GodType.EVIL);
			this.altarBlockTypes.put(Material.OBSIDIAN, list);
		}

		{
			ArrayList<GodManager.GodType> list = new ArrayList<GodType>();
			list.add(GodManager.GodType.FROST);
			this.altarBlockTypes.put(Material.SNOW_BLOCK, list);
		}

		{
			ArrayList<GodManager.GodType> list = new ArrayList<GodType>();
			list.add(GodManager.GodType.CREATURES);
			this.altarBlockTypes.put(Material.OAK_LOG, list);
		}

		{
			ArrayList<GodManager.GodType> list = new ArrayList<GodType>();
			list.add(GodManager.GodType.NATURE);
			this.altarBlockTypes.put(Material.MELON, list);
		}

		{
			ArrayList<GodManager.GodType> list = new ArrayList<GodType>();
			list.add(GodManager.GodType.WISDOM);
			this.altarBlockTypes.put(Material.BOOKSHELF, list);
		}

		{
			ArrayList<GodManager.GodType> list = new ArrayList<GodType>();
			list.add(GodManager.GodType.LOVE);
			this.altarBlockTypes.put(Material.DIAMOND_BLOCK, list);
		}

		{
			ArrayList<GodManager.GodType> list = new ArrayList<GodType>();
			list.add(GodManager.GodType.THUNDER);
			this.altarBlockTypes.put(Material.QUARTZ_BLOCK, list);
		}

		{
			ArrayList<GodManager.GodType> list = new ArrayList<GodType>();
			list.add(GodManager.GodType.SUN);
			this.altarBlockTypes.put(Material.SANDSTONE, list);
		}

		{
			ArrayList<GodManager.GodType> list = new ArrayList<GodType>();
			list.add(GodManager.GodType.PARTY);
			this.altarBlockTypes.put(Material.EMERALD_BLOCK, list);
		}

		{
			ArrayList<GodManager.GodType> list = new ArrayList<GodType>();
			list.add(GodManager.GodType.WAR);
			this.altarBlockTypes.put(Material.NETHERRACK, list);
		}

		{
			ArrayList<GodManager.GodType> list = new ArrayList<GodType>();
			list.add(GodManager.GodType.SEA);
			this.altarBlockTypes.put(Material.LAPIS_BLOCK, list);
		}
	}

	public void setAltarBlockTypeForGodType(GodType godType, Material blockMaterial)
	{
		if(blockMaterial==null)
		{
			Gods.instance().log("setAltarBlockTypeForGodType blockMaterial==null for godType " + godType.name());
			return;
		}
		
		List<GodType> godTypes = new ArrayList<GodType>();
		
		if (!this.altarBlockTypes.containsKey(blockMaterial))
		{
			altarBlockTypes.put(blockMaterial, new ArrayList<GodType>());
		}
				
		godTypes = this.altarBlockTypes.get(blockMaterial);
		
		if (!godTypes.contains(godType))
		{
			godTypes.add(godType);
		}
		
		this.altarBlockTypes.put(blockMaterial, godTypes);
	}
}