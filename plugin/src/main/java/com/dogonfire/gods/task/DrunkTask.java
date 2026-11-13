package com.dogonfire.gods.task;

import com.dogonfire.gods.GodsContext;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DrunkTask extends Task {
  private Player player;
  private long seconds;

  public DrunkTask(GodsContext ctx, Player player, long seconds) {
    super(ctx);
    this.player = player;
    this.seconds = seconds;
  }

  @Override
  public void run() {
    if (!this.player.isOnline()) {
      return;
    }

    int ticks = (int) Math.max(1L, Math.min(this.seconds, 3600L)) * 20;

    this.player.playSound(this.player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0F, 0.5F);
    this.player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, ticks, 0));
    this.player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, 0));
  }
}
