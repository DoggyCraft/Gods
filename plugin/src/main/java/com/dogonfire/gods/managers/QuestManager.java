package com.dogonfire.gods.managers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Random;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.managers.LanguageManager.LANGUAGESTRING;
import com.dogonfire.gods.storage.DataFile;

public class QuestManager
{
	// Every constant may be stored in quests.yml, so keep them all even if no longer generated
	static enum QUESTTYPE {
		NONE, FIREWORKPARTY, HUMANSACRIFICE, MOBSACRIFICE, ITEMSACRIFICE, GIVEROSE, KILLBOSS, HARVEST, CONVERT, GETHOLYARTIFACT, DELIVERITEM, SLAY, BUILDALTARS, BUILDTOWER, HOLYFEAST, COLLECTBIBLES, BURNBIBLES, GIVEITEMS, PILGRIMAGE, HOLYWAR, CLAIMHOLYLAND, PVPREVENGE, SPREADLOVE, SHOWHOLYARTIFACT, CRAFTITEMS, SLAYDRAGON, CRUSADE;

		@Override
		public String toString()
		{
			String output = name().toString();
			output = output.charAt(0) + output.substring(1).toLowerCase();

			return output;
		}
	}

	private static enum Phase {
		STARTED, PROGRESS, STATUS, COMPLETED, FAILED
	}

	// What each god says about a quest, indexed by Phase. Types without speech have no entry.
	private static final Map<QUESTTYPE, LANGUAGESTRING[]>	QUEST_SPEECH			= new EnumMap<QUESTTYPE, LANGUAGESTRING[]>(QUESTTYPE.class);

	static
	{
		speech(QUESTTYPE.SLAY, LANGUAGESTRING.GodToBelieversSlayQuestStarted, LANGUAGESTRING.GodToBelieversSlayQuestProgress, LANGUAGESTRING.GodToBelieversSlayQuestStatus, LANGUAGESTRING.GodToBelieversSlayQuestCompleted, LANGUAGESTRING.GodToBelieversSlayQuestFailed);
		speech(QUESTTYPE.CONVERT, LANGUAGESTRING.GodToBelieversConvertQuestStarted, LANGUAGESTRING.GodToBelieversConvertQuestProgress, LANGUAGESTRING.GodToBelieversConvertQuestStatus, LANGUAGESTRING.GodToBelieversConvertQuestCompleted, LANGUAGESTRING.GodToBelieversConvertQuestFailed);
		speech(QUESTTYPE.BUILDALTARS, LANGUAGESTRING.GodToBelieversBuildAltarsQuestStarted, LANGUAGESTRING.GodToBelieversBuildAltarsQuestProgress, LANGUAGESTRING.GodToBelieversBuildAltarsQuestStatus, LANGUAGESTRING.GodToBelieversBuildAltarsQuestCompleted, LANGUAGESTRING.GodToBelieversBuildAltarsQuestFailed);
		speech(QUESTTYPE.BUILDTOWER, LANGUAGESTRING.GodToBelieversBuildTowerQuestStarted, LANGUAGESTRING.GodToBelieversBuildTowerQuestProgress, LANGUAGESTRING.GodToBelieversBuildTowerQuestStatus, LANGUAGESTRING.GodToBelieversBuildTowerQuestCompleted, LANGUAGESTRING.GodToBelieversBuildTowerQuestFailed);
		speech(QUESTTYPE.ITEMSACRIFICE, LANGUAGESTRING.GodToBelieversSacrificeQuestStarted, LANGUAGESTRING.GodToBelieversSacrificeQuestProgress, LANGUAGESTRING.GodToBelieversSacrificeQuestStatus, LANGUAGESTRING.GodToBelieversSacrificeQuestCompleted, LANGUAGESTRING.GodToBelieversSacrificeQuestFailed);
		speech(QUESTTYPE.HOLYFEAST, LANGUAGESTRING.GodToBelieversHolyFeastQuestStarted, LANGUAGESTRING.GodToBelieversHolyFeastQuestProgress, LANGUAGESTRING.GodToBelieversHolyFeastQuestStatus, LANGUAGESTRING.GodToBelieversHolyFeastQuestCompleted, LANGUAGESTRING.GodToBelieversHolyFeastQuestFailed);
		speech(QUESTTYPE.GIVEITEMS, LANGUAGESTRING.GodToBelieversGiveItemsQuestStarted, LANGUAGESTRING.GodToBelieversGiveBiblesQuestProgress, LANGUAGESTRING.GodToBelieversGiveItemsQuestStatus, LANGUAGESTRING.GodToBelieversGiveItemsQuestCompleted, LANGUAGESTRING.GodToBelieversGiveItemsQuestFailed);
		speech(QUESTTYPE.BURNBIBLES, LANGUAGESTRING.GodToBelieversBurnBiblesQuestStarted, LANGUAGESTRING.GodToBelieversBurnBiblesQuestProgress, LANGUAGESTRING.GodToBelieversBurnBiblesQuestStatus, LANGUAGESTRING.GodToBelieversBurnBiblesQuestCompleted, LANGUAGESTRING.GodToBelieversBurnBiblesQuestFailed);
		speech(QUESTTYPE.CRUSADE, LANGUAGESTRING.GodToBelieversCrusadeQuestStarted, LANGUAGESTRING.GodToBelieversCrusadeQuestProgress, LANGUAGESTRING.GodToBelieversCrusadeQuestStatus, LANGUAGESTRING.GodToBelieversCrusadeQuestCompleted, LANGUAGESTRING.GodToBelieversCrusadeQuestFailed);
		speech(QUESTTYPE.PILGRIMAGE, LANGUAGESTRING.GodToBelieversPilgrimageQuestStarted, LANGUAGESTRING.GodToBelieversPilgrimageQuestProgress, LANGUAGESTRING.GodToBelieversPilgrimageQuestStatus, LANGUAGESTRING.GodToBelieversPilgrimageQuestCompleted, LANGUAGESTRING.GodToBelieversPilgrimageQuestFailed);
		speech(QUESTTYPE.CLAIMHOLYLAND, LANGUAGESTRING.GodToBelieversClaimHolyLandQuestStarted, LANGUAGESTRING.GodToBelieversClaimHolyLandQuestProgress, LANGUAGESTRING.GodToBelieversClaimHolyLandQuestStatus, LANGUAGESTRING.GodToBelieversClaimHolyLandQuestCompleted, LANGUAGESTRING.GodToBelieversClaimHolyLandQuestFailed);
	}

