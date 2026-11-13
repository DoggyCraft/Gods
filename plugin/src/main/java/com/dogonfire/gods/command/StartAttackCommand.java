package com.dogonfire.gods.command;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.message.Voice;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /g startattack: the priest starts an attack on the enemy holy land they stand in */
public final class StartAttackCommand extends SubCommand {
  public StartAttackCommand(GodsContext ctx) {
    super(ctx, "startattack", "gods.priest.startattack");
    priestOnly();
  }

  @Override
  public String disabledMessage() {
    return this.ctx.settings().holyLand().enabled() ? null : "command.disabled.holy-land";
  }

  @Override
  public void execute(CommandSender sender, List<String> args) throws CommandException {
    Player player = (Player) sender;
    String godName = believersGod(player);
    Location location = player.getLocation();
    if (this.ctx.relationService().isAttackingHolyLand(godName)) {
      throw new CommandException("command.startattack.already");
    }
    String otherGodName = this.ctx.holyLandService().getGodAtHolyLandLocation(location);
    if (otherGodName == null || this.ctx.holyLandService().isNeutralLandLocation(location)) {
      throw new CommandException("command.startattack.not-holy-land");
    }
    if (otherGodName.equals(godName)) {
      throw new CommandException("command.startattack.own-land");
    }
    if (!this.ctx.relationService().hasWarRelation(godName, otherGodName)) {
      throw new CommandException("command.startattack.not-at-war", Messages.god(otherGodName));
    }
    if (this.ctx.holyLandService().isUnderAttack(location)) {
      throw new CommandException("command.startattack.under-attack");
    }
    if (!this.ctx.relationService().setContestedHolyLandForGod(godName, location)) {
      throw new CommandException("command.cancelled");
    }
    send(sender, "command.startattack.done", Messages.god(otherGodName));
    this.ctx.godSpeech().sendInfoToBelievers(godName, "holy-land.attacking-hint");
    this.ctx
        .godSpeech()
        .sayToBelievers(godName, Voice.BELIEVERS_ATTACK_STARTED, 40, Messages.player(otherGodName));
  }
}
