package com.dogonfire.gods.model;

/**
 * A block in a world, by the world's name. In data files it is written as {@code "x,y,z"} next to the world
 * ({@link #coordinates()}, {@link #parse}).
 */
public record BlockPos(String world, int x, int y, int z)
{
	/** The chunk the block is in */
	public ChunkPos chunk()
	{
		return new ChunkPos(this.world, this.x >> 4, this.z >> 4);
	}

	/** The block {@code dy} higher (lower when negative) */
	public BlockPos up(int dy)
	{
		return new BlockPos(this.world, this.x, this.y + dy, this.z);
	}

	/** {@code "x,y,z"} */
	public String coordinates()
	{
		return this.x + "," + this.y + "," + this.z;
	}

	/** The block at {@code "x,y,z"} (spaces allowed) in the world, or null if the text isn't three whole numbers */
	public static BlockPos parse(String world, String coordinates)
	{
		if (world == null || coordinates == null)
		{
			return null;
		}
		String[] parts = coordinates.split(",");
		if (parts.length != 3)
		{
			return null;
		}
		try
		{
			return new BlockPos(world, Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim()));
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	/** A chunk of a world: block coordinates divided by 16, rounded down */
	public record ChunkPos(String world, int x, int z)
	{
	}
}
