package com.dogonfire.gods.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.config.Theme;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.model.God;
import com.dogonfire.gods.storage.GodRepository;

/**
 * A god's commandments and sacrifices: the food it wants eaten and the food it forbids, the mob to slay and the mob to
 * spare (each picked once and kept), what happens when a believer eats or kills, and the items it wants sacrificed at
 * its altars and how much (it wants more of some over time and forgets unwanted ones). What a god picks from is its
 * type's theme (themes.yml, {@link #themeForGod}); the rules are in {@link CommandmentRules} and {@link SacrificeRules}.
 * Main thread only.
 */
public class CommandmentService
{
	private final Random			random	= new Random();
	private final GodsContext		ctx;
	private final GodRepository		repository;

	public CommandmentService(GodsContext ctx, GodRepository repository)
	{
		this.ctx = ctx;
		this.repository = repository;
	}

	/**
	 * The theme of the god's type (themes.yml; the default theme for a god without a type), which its commandments,
	 * sacrifice needs and slay quests are picked from
	 */
	public Theme themeForGod(String godName)
	{
		return ctx.themes().of(ctx.godService().getDivineForceForGod(godName));
	}

	/** A list of the god's theme, or the default theme's when it is empty (e.g. every name in it is newer than the server) */
	private <T> List<T> fromTheme(String godName, Function<Theme, List<T>> list)
	{
		List<T> own = list.apply(themeForGod(godName));
		return own.isEmpty() ? list.apply(ctx.themes().defaults()) : own;
	}

