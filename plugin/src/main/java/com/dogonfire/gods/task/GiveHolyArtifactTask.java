package com.dogonfire.gods.task;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.model.GodType;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** A god gives a holy artifact: it appears in front of the player ({@link Task#giftSpot}) */
public class GiveHolyArtifactTask extends Task {
  private Player player = null;
  private String godName = null;
  private GodType godType = null;
  private boolean speak = false;

  public GiveHolyArtifactTask(
      GodsContext ctx, String god, GodType godType, Player p, boolean godspeak) {
    super(ctx);
    this.player = p;
    this.godName = god;
    this.godType = godType;
    this.speak = godspeak;
  }

  @Override
  public void run() {
    if (!this.player.isOnline()) {
      return;
    }
    Location spawnLocation = giftSpot(this.player);

    getPlugin().logDebug("Creating holy artifact for " + this.player.getName());
    if (ctx.holyArtifactService().createHolyArtifact(this.godType, this.godName, spawnLocation)
        == null) {
      return;
    }
    spawnLocation.getWorld().playEffect(spawnLocation, Effect.MOBSPAWNER_FLAMES, 25);

    if (this.speak) {
      ctx.godSpeech()
          .say(
              this.godName,
              this.player,
              Voice.BELIEVER_HOLY_ARTIFACT_BLESSING,
              2 + ThreadLocalRandom.current().nextInt(10),
              Messages.player(this.player.getName()));
    }
  }
}
