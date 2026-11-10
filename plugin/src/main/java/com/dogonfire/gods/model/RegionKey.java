package com.dogonfire.gods.model;

/**
 * A 128x128 block region of a world, the unit of holy and neutral land. In holyland.yml it is
 * {@code <world key>.<hash>}: the world name with '.' (the YAML path separator) replaced by '_', and the region
 * coordinates packed into one number, region X in the high 32 bits and region Z in the low 32 bits.
 */
public record RegionKey(String world, long hash)
{
	/** Holy land regions are 2^7 = 128 blocks wide */
	public static final int REGION_SHIFT = 7;

	/** The region of a block */
	public static RegionKey of(String worldName, int blockX, int blockZ)
	{
		return new RegionKey(worldKey(worldName), hash(blockX, blockZ));
	}

	/** World names may contain '.', the YAML path separator */
	public static String worldKey(String worldName)
	{
		return worldName.replace('.', '_');
	}

	public static long hash(int blockX, int blockZ)
	{
		long regionX = blockX >> REGION_SHIFT;
		long regionZ = blockZ >> REGION_SHIFT;

		return (regionX << 32) | (regionZ & 0xFFFFFFFFL);
	}

	/** Whether two blocks (of the same world) are in the same region */
	public static boolean sameRegion(int blockX1, int blockZ1, int blockX2, int blockZ2)
	{
		return (blockX1 >> REGION_SHIFT) == (blockX2 >> REGION_SHIFT) && (blockZ1 >> REGION_SHIFT) == (blockZ2 >> REGION_SHIFT);
	}

	/** The region X coordinate (block X / 128, rounded down) */
	public int regionX()
	{
		return (int) (this.hash >> 32);
	}

	/** The region Z coordinate (block Z / 128, rounded down) */
	public int regionZ()
	{
		return (int) this.hash;
	}
}
