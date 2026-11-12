package com.dogonfire.gods.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.api.event.HolyLandClaimEvent;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.model.Altar;
import com.dogonfire.gods.model.AltarKind;
import com.dogonfire.gods.model.BlockPos;
import com.dogonfire.gods.model.ChunkKey;
import com.dogonfire.gods.model.HolyLand;
import com.dogonfire.gods.model.NeutralLand;
import com.dogonfire.gods.storage.HolyLandRepository;
import com.dogonfire.gods.util.LegacyDates;

/**
 * Holy lands: one land per altar, a set of chunks ({@link ChunkKey}) a god claims when a believer builds or prays at
 * the altar and that grows with the god's rank ({@link #claim}), claim lands (a priest's GriefPrevention claim made holy
 * land with /g holyclaim, {@link #holyClaim}; it wins over chunks it overlaps), neutral land chunks set by admins, and
 * attacks on holy lands (the rules are in {@link HolyLandContest}). Owns every change to holyland.yml ({@link HolyLandRepository});
 * every change is written at once, as before. The events are handled by {@code listener/HolyLandListener}. Its data is
 * only loaded while holy land is enabled. Main thread only.
 */
public class HolyLandService
{
	/** {@link #getGodAtHolyLandLocationFrom} for a player last seen in neutral land */
	public static final String			NEUTRAL_LAND_MARKER	= "NeutralLand";

	private static final LegacyDates	DATES				= LegacyDates.MINUTES;

	private final GodsContext			ctx;
	private final HolyLandRepository	repository;

	// The god of the land each player was last seen in (for the titles shown when entering land)
	private final Map<UUID, String>		fromLocations		= new HashMap<UUID, String>();
	private final Random				random				= new Random();

	public HolyLandService(GodsContext ctx, HolyLandRepository repository)
	{
		this.ctx = ctx;
		this.repository = repository;
	}

	public void load()
	{
		this.repository.load();
	}

	/**
	 * Whether holy and neutral land protect themselves now (holy-land.protection; with {@code auto}, not while
	 * GriefPrevention or WorldGuard is on the server). Read at use time. Claims, growth, titles, aura and attacks don't
	 * depend on it.
	 */
	public boolean protects()
	{
		return HolyLandRules.protects(ctx.settings().holyLand().protection(), claimPlugin() != null);
	}

	/** The claim plugin on the server that holy-land.protection auto leaves protection to, or null */
	public String claimPlugin()
	{
		return ctx.griefPrevention() != null ? "GriefPrevention" : ctx.worldGuard() != null ? "WorldGuard" : null;
	}

	// ---------------------------------------------------------------------
	// Chunks

	/** The chunk of a location */
	public static ChunkKey chunkOf(Location location)
	{
		return ChunkKey.of(location.getWorld().getName(), location.getBlockX(), location.getBlockZ());
	}

	/** Whether two locations are in the same chunk of the same world */
	public boolean isSameChunk(Location one, Location two)
	{
		return ChunkKey.sameChunk(one.getBlockX(), one.getBlockZ(), two.getBlockX(), two.getBlockZ()) && one.getWorld() == two.getWorld();
	}

	/**
	 * The holy land at a location: the claim land of the GriefPrevention claim there (it wins over a land of chunks
	 * overlapping it), else the land of its chunk
	 */
	private HolyLand find(Location location)
	{
		HolyLand claimLand = claimLandAt(location);
		return claimLand != null ? claimLand : this.repository.find(chunkOf(location));
	}

	/**
	 * Whether claim lands count in lookups now: only while GriefPrevention is on the server and there is a claim land.
	 * Without GriefPrevention claim lands stay in holyland.yml untouched but hold no land.
	 */
	public boolean watchesClaims()
	{
		return this.repository.hasClaimLands() && ctx.griefPrevention() != null;
	}

	/**
	 * Whether two locations of a player's move may be in different claim lands though they are in the same chunk: while
	 * claim lands count, when the claim land at both differs (only asked when the block x or z changed)
	 */
	public boolean crossesClaimLandBorder(Location from, Location to)
	{
		if (!watchesClaims() || (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()))
		{
			return false;
		}
		return claimLandAt(from) != claimLandAt(to);
	}

