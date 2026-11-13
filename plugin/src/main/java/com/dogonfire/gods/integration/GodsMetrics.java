package com.dogonfire.gods.integration;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.model.GodType;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.plugin.PluginManager;

/**
 * Anonymous usage statistics on bStats (https://bstats.org/plugin/bukkit/Gods/35252), with Gods'
 * own charts. Server owners turn bStats off for all plugins in plugins/bStats/config.yml.
 *
 * <p>bStats asks for the chart values on its own thread, where the plugin's data must not be read,
 * so the charts read a {@link Snapshot} that {@link #refresh()} takes on the main thread.
 */
public final class GodsMetrics {
  public static final int BSTATS_ID = 35252;

  /** How often the snapshot is taken, in ticks (5 minutes); bStats sends data every 30 minutes */
  public static final long REFRESH_TICKS = 5 * 60 * 20L;

  /** What the charts show, taken on the main thread */
  private record Snapshot(
      int gods,
      int believers,
      Map<String, Integer> godTypes,
      Map<String, Integer> features,
      String language,
      Map<String, Integer> integrations) {}

  private final GodsContext ctx;
  private final Metrics metrics;
  private volatile Snapshot snapshot;

  public GodsMetrics(GodsContext ctx) {
    this.ctx = ctx;
    refresh();

    this.metrics = new Metrics(ctx.plugin(), BSTATS_ID);
    this.metrics.addCustomChart(new SingleLineChart("gods", () -> this.snapshot.gods()));
    this.metrics.addCustomChart(new SingleLineChart("believers", () -> this.snapshot.believers()));
    this.metrics.addCustomChart(new AdvancedPie("god_types", () -> this.snapshot.godTypes()));
    this.metrics.addCustomChart(new AdvancedPie("features", () -> this.snapshot.features()));
    this.metrics.addCustomChart(new SimplePie("language", () -> this.snapshot.language()));
    this.metrics.addCustomChart(
        new AdvancedPie("integrations", () -> this.snapshot.integrations()));
  }

  /** Main thread: takes a new snapshot of the chart values */
  public void refresh() {
    Settings settings = this.ctx.settings();

    Map<String, Integer> godTypes = new LinkedHashMap<String, Integer>();
    for (String godName : this.ctx.godService().getAllGods()) {
      GodType type = this.ctx.godService().getDivineForceForGod(godName);
      if (type != null) {
        godTypes.merge(type.name().toLowerCase(Locale.ROOT), 1, Integer::sum);
      }
    }

    Map<String, Integer> features = new LinkedHashMap<String, Integer>();
    addIf(features, "Holy land", settings.holyLand().enabled());
    addIf(features, "Quests", settings.quests().enabled());
    addIf(features, "Marriage", settings.marriage().enabled());
    addIf(features, "Bibles", settings.bibles().enabled());
    addIf(features, "Holy artifacts", settings.holyArtifacts().enabled());
    addIf(features, "Blessings", settings.blessings().enabled());
    addIf(features, "Curses", settings.curses().enabled());
    addIf(features, "Sacrifices", settings.sacrifices().enabled());
    addIf(features, "Commandments", settings.commandments().enabled());
    addIf(features, "Chat formatting", settings.chat().formatting());

    PluginManager pluginManager = this.ctx.plugin().getServer().getPluginManager();
    Map<String, Integer> integrations = new LinkedHashMap<String, Integer>();
    addIf(integrations, "WorldGuard", pluginManager.isPluginEnabled("WorldGuard"));
    addIf(integrations, "PlaceholderAPI", pluginManager.isPluginEnabled("PlaceholderAPI"));

    this.snapshot =
        new Snapshot(
            this.ctx.godService().getAllGods().size(),
            this.ctx.believerService().getBelieverCount(),
            Map.copyOf(godTypes),
            Map.copyOf(features),
            settings.general().language(),
            Map.copyOf(integrations));
  }

  private static void addIf(Map<String, Integer> map, String name, boolean present) {
    if (present) {
      map.put(name, 1);
    }
  }

  public void shutdown() {
    this.metrics.shutdown();
  }
}
