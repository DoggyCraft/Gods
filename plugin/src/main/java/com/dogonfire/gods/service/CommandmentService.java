package com.dogonfire.gods.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.model.God;
import com.dogonfire.gods.storage.GodRepository;

/**
 * A god's commandments and sacrifices: the food it wants eaten and the food it forbids, the mob to slay and the mob to
 * spare (each picked once and kept), what happens when a believer eats or kills, and the items it wants sacrificed at
 * its altars and how much (it wants more of some over time and forgets unwanted ones). The rules are in
 * {@link SacrificeRules}. Main thread only.
 */
public class CommandmentService
{
	private final Random			random	= new Random();
	private final GodsContext		ctx;
	private final GodRepository		repository;

	/** Holy and unholy food are picked from these */
	private static final Material[]	FOODS				= { Material.APPLE, Material.BREAD, Material.COOKED_SALMON, Material.MELON_SLICE, Material.COOKED_BEEF, Material.COOKED_PORKCHOP,
			Material.CARROT };

	/** What a god may come to want sacrificed; its seed picks five. null is the god's unholy food. */
	private static final Material[]	SACRIFICE_NEEDS		= { Material.POPPY, Material.COOKED_PORKCHOP, null, Material.RABBIT_HIDE, Material.RABBIT_FOOT, Material.CACTUS,
			Material.BREAD, Material.CARROT, Material.IRON_PICKAXE, Material.IRON_INGOT, Material.GOLD_INGOT, Material.APPLE, Material.BOOK, Material.CAKE, Material.MELON_SLICE,
			Material.COOKIE, Material.PUMPKIN, Material.SUGAR_CANE, Material.EGG, Material.WHEAT, Material.NETHERRACK, Material.POTATO, Material.BONE, Material.FEATHER };

	public CommandmentService(GodsContext ctx, GodRepository repository)
	{
		this.ctx = ctx;
		this.repository = repository;
	}

	private String generateHolyMobTypeForGod()
	{
		EntityType[] holyMobTypes = new EntityType[] { EntityType.CHICKEN, EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.RABBIT, EntityType.HORSE, EntityType.BAT };

		return holyMobTypes[this.random.nextInt(holyMobTypes.length)].name();
	}