	/** The claim land of the top-level GriefPrevention claim at a location (any height), or null; no lookup without claim lands */
	private HolyLand claimLandAt(Location location)
	{
		if (!watchesClaims() || location.getWorld() == null)
		{
			return null;
		}
		Long claimId = ctx.griefPrevention().topLevelClaimIdAt(location);
		return claimId != null ? this.repository.findClaim(claimId) : null;
	}

	/** Whether the GriefPrevention claim of this id is a claim land */
	public boolean isClaimLand(long claimId)
	{
		return this.repository.findClaim(claimId) != null;
	}

	/** Whether a chunk belongs to no holy land and isn't neutral land */
	private boolean isFree(ChunkKey chunk)
	{
		return this.repository.find(chunk) == null && this.repository.findNeutral(chunk) == null;
	}

	// ---------------------------------------------------------------------
	// Holy lands

	public String getGodAtHolyLandLocation(Location location)
	{
		HolyLand land = find(location);
		return land != null ? land.godName() : null;
	}

	public String getGodAtHolyLandLocationFrom(UUID believerId)
	{
		return this.fromLocations.get(believerId);
	}

	/** The god of the land at {@code location}, remembered as where the player is now */
	public String getGodAtHolyLandLocationTo(UUID believerId, Location location)
	{
		String godName = getGodAtHolyLandLocation(location);

		this.fromLocations.put(believerId, godName);

		return godName;
	}

	public void setNeutralLandLocationFrom(UUID believerId)
	{
		this.fromLocations.put(believerId, NEUTRAL_LAND_MARKER);
	}

	public void handleQuit(UUID playerId)
	{
		this.fromLocations.remove(playerId);
	}

	public String getHolyLandName(Location location)
	{
		HolyLand land = find(location);
		return land != null ? land.name() : null;
	}

	private String generateHolyLandName(Biome type)
	{
		String[] first = new String[] {"Upper ", "Lower ", "High ", ""};
		String[] second = new String[] {"Flower", "Grass", "Cold"};
		String[] third = new String[] {"place", "field", "shire"};

		// Biome is no longer an enum, so this can't be a switch
		if (type == Biome.SUNFLOWER_PLAINS || type == Biome.PLAINS)
		{
			first = new String[] {"Upper ", "Lower ", "High ", ""};
			second = new String[] {"Flower", "Grass", "Cold"};
			third = new String[] {"place", "field", "shire"};
		}
		else if (type == Biome.DARK_FOREST || type == Biome.BIRCH_FOREST)
		{
			first = new String[] {"Upper ", "Lower ", "High ", ""};
			second = new String[] {"Fire", "Dank", "Cold"};
			third = new String[] {"forest", "wood", ""};
		}
		else if (type == Biome.WINDSWEPT_HILLS)
		{
			first = new String[] {"Upper ", "Lower ", "High ", ""};
			second = new String[] {"Rock", "High", "Sky"};
			third = new String[] {"hill", "mountain"};
		}

		return first[random.nextInt(first.length)] + second[random.nextInt(second.length)] + third[random.nextInt(third.length)];
	}

	/**
	 * Whether the world may spawn this mob in the holy land of a god that doesn't allow mob spawning: the peaceful mobs
	 * of its god type's theme (themes.yml peaceful-mobs)
	 */
	public boolean isMobTypeAllowedToSpawn(String godName, EntityType mobType)
	{
		return ctx.commandmentService().themeForGod(godName).peacefulMobs().contains(mobType);
	}

	/** Gives the land to {@code godName} (keeping when it was first prayed in) and counts it as prayed in now */
	private void giveLand(HolyLand land, String godName)
	{
		Instant now = DATES.now();
		if (land.firstPrayerTime() == null)
		{
			land.setFirstPrayerTime(now);
		}
		land.setGodName(godName);
		land.setLastPrayerTime(now);
	}

	/** Removes holy lands nobody has prayed in for the configured number of days */
	public void removeAbandonedLands()
	{
		long timeBefore = System.currentTimeMillis();

		long now = System.currentTimeMillis();
		long maxMinutes = ctx.settings().holyLand().deleteAbandonedAfterDays() * 24L * 60L;
		List<HolyLand> lands = new ArrayList<HolyLand>(this.repository.all());
		int removed = 0;

		for (HolyLand land : lands)
		{
			// Nobody can pray in a claim land while GriefPrevention is missing: it is left as it is
			if (land.isClaim() && ctx.griefPrevention() == null)
			{
				continue;
			}
			Instant lastPrayer = land.lastPrayerTime();
			// A time that couldn't be read (kept as it was in the extras, under its key in holyland.yml) proves nothing
			if (lastPrayer == null && land.extras().containsKey("LastPrayerTime"))
			{
				continue;
			}
			// No prayer time at all: treated as abandoned
			if (lastPrayer == null || (now - lastPrayer.toEpochMilli()) / 60000L > maxMinutes)
			{
				this.repository.remove(land);
				removed++;
			}
		}

		if (removed > 0)
		{
			this.repository.save();
		}

		long timeAfter = System.currentTimeMillis();

		ctx.plugin().logDebug("Traversed " + lands.size() + " Holy lands in " + (timeAfter - timeBefore) + " ms, removed " + removed + " abandoned");
	}

