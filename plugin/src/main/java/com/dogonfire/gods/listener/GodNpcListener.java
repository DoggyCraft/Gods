package com.dogonfire.gods.listener;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.service.GodNpcService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * The gods' NPCs at their main altars ({@link GodNpcService}): a right click on one prays at the
 * altar, and nothing else happens to it (no item use, no damage). Clicks another plugin refused (a
 * protection plugin) don't pray. NPCs without a server-side entity (FancyNpcs) report their clicks
 * through their own plugin's event instead. Registered while {@code npc.enabled} is on.
 */
public class GodNpcListener implements Listener {
  private final GodsContext ctx;

  public GodNpcListener(GodsContext ctx) {
    this.ctx = ctx;
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
    String godName = ctx.godNpcs().godOf(event.getRightClicked());
    if (godName == null) {
      return;
    }
    // Bukkit fires it once per hand: both are cancelled, only the main hand's prays
    event.setCancelled(true);
    ctx.godNpcs().clicked(event.getPlayer(), godName, event.getHand() == EquipmentSlot.HAND);
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
    if (ctx.godNpcs().godOf(event.getRightClicked()) != null) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onEntityDamage(EntityDamageEvent event) {
    if (ctx.godNpcs().godOf(event.getEntity()) != null) {
      event.setCancelled(true);
    }
  }
}
