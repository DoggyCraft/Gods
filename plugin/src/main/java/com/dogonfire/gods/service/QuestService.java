package com.dogonfire.gods.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.managers.LanguageManager.LANGUAGESTRING;
import com.dogonfire.gods.model.Quest;
import com.dogonfire.gods.model.QuestLocation;
import com.dogonfire.gods.model.QuestTarget;
import com.dogonfire.gods.model.QuestType;
import com.dogonfire.gods.storage.QuestRepository;
import com.dogonfire.gods.util.LegacyDates;

/**
 * Gods' quests: making a new one (only PILGRIMAGE, SLAY, ITEMSACRIFICE and GIVEITEMS, each only while it is enabled),
 * counting progress when believers act, completing and failing them, the rewards, and what the gods say about them.
 * Owns every change to quests.yml ({@link QuestRepository}); every change is written at once, as before. The numbers are
 * in {@link QuestRules}, the pilgrimage chests in {@link PilgrimageSites}. Its data is loaded always (a god keeps its
 * quest while quests are switched off). Main thread only.
 */
public class QuestService
{
	private enum Phase
	{
		STARTED, PROGRESS, STATUS, COMPLETED, FAILED
	}

	// What each god says about a quest, indexed by Phase. Types without speech have no entry.
	private static final Map<QuestType, LANGUAGESTRING[]>	QUEST_SPEECH	= new EnumMap<QuestType, LANGUAGESTRING[]>(QuestType.class);

	static
	{
		speech(QuestType.SLAY, LANGUAGESTRING.GodToBelieversSlayQuestStarted, LANGUAGESTRING.GodToBelieversSlayQuestProgress, LANGUAGESTRING.GodToBelieversSlayQuestStatus, LANGUAGESTRING.GodToBelieversSlayQuestCompleted, LANGUAGESTRING.GodToBelieversSlayQuestFailed);
		speech(QuestType.CONVERT, LANGUAGESTRING.GodToBelieversConvertQuestStarted, LANGUAGESTRING.GodToBelieversConvertQuestProgress, LANGUAGESTRING.GodToBelieversConvertQuestStatus, LANGUAGESTRING.GodToBelieversConvertQuestCompleted, LANGUAGESTRING.GodToBelieversConvertQuestFailed);
		speech(QuestType.BUILDALTARS, LANGUAGESTRING.GodToBelieversBuildAltarsQuestStarted, LANGUAGESTRING.GodToBelieversBuildAltarsQuestProgress, LANGUAGESTRING.GodToBelieversBuildAltarsQuestStatus, LANGUAGESTRING.GodToBelieversBuildAltarsQuestCompleted, LANGUAGESTRING.GodToBelieversBuildAltarsQuestFailed);
		speech(QuestType.BUILDTOWER, LANGUAGESTRING.GodToBelieversBuildTowerQuestStarted, LANGUAGESTRING.GodToBelieversBuildTowerQuestProgress, LANGUAGESTRING.GodToBelieversBuildTowerQuestStatus, LANGUAGESTRING.GodToBelieversBuildTowerQuestCompleted, LANGUAGESTRING.GodToBelieversBuildTowerQuestFailed);
		speech(QuestType.ITEMSACRIFICE, LANGUAGESTRING.GodToBelieversSacrificeQuestStarted, LANGUAGESTRING.GodToBelieversSacrificeQuestProgress, LANGUAGESTRING.GodToBelieversSacrificeQuestStatus, LANGUAGESTRING.GodToBelieversSacrificeQuestCompleted, LANGUAGESTRING.GodToBelieversSacrificeQuestFailed);
		speech(QuestType.HOLYFEAST, LANGUAGESTRING.GodToBelieversHolyFeastQuestStarted, LANGUAGESTRING.GodToBelieversHolyFeastQuestProgress, LANGUAGESTRING.GodToBelieversHolyFeastQuestStatus, LANGUAGESTRING.GodToBelieversHolyFeastQuestCompleted, LANGUAGESTRING.GodToBelieversHolyFeastQuestFailed);
		speech(QuestType.GIVEITEMS, LANGUAGESTRING.GodToBelieversGiveItemsQuestStarted, LANGUAGESTRING.GodToBelieversGiveBiblesQuestProgress, LANGUAGESTRING.GodToBelieversGiveItemsQuestStatus, LANGUAGESTRING.GodToBelieversGiveItemsQuestCompleted, LANGUAGESTRING.GodToBelieversGiveItemsQuestFailed);
		speech(QuestType.BURNBIBLES, LANGUAGESTRING.GodToBelieversBurnBiblesQuestStarted, LANGUAGESTRING.GodToBelieversBurnBiblesQuestProgress, LANGUAGESTRING.GodToBelieversBurnBiblesQuestStatus, LANGUAGESTRING.GodToBelieversBurnBiblesQuestCompleted, LANGUAGESTRING.GodToBelieversBurnBiblesQuestFailed);
		speech(QuestType.CRUSADE, LANGUAGESTRING.GodToBelieversCrusadeQuestStarted, LANGUAGESTRING.GodToBelieversCrusadeQuestProgress, LANGUAGESTRING.GodToBelieversCrusadeQuestStatus, LANGUAGESTRING.GodToBelieversCrusadeQuestCompleted, LANGUAGESTRING.GodToBelieversCrusadeQuestFailed);
		speech(QuestType.PILGRIMAGE, LANGUAGESTRING.GodToBelieversPilgrimageQuestStarted, LANGUAGESTRING.GodToBelieversPilgrimageQuestProgress, LANGUAGESTRING.GodToBelieversPilgrimageQuestStatus, LANGUAGESTRING.GodToBelieversPilgrimageQuestCompleted, LANGUAGESTRING.GodToBelieversPilgrimageQuestFailed);
		speech(QuestType.CLAIMHOLYLAND, LANGUAGESTRING.GodToBelieversClaimHolyLandQuestStarted, LANGUAGESTRING.GodToBelieversClaimHolyLandQuestProgress, LANGUAGESTRING.GodToBelieversClaimHolyLandQuestStatus, LANGUAGESTRING.GodToBelieversClaimHolyLandQuestCompleted, LANGUAGESTRING.GodToBelieversClaimHolyLandQuestFailed);
	}

