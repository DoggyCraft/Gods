package com.dogonfire.gods.managers;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.LanguageManager.LANGUAGESTRING;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;

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
		speech(QUESTTYPE.SLAYDRAGON, LANGUAGESTRING.GodToBelieversSlayDragonQuestStarted, LANGUAGESTRING.GodToBelieversSlayDragonQuestProgress, LANGUAGESTRING.GodToBelieversSlayDragonQuestStatus, LANGUAGESTRING.GodToBelieversSlayDragonQuestCompleted, LANGUAGESTRING.GodToBelieversSlayDragonQuestFailed);
		speech(QUESTTYPE.CONVERT, LANGUAGESTRING.GodToBelieversConvertQuestStarted, LANGUAGESTRING.GodToBelieversConvertQuestProgress, LANGUAGESTRING.GodToBelieversConvertQuestStatus, LANGUAGESTRING.GodToBelieversConvertQuestCompleted, LANGUAGESTRING.GodToBelieversConvertQuestFailed);
		speech(QUESTTYPE.BUILDALTARS, LANGUAGESTRING.GodToBelieversBuildAltarsQuestStarted, LANGUAGESTRING.GodToBelieversBuildAltarsQuestProgress, LANGUAGESTRING.GodToBelieversBuildAltarsQuestStatus, LANGUAGESTRING.GodToBelieversBuildAltarsQuestCompleted, LANGUAGESTRING.GodToBelieversBuildAltarsQuestFailed);
		speech(QUESTTYPE.BUILDTOWER, LANGUAGESTRING.GodToBelieversBuildTowerQuestStarted, LANGUAGESTRING.GodToBelieversBuildTowerQuestProgress, LANGUAGESTRING.GodToBelieversBuildTowerQuestStatus, LANGUAGESTRING.GodToBelieversBuildTowerQuestCompleted, LANGUAGESTRING.GodToBelieversBuildTowerQuestFailed);
		speech(QUESTTYPE.ITEMSACRIFICE, LANGUAGESTRING.GodToBelieversSacrificeQuestStarted, LANGUAGESTRING.GodToBelieversSacrificeQuestProgress, LANGUAGESTRING.GodToBelieversSacrificeQuestStatus, LANGUAGESTRING.GodToBelieversSacrificeQuestCompleted, LANGUAGESTRING.GodToBelieversSacrificeQuestFailed);
		speech(QUESTTYPE.HOLYFEAST, LANGUAGESTRING.GodToBelieversHolyFeastQuestStarted, LANGUAGESTRING.GodToBelieversHolyFeastQuestProgress, LANGUAGESTRING.GodToBelieversHolyFeastQuestStatus, LANGUAGESTRING.GodToBelieversHolyFeastQuestCompleted, LANGUAGESTRING.GodToBelieversHolyFeastQuestFailed);
		speech(QUESTTYPE.GIVEITEMS, LANGUAGESTRING.GodToBelieversGiveItemsQuestStarted, LANGUAGESTRING.GodToBelieversGiveBiblesQuestProgress, LANGUAGESTRING.GodToBelieversGiveItemsQuestStatus, LANGUAGESTRING.GodToBelieversGiveItemsQuestCompleted, LANGUAGESTRING.GodToBelieversGiveItemsQuestFailed);
		speech(QUESTTYPE.BURNBIBLES, LANGUAGESTRING.GodToBelieversBurnBiblesQuestStarted, LANGUAGESTRING.GodToBelieversBurnBiblesQuestProgress, LANGUAGESTRING.GodToBelieversBurnBiblesQuestStatus, LANGUAGESTRING.GodToBelieversBurnBiblesQuestCompleted, LANGUAGESTRING.GodToBelieversBurnBiblesQuestFailed);
		speech(QUESTTYPE.CRUSADE, LANGUAGESTRING.GodToBelieversCrusadeQuestStarted, LANGUAGESTRING.GodToBelieversCrusadeQuestProgress, LANGUAGESTRING.GodToBelieversCrusadeQuestStatus, LANGUAGESTRING.GodToBelieversCrusadeQuestCompleted, LANGUAGESTRING.GodToBelieversCrusadeQuestFailed);
		speech(QUESTTYPE.PVPREVENGE, LANGUAGESTRING.GodToBelieversPVPRevengeQuestStarted, LANGUAGESTRING.GodToBelieversPVPRevengeQuestProgress, LANGUAGESTRING.GodToBelieversPVPRevengeQuestStatus, LANGUAGESTRING.GodToBelieversPVPRevengeQuestCompleted, LANGUAGESTRING.GodToBelieversPVPRevengeQuestFailed);
		speech(QUESTTYPE.PILGRIMAGE, LANGUAGESTRING.GodToBelieversPilgrimageQuestStarted, LANGUAGESTRING.GodToBelieversPilgrimageQuestProgress, LANGUAGESTRING.GodToBelieversPilgrimageQuestStatus, LANGUAGESTRING.GodToBelieversPilgrimageQuestCompleted, LANGUAGESTRING.GodToBelieversPilgrimageQuestFailed);
		speech(QUESTTYPE.CLAIMHOLYLAND, LANGUAGESTRING.GodToBelieversClaimHolyLandQuestStarted, LANGUAGESTRING.GodToBelieversClaimHolyLandQuestProgress, LANGUAGESTRING.GodToBelieversClaimHolyLandQuestStatus, LANGUAGESTRING.GodToBelieversClaimHolyLandQuestCompleted, LANGUAGESTRING.GodToBelieversClaimHolyLandQuestFailed);
		speech(QUESTTYPE.GETHOLYARTIFACT, LANGUAGESTRING.GodToBelieversGetHolyArtifactQuestStarted, LANGUAGESTRING.GodToBelieversGetHolyArtifactQuestProgress, LANGUAGESTRING.GodToBelieversGetHolyArtifactQuestStatus, LANGUAGESTRING.GodToBelieversGetHolyArtifactQuestCompleted, LANGUAGESTRING.GodToBelieversGetHolyArtifactQuestFailed);
		speech(QUESTTYPE.HOLYWAR, LANGUAGESTRING.GodToBelieversHolywarQuestStarted, LANGUAGESTRING.GodToBelieversHolywarQuestProgress, LANGUAGESTRING.GodToBelieversHolywarQuestStatus, LANGUAGESTRING.GodToBelieversHolywarQuestCompleted, LANGUAGESTRING.GodToBelieversHolywarQuestFailed);
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

	private static QuestManager								instance;

	public static QuestManager instance()
	{
		if (instance == null)
			instance = new QuestManager();

		return instance;
	}

	private FileConfiguration			questsConfig		= null;
	private File						questsConfigFile	= null;

	private Random						random				= new Random();

	private HashMap<Material, Integer>	rewardValues		= new HashMap<Material, Integer>();

	private QuestManager()
	{
	}

	private boolean addQuestPlayerProgressForGod(String godName, UUID playerId)
	{
		boolean complete = false;

		List<String> players = this.questsConfig.getStringList(godName + ".Players");
		if (players.contains(playerId.toString()))
		{
			return false;
		}

		players.add(playerId.toString());

		int questAmount = this.questsConfig.getInt(godName + ".Amount");

		complete = players.size() >= questAmount;

		this.questsConfig.set(godName + ".Players", players);
		this.questsConfig.set(godName + ".Progress", Integer.valueOf(players.size()));

		save();

		return complete;
	}

	private boolean addQuestProgressForGod(String godName)
	{
		boolean complete = false;

		int questAmount = this.questsConfig.getInt(godName + ".Amount");
		int questProgress = this.questsConfig.getInt(godName + ".Progress");

		questProgress++;

		complete = questProgress >= questAmount;

		this.questsConfig.set(godName + ".Progress", Integer.valueOf(questProgress));

		save();

		return complete;
	}

	void clearQuestTarget(Location location)
	{
		int hash = hashVector(location);
		this.questsConfig.set("QuestTargets." + hash, null);

		this.save();
	}

	/**
	 * Quest: make non-believers read the god's holy book. Completed in {@link #handleReadBible}.
	 */
	private boolean generateGiveItemsQuest(String godName)
	{
		String bibleTitle = HolyBookManager.instance().getBibleTitle(godName);
		if (bibleTitle == null)
		{
			return false;
		}

		int nonBelievers = 0;
		for (Player player : Gods.instance().getServer().getOnlinePlayers())
		{
			if (BelieverManager.instance().getGodForBeliever(player.getUniqueId()) == null)
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

		this.questsConfig.set(godName + ".Type", questType.toString());
		this.questsConfig.set(godName + ".Amount", Integer.valueOf(questAmount));
		this.questsConfig.set(godName + ".TargetType", bibleTitle);
		this.questsConfig.set(godName + ".MaxDuration", Integer.valueOf(questMaxDuration));
		this.questsConfig.set(godName + ".Progress", null);
		this.questsConfig.set(godName + ".Players", null);
		this.questsConfig.set(godName + ".CreatedTime", formatter.format(thisDate));

		save();

		Gods.instance().log(godName + " issued a quest: Make " + questAmount + " non-believers read the Holy Book");

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

		Set<UUID> thisGodOnlineBelievers = BelieverManager.instance().getOnlineBelieversForGod(godName);
		if (thisGodOnlineBelievers.isEmpty())
		{
			Gods.instance().logDebug("No online believers for a pilgrimage quest for " + godName);
			return false;
		}

		UUID randomId = (UUID) thisGodOnlineBelievers.toArray()[this.random.nextInt(thisGodOnlineBelievers.size())];
		Player believer = Gods.instance().getServer().getPlayer(randomId);
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

		this.questsConfig.set(godName + ".Type", questType.toString());
		this.questsConfig.set(godName + ".TargetType", questTargetType);
		this.questsConfig.set(godName + ".Location.World", targetLocation.getWorld().getName());
		this.questsConfig.set(godName + ".Location.X", Integer.valueOf(targetLocation.getBlockX()));
		this.questsConfig.set(godName + ".Location.Y", Integer.valueOf(targetLocation.getBlockY()));
		this.questsConfig.set(godName + ".Location.Z", Integer.valueOf(targetLocation.getBlockZ()));
		this.questsConfig.set(godName + ".Location.Ground", groundType.name());
		this.questsConfig.set(godName + ".MaxDuration", Integer.valueOf(questMaxDuration));
		this.questsConfig.set(godName + ".Progress", null);
		this.questsConfig.set(godName + ".CreatedTime", formatter.format(thisDate));

		save();

		Gods.instance().log(godName + " issued a quest: Pilgrimage to " + questTargetType + " in " + targetLocation.getWorld().getName() + " !");

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

		if (GodsConfiguration.instance().isPilgrimageQuestsEnabled())
		{
			questTypes.add(QUESTTYPE.PILGRIMAGE);
		}
		if (GodsConfiguration.instance().isSlayQuestsEnabled())
		{
			questTypes.add(QUESTTYPE.SLAY);
		}
		if (GodsConfiguration.instance().isSacrificeQuestsEnabled() && GodsConfiguration.instance().isSacrificesEnabled())
		{
			questTypes.add(QUESTTYPE.ITEMSACRIFICE);
		}
		if (GodsConfiguration.instance().isGiveItemsQuestsEnabled() && GodsConfiguration.instance().isBiblesEnabled())
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

		GodManager.instance().setTimeSinceLastQuest(godName);

		if (newQuest)
		{
			godSayNewQuest(godName);
		}
		else
		{
			Gods.instance().logDebug("Could not generate any quest for " + godName);
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
		int questAmount = 1 + this.random.nextInt(1 + BelieverManager.instance().getOnlineBelieversForGod(godName).size());
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

		this.questsConfig.set(godName + ".Type", questType.toString());
		this.questsConfig.set(godName + ".Amount", Integer.valueOf(questAmount));
		this.questsConfig.set(godName + ".TargetType", questTargetType);
		this.questsConfig.set(godName + ".MaxDuration", Integer.valueOf(questMaxDuration));
		this.questsConfig.set(godName + ".Progress", null);
		this.questsConfig.set(godName + ".CreatedTime", formatter.format(thisDate));

		save();

		Gods.instance().log(godName + " issued a quest: Sacrifice " + questAmount + " " + questTargetType);

		return true;
	}

	private boolean generateSlayQuest(String godName)
	{
		Date thisDate = new Date();
		String pattern = "HH:mm dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);

		QUESTTYPE questType = QUESTTYPE.SLAY;
		int questAmount = 1 + this.random.nextInt(3) + BelieverManager.instance().getOnlineBelieversForGod(godName).size();
		int questMaxDuration = (5 + this.random.nextInt(5)) * questAmount;

		EntityType holyCreature = GodManager.instance().getHolyMobTypeForGod(godName);
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
				targetType = GodManager.instance().getUnholyMobTypeForGod(godName);
				break;
			}
		}
		while (targetType == null || targetType == holyCreature);

		this.questsConfig.set(godName + ".Type", questType.toString());
		this.questsConfig.set(godName + ".Amount", Integer.valueOf(questAmount));
		this.questsConfig.set(godName + ".TargetType", targetType.name());
		this.questsConfig.set(godName + ".MaxDuration", Integer.valueOf(questMaxDuration));
		this.questsConfig.set(godName + ".Progress", null);
		this.questsConfig.set(godName + ".CreatedTime", formatter.format(thisDate));

		save();

		Gods.instance().log(godName + " issued a quest: Kill " + questAmount + " " + targetType.name());

		return true;
	}

	/**
	 * Finds a spot for a pilgrimage chest between minDist and maxDist blocks from center, or returns null. Returns the location where the chest goes.
	 */
	public Location getLocationForPilgrimageQuest(String worldName, int minDist, int maxDist, Location center)
	{
		Block chestBlock = findPilgrimageChestBlock(Gods.instance().getServer().getWorld(worldName), minDist, maxDist, center);

		return (chestBlock == null) ? null : chestBlock.getLocation();
	}

	/**
	 * Looks for flat, open, unprotected ground in a fitting biome of an overworld. Tries at most MAX_LOCATION_ATTEMPTS spots and loads at most
	 * MAX_CHUNK_LOADS chunks that weren't loaded already. Returns the block where the chest goes, or null.
	 */
	private Block findPilgrimageChestBlock(World world, int minDist, int maxDist, Location center)
	{
		if (world == null || world.getEnvironment() != World.Environment.NORMAL || !Gods.instance().isEnabledInWorld(world))
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

			if (isProtectedByWorldGuard(world, chestBlock))
			{
				continue;
			}

			Gods.instance().logDebug("Pilgrimage Holy mountain found in " + run + " runs (" + chunkLoads + " chunk loads): " + chestBlock.getX() + "," + chestBlock.getY() + "," + chestBlock.getZ());

			return chestBlock;
		}

		Gods.instance().log("Pilgrimage Holy mountain generation FAILED in " + world.getName());

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

	private boolean isProtectedByWorldGuard(World world, Block block)
	{
		if (getWorldGuard() == null)
		{
			return false;
		}

		RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
		if (regionManager == null)
		{
			return false;
		}

		return regionManager.getApplicableRegions(BlockVector3.at(block.getX(), block.getY(), block.getZ())).size() > 0;
	}

	public int getQuestAmountForGod(String godName)
	{
		int value = this.questsConfig.getInt(godName + ".Amount");

		return value;
	}

	public Date getQuestCreatedTimeForGod(String godName)
	{
		String pattern = "HH:mm dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		String startTime = this.questsConfig.getString(godName + ".CreatedTime");
		Date startTimeDate = null;
		try
		{
			startTimeDate = formatter.parse(startTime);
		}
		catch (Exception ex)
		{
			Gods.instance().log("Invalid quest created date for " + godName + ". Reset.");
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
		String worldName = this.questsConfig.getString(godName + ".Location.World");

		if (worldName == null)
		{
			return null;
		}

		int x = this.questsConfig.getInt(godName + ".Location.X");
		int y = this.questsConfig.getInt(godName + ".Location.Y");
		int z = this.questsConfig.getInt(godName + ".Location.Z");

		return new Location(Gods.instance().getServer().getWorld(worldName), x, y, z);
	}

	public int getQuestMaxDurationForGod(String godName)
	{
		int value = this.questsConfig.getInt(godName + ".MaxDuration");

		return value;
	}

	public int getQuestProgressForGod(String godName)
	{
		int value = this.questsConfig.getInt(godName + ".Progress");

		return value;
	}

	String getQuestTargetGod(Location location)
	{
		return this.questsConfig.getString("QuestTargets." + hashVector(location) + ".God");
	}

	public String getQuestTargetTypeForGod(String godName)
	{
		String value = this.questsConfig.getString(godName + ".TargetType");

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
				return LanguageManager.instance().getMobTypeName(EntityType.valueOf(targetType.toUpperCase()));
			case ITEMSACRIFICE:
			case BURNBIBLES:
				Material material = Material.matchMaterial(targetType);
				String itemName = (material == null) ? null : LanguageManager.instance().getItemTypeName(material);
				return (itemName == null) ? targetType : itemName;
			case GIVEITEMS:
				String bibleTitle = HolyBookManager.instance().getBibleTitle(godName);
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
		String typeName = this.questsConfig.getString(godName + ".Type");
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
			Gods.instance().logDebug("Could not recognize quest type '" + typeName + "' for " + godName + ": " + ex.getMessage());
			return QUESTTYPE.NONE;
		}
	}

	/**
	 * Random reward items worth about power in total.
	 */
	public List<ItemStack> getRewardsForQuestCompletion(int power)
	{
		List<ItemStack> rewards = new ArrayList<ItemStack>();

		if (this.rewardValues.isEmpty())
		{
			resetItemRewardValues();
		}

		List<Map.Entry<Material, Integer>> rewardItems = new ArrayList<Map.Entry<Material, Integer>>();
		for (Map.Entry<Material, Integer> entry : this.rewardValues.entrySet())
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
		return getRewardsForQuestCompletion((int) GodManager.instance().getGodPower(godName));
	}

	public WorldGuardPlugin getWorldGuard()
	{
		Plugin worldGuardPlugin = Gods.instance().getServer().getPluginManager().getPlugin("WorldGuard");

		if (!(worldGuardPlugin instanceof WorldGuardPlugin))
		{
			return null;
		}

		return (WorldGuardPlugin) worldGuardPlugin;
	}

	// Sets $Amount and $Type for a quest message. LanguageManager is shared, so call it right before speaking.
	private void setQuestSpeechValues(String godName, QUESTTYPE questType, int amount)
	{
		LanguageManager.instance().setAmount(amount);

		String targetName = getQuestTargetDisplayName(godName, questType);
		try
		{
			LanguageManager.instance().setType((targetName == null) ? "" : targetName);
		}
		catch (Exception ex)
		{
			Gods.instance().logDebug(ex.getMessage());
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
		GodManager.instance().godSayToBelievers(godName, message, 2 + this.random.nextInt(100));
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
		GodManager.instance().godSayToBelievers(godName, message, 2 + this.random.nextInt(100));

		switch (questType)
		{
		case PILGRIMAGE:
			GodManager.instance().sendInfoToBelievers(godName, LANGUAGESTRING.QuestTargetHelp, ChatColor.AQUA, 150);
			break;
		case ITEMSACRIFICE:
			GodManager.instance().sendInfoToBelievers(godName, LANGUAGESTRING.SacrificeHelp, ChatColor.AQUA, 150);
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
		GodManager.instance().godSayToBelievers(godName, message, 2 + this.random.nextInt(10));
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
				GodManager.instance().godSayToBelievers(godName, message, 2 + this.random.nextInt(100));
			}
			sendQuestTargetHints(godName);
			break;
		default:
			GodManager.instance().godSayToBelievers(godName, message, 2 + this.random.nextInt(100));
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

		for (UUID believerId : BelieverManager.instance().getBelieversForGod(godName))
		{
			Player player = Gods.instance().getServer().getPlayer(believerId);

			if (player == null)
			{
				continue;
			}

			if (!BelieverManager.instance().isHunting(believerId))
			{
				if (this.random.nextInt(10) == 0)
				{
					Gods.instance().sendInfo(believerId, LANGUAGESTRING.QuestTargetHelp, ChatColor.AQUA, 0, "", 20);
				}
				continue;
			}

			if (!targetLocation.getWorld().equals(player.getWorld()))
			{
				continue;
			}

			int distance = (int) targetLocation.toVector().distance(player.getLocation().toVector());

			Gods.instance().sendInfo(believerId, LANGUAGESTRING.QuestTargetRange, ChatColor.AQUA, distance, "", 20);
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
			GodManager.instance().godSayToBelievers(godName, LANGUAGESTRING.GodToBelieversBuildAltarsQuestCompleted, 2 + this.random.nextInt(10));
			GodManager.instance().addBeliefAndRewardBelievers(godName);

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

		Gods.instance().logDebug("handling quest eating for " + godName);

		if (addQuestProgressForGod(godName))
		{
			GodManager.instance().godSayToBelievers(godName, LANGUAGESTRING.GodToBelieversHolyFeastQuestCompleted, 2 + this.random.nextInt(10));

			GodManager.instance().addMoodForGod(godName, 2.0F * GodManager.instance().getPleasedModifierForGod(godName));
			GodManager.instance().addBeliefAndRewardBelievers(godName);

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
			LanguageManager.instance().setPlayerName(playerName);
			GodManager.instance().godSayToBelievers(godName, LANGUAGESTRING.GodToBelieversConvertQuestCompleted, 2 + this.random.nextInt(10));

			GodManager.instance().addMoodForGod(godName, 1.0F * GodManager.instance().getPleasedModifierForGod(godName));
			GodManager.instance().addBeliefAndRewardBelievers(godName);

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
			GodManager.instance().godSayToBelievers(godName, LANGUAGESTRING.GodToBelieversSlayQuestCompleted, 2 + this.random.nextInt(10));

			GodManager.instance().addMoodForGod(godName, 2.0F * GodManager.instance().getPleasedModifierForGod(godName));
			GodManager.instance().addBeliefAndRewardBelievers(godName);

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
		String godName = BelieverManager.instance().getGodForBeliever(playerId);

		// Only believers of the god who set the pilgrimage may open its chest
		if (questGod == null || godName == null || !questGod.equals(godName))
		{
			return true;
		}

		try
		{
			LanguageManager.instance().setType(getQuestTargetTypeForGod(questGod));
		}
		catch (Exception ex)
		{
			Gods.instance().logDebug(ex.getMessage());
		}

		GodManager.instance().godSayToBelievers(questGod, LANGUAGESTRING.GodToBelieversPilgrimageQuestCompleted, 2);

		GodManager.instance().addMoodForGod(godName, 2.0F * GodManager.instance().getPleasedModifierForGod(godName));
		GodManager.instance().addBeliefAndRewardBelievers(godName);
		BelieverManager.instance().increasePrayerPower(playerId, 1);

		Player player = Gods.instance().getServer().getPlayer(playerId);
		if (player != null)
		{
			Gods.instance().getServer().broadcastMessage(ChatColor.GOLD + player.getDisplayName() + ChatColor.AQUA + " completed a pilgrimage to " + ChatColor.GOLD + getQuestTargetTypeForGod(questGod));
		}

		BelieverManager.instance().setHunting(playerId, false);

		removeSuccessQuestForGod(godName);

		return false;
	}

	public void handlePrayer(String godName, UUID playerId)
	{
		if (!hasQuest(godName) || getQuestTypeForGod(godName) != QUESTTYPE.CONVERT)
		{
			return;
		}

		String playerGod = BelieverManager.instance().getGodForBeliever(playerId);

		if ((playerGod != null) && (playerGod.equals(godName)))
		{
			return;
		}

		Player player = Gods.instance().getServer().getPlayer(playerId);

		if (addQuestPlayerProgressForGod(godName, playerId))
		{
			LanguageManager.instance().setPlayerName((player != null) ? player.getDisplayName() : "");
			GodManager.instance().godSayToBelievers(godName, LANGUAGESTRING.GodToBelieversConvertQuestCompleted, 2 + this.random.nextInt(10));

			GodManager.instance().addMoodForGod(godName, 1.0F * GodManager.instance().getPleasedModifierForGod(godName));
			GodManager.instance().addBeliefAndRewardBelievers(godName);

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
		if (BelieverManager.instance().getGodForBeliever(playerId) != null)
		{
			return;
		}

		Player player = Gods.instance().getServer().getPlayer(playerId);

		boolean complete = addQuestPlayerProgressForGod(godName, playerId);

		LanguageManager.instance().setPlayerName((player != null) ? player.getDisplayName() : "");

		if (complete)
		{
			setQuestSpeechValues(godName, QUESTTYPE.GIVEITEMS, 0);
			GodManager.instance().godSayToBelievers(godName, LANGUAGESTRING.GodToBelieversGiveItemsQuestCompleted, 2 + this.random.nextInt(10));

			GodManager.instance().addMoodForGod(godName, 2.0F * GodManager.instance().getPleasedModifierForGod(godName));
			GodManager.instance().addBeliefAndRewardBelievers(godName);

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
		BelieverManager.instance().increasePrayerPower(player.getUniqueId(), 1);

		if (complete)
		{
			setQuestSpeechValues(godName, questType, 0);
			LanguageManager.instance().setPlayerName(player.getDisplayName());
			GodManager.instance().godSayToBelievers(godName, questSpeech(questType, Phase.COMPLETED), 2 + this.random.nextInt(10));

			GodManager.instance().addMoodForGod(godName, 2.0F * GodManager.instance().getPleasedModifierForGod(godName));
			GodManager.instance().addBeliefAndRewardBelievers(godName);

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
		String quest = this.questsConfig.getString(godName + ".Type");

		return quest != null;
	}

	boolean isQuestTarget(Location location)
	{
		int hash = hashVector(location);

		String questTargetLocationWorld = this.questsConfig.getString("QuestTargets." + hash + ".Location.World");

		if (questTargetLocationWorld == null || location.getWorld() == null)
		{
			return false;
		}

		int questTargetLocationX = this.questsConfig.getInt("QuestTargets." + hash + ".Location.X");
		int questTargetLocationY = this.questsConfig.getInt("QuestTargets." + hash + ".Location.Y");
		int questTargetLocationZ = this.questsConfig.getInt("QuestTargets." + hash + ".Location.Z");

		return location.getBlockX() == questTargetLocationX && location.getBlockY() == questTargetLocationY && location.getBlockZ() == questTargetLocationZ && location.getWorld().getName().equals(questTargetLocationWorld);
	}

	public void load()
	{
		if (this.questsConfigFile == null)
		{
			this.questsConfigFile = new File(Gods.instance().getDataFolder(), "quests.yml");
		}
		this.questsConfig = YamlConfiguration.loadConfiguration(this.questsConfigFile);

		Gods.instance().log("Loaded " + this.questsConfig.getKeys(false).size() + " quests.");
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
			Gods.instance().log("Could not remove the pilgrimage chest of " + godName + ": " + ex.getMessage());
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
			String groundName = this.questsConfig.getString(godName + ".Location.Ground");
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

		this.questsConfig.set(godName, null);

		save();
	}

	public void clearItemRewardValues()
	{
		this.rewardValues.clear();
	}

	public void resetItemRewardValues()
	{
		this.rewardValues.clear();
		this.rewardValues.put(Material.NETHER_WART, 33);
		this.rewardValues.put(Material.GLOWSTONE, 50);
		this.rewardValues.put(Material.COAL, 5);
		this.rewardValues.put(Material.IRON_INGOT, 10);
		this.rewardValues.put(Material.GOLD_INGOT, 50);
		this.rewardValues.put(Material.DIAMOND, 100);
		this.rewardValues.put(Material.PLAYER_HEAD, 75);
		this.rewardValues.put(Material.ENCHANTED_BOOK, 75);
		this.rewardValues.put(Material.POTION, 75);
		this.rewardValues.put(Material.COCOA, 30);
		this.rewardValues.put(Material.BLAZE_ROD, 50);
		this.rewardValues.put(Material.BOOK, 30);
		this.rewardValues.put(Material.COMPASS, 50);
		this.rewardValues.put(Material.PAPER, 20);
		this.rewardValues.put(Material.SUGAR_CANE, 25);
		this.rewardValues.put(Material.MELON_SEEDS, 5);
		this.rewardValues.put(Material.PUMPKIN_SEEDS, 5);
		this.rewardValues.put(Material.POPPY, 1);
		this.rewardValues.put(Material.SUNFLOWER, 1);
	}

	public void save()
	{
		if ((this.questsConfig == null) || (this.questsConfigFile == null))
		{
			return;
		}
		try
		{
			this.questsConfig.save(this.questsConfigFile);
		}
		catch (Exception ex)
		{
			Gods.instance().log("Could not save config to " + this.questsConfigFile.getName() + ": " + ex.getMessage());
		}
	}

	public void setItemRewardValue(Material item, int value)
	{
		this.rewardValues.put(item, Integer.valueOf(value));
	}

	void setQuestTarget(String godName, Location location)
	{
		int hash = hashVector(location);

		this.questsConfig.set("QuestTargets." + hash + ".God", godName);
		this.questsConfig.set("QuestTargets." + hash + ".Location.X", location.getBlockX());
		this.questsConfig.set("QuestTargets." + hash + ".Location.Y", location.getBlockY());
		this.questsConfig.set("QuestTargets." + hash + ".Location.Z", location.getBlockZ());
		this.questsConfig.set("QuestTargets." + hash + ".Location.World", location.getWorld().getName());

		this.save();
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
