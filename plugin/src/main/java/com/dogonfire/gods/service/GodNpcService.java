package com.dogonfire.gods.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.integration.npc.GodNpcProvider;
import com.dogonfire.gods.integration.npc.MannequinNpcs;
import com.dogonfire.gods.integration.npc.NpcProviders;
import com.dogonfire.gods.integration.npc.NpcSpec;
import com.dogonfire.gods.integration.npc.SkinResolver;
import com.dogonfire.gods.model.Altar;
import com.dogonfire.gods.model.AltarKind;
import com.dogonfire.gods.model.BlockPos;
import com.dogonfire.gods.model.NpcSkin;

/**
 * The god as a figure at its main altar (config.yml's {@code npc} section): it stands on the spot in front of the main
 * altar's sign ({@link MainAltarRules#standingSpot}), facing the altar, and a right click on it prays as a click on
 * the sign does. Who makes the figure is a {@link GodNpcProvider} ({@link NpcProviders} picks it). Rules, as for the
 * floating texts ({@link AltarTextService}):
 * <ul>
 * <li>Never saved: every NPC is made again from gods.yml and altars.yml. {@link #start()} removes strays a crash left
 * behind, {@link #stop()} (reload, disable) removes them all.</li>
 * <li>Shown for an existing god whose main altar is registered as its praying altar, when the chunks of its sign,
 * altar block and spot are loaded and a figure fits on the spot; at most {@link GodNpcRules#MAX_NPCS}. Shown or
 * updated when a chunk with the altar is scanned, the main altar changes, the god is created, and on the god's turn
 * ({@code GodUpdateTask}); removed when a chunk of it unloads, the main altar goes or changes, or the god is
 * removed.</li>
 * <li>Its name shows above its head only when the altar has no floating text ({@link GodNpcRules#showsName}).</li>
 * </ul>
 * Main thread only.
 */
public class GodNpcService
{
	private final GodsContext		ctx;
	// The provider in use, or null when gods have no NPCs
	private GodNpcProvider			provider;
	// The gods that have an NPC
	private final Set<String>		shown	= new HashSet<>();
	private BukkitTask				task;
	// The provider wasn't ready when the NPCs were to be shown; they are shown when it is
	private boolean					waiting;
	// Players' skins by name, kept for the life of the plugin
	private final SkinResolver		skins;

	public GodNpcService(GodsContext ctx)
	{
		this.ctx = ctx;
		this.skins = new SkinResolver(ctx.plugin());
	}

	/** Picks the provider config.yml asks for, removes strays, and shows the NPCs of main altars that are loaded */
	public void start()
	{
		Settings.Npc settings = ctx.settings().npc();
		Plugin plugin = ctx.plugin();
		boolean mannequin = MannequinNpcs.isAvailable();
		NpcProviders.Choice choice = NpcProviders.choose(settings.enabled(), settings.provider(), isEnabled(NpcProviders.CITIZENS), isEnabled(NpcProviders.FANCY_NPCS),
				mannequin);
		if (choice.warning() != null)
		{
			plugin.getLogger().warning(choice.warning());
		}
		if (mannequin && choice.provider() != Settings.NpcProvider.BUILTIN)
		{
			// Built-in NPCs a crash left behind, though another provider (or none) is used now
			MannequinNpcs.removeTagged(plugin);
		}
		this.provider = create(choice.provider());
		if (this.provider == null)
		{
			ctx.plugin().logDebug("Gods have no NPCs at their main altars.");
			return;
		}
		ctx.plugin().log("Gods appear at their main altars through " + this.provider.name() + ".");
		this.provider.removeStrays();
		this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
		this.waiting = true;
		tick();
	}

	/** Removes every NPC and lets go of the provider (reload, disable) */
	public void stop()
	{
		if (this.task != null)
		{
			this.task.cancel();
			this.task = null;
		}
		if (this.provider != null)
		{
			try
			{
				this.provider.close();
			}
			catch (RuntimeException | LinkageError e)
			{
				ctx.plugin().getLogger().log(Level.WARNING, "Could not remove the gods' NPCs made through " + this.provider.name(), e);
			}
			this.provider = null;
		}
		this.shown.clear();
	}

