package com.dogonfire.gods.service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.model.Altar;
import com.dogonfire.gods.model.AltarKind;
import com.dogonfire.gods.model.BlockPos;
import com.dogonfire.gods.storage.AltarRepository;

/**
 * Where the altars are (altars.yml, {@link AltarRepository}): every tagged altar sign Gods made, used or found, with its
 * altar block, kind and god, and each god's main altar (/g setaltar; stored on the god in gods.yml, see
 * {@link #mainAltar}).
 * <ul>
 * <li>Added when an altar is built (after its sign is tagged), when one is used (an old altar is tagged on first use),
 * and when a scan finds one.</li>
 * <li>Scans: every chunk that loads, and every chunk already loaded on enable and /g reload, is queued; at most
 * {@link AltarScan#CHUNKS_PER_TICK} are scanned per tick, each only through its tile entities (the signs tagged
 * {@code gods:altar}), never block by block. A scan adds what it finds and removes registered altars whose sign is
 * gone or no longer tagged ({@link AltarScan}), so edits Gods didn't see (WorldEdit, a lost altars.yml) heal as chunks
 * load.</li>
 * <li>Removed when a block event (break, burn, explosion, piston, mob, place) touches an altar's sign or altar block
 * and, a tick later, the sign is gone or untagged (a cancelled or harmless event changes nothing). Breaking the altar
 * block drops the sign, so that removes the altar too.</li>
 * <li>A removed god's altars stay registered: they still stand, and praying at one founds the god anew. They show no
 * floating text while the god doesn't exist.</li>
 * </ul>
 * Only worlds Gods is active in are scanned. Main thread only.
 */
public class AltarRegistry
{
	private final GodsContext							ctx;
	private final AltarRepository						repository;
	// Chunks waiting for a scan, in the order they loaded
	private final Deque<BlockPos.ChunkPos>				queue			= new ArrayDeque<>();
	private final Set<BlockPos.ChunkPos>				queued			= new HashSet<>();
	// Signs a block event touched, checked a tick later
	private final Set<BlockPos>							toValidate		= new LinkedHashSet<>();

	public AltarRegistry(GodsContext ctx, AltarRepository repository)
	{
		this.ctx = ctx;
		this.repository = repository;
	}

	/** Reads altars.yml (or keeps what is in memory on /g reload), then queues every loaded chunk for a scan */
	public void load()
	{
		this.repository.load();
		queueLoadedChunks();
	}

	// ---------------------------------------------------------------------
	// Lookups

	/** The altar with this sign, or null */
	public Altar atSign(BlockPos sign)
	{
		return this.repository.atSign(sign);
	}

	/** The altars whose signs hang on this block */
	public List<Altar> onBlock(BlockPos block)
	{
		return this.repository.onBlock(block);
	}

	/** The altars with their sign or altar block in this chunk */
	public List<Altar> inChunk(BlockPos.ChunkPos chunk)
	{
		return this.repository.inChunk(chunk);
	}

	/** The god's altars */
	public List<Altar> forGod(String godName)
	{
		return this.repository.forGod(godName);
	}

	public Collection<Altar> all()
	{
		return this.repository.all();
	}

