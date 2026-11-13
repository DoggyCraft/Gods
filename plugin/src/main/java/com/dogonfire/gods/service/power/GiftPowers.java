package com.dogonfire.gods.service.power;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.service.HolyPowerRules;
import com.dogonfire.gods.task.BoostKnowledgeTask;
import com.dogonfire.gods.task.DrunkTask;
import com.dogonfire.gods.task.FireworkTask;
import com.dogonfire.gods.task.HealRadiusTask;
import com.dogonfire.gods.util.SafeSpots;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.BlockIterator;

/**
 * The powers that do something for the player or those around: knowledge, healing, fireworks, drink
 * and teleporting
 */
public final class GiftPowers {
  // How far TELEPORT lets a player drop to the ground below where they look
  private static final int SAFE_SPOT_DROP = 6;

  private final GodsContext ctx;

  public GiftPowers(GodsContext ctx) {
    this.ctx = ctx;
  }

  /** KNOWLEDGE: powerValue levels of xp */
  public void boostKnowledge(Player player, int powerValue) {
    ctx.plugin().logDebug("Boosting " + powerValue + " xp of knowledge");

    ctx.plugin()
        .getServer()
        .getScheduler()
        .scheduleSyncDelayedTask(ctx.plugin(), new BoostKnowledgeTask(ctx, player, powerValue), 1L);
  }

  /** HEALING: heals up to powerValue other players around */
  public void healing(Player player, int powerValue) {
    ctx.plugin().logDebug("Healing up to " + powerValue + " creatures");

    ctx.plugin()
        .getServer()
        .getScheduler()
        .scheduleSyncDelayedTask(ctx.plugin(), new HealRadiusTask(ctx, player, powerValue), 1L);
  }

  /** FIREWORK: powerValue rockets, then fewer in later rounds */
  public void shootFirework(Player player, int powerValue) {
    ctx.plugin().logDebug("Starting fireworks with " + powerValue + " rockets");

    ctx.plugin()
        .getServer()
        .getScheduler()
        .scheduleSyncDelayedTask(ctx.plugin(), new FireworkTask(ctx, player, powerValue), 1L);
  }

  /** DRUNK: nausea for powerValue seconds */
  public void drunk(Player player, int powerValue) {
    ctx.plugin().logDebug("Getting " + player.getName() + " drunk for " + powerValue + " seconds");

    ctx.plugin()
        .getServer()
        .getScheduler()
        .scheduleSyncDelayedTask(ctx.plugin(), new DrunkTask(ctx, player, powerValue), 1L);
  }

  /**
   * TELEPORT: to the farthest safe spot (solid ground, two free blocks, no liquid or fire) along
   * where the player looks, up to {@link HolyPowerRules#teleportDistance} blocks away, stopping at
   * the first block or liquid in the way and at unloaded chunks. To Bukkit it is an ender pearl
   * teleport, so holy land (and other plugins' regions) keep the player out as they keep out ender
   * pearls.
   */
  public void teleport(Player player, int powerStrength) {
    World world = player.getWorld();
    Location eye = player.getEyeLocation();

    // The free blocks along the line of sight, in loaded chunks only
    List<Block> path = new ArrayList<Block>();
    BlockIterator blocks =
        new BlockIterator(
            world,
            eye.toVector(),
            eye.getDirection(),
            0.0D,
            HolyPowerRules.teleportDistance(powerStrength));
    while (blocks.hasNext()) {
      Block block = blocks.next();
      if (block.getY() < world.getMinHeight()
          || block.getY() >= world.getMaxHeight()
          || !world.isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)
          || !isFree(block)) {
        break;
      }
      path.add(block);
    }

    Block feet = null;
    for (int i = path.size() - 1; i >= 0 && feet == null; i--) {
      feet = safeSpotBelow(world, path.get(i));
    }
    if (feet == null) {
      player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0F, 1.5F);
      return;
    }

    Location to = feet.getLocation().add(0.5D, 0.0D, 0.5D);
    to.setPitch(eye.getPitch());
    to.setYaw(eye.getYaw());
    if (!player.teleport(to, TeleportCause.ENDER_PEARL)) {
      return;
    }
    world.playEffect(to, Effect.ENDER_SIGNAL, 0);
    world.playSound(to, Sound.ENTITY_ENDERMAN_AMBIENT, 1.0F, 0.3F);
  }

  // The highest safe spot in the column at or a few blocks below a free block, or null
  private static Block safeSpotBelow(World world, Block from) {
    for (int down = 0; down <= SAFE_SPOT_DROP; down++) {
      Block feet = from.getRelative(0, -down, 0);
      Block ground = feet.getRelative(0, -1, 0);
      if (ground.getY() < world.getMinHeight()) {
        // The void
        return null;
      }
      if (SafeSpots.isSafe(world, feet)) {
        return feet;
      }
      if (!SafeSpots.isFree(ground)) {
        // Ground that isn't safe: nothing lower in this column
        return null;
      }
    }
    return null;
  }

  private static boolean isFree(Block block) {
    return SafeSpots.isFree(block);
  }
}
