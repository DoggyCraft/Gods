package com.dogonfire.gods.integration.npc;

import com.dogonfire.gods.model.NpcSkin;
import org.bukkit.Location;

/**
 * What a god's NPC should be now: where it stands (with the way it faces when nobody is near), the
 * name above its head and its skin. {@code GodNpcService} works it out; a {@link GodNpcProvider}
 * makes the NPC match it.
 *
 * @param god the god's name
 * @param location the block it stands on top of, centred, with its yaw
 * @param name the name shown above its head, or null for none (the altar's floating text already
 *     shows it)
 * @param skin its skin, or null for the provider's default look
 */
public record NpcSpec(String god, Location location, String name, NpcSkin skin) {
  /**
   * Whether an NPC made for {@code other} can stay as it is for this one (same place, name and
   * skin)
   */
  public boolean sameAs(NpcSpec other) {
    return other != null
        && this.god.equals(other.god)
        && sameSpot(this.location, other.location)
        && java.util.Objects.equals(this.name, other.name)
        && java.util.Objects.equals(this.skin, other.skin);
  }

  private static boolean sameSpot(Location a, Location b) {
    return a.getWorld() == b.getWorld()
        && a.getX() == b.getX()
        && a.getY() == b.getY()
        && a.getZ() == b.getZ()
        && a.getYaw() == b.getYaw();
  }
}
