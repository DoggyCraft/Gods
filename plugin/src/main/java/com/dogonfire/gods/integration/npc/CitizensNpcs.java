package com.dogonfire.gods.integration.npc;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.dogonfire.gods.model.NpcSkin;
import com.dogonfire.gods.service.GodNpcRules;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.SpawnReason;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.LookClose;
import net.citizensnpcs.trait.SkinTrait;

/**
 * God NPCs made by Citizens: player NPCs in an in-memory registry of Gods' own ({@value #REGISTRY}), so Citizens never
 * saves them to its files and {@code /npc} lists don't fill up with gods. Protected (no damage, no pushing), with
 * Citizens' look-close trait turning them toward players close by, and the god's skin through the skin trait: a
 * signed texture as it is, else a player's name for Citizens to fetch; never the god's own name (Citizens' default),
 * which could be anyone's. Only made when Citizens is installed: this class names Citizens' types.
 */
public final class CitizensNpcs implements GodNpcProvider
{
	/** The name of Gods' NPC registry in Citizens */
	public static final String			REGISTRY	= "Gods";

	private final NPCRegistry			registry;
	private final Map<String, NPC>		npcs		= new HashMap<>();
	private final Map<String, NpcSpec>	specs		= new HashMap<>();
	// The god of each NPC, by its Citizens id
	private final Map<Integer, String>	gods		= new HashMap<>();

	public CitizensNpcs()
	{
		// One left by an earlier run (a reload that didn't stop cleanly) goes first
		NPCRegistry old = CitizensAPI.getNamedNPCRegistry(REGISTRY);
		if (old != null)
		{
			old.deregisterAll();
			CitizensAPI.removeNamedNPCRegistry(REGISTRY);
		}
		this.registry = CitizensAPI.createInMemoryNPCRegistry(REGISTRY);
	}

	@Override
	public String name()
	{
		return "Citizens";
	}

	@Override
	public boolean show(NpcSpec spec)
	{
		NPC npc = this.npcs.get(spec.god());
		NpcSpec old = this.specs.get(spec.god());
		if (npc != null && npc.isSpawned() && spec.sameAs(old))
		{
			return true;
		}
		if (npc != null && old != null && !Objects.equals(old.skin(), spec.skin()))
		{
			// A new skin is a new NPC (the skin trait can't go back to the default look)
			remove(spec.god());
			npc = null;
			old = null;
		}
		if (npc == null)
		{
			npc = this.registry.createNPC(EntityType.PLAYER, spec.god());
			this.npcs.put(spec.god(), npc);
			this.gods.put(npc.getId(), spec.god());
			npc.setProtected(true);
			npc.data().set(NPC.Metadata.COLLIDABLE, false);
			npc.data().set(NPC.Metadata.SILENT, true);
			LookClose look = npc.getOrAddTrait(LookClose.class);
			look.setRange(GodNpcRules.LOOK_RADIUS_BLOCKS);
			look.lookClose(true);
			// Without a skin of its own it would wear the skin of whoever has the god's name
			npc.getOrAddTrait(SkinTrait.class).setFetchDefaultSkin(false);
		}
		npc.data().set(NPC.Metadata.NAMEPLATE_VISIBLE, spec.name() != null);
		if (old == null)
		{
			dress(npc, spec);
		}
		if (!npc.isSpawned())
		{
			npc.spawn(spec.location(), SpawnReason.PLUGIN);
		}
		else if (old == null || !old.location().equals(spec.location()))
		{
			npc.teleport(spec.location(), PlayerTeleportEvent.TeleportCause.PLUGIN);
		}
		this.specs.put(spec.god(), spec);
		return npc.isSpawned();
	}

	/** A signed texture as it is; a name for Citizens to fetch; nothing for the default look */
	private static void dress(NPC npc, NpcSpec spec)
	{
		SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
		NpcSkin skin = spec.skin();
		if (skin != null && skin.isSigned())
		{
			String id = skin.name() != null ? skin.name() : "gods-" + Integer.toHexString(spec.god().hashCode());
			skinTrait.setSkinPersistent(id, skin.signature(), skin.value());
		}
		else if (skin != null && skin.name() != null)
		{
			skinTrait.setSkinName(skin.name(), true);
		}
	}

	@Override
	public void remove(String god)
	{
		this.specs.remove(god);
		NPC npc = this.npcs.remove(god);
		if (npc != null)
		{
			this.gods.remove(npc.getId());
			npc.destroy();
		}
	}

	@Override
	public void close()
	{
		this.npcs.clear();
		this.specs.clear();
		this.gods.clear();
		this.registry.deregisterAll();
		CitizensAPI.removeNamedNPCRegistry(REGISTRY);
	}

	@Override
	public String godOf(Entity entity)
	{
		if (!this.registry.isNPC(entity))
		{
			return null;
		}
		NPC npc = this.registry.getNPC(entity);
		return npc != null ? this.gods.get(npc.getId()) : null;
	}

	@Override
	public Entity entity(String god)
	{
		NPC npc = this.npcs.get(god);
		return npc != null && npc.isSpawned() ? npc.getEntity() : null;
	}
}
