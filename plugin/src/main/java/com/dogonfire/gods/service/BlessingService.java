package com.dogonfire.gods.service;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.api.event.GodBlessPlayerEvent;
import com.dogonfire.gods.api.event.GodCursePlayerEvent;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.model.God;
import com.dogonfire.gods.model.GodType;
import com.dogonfire.gods.storage.GodRepository;
import com.dogonfire.gods.task.GiveHolyArtifactTask;
import com.dogonfire.gods.task.GiveItemTask;
import com.dogonfire.gods.task.HealPlayerTask;
import com.dogonfire.gods.task.SpawnHostileMobsTask;
import com.dogonfire.gods.util.LegacyDates;
import com.dogonfire.gods.util.Lightning;

/**
 * What gods do to players: blessings (potion effects, items, healing, holy artifacts) and curses (bad effects,
 * lightning, hostile mobs), and the player a god blesses or curses for a while after a priest asked it to at an altar.
 * The rules are in {@link BlessingRules}. Main thread only.
 */
public class BlessingService
{
	private final Random			random	= new Random();
	private final GodsContext		ctx;
	private final GodRepository		repository;

	// The tools a god gives, by tier (see BlessingRules.toolTier)
	private static final Material[]	PICKAXES	= { Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE };
	private static final Material[]	SWORDS		= { Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD };
	private static final Material[]	SHOVELS		= { Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.DIAMOND_SHOVEL };
	private static final Material[]	AXES		= { Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.DIAMOND_AXE };
	private static final Material[]	HOES		= { Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE, Material.DIAMOND_HOE };

	public BlessingService(GodsContext ctx, GodRepository repository)
	{
		this.ctx = ctx;
		this.repository = repository;
	}