	/**
	 * @return the entity type with this name, or null if the name is missing or no longer exists (e.g. PIG_ZOMBIE)
	 */
	private EntityType parseEntityType(String mobTypeString)
	{
		if (mobTypeString == null)
		{
			return null;
		}
		try
		{
			return EntityType.valueOf(mobTypeString.toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex)
		{
			return null;
		}
	}

	/** A god keeps its holy food once picked; one this server doesn't know (any more) counts as a carrot */
	public Material getHolyFoodTypeForGod(String godName)
	{
		God god = this.repository.forChange(godName);
		String stored = god != null ? god.holyFood() : null;
		if (stored != null)
		{
			Material food = Material.getMaterial(stored);
			return food != null ? food : Material.CARROT;
		}

		Material unholyFood = (god != null && god.unholyFood() != null) ? Material.getMaterial(god.unholyFood()) : null;
		Material food = CommandmentRules.pick(this.random, fromTheme(godName, Theme::holyFoods), unholyFood);
		if (food == null)
		{
			food = Material.CARROT;
		}
		if (god != null)
		{
			god.setHolyFood(food.name());
		}
		this.repository.markDirty();
		return food;
	}

	/** A god keeps its holy mob once picked; one this server doesn't know (any more) is picked again */
	public EntityType getHolyMobTypeForGod(String godName)
	{
		God god = this.repository.forChange(godName);
		EntityType mobType = parseEntityType(god != null ? god.holyMob() : null);
		if (mobType == null)
		{
			mobType = CommandmentRules.pick(this.random, fromTheme(godName, Theme::holyMobs), getUnholyMobTypeForGod(godName));
			if (mobType == null)
			{
				mobType = EntityType.COW;
			}
			if (god != null)
			{
				god.setHolyMob(mobType.name());
			}
			this.repository.markDirty();
		}
		return mobType;
	}

	/** A god keeps its unholy food once picked, never its holy food; one this server doesn't know counts as a carrot */
	public Material getUnholyFoodTypeForGod(String godName)
	{
		God god = this.repository.forChange(godName);
		String stored = god != null ? god.unholyFood() : null;
		if (stored != null)
		{
			Material food = Material.getMaterial(stored);
			return food != null ? food : Material.CARROT;
		}

		Material food = CommandmentRules.pick(this.random, fromTheme(godName, Theme::unholyFoods), getHolyFoodTypeForGod(godName));
		if (food == null)
		{
			food = Material.CARROT;
		}
		if (god != null)
		{
			god.setUnholyFood(food.name());
		}
		this.repository.markDirty();
		return food;
	}

	public Material getSacrificeItemTypeForGod(String godName)
	{
		String itemName = "";
		String sacrificeItemName = null;

		God god = this.repository.find(godName);
		if ((god == null) || (god.sacrificeValues().size() == 0))
		{
			return null;
		}
		Map<String, Float> values = god.sacrificeValues();
		for (int i = 0; i < values.size(); i++)
		{
			itemName = (String) values.keySet().toArray()[this.random.nextInt(values.size())];

			if (SacrificeRules.isAskedFor(values.get(itemName)))
			{
				sacrificeItemName = itemName;
			}
		}
		if (sacrificeItemName != null)
		{
			return Material.getMaterial(sacrificeItemName);
		}
		return null;
	}

	/** One of the items the god's seed picked from its type's sacrifice needs, or its unholy food; null if none fits */
	private Material getSacrificeNeedForGod(String godName)
	{
		return CommandmentRules.sacrificeNeed(this.random, ctx.godService().getSeedForGod(godName), fromTheme(godName, Theme::sacrificeNeeds), () -> getUnholyFoodTypeForGod(godName), getHolyFoodTypeForGod(godName));
	}

	private Material getSacrificeUnwantedForGod(String godName)
	{
		List<Material> unwantedItems = new ArrayList<Material>();
		God god = this.repository.find(godName);
		if (god != null && !god.sacrificeValues().isEmpty())
		{
			for (String itemType : god.sacrificeValues().keySet())
			{
				Material item = null;
				try
				{
					item = Material.valueOf(itemType);
				}
				catch (Exception ex)
				{
					continue;
				}
				if (god.sacrificeValues().get(itemType) <= 0.0D)
				{
					unwantedItems.add(item);
				}
			}
		}
		else
		{
			return null;
		}
		if (unwantedItems.size() == 0)
		{
			return null;
		}
		return unwantedItems.get(this.random.nextInt(unwantedItems.size()));
	}

	private float getSacrificeValueForGod(String godName, Material type)
	{
		God god = this.repository.find(godName);
		Float value = god != null ? god.sacrificeValues().get(type.name()) : null;
		return value != null ? value : 0;
	}

	/** Puts a sacrifice value; null removes it */
	private void setSacrificeValueForGod(String godName, Material type, Float value)
	{
		if (value == null)
		{
			God god = this.repository.find(godName);
			if (god != null)
			{
				god.sacrificeValues().remove(type.name());
			}
			return;
		}
		God god = this.repository.forChange(godName);
		if (god != null)
		{
			god.sacrificeValues().put(type.name(), value);
		}
	}

	/**
	 * The holy mob of a god as stored, or null if there is no such god or it has none yet. Unlike
	 * {@link #getHolyMobTypeForGod} it changes nothing (it never picks one or creates the god), so event handlers can
	 * call it for any land.
	 */
	public EntityType findHolyMobType(String godName)
	{
		God god = this.repository.find(godName);
		return god != null ? parseEntityType(god.holyMob()) : null;
	}

	/** {@link #findHolyMobType} for the unholy mob */
	public EntityType findUnholyMobType(String godName)
	{
		God god = this.repository.find(godName);
		return god != null ? parseEntityType(god.unholyMob()) : null;
	}

	/** A god keeps its unholy mob once picked; one this server doesn't know (any more) is picked again */
	public EntityType getUnholyMobTypeForGod(String godName)
	{
		God god = this.repository.forChange(godName);
		EntityType mobType = parseEntityType(god != null ? god.unholyMob() : null);
		if (mobType == null)
		{
			mobType = CommandmentRules.pick(this.random, fromTheme(godName, Theme::unholyMobs), parseEntityType(god != null ? god.holyMob() : null));
			if (mobType == null)
			{
				mobType = EntityType.ZOMBIE;
			}
			if (god != null)
			{
				god.setUnholyMob(mobType.name());
			}
			this.repository.markDirty();
		}
		return mobType;
	}

	public boolean sayNeededSacrificeToBeliever(String godName, UUID believerId)
	{
		if (ctx.settings().sacrifices().enabled())
		{
			Material itemType = getSacrificeItemTypeForGod(godName);
			if (itemType != null)
			{
				ctx.godSpeech().sayToBeliever(godName, believerId, Voice.BELIEVERS_SACRIFICE_ITEM_TYPE, Messages.type(ctx.messages().itemName(itemType)));

				return true;
			}
		}
		return false;
	}

	public void handleEat(Player player, String godName, String foodType)
	{
		Material eatFoodType = getHolyFoodTypeForGod(godName);
		Material notEatFoodType = getUnholyFoodTypeForGod(godName);

		if (foodType.equals(eatFoodType.name()))
		{
			ctx.devotionService().earn(player.getUniqueId(), godName, DevotionService.Deed.HOLY_FOOD);
			ctx.moodService().addMoodForGod(godName, ctx.moodService().getPleasedModifierForGod(godName));

			if (ctx.blessingService().blessPlayer(godName, player.getUniqueId(), ctx.godService().getGodPower(godName)))
			{
				
				
				if (ctx.settings().commandments().broadcastFoodEaten())
				{
					ctx.godSpeech().sayToBelievers(godName, Voice.BELIEVERS_EAT_FOOD_BLESSING, 2 + this.random.nextInt(20), Messages.displayName(player.getDisplayName()), Messages.type(ctx.messages().itemName(eatFoodType)));
				}
				else
				{
					ctx.godSpeech().sayToBeliever(godName, player.getUniqueId(), Voice.BELIEVERS_EAT_FOOD_BLESSING, Messages.displayName(player.getDisplayName()), Messages.type(ctx.messages().itemName(eatFoodType)));
				}
			}
		}

		if (foodType.equals(notEatFoodType.name()))
		{
			ctx.moodService().addMoodForGod(godName, ctx.moodService().getAngryModifierForGod(godName));
			
			if (ctx.blessingService().cursePlayer(godName, player.getUniqueId(), ctx.godService().getGodPower(godName)))
			{


				if (ctx.settings().commandments().broadcastFoodEaten())
				{
					ctx.godSpeech().sayToBelievers(godName, Voice.BELIEVERS_NOT_EAT_FOOD_CURSING, 2 + this.random.nextInt(10), Messages.displayName(player.getDisplayName().toUpperCase(Locale.ROOT)), Messages.type(ctx.messages().itemName(notEatFoodType)));
				}
				else
				{
					ctx.godSpeech().sayToBeliever(godName, player.getUniqueId(), Voice.BELIEVERS_NOT_EAT_FOOD_CURSING, Messages.displayName(player.getDisplayName().toUpperCase(Locale.ROOT)), Messages.type(ctx.messages().itemName(notEatFoodType)));
				}
			}
		}
	}

	public void handleKilled(Player player, String godName, String mobType)
	{
		if ((!ctx.settings().commandments().enabled()) || (mobType == null))
		{
			return;
		}
		EntityType holyMobType = getHolyMobTypeForGod(godName);
		EntityType unholyMobType = getUnholyMobTypeForGod(godName);
		if ((unholyMobType != null) && (mobType.equals(unholyMobType.name())))
		{
			ctx.devotionService().earn(player.getUniqueId(), godName, DevotionService.Deed.UNHOLY_MOB);
			if (ctx.blessingService().blessPlayer(godName, player.getUniqueId(), ctx.godService().getGodPower(godName)))
			{
				ctx.moodService().addMoodForGod(godName, ctx.moodService().getPleasedModifierForGod(godName));

				if (ctx.settings().commandments().broadcastMobSlain())
				{
					ctx.godSpeech().sayToBelievers(godName, Voice.BELIEVERS_SLAY_MOB_BLESSING, 2 + this.random.nextInt(20), Messages.displayName(player.getDisplayName()), Messages.type(ctx.messages().mobName(unholyMobType)));
				}
				else
				{
					ctx.godSpeech().sayToBeliever(godName, player.getUniqueId(), Voice.BELIEVERS_SLAY_MOB_BLESSING, Messages.displayName(player.getDisplayName()), Messages.type(ctx.messages().mobName(unholyMobType)));
				}
			}
		}

		if ((holyMobType != null) && mobType.equals(holyMobType.name()))
		{
			if (ctx.blessingService().cursePlayer(godName, player.getUniqueId(), ctx.godService().getGodPower(godName)))
			{
				ctx.moodService().addMoodForGod(godName, ctx.moodService().getAngryModifierForGod(godName));

				
				if (ctx.settings().commandments().broadcastMobSlain())
				{
					ctx.godSpeech().sayToBelievers(godName, Voice.BELIEVERS_NOT_SLAY_MOB_CURSING, 2 + this.random.nextInt(10), Messages.displayName(player.getDisplayName().toUpperCase(Locale.ROOT)), Messages.type(ctx.messages().mobName(holyMobType)));
				}
				else
				{
					ctx.godSpeech().sayToBeliever(godName, player.getUniqueId(), Voice.BELIEVERS_NOT_SLAY_MOB_CURSING, Messages.displayName(player.getDisplayName().toUpperCase(Locale.ROOT)), Messages.type(ctx.messages().mobName(holyMobType)));
				}
			}
		}
	}

	/**
	 * A believer's item burns as a sacrifice: the god judges it ({@link SacrificeRules}), and it floats up in the god's
	 * style ({@link EffectService#sacrifice})
	 */
	public void handleSacrifice(String godName, Player believer, Item burning)
	{
		if (believer == null)
		{
			return;
		}
		Material type = burning.getItemStack().getType();

		if (!ctx.plugin().isEnabledInWorld(believer.getWorld()))
		{
			return;
		}

		if (godName == null)
		{
			return;
		}

		int godPower = (int) ctx.godService().getGodPower(godName);

		ctx.plugin().log(believer.getDisplayName() + " sacrificed " + type.name() + " to " + godName);

		Material eatFoodType = getHolyFoodTypeForGod(godName);

		if (type == eatFoodType)
		{
			ctx.moodService().addMoodForGod(godName, ctx.moodService().getAngryModifierForGod(godName));
			ctx.blessingService().cursePlayer(godName, believer.getUniqueId(), ctx.godService().getGodPower(godName));



			if (ctx.settings().commandments().broadcastFoodEaten())
			{
				ctx.godSpeech().sayToBelievers(godName, Voice.BELIEVER_HOLY_FOOD_SACRIFICE, 2 + this.random.nextInt(10), Messages.displayName(believer.getDisplayName()), Messages.type(ctx.messages().itemName(eatFoodType)));
			}
			else
			{
				ctx.godSpeech().sayToBeliever(godName, believer.getUniqueId(), Voice.BELIEVER_HOLY_FOOD_SACRIFICE, Messages.displayName(believer.getDisplayName()), Messages.type(ctx.messages().itemName(eatFoodType)));
			}

			ctx.effectService().sacrifice(godName, burning, Theme.EffectMood.ANGRY);
			ctx.blessingService().smite(godName, believer, 1 + this.random.nextInt(3));

			return;
		}

		float value = getSacrificeValueForGod(godName, type);



		SacrificeRules.Verdict verdict = SacrificeRules.verdict(value);
		ctx.effectService().sacrifice(godName, burning, EffectRules.sacrificeMood(verdict));
		if (verdict == SacrificeRules.Verdict.GOOD)
		{
			ctx.rankService().countSacrifice(godName);
			ctx.devotionService().earn(believer.getUniqueId(), godName, DevotionService.Deed.SACRIFICE);
			ctx.moodService().addMoodForGod(godName, ctx.moodService().getPleasedModifierForGod(godName));
			ctx.believerService().addPrayer(believer.getUniqueId(), godName);

			ctx.blessingService().blessPlayer(godName, believer.getUniqueId(), godPower);
			ctx.godSpeech().sayToBeliever(godName, believer.getUniqueId(), Voice.BELIEVER_GOOD_SACRIFICE, Messages.displayName(believer.getDisplayName()), Messages.type(ctx.messages().itemName(type)));

			ctx.believerService().increasePrayerPower(believer.getUniqueId(), 1);
		}
		else if (verdict == SacrificeRules.Verdict.MEH)
		{
			ctx.godSpeech().sayToBeliever(godName, believer.getUniqueId(), Voice.BELIEVER_MEH_SACRIFICE, Messages.displayName(believer.getDisplayName()), Messages.type(ctx.messages().itemName(type)));
		}
		else
		{
			ctx.moodService().addMoodForGod(godName, ctx.moodService().getAngryModifierForGod(godName));
			ctx.blessingService().smite(godName, believer, 1 + this.random.nextInt(3));
			ctx.godSpeech().sayToBeliever(godName, believer.getUniqueId(), Voice.BELIEVER_BAD_SACRIFICE, Messages.displayName(believer.getDisplayName()), Messages.type(ctx.messages().itemName(type)));
		}

		setSacrificeValueForGod(godName, type, Float.valueOf(SacrificeRules.afterSacrifice(value)));

		this.repository.markDirty();
	}

	public void updateSacrificeNeeds(String godName)
	{
		if (!ctx.settings().sacrifices().enabled())
		{
			return;
		}

		int godPower = 1 + (int) ctx.godService().getGodPower(godName);
		if (this.random.nextInt(20 + (int) (70.0F / godPower)) > 0)
		{
			return;
		}
		
		Material type = getSacrificeNeedForGod(godName);
		if (type == null)
		{
			return;
		}

		float value = getSacrificeValueForGod(godName, type);

		value = SacrificeRules.wantMore(value, 1 + this.random.nextInt(3));
		ctx.plugin().logDebug("Increasing wanted " + type.name() + " sacrifice need for " + godName + " to " + value);

		setSacrificeValueForGod(godName, type, Float.valueOf(value));

		this.repository.markDirty();

		type = getSacrificeUnwantedForGod(godName);
		if (type != null)
		{
			value = SacrificeRules.forgetUnwanted(getSacrificeValueForGod(godName, type));
			ctx.plugin().logDebug("Reducing unwanted " + type.name() + " sacrifice need for " + godName + " to " + value);
			if (value == 0.0F)
			{
				setSacrificeValueForGod(godName, type, null);
			}
			else
			{
				setSacrificeValueForGod(godName, type, Float.valueOf(value));
			}
			this.repository.save();
		}
	}
}
