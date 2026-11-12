package com.dogonfire.gods.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.model.BlockPos;
import com.dogonfire.gods.service.AltarRegistry;

/**
 * Keeps the altar registry ({@link AltarRegistry}) in line with the world: chunks that load are queued for a scan of
 * their tagged altar signs, chunks that unload take their floating texts with them, and block events that touch an
 * altar's sign or altar block have it checked a tick later. Altars don't burn ({@link #onAltarBurn}); every other
 * handler only looks at what happened (MONITOR, ignoring cancelled events), and the lookups are hash map hits. Always
 * registered.
 */
public class AltarWorldListener implements Listener
{
	private final GodsContext ctx;

	public AltarWorldListener(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onChunkLoad(ChunkLoadEvent event)
	{
		// A chunk generated just now has no altars
		if (!event.isNewChunk() && ctx.plugin().isEnabledInWorld(event.getWorld()))
		{
			ctx.altarRegistry().queue(event.getChunk());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onChunkUnload(ChunkUnloadEvent event)
	{
		ctx.altarRegistry().chunkUnloading(event.getChunk());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event)
	{
		ctx.altarRegistry().blockChanging(event.getBlock());
	}

	/** Altars don't burn: a fire lighting an altar would otherwise spread to a wooden sign or altar block */
	@EventHandler(ignoreCancelled = true)
	public void onAltarBurn(BlockBurnEvent event)
	{
		BlockPos pos = AltarRegistry.pos(event.getBlock());
		if (ctx.altarRegistry().atSign(pos) != null || !ctx.altarRegistry().onBlock(pos).isEmpty())
		{
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event)
	{
		ctx.altarRegistry().blockChanging(event.getBlock());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event)
	{
		// A block placed where a sign or altar block was (replaced by a plugin, or a sign placed over a broken one)
		ctx.altarRegistry().blockChanging(event.getBlockPlaced());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event)
	{
		ctx.altarRegistry().blocksChanging(event.blockList());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockExplode(BlockExplodeEvent event)
	{
		ctx.altarRegistry().blocksChanging(event.blockList());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPistonExtend(BlockPistonExtendEvent event)
	{
		ctx.altarRegistry().blocksChanging(event.getBlocks());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPistonRetract(BlockPistonRetractEvent event)
	{
		ctx.altarRegistry().blocksChanging(event.getBlocks());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityChangeBlock(EntityChangeBlockEvent event)
	{
		// Endermen, withers, ravagers, falling blocks
		ctx.altarRegistry().blockChanging(event.getBlock());
	}
}
