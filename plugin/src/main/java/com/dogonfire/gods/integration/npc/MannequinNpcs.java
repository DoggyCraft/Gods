package com.dogonfire.gods.integration.npc;

import com.dogonfire.gods.model.NpcSkin;
import com.dogonfire.gods.service.GodNpcRules;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

/**
 * The built-in god NPC: the vanilla mannequin (Minecraft 1.21.9+), a player-shaped entity with a
 * skin and no AI.
 *
 * <p>Gods supports servers back to 1.21.3, which have no mannequin, and Spigot and Paper give it
 * different APIs (Spigot: {@code setPlayerProfile(org.bukkit.profile.PlayerProfile)}, {@code
 * setDescription(String)}, {@code setHideDescription}; Paper: {@code
 * setProfile(ResolvableProfile)}, {@code setDescription(Component)}). So nothing here names the
 * {@code Mannequin} type: the entity type is looked up in the registry by its key, spawned through
 * its entity class, and the mannequin's own methods are found by reflection once, picking Paper's
 * or Spigot's skin call by what the interface has. If any of it fails, it is logged once and no
 * more NPCs are made.
 *
 * <p>Each NPC is spawned with {@code setPersistent(false)} and the {@code gods:npc} tag (its god's
 * name) before it enters the world, so it is never saved, and {@link #removeStrays()} finds any a
 * crash left behind. It can't be hurt, pushed or heard, doesn't fall, and turns toward the nearest
 * player close by ({@link #tick()}).
 */
public final class MannequinNpcs implements GodNpcProvider {
  private final Logger logger;
  private final NamespacedKey npcKey;
  private final EntityType type;
  private final Calls calls;
  // The NPCs made, and what they were made as, by god
  private final Map<String, Entity> npcs = new HashMap<>();
  private final Map<String, NpcSpec> specs = new HashMap<>();
  private boolean broken;

  /** The mannequin entity type, or null on servers without one (before 1.21.9) */
  public static EntityType mannequinType() {
    try {
      return Registry.ENTITY_TYPE.get(NamespacedKey.minecraft("mannequin"));
    } catch (RuntimeException e) {
      return null;
    }
  }

  /** Whether the server has mannequins */
  public static boolean isAvailable() {
    EntityType type = mannequinType();
    return type != null && type.getEntityClass() != null;
  }

  /**
   * Only made when {@link #isAvailable()}; finds the mannequin's methods (failing that, it makes no
   * NPCs)
   */
  public MannequinNpcs(Plugin plugin) {
    this.logger = plugin.getLogger();
    this.npcKey = new NamespacedKey(plugin, "npc");
    this.type = mannequinType();
    Calls found = null;
    try {
      found = Calls.find(this.type.getEntityClass());
    } catch (ReflectiveOperationException | RuntimeException e) {
      fail("its mannequin API is not what Gods knows", e);
    }
    this.calls = found;
  }

  @Override
  public String name() {
    return "the built-in mannequin"
        + (this.calls != null ? " (" + (this.calls.paper() ? "Paper" : "Spigot") + ")" : "");
  }

  @Override
  public boolean ready() {
    return !this.broken;
  }

  @Override
  public void removeStrays() {
    removeTagged(this.npcKey, this.type, this.npcs.values(), this.logger);
  }

  /**
   * Removes every built-in god NPC in loaded chunks (left by a crash), also while another provider
   * makes the NPCs; does nothing on a server without mannequins
   */
  public static void removeTagged(Plugin plugin) {
    EntityType type = mannequinType();
    if (type != null && type.getEntityClass() != null) {
      removeTagged(new NamespacedKey(plugin, "npc"), type, java.util.List.of(), plugin.getLogger());
    }
  }

  private static void removeTagged(
      NamespacedKey key, EntityType type, java.util.Collection<Entity> keep, Logger logger) {
    int removed = 0;
    for (World world : org.bukkit.Bukkit.getWorlds()) {
      for (Entity entity : world.getEntitiesByClass(type.getEntityClass())) {
        if (entity.getPersistentDataContainer().has(key, PersistentDataType.STRING)
            && !keep.contains(entity)) {
          entity.remove();
          removed++;
        }
      }
    }
    if (removed > 0) {
      logger.info("Removed " + removed + " god NPCs left from before.");
    }
  }

