package com.dogonfire.gods.managers;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.model.God;
import com.dogonfire.gods.model.GodMood;
import com.dogonfire.gods.model.MarriedCouple;
import com.dogonfire.gods.service.GodSpeech;
import com.dogonfire.gods.storage.GodRepository;
import com.dogonfire.gods.util.LegacyDates;

public class GodManager
{

	private Random				random			= new Random();

	private final GodsContext ctx;

	public GodManager(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	/** gods.yml; see {@link GodRepository} */
	private GodRepository gods()
	{
		return ctx.storage().gods();
	}

	/** The god with this name, or null */
	private God god(String godName)
	{
		return gods().find(godName);
	}

	/**
	 * The god to change, created if there is none (setting a value in gods.yml always did). Null for a null god name,
	 * so a lookup for a player without a god can't create a god called "null".
	 */
	private God godToChange(String godName, String what)
	{
		if (godName == null)
		{
			ctx.plugin().logDebug("Not writing " + what + " for a null god name");
			return null;
		}
		return gods().getOrCreate(godName);
	}

	private GodSpeech speech()
	{
		return ctx.godSpeech();
	}

	private static Instant now()
	{
		return LegacyDates.SECONDS.now();
	}

	/**
	 * @return the UUID in this string, or null if it is missing or not a UUID (e.g. a player name from Gods 0.9.x)
	 */
	private static UUID parseUUID(String uuidString)
	{
		if (uuidString == null)
		{
			return null;
		}
		try
		{
			return UUID.fromString(uuidString);
		}
		catch (IllegalArgumentException ex)
		{
			return null;
		}
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

				speech().say(godName, believer, LanguageManager.LANGUAGESTRING.GodToBelieverCursedAngry, 2 + this.random.nextInt(10));

				return true;
			}
		}

		if (this.random.nextInt(1 + 1000 / ctx.godService().getVerbosityForGod(godName)) == 0)
		{
			if ((ctx.believerService().hasRecentPrayer(believer.getUniqueId())) && (this.random.nextInt(2) == 0))
			{
				return false;
			}
			speech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverRandomAngrySpeech);
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
			speech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverRandomDispleasedSpeech);
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

		if ((believer.getGameMode() != GameMode.CREATIVE) && ctx.permissionsManager().hasPermission(believer, "gods.itemblessings"))
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

				speech().say(godName, believer, LanguageManager.LANGUAGESTRING.GodToPlayerBlessed, 2 + this.random.nextInt(10));

				speech().sayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPlayerBlessed, believer.getUniqueId());

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
				speech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverMarriedCouple);
				return true;
			}
		}

		if (this.random.nextInt(1 + 1000 / ctx.godService().getVerbosityForGod(godName)) == 0)
		{
			if ((ctx.believerService().hasRecentPrayer(believer.getUniqueId())) && (this.random.nextInt(2) == 0))
			{
				return false;
			}
			speech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverRandomExaltedSpeech);
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
				speech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverMarriedCouple);
				return true;
			}
		}
		if (this.random.nextInt(1 + 1000 / ctx.godService().getVerbosityForGod(godName)) == 0)
		{
			if ((ctx.believerService().hasRecentPrayer(believer.getUniqueId())) && (this.random.nextInt(2) == 0))
			{
				return false;
			}
			speech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverRandomNeutralSpeech);
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

		if (believer.getGameMode() != GameMode.CREATIVE && ctx.permissionsManager().hasPermission(believer, "gods.itemblessings"))
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
				speech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverMarriedCouple);
				return true;
			}
		}

		if (this.random.nextInt(1 + 1000 / ctx.godService().getVerbosityForGod(godName)) == 0)
		{
			if ((ctx.believerService().hasRecentPrayer(believer.getUniqueId())) && (this.random.nextInt(2) == 0))
			{
				return false;
			}

			speech().sayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverRandomPleasedSpeech);

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

		if (!ctx.questManager().hasQuest(godName))
		{
			if (numberOfBelievers < ctx.settings().quests().requiredBelievers() || ctx.godService().getMinutesSinceLastQuest(godName) < ctx.settings().quests().minMinutesBetweenQuests())
			{
				return;
			}

			ctx.questManager().generateQuest(godName);
		}
		else if (ctx.questManager().hasExpiredQuest(godName))
		{
			ctx.moodService().addMoodForGod(godName, ctx.moodService().getAngryModifierForGod(godName));

			ctx.questManager().godSayFailed(godName);

			ctx.questManager().removeFailedQuestForGod(godName);
		}
		else if (random.nextInt(5) == 0)
		{
			ctx.questManager().godSayStatus(godName);
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

	public void update()
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
