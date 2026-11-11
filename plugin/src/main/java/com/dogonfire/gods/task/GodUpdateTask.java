package com.dogonfire.gods.task;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.model.GodMood;
import com.dogonfire.gods.model.MarriedCouple;

/**
 * The gods' update loop, run every {@link #PERIOD_TICKS} ticks for the life of the plugin (started by {@code Gods}).
 * Each run one god with a believer online gets a turn: its mood falls off, it looks for or reminds priests, loses
 * believers who stopped praying, acts on a few online believers as its mood says (blessings, items, healing, holy
 * artifacts, curses, speech), runs its quests, blesses or curses its blessed or cursed player and changes what it
 * wants sacrificed. Now and then dead offline gods, abandoned holy lands and dropped sacrifices are cleaned up. The
 * effects themselves are the services'; this decides when. Main thread only.
 */
public final class GodUpdateTask implements Runnable
{
	/** How often a god gets its turn, in ticks (10 seconds) */
	public static final long	PERIOD_TICKS	= 200L;
	/** The first turn, in ticks after the plugin started */
	public static final long	DELAY_TICKS		= 20L;

	private final Random		random			= new Random();
	private final GodsContext	ctx;

	public GodUpdateTask(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	private boolean manageBelieverForAngryGod(String godName, Player believer)
	{
		if (!ctx.plugin().isEnabledInWorld(believer.getWorld()))
		{
			return false;
		}

		int godPower = 1 + (int) ctx.godService().getGodPower(godName);

		if (this.random.nextInt(1 + 1000 / godPower) == 0)
		{
			if (ctx.believerService().hasRecentPrayer(believer.getUniqueId()))
			{
				return false;
			}

			if (ctx.blessingService().cursePlayer(godName, believer.getUniqueId(), godPower))
			{
				ctx.languageManager().setPlayerName(believer.getDisplayName());

				ctx.godSpeech().say(godName, believer, LanguageManager.LANGUAGESTRING.GodToBelieverCursedAngry, 2 + this.random.nextInt(10));

				return true;
			}
		}

		if (this.random.nextInt(1 + 1000 / ctx.godService().getVerbosityForGod(godName)) == 0)
		{
			if ((ctx.believerService().hasRecentPrayer(believer.getUniqueId())) && (this.random.nextInt(2) == 0))
			{
				return false;
			}
			ctx.godSpeech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverRandomAngrySpeech);
			return true;
		}

		if (this.random.nextInt(1 + 600 / ctx.godService().getVerbosityForGod(godName)) == 0)
		{
			if (ctx.commandmentService().sayNeededSacrificeToBeliever(godName, believer.getUniqueId()))
			{
				return true;
			}
		}
		return false;
	}