	private boolean isEnabled(String pluginName)
	{
		Plugin other = ctx.plugin().getServer().getPluginManager().getPlugin(pluginName);
		return other != null && other.isEnabled();
	}

	/** The provider; null for none, or if it can't be made (logged) */
	private GodNpcProvider create(Settings.NpcProvider kind)
	{
		try
		{
			return switch (kind)
			{
				case BUILTIN -> new MannequinNpcs(ctx.plugin());
				default -> null;
			};
		}
		catch (RuntimeException | LinkageError e)
		{
			ctx.plugin().getLogger().log(Level.WARNING, "Could not start the gods' NPCs (" + kind.name().toLowerCase(java.util.Locale.ROOT) + "); gods have no NPCs.", e);
			return null;
		}
	}

	/** Once a second: shows the NPCs when a provider became ready, and lets the NPCs look at players */
	private void tick()
	{
		if (this.provider == null || !this.provider.ready())
		{
			return;
		}
		if (this.waiting)
		{
			this.waiting = false;
			for (String godName : ctx.godService().getAllGods())
			{
				refreshGod(godName);
			}
		}
		this.provider.tick();
	}

	// ---------------------------------------------------------------------
	// Showing and hiding

	/** Shows, updates or removes the god's NPC as things are now */
	public void refreshGod(String godName)
	{
		if (this.provider == null || godName == null || this.waiting)
		{
			return;
		}
		NpcSpec spec = specFor(godName);
		if (spec == null || (!this.shown.contains(godName) && this.shown.size() >= GodNpcRules.MAX_NPCS))
		{
			hide(godName);
			return;
		}
		boolean ok;
		try
		{
			ok = this.provider.show(spec);
		}
		catch (RuntimeException | LinkageError e)
		{
			ctx.plugin().getLogger().log(Level.WARNING, "Could not show the NPC of " + godName + " through " + this.provider.name(), e);
			ok = false;
		}
		if (ok)
		{
			this.shown.add(godName);
		}
		else
		{
			hide(godName);
		}
	}

	private void hide(String godName)
	{
		if (this.shown.remove(godName) && this.provider != null)
		{
			this.provider.remove(godName);
		}
	}

	/** A chunk was scanned for altars: the NPCs of the main altars in it are shown */
	public void chunkScanned(BlockPos.ChunkPos chunk)
	{
		if (this.provider == null)
		{
			return;
		}
		for (Altar altar : ctx.altarRegistry().inChunk(chunk))
		{
			if (ctx.altarRegistry().isMainAltar(altar))
			{
				refreshGod(altar.god());
			}
		}
	}

	/** A chunk unloads: NPCs whose altar or spot is in it go (made again when it loads) */
	public void chunkUnloading(BlockPos.ChunkPos chunk)
	{
		if (this.provider == null || this.shown.isEmpty())
		{
			return;
		}
		for (String godName : new ArrayList<>(this.shown))
		{
			Altar altar = ctx.altarRegistry().mainAltar(godName);
			if (altar == null || altar.sign().chunk().equals(chunk) || altar.block().chunk().equals(chunk) || MainAltarRules.standingSpot(altar).chunk().equals(chunk))
			{
				hide(godName);
			}
		}
	}

