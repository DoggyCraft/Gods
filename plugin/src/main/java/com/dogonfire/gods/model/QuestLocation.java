package com.dogonfire.gods.model;

/**
 * A block a quest is about (a pilgrimage chest), by world name. In quests.yml it is {@code Location.World/X/Y/Z}; a
 * god's quest also keeps {@code Location.Ground}, the block the glowstone marker under the chest replaced (null if not
 * known), so it can be put back.
 */
public record QuestLocation(String world, int x, int y, int z, String ground)
{
	public QuestLocation withoutGround()
	{
		return new QuestLocation(this.world, this.x, this.y, this.z, null);
	}

	/** Same world and block */
	public boolean isAt(String worldName, int blockX, int blockY, int blockZ)
	{
		return this.x == blockX && this.y == blockY && this.z == blockZ && this.world.equals(worldName);
	}
}