	public void removeHolyLandsForGod(String godName)
	{
		boolean changed = false;

		for (HolyLand land : new ArrayList<HolyLand>(this.repository.all()))
		{
			if (godName != null && godName.equals(land.godName()))
			{
				this.repository.remove(land);
				changed = true;
			}
		}

		if (changed)
		{
			this.repository.save();
		}
	}

	/**
	 * How many holy lands the god holds: one per altar that claimed land and one per claim land (/g holyclaim), whatever
	 * its size; claim lands only while GriefPrevention is on the server
	 */
	public int getHolyLandsForGod(String godName)
	{
		int lands = 0;
		boolean claims = ctx.griefPrevention() != null;

		for (HolyLand land : this.repository.all())
		{
			if (godName != null && godName.equals(land.godName()) && (claims || !land.isClaim()))
			{
				lands++;
			}
		}

		return lands;
	}

	/** How many holy lands the god may hold: power / 10 + 3 */
	public int getMaximumHolyLandsForGod(String godName)
	{
		return RankRules.maxHolyLands(ctx.godService().getGodPower(godName));
	}

	/** How many chunks around its center an altar's land of this god takes (ranks.unlocks.holy-land-radius-chunks) */
	private int radiusFor(String godName)
	{
		return ctx.settings().ranks().unlocks().holyLandRadiusChunks(ctx.rankService().getRank(godName));
	}

	/**
	 * A believer built a praying altar: the god claims its land (see {@link #claim}). Tells the believer when the god
	 * holds as many holy lands as it may.
	 *
	 * @param altarBlock the altar block
	 * @param sign       the altar's sign
	 */
	public void addAltar(Player player, String godName, Location altarBlock, Location sign)
	{
		claim(player, godName, altarBlock, sign, true);
	}

	/**
	 * A believer prayed at a praying altar: the land of the altar is kept from being abandoned and grows with the god's
	 * rank, or the god claims it if it's free (see {@link #claim}).
	 */
	public void addPrayer(Player player, String godName, Location altarBlock, Location sign)
	{
		claim(player, godName, altarBlock, sign, false);
	}

	/**
	 * When the altar block's chunk belongs to a land of the god, that land counts as prayed in now and takes the free
	 * chunks of the altar ({@link HolyLandRules#footprint}) and of the square its rank gives it around its center. When
	 * the chunk is free and the god may hold another holy land, a new land is claimed (a cancellable
	 * {@link HolyLandClaimEvent}) with those chunks. Another god's land and neutral land are never taken.
	 */
	private void claim(Player player, String godName, Location altarBlock, Location sign, boolean built)
	{
		// In a claim land the claim is the land: the god's own counts as prayed in, no chunks are claimed in any
		HolyLand claimLand = claimLandAt(altarBlock);
		if (claimLand != null)
		{
			if (godName.equals(claimLand.godName()))
			{
				claimLand.setLastPrayerTime(DATES.now());
				this.repository.save();
			}
			return;
		}

		ChunkKey center = chunkOf(altarBlock);
		List<ChunkKey> footprint = HolyLandRules.footprint(center.world(), altarBlock.getBlockX(), altarBlock.getBlockZ(), sign.getBlockX(), sign.getBlockZ());
		HolyLand land = this.repository.find(center);

		if (land != null)
		{
			if (godName.equals(land.godName()))
			{
				land.setLastPrayerTime(DATES.now());
				grow(land, footprint, godName);
				this.repository.save();
			}
			return;
		}

		if (this.repository.findNeutral(center) != null)
		{
			return;
		}

		if (getHolyLandsForGod(godName) >= getMaximumHolyLandsForGod(godName))
		{
			if (built)
			{
				ctx.godSpeech().sendInfo(player.getUniqueId(), "holy-land.cannot-claim-more", 1);
			}
			return;
		}

		if (ctx.fire(new HolyLandClaimEvent(godName, player, altarBlock)).isCancelled())
		{
			return;
		}

		land = this.repository.create(center);
		giveLand(land, godName);
		land.setWorldName(altarBlock.getWorld().getName());
		land.setName(generateHolyLandName(altarBlock.getBlock().getBiome()));
		grow(land, footprint, godName);
		this.repository.save();

		int claimed = getHolyLandsForGod(godName);
		int max = getMaximumHolyLandsForGod(godName);

		ctx.godSpeech().sayToBelievers(godName, Voice.BELIEVERS_CLAIMED_HOLY_LAND, 1, Messages.player(player.getName()));
		ctx.godSpeech().sendInfo(player.getUniqueId(), "holy-land.size", 20 * 5, Messages.text("claimed", claimed), Messages.text("max", max));
	}

