package com.dogonfire.gods.command;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /g divorce: ends the player's marriage */
public final class DivorceCommand extends SubCommand {
  public DivorceCommand(GodsContext ctx) {
    super(ctx, "divorce", "gods.divorce");
    believerOnly();
  }

  @Override
  public String disabledMessage() {
    return this.ctx.settings().marriage().enabled() ? null : "command.disabled.marriage";
  }

  @Override
  public void execute(CommandSender sender, List<String> args) throws CommandException {
    Player player = (Player) sender;
    String partnerName = this.ctx.marriageService().getPartnerName(player.getUniqueId());
    if (partnerName == null) {
      throw new CommandException("command.not-married");
    }
    this.ctx.marriageService().divorce(player.getUniqueId());
    send(player, "command.divorce.done", Messages.player(partnerName));
  }
}
