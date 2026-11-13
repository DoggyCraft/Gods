package com.dogonfire.gods.service.power;

import com.dogonfire.gods.service.HolyPowerRules;
import com.dogonfire.gods.util.NearbyEntities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * The powers that hit something: a fireball, an ice spike and an earthquake. The ice an ice spike
 * leaves is packed ice (it doesn't melt into water), can't be broken ({@link #isEncased}) and is
 * put back as it was after a few seconds, or at once by {@link #restoreAll} when the plugin stops.
 */
public final class AttackPowers {
  /** What the ice spike encases its target in: doesn't melt, and breaking it leaves no water */
  static final Material ENCASING = Material.PACKED_ICE;

  // How far around the spot where the earthquake stops it hurts and throws up creatures
  private static final double QUAKE_RADIUS = 3.0D;

  private final Plugin plugin;
  private final Random random = new Random();

  // Every block an ice spike turned to ice and what it was before, until it is put back
  private final Map<Block, BlockData> encased = new LinkedHashMap<Block, BlockData>();

  public AttackPowers(Plugin plugin) {
    this.plugin = plugin;
  }

  /** FIREBALL: a small fireball where the player looks */
  public void fireBall(Player player, int power) {
    player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1.0F, 1.0F);
    player.launchProjectile(SmallFireball.class);
  }

  /**
   * EARTHQUAKE: a shockwave runs along the ground ahead of the player (up to 10 steps). At the
   * first creature it reaches it bursts: the creatures within a few blocks are hurt (by the caster,
   * so holy land and PvP rules apply) and thrown a few blocks into the air. Breaks no blocks and
   * never hurts the caster.
   */
  public void earthQuake(final Player player, final int value) {
    final Location location = player.getLocation().add(0.0D, -0.2D, 0.0D);
    final Vector direction = player.getLocation().getDirection();
    direction.setY(0);
    if (direction.lengthSquared() < 1.0E-6D) {
      // Looking straight up or down
      return;
    }
    direction.normalize();
    BukkitRunnable task =
        new BukkitRunnable() {
          private int count = 0;

          @Override
          public void run() {
            Location above = location.clone().add(0.0D, 1.0D, 0.0D);
            if (!player.isOnline()
                || (above.getBlock().getType().isSolid())
                || (!location.getBlock().getType().isSolid())) {
              cancel();
              return;
            }
            Location temp = location.clone();
            for (int x = -2; x <= 2; x++) {
              for (int z = -2; z <= 2; z++) {
                temp.setX(x + location.getBlockX());
                temp.setZ(z + location.getBlockZ());
                Block block = temp.getBlock();
                playBlockBreakEffect(temp, block);
              }
            }

            boolean hit = false;
            for (Entity e : NearbyEntities.all(location, 1.5D)) {
              if (e != player && e instanceof LivingEntity) {
                hit = true;
                break;
              }
            }
            if (hit) {
              burst(player, location.clone().add(0.0D, 1.0D, 0.0D), value);
              cancel();
              return;
            }
            location.add(direction);
            if (this.count >= 10) {
              cancel();
            }
            this.count += 1;
          }
        };
    task.runTaskTimer(this.plugin, 0L, 3L);
  }

  // The earthquake's burst: an explosion to see and hear (that touches nothing), then damage and a
  // throw upwards
  private void burst(Player caster, Location center, int value) {
    World world = center.getWorld();
    world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
    world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 4.0F, 0.7F);

    double damage = HolyPowerRules.earthquakeDamage(value);
    for (Entity e : world.getNearbyEntities(center, QUAKE_RADIUS, QUAKE_RADIUS, QUAKE_RADIUS)) {
      if (e == caster
          || !(e instanceof LivingEntity living)
          || living.isDead()
          || e.getLocation().distanceSquared(center) > QUAKE_RADIUS * QUAKE_RADIUS) {
        continue;
      }
      living.damage(damage, caster);
      if (living.isDead()) {
        continue;
      }
      // About 3 to 5 blocks up: a fall that hurts a little, not one that kills
      living.setVelocity(
          new Vector(
              this.random.nextGaussian() / 4.0D,
              0.8D + this.random.nextDouble() * 0.3D,
              this.random.nextGaussian() / 4.0D));
    }
  }

  /**
   * ICE: an ice block flies where the player looks and encases what it hits in ice for a few
   * seconds
   */
  public void ice(final Player player, int power) {
    player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0F, 0.1F);

    final FallingBlock block =
        player
            .getWorld()
            .spawnFallingBlock(
                player.getLocation().add(0.0D, 1.8D, 0.0D), Material.ICE.createBlockData());
    block.setVelocity(player.getLocation().getDirection().multiply(2.0D));

    // It never turns into a block where it lands, nor into an item
    block.setDropItem(false);
    block.setCancelDrop(true);

    BukkitRunnable run =
        new BukkitRunnable() {
          private int ticks = 0;

          @Override
          public void run() {
            boolean hit = false;
            World world = block.getWorld();
            Location bLoc = block.getLocation();
            Location loc;

            for (int x = -1; x < 2 && !hit; x++) {
              for (int y = -1; y < 2 && !hit; y++) {
                for (int z = -1; z < 2 && !hit; z++) {
                  loc = bLoc.clone().add(x, y, z);
                  Block b = world.getBlockAt(loc);
                  if (b.getType().isSolid()
                      && overlaps(
                          loc.getBlockX(),
                          loc.getBlockY(),
                          loc.getBlockZ(),
                          1.0D,
                          1.0D,
                          1.0D,
                          bLoc.getX() - 0.5D,
                          bLoc.getY() - 0.5D,
                          bLoc.getZ() - 0.5D,
                          1.0D,
                          1.0D,
                          1.0D)) {
                    hit = true;
                  }
                }
              }
            }

            if (!hit) {
              for (Entity e : block.getNearbyEntities(1.0D, 1.0D, 1.0D)) {
                if (e != player) {
                  hit = true;
                  break;
                }
              }
            }

            // A spike that flies on and on (out of loaded chunks, into the void) stops after 10
            // seconds
            if (block.isDead() || hit || ++this.ticks > 200) {
              Block target = block.getLocation().getBlock();
              block.remove();
              cancel();

              if (this.ticks <= 200) {
                encaseInIce(target);
              }
            }
          }
        };
    run.runTaskTimer(this.plugin, 0L, 1L);
  }

  /**
   * Whether this block is ice an ice spike left, which is put back by itself and so can't be broken
   */
  public boolean isEncased(Block block) {
    return this.encased.containsKey(block);
  }

  /** Puts back at once every block ice spikes turned to ice (when the plugin stops) */
  public void restoreAll() {
    for (Map.Entry<Block, BlockData> entry : this.encased.entrySet()) {
      restore(entry.getKey(), entry.getValue());
    }
    this.encased.clear();
  }

  // Encase the target in ice for a few seconds, then put back what was there
  private void encaseInIce(Block landed) {
    final List<Block> changedBlocks = new ArrayList<Block>();

    for (int x = -1; x < 2; x++) {
      for (int y = -1; y < 3; y++) {
        for (int z = -1; z < 2; z++) {
          Block b = landed.getRelative(x, y, z);
          Material type = b.getType();

          // Only air and what a block may replace (water, grass, ...); never ice that was there
          // already
          if (!this.encased.containsKey(b)
              && (type.isAir() || (Tag.REPLACEABLE.isTagged(type) && type != Material.LAVA))) {
            this.encased.put(b, b.getBlockData());
            changedBlocks.add(b);
            b.setType(ENCASING, false);
          }
        }
      }
    }

    Collections.shuffle(changedBlocks);

    new BukkitRunnable() {
      @Override
      public void run() {
        for (int i = 0; i < 4; i++) {
          if (changedBlocks.isEmpty()) {
            cancel();
            return;
          }

          Block c = changedBlocks.remove(changedBlocks.size() - 1);
          BlockData original = encased.remove(c);
          if (original != null && c.getType() == ENCASING) {
            playBlockBreakEffect(c.getLocation(), c);
            restore(c, original);
          }
        }
      }
    }.runTaskTimer(this.plugin, 80 + this.random.nextInt(40), 3L);
  }

  // Puts back what a block was, if it is still the ice the spike left (a piston may have moved it)
  private static void restore(Block block, BlockData original) {
    if (block.getType() == ENCASING) {
      block.setBlockData(original, false);
    }
  }

  // Whether two boxes (corner and size) touch
  private static boolean overlaps(
      double x1,
      double y1,
      double z1,
      double w1,
      double h1,
      double d1,
      double x2,
      double y2,
      double z2,
      double w2,
      double h2,
      double d2) {
    return !(x1 + w1 < x2
        || x2 + w2 < x1
        || y1 + h1 < y2
        || y2 + h2 < y1
        || z1 + d1 < z2
        || z2 + d2 < z1);
  }

  // Block break particles and sound, as the removed Effect.STEP_SOUND did
  private static void playBlockBreakEffect(Location location, Block block) {
    if (block.getType().isAir()) {
      return;
    }
    BlockData data = block.getBlockData();
    location
        .getWorld()
        .spawnParticle(
            Particle.BLOCK, location.clone().add(0.5D, 0.5D, 0.5D), 30, 0.25D, 0.25D, 0.25D, data);
    location.getWorld().playSound(location, data.getSoundGroup().getBreakSound(), 1.0F, 1.0F);
  }
}