	public boolean blessPlayer(String godName, UUID playerId, float godPower)
	{
		Player player = ctx.plugin().getServer().getPlayer(playerId);

		if (player == null)
		{
			return false;
		}

		if (ctx.believerService().hasRecentBlessing(playerId))
		{
			return false;
		}

		Settings.Blessings blessings = ctx.settings().blessings();
		int blessingType = BlessingRules.pickKind(this.random, BlessingRules.BLESSING_KINDS, kind -> switch (kind)
		{
			case 0 -> blessings.fastDigging();
			case 1 -> blessings.heal();
			case 2 -> blessings.regeneration();
			case 3 -> blessings.speed();
			default -> blessings.increaseDamage();
		});

		int ticks = BlessingRules.blessingTicks(godPower);

		if (ctx.fire(new GodBlessPlayerEvent(godName, player, GodBlessPlayerEvent.Blessing.EFFECT)).isCancelled())
		{
			return false;
		}

		switch (blessingType)
		{
		case 0:
			player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, ticks, 1));
			break;
		case 1:
			player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, ticks, 1));
			break;
		case 2:
			player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, ticks, 1));
			break;
		case 3:
			player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ticks, 1));
			break;
		case 4:
			player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, ticks, 1));
		}

		ctx.believerService().setBlessingTime(player.getUniqueId());
		ctx.effectService().blessing(godName, player);

		return true;
	}

	/** Gives the player a holy artifact of the god's type; false if they can't get one (see {@link #giveHolyArtifact}) */
	public boolean blessPlayerWithHolyArtifact(String godName, Player player)
	{
		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return false;
		}
		return giveHolyArtifact(godName, ctx.godService().getDivineForceForGod(godName), player, true);
	}

	public ItemStack blessPlayerWithItem(String godName, Player player)
	{
		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return null;
		}
		ItemStack item = getItemNeed(godName, player);
		if (item == null || !giveItem(godName, player, item.getType(), true))
		{
			return null;
		}
		return item;
	}

	public boolean cursePlayer(String godName, UUID playerId, float godPower)
	{
		Player player = ctx.plugin().getServer().getPlayer(playerId);

		if (player == null)
		{
			return false;
		}

		if (ctx.believerService().hasRecentCursing(playerId))
		{
			return false;
		}

		Settings.Curses curses = ctx.settings().curses();
		int curseType = BlessingRules.pickKind(this.random, BlessingRules.CURSE_KINDS,
				kind -> !((kind == BlessingRules.CURSE_LIGHTNING && !curses.lightning()) || (kind == BlessingRules.CURSE_MOBS && !curses.mobs())));

		int ticks = BlessingRules.curseTicks(godPower);

		GodCursePlayerEvent.Curse curse = switch (curseType)
		{
			case BlessingRules.CURSE_LIGHTNING -> GodCursePlayerEvent.Curse.LIGHTNING;
			case BlessingRules.CURSE_MOBS -> GodCursePlayerEvent.Curse.MOBS;
			default -> GodCursePlayerEvent.Curse.EFFECT;
		};
		if (ctx.fire(new GodCursePlayerEvent(godName, player, curse)).isCancelled())
		{
			return false;
		}

		switch (curseType)
		{
		case 0:
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, ticks, 1));
			break;
		case 1:
			player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, ticks, 1));
			break;
		case 2:
			player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, ticks, 1));
			break;
		case 3:
			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, 1));
			break;
		case 4:
			player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, ticks, 1));
			break;
		case 5:
			strikePlayerWithLightning(playerId, 1);
			break;
		case 6:
			strikePlayerWithMobs(godName, playerId, godPower);
		}

		ctx.believerService().setCursingTime(player.getUniqueId());
		ctx.effectService().curse(godName, player);

		return true;
	}

	public UUID getBlessedPlayerForGod(String godName)
	{
		God god = this.repository.find(godName);
		if (god == null || god.blessedTime() == null)
		{
			return null;
		}

		UUID blessedPlayer = god.blessedPlayer();
		if (blessedPlayer == null || BlessingRules.blessingOver(god.blessedTime(), Instant.now(), ctx.settings().blessings().blessedSeconds()))
		{
			setBlessedPlayerForGod(godName, null);

			return null;
		}
		return blessedPlayer;
	}

	public Player getCursedPlayerForGod(String godName)
	{
		God god = this.repository.find(godName);
		if (god == null || god.cursedTime() == null)
		{
			return null;
		}

		UUID cursedPlayer = god.cursedPlayer();
		if (cursedPlayer == null || BlessingRules.curseOver(god.cursedTime(), Instant.now(), ctx.settings().curses().cursedMinutes()))
		{
			setCursedPlayerForGod(godName, null);

			return null;
		}

		return ctx.plugin().getServer().getPlayer(cursedPlayer);
	}

	public double getHealthNeed(String godName, Player player)
	{
		return player.getAttribute(Attribute.MAX_HEALTH).getValue() - player.getHealth();
	}

	/** What the player lacks, in this order: the god's holy food, a pickaxe, a sword, a shovel, an axe, a hoe; or null */
	private ItemStack getItemNeed(String godName, Player player)
	{
		if (!hasFood(player, godName))
		{
			return new ItemStack(ctx.commandmentService().getHolyFoodTypeForGod(godName));
		}
		if (!hasAny(player, PICKAXES))
		{
			return new ItemStack(toolBlessing(godName, PICKAXES));
		}
		if (!hasSword(player))
		{
			return new ItemStack(toolBlessing(godName, SWORDS));
		}
		if (!hasAny(player, SHOVELS))
		{
			return new ItemStack(toolBlessing(godName, SHOVELS));
		}
		if (!hasAny(player, AXES))
		{
			return new ItemStack(toolBlessing(godName, AXES));
		}
		if (!hasAny(player, HOES))
		{
			return new ItemStack(toolBlessing(godName, HOES));
		}
		return null;
	}

	/** The tool of the tier the god's power allows (see {@link BlessingRules#toolTier}) */
	private Material toolBlessing(String godName, Material[] tiers)
	{
		Settings.ItemBlessings items = ctx.settings().itemBlessings();
		return tiers[BlessingRules.toolTier(ctx.godService().getGodPower(godName), items.godPowerForLevel1Items(), items.godPowerForLevel2Items(), items.godPowerForLevel3Items())];
	}

	private static boolean hasAny(Player player, Material[] tools)
	{
		PlayerInventory inventory = player.getInventory();
		for (Material tool : tools)
		{
			if (inventory.contains(tool))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Drops a holy artifact in front of the player (a {@link GodBlessPlayerEvent})
	 *
	 * @return false if the god has no type, they carry one already or a listener cancelled it
	 */
	public boolean giveHolyArtifact(String godName, GodType godType, Player player, boolean speak)
	{
		// A god without a type has no kind of artifact to give
		if (godType == null)
		{
			return false;
		}
		// A second artifact would only drop at their feet: the pickup is refused while they carry one
		if (ctx.holyArtifactService().hasHolyArtifact(player))
		{
			return false;
		}
		if (ctx.fire(new GodBlessPlayerEvent(godName, player, GodBlessPlayerEvent.Blessing.HOLY_ARTIFACT)).isCancelled())
		{
			return false;
		}
		ctx.plugin().getServer().getScheduler().runTaskLater(ctx.plugin(), new GiveHolyArtifactTask(ctx, godName, godType, player, speak), 2L);
		return true;
	}

	/**
	 * Drops an item in front of the player (a {@link GodBlessPlayerEvent})
	 *
	 * @return false if a listener cancelled it
	 */
	public boolean giveItem(String godName, Player player, Material material, boolean speak)
	{
		if (ctx.fire(new GodBlessPlayerEvent(godName, player, GodBlessPlayerEvent.Blessing.ITEM)).isCancelled())
		{
			return false;
		}
		ctx.plugin().getServer().getScheduler().runTaskLater(ctx.plugin(), new GiveItemTask(ctx, godName, player, material, speak), 2L);
		return true;
	}

	private boolean hasFood(Player player, String godName)
	{
		PlayerInventory inventory = player.getInventory();
		if (inventory.contains(ctx.commandmentService().getHolyFoodTypeForGod(godName)))
		{
			return true;
		}
		return false;
	}

	private boolean hasSword(Player player)
	{
		PlayerInventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getSize(); i++)
		{
			ItemStack stack = inventory.getItem(i);
			if ((stack != null) && ((stack.getType().equals(Material.WOODEN_SWORD)) || (stack.getType().equals(Material.STONE_SWORD)) || (stack.getType().equals(Material.IRON_SWORD)) || (stack.getType().equals(Material.DIAMOND_SWORD))) && (stack.getAmount() != 0))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Heals the player (a {@link GodBlessPlayerEvent}); the god says so
	 *
	 * @return false if a listener cancelled it
	 */
	public boolean healPlayer(String godName, Player player)
	{
		if (ctx.fire(new GodBlessPlayerEvent(godName, player, GodBlessPlayerEvent.Blessing.HEAL)).isCancelled())
		{
			return false;
		}
		ctx.plugin().getServer().getScheduler().runTaskLater(ctx.plugin(), new HealPlayerTask(ctx, godName, player, Voice.BELIEVER_HEALTH_BLESSING), 2L);
		return true;
	}

	public void manageBlessedPlayer(String godName)
	{
		if (!ctx.settings().blessings().enabled())
		{
			return;
		}
		UUID blessedPlayer = getBlessedPlayerForGod(godName);
		if (blessedPlayer == null)
		{
			return;
		}

		int godPower = 1 + (int) ctx.godService().getGodPower(godName);

		if (this.random.nextInt(1 + 100 / godPower) == 0)
		{
			blessBlessedPlayer(godName, ctx.plugin().getServer().getPlayer(blessedPlayer));
		}
	}

	/**
	 * The god blesses its blessed player now: when a priest names them, and now and then after that. Not while they were
	 * blessed too recently ({@link #blessPlayer}'s cooldown) or lack gods.blessings.
	 *
	 * @return whether they were blessed
	 */
	public boolean blessBlessedPlayer(String godName, Player player)
	{
		if ((player == null) || (!ctx.permissions().hasPermission(player, "gods.blessings")))
		{
			return false;
		}
		if (!blessPlayer(godName, player.getUniqueId(), ctx.godService().getGodPower(godName)))
		{
			return false;
		}
		ctx.godSpeech().say(godName, player, Voice.PLAYER_BLESSED, 2 + this.random.nextInt(10), Messages.player(player.getName()));
		ctx.godSpeech().sayToBelieversExcept(godName, Voice.BELIEVERS_PLAYER_BLESSED, player.getUniqueId(), Messages.player(player.getName()));
		return true;
	}

	public void manageCursedPlayer(String godName)
	{
		if (!ctx.settings().curses().enabled())
		{
			return;
		}

		Player cursedPlayer = getCursedPlayerForGod(godName);

		if (cursedPlayer == null)
		{
			return;
		}

		int godPower = 1 + (int) ctx.godService().getGodPower(godName);

		if (this.random.nextInt(1 + 100 / godPower) == 0)
		{
			curseCursedPlayer(godName, cursedPlayer);
		}
	}

	/**
	 * The god curses its cursed player now: when a priest names them, and now and then after that. Not while they were
	 * cursed too recently ({@link #cursePlayer}'s cooldown) or lack gods.curses.
	 *
	 * @return whether they were cursed
	 */
	public boolean curseCursedPlayer(String godName, Player cursedPlayer)
	{
		if ((cursedPlayer == null) || (!ctx.permissions().hasPermission(cursedPlayer, "gods.curses")))
		{
			return false;
		}
		if (!cursePlayer(godName, cursedPlayer.getUniqueId(), 1 + (int) ctx.godService().getGodPower(godName)))
		{
			return false;
		}
		ctx.godSpeech().say(godName, cursedPlayer, Voice.PLAYER_CURSED, 2 + this.random.nextInt(10), Messages.displayName(cursedPlayer.getDisplayName()));
		ctx.godSpeech().sayToBelieversExcept(godName, Voice.BELIEVERS_PLAYER_CURSED, cursedPlayer.getUniqueId(), Messages.displayName(cursedPlayer.getDisplayName()));
		return true;
	}

	/**
	 * Makes the player the god's blessed player from now on, or clears it when believerId is null.
	 */
	public void setBlessedPlayerForGod(String godName, UUID believerId)
	{
		God god = believerId == null ? this.repository.find(godName) : this.repository.forChange(godName);
		if (god != null)
		{
			god.setBlessed(believerId, believerId == null ? null : LegacyDates.SECONDS.now());
		}

		this.repository.markDirty();
	}

	/**
	 * Makes the player the god's cursed player from now on, or clears it when believerId is null.
	 */
	public void setCursedPlayerForGod(String godName, UUID believerId)
	{
		God god = believerId == null ? this.repository.find(godName) : this.repository.forChange(godName);
		if (god != null)
		{
			god.setCursed(believerId, believerId == null ? null : LegacyDates.SECONDS.now());
		}

		this.repository.markDirty();
	}

	public void spawnHostileMobs(String godName, Player player, EntityType mobType, int numberOfMobs)
	{
		ctx.plugin().getServer().getScheduler().runTaskLater(ctx.plugin(), new SpawnHostileMobsTask(ctx, godName, player, mobType, numberOfMobs), 2L);
	}

	/**
	 * The god strikes the player with lightning (a {@link GodCursePlayerEvent}); {@code damage} 0 or less only looks
	 * like lightning
	 *
	 * @return false if a listener cancelled it
	 */
	public boolean smite(String godName, Player player, int damage)
	{
		if (ctx.fire(new GodCursePlayerEvent(godName, player, GodCursePlayerEvent.Curse.LIGHTNING)).isCancelled())
		{
			return false;
		}
		return strikePlayerWithLightning(player.getUniqueId(), damage);
	}

	/** Lightning strikes the player, without an event: for curses that fired one already */
	private boolean strikePlayerWithLightning(UUID playerId, int damage)
	{
		Player player = ctx.plugin().getServer().getPlayer(playerId);

		if (player != null)
		{
			if (damage <= 0)
			{
				player.getWorld().strikeLightningEffect(player.getLocation());
			}
			else
			{
				// Lightning that sets no fire and changes no creature; 1 hurts as much as a real bolt, each point more half a heart more
				Lightning.strike(player, Lightning.LIGHTNING_DAMAGE + damage - 1);
			}
		}
		return true;
	}

	public boolean strikePlayerWithMobs(String godName, UUID playerId, float godPower)
	{
		Player player = ctx.plugin().getServer().getPlayer(playerId);

		if (player == null)
		{
			ctx.plugin().logDebug("player is null");
			return false;
		}

		// The god type's curse mobs (themes.yml curse-mobs), else the default theme's
		List<EntityType> curseMobs = ctx.commandmentService().themeForGod(godName).curseMobs();
		EntityType mobType = BlessingRules.curseMob(this.random, curseMobs.isEmpty() ? ctx.themes().defaults().curseMobs() : curseMobs);
		if (mobType == null)
		{
			ctx.plugin().logDebug(godName + " has no mob to curse with (themes.yml curse-mobs)");
			return false;
		}
		int numberOfMobs = BlessingRules.curseMobs(godPower);

		spawnHostileMobs(godName, player, mobType, numberOfMobs);

		return true;
	}
}