	/** Adds the free chunks of the footprint and of the square of the god's radius around the land's center */
	private void grow(HolyLand land, List<ChunkKey> footprint, String godName)
	{
		List<ChunkKey> wanted = new ArrayList<ChunkKey>(footprint);
		wanted.addAll(HolyLandRules.square(land.center(), radiusFor(godName)));
		for (ChunkKey chunk : HolyLandRules.claimable(wanted, this::isFree))
		{
			this.repository.addChunk(land, chunk);
		}
	}

	/**
	 * A praying altar of {@code godName} with its altar block at {@code block} is gone (or no longer the god's praying
	 * altar; the altar registry has already forgotten it): the land it stands in goes with it when it is the god's and no
	 * other registered praying altar of the god stands in it ({@link HolyLandRules#goesWithAltar}). The god's online
	 * believers are told. Depends on the world only, like the registry.
	 *
	 * @return whether the land went (and the believers were told)
	 */
	public boolean altarRemoved(String godName, BlockPos block)
	{
		if (godName == null)
		{
			return false;
		}
		boolean claimLandWent = claimLandAltarRemoved(godName, block);

		HolyLand land = this.repository.find(ChunkKey.of(block.world(), block.x(), block.z()));
		if (land == null)
		{
			return claimLandWent;
		}
		List<ChunkKey> otherAltars = new ArrayList<ChunkKey>();
		for (Altar altar : ctx.altarRegistry().forGod(godName))
		{
			if (altar.kind() == AltarKind.PRAYING)
			{
				otherAltars.add(ChunkKey.of(altar.block().world(), altar.block().x(), altar.block().z()));
			}
		}
		if (!HolyLandRules.goesWithAltar(land.godName(), godName, land.chunks(), otherAltars))
		{
			return claimLandWent;
		}

		this.repository.remove(land);
		this.repository.save();

		ctx.plugin().log("The holy land " + land.name() + " of " + godName + " in " + land.worldName() + " at chunk " + land.center().coordinates() + " is gone with its altar.");
		ctx.godSpeech().sendInfoToBelievers(godName, "holy-land.lost-altar", Messages.god(godName), Messages.text("name", land.name() != null ? land.name() : ""));
		return true;
	}

	// ---------------------------------------------------------------------
	// Claim lands (/g holyclaim: a priest's GriefPrevention claim is the holy land of their god)

	/** The altar blocks of the god's registered praying altars */
	private List<BlockPos> prayingAltarBlocks(String godName)
	{
		List<BlockPos> blocks = new ArrayList<BlockPos>();
		for (Altar altar : ctx.altarRegistry().forGod(godName))
		{
			if (altar.kind() == AltarKind.PRAYING)
			{
				blocks.add(altar.block());
			}
		}
		return blocks;
	}