	private boolean manageBelieverForDispleasedGod(String godName, Player believer)
	{
		if (believer == null)
		{
			return false;
		}
		if (!ctx.plugin().isEnabledInWorld(believer.getWorld()))
		{
			return false;
		}
		if (this.random.nextInt(1 + 1000 / ctx.godService().getVerbosityForGod(godName)) == 0)
		{
			if ((ctx.believerService().hasRecentPrayer(believer.getUniqueId())) && (this.random.nextInt(2) == 0))
			{
				return false;
			}
			ctx.godSpeech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverRandomDispleasedSpeech);
			return true;
		}
		if (this.random.nextInt(1 + 600 / ctx.godService().getVerbosityForGod(godName)) == 0)
		{
			if (ctx.commandmentService().sayNeededSacrificeToBeliever(godName, believer.getUniqueId()))
			{
				return true;
			}
		}
		return false;
	}

	private boolean manageBelieverForExaltedGod(String godName, Player believer)
	{
		if (believer == null)
		{
			return false;
		}

		if (!ctx.plugin().isEnabledInWorld(believer.getWorld()))
		{
			return false;
		}

		if ((believer.getGameMode() != GameMode.CREATIVE) && ctx.permissions().hasPermission(believer, "gods.itemblessings"))
		{
			if (!ctx.believerService().hasRecentItemBlessing(believer.getUniqueId()))
			{
				if (ctx.settings().itemBlessings().enabled())
				{
					float power = ctx.godService().getGodPower(godName);

					if (power >= ctx.settings().itemBlessings().minGodPower() && this.random.nextInt((int) (1.0F + 50.0F / power)) == 0)
					{
						double healing = ctx.blessingService().getHealthNeed(godName, believer);

						if ((healing > 1.0D) && (this.random.nextInt(3) == 0))
						{
							ctx.blessingService().healPlayer(godName, believer);

							ctx.believerService().setItemBlessingTime(believer.getUniqueId());

							return true;
						}

						ItemStack blessedItem = ctx.blessingService().blessPlayerWithItem(godName, believer);

						if (blessedItem != null)
						{
							ctx.languageManager().setPlayerName(believer.getDisplayName());
							try
							{
								ctx.languageManager().setType(ctx.languageManager().getItemTypeName(blessedItem.getType()));
							}
							catch (Exception ex)
							{
								ctx.plugin().logDebug(ex.getMessage());
							}

							ctx.believerService().setItemBlessingTime(believer.getUniqueId());

							return true;
						}
					}
				}
			}

			if (ctx.settings().holyArtifacts().enabled())
			{
				if (!ctx.believerService().hasRecentHolyArtifactBlessing(believer.getUniqueId()))
				{
					float power = ctx.godService().getGodPower(godName);

					if ((power >= ctx.settings().itemBlessings().minGodPower()) && (this.random.nextInt((int) (1.0F + 100.0F / power)) == 0))
					{
						ctx.blessingService().blessPlayerWithHolyArtifact(godName, believer);

						ctx.languageManager().setPlayerName(believer.getDisplayName());
						ctx.believerService().setHolyArtifactBlessingTime(believer.getUniqueId());

						return true;
					}
				}
			}
		}

		if (!ctx.believerService().hasRecentItemBlessing(believer.getUniqueId()))
		{
			if (ctx.blessingService().blessPlayer(godName, believer.getUniqueId(), ctx.godService().getGodPower(godName)))
			{
				ctx.languageManager().setPlayerName(believer.getDisplayName());

				ctx.godSpeech().say(godName, believer, LanguageManager.LANGUAGESTRING.GodToPlayerBlessed, 2 + this.random.nextInt(10));

				ctx.godSpeech().sayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPlayerBlessed, believer.getUniqueId());

				return true;
			}
		}

		if (ctx.settings().marriage().enabled() && this.random.nextInt(501) == 0)
		{
			List<MarriedCouple> marriedCouples = ctx.marriageService().getMarriedCouples();
			if (marriedCouples.size() > 0)
			{
				MarriedCouple couple = marriedCouples.get(this.random.nextInt(marriedCouples.size()));

				ctx.languageManager().setPlayerName(ctx.plugin().getServer().getOfflinePlayer(couple.player1Id()).getName() + " and " + ctx.plugin().getServer().getOfflinePlayer(couple.player2Id()).getName());
				ctx.godSpeech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverMarriedCouple);
				return true;
			}
		}

		if (this.random.nextInt(1 + 1000 / ctx.godService().getVerbosityForGod(godName)) == 0)
		{
			if ((ctx.believerService().hasRecentPrayer(believer.getUniqueId())) && (this.random.nextInt(2) == 0))
			{
				return false;
			}
			ctx.godSpeech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverRandomExaltedSpeech);
			return true;
		}

		if (this.random.nextInt(1 + 600 / ctx.godService().getVerbosityForGod(godName)) == 0)
		{
			if (ctx.commandmentService().sayNeededSacrificeToBeliever(godName, believer.getUniqueId()))
			{
				return true;
			}
		}
		return false;
	}

	private boolean manageBelieverForNeutralGod(String godName, Player believer)
	{
		if (believer == null)
		{
			return false;
		}

		if (!ctx.plugin().isEnabledInWorld(believer.getWorld()))
		{
			return false;
		}
		if ((ctx.settings().marriage().enabled()) && (this.random.nextInt(501) == 0))
		{
			List<MarriedCouple> marriedCouples = ctx.marriageService().getMarriedCouples();
			if (marriedCouples.size() > 0)
			{
				MarriedCouple couple = marriedCouples.get(this.random.nextInt(marriedCouples.size()));

				ctx.languageManager().setPlayerName(ctx.plugin().getServer().getOfflinePlayer(couple.player1Id()).getName() + " and " + ctx.plugin().getServer().getOfflinePlayer(couple.player2Id()).getName());
				ctx.godSpeech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverMarriedCouple);
				return true;
			}
		}
		if (this.random.nextInt(1 + 1000 / ctx.godService().getVerbosityForGod(godName)) == 0)
		{
			if ((ctx.believerService().hasRecentPrayer(believer.getUniqueId())) && (this.random.nextInt(2) == 0))
			{
				return false;
			}
			ctx.godSpeech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverRandomNeutralSpeech);
			return true;
		}

		if (this.random.nextInt(1 + 600 / ctx.godService().getVerbosityForGod(godName)) == 0)
		{
			if (ctx.commandmentService().sayNeededSacrificeToBeliever(godName, believer.getUniqueId()))
			{
				return true;
			}
		}
		return false;
	}

	private boolean manageBelieverForPleasedGod(String godName, Player believer)
	{
		if (believer == null)
		{
			return false;
		}

		if (!ctx.plugin().isEnabledInWorld(believer.getWorld()))
		{
			return false;
		}

		if (believer.getGameMode() != GameMode.CREATIVE && ctx.permissions().hasPermission(believer, "gods.itemblessings"))
		{
			if (!ctx.believerService().hasRecentItemBlessing(believer.getUniqueId()))
			{
				if (ctx.settings().itemBlessings().enabled())
				{
					float power = ctx.godService().getGodPower(godName);
					if ((power >= ctx.settings().itemBlessings().minGodPower()) && (this.random.nextInt((int) (1.0F + 100.0F / power)) == 0))
					{
						double healing = ctx.blessingService().getHealthNeed(godName, believer);
						if ((healing > 1.0D) && (this.random.nextInt(2) == 0))
						{
							ctx.blessingService().healPlayer(godName, believer);

							ctx.believerService().setItemBlessingTime(believer.getUniqueId());

							return true;
						}

						ItemStack blessedItem = ctx.blessingService().blessPlayerWithItem(godName, believer);

						if (blessedItem != null)
						{
							ctx.languageManager().setPlayerName(believer.getDisplayName());
							try
							{
								ctx.languageManager().setType(ctx.languageManager().getItemTypeName(blessedItem.getType()));
							}
							catch (Exception ex)
							{
								ctx.plugin().logDebug(ex.getMessage());
							}
							ctx.believerService().setItemBlessingTime(believer.getUniqueId());

							return true;
						}
					}
				}
			}
		}

		if ((ctx.settings().marriage().enabled()) && (this.random.nextInt(501) == 0))
		{
			List<MarriedCouple> marriedCouples = ctx.marriageService().getMarriedCouples();

			if (marriedCouples.size() > 0)
			{
				MarriedCouple couple = marriedCouples.get(this.random.nextInt(marriedCouples.size()));

				ctx.languageManager().setPlayerName(ctx.plugin().getServer().getOfflinePlayer(couple.player1Id()).getName() + " and " + ctx.plugin().getServer().getOfflinePlayer(couple.player2Id()).getName());
				ctx.godSpeech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverMarriedCouple);
				return true;
			}
		}

		if (this.random.nextInt(1 + 1000 / ctx.godService().getVerbosityForGod(godName)) == 0)
		{
			if ((ctx.believerService().hasRecentPrayer(believer.getUniqueId())) && (this.random.nextInt(2) == 0))
			{
				return false;
			}

			ctx.godSpeech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverRandomPleasedSpeech);

			return true;
		}

		if (this.random.nextInt(1 + 600 / ctx.godService().getVerbosityForGod(godName)) == 0)
		{
			if (ctx.commandmentService().sayNeededSacrificeToBeliever(godName, believer.getUniqueId()))
			{
				return true;
			}
		}
		return false;
	}

	private void manageBelievers(String godName)
	{
		Set<UUID> believers = ctx.believerService().getOnlineBelieversForGod(godName);
		Set<UUID> managedBelievers = new HashSet<UUID>();
		if (believers.size() == 0)
		{
			return;
		}

		GodMood godMood = ctx.moodService().getMoodForGod(godName);

		List<UUID> priests = ctx.priestService().getPriestsForGod(godName);
		for (int n = 0; n < 10; n++)
		{
			UUID believerId = (UUID) believers.toArray()[this.random.nextInt(believers.size())];

			if (!managedBelievers.contains(believerId))
			{
				String priestName = null;
				if (!priests.isEmpty())
				{
					priestName = ctx.plugin().getServer().getOfflinePlayer(priests.get(this.random.nextInt(priests.size()))).getName();
				}
				ctx.languageManager().setPlayerName(priestName != null ? priestName : "our priest");

				Player believer = ctx.plugin().getServer().getPlayer(believerId);

				switch (godMood)
				{
					case EXALTED: manageBelieverForExaltedGod(godName, believer); break;
					case PLEASED: manageBelieverForPleasedGod(godName, believer); break;
					case NEUTRAL: manageBelieverForNeutralGod(godName, believer); break;
					case DISPLEASED: manageBelieverForDispleasedGod(godName, believer); break;
					case ANGRY: manageBelieverForAngryGod(godName, believer);
				}

				managedBelievers.add(believerId);
			}
		}
	}

	private void manageHolyLands()
	{
		if (!ctx.settings().holyLand().enabled())
		{
			return;
		}
		if (this.random.nextInt(1000) > 0)
		{
			return;
		}
		
		// Holy powers effects
		
		// Mobs: Spawn types of mob in random radius
		// Nature: Grow/Spawn type of sapling/vine/flower in random radius
		// Battle: Spawn swords, bows & armour in random block
		// Love: Spawn luck/food/Health/Gold/
		// Sea: Spawn fish in sea within radius
		
		ctx.holyLandService().removeAbandonedLands();
	}
	

	private void manageQuests(String godName)
	{
		if (!ctx.settings().quests().enabled())
		{
			return;
		}

		int numberOfBelievers = ctx.believerService().getOnlineBelieversForGod(godName).size();

		if (!ctx.questService().hasQuest(godName))
		{
			if (numberOfBelievers < ctx.settings().quests().requiredBelievers() || ctx.godService().getMinutesSinceLastQuest(godName) < ctx.settings().quests().minMinutesBetweenQuests())
			{
				return;
			}

			ctx.questService().generateQuest(godName);
		}
		else if (ctx.questService().hasExpiredQuest(godName))
		{
			ctx.moodService().addMoodForGod(godName, ctx.moodService().getAngryModifierForGod(godName));

			ctx.questService().godSayFailed(godName);

			ctx.questService().removeFailedQuestForGod(godName);
		}
		else if (random.nextInt(5) == 0)
		{
			ctx.questService().godSayStatus(godName);
		}
	}

	private void manageSacrifices()
	{
		if (!ctx.settings().sacrifices().enabled())
		{
			return;
		}

		if (this.random.nextInt(10) > 0)
		{
			return;
		}

		ctx.altarService().clearDroppedItems();
	}

	/** One god's turn (every 10 seconds); now and then, dead offline gods are removed */
	@Override
	public void run()
	{
		if (this.random.nextInt(50) == 0)
		{
			ctx.plugin().logDebug("Processing dead offline Gods...");

			long timeBefore = System.currentTimeMillis();

			List<String> godNames = ctx.godService().getOfflineGods();
			for (String offlineGodName : godNames)
			{
				if (ctx.godService().isDeadGod(offlineGodName))
				{
					ctx.plugin().log("Removed dead offline God '" + offlineGodName + "'");
				}
			}
			long timeAfter = System.currentTimeMillis();

			ctx.plugin().logDebug("Processed " + godNames.size() + " offline Gods in " + (timeAfter - timeBefore) + " ms");
		}

		List<String> godNames = ctx.godService().getOnlineGods();

		long timeBefore = System.currentTimeMillis();

		if (godNames.size() == 0)
		{
			return;
		}
		String godName = (String) godNames.toArray()[this.random.nextInt(godNames.size())];

		ctx.plugin().logDebug("Processing God '" + godName + "'");

		ctx.moodService().applyFalloff(godName);

		boolean godTalk = ctx.priestService().managePriests(godName);

		ctx.prayerService().removeLostBelievers(godName);

		if (!godTalk)
		{
			manageBelievers(godName);
		}

		if (!godTalk)
		{
			manageQuests(godName);
		}

		ctx.blessingService().manageBlessedPlayer(godName);

		ctx.blessingService().manageCursedPlayer(godName);

		ctx.commandmentService().updateSacrificeNeeds(godName);

		manageSacrifices();

		manageHolyLands();

		long timeAfter = System.currentTimeMillis();

		ctx.plugin().logDebug("Processed 1 Online God in " + (timeAfter - timeBefore) + " ms");
	}

}