	private static void speech(QuestType type, LANGUAGESTRING started, LANGUAGESTRING progress, LANGUAGESTRING status, LANGUAGESTRING completed, LANGUAGESTRING failed)
	{
		QUEST_SPEECH.put(type, new LANGUAGESTRING[] { started, progress, status, completed, failed });
	}

	private static LANGUAGESTRING questSpeech(QuestType type, Phase phase)
	{
		LANGUAGESTRING[] strings = QUEST_SPEECH.get(type);
		return (strings == null) ? null : strings[phase.ordinal()];
	}

	private static final LegacyDates	DATES	= LegacyDates.MINUTES;

	private final GodsContext			ctx;
	private final QuestRepository		repository;
	private final Random				random	= new Random();
	private final PilgrimageSites		pilgrimageSites;

	public QuestService(GodsContext ctx, QuestRepository repository)
	{
		this.ctx = ctx;
		this.repository = repository;
		this.pilgrimageSites = new PilgrimageSites(ctx, this.random);
	}

	public void load()
	{
		this.repository.load();
	}

	// ---------------------------------------------------------------------
	// The current quest

	/** The god's quest, or null if it has none */
	private Quest quest(String godName)
	{
		Quest quest = this.repository.find(godName);
		return (quest != null && quest.typeName() != null) ? quest : null;
	}

	public boolean hasQuest(String godName)
	{
		return quest(godName) != null;
	}

	private QuestType questType(String godName)
	{
		Quest quest = quest(godName);
		if (quest == null)
		{
			return QuestType.NONE;
		}
		QuestType type = quest.type();
		if (type == QuestType.NONE)
		{
			ctx.plugin().logDebug("Could not recognize quest type '" + quest.typeName() + "' for " + godName);
		}
		return type;
	}

	private int amount(String godName)
	{
		Quest quest = this.repository.find(godName);
		return (quest != null) ? quest.amountOrZero() : 0;
	}