	/**
	 * /g holyclaim: {@code player}, a priest of {@code godName}, makes the GriefPrevention claim they stand in the holy
	 * land of their god, or releases it when it already is ({@link HolyClaimRules#decide}). A new claim land counts as one
	 * holy land whatever the claim's size, fires the cancellable {@link HolyLandClaimEvent} (at the altar block in it),
	 * and the god tells its believers; a release tells them too.
	 *
	 * @return what was done or why not
	 */
	public HolyClaimRules.Answer holyClaim(Player player, String godName)
	{
		HolyClaimRules.ClaimInfo claim = ctx.griefPrevention() != null ? ctx.griefPrevention().claimInfoAt(player.getLocation(), false) : null;
		HolyLand existing = claim != null ? this.repository.findClaim(claim.id()) : null;
		BlockPos altar = null;
		if (claim != null)
		{
			for (BlockPos block : prayingAltarBlocks(godName))
			{
				if (claim.contains(block))
				{
					altar = block;
					break;
				}
			}
		}
		// A claim land without a god (a hand edit) is nobody's to take
		String claimLandGod = existing == null ? null : existing.godName() != null ? existing.godName() : "";
		HolyClaimRules.Answer answer = HolyClaimRules.decide(claim, player.getUniqueId(), godName, claimLandGod, altar != null, getHolyLandsForGod(godName), getMaximumHolyLandsForGod(godName));

		if (answer == HolyClaimRules.Answer.RELEASE)
		{
			this.repository.remove(existing);
			this.repository.save();
			ctx.plugin().log(player.getName() + " released the GriefPrevention claim " + claim.id() + " in " + claim.world() + ": it is no longer the holy land " + existing.name() + " of " + godName + ".");
			ctx.godSpeech().sendInfoToBelievers(godName, "holy-land.claim-released", Messages.god(godName), Messages.player(player.getName()), Messages.text("name", nullToEmpty(existing.name())));
		}
		else if (answer == HolyClaimRules.Answer.CLAIM)
		{
			Location altarBlock = new Location(player.getWorld(), altar.x(), altar.y(), altar.z());
			if (ctx.fire(new HolyLandClaimEvent(godName, player, altarBlock)).isCancelled())
			{
				return HolyClaimRules.Answer.CANCELLED;
			}
			HolyLand land = this.repository.createClaim(ChunkKey.of(altar.world(), altar.x(), altar.z()), claim.id());
			giveLand(land, godName);
			land.setWorldName(altar.world());
			land.setName(generateHolyLandName(altarBlock.getBlock().getBiome()));
			this.repository.save();
			ctx.plugin().log(player.getName() + " made the GriefPrevention claim " + claim.id() + " in " + claim.world() + " the holy land " + land.name() + " of " + godName + ".");
			ctx.godSpeech().sayToBelievers(godName, Voice.BELIEVERS_CLAIMED_HOLY_LAND, 1, Messages.player(player.getName()));
		}
		return answer;
	}

	/**
	 * A praying altar of {@code godName} with its altar block at {@code block} is gone (the registry has forgotten it):
	 * the claim land it stood in goes with it when it is the god's and no other registered praying altar of the god
	 * stands in the claim ({@link HolyClaimRules#goesWithAltar}).
	 *
	 * @return whether the claim land went (and the believers were told)
	 */
	private boolean claimLandAltarRemoved(String godName, BlockPos block)
	{
		if (!watchesClaims())
		{
			return false;
		}
		World world = ctx.plugin().getServer().getWorld(block.world());
		if (world == null)
		{
			return false;
		}
		HolyLand land = claimLandAt(new Location(world, block.x(), block.y(), block.z()));
		if (land == null)
		{
			return false;
		}
		HolyClaimRules.ClaimInfo claim = ctx.griefPrevention().claim(land.claimId());
		if (HolyClaimRules.goesWithAltar(land.godName(), godName, claim, prayingAltarBlocks(godName)))
		{
			removeClaimLand(land, HolyClaimRules.Loss.NO_ALTAR);
			return true;
		}
		return false;
	}

	/**
	 * Checks that the claim land of this GriefPrevention claim still is holy land ({@link HolyClaimRules#check}): the
	 * claim exists and is a player's, and with {@code altarRule} (after a resize) a praying altar of its god still stands
	 * in it. Nothing happens without GriefPrevention: claim lands are never removed because it is missing.
	 */
	public void checkClaimLand(long claimId, boolean altarRule)
	{
		HolyLand land = this.repository.findClaim(claimId);
		if (land == null || ctx.griefPrevention() == null)
		{
			return;
		}
		HolyClaimRules.ClaimInfo claim = ctx.griefPrevention().claim(claimId);
		Boolean altarInside = altarRule && claim != null ? HolyClaimRules.anyInside(claim, prayingAltarBlocks(land.godName())) : null;
		HolyClaimRules.Loss loss = HolyClaimRules.check(claim, altarInside);
		if (loss != null)
		{
			removeClaimLand(land, loss);
		}
	}

