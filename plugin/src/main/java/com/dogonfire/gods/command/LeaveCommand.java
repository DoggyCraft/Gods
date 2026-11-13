package com.dogonfire.gods.command;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /g leave: leaves the player's religion */
public final class LeaveCommand extends SubCommand {
  public LeaveCommand(GodsContext ctx) {
    super(ctx, "leave", "gods.leave");
    playerOnly();
  }

  @Override
  public void execute(CommandSender sender, List<String> args) throws CommandException {
    Player player = (Player) sender;
    String godName = godOf(player);
    if (!this.ctx.prayerService().believerLeaveGod(player.getUniqueId())) {
      throw new CommandException("command.leave.no-god");
    }
    send(sender, "command.leave.done", Messages.god(godName));
  }
}