	public static BlockPos pos(Block block)
	{
		return new BlockPos(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	public static BlockPos.ChunkPos pos(Chunk chunk)
	{
		return new BlockPos.ChunkPos(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
	}

	/** The block at the position, or null if its world isn't loaded */
	public Block block(BlockPos pos)
	{
		World world = ctx.plugin().getServer().getWorld(pos.world());
		return world != null ? world.getBlockAt(pos.x(), pos.y(), pos.z()) : null;
	}

	/** Whether the chunk is loaded (false for a world that isn't) */
	public boolean isLoaded(BlockPos.ChunkPos chunk)
	{
		World world = ctx.plugin().getServer().getWorld(chunk.world());
		return world != null && world.isChunkLoaded(chunk.x(), chunk.z());
	}

	// ---------------------------------------------------------------------
	// Adding and removing

	/**
	 * Registers the tagged altar sign (built, used or found), or updates its god and kind; does nothing if the registry
	 * already has it as it is
	 *
	 * @param creator who built it, or null if not known (kept from before when it is known)
	 */
	public void register(Block sign, AltarSigns.AltarSign altar, UUID creator)
	{
		Block behind = AltarService.blockBehindSign(sign);
		if (behind == null || altar == null)
		{
			return;
		}
		AltarScan.Found found = new AltarScan.Found(pos(sign), pos(behind), AltarKind.byKey(altar.kind().key()), altar.god());
		Altar existing = this.repository.atSign(found.sign());
		if (existing != null && AltarScan.matches(existing, found))
		{
			return;
		}
		// Built while Gods watched: known creator and time; used or found: neither (kept when it had them)
		UUID by = creator != null ? creator : existing != null ? existing.creator() : null;
		Instant created = existing != null ? existing.created() : creator != null ? Instant.now() : null;
		apply(found, existing, by, created);
		this.repository.markDirty();
	}

	/** Adds or updates one altar from what its sign says, keeping the main altar in line */
	private void apply(AltarScan.Found found, Altar existing, UUID creator, Instant created)
	{
		if (existing != null && existing.block().equals(found.block()))
		{
			String oldGod = existing.god();
			existing.setKind(found.kind());
			existing.setGod(found.god());
			if (!Objects.equals(oldGod, found.god()) || found.kind() != AltarKind.PRAYING)
			{
				dropMainAltar(oldGod, existing, "its sign changed");
			}
			ctx.altarTexts().show(existing);
			return;
		}
		if (existing != null)
		{
			remove(existing.sign(), "its sign moved to another block");
		}
		Altar altar = new Altar(found.sign(), found.block(), found.kind(), found.god(), created, creator);
		this.repository.add(altar);
		ctx.plugin().logDebug("Registered the " + altar);
		ctx.altarTexts().show(altar);
	}

	/** Unregisters the altar with this sign (it is gone); clears its god's main altar if it was that */
	private void remove(BlockPos sign, String why)
	{
		Altar altar = this.repository.remove(sign);
		if (altar == null)
		{
			return;
		}
		ctx.plugin().logDebug("Unregistered the " + altar + ": " + why);
		ctx.altarTexts().hide(sign);
		dropMainAltar(altar.god(), altar, why);
		this.repository.markDirty();
	}

	private void dropMainAltar(String godName, Altar altar, String why)
	{
		if (godName != null && altar.sign().equals(ctx.godService().getMainAltar(godName)))
		{
			ctx.godService().setMainAltar(godName, null);
			ctx.plugin().log("The main altar of " + godName + " at " + describe(altar.sign()) + " is gone (" + why + "); " + godName + " has no main altar now.");
		}
	}

	// ---------------------------------------------------------------------
	// Block events

	/**
	 * Blocks that may have been broken, burned, blown up, moved or replaced: altars whose sign or altar block is among
	 * them are checked a tick later, when the world shows the outcome (an event another plugin cancelled changes
	 * nothing)
	 */
	public void blocksChanging(Collection<Block> blocks)
	{
		if (this.repository.isEmpty())
		{
			return;
		}
		boolean wasEmpty = this.toValidate.isEmpty();
		for (Block block : blocks)
		{
			BlockPos pos = pos(block);
			Altar altar = this.repository.atSign(pos);
			if (altar != null)
			{
				this.toValidate.add(altar.sign());
			}
			for (Altar on : this.repository.onBlock(pos))
			{
				this.toValidate.add(on.sign());
			}
		}
		if (wasEmpty && !this.toValidate.isEmpty())
		{
			ctx.plugin().getServer().getScheduler().runTask(ctx.plugin(), this::validateTouched);
		}
	}

	public void blockChanging(Block block)
	{
		blocksChanging(List.of(block));
	}

	private void validateTouched()
	{
		List<BlockPos> signs = new ArrayList<>(this.toValidate);
		this.toValidate.clear();
		for (BlockPos sign : signs)
		{
			validate(sign);
		}
	}

	/** Removes the altar if its sign is gone or no longer tagged; updates it if the sign says otherwise now */
	private void validate(BlockPos signPos)
	{
		Altar altar = this.repository.atSign(signPos);
		Block block = block(signPos);
		if (altar == null || block == null || !isLoaded(signPos.chunk()))
		{
			return;
		}
		AltarSigns.AltarSign tagged = block.getState() instanceof Sign sign && AltarService.isWallSign(block) ? ctx.altarService().readTaggedAltar(sign) : null;
		if (tagged == null)
		{
			remove(signPos, "its sign was broken or moved");
			return;
		}
		register(block, tagged, null);
	}

	// ---------------------------------------------------------------------
	// Chunk scans

	/** Queues every chunk loaded now in the worlds Gods is active in (enable, /g reload) */
	public void queueLoadedChunks()
	{
		for (World world : ctx.plugin().getServer().getWorlds())
		{
			if (!ctx.plugin().isEnabledInWorld(world))
			{
				continue;
			}
			for (Chunk chunk : world.getLoadedChunks())
			{
				queue(chunk);
			}
		}
	}

	/** Queues a chunk that loaded for a scan (a few per tick, see {@link #processQueue()}) */
	public void queue(Chunk chunk)
	{
		BlockPos.ChunkPos pos = pos(chunk);
		if (this.queued.add(pos))
		{
			this.queue.add(pos);
		}
	}

	/** A chunk unloads: its floating texts go, and a pending scan of it is dropped */
	public void chunkUnloading(Chunk chunk)
	{
		BlockPos.ChunkPos pos = pos(chunk);
		if (this.queued.remove(pos))
		{
			this.queue.remove(pos);
		}
		for (Altar altar : this.repository.inChunk(pos))
		{
			ctx.altarTexts().hide(altar.sign());
		}
		ctx.godNpcs().chunkUnloading(pos);
	}

	/** Every tick: scans up to {@link AltarScan#CHUNKS_PER_TICK} queued chunks that are still loaded */
	public void processQueue()
	{
		for (int scanned = 0; scanned < AltarScan.CHUNKS_PER_TICK && !this.queue.isEmpty();)
		{
			BlockPos.ChunkPos pos = this.queue.poll();
			this.queued.remove(pos);
			World world = ctx.plugin().getServer().getWorld(pos.world());
			if (world == null || !world.isChunkLoaded(pos.x(), pos.z()) || !ctx.plugin().isEnabledInWorld(world))
			{
				continue;
			}
			scan(world.getChunkAt(pos.x(), pos.z()));
			scanned++;
		}
	}

	/** Brings the registry in line with the tagged altar signs in the chunk ({@link AltarScan}) */
	private void scan(Chunk chunk)
	{
		List<AltarScan.Found> found = new ArrayList<>();
		for (BlockState state : chunk.getTileEntities())
		{
			if (!(state instanceof Sign sign) || !AltarService.isWallSign(state.getBlock()))
			{
				continue;
			}
			AltarSigns.AltarSign altar = ctx.altarService().readTaggedAltar(sign);
			Block behind = altar != null ? AltarService.blockBehindSign(state.getBlock()) : null;
			if (behind != null)
			{
				found.add(new AltarScan.Found(pos(state.getBlock()), pos(behind), AltarKind.byKey(altar.kind().key()), altar.god()));
			}
		}

		BlockPos.ChunkPos pos = pos(chunk);
		AltarScan.Changes changes = AltarScan.reconcile(this.repository.signsInChunk(pos), found);
		for (BlockPos removed : changes.removed())
		{
			remove(removed, "its sign is gone or no longer tagged");
		}
		for (AltarScan.Found added : changes.added())
		{
			apply(added, null, null, null);
		}
		for (AltarScan.Found changed : changes.changed())
		{
			Altar existing = this.repository.atSign(changed.sign());
			apply(changed, existing, existing.creator(), existing.created());
		}
		if (!changes.isEmpty())
		{
			this.repository.markDirty();
		}
		// Altars whose sign is in a neighbouring chunk but whose altar block (and so its text) is here
		for (Altar altar : this.repository.inChunk(pos))
		{
			ctx.altarTexts().show(altar);
		}
		// Gods whose main altar is here appear in front of it
		ctx.godNpcs().chunkScanned(pos);
	}

	// ---------------------------------------------------------------------
	// Main altars

	/**
	 * The god's main altar (/g setaltar), or null if it has none. gods.yml keeps it (its sign) on the god; the
	 * registry's altar is returned when it has it, else one made from the stored sign (an altar not found again yet,
	 * e.g. after altars.yml was lost; its altar block is then not known and given as the sign).
	 * <p>
	 * This is where a god that descended appears as an NPC ({@link GodNpcService}): {@link MainAltarRules#standingSpot} is the spot in
	 * front of its sign, {@link #mainAltarLocation} the altar block.
	 */
	public Altar mainAltar(String godName)
	{
		BlockPos sign = ctx.godService().getMainAltar(godName);
		if (sign == null)
		{
			return null;
		}
		Altar altar = this.repository.atSign(sign);
		return altar != null ? altar : new Altar(sign, sign, AltarKind.PRAYING, godName, null, null);
	}

	/** Whether this altar is its god's main altar */
	public boolean isMainAltar(Altar altar)
	{
		return altar.god() != null && altar.sign().equals(ctx.godService().getMainAltar(altar.god()));
	}

	/**
	 * The altar block of the god's main altar as a location (its world loaded), or null if it has none or its world
	 * isn't loaded
	 */
	public Location mainAltarLocation(String godName)
	{
		Altar altar = mainAltar(godName);
		World world = altar != null ? ctx.plugin().getServer().getWorld(altar.world()) : null;
		return world != null ? new Location(world, altar.block().x(), altar.block().y(), altar.block().z()) : null;
	}

	/** Makes the altar its god's main altar; the floating texts of the old and new main altar follow */
	public void setMainAltar(Altar altar)
	{
		Altar old = mainAltar(altar.god());
		ctx.godService().setMainAltar(altar.god(), altar.sign());
		ctx.plugin().log("The main altar of " + altar.god() + " is now at " + describe(altar.sign()) + ".");
		if (old != null)
		{
			ctx.altarTexts().show(old);
		}
		ctx.altarTexts().show(altar);
	}

	/** "world 10, 64, -3" for the log */
	public static String describe(BlockPos pos)
	{
		return pos.world() + " " + pos.x() + ", " + pos.y() + ", " + pos.z();
	}
}