	/** The god's turn: its claim lands whose claims are gone (also while Gods was off) are no longer holy land */
	public void checkClaimLands(String godName)
	{
		if (ctx.griefPrevention() == null || !this.repository.hasClaimLands())
		{
			return;
		}
		for (HolyLand land : new ArrayList<HolyLand>(this.repository.claimLands()))
		{
			if (godName.equals(land.godName()))
			{
				checkClaimLand(land.claimId(), false);
			}
		}
	}

	/**
	 * GriefPrevention gives the claim of a claim land to {@code newOwner} (null: it becomes an admin claim): the land
	 * stays only when the new owner is a priest of its god ({@link HolyClaimRules#keptByNewOwner})
	 */
	public void claimTransferred(long claimId, UUID newOwner)
	{
		HolyLand land = this.repository.findClaim(claimId);
		if (land == null)
		{
			return;
		}
		boolean priest = newOwner != null && land.godName() != null && ctx.priestService().isPriestForGod(newOwner, land.godName());
		if (!HolyClaimRules.keptByNewOwner(newOwner, priest))
		{
			removeClaimLand(land, HolyClaimRules.Loss.NEW_OWNER);
		}
	}

	private void removeClaimLand(HolyLand land, HolyClaimRules.Loss loss)
	{
		this.repository.remove(land);
		this.repository.save();

		String godName = land.godName();
		ctx.plugin().log("The holy land " + land.name() + " of " + godName + " (the GriefPrevention claim " + land.claimId() + " in " + land.worldName() + ") is gone: " + loss + ".");
		if (godName != null)
		{
			ctx.godSpeech().sendInfoToBelievers(godName, loss == HolyClaimRules.Loss.NO_ALTAR ? "holy-land.lost-altar" : "holy-land.claim-lost", Messages.god(godName), Messages.text("name", nullToEmpty(land.name())));
		}
	}

	private static String nullToEmpty(String text)
	{
		return text != null ? text : "";
	}

	// ---------------------------------------------------------------------
	// Neutral lands

	public boolean isNeutralLandLocation(Location location)
	{
		return this.repository.findNeutral(chunkOf(location)) != null;
	}

	/** Makes the chunks around the location neutral land ({@link HolyLandRules#NEUTRAL_RADIUS_CHUNKS}) */
	public void setNeutralLand(Location location)
	{
		Instant now = DATES.now();
		for (ChunkKey chunk : HolyLandRules.square(chunkOf(location), HolyLandRules.NEUTRAL_RADIUS_CHUNKS))
		{
			this.repository.putNeutral(new NeutralLand(chunk, now));
		}

		this.repository.save();
	}

	/** Makes the chunks around the location ({@link HolyLandRules#NEUTRAL_RADIUS_CHUNKS}) no longer neutral land */
	public void clearNeutralLand(Location location)
	{
		for (ChunkKey chunk : HolyLandRules.square(chunkOf(location), HolyLandRules.NEUTRAL_RADIUS_CHUNKS))
		{
			this.repository.removeNeutral(chunk);
		}

		this.repository.save();
	}

	// ---------------------------------------------------------------------
	// Contested lands (a priest started an attack on another god's holy land: the whole land of an altar)

	public boolean isContestedLand(Location location)
	{
		return getAttackingGod(location) != null;
	}

	/** The god attacking the holy land at location, or null */
	public String getAttackingGod(Location location)
	{
		HolyLand land = find(location);
		return land != null ? land.attackingGodName() : null;
	}

	public void setContestedLand(Location location, String attackingGodName)
	{
		HolyLand land = find(location);
		if (land == null)
		{
			return;
		}

		land.setAttackingGodName(attackingGodName);
		land.setContestedTime(DATES.now());

		this.repository.save();
	}

	public void clearContestedLand(Location location)
	{
		HolyLand land = find(location);
		if (land != null)
		{
			land.clearContest();
		}

		this.repository.save();
	}


	/** Minutes left of the attack on this holy land; 0 when the defenders have held out. */
	public long getContestedTimeAtHolyLand(Location location)
	{
		HolyLand land = find(location);
		return HolyLandContest.minutesLeft(land != null ? land.contestedTime() : null, Instant.now());
	}

