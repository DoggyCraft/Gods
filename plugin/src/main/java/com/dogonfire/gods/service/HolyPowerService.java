package com.dogonfire.gods.service;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.model.HolyPower;
import com.dogonfire.gods.service.power.AttackPowers;
import com.dogonfire.gods.service.power.GiftPowers;
import com.dogonfire.gods.service.power.NaturePowers;
import com.dogonfire.gods.service.power.SkyPowers;

/**
 * Holy powers, the effects of holy artifacts: checks the cooldowns ({@link PowerCooldowns}) and starts the power. The
 * powers themselves are in {@code service/power}, grouped by kind; their texts and numbers in {@link HolyPowerRules}.
 * Fireworks are also used by marriages. Keeps no data file. Main thread only.
 */
public class HolyPowerService
{
	private final GodsContext		ctx;
	private final PowerCooldowns	cooldowns	= new PowerCooldowns();
	private final SkyPowers			sky;
	private final GiftPowers		gifts;
	private final NaturePowers		nature;
	private final AttackPowers		attacks;

	public HolyPowerService(GodsContext ctx)
	{
		this.ctx = ctx;
		this.sky = new SkyPowers(ctx);
		this.gifts = new GiftPowers(ctx);
		this.nature = new NaturePowers();
		this.attacks = new AttackPowers(ctx.plugin());
	}

	/**
	 * Uses a power, unless the player used it too recently or (for the sun, moon and storm powers) another one is still
	 * changing the world. Tells the player why not and returns false in that case.
	 */
	public boolean activatePower(Player player, HolyPower powerType, int powerStrength)
	{
		PowerCooldowns.Result result = this.cooldowns.use(player.getUniqueId(), player.getWorld().getUID(), powerType, powerStrength, System.currentTimeMillis());
		if (result instanceof PowerCooldowns.Cooling cooling)
		{
			ctx.messages().send(player, "artifact.cooling-down", Messages.text("seconds", cooling.seconds()));
			return false;
		}
		if (result instanceof PowerCooldowns.WorldBusy)
		{
			ctx.messages().send(player, "artifact.world-busy");
			return false;
		}

		switch (powerType)
		{
		case KNOWLEDGE -> this.gifts.boostKnowledge(player, powerStrength);
		case CALLMOON -> this.sky.callMoon(player, powerStrength);
		case CALLSUN -> this.sky.callSun(player, powerStrength);
		case HEALING -> this.gifts.healing(player, powerStrength);
		case TAME -> this.nature.tame(player, powerStrength);
		case LIGHTNING -> this.sky.lightning(player, powerStrength);
		case LIGHTNING_STORM -> this.sky.lightningStorm(player, powerStrength);
		case TELEPORT -> this.gifts.teleport(player, powerStrength);
		case FREEZE -> this.nature.freeze(player, powerStrength);
		case ICE -> this.attacks.ice(player, powerStrength);
		case FIREBALL -> this.attacks.fireBall(player, powerStrength);
		case EARTHQUAKE -> this.attacks.earthQuake(player, powerStrength);
		case NATURE -> this.nature.growNature(player, powerStrength);
		case FIREWORK -> this.gifts.shootFirework(player, powerStrength);
		case DRUNK -> this.gifts.drunk(player, powerStrength);
		}
		return true;
	}

	/** Whether this block is the ice of an ice spike, which can't be broken */
	public boolean isEncased(Block block)
	{
		return this.attacks.isEncased(block);
	}

	/**
	 * Puts back at once what powers changed for a while (the ice of ice spikes). Call it before the scheduler's tasks are
	 * cancelled when the plugin stops, or the blocks stay changed.
	 */
	public void stop()
	{
		this.attacks.restoreAll();
	}

	/** Fireworks around the player (also for weddings and love), without a cooldown */
	public void shootFirework(Player player, int rockets)
	{
		this.gifts.shootFirework(player, rockets);
	}
}
