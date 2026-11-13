package com.dogonfire.gods.integration.npc;

import com.dogonfire.gods.config.Settings;

/**
 * Which provider makes the gods' NPCs (pure, tested): config.yml's {@code npc.provider} against
 * what the server has. {@code auto} takes Citizens if it is installed, else FancyNpcs if it is
 * installed, else the built-in mannequin where the server has mannequins (Minecraft 1.21.9+), else
 * none. A provider named outright that the server lacks gives none, with a warning: the admin asked
 * for that one.
 */
public final class NpcProviders {
  /** Plugin names as Bukkit knows them */
  public static final String CITIZENS = "Citizens";

  public static final String FANCY_NPCS = "FancyNpcs";

  private NpcProviders() {}

  /**
   * The provider to use, never {@code AUTO}, and a warning for the log when it isn't what
   * config.yml asked for (null when it is, or when auto found none, which isn't worth a warning)
   */
  public record Choice(Settings.NpcProvider provider, String warning) {}

  /**
   * @param enabled {@code npc.enabled}
   * @param wanted {@code npc.provider}
   * @param citizens whether Citizens is installed and enabled
   * @param fancyNpcs whether FancyNpcs is installed and enabled
   * @param mannequin whether the server has the mannequin entity
   */
  public static Choice choose(
      boolean enabled,
      Settings.NpcProvider wanted,
      boolean citizens,
      boolean fancyNpcs,
      boolean mannequin) {
    if (!enabled || wanted == Settings.NpcProvider.OFF) {
      return new Choice(Settings.NpcProvider.OFF, null);
    }
    return switch (wanted) {
      case AUTO ->
          new Choice(
              citizens
                  ? Settings.NpcProvider.CITIZENS
                  : fancyNpcs
                      ? Settings.NpcProvider.FANCYNPCS
                      : mannequin ? Settings.NpcProvider.BUILTIN : Settings.NpcProvider.OFF,
              null);
      case BUILTIN ->
          mannequin
              ? new Choice(wanted, null)
              : missing(
                  "npc.provider is builtin, but this server has no mannequins (Minecraft 1.21.9 and newer have them)");
      case CITIZENS ->
          citizens
              ? new Choice(wanted, null)
              : missing("npc.provider is citizens, but Citizens is not installed");
      case FANCYNPCS ->
          fancyNpcs
              ? new Choice(wanted, null)
              : missing("npc.provider is fancynpcs, but FancyNpcs is not installed");
      case OFF -> new Choice(Settings.NpcProvider.OFF, null);
    };
  }

  private static Choice missing(String why) {
    return new Choice(Settings.NpcProvider.OFF, why + "; gods have no NPCs.");
  }
}
