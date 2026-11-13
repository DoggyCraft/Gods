package com.dogonfire.gods.task;

import com.dogonfire.gods.GodsContext;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;

/** Sends a god's words (already rendered by GodSpeech) to a player, if still online */
public class GodSpeakTask extends Task {
  private final UUID playerId;
  private final List<String> lines;

  public GodSpeakTask(GodsContext ctx, UUID playerId, List<String> lines) {
    super(ctx);
    this.playerId = playerId;
    this.lines = List.copyOf(lines);
  }

  @Override
  public void run() {
    Player player = getPlugin().getServer().getPlayer(this.playerId);
    if (player == null) {
      return;
    }
    for (String line : this.lines) {
      player.sendMessage(line);
    }
  }
}