  @Override
  public boolean show(NpcSpec spec) {
    if (this.broken) {
      return false;
    }
    Entity entity = this.npcs.get(spec.god());
    NpcSpec old = this.specs.get(spec.god());
    // A new skin is a new mannequin (one without a skin can't be given the default look back)
    if (entity != null
        && entity.isValid()
        && old != null
        && java.util.Objects.equals(old.skin(), spec.skin())) {
      if (spec.sameAs(old)) {
        return true;
      }
      try {
        if (!old.location().equals(spec.location())) {
          entity.teleport(spec.location());
        }
        dress(entity, spec);
        this.specs.put(spec.god(), spec);
        return true;
      } catch (ReflectiveOperationException | RuntimeException e) {
        fail("the NPC of " + spec.god() + " could not be changed", e);
        return false;
      }
    }

    remove(spec.god());
    World world = spec.location().getWorld();
    if (world == null) {
      return false;
    }
    Entity spawned;
    try {
      spawned =
          spawn(
              world,
              spec.location(),
              this.type.getEntityClass(),
              setup -> {
                setup.setPersistent(false);
                setup
                    .getPersistentDataContainer()
                    .set(this.npcKey, PersistentDataType.STRING, spec.god());
                setup.setInvulnerable(true);
                setup.setSilent(true);
                setup.setGravity(false);
                if (setup instanceof LivingEntity living) {
                  living.setAI(false);
                  living.setRemoveWhenFarAway(false);
                  living.setCanPickupItems(false);
                }
                try {
                  this.calls.setImmovable.invoke(setup, true);
                  dress(setup, spec);
                } catch (ReflectiveOperationException e) {
                  throw new IllegalStateException(e);
                }
              });
    } catch (RuntimeException e) {
      fail("the NPC of " + spec.god() + " could not be made", e);
      return false;
    }
    if (spawned == null || !spawned.isValid()) {
      // Another plugin (a protection plugin) refused the spawn; tried again on the god's next
      // refresh
      if (spawned != null) {
        spawned.remove();
      }
      return false;
    }
    this.npcs.put(spec.god(), spawned);
    this.specs.put(spec.god(), spec);
    return true;
  }

  @SuppressWarnings("unchecked")
  private static <T extends Entity> Entity spawn(
      World world, Location location, Class<? extends Entity> type, Consumer<Entity> setup) {
    return world.spawn(location, (Class<T>) type, setup::accept);
  }

  /** The name above its head, the description below it (hidden), and the skin */
  private void dress(Entity entity, NpcSpec spec) throws ReflectiveOperationException {
    entity.setCustomName(spec.name());
    entity.setCustomNameVisible(spec.name() != null);
    this.calls.hideDescription(entity);
    NpcSkin skin = spec.skin();
    if (skin != null && skin.hasTexture()) {
      this.calls.setSkin(entity, spec.god(), skin);
    }
  }

  @Override
  public void remove(String god) {
    this.specs.remove(god);
    Entity entity = this.npcs.remove(god);
    if (entity != null && entity.isValid()) {
      entity.remove();
    }
  }

  @Override
  public void close() {
    for (Entity entity : this.npcs.values()) {
      if (entity.isValid()) {
        entity.remove();
      }
    }
    this.npcs.clear();
    this.specs.clear();
  }

  @Override
  public String godOf(Entity entity) {
    String god = entity.getPersistentDataContainer().get(this.npcKey, PersistentDataType.STRING);
    return god != null && this.npcs.get(god) == entity ? god : null;
  }

  @Override
  public Entity entity(String god) {
    Entity entity = this.npcs.get(god);
    return entity != null && entity.isValid() ? entity : null;
  }

  /**
   * Each NPC turns toward the nearest player within {@link GodNpcRules#LOOK_RADIUS_BLOCKS}, else
   * back to the altar
   */
  @Override
  public void tick() {
    for (Iterator<Map.Entry<String, Entity>> it = this.npcs.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<String, Entity> entry = it.next();
      Entity npc = entry.getValue();
      if (!npc.isValid()) {
        // Killed by a command or unloaded with its chunk: made again on the god's next refresh
        it.remove();
        this.specs.remove(entry.getKey());
        continue;
      }
      Location at = npc.getLocation();
      Player nearest = null;
      double best = GodNpcRules.LOOK_RADIUS_BLOCKS * GodNpcRules.LOOK_RADIUS_BLOCKS;
      for (Player player : npc.getWorld().getPlayers()) {
        double distance = player.getLocation().distanceSquared(at);
        if (distance <= best) {
          best = distance;
          nearest = player;
        }
      }
      float yaw;
      float pitch;
      if (nearest != null) {
        Location eyes = nearest.getEyeLocation();
        yaw = GodNpcRules.yawToward(at.getX(), at.getZ(), eyes.getX(), eyes.getZ());
        pitch =
            GodNpcRules.pitchToward(
                eyes.getX() - at.getX(),
                eyes.getY() - (at.getY() + GodNpcRules.EYE_HEIGHT),
                eyes.getZ() - at.getZ());
      } else {
        NpcSpec spec = this.specs.get(entry.getKey());
        yaw = spec != null ? spec.location().getYaw() : at.getYaw();
        pitch = 0F;
      }
      if (GodNpcRules.shouldTurn(at.getYaw(), yaw)
          || GodNpcRules.shouldTurn(at.getPitch(), pitch)) {
        npc.setRotation(yaw, pitch);
      }
    }
  }

