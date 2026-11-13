package com.dogonfire.gods.command;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /g check &lt;player&gt;: shows the religion of an online player */
public final class CheckCommand extends SubCommand {
  public CheckCommand(GodsContext ctx) {
    super(ctx, "check", "gods.check");
    args(1, ANY);
  }

  @Override
  public void execute(CommandSender sender, List<String> args) throws CommandException {
    Player believer = onlinePlayer(args.getFirst(), "command.check.no-player");
    String godName = godOf(believer);
    if (godName == null) {
      send(sender, "command.check.no-god", Messages.displayName(believer.getDisplayName()));
    } else if (this.ctx.priestService().isPriest(believer.getUniqueId())) {
      send(
          sender,
          "command.check.priest",
          Messages.displayName(believer.getDisplayName()),
          Messages.god(godName));
    } else {
      send(
          sender,
          "command.check.believer",
          Messages.displayName(believer.getDisplayName()),
          Messages.god(godName));
    }
  }

  @Override
  public List<String> tabComplete(CommandSender sender, List<String> args) {
    return args.size() == 1 ? onlinePlayerNames(sender) : List.of();
  }
}
