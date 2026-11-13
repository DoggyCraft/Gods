package com.dogonfire.gods.model;

/**
 * A 16x16 block chunk of a world, the unit of holy and neutral land. {@code world} is the world
 * key: the world name with '.' (the YAML path separator) replaced by '_'. In holyland.yml a chunk
 * is written as {@code "x,z"} ({@link #coordinates()}, {@link #parse}).
 */
public record ChunkKey(String world, int x, int z) {
  /** Chunks are 2^4 = 16 blocks wide */
  public static final int CHUNK_SHIFT = 4;

  /** The chunk of a block */
  public static ChunkKey of(String worldName, int blockX, int blockZ) {
    return new ChunkKey(worldKey(worldName), blockX >> CHUNK_SHIFT, blockZ >> CHUNK_SHIFT);
  }

  /** World names may contain '.', the YAML path separator */
  public static String worldKey(String worldName) {
    return worldName.replace('.', '_');
  }

  /** Whether two blocks (of the same world) are in the same chunk */
  public static boolean sameChunk(int blockX1, int blockZ1, int blockX2, int blockZ2) {
    return (blockX1 >> CHUNK_SHIFT) == (blockX2 >> CHUNK_SHIFT)
        && (blockZ1 >> CHUNK_SHIFT) == (blockZ2 >> CHUNK_SHIFT);
  }

  /** The chunk {@code dx}, {@code dz} chunks away */
  public ChunkKey offset(int dx, int dz) {
    return new ChunkKey(this.world, this.x + dx, this.z + dz);
  }

  /** {@code "x,z"} */
  public String coordinates() {
    return this.x + "," + this.z;
  }

  /**
   * The chunk at {@code "x,z"} (spaces allowed) in the world (key), or null if the text isn't two
   * whole numbers
   */
  public static ChunkKey parse(String world, String coordinates) {
    if (world == null || coordinates == null) {
      return null;
    }
    String[] parts = coordinates.split(",");
    if (parts.length != 2) {
      return null;
    }
    try {
      return new ChunkKey(
          world, Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
