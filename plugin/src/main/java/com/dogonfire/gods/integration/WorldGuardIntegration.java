package com.dogonfire.gods.integration;

import org.bukkit.block.Block;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;

/**
 * WorldGuard regions. Only created when WorldGuard is installed (see {@link com.dogonfire.gods.GodsContext}), since
 * this class references WorldGuard's classes.
 */
public final class WorldGuardIntegration
{
	/** Whether the block is inside any WorldGuard region */
	public boolean isInRegion(Block block)
	{
		RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(block.getWorld()));
		if (regionManager == null)
		{
			return false;
		}

		return regionManager.getApplicableRegions(BlockVector3.at(block.getX(), block.getY(), block.getZ())).size() > 0;
	}
}