	private int progress(String godName)
	{
		Quest quest = this.repository.find(godName);
		return (quest != null) ? quest.progressOrZero() : 0;
	}

	private String targetType(String godName)
	{
		Quest quest = this.repository.find(godName);
		return (quest != null) ? quest.targetType() : null;
	}

	public boolean hasExpiredQuest(String godName)
	{
		Quest quest = this.repository.find(godName);
		if (quest == null || quest.createdTime() == null)
		{
			ctx.plugin().log("Invalid quest created date for " + godName + ". Reset.");
		}
		return QuestRules.isExpired((quest != null) ? quest.createdTime() : null, (quest != null) ? quest.maxDurationOrZero() : 0, java.time.Instant.now());
	}

	/** The quest target of the god, or null if it has none or its world isn't loaded */
	public Location getQuestLocation(String godName)
	{
		Location location = storedQuestLocation(godName);

		if (location == null || location.getWorld() == null)
		{
			return null;
		}

		return location;
	}

	// The stored quest target; its world is null if that world isn't loaded
	private Location storedQuestLocation(String godName)
	{
		Quest quest = this.repository.find(godName);
		QuestLocation location = (quest != null) ? quest.location() : null;

		if (location == null)
		{
			return null;
		}

		return new Location(ctx.plugin().getServer().getWorld(location.world()), location.x(), location.y(), location.z());
	}

	/** Counts one more; true if that completes the quest */
	private boolean addProgress(String godName)
	{
		Quest quest = this.repository.getOrCreate(godName);
		int progress = quest.progressOrZero() + 1;

		quest.setProgress(progress);
		this.repository.save();

		return progress >= quest.amountOrZero();
	}

	/** Counts the player once; true if that completes the quest. False if the player counted already. */
	private boolean addPlayerProgress(String godName, UUID playerId)
	{
		Quest quest = this.repository.getOrCreate(godName);

		List<String> players = quest.playersOrEmpty();
		if (players.contains(playerId.toString()))
		{
			return false;
		}

		players.add(playerId.toString());

		quest.setPlayers(players);
		quest.setProgress(players.size());
		this.repository.save();

		return players.size() >= quest.amountOrZero();
	}

	// ---------------------------------------------------------------------
	// Quest targets

