package com.dogonfire.gods.service.power;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.task.CallMoonTask;
import com.dogonfire.gods.task.CallSunTask;
import com.dogonfire.gods.task.ThunderStormTask;
import com.dogonfire.gods.util.Lightning;
import com.dogonfire.gods.util.NearbyEntities;
import org.bukkit.Sound;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * The powers of the sky: calling the moon or the sun, a thunderstorm, and lightning on the enemies
 * around
 */
public final class SkyPowers {
  private final GodsContext ctx;

  public SkyPowers(GodsContext ctx) {
    this.ctx = ctx;
  }

  /** CALLMOON: night for powerValue minutes */
  public void callMoon(Player player, int powerValue) {
    ctx.plugin().logDebug("Calling the moon for " + powerValue + " minutes");

    ctx.plugin()
        .getServer()
        .getScheduler()
        .scheduleSyncDelayedTask(
            ctx.plugin(),
            new CallMoonTask(ctx, player, System.currentTimeMillis() + powerValue * 60000L),
            1L);
  }

  /** CALLSUN: day for powerValue minutes */
  public void callSun(Player player, int powerValue) {
    ctx.plugin().logDebug("Calling the sun for " + powerValue + " minutes");

    ctx.plugin()
        .getServer()
        .getScheduler()
        .scheduleSyncDelayedTask(
            ctx.plugin(),
            new CallSunTask(ctx, player, System.currentTimeMillis() + powerValue * 60000L),
            1L);
  }

  /** LIGHTNING_STORM: a storm with lightning around the player for powerValue minutes */
  public void lightningStorm(Player player, int powerValue) {
    player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0F, 0.1F);

    player.getWorld().setStorm(true);

    ctx.plugin().logDebug("Starting thunderstorm for " + powerValue + " minutes");

    ctx.plugin()
        .getServer()
        .getScheduler()
        .scheduleSyncDelayedTask(
            ctx.plugin(),
            new ThunderStormTask(ctx, player, System.currentTimeMillis() + powerValue * 60_000L),
            60L);
  }

  /** LIGHTNING: strikes up to powerStrength living things within 10 blocks */
  /**
   * Lightning only strikes hostile mobs and believers of gods at war with the player's god, never
   * pets, villagers or friends
   */
  private boolean isEnemy(Player player, Entity entity) {
    if (entity instanceof Enemy) {
      return true;
    }
    if (entity instanceof Player other) {
      String god = this.ctx.believerService().getGodForBeliever(player.getUniqueId());
      String otherGod = this.ctx.believerService().getGodForBeliever(other.getUniqueId());
      return god != null
          && otherGod != null
          && this.ctx.relationService().hasWarRelation(god, otherGod);
    }
    return false;
  }

  public void lightning(Player player, int powerStrength) {
    int strikes = 0;
    for (Entity entity : NearbyEntities.living(player.getLocation(), 10.0D)) {
      if (strikes >= powerStrength) {
        break;
      }
      if (entity.getEntityId() != player.getEntityId() && isEnemy(player, entity)) {
        Lightning.strike((LivingEntity) entity, Lightning.LIGHTNING_DAMAGE);

        strikes++;
      }
    }
  }
}
