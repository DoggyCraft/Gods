package com.dogonfire.gods.service;

import com.dogonfire.gods.GodsContext;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;

/**
 * Where pilgrimage chests go: finding flat, open, unprotected ground in a fitting biome, placing
 * the chest with the rewards on a glowstone marker, and taking it away again. Searches run on the
 * main thread, so they try at most {@link #MAX_LOCATION_ATTEMPTS} spots and load at most {@link
 * #MAX_CHUNK_LOADS} chunks that weren't loaded already: never loop until a spot is found.
 */
final class PilgrimageSites {
  static final int MAX_LOCATION_ATTEMPTS = 32;
  static final int MAX_CHUNK_LOADS = 6;

  private static final Set<Material> GROUND =
      EnumSet.of(
          Material.GRASS_BLOCK,
          Material.PODZOL,
          Material.MYCELIUM,
          Material.SNOW_BLOCK,
          Material.SAND,
          Material.RED_SAND,
          Material.MOSS_BLOCK);

  private static final Set<Biome> BIOMES = new HashSet<Biome>();

  static {
    // Biomes are registry entries, so skip any this server doesn't know
    for (Biome biome :
        new Biome[] {
          Biome.FLOWER_FOREST,
          Biome.ICE_SPIKES,
          Biome.DESERT,
          Biome.MUSHROOM_FIELDS,
          Biome.JUNGLE,
          Biome.TAIGA,
          Biome.OLD_GROWTH_SPRUCE_TAIGA,
          Biome.OLD_GROWTH_PINE_TAIGA,
          Biome.PLAINS,
          Biome.SUNFLOWER_PLAINS,
          Biome.MEADOW,
          Biome.CHERRY_GROVE,
          Biome.FOREST,
          Biome.BIRCH_FOREST,
          Biome.SAVANNA,
          Biome.SNOWY_PLAINS,
          Biome.SNOWY_TAIGA,
          Biome.GROVE,
          Biome.SNOWY_SLOPES
        }) {
      if (biome != null) {
        BIOMES.add(biome);
      }
    }
  }

  private final GodsContext ctx;
  private final Random random;

  PilgrimageSites(GodsContext ctx, Random random) {
    this.ctx = ctx;
    this.random = random;
  }

  /**
   * Looks for a spot in an overworld Gods is enabled in, between minDist and maxDist blocks from
   * center. Returns the block where the chest goes, or null.
   */
  Block find(World world, int minDist, int maxDist, Location center) {
    if (world == null
        || world.getEnvironment() != World.Environment.NORMAL
        || !ctx.plugin().isEnabledInWorld(world)) {
      return null;
    }

    int chunkLoads = 0;

    for (int run = 1; run <= MAX_LOCATION_ATTEMPTS; run++) {
      int[] spot =
          QuestRules.pilgrimageSpot(
              this.random, center.getBlockX(), center.getBlockZ(), minDist, maxDist);
      int x = spot[0];
      int z = spot[1];

      if (!world.isChunkLoaded(x >> 4, z >> 4)) {
        if (chunkLoads >= MAX_CHUNK_LOADS) {
          continue;
        }
        chunkLoads++;
      }

      Block ground = world.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);

      // Step down through snow layers and plants that count as the top block
      for (int i = 0; i < 2 && isOpenSpace(ground) && ground.getY() > world.getMinHeight(); i++) {
        ground = ground.getRelative(BlockFace.DOWN);
      }

      if (!QuestRules.isFittingHeight(ground.getY(), world.getSeaLevel(), world.getMaxHeight())) {
        continue;
      }

      if (!BIOMES.contains(ground.getBiome()) || !isFlatGround(ground)) {
        continue;
      }

      Block chestBlock = ground.getRelative(BlockFace.UP);

      if (ctx.worldGuard() != null && ctx.worldGuard().isInRegion(chestBlock)) {
        continue;
      }

      ctx.plugin()
          .logDebug(
              "Pilgrimage Holy mountain found in "
                  + run
                  + " runs ("
                  + chunkLoads
                  + " chunk loads): "
                  + chestBlock.getX()
                  + ","
                  + chestBlock.getY()
                  + ","
                  + chestBlock.getZ());

      return chestBlock;
    }

    ctx.plugin().log("Pilgrimage Holy mountain generation FAILED in " + world.getName());

    return null;
  }

  // Ground in the list with open space above it, and the same for its four neighbours
  private static boolean isFlatGround(Block ground) {
    if (!GROUND.contains(ground.getType()) || !isOpenSpace(ground.getRelative(BlockFace.UP))) {
      return false;
    }

    for (BlockFace face :
        new BlockFace[] {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
      Block neighbour = ground.getRelative(face);
      if (!GROUND.contains(neighbour.getType())
          || !isOpenSpace(neighbour.getRelative(BlockFace.UP))) {
        return false;
      }
    }

    return true;
  }

  // Air, or something like grass or a snow layer that a chest may replace (but not water or lava)
  private static boolean isOpenSpace(Block block) {
    Material type = block.getType();

    return type.isAir()
        || (Tag.REPLACEABLE.isTagged(type)
            && !block.isLiquid()
            && type != Material.WATER
            && type != Material.LAVA);
  }

  /**
   * Places the chest with the rewards on a glowstone marker. Undoes it and returns false if the
   * chest can't be filled.
   */
  boolean placeChest(Block chestBlock, List<ItemStack> rewards) {
    Block underBlock = chestBlock.getRelative(BlockFace.DOWN);
    Material groundType = underBlock.getType();

    chestBlock.setType(Material.CHEST);
    underBlock.setType(Material.GLOWSTONE);

    if (!(chestBlock.getState() instanceof Chest chest)) {
      chestBlock.setType(Material.AIR);
      underBlock.setType(groundType);
      return false;
    }

    chest.getBlockInventory().addItem(rewards.toArray(new ItemStack[0]));

    return true;
  }

  /**
   * Empties and removes the chest, and puts the ground (by Material name; grass if unknown) back
   * under it
   */
  void removeChest(Location location, String groundName) {
    Block chestBlock = location.getBlock();

    if (chestBlock.getState() instanceof Chest chest) {
      chest.getBlockInventory().clear();
      chestBlock.setType(Material.AIR);
    }

    Block underBlock = chestBlock.getRelative(BlockFace.DOWN);
    if (underBlock.getType() == Material.GLOWSTONE) {
      Material ground = (groundName == null) ? null : Material.matchMaterial(groundName);
      underBlock.setType((ground != null && ground.isBlock()) ? ground : Material.GRASS_BLOCK);
    }
  }
}