	private static void speech(QUESTTYPE type, LANGUAGESTRING started, LANGUAGESTRING progress, LANGUAGESTRING status, LANGUAGESTRING completed, LANGUAGESTRING failed)
	{
		QUEST_SPEECH.put(type, new LANGUAGESTRING[] { started, progress, status, completed, failed });
	}

	private static LANGUAGESTRING questSpeech(QUESTTYPE type, Phase phase)
	{
		LANGUAGESTRING[] strings = QUEST_SPEECH.get(type);
		return (strings == null) ? null : strings[phase.ordinal()];
	}

	// Location searches run on the main thread, so they try few spots and load few chunks
	private static final int								MAX_LOCATION_ATTEMPTS	= 32;
	private static final int								MAX_CHUNK_LOADS			= 6;
	private static final int								MAX_REWARD_ROLLS		= 100;

	private static final Set<Material>						PILGRIMAGE_GROUND		= EnumSet.of(Material.GRASS_BLOCK, Material.PODZOL, Material.MYCELIUM, Material.SNOW_BLOCK, Material.SAND, Material.RED_SAND, Material.MOSS_BLOCK);

	private static final Set<Biome>							PILGRIMAGE_BIOMES		= new HashSet<Biome>();

	static
	{
		// Biomes are registry entries, so skip any this server doesn't know
		for (Biome biome : new Biome[] { Biome.FLOWER_FOREST, Biome.ICE_SPIKES, Biome.DESERT, Biome.MUSHROOM_FIELDS, Biome.JUNGLE, Biome.TAIGA, Biome.OLD_GROWTH_SPRUCE_TAIGA, Biome.OLD_GROWTH_PINE_TAIGA, Biome.PLAINS, Biome.SUNFLOWER_PLAINS, Biome.MEADOW, Biome.CHERRY_GROVE, Biome.FOREST, Biome.BIRCH_FOREST, Biome.SAVANNA, Biome.SNOWY_PLAINS, Biome.SNOWY_TAIGA, Biome.GROVE, Biome.SNOWY_SLOPES })
		{
			if (biome != null)
			{
				PILGRIMAGE_BIOMES.add(biome);
			}
		}
	}

	private Random						random				= new Random();

	private final GodsContext ctx;

