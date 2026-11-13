package com.dogonfire.gods.service;

import com.dogonfire.gods.model.HolyPower;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * When each player may use each holy power again, and until when a sun, moon or storm power is
 * moving a world's sky ({@link HolyPowerRules}). Kept in memory only. Times are passed in, so it
 * can be tested without a server.
 */
public final class PowerCooldowns {
  /** The answer to a use: allowed (and counted), or why not */
  public sealed interface Result {}

  public record Ready() implements Result {}

  /** The player used this power too recently */
  public record Cooling(long remainingMillis) implements Result {
    /** Seconds left, rounded up, as the player is told */
    public long seconds() {
      return this.remainingMillis / 1000L + 1;
    }
  }

  /** Another sun, moon or storm power is moving this world's sky */
  public record WorldBusy() implements Result {}

  // Player UUID -> power -> when it can be used again
  private final Map<UUID, Map<HolyPower, Long>> nextUseTimes = new HashMap<>();
  // World UUID -> until when a sun, moon or storm power is changing that world's time or weather
  private final Map<UUID, Long> worldBusyUntil = new HashMap<>();

  /** Uses the power at {@code now} if it may be used, and starts its cooldown */
  public Result use(UUID playerId, UUID worldId, HolyPower power, int strength, long now) {
    Map<HolyPower, Long> playerTimes =
        this.nextUseTimes.computeIfAbsent(playerId, id -> new HashMap<>());
    long nextUse = playerTimes.getOrDefault(power, 0L);
    if (now < nextUse) {
      return new Cooling(nextUse - now);
    }

    boolean changesWorld = HolyPowerRules.changesWorld(power);
    if (changesWorld && now < this.worldBusyUntil.getOrDefault(worldId, 0L)) {
      return new WorldBusy();
    }

    playerTimes.put(power, now + HolyPowerRules.cooldownMillis(power, strength));
    if (changesWorld) {
      this.worldBusyUntil.put(worldId, now + HolyPowerRules.worldBusyMillis(strength));
    }
    return new Ready();
  }
}
