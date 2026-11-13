package com.dogonfire.gods.integration.npc;

import com.dogonfire.gods.model.NpcSkin;
import com.dogonfire.gods.service.GodNpcRules;
import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import de.oliver.fancynpcs.api.actions.ActionTrigger;
import de.oliver.fancynpcs.api.events.NpcInteractEvent;
import de.oliver.fancynpcs.api.skins.SkinData;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * God NPCs made by FancyNpcs (Paper only): packet NPCs that FancyNpcs shows to players near them,
 * never saved to its files ({@code setSaveToFile(false)}), turning toward players close by. They
 * have no server-side entity, so their clicks come through FancyNpcs' {@link NpcInteractEvent}, and
 * {@link #godOf}/{@link #entity} know none. The display name is MiniMessage in FancyNpcs, so the
 * god's name goes in escaped ({@code <empty>} for none); only strings are passed, never Gods'
 * relocated Adventure objects. Only made when FancyNpcs is installed: this class names its types.
 */
public final class FancyNpcsNpcs implements GodNpcProvider, Listener {
  /** The creator FancyNpcs records for Gods' NPCs */
  private static final UUID CREATOR = new UUID(0x60D5L, 0x60D5L);

  private final Plugin plugin;
  private final BiConsumer<Player, String> onRightClick;
  private final Map<String, Npc> npcs = new HashMap<>();
  private final Map<String, NpcSpec> specs = new HashMap<>();
  private final Map<Npc, String> gods = new IdentityHashMap<>();

  /**
   * @param onRightClick called on the main thread with the player and the god whose NPC they
   *     right-clicked
   */
  public FancyNpcsNpcs(Plugin plugin, BiConsumer<Player, String> onRightClick) {
    this.plugin = plugin;
    this.onRightClick = onRightClick;
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @Override
  public String name() {
    return "FancyNpcs";
  }

  /** FancyNpcs loads its own NPCs after it starts; Gods' are added after that */
  @Override
  public boolean ready() {
    return FancyNpcsPlugin.get().getNpcManager().isLoaded();
  }

  @Override
  public boolean show(NpcSpec spec) {
    Npc existing = this.npcs.get(spec.god());
    if (existing != null && spec.sameAs(this.specs.get(spec.god()))) {
      return true;
    }
    // Rare (the altar, name or skin changed): made anew
    remove(spec.god());

    String id = "gods_" + spec.god().replaceAll("[^A-Za-z0-9_]", "_");
    NpcData data = new NpcData(id, CREATOR, spec.location());
    data.setDisplayName(
        spec.name() != null ? MiniMessage.miniMessage().escapeTags(spec.name()) : "<empty>");
    // Only signed textures: a name would make FancyNpcs look it up, maybe on the main thread. Gods
    // resolves names itself
    // (signed on Paper, which FancyNpcs needs) and the NPC is made again when it has the texture.
    NpcSkin skin = spec.skin();
    if (skin != null && skin.isSigned()) {
      data.setSkinData(
          new SkinData(
              skin.name() != null ? skin.name() : id,
              SkinData.SkinVariant.AUTO,
              skin.value(),
              skin.signature()));
    }
    data.setShowInTab(false);
    data.setCollidable(false);
    data.setTurnToPlayer(true);
    data.setTurnToPlayerDistance((int) GodNpcRules.LOOK_RADIUS_BLOCKS);

    FancyNpcsPlugin fancyNpcs = FancyNpcsPlugin.get();
    Npc npc = fancyNpcs.getNpcAdapter().apply(data);
    npc.setSaveToFile(false);
    fancyNpcs.getNpcManager().registerNpc(npc);
    npc.create();
    npc.spawnForAll();
    this.npcs.put(spec.god(), npc);
    this.specs.put(spec.god(), spec);
    this.gods.put(npc, spec.god());
    return true;
  }

  @Override
  public void remove(String god) {
    this.specs.remove(god);
    Npc npc = this.npcs.remove(god);
    if (npc != null) {
      this.gods.remove(npc);
      npc.removeForAll();
      FancyNpcsPlugin.get().getNpcManager().removeNpc(npc);
    }
  }

  @Override
  public void close() {
    for (String god : new java.util.ArrayList<>(this.npcs.keySet())) {
      remove(god);
    }
    HandlerList.unregisterAll(this);
  }

  @Override
  public String godOf(Entity entity) {
    return null;
  }

  @Override
  public Entity entity(String god) {
    return null;
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onNpcInteract(NpcInteractEvent event) {
    String god = this.gods.get(event.getNpc());
    if (god == null) {
      return;
    }
    if (event.getInteractionType() != ActionTrigger.RIGHT_CLICK) {
      return;
    }
    Player player = event.getPlayer();
    if (this.plugin.getServer().isPrimaryThread()) {
      this.onRightClick.accept(player, god);
    } else {
      this.plugin
          .getServer()
          .getScheduler()
          .runTask(this.plugin, () -> this.onRightClick.accept(player, god));
    }
  }
}
