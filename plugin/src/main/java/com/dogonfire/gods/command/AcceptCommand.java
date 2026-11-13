package com.dogonfire.gods.command;

import com.dogonfire.gods.GodsContext;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /g yes: accepts what the player's god asked them */
public final class AcceptCommand extends SubCommand {
  public AcceptCommand(GodsContext ctx) {
    super(ctx, "yes", "gods.accept");
    // A player without a god answers invitations too
    playerOnly();
  }

  @Override
  public void execute(CommandSender sender, List<String> args) {
    this.ctx.prayerService().believerAccept(((Player) sender).getUniqueId());
  }
}