	/**
	 * What the god's NPC should be now, or null if it should have none: only for an existing god whose main altar is
	 * a registered praying altar of it, in a world Gods is active in, with the chunks of its sign, altar block and spot
	 * loaded and room for a figure on the spot
	 */
	private NpcSpec specFor(String godName)
	{
		if (!ctx.godService().godExist(godName))
		{
			return null;
		}
		BlockPos mainSign = ctx.godService().getMainAltar(godName);
		Altar altar = mainSign != null ? ctx.altarRegistry().atSign(mainSign) : null;
		if (altar == null || altar.kind() != AltarKind.PRAYING || !godName.equals(altar.god()))
		{
			return null;
		}
		World world = ctx.plugin().getServer().getWorld(altar.world());
		if (world == null || !ctx.plugin().isEnabledInWorld(world))
		{
			return null;
		}
		BlockPos spot = MainAltarRules.standingSpot(altar);
		AltarRegistry registry = ctx.altarRegistry();
		if (!registry.isLoaded(altar.sign().chunk()) || !registry.isLoaded(altar.block().chunk()) || !registry.isLoaded(spot.chunk()))
		{
			return null;
		}
		Block feet = world.getBlockAt(spot.x(), spot.y(), spot.z());
		if (!GodNpcRules.fits(feet.isPassable(), feet.getRelative(BlockFace.UP).isPassable()))
		{
			ctx.plugin().logDebug("No room for the NPC of " + godName + " at " + AltarRegistry.describe(spot));
			return null;
		}
		Location location = new Location(world, spot.x() + 0.5, spot.y(), spot.z() + 0.5, GodNpcRules.facingYaw(altar), 0F);
		String name = GodNpcRules.showsName(ctx.settings().altarTexts().floatingText()) ? godName : null;
		return new NpcSpec(godName, location, name, skinFor(godName));
	}

	/**
	 * The god's skin ({@link GodNpcRules#skin}: its own, else its god type's), or null for the provider's default look.
	 * A skin known only by name is resolved in the background; until then it is given by name (Citizens and FancyNpcs
	 * resolve names themselves, the mannequin waits), and the NPCs are refreshed when it is known.
	 */
	private NpcSkin skinFor(String godName)
	{
		NpcSkin skin = GodNpcRules.skin(ctx.godService().getNpcSkin(godName), ctx.themes().of(ctx.godService().getDivineForceForGod(godName)).npcSkin());
		if (skin == null || skin.hasTexture())
		{
			return skin;
		}
		NpcSkin known = this.skins.known(skin.name());
		if (known != null)
		{
			return known;
		}
		this.skins.resolve(skin.name(), resolved -> {
			if (resolved != null)
			{
				// Next tick: the answer may come at once (an online player), in the middle of a refresh
				ctx.plugin().getServer().getScheduler().runTask(ctx.plugin(), this::refreshShown);
			}
		});
		known = this.skins.known(skin.name());
		return known != null ? known : skin;
	}

	/** The NPCs shown are brought in line again (a skin was resolved) */
	private void refreshShown()
	{
		for (String godName : new ArrayList<>(this.shown))
		{
			refreshGod(godName);
		}
	}

	/** Finds players' skins by name (/g godskin, and themes that name a player) */
	public SkinResolver skins()
	{
		return this.skins;
	}

	// ---------------------------------------------------------------------
	// Clicks and lookups

	/** The god whose NPC the entity is, or null */
	public String godOf(Entity entity)
	{
		return this.provider != null ? this.provider.godOf(entity) : null;
	}

	/** The entity of the god's NPC, or null (none, or a provider without server-side entities) */
	public Entity entity(String godName)
	{
		return this.provider != null && godName != null && this.shown.contains(godName) ? this.provider.entity(godName) : null;
	}

	/**
	 * A player right-clicked the god's NPC: prays at its main altar as a click on the altar's sign does (the same
	 * checks, cooldowns, effects and events)
	 *
	 * @param mainHand whether it is the main hand's event (the off hand's does nothing)
	 */
	public void clicked(Player player, String godName, boolean mainHand)
	{
		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return;
		}
		BlockPos mainSign = ctx.godService().getMainAltar(godName);
		Altar altar = mainSign != null ? ctx.altarRegistry().atSign(mainSign) : null;
		if (GodNpcRules.click(mainHand, godName, altar) != GodNpcRules.Click.PRAY)
		{
			return;
		}
		Block sign = ctx.altarRegistry().block(altar.sign());
		AltarSigns.AltarSign read = sign != null ? ctx.altarService().readAltarSign(sign) : null;
		if (read == null || read.kind() != AltarSigns.Kind.PRAYING)
		{
			return;
		}
		ctx.prayerService().prayAtAltarSign(player, sign, read.god());
	}
}
