package com.dogonfire.gods.service;

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

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.model.GodType;

/**
 * Altars: a block of an altar type with a torch on top and a wall sign on it. Recognizes altar blocks and signs, turns
 * newly written signs into praying, cursing and blessing altars (the text rules are in {@link AltarSigns}), and
 * remembers who dropped which item, for sacrifices. Has no data file. The events are handled by
 * {@code listener/AltarListener}. Main thread only.
 */
public class AltarService
{
	private Random							random			= new Random();
	private Map<Integer, UUID>			droppedItems	= new HashMap<Integer, UUID>();

	private final GodsContext ctx;

	public AltarService(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	public void addDroppedItem(int entityID, UUID playerId)
	{
		this.droppedItems.put(Integer.valueOf(entityID), playerId);
	}

	public void clearDroppedItems()
	{
		ctx.plugin().logDebug("Cleared " + this.droppedItems.size() + " dropped items");
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
		ctx.plugin().getServer().getScheduler().runTask(ctx.plugin(), () -> {
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

		ctx.plugin().logDebug("getAltarBlockFromSign(): AltarBlock block is " + altarBlock.getType().name());
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

	public List<String> getAltarBlockTypesFromGodType(GodType godType)
	{
		List<String> list = new ArrayList<String>();
		for (Material blockMaterial : ctx.settings().altars().blocksFor(godType))
		{
			list.add(blockMaterial.name());
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
		if (!isWallSign(block))
		{
			return null;
		}

		String playerName = AltarSigns.namedPlayer(lines, keyword);

		// Exact name: getPlayer(String) would also match name prefixes
		return playerName != null ? ctx.plugin().getServer().getPlayerExact(playerName) : null;
	}

	public UUID getDroppedItemPlayer(int entityID)
	{
		return this.droppedItems.get(entityID);
	}

	public GodGender getGodGenderFromAltarBlock(Block block)
	{
		if (block.getRelative(BlockFace.UP).getType().equals(Material.REDSTONE_TORCH))
		{
			return GodGender.Female;
		}
		return GodGender.Male;
	}

	public GodType getGodTypeForAltarBlockType(Material altarBlockType)
	{
		List<GodType> godTypes = ctx.settings().altars().godTypesFor(altarBlockType);
		if (godTypes.isEmpty())
		{
			ctx.plugin().logDebug("No god types available for block type " + altarBlockType.name() + "!");
			return null;
		}
		return godTypes.get(this.random.nextInt(godTypes.size()));
	}

	public boolean handleNewBlessingAltar(SignChangeEvent event)
	{
		Player player = event.getPlayer();

		if (!player.isOp() && !ctx.permissionsManager().hasPermission(player, "gods.altar.build"))
		{
			return false;
		}
		event.setLine(0, "Blessings");
		event.setLine(1, "On");

		event.setLine(3, "");

		ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.BlessingsHelp, ChatColor.AQUA, 0, "", 10);

		return true;
	}

	public boolean handleNewCursingAltar(SignChangeEvent event)
	{
		Player player = event.getPlayer();
		if ((!player.isOp()) && (!ctx.permissionsManager().hasPermission(player, "gods.altar.build")))
		{
			ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.BuildAltarNotAllowed, ChatColor.DARK_RED, 0, "", 10);
			return false;
		}
		
		event.setLine(0, "Curses");
		event.setLine(1, "On");
		event.setLine(3, "");

		ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.CursesHelp, ChatColor.AQUA, 0, "", 20);

		return true;
	}

	public boolean handleNewPrayingAltar(SignChangeEvent event)
	{
		Player player = event.getPlayer();
		if ((!player.isOp()) && (!ctx.permissionsManager().hasPermission(player, "gods.altar.build")))
		{
			ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.BuildAltarNotAllowed, ChatColor.DARK_RED, 0, "", 10);
			return false;
		}

		// The god's name is the one line the player wrote on the sign
		AltarSigns.GodNameLine nameLine = AltarSigns.godNameOnNewSign(event.getLines());
		if (nameLine.problem() == AltarSigns.Problem.INVALID_SIGN)
		{
			ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.InvalidAltarSign, ChatColor.DARK_RED, 0, "", 10);
			return false;
		}
		if (nameLine.problem() == AltarSigns.Problem.INVALID_GOD_NAME)
		{
			ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.InvalidGodName, ChatColor.DARK_RED, 0, "", 20);
			return false;
		}

		String godName = nameLine.godName().trim();
		godName = ctx.godManager().formatGodName(godName);

		if ((godName.length() <= 1) || (godName.contains(" ")))
		{
			ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.InvalidGodName, ChatColor.DARK_RED, 0, "", 20);
			return false;
		}

		if (!ctx.godListService().isAllowedGod(godName))
		{
			ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.PrayToBlacklistedGodNotAllowed, ChatColor.DARK_RED, 0, "", 1);
			return false;
		}

		if (!ctx.godManager().hasGodAccess(player.getUniqueId(), godName))
		{
			ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.PrivateGodNoAccess, ChatColor.DARK_RED, 0, "", 1);
			return false;
		}

		String currentGodName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		if (currentGodName != null && !currentGodName.equals(godName))
		{
			ctx.plugin().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.CannotBuildAltarToOtherGods, ChatColor.DARK_RED, 0, godName, 1);
			return false;
		}

		Block altarBlock = getAltarBlockFromSign(player.getWorld().getBlockAt(event.getBlock().getLocation()));

		if (altarBlock == null)
		{
			return false;
		}
		
		if (ctx.godManager().addAltar(event.getPlayer(), godName, event.getBlock().getLocation()))
		{
			if ((ctx.settings().holyLand().enabled()) && (ctx.permissionsManager().hasPermission(player, "gods.holyland")))
			{
				ctx.holyLandService().addAltar(player, godName, altarBlock.getLocation());
			}
			
			ctx.questManager().handleBuiltPrayingAltar(godName);

			event.setLine(0, "Altar");
			event.setLine(1, "of");
			event.setLine(2, godName);
			event.setLine(3, "");

			ctx.plugin().sendInfo(event.getPlayer().getUniqueId(), LanguageManager.LANGUAGESTRING.PrayAlterHelp, ChatColor.AQUA, 0, godName, 80);
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
		return AltarSigns.isFormatted(getSignLines(block));
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
}
