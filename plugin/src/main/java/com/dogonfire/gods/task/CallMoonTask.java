package com.dogonfire.gods.task;

import com.dogonfire.gods.GodsContext;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Speeds time up until night falls, then keeps it night until stopTime
 * (System.currentTimeMillis()).
 */
public class CallMoonTask extends Task {
  private static final long NIGHT_START = 13000L;
  private static final long NIGHT_END = 23000L;

  private long stopTime;
  private Player player;
  private World world;

  public CallMoonTask(GodsContext ctx, Player player, long stopTime) {
    super(ctx);
    this.stopTime = stopTime;
    this.player = player;
    this.world = player.getWorld();
  }

  @Override
  public void run() {
    // It is the world the player called the moon in; leaving it ends the power
    if (!this.player.isOnline()
        || !this.world.equals(this.player.getWorld())
        || System.currentTimeMillis() >= this.stopTime) {
      return;
    }

    long time = this.world.getFullTime() % 24000L;

    if ((time < NIGHT_START) || (time >= NIGHT_END)) {
      this.player.playSound(this.player.getLocation(), Sound.BLOCK_STONE_STEP, 1.0F, 0.1F);
      this.world.setFullTime(this.world.getFullTime() + 1000L);
    } else if (time >= NIGHT_END - 1000L) {
      // Hold the night: turn the clock back before dawn
      this.world.setFullTime(this.world.getFullTime() - 1000L);
    }

    getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), this, 20L);
  }
}