	/**
	 * True if godName is attacking the holy land at location right now. An attack whose time ran out is settled here
	 * (the defenders win), so an attack nobody visits still ends.
	 */
	public boolean isAttackingHolyLand(String godName, Location location)
	{
		if (location == null || location.getWorld() == null || !godName.equals(getAttackingGod(location)))
		{
			return false;
		}
		if (getContestedTimeAtHolyLand(location) <= 0L)
		{
			resolveContestedLand(location, getGodAtHolyLandLocation(location), godName, true);
			return false;
		}
		return true;
	}

	/** True if some god is attacking the holy land at location right now. */
	public boolean isUnderAttack(Location location)
	{
		String attackingGodName = getAttackingGod(location);
		return attackingGodName != null && isAttackingHolyLand(attackingGodName, location);
	}

	/** The status of the attack on the land at location, for the title shown when entering it; null if none */
	public HolyLand getContest(Location location)
	{
		HolyLand land = find(location);
		return land != null && land.attackingGodName() != null ? land : null;
	}

	/** A player was killed by {@code killer} in a contested holy land: the kill counts for the killer's side */
	public void handleContestKill(Location location, Player killer)
	{
		HolyLand land = getContest(location);
		if (land == null)
		{
			return;
		}

		String defendingGodName = land.godName();
		String killerGodName = ctx.believerService().getGodForBeliever(killer.getUniqueId());
		if (killerGodName == null || defendingGodName == null)
		{
			return;
		}

		String attackingGodName = land.attackingGodName();
		if (!killerGodName.equals(attackingGodName) && !killerGodName.equals(defendingGodName))
		{
			return;
		}

		HolyLandContest.Outcome outcome = HolyLandContest.recordKill(land, killerGodName);
		this.repository.save();
		settle(location, defendingGodName, attackingGodName, outcome);
	}

	/**
	 * A player ate in a contested holy land: outsiders eating the god's unholy food desecrate it, the god's believers
	 * eating its holy food undo that.
	 */
	public void handleContestMeal(Location location, Player player, Material food)
	{
		HolyLand land = getContest(location);
		if (land == null || land.godName() == null)
		{
			return;
		}

		String holyLandGodName = land.godName();
		String attackingGodName = land.attackingGodName();
		String playerGodName = ctx.believerService().getGodForBeliever(player.getUniqueId());

		boolean unholyFood = food == ctx.commandmentService().getUnholyFoodTypeForGod(holyLandGodName);
		boolean holyFood = food == ctx.commandmentService().getHolyFoodTypeForGod(holyLandGodName);
		HolyLandContest.Outcome outcome = HolyLandContest.recordMeal(land, playerGodName, unholyFood, holyFood);
		if (outcome == null)
		{
			return;
		}
		this.repository.save();
		settle(location, holyLandGodName, attackingGodName, outcome);
	}

	private void settle(Location location, String defendingGodName, String attackingGodName, HolyLandContest.Outcome outcome)
	{
		if (outcome == HolyLandContest.Outcome.ATTACKERS_WIN)
		{
			resolveContestedLand(location, defendingGodName, attackingGodName, false);
		}
		else if (outcome == HolyLandContest.Outcome.DEFENDERS_WIN)
		{
			resolveContestedLand(location, defendingGodName, attackingGodName, true);
		}
	}

	/** Ends the attack on the land at location: the whole land goes to the attackers unless the defenders won */
	public void resolveContestedLand(Location location, String defendingGodName, String attackingGodName, boolean defenderWins)
	{
		if (defendingGodName != null && attackingGodName != null)
		{
			if (defenderWins)
			{
				ctx.godSpeech().sayToBelievers(defendingGodName, Voice.BELIEVERS_DEFEND_HOLY_LAND_SUCCESS, 1);
				ctx.godSpeech().sayToBelievers(attackingGodName, Voice.BELIEVERS_ATTACK_HOLY_LAND_FAILED, 1);
			}
			else
			{
				HolyLand land = find(location);
				if (land != null)
				{
					giveLand(land, attackingGodName);
					this.repository.save();
				}
				ctx.godSpeech().sayToBelievers(defendingGodName, Voice.BELIEVERS_DEFEND_HOLY_LAND_FAILED, 1);
				ctx.godSpeech().sayToBelievers(attackingGodName, Voice.BELIEVERS_ATTACK_HOLY_LAND_SUCCESS, 1);
			}
		}

		clearContestedLand(location);

		// Only the attacker's attack ends; the defender may be attacking a land of its own
		if (attackingGodName != null)
		{
			ctx.relationService().clearContestedHolyLandForGod(attackingGodName);
		}
	}
}