	private String generateUnholyMobTypeForGod()
	{
		EntityType[] unholyMobTypes = new EntityType[] { EntityType.CHICKEN, EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.RABBIT, EntityType.HORSE, EntityType.BAT, EntityType.WOLF, EntityType.SKELETON, EntityType.ZOMBIE };

		return unholyMobTypes[this.random.nextInt(unholyMobTypes.length)].name();
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
			return EntityType.valueOf(mobTypeString.toUpperCase());
		}
		catch (IllegalArgumentException ex)
		{
			return null;
		}
	}

	public Material getHolyFoodTypeForGod(String godName)
	{
		God god = this.repository.forChange(godName);
		String foodTypeString = god != null ? god.holyFood() : null;
		
		Material foodType = Material.AIR;
		
		if (foodTypeString == null)
		{
			foodType = FOODS[this.random.nextInt(FOODS.length)];
			foodTypeString = foodType.name();

			if (god != null)
			{
				god.setHolyFood(foodTypeString);
			}

			this.repository.markDirty();
		}
		else
		{
			foodType = Material.getMaterial(foodTypeString);
		}
		
		if(foodType == null)
		{
			foodType = Material.CARROT;
		}
		
		return foodType;
	}

	public EntityType getHolyMobTypeForGod(String godName)
	{
		God god = this.repository.forChange(godName);
		EntityType mobType = parseEntityType(god != null ? god.holyMob() : null);
		if (mobType == null)
		{
			String unholyMobTypeString = getUnholyMobTypeForGod(godName).name();
			String mobTypeString;
			do
			{
				mobTypeString = generateHolyMobTypeForGod();
			}
			while (mobTypeString.equals(unholyMobTypeString));

			if (god != null)
			{
				god.setHolyMob(mobTypeString);
			}
			this.repository.markDirty();

			mobType = EntityType.valueOf(mobTypeString);
		}
		return mobType;
	}

	public Material getUnholyFoodTypeForGod(String godName)
	{
		God god = this.repository.forChange(godName);
		String foodTypeString = god != null ? god.unholyFood() : null;
		Material foodType = Material.AIR;
		if (foodTypeString == null)
		{
			do
			{
				foodType = FOODS[this.random.nextInt(FOODS.length)];
				foodTypeString = foodType.name();
			}
			while (foodTypeString.equals(getHolyFoodTypeForGod(godName).name()));
			
			if (god != null)
			{
				god.setUnholyFood(foodTypeString);
			}

			this.repository.markDirty();
		}
		else
		{
			foodType = Material.getMaterial(foodTypeString);
		}
		
		if(foodType == null)
		{
			foodType = Material.CARROT;
		}

		return foodType;
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

	private Material getSacrificeNeedForGod(String godName)
	{
		Random materialRandom = new Random(ctx.godService().getSeedForGod(godName));
		List<Integer> materials = new ArrayList<Integer>();

		for (int n = 0; n < 5; n++)
		{
			materials.add(materialRandom.nextInt(SACRIFICE_NEEDS.length));
		}

		int typeIndex = 0;
		Material type = Material.AIR;

		do
		{
			typeIndex = materials.get(this.random.nextInt(materials.size())).intValue();

			// null: the god's unholy food
			type = SACRIFICE_NEEDS[typeIndex] != null ? SACRIFICE_NEEDS[typeIndex] : getUnholyFoodTypeForGod(godName);
		}
		while (type == getHolyFoodTypeForGod(godName) || type == Material.AIR);

		return type;
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

	public EntityType getUnholyMobTypeForGod(String godName)
	{
		God god = this.repository.forChange(godName);
		EntityType mobType = parseEntityType(god != null ? god.unholyMob() : null);
		if (mobType == null)
		{
			String mobTypeString = generateUnholyMobTypeForGod();

			if (god != null)
			{
				god.setUnholyMob(mobTypeString);
			}
			this.repository.markDirty();

			mobType = EntityType.valueOf(mobTypeString);
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
					ctx.godSpeech().sayToBelievers(godName, Voice.BELIEVERS_NOT_EAT_FOOD_CURSING, 2 + this.random.nextInt(10), Messages.displayName(player.getDisplayName().toUpperCase()), Messages.type(ctx.messages().itemName(notEatFoodType)));
				}
				else
				{
					ctx.godSpeech().sayToBeliever(godName, player.getUniqueId(), Voice.BELIEVERS_NOT_EAT_FOOD_CURSING, Messages.displayName(player.getDisplayName().toUpperCase()), Messages.type(ctx.messages().itemName(notEatFoodType)));
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
					ctx.godSpeech().sayToBelievers(godName, Voice.BELIEVERS_NOT_SLAY_MOB_CURSING, 2 + this.random.nextInt(10), Messages.displayName(player.getDisplayName().toUpperCase()), Messages.type(ctx.messages().mobName(holyMobType)));
				}
				else
				{
					ctx.godSpeech().sayToBeliever(godName, player.getUniqueId(), Voice.BELIEVERS_NOT_SLAY_MOB_CURSING, Messages.displayName(player.getDisplayName().toUpperCase()), Messages.type(ctx.messages().mobName(holyMobType)));
				}
			}
		}
	}

	public void handleSacrifice(String godName, Player believer, Material type)
	{
		if (believer == null)
		{
			return;
		}

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

			ctx.blessingService().smite(godName, believer, 1 + this.random.nextInt(3));

			return;
		}

		float value = getSacrificeValueForGod(godName, type);



		SacrificeRules.Verdict verdict = SacrificeRules.verdict(value);
		if (verdict == SacrificeRules.Verdict.GOOD)
		{
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
