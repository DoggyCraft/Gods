package com.dogonfire.gods.command;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.model.DevotionLevel;
import com.dogonfire.gods.service.DevotionRules;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /g devotion [player]: a believer's devotion to their god (the player's own by default): their
 * level and points and, for their own, the next level, whether it fades, what earns points and what
 * each level unlocks
 */
public final class DevotionCommand extends SubCommand {
  public DevotionCommand(GodsContext ctx) {
    super(ctx, "devotion", "gods.devotion");
    args(0, 1);
  }

  @Override
  public String disabledMessage() {
    return this.ctx.settings().devotion().enabled() ? null : "command.disabled.devotion";
  }

  @Override
  public void execute(CommandSender sender, List<String> args)
      throws CommandException, UsageException {
    UUID believerId;
    String name;
    if (args.isEmpty()) {
      if (!(sender instanceof Player player)) {
        throw new UsageException();
      }
      believerId = player.getUniqueId();
      name = player.getName();
      if (godOf(player) == null) {
        throw new CommandException("command.devotion.no-god");
      }
    } else {
      name = args.getFirst();
      Player online = this.ctx.plugin().getServer().getPlayerExact(name);
      String ownGod = sender instanceof Player player ? godOf(player) : null;
      believerId =
          online != null
              ? online.getUniqueId()
              : (ownGod != null ? knownBeliever(ownGod, name) : null);
      if (believerId == null) {
        throw new CommandException("command.devotion.no-player", Messages.player(name));
      }
      name = online != null ? online.getName() : nameOf(believerId);
      if (this.ctx.believerService().getGodForBeliever(believerId) == null) {
        throw new CommandException("command.devotion.not-believer", Messages.player(name));
      }
    }

    String godName = this.ctx.believerService().getGodForBeliever(believerId);
    DevotionLevel level = this.ctx.devotionService().getLevel(believerId);
    int points = this.ctx.devotionService().getPoints(believerId);
    send(sender, "command.devotion.header", Messages.player(name));
    send(
        sender,
        "command.devotion.level",
        Messages.player(name),
        Messages.god(godName),
        Messages.text("level", this.ctx.devotionService().levelName(godName, level)),
        Messages.amount(points));
    if (!(sender instanceof Player player) || !player.getUniqueId().equals(believerId)) {
      return;
    }

    Settings.Devotion devotion = this.ctx.settings().devotion();
    DevotionLevel next = level.next();
    if (next == null) {
      send(sender, "command.devotion.top");
    } else {
      send(
          sender,
          "command.devotion.next",
          Messages.text("level", this.ctx.devotionService().levelName(godName, next)),
          Messages.text("needed", DevotionRules.pointsToGo(devotion, next, points)));
    }
    Instant lastPrayer =
        this.ctx.believerService().hasUnreadableLastPrayer(believerId)
            ? null
            : this.ctx.believerService().getLastPrayerTime(believerId).toInstant();
    long days =
        lastPrayer != null && lastPrayer.toEpochMilli() > 0
            ? Duration.between(lastPrayer, Instant.now()).toDays()
            : 0;
    if (devotion.fadePercentPerDay() > 0 && points > 0 && days >= devotion.fadeAfterDays()) {
      send(sender, "command.devotion.fading", Messages.text("days", days));
    }

    Settings.DevotionPoints earn = devotion.points();
    send(
        sender,
        "command.devotion.earn",
        Messages.text("prayer", earn.prayer()),
        Messages.text("sacrifice", earn.sacrifice()),
        Messages.text("quest", earn.quest()),
        Messages.text("convert", earn.convert()),
        Messages.text("holy-food", earn.holyFood()),
        Messages.text("unholy-mob", earn.unholyMob()));

    send(sender, "command.devotion.unlocks-header");
    Settings.DevotionUnlocks unlocks = devotion.unlocks();
    if (this.ctx.settings().prayers().enabled()) {
      unlock(
          sender,
          godName,
          level,
          unlocks.prayForDiscount(),
          "prayfor-discount",
          Messages.text("percent", devotion.prayForDiscountPercent()));
    }
    if (this.ctx.settings().holyArtifacts().enabled()) {
      unlock(sender, godName, level, unlocks.holyArtifacts(), "holy-artifacts");
    }
    unlock(
        sender,
        godName,
        level,
        unlocks.moreBlessings(),
        "more-blessings",
        Messages.text("times", devotion.blessingChanceMultiplier()));
    unlock(sender, godName, level, unlocks.praise(), "praise");
    unlock(sender, godName, level, unlocks.title(), "title");
    unlock(sender, godName, level, unlocks.priesthood(), "priesthood");
  }

  /**
   * One unlock: what it gives (command.devotion.unlock.<key>) and the level it needs, met or not
   */
  private void unlock(
      CommandSender sender,
      String godName,
      DevotionLevel level,
      DevotionLevel needed,
      String key,
      TagResolver... tags) {
    TagResolver what =
        Messages.component(
            "unlock", this.ctx.messages().component("command.devotion.unlock." + key, tags));
    send(
        sender,
        DevotionRules.unlocked(level, needed) ? "command.devotion.met" : "command.devotion.unmet",
        what,
        Messages.text("level", this.ctx.devotionService().levelName(godName, needed)));
  }

  @Override
  public List<String> tabComplete(CommandSender sender, List<String> args) {
    return args.size() == 1 ? onlinePlayerNames(sender) : List.of();
  }
}
