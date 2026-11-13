package com.dogonfire.gods.command;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import java.util.List;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /g love: shows the player's love to their partner, who must be online */
public final class LoveCommand extends SubCommand {
  public LoveCommand(GodsContext ctx) {
    super(ctx, "love", "gods.love");
    believerOnly();
  }

  @Override
  public String disabledMessage() {
    return this.ctx.settings().marriage().enabled() ? null : "command.disabled.marriage";
  }

  @Override
  public void execute(CommandSender sender, List<String> args) throws CommandException {
    Player player = (Player) sender;
    UUID partnerId = this.ctx.marriageService().getPartnerId(player.getUniqueId());
    if (partnerId == null) {
      throw new CommandException("command.not-married");
    }
    Player partner = this.ctx.plugin().getServer().getPlayer(partnerId);
    if (partner == null) {
      throw new CommandException("command.love.offline", Messages.player(nameOf(partnerId)));
    }
    this.ctx.marriageService().love(player.getUniqueId());
    send(player, "command.love.done", Messages.displayName(partner.getDisplayName()));
  }
}