	public QuestManager(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	/** quests.yml; see {@link DataFile} */
	private DataFile questsFile()
	{
		return ctx.storage().quests();
	}

	private YamlConfiguration questsConfig()
	{
		return questsFile().yaml();
	}

	private boolean addQuestPlayerProgressForGod(String godName, UUID playerId)
	{
		boolean complete = false;

		List<String> players = questsConfig().getStringList(godName + ".Players");
		if (players.contains(playerId.toString()))
		{
			return false;
		}

		players.add(playerId.toString());

		int questAmount = questsConfig().getInt(godName + ".Amount");

		complete = players.size() >= questAmount;

		questsConfig().set(godName + ".Players", players);
		questsConfig().set(godName + ".Progress", Integer.valueOf(players.size()));

		questsFile().save();

		return complete;
	}

	private boolean addQuestProgressForGod(String godName)
	{
		boolean complete = false;

		int questAmount = questsConfig().getInt(godName + ".Amount");
		int questProgress = questsConfig().getInt(godName + ".Progress");

		questProgress++;

		complete = questProgress >= questAmount;

		questsConfig().set(godName + ".Progress", Integer.valueOf(questProgress));

		questsFile().save();

		return complete;
	}

	void clearQuestTarget(Location location)
	{
		int hash = hashVector(location);
		questsConfig().set("QuestTargets." + hash, null);

		questsFile().save();
	}

	/**
	 * Quest: make non-believers read the god's holy book. Completed in {@link #handleReadBible}.
	 */
	private boolean generateGiveItemsQuest(String godName)
	{
		String bibleTitle = ctx.holyBookManager().getBibleTitle(godName);
		if (bibleTitle == null)
		{
			return false;
		}

		int nonBelievers = 0;
		for (Player player : ctx.plugin().getServer().getOnlinePlayers())
		{
			if (ctx.believerService().getGodForBeliever(player.getUniqueId()) == null)
			{
				nonBelievers++;
			}
		}
		if (nonBelievers == 0)
		{
			return false;
		}

		Date thisDate = new Date();
		String pattern = "HH:mm dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);

		QUESTTYPE questType = QUESTTYPE.GIVEITEMS;
		int questAmount = 1 + nonBelievers / (2 + this.random.nextInt(3));
		int questMaxDuration = (1 + this.random.nextInt(5)) * questAmount;

		questsConfig().set(godName + ".Type", questType.toString());
		questsConfig().set(godName + ".Amount", Integer.valueOf(questAmount));
		questsConfig().set(godName + ".TargetType", bibleTitle);
		questsConfig().set(godName + ".MaxDuration", Integer.valueOf(questMaxDuration));
		questsConfig().set(godName + ".Progress", null);
		questsConfig().set(godName + ".Players", null);
		questsConfig().set(godName + ".CreatedTime", formatter.format(thisDate));

		questsFile().save();

		ctx.plugin().log(godName + " issued a quest: Make " + questAmount + " non-believers read the Holy Book");

		return true;
	}

	private String generateHolyMountainName()
	{
		return "Holy mountain of " + generateHolyName();
	}

	private String generateHolyName()
	{
		final String[] consonants = { "b", "f", "g", "h", "j", "k", "l", "ll", "v", "r", "rr", "kk", "p", "t", "s", "x", "d", "n", "m", "nn" };
		final String[] vowels = { "a", "o", "i", "u", "y", "e" };

		int length = 2 + this.random.nextInt(6);
		StringBuilder name = new StringBuilder();

		boolean wasVowel = false;
		for (int l = 0; l < length; l++)
		{
			String[] letters = wasVowel ? consonants : vowels;
			name.append(letters[this.random.nextInt(letters.length)]);
			wasVowel = !wasVowel;
		}
		return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
	}

	private boolean generatePilgrimageQuest(String godName)
	{
		String questTargetType = generateHolyMountainName();
		Date thisDate = new Date();
		String pattern = "HH:mm dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);

		QUESTTYPE questType = QUESTTYPE.PILGRIMAGE;
		int questMaxDuration = 15 + this.random.nextInt(10);

		Set<UUID> thisGodOnlineBelievers = ctx.believerService().getOnlineBelieversForGod(godName);
		if (thisGodOnlineBelievers.isEmpty())
		{
			ctx.plugin().logDebug("No online believers for a pilgrimage quest for " + godName);
			return false;
		}

		UUID randomId = (UUID) thisGodOnlineBelievers.toArray()[this.random.nextInt(thisGodOnlineBelievers.size())];
		Player believer = ctx.plugin().getServer().getPlayer(randomId);
		if (believer == null)
		{
			return false;
		}

		Block chestBlock = findPilgrimageChestBlock(believer.getWorld(), 500, 2000, believer.getLocation());
		if (chestBlock == null)
		{
			return false;
		}

		List<ItemStack> rewards = getRewardsForQuestCompletion(200 + this.random.nextInt(150));
		Material groundType = chestBlock.getRelative(BlockFace.DOWN).getType();

		if (!placePilgrimageChest(chestBlock, rewards))
		{
			return false;
		}

		Location targetLocation = chestBlock.getLocation();

		this.setQuestTarget(godName, targetLocation);

		questsConfig().set(godName + ".Type", questType.toString());
		questsConfig().set(godName + ".TargetType", questTargetType);
		questsConfig().set(godName + ".Location.World", targetLocation.getWorld().getName());
		questsConfig().set(godName + ".Location.X", Integer.valueOf(targetLocation.getBlockX()));
		questsConfig().set(godName + ".Location.Y", Integer.valueOf(targetLocation.getBlockY()));
		questsConfig().set(godName + ".Location.Z", Integer.valueOf(targetLocation.getBlockZ()));
		questsConfig().set(godName + ".Location.Ground", groundType.name());
		questsConfig().set(godName + ".MaxDuration", Integer.valueOf(questMaxDuration));
		questsConfig().set(godName + ".Progress", null);
		questsConfig().set(godName + ".CreatedTime", formatter.format(thisDate));

		questsFile().save();

		ctx.plugin().log(godName + " issued a quest: Pilgrimage to " + questTargetType + " in " + targetLocation.getWorld().getName() + " !");

		return true;
	}

	/**
	 * Tries to give the god a new quest. Only quest types that can be completed are generated. Every attempt counts as the god's last quest time, so a failed
	 * attempt waits as long as a successful one before the next try.
	 */
	public boolean generateQuest(String godName)
	{
		if (hasQuest(godName))
		{
			return false;
		}

		List<QUESTTYPE> questTypes = new ArrayList<QUESTTYPE>();

		if (ctx.settings().quests().pilgrimage())
		{
			questTypes.add(QUESTTYPE.PILGRIMAGE);
		}
		if (ctx.settings().quests().slay())
		{
			questTypes.add(QUESTTYPE.SLAY);
		}
		if (ctx.settings().quests().sacrifice() && ctx.settings().sacrifices().enabled())
		{
			questTypes.add(QUESTTYPE.ITEMSACRIFICE);
		}
		if (ctx.settings().quests().giveItems() && ctx.settings().bibles().enabled())
		{
			questTypes.add(QUESTTYPE.GIVEITEMS);
		}

		Collections.shuffle(questTypes, this.random);

		boolean newQuest = false;

		for (QUESTTYPE questType : questTypes)
		{
			newQuest = switch (questType)
			{
			case PILGRIMAGE -> generatePilgrimageQuest(godName);
			case SLAY -> generateSlayQuest(godName);
			case ITEMSACRIFICE -> generateSacrificeQuest(godName);
			case GIVEITEMS -> generateGiveItemsQuest(godName);
			default -> false;
			};

			if (newQuest)
			{
				break;
			}
		}

		ctx.godManager().setTimeSinceLastQuest(godName);

		if (newQuest)
		{
			godSayNewQuest(godName);
		}
		else
		{
			ctx.plugin().logDebug("Could not generate any quest for " + godName);
		}

		return newQuest;
	}

	private boolean generateSacrificeQuest(String godName)
	{
		String questTargetType = "NONE";
		Date thisDate = new Date();
		String pattern = "HH:mm dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);

		QUESTTYPE questType = QUESTTYPE.ITEMSACRIFICE;
		int questAmount = 1 + this.random.nextInt(1 + ctx.believerService().getOnlineBelieversForGod(godName).size());
		int questMaxDuration = (1 + this.random.nextInt(5)) * questAmount;

		switch (this.random.nextInt(10))
		{
		case 0:
			questTargetType = Material.GOLDEN_SWORD.name();
			break;
		case 1:
			questTargetType = Material.GOLDEN_PICKAXE.name();
			break;
		case 2:
			questTargetType = Material.GOLDEN_SHOVEL.name();
			break;
		case 3:
			questTargetType = Material.GOLDEN_AXE.name();
			break;
		case 4:
			questTargetType = Material.FISHING_ROD.name();
			break;
		case 5:
			questTargetType = Material.ANVIL.name();
			break;
		case 6:
			questTargetType = Material.OAK_BOAT.name();
			break;
		case 7:
			questTargetType = Material.BOOK.name();
			break;
		case 8:
			questTargetType = Material.BOW.name();
			break;
		default:
			questTargetType = Material.APPLE.name();
			break;
		}

		questsConfig().set(godName + ".Type", questType.toString());
		questsConfig().set(godName + ".Amount", Integer.valueOf(questAmount));
		questsConfig().set(godName + ".TargetType", questTargetType);
		questsConfig().set(godName + ".MaxDuration", Integer.valueOf(questMaxDuration));
		questsConfig().set(godName + ".Progress", null);
		questsConfig().set(godName + ".CreatedTime", formatter.format(thisDate));

		questsFile().save();

		ctx.plugin().log(godName + " issued a quest: Sacrifice " + questAmount + " " + questTargetType);

		return true;
	}

	private boolean generateSlayQuest(String godName)
	{
		Date thisDate = new Date();
		String pattern = "HH:mm dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);

		QUESTTYPE questType = QUESTTYPE.SLAY;
		int questAmount = 1 + this.random.nextInt(3) + ctx.believerService().getOnlineBelieversForGod(godName).size();
		int questMaxDuration = (5 + this.random.nextInt(5)) * questAmount;

		EntityType holyCreature = ctx.godManager().getHolyMobTypeForGod(godName);
		EntityType targetType = null;
		do
		{
			switch (this.random.nextInt(10))
			{
			case 0:
				targetType = EntityType.SHEEP;
				break;
			case 1:
				targetType = EntityType.SPIDER;
				break;
			case 2:
				targetType = EntityType.CHICKEN;
				break;
			case 3:
				targetType = EntityType.COW;
				break;
			case 4:
				targetType = EntityType.PIG;
				break;
			default:
				targetType = ctx.godManager().getUnholyMobTypeForGod(godName);
				break;
			}
		}
		while (targetType == null || targetType == holyCreature);

		questsConfig().set(godName + ".Type", questType.toString());
		questsConfig().set(godName + ".Amount", Integer.valueOf(questAmount));
		questsConfig().set(godName + ".TargetType", targetType.name());
		questsConfig().set(godName + ".MaxDuration", Integer.valueOf(questMaxDuration));
		questsConfig().set(godName + ".Progress", null);
		questsConfig().set(godName + ".CreatedTime", formatter.format(thisDate));

		questsFile().save();

		ctx.plugin().log(godName + " issued a quest: Kill " + questAmount + " " + targetType.name());

		return true;
	}

	/**
	 * Looks for flat, open, unprotected ground in a fitting biome of an overworld. Tries at most MAX_LOCATION_ATTEMPTS spots and loads at most
	 * MAX_CHUNK_LOADS chunks that weren't loaded already. Returns the block where the chest goes, or null.
	 */
	private Block findPilgrimageChestBlock(World world, int minDist, int maxDist, Location center)
	{
		if (world == null || world.getEnvironment() != World.Environment.NORMAL || !ctx.plugin().isEnabledInWorld(world))
		{
			return null;
		}

		int chunkLoads = 0;

		for (int run = 1; run <= MAX_LOCATION_ATTEMPTS; run++)
		{
			double angle = this.random.nextDouble() * 2.0D * Math.PI;
			double distance = minDist + this.random.nextDouble() * (maxDist - minDist);

			int x = center.getBlockX() + (int) (Math.cos(angle) * distance);
			int z = center.getBlockZ() + (int) (Math.sin(angle) * distance);

			// Keep the spot and its four neighbours inside one chunk
			x = (x & ~15) | (1 + this.random.nextInt(14));
			z = (z & ~15) | (1 + this.random.nextInt(14));

			if (!world.isChunkLoaded(x >> 4, z >> 4))
			{
				if (chunkLoads >= MAX_CHUNK_LOADS)
				{
					continue;
				}
				chunkLoads++;
			}

			Block ground = world.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);

			// Step down through snow layers and plants that count as the top block
			for (int i = 0; i < 2 && isOpenSpace(ground) && ground.getY() > world.getMinHeight(); i++)
			{
				ground = ground.getRelative(BlockFace.DOWN);
			}

			if (ground.getY() <= world.getSeaLevel() || ground.getY() >= world.getMaxHeight() - 2)
			{
				continue;
			}

			if (!PILGRIMAGE_BIOMES.contains(ground.getBiome()) || !isFlatGround(ground))
			{
				continue;
			}

			Block chestBlock = ground.getRelative(BlockFace.UP);

			if (ctx.worldGuard() != null && ctx.worldGuard().isInRegion(chestBlock))
			{
				continue;
			}

			ctx.plugin().logDebug("Pilgrimage Holy mountain found in " + run + " runs (" + chunkLoads + " chunk loads): " + chestBlock.getX() + "," + chestBlock.getY() + "," + chestBlock.getZ());

			return chestBlock;
		}

		ctx.plugin().log("Pilgrimage Holy mountain generation FAILED in " + world.getName());

		return null;
	}