	private void setQuestTarget(String godName, Location location)
	{
		QuestLocation at = new QuestLocation(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), null);
		this.repository.putTarget(new QuestTarget(QuestTarget.keyFor(at), godName, at));
		this.repository.save();
	}

	private void clearQuestTarget(Location location)
	{
		this.repository.removeTarget(QuestTarget.keyFor(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
		this.repository.save();
	}

	private QuestTarget targetAt(Location location)
	{
		return this.repository.target(QuestTarget.keyFor(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
	}

	private boolean isQuestTarget(Location location)
	{
		QuestTarget target = targetAt(location);

		if (target == null || target.location() == null || location.getWorld() == null)
		{
			return false;
		}

		return target.location().isAt(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
	}

	// ---------------------------------------------------------------------
	// New quests

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

		List<QuestType> questTypes = new ArrayList<QuestType>();

		if (ctx.settings().quests().pilgrimage())
		{
			questTypes.add(QuestType.PILGRIMAGE);
		}
		if (ctx.settings().quests().slay())
		{
			questTypes.add(QuestType.SLAY);
		}
		if (ctx.settings().quests().sacrifice() && ctx.settings().sacrifices().enabled())
		{
			questTypes.add(QuestType.ITEMSACRIFICE);
		}
		if (ctx.settings().quests().giveItems() && ctx.settings().bibles().enabled())
		{
			questTypes.add(QuestType.GIVEITEMS);
		}

		Collections.shuffle(questTypes, this.random);

		boolean newQuest = false;

		for (QuestType questType : questTypes)
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

		ctx.godService().setTimeSinceLastQuest(godName);

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

	/** Sets what every new quest sets, keeping what the god's section has besides */
	private Quest newQuest(String godName, QuestType type, String targetType, Integer amount, int maxDuration)
	{
		Quest quest = this.repository.getOrCreate(godName);
		quest.setType(type);
		if (amount != null)
		{
			quest.setAmount(amount);
		}
		quest.setTargetType(targetType);
		quest.setMaxDuration(maxDuration);
		quest.setProgress(null);
		quest.setCreatedTime(DATES.now());
		return quest;
	}

	/** Quest: make non-believers read the god's holy book. Completed in {@link #handleReadBible}. */
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

		QuestRules.Size size = QuestRules.giveItems(this.random, nonBelievers);

		Quest quest = newQuest(godName, QuestType.GIVEITEMS, bibleTitle, size.amount(), size.maxDurationMinutes());
		quest.setPlayers(null);
		this.repository.save();

		ctx.plugin().log(godName + " issued a quest: Make " + size.amount() + " non-believers read the Holy Book");

		return true;
	}

	private boolean generatePilgrimageQuest(String godName)
	{
		String questTargetType = QuestRules.holyMountainName(this.random);
		int questMaxDuration = QuestRules.pilgrimageMinutes(this.random);

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

		Block chestBlock = this.pilgrimageSites.find(believer.getWorld(), QuestRules.PILGRIMAGE_MIN_DISTANCE, QuestRules.PILGRIMAGE_MAX_DISTANCE, believer.getLocation());
		if (chestBlock == null)
		{
			return false;
		}

		List<ItemStack> rewards = getRewardsForQuestCompletion(QuestRules.pilgrimageRewardPower(this.random));
		Material groundType = chestBlock.getRelative(BlockFace.DOWN).getType();

		if (!this.pilgrimageSites.placeChest(chestBlock, rewards))
		{
			return false;
		}

		Location targetLocation = chestBlock.getLocation();

		setQuestTarget(godName, targetLocation);

		Quest quest = newQuest(godName, QuestType.PILGRIMAGE, questTargetType, null, questMaxDuration);
		quest.setLocation(new QuestLocation(targetLocation.getWorld().getName(), targetLocation.getBlockX(), targetLocation.getBlockY(), targetLocation.getBlockZ(), groundType.name()));
		this.repository.save();

		ctx.plugin().log(godName + " issued a quest: Pilgrimage to " + questTargetType + " in " + targetLocation.getWorld().getName() + " !");

		return true;
	}

	private boolean generateSacrificeQuest(String godName)
	{
		QuestRules.Size size = QuestRules.sacrifice(this.random, ctx.believerService().getOnlineBelieversForGod(godName).size());
		String questTargetType = QuestRules.sacrificeTarget(this.random);

		newQuest(godName, QuestType.ITEMSACRIFICE, questTargetType, size.amount(), size.maxDurationMinutes());
		this.repository.save();

		ctx.plugin().log(godName + " issued a quest: Sacrifice " + size.amount() + " " + questTargetType);

		return true;
	}

	private boolean generateSlayQuest(String godName)
	{
		QuestRules.Size size = QuestRules.slay(this.random, ctx.believerService().getOnlineBelieversForGod(godName).size());

		List<EntityType> common = new ArrayList<EntityType>();
		for (String name : QuestRules.SLAY_TARGETS)
		{
			common.add(EntityType.valueOf(name));
		}
		EntityType holyCreature = ctx.commandmentService().getHolyMobTypeForGod(godName);
		EntityType targetType = QuestRules.slayTarget(this.random, common, () -> ctx.commandmentService().getUnholyMobTypeForGod(godName), holyCreature);

		newQuest(godName, QuestType.SLAY, targetType.name(), size.amount(), size.maxDurationMinutes());
		this.repository.save();

		ctx.plugin().log(godName + " issued a quest: Kill " + size.amount() + " " + targetType.name());

		return true;
	}

	// ---------------------------------------------------------------------
	// Rewards

	/** Random reward items worth about power in total */
	public List<ItemStack> getRewardsForQuestCompletion(int power)
	{
		List<Map.Entry<Material, Integer>> rewardItems = new ArrayList<Map.Entry<Material, Integer>>();
		for (Map.Entry<Material, Integer> entry : ctx.settings().quests().rewardValues().entrySet())
		{
			if (entry.getKey() != null && entry.getKey().isItem() && entry.getValue() != null && entry.getValue() > 0)
			{
				rewardItems.add(entry);
			}
		}

		List<ItemStack> rewards = new ArrayList<ItemStack>();
		for (Material material : QuestRules.rollRewards(this.random, rewardItems, power))
		{
			rewards.add(new ItemStack(material, 1));
		}
		return rewards;
	}

	/** Rewards worth the god's power */
	public List<ItemStack> getRewardsForQuestCompletion(String godName)
	{
		return getRewardsForQuestCompletion((int) ctx.godService().getGodPower(godName));
	}

	// ---------------------------------------------------------------------
	// What the gods say

	// The quest target as the gods should say it: mob and item names from the language file, the holy book title, ...
	private String targetDisplayName(String godName, QuestType questType)
	{
		String targetType = targetType(godName);

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

	// Sets $Amount and $Type for a quest message. LanguageManager is shared, so call it right before speaking.
	private void setQuestSpeechValues(String godName, QuestType questType, int amount)
	{
		ctx.languageManager().setAmount(amount);

		String targetName = targetDisplayName(godName, questType);
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
		QuestType questType = questType(godName);
		LANGUAGESTRING message = questSpeech(questType, Phase.FAILED);

		if (message == null)
		{
			return;
		}

		setQuestSpeechValues(godName, questType, amount(godName) - progress(godName));
		ctx.godSpeech().sayToBelievers(godName, message, 2 + this.random.nextInt(100));
	}

	private void godSayNewQuest(String godName)
	{
		QuestType questType = questType(godName);
		LANGUAGESTRING message = questSpeech(questType, Phase.STARTED);

		if (message == null)
		{
			return;
		}

		setQuestSpeechValues(godName, questType, amount(godName));
		ctx.godSpeech().sayToBelievers(godName, message, 2 + this.random.nextInt(100));

		switch (questType)
		{
		case PILGRIMAGE:
			ctx.godSpeech().sendInfoToBelievers(godName, LANGUAGESTRING.QuestTargetHelp, ChatColor.AQUA, 150);
			break;
		case ITEMSACRIFICE:
			ctx.godSpeech().sendInfoToBelievers(godName, LANGUAGESTRING.SacrificeHelp, ChatColor.AQUA, 150);
			break;
		default:
			break;
		}
	}

	private void godSayProgress(String godName)
	{
		QuestType questType = questType(godName);
		LANGUAGESTRING message = questSpeech(questType, Phase.PROGRESS);

		if (message == null)
		{
			return;
		}

		setQuestSpeechValues(godName, questType, amount(godName) - progress(godName));
		ctx.godSpeech().sayToBelievers(godName, message, 2 + this.random.nextInt(10));
	}

	public void godSayStatus(String godName)
	{
		QuestType questType = questType(godName);
		LANGUAGESTRING message = questSpeech(questType, Phase.STATUS);

		if (message == null)
		{
			return;
		}

		setQuestSpeechValues(godName, questType, amount(godName) - progress(godName));

		switch (questType)
		{
		case PILGRIMAGE:
		case SLAYDRAGON:
			// Quests with a place to find: remind now and then, and tell hunting believers how far they are
			if (this.random.nextInt(6) == 0)
			{
				ctx.godSpeech().sayToBelievers(godName, message, 2 + this.random.nextInt(100));
			}
			sendQuestTargetHints(godName);
			break;
		default:
			ctx.godSpeech().sayToBelievers(godName, message, 2 + this.random.nextInt(100));
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
					ctx.godSpeech().sendInfo(believerId, LANGUAGESTRING.QuestTargetHelp, ChatColor.AQUA, 0, "", 20);
				}
				continue;
			}

			if (!targetLocation.getWorld().equals(player.getWorld()))
			{
				continue;
			}

			int distance = (int) targetLocation.toVector().distance(player.getLocation().toVector());

			ctx.godSpeech().sendInfo(believerId, LANGUAGESTRING.QuestTargetRange, ChatColor.AQUA, distance, "", 20);
		}
	}

	// ---------------------------------------------------------------------
	// What believers do

	/** Pleases the god, rewards its believers and ends the quest */
	private void complete(String godName, float moodFactor)
	{
		ctx.moodService().addMoodForGod(godName, moodFactor * ctx.moodService().getPleasedModifierForGod(godName));
		ctx.prayerService().addBeliefAndRewardBelievers(godName);

		removeSuccessQuestForGod(godName);
	}

	public void handleBuiltPrayingAltar(String godName)
	{
		if (!hasQuest(godName) || questType(godName) != QuestType.BUILDALTARS)
		{
			return;
		}

		if (addProgress(godName))
		{
			ctx.godSpeech().sayToBelievers(godName, LANGUAGESTRING.GodToBelieversBuildAltarsQuestCompleted, 2 + this.random.nextInt(10));
			ctx.prayerService().addBeliefAndRewardBelievers(godName);

			removeSuccessQuestForGod(godName);
		}
		else
		{
			godSayProgress(godName);
		}
	}

	public void handleEat(String playerName, String godName, String entityType)
	{
		if (!hasQuest(godName) || questType(godName) != QuestType.HOLYFEAST)
		{
			return;
		}

		String questTargetType = targetType(godName);
		if ((questTargetType == null) || (!questTargetType.equalsIgnoreCase(entityType)))
		{
			return;
		}

		ctx.plugin().logDebug("handling quest eating for " + godName);

		if (addProgress(godName))
		{
			ctx.godSpeech().sayToBelievers(godName, LANGUAGESTRING.GodToBelieversHolyFeastQuestCompleted, 2 + this.random.nextInt(10));
			complete(godName, 2.0F);
		}
		else
		{
			godSayProgress(godName);
		}
	}

	public boolean handleJoinReligion(String playerName, String godName)
	{
		if (!hasQuest(godName) || questType(godName) != QuestType.CONVERT)
		{
			return false;
		}

		if (addProgress(godName))
		{
			ctx.languageManager().setPlayerName(playerName);
			ctx.godSpeech().sayToBelievers(godName, LANGUAGESTRING.GodToBelieversConvertQuestCompleted, 2 + this.random.nextInt(10));
			complete(godName, 1.0F);
		}
		else
		{
			godSayProgress(godName);
		}
		return true;
	}

	public void handleKilledMob(String godName, String mobType)
	{
		if (!hasQuest(godName) || questType(godName) != QuestType.SLAY)
		{
			return;
		}

		String questTargetType = targetType(godName);
		if (questTargetType == null || !questTargetType.equalsIgnoreCase(mobType))
		{
			return;
		}

		if (addProgress(godName))
		{
			ctx.godSpeech().sayToBelievers(godName, LANGUAGESTRING.GodToBelieversSlayQuestCompleted, 2 + this.random.nextInt(10));
			complete(godName, 2.0F);
		}
		else
		{
			godSayProgress(godName);
		}
	}

	/**
	 * A player opened a block. Completes the pilgrimage if it is the chest of the player's god. Returns true if the
	 * chest belongs to a quest the player may not open (the caller cancels the event).
	 */
	public boolean handleOpenChest(UUID playerId, Location blockLocation)
	{
		if (!isQuestTarget(blockLocation))
		{
			return false;
		}

		String questGod = targetAt(blockLocation).godName();
		String godName = ctx.believerService().getGodForBeliever(playerId);

		// Only believers of the god who set the pilgrimage may open its chest
		if (questGod == null || godName == null || !questGod.equals(godName))
		{
			return true;
		}

		try
		{
			ctx.languageManager().setType(targetType(questGod));
		}
		catch (Exception ex)
		{
			ctx.plugin().logDebug(ex.getMessage());
		}

		ctx.godSpeech().sayToBelievers(questGod, LANGUAGESTRING.GodToBelieversPilgrimageQuestCompleted, 2);

		ctx.moodService().addMoodForGod(godName, 2.0F * ctx.moodService().getPleasedModifierForGod(godName));
		ctx.prayerService().addBeliefAndRewardBelievers(godName);
		ctx.believerService().increasePrayerPower(playerId, 1);

		Player player = ctx.plugin().getServer().getPlayer(playerId);
		if (player != null)
		{
			ctx.plugin().getServer().broadcastMessage(ChatColor.GOLD + player.getDisplayName() + ChatColor.AQUA + " completed a pilgrimage to " + ChatColor.GOLD + targetType(questGod));
		}

		ctx.believerService().setHunting(playerId, false);

		removeSuccessQuestForGod(godName);

		return false;
	}

	/** A player prayed to the god: counts for a convert quest if they are not its believer yet */
	public void handlePrayer(String godName, UUID playerId)
	{
		if (!hasQuest(godName) || questType(godName) != QuestType.CONVERT)
		{
			return;
		}

		String playerGod = ctx.believerService().getGodForBeliever(playerId);

		if ((playerGod != null) && (playerGod.equals(godName)))
		{
			return;
		}

		Player player = ctx.plugin().getServer().getPlayer(playerId);

		if (addPlayerProgress(godName, playerId))
		{
			ctx.languageManager().setPlayerName((player != null) ? player.getDisplayName() : "");
			ctx.godSpeech().sayToBelievers(godName, LANGUAGESTRING.GodToBelieversConvertQuestCompleted, 2 + this.random.nextInt(10));
			complete(godName, 1.0F);
		}
		else
		{
			godSayProgress(godName);
		}
	}

	/** A player read the god's holy book: counts for a GIVEITEMS quest if they have no god */
	public void handleReadBible(String godName, UUID playerId)
	{
		if (!hasQuest(godName) || questType(godName) != QuestType.GIVEITEMS)
		{
			return;
		}

		// Only non-believers count
		if (ctx.believerService().getGodForBeliever(playerId) != null)
		{
			return;
		}

		Player player = ctx.plugin().getServer().getPlayer(playerId);

		boolean completed = addPlayerProgress(godName, playerId);

		ctx.languageManager().setPlayerName((player != null) ? player.getDisplayName() : "");

		if (completed)
		{
			setQuestSpeechValues(godName, QuestType.GIVEITEMS, 0);
			ctx.godSpeech().sayToBelievers(godName, LANGUAGESTRING.GodToBelieversGiveItemsQuestCompleted, 2 + this.random.nextInt(10));
			complete(godName, 2.0F);
		}
		else
		{
			godSayProgress(godName);
		}
	}

	/** A believer sacrificed an item; returns true if it counted for the god's quest */
	public boolean handleSacrifice(Player player, String godName, String entityType)
	{
		if (!hasQuest(godName))
		{
			return false;
		}

		QuestType questType = questType(godName);

		if ((questType != QuestType.ITEMSACRIFICE) && (questType != QuestType.BURNBIBLES))
		{
			return false;
		}

		String questTargetType = targetType(godName);
		if ((questTargetType == null) || (!questTargetType.equalsIgnoreCase(entityType)))
		{
			return false;
		}

		boolean completed = addProgress(godName);
		ctx.believerService().increasePrayerPower(player.getUniqueId(), 1);

		if (completed)
		{
			setQuestSpeechValues(godName, questType, 0);
			ctx.languageManager().setPlayerName(player.getDisplayName());
			ctx.godSpeech().sayToBelievers(godName, questSpeech(questType, Phase.COMPLETED), 2 + this.random.nextInt(10));
			complete(godName, 2.0F);
		}
		else
		{
			godSayProgress(godName);
		}

		return true;
	}

	// ---------------------------------------------------------------------
	// Ending quests

	/** Removes a failed quest: takes away its pilgrimage chest, then always clears the quest, even if the chest can't be removed */
	public void removeFailedQuestForGod(String godName)
	{
		try
		{
			if (questType(godName) == QuestType.PILGRIMAGE)
			{
				Location location = getQuestLocation(godName);
				if (location != null)
				{
					Quest quest = this.repository.find(godName);
					this.pilgrimageSites.removeChest(location, quest.location().ground());
				}
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

	public void removeSuccessQuestForGod(String godName)
	{
		removeQuest(godName);
	}

	private void removeQuest(String godName)
	{
		Location questLocation = storedQuestLocation(godName);

		if (questLocation != null)
		{
			clearQuestTarget(questLocation);
		}

		this.repository.remove(godName);
		this.repository.save();
	}
}