  private void fail(String what, Throwable e) {
    if (!this.broken) {
      this.broken = true;
      this.logger.log(
          Level.WARNING,
          "The built-in god NPC does not work on this server: "
              + what
              + ". Gods makes no more built-in NPCs until it is"
              + " reloaded; Citizens or FancyNpcs (npc.provider) can make them instead.",
          e instanceof InvocationTargetException ite ? ite.getCause() : e);
    }
  }

  /**
   * The mannequin's own methods, found once. Paper has {@code setProfile(ResolvableProfile)};
   * Spigot has {@code setPlayerProfile(PlayerProfile)}.
   */
  private record Calls(
      Method setImmovable,
      Method setPlayerProfile,
      Method setHideDescription,
      Method setProfile,
      Method setDescription,
      Method profileBuilder,
      Method addProperty,
      Method build,
      Class<?> profileProperty) {
    static Calls find(Class<?> mannequin) throws ReflectiveOperationException {
      Method setImmovable = mannequin.getMethod("setImmovable", boolean.class);
      Method setProfile = method(mannequin, "setProfile");
      if (setProfile != null) {
        // Paper: a ResolvableProfile built from the texture property
        Class<?> resolvable = setProfile.getParameterTypes()[0];
        Method builder = resolvable.getMethod("resolvableProfile");
        Class<?> property = Class.forName("com.destroystokyo.paper.profile.ProfileProperty");
        Method addProperty = builder.getReturnType().getMethod("addProperty", property);
        Method build = builder.getReturnType().getMethod("build");
        return new Calls(
            setImmovable,
            null,
            null,
            setProfile,
            method(mannequin, "setDescription"),
            builder,
            addProperty,
            build,
            property);
      }
      // Spigot
      return new Calls(
          setImmovable,
          mannequin.getMethod("setPlayerProfile", PlayerProfile.class),
          mannequin.getMethod("setHideDescription", boolean.class),
          null,
          null,
          null,
          null,
          null,
          null);
    }

    /** The one-parameter method of that name, or null */
    private static Method method(Class<?> type, String name) {
      for (Method method : type.getMethods()) {
        if (method.getName().equals(name) && method.getParameterCount() == 1) {
          return method;
        }
      }
      return null;
    }

    boolean paper() {
      return this.setProfile != null;
    }

    /** No "NPC" line below its name */
    void hideDescription(Entity entity) throws ReflectiveOperationException {
      if (paper()) {
        if (this.setDescription != null) {
          // Paper: a null description shows none (the server's own Component type, so nothing of
          // Gods' is passed)
          this.setDescription.invoke(entity, (Object) null);
        }
      } else {
        this.setHideDescription.invoke(entity, true);
      }
    }

    void setSkin(Entity entity, String god, NpcSkin skin) throws ReflectiveOperationException {
      if (paper()) {
        Object property =
            skin.signature() != null
                ? this.profileProperty
                    .getConstructor(String.class, String.class, String.class)
                    .newInstance("textures", skin.value(), skin.signature())
                : this.profileProperty
                    .getConstructor(String.class, String.class)
                    .newInstance("textures", skin.value());
        Object builder = this.profileBuilder.invoke(null);
        this.addProperty.invoke(builder, property);
        this.setProfile.invoke(entity, this.build.invoke(builder));
        return;
      }
      // Spigot takes the skin's URL and model, not the texture value
      SkinTextures.Texture texture = SkinTextures.decode(skin.value());
      if (texture == null) {
        return;
      }
      PlayerProfile profile =
          org.bukkit.Bukkit.createPlayerProfile(
              UUID.nameUUIDFromBytes(("gods-npc:" + god).getBytes(StandardCharsets.UTF_8)), null);
      PlayerTextures textures = profile.getTextures();
      try {
        textures.setSkin(
            new URI(texture.url()).toURL(),
            texture.slim() ? PlayerTextures.SkinModel.SLIM : PlayerTextures.SkinModel.CLASSIC);
      } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
        return;
      }
      profile.setTextures(textures);
      this.setPlayerProfile.invoke(entity, profile);
    }
  }
}