	// Ground in the list with open space above it, and the same for its four neighbours
	private static boolean isFlatGround(Block ground)
	{
		if (!PILGRIMAGE_GROUND.contains(ground.getType()) || !isOpenSpace(ground.getRelative(BlockFace.UP)))
		{
			return false;
		}

		for (BlockFace face : new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST })
		{
			Block neighbour = ground.getRelative(face);
			if (!PILGRIMAGE_GROUND.contains(neighbour.getType()) || !isOpenSpace(neighbour.getRelative(BlockFace.UP)))
			{
				return false;
			}
		}

		return true;
	}

	// Air, or something like grass or a snow layer that a chest may replace (but not water or lava)
	private static boolean isOpenSpace(Block block)
	{
		Material type = block.getType();

		return type.isAir() || (Tag.REPLACEABLE.isTagged(type) && !block.isLiquid() && type != Material.WATER && type != Material.LAVA);
	}

	public int getQuestAmountForGod(String godName)
	{
		int value = questsConfig().getInt(godName + ".Amount");

		return value;
	}

	public Date getQuestCreatedTimeForGod(String godName)
	{
		String pattern = "HH:mm dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		String startTime = questsConfig().getString(godName + ".CreatedTime");
		Date startTimeDate = null;
		try
		{
			startTimeDate = formatter.parse(startTime);
		}
		catch (Exception ex)
		{
			ctx.plugin().log("Invalid quest created date for " + godName + ". Reset.");
			startTimeDate = new Date();
			startTimeDate.setTime(0L);
		}
		return startTimeDate;
	}

	/**
	 * The quest target of the god, or null if it has none or its world isn't loaded.
	 */
	public Location getQuestLocation(String godName)
	{
		Location location = getStoredQuestLocation(godName);

		if (location == null || location.getWorld() == null)
		{
			return null;
		}

		return location;
	}

	// The stored quest target; its world is null if that world isn't loaded
	private Location getStoredQuestLocation(String godName)
	{
		String worldName = questsConfig().getString(godName + ".Location.World");

		if (worldName == null)
		{
			return null;
		}

		int x = questsConfig().getInt(godName + ".Location.X");
		int y = questsConfig().getInt(godName + ".Location.Y");
		int z = questsConfig().getInt(godName + ".Location.Z");

		return new Location(ctx.plugin().getServer().getWorld(worldName), x, y, z);
	}

	public int getQuestMaxDurationForGod(String godName)
	{
		int value = questsConfig().getInt(godName + ".MaxDuration");

		return value;
	}

	public int getQuestProgressForGod(String godName)
	{
		int value = questsConfig().getInt(godName + ".Progress");

		return value;
	}

	String getQuestTargetGod(Location location)
	{
		return questsConfig().getString("QuestTargets." + hashVector(location) + ".God");
	}

	public String getQuestTargetTypeForGod(String godName)
	{
		String value = questsConfig().getString(godName + ".TargetType");

		return value;
	}

	// The quest target as the gods should say it: mob and item names from the language file, the holy book title, ...
	private String getQuestTargetDisplayName(String godName, QUESTTYPE questType)
	{
		String targetType = getQuestTargetTypeForGod(godName);

		try
		{
			switch (questType)
			{
			case SLAY:
				return ctx.languageManager().getMobTypeName(EntityType.valueOf(targetType.toUpperCase()));
			case ITEMSACRIFICE:
			case BURNBIBLES:
				Material material = Material.matchMaterial(targetType);
				String itemName = (material == null) ? null : ctx.languageManager().getItemTypeName(material);
				return (itemName == null) ? targetType : itemName;
			case GIVEITEMS:
				String bibleTitle = ctx.holyBookManager().getBibleTitle(godName);
				return (bibleTitle == null) ? targetType : bibleTitle;
			default:
				return targetType;
			}
		}
		catch (RuntimeException ex)
		{
			return targetType;
		}
	}

	public QUESTTYPE getQuestTypeForGod(String godName)
	{
		String typeName = questsConfig().getString(godName + ".Type");
		if (typeName == null)
		{
			return QUESTTYPE.NONE;
		}

		try
		{
			return QUESTTYPE.valueOf(typeName.toUpperCase());
		}
		catch (IllegalArgumentException ex)
		{
			ctx.plugin().logDebug("Could not recognize quest type '" + typeName + "' for " + godName + ": " + ex.getMessage());
			return QUESTTYPE.NONE;
		}
	}

	/**
	 * Random reward items worth about power in total.
	 */
	public List<ItemStack> getRewardsForQuestCompletion(int power)
	{
		List<ItemStack> rewards = new ArrayList<ItemStack>();

		List<Map.Entry<Material, Integer>> rewardItems = new ArrayList<Map.Entry<Material, Integer>>();
		for (Map.Entry<Material, Integer> entry : ctx.settings().quests().rewardValues().entrySet())
		{
			if (entry.getKey() != null && entry.getKey().isItem() && entry.getValue() != null && entry.getValue() > 0)
			{
				rewardItems.add(entry);
			}
		}

		if (rewardItems.isEmpty())
		{
			return rewards;
		}

		for (int t = 0; power > 0 && t < MAX_REWARD_ROLLS; t++)
		{
			Map.Entry<Material, Integer> rewardItem = rewardItems.get(this.random.nextInt(rewardItems.size()));

			if (rewardItem.getValue() <= power)
			{
				rewards.add(new ItemStack(rewardItem.getKey(), 1));
				power -= rewardItem.getValue();
			}
		}

		return rewards;
	}

	public List<ItemStack> getRewardsForQuestCompletion(String godName)
	{
		return getRewardsForQuestCompletion((int) ctx.godManager().getGodPower(godName));
	}

	// Sets $Amount and $Type for a quest message. LanguageManager is shared, so call it right before speaking.
	private void setQuestSpeechValues(String godName, QUESTTYPE questType, int amount)
	{
		ctx.languageManager().setAmount(amount);

		String targetName = getQuestTargetDisplayName(godName, questType);
		try
		{
			ctx.languageManager().setType((targetName == null) ? "" : targetName);
		}
		catch (Exception ex)
		{
			ctx.plugin().logDebug(ex.getMessage());
		}
	}

	public void godSayFailed(String godName)
	{
		QUESTTYPE questType = getQuestTypeForGod(godName);
		LANGUAGESTRING message = questSpeech(questType, Phase.FAILED);

		if (message == null)
		{
			return;
		}

		setQuestSpeechValues(godName, questType, getQuestAmountForGod(godName) - getQuestProgressForGod(godName));
		ctx.godManager().godSayToBelievers(godName, message, 2 + this.random.nextInt(100));
	}

	private void godSayNewQuest(String godName)
	{
		QUESTTYPE questType = getQuestTypeForGod(godName);
		LANGUAGESTRING message = questSpeech(questType, Phase.STARTED);

		if (message == null)
		{
			return;
		}

		setQuestSpeechValues(godName, questType, getQuestAmountForGod(godName));
		ctx.godManager().godSayToBelievers(godName, message, 2 + this.random.nextInt(100));

		switch (questType)
		{
		case PILGRIMAGE:
			ctx.godManager().sendInfoToBelievers(godName, LANGUAGESTRING.QuestTargetHelp, ChatColor.AQUA, 150);
			break;
		case ITEMSACRIFICE:
			ctx.godManager().sendInfoToBelievers(godName, LANGUAGESTRING.SacrificeHelp, ChatColor.AQUA, 150);
			break;
		default:
			break;
		}
	}

	public void godSayProgress(String godName)
	{
		QUESTTYPE questType = getQuestTypeForGod(godName);
		LANGUAGESTRING message = questSpeech(questType, Phase.PROGRESS);

		if (message == null)
		{
			return;
		}

		setQuestSpeechValues(godName, questType, getQuestAmountForGod(godName) - getQuestProgressForGod(godName));
		ctx.godManager().godSayToBelievers(godName, message, 2 + this.random.nextInt(10));
	}

	public void godSayStatus(String godName)
	{
		QUESTTYPE questType = getQuestTypeForGod(godName);
		LANGUAGESTRING message = questSpeech(questType, Phase.STATUS);

		if (message == null)
		{
			return;
		}

		setQuestSpeechValues(godName, questType, getQuestAmountForGod(godName) - getQuestProgressForGod(godName));

		switch (questType)
		{
		case PILGRIMAGE:
		case SLAYDRAGON:
			// Quests with a place to find: remind now and then, and tell hunting believers how far they are
			if (this.random.nextInt(6) == 0)
			{
				ctx.godManager().godSayToBelievers(godName, message, 2 + this.random.nextInt(100));
			}
			sendQuestTargetHints(godName);
			break;
		default:
			ctx.godManager().godSayToBelievers(godName, message, 2 + this.random.nextInt(100));
			break;
		}
	}

	private void sendQuestTargetHints(String godName)
	{
		Location targetLocation = getQuestLocation(godName);

		if (targetLocation == null)
		{
			return;
		}

		for (UUID believerId : ctx.believerService().getBelieversForGod(godName))
		{
			Player player = ctx.plugin().getServer().getPlayer(believerId);

			if (player == null)
			{
				continue;
			}

			if (!ctx.believerService().isHunting(believerId))
			{
				if (this.random.nextInt(10) == 0)
				{
					ctx.plugin().sendInfo(believerId, LANGUAGESTRING.QuestTargetHelp, ChatColor.AQUA, 0, "", 20);
				}
				continue;
			}

			if (!targetLocation.getWorld().equals(player.getWorld()))
			{
				continue;
			}

			int distance = (int) targetLocation.toVector().distance(player.getLocation().toVector());

			ctx.plugin().sendInfo(believerId, LANGUAGESTRING.QuestTargetRange, ChatColor.AQUA, distance, "", 20);
		}
	}

	public void handleBuiltPrayingAltar(String godName)
	{
		if (!hasQuest(godName) || getQuestTypeForGod(godName) != QUESTTYPE.BUILDALTARS)
		{
			return;
		}

		if (addQuestProgressForGod(godName))
		{
			ctx.godManager().godSayToBelievers(godName, LANGUAGESTRING.GodToBelieversBuildAltarsQuestCompleted, 2 + this.random.nextInt(10));
			ctx.godManager().addBeliefAndRewardBelievers(godName);

			removeSuccessQuestForGod(godName);
		}
		else
		{
			godSayProgress(godName);
		}
	}

	public void handleEat(String playerName, String godName, String entityType)
	{
		if (!hasQuest(godName) || getQuestTypeForGod(godName) != QUESTTYPE.HOLYFEAST)
		{
			return;
		}

		String questTargetType = getQuestTargetTypeForGod(godName);
		if ((questTargetType == null) || (!questTargetType.equalsIgnoreCase(entityType)))
		{
			return;
		}

		ctx.plugin().logDebug("handling quest eating for " + godName);

		if (addQuestProgressForGod(godName))
		{
			ctx.godManager().godSayToBelievers(godName, LANGUAGESTRING.GodToBelieversHolyFeastQuestCompleted, 2 + this.random.nextInt(10));

			ctx.godManager().addMoodForGod(godName, 2.0F * ctx.godManager().getPleasedModifierForGod(godName));
			ctx.godManager().addBeliefAndRewardBelievers(godName);

			removeSuccessQuestForGod(godName);
		}
		else
		{
			godSayProgress(godName);
		}
	}

	public boolean handleJoinReligion(String playerName, String godName)
	{
		if (!hasQuest(godName))
		{
			return false;
		}

		QUESTTYPE questType = getQuestTypeForGod(godName);

		if (questType != QUESTTYPE.CONVERT)
		{
			return false;
		}

		if (addQuestProgressForGod(godName))
		{
			ctx.languageManager().setPlayerName(playerName);
			ctx.godManager().godSayToBelievers(godName, LANGUAGESTRING.GodToBelieversConvertQuestCompleted, 2 + this.random.nextInt(10));

			ctx.godManager().addMoodForGod(godName, 1.0F * ctx.godManager().getPleasedModifierForGod(godName));
			ctx.godManager().addBeliefAndRewardBelievers(godName);

			removeSuccessQuestForGod(godName);
		}
		else
		{
			godSayProgress(godName);
		}
		return true;
	}

	public void handleKilledMob(String godName, String mobType)
	{
		if (!hasQuest(godName) || getQuestTypeForGod(godName) != QUESTTYPE.SLAY)
		{
			return;
		}

		String questTargetType = getQuestTargetTypeForGod(godName);
		if (questTargetType == null || !questTargetType.equalsIgnoreCase(mobType))
		{
			return;
		}

		if (addQuestProgressForGod(godName))
		{
			ctx.godManager().godSayToBelievers(godName, LANGUAGESTRING.GodToBelieversSlayQuestCompleted, 2 + this.random.nextInt(10));

			ctx.godManager().addMoodForGod(godName, 2.0F * ctx.godManager().getPleasedModifierForGod(godName));
			ctx.godManager().addBeliefAndRewardBelievers(godName);

			removeSuccessQuestForGod(godName);
		}
		else
		{
			godSayProgress(godName);
		}
	}

	public boolean handleOpenChest(UUID playerId, Location blockLocation)
	{
		if (!isQuestTarget(blockLocation))
		{
			return false;
		}

		String questGod = getQuestTargetGod(blockLocation);
		String godName = ctx.believerService().getGodForBeliever(playerId);

		// Only believers of the god who set the pilgrimage may open its chest
		if (questGod == null || godName == null || !questGod.equals(godName))
		{
			return true;
		}

		try
		{
			ctx.languageManager().setType(getQuestTargetTypeForGod(questGod));
		}
		catch (Exception ex)
		{
			ctx.plugin().logDebug(ex.getMessage());
		}

		ctx.godManager().godSayToBelievers(questGod, LANGUAGESTRING.GodToBelieversPilgrimageQuestCompleted, 2);

		ctx.godManager().addMoodForGod(godName, 2.0F * ctx.godManager().getPleasedModifierForGod(godName));
		ctx.godManager().addBeliefAndRewardBelievers(godName);
		ctx.believerService().increasePrayerPower(playerId, 1);

		Player player = ctx.plugin().getServer().getPlayer(playerId);
		if (player != null)
		{
			ctx.plugin().getServer().broadcastMessage(ChatColor.GOLD + player.getDisplayName() + ChatColor.AQUA + " completed a pilgrimage to " + ChatColor.GOLD + getQuestTargetTypeForGod(questGod));
		}

		ctx.believerService().setHunting(playerId, false);

		removeSuccessQuestForGod(godName);

		return false;
	}

	public void handlePrayer(String godName, UUID playerId)
	{
		if (!hasQuest(godName) || getQuestTypeForGod(godName) != QUESTTYPE.CONVERT)
		{
			return;
		}

		String playerGod = ctx.believerService().getGodForBeliever(playerId);

		if ((playerGod != null) && (playerGod.equals(godName)))
		{
			return;
		}

		Player player = ctx.plugin().getServer().getPlayer(playerId);

		if (addQuestPlayerProgressForGod(godName, playerId))
		{
			ctx.languageManager().setPlayerName((player != null) ? player.getDisplayName() : "");
			ctx.godManager().godSayToBelievers(godName, LANGUAGESTRING.GodToBelieversConvertQuestCompleted, 2 + this.random.nextInt(10));

			ctx.godManager().addMoodForGod(godName, 1.0F * ctx.godManager().getPleasedModifierForGod(godName));
			ctx.godManager().addBeliefAndRewardBelievers(godName);

			removeSuccessQuestForGod(godName);
		}
		else
		{
			godSayProgress(godName);
		}
	}

	public void handleReadBible(String godName, UUID playerId)
	{
		if (!hasQuest(godName) || getQuestTypeForGod(godName) != QUESTTYPE.GIVEITEMS)
		{
			return;
		}

		// Only non-believers count
		if (ctx.believerService().getGodForBeliever(playerId) != null)
		{
			return;
		}

		Player player = ctx.plugin().getServer().getPlayer(playerId);

		boolean complete = addQuestPlayerProgressForGod(godName, playerId);

		ctx.languageManager().setPlayerName((player != null) ? player.getDisplayName() : "");

		if (complete)
		{
			setQuestSpeechValues(godName, QUESTTYPE.GIVEITEMS, 0);
			ctx.godManager().godSayToBelievers(godName, LANGUAGESTRING.GodToBelieversGiveItemsQuestCompleted, 2 + this.random.nextInt(10));

			ctx.godManager().addMoodForGod(godName, 2.0F * ctx.godManager().getPleasedModifierForGod(godName));
			ctx.godManager().addBeliefAndRewardBelievers(godName);

			removeSuccessQuestForGod(godName);
		}
		else
		{
			godSayProgress(godName);
		}
	}

	public boolean handleSacrifice(Player player, String godName, String entityType)
	{
		if (!hasQuest(godName))
		{
			return false;
		}

		QUESTTYPE questType = getQuestTypeForGod(godName);

		if ((questType != QUESTTYPE.ITEMSACRIFICE) && (questType != QUESTTYPE.BURNBIBLES))
		{
			return false;
		}

		String questTargetType = getQuestTargetTypeForGod(godName);
		if ((questTargetType == null) || (!questTargetType.equalsIgnoreCase(entityType)))
		{
			return false;
		}

		boolean complete = addQuestProgressForGod(godName);
		ctx.believerService().increasePrayerPower(player.getUniqueId(), 1);

		if (complete)
		{
			setQuestSpeechValues(godName, questType, 0);
			ctx.languageManager().setPlayerName(player.getDisplayName());
			ctx.godManager().godSayToBelievers(godName, questSpeech(questType, Phase.COMPLETED), 2 + this.random.nextInt(10));

			ctx.godManager().addMoodForGod(godName, 2.0F * ctx.godManager().getPleasedModifierForGod(godName));
			ctx.godManager().addBeliefAndRewardBelievers(godName);

			removeSuccessQuestForGod(godName);
		}
		else
		{
			godSayProgress(godName);
		}

		return true;
	}

	boolean hasExpiredQuest(String godName)
	{
		Date thisDate = new Date();
		Date questStartTime = getQuestCreatedTimeForGod(godName);
		int questMaxDuration = getQuestMaxDurationForGod(godName);

		long diff = thisDate.getTime() - questStartTime.getTime();
		long diffMinutes = diff / 60000L;

		return diffMinutes > questMaxDuration;
	}

	private int hashVector(Location location)
	{
		return location.getBlockX() * 73856093 ^ location.getBlockY() * 19349663 ^ location.getBlockZ() * 83492791;
	}

	public boolean hasQuest(String godName)
	{
		String quest = questsConfig().getString(godName + ".Type");

		return quest != null;
	}

	boolean isQuestTarget(Location location)
	{
		int hash = hashVector(location);

		String questTargetLocationWorld = questsConfig().getString("QuestTargets." + hash + ".Location.World");

		if (questTargetLocationWorld == null || location.getWorld() == null)
		{
			return false;
		}

		int questTargetLocationX = questsConfig().getInt("QuestTargets." + hash + ".Location.X");
		int questTargetLocationY = questsConfig().getInt("QuestTargets." + hash + ".Location.Y");
		int questTargetLocationZ = questsConfig().getInt("QuestTargets." + hash + ".Location.Z");

		return location.getBlockX() == questTargetLocationX && location.getBlockY() == questTargetLocationY && location.getBlockZ() == questTargetLocationZ && location.getWorld().getName().equals(questTargetLocationWorld);
	}

	public void load()
	{
		questsFile().load();
		if (!questsFile().isReadOnly())
		{
			removeObsoleteQuests();
		}

		ctx.plugin().log("Loaded " + questsConfig().getKeys(false).size() + " quests.");
	}

	/**
	 * Gods 0.9.x could leave quests of types that are no longer generated (and can't be completed or talked
	 * about), and the data of the removed global quests. Those are dropped on load.
	 */
	private void removeObsoleteQuests()
	{
		boolean changed = questsConfig().contains("Global");
		questsConfig().set("Global", null);

		for (String godName : questsConfig().getKeys(false))
		{
			if (godName.equals("QuestTargets"))
			{
				continue;
			}
			QUESTTYPE type = getQuestTypeForGod(godName);
			if (type == QUESTTYPE.HOLYWAR || type == QUESTTYPE.SLAYDRAGON || type == QUESTTYPE.GETHOLYARTIFACT || type == QUESTTYPE.PVPREVENGE)
			{
				ctx.plugin().log("Removing the " + type.name() + " quest of " + godName + ": that quest type no longer exists");
				Location questLocation = getStoredQuestLocation(godName);
				if (questLocation != null)
				{
					clearQuestTarget(questLocation);
				}
				questsConfig().set(godName, null);
				changed = true;
			}
		}

		if (changed)
		{
			questsFile().save();
		}
	}

	/**
	 * Removes a failed quest: takes away its pilgrimage chest, then always clears the quest, even if the chest can't be removed.
	 */
	public void removeFailedQuestForGod(String godName)
	{
		try
		{
			if (getQuestTypeForGod(godName) == QUESTTYPE.PILGRIMAGE)
			{
				removePilgrimageChest(godName);
			}
		}
		catch (RuntimeException ex)
		{
			ctx.plugin().log("Could not remove the pilgrimage chest of " + godName + ": " + ex.getMessage());
		}
		finally
		{
			removeQuest(godName);
		}
	}

	private void removePilgrimageChest(String godName)
	{
		Location location = getQuestLocation(godName);

		if (location == null)
		{
			return;
		}

		Block chestBlock = location.getBlock();

		if (chestBlock.getState() instanceof Chest chest)
		{
			chest.getBlockInventory().clear();
			chestBlock.setType(Material.AIR);
		}

		Block underBlock = chestBlock.getRelative(BlockFace.DOWN);
		if (underBlock.getType() == Material.GLOWSTONE)
		{
			String groundName = questsConfig().getString(godName + ".Location.Ground");
			Material ground = (groundName == null) ? null : Material.matchMaterial(groundName);
			underBlock.setType((ground != null && ground.isBlock()) ? ground : Material.GRASS_BLOCK);
		}
	}

	public void removeSuccessQuestForGod(String godName)
	{
		removeQuest(godName);
	}

	private void removeQuest(String godName)
	{
		Location questLocation = getStoredQuestLocation(godName);

		if (questLocation != null)
		{
			clearQuestTarget(questLocation);
		}

		questsConfig().set(godName, null);

		questsFile().save();
	}

	void setQuestTarget(String godName, Location location)
	{
		int hash = hashVector(location);

		questsConfig().set("QuestTargets." + hash + ".God", godName);
		questsConfig().set("QuestTargets." + hash + ".Location.X", location.getBlockX());
		questsConfig().set("QuestTargets." + hash + ".Location.Y", location.getBlockY());
		questsConfig().set("QuestTargets." + hash + ".Location.Z", location.getBlockZ());
		questsConfig().set("QuestTargets." + hash + ".Location.World", location.getWorld().getName());

		questsFile().save();
	}

	/**
	 * Places the chest with the rewards on a glowstone marker. Undoes it and returns false if the chest can't be filled.
	 */
	private boolean placePilgrimageChest(Block chestBlock, List<ItemStack> rewards)
	{
		Block underBlock = chestBlock.getRelative(BlockFace.DOWN);
		Material groundType = underBlock.getType();

		chestBlock.setType(Material.CHEST);
		underBlock.setType(Material.GLOWSTONE);

		if (!(chestBlock.getState() instanceof Chest chest))
		{
			chestBlock.setType(Material.AIR);
			underBlock.setType(groundType);
			return false;
		}

		chest.getBlockInventory().addItem(rewards.toArray(new ItemStack[0]));

		return true;
	}
}
