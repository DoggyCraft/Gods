package com.dogonfire.gods.command;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.service.HolyClaimRules;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /g holyclaim: a priest makes the GriefPrevention claim they stand in (their own, with an altar of
 * their god in it) the holy land of their god, or releases it when it already is ({@code
 * HolyLandService.holyClaim})
 */
public final class HolyClaimCommand extends SubCommand {
  public HolyClaimCommand(GodsContext ctx) {
    super(ctx, "holyclaim", "gods.priest.holyclaim");
    priestOnly("command.holyclaim.not-priest");
    args(0, 0);
  }

  @Override
  public String disabledMessage() {
    if (!this.ctx.settings().holyLand().enabled()) {
      return "command.disabled.holy-land";
    }
    return this.ctx.griefPrevention() != null ? null : "command.disabled.griefprevention";
  }

  @Override
  public void execute(CommandSender sender, List<String> args) throws CommandException {
    Player player = (Player) sender;
    String godName = believersGod(player);
    HolyClaimRules.Answer answer = this.ctx.holyLandService().holyClaim(player, godName);
    switch (answer) {
      case NO_CLAIM -> throw new CommandException("command.holyclaim.no-claim");
      case SUBCLAIM -> throw new CommandException("command.holyclaim.subclaim");
      case ADMIN_CLAIM -> throw new CommandException("command.holyclaim.admin-claim");
      case NOT_OWNER -> throw new CommandException("command.holyclaim.not-owner");
      case OTHER_GOD ->
          throw new CommandException(
              "command.holyclaim.other-god",
              Messages.god(
                  nullToEmpty(
                      this.ctx.holyLandService().getGodAtHolyLandLocation(player.getLocation()))));
      case NO_ALTAR ->
          throw new CommandException("command.holyclaim.no-altar", Messages.god(godName));
      case AT_LIMIT ->
          throw new CommandException(
              "command.holyclaim.at-limit",
              Messages.text("max", this.ctx.holyLandService().getMaximumHolyLandsForGod(godName)));
      case CANCELLED -> throw new CommandException("command.cancelled");
      case RELEASE -> send(sender, "command.holyclaim.released", Messages.god(godName));
      case CLAIM ->
          send(
              sender,
              "command.holyclaim.done",
              Messages.god(godName),
              Messages.text(
                  "name",
                  nullToEmpty(this.ctx.holyLandService().getHolyLandName(player.getLocation()))),
              Messages.text("claimed", this.ctx.holyLandService().getHolyLandsForGod(godName)),
              Messages.text("max", this.ctx.holyLandService().getMaximumHolyLandsForGod(godName)));
    }
  }

  private static String nullToEmpty(String text) {
    return text != null ? text : "";
  }
}
