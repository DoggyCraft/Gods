package com.dogonfire.gods.command;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.service.HolyLandNames;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /g landname &lt;name&gt;: a priest names the holy land of their god they stand in (chunk land or
 * claim land; {@code HolyLandService.renameLand})
 */
public final class LandNameCommand extends SubCommand {
  public LandNameCommand(GodsContext ctx) {
    super(ctx, "landname", "gods.priest.landname");
    priestOnly("command.landname.not-priest");
    args(1, ANY);
  }

  @Override
  public String disabledMessage() {
    return this.ctx.settings().holyLand().enabled() ? null : "command.disabled.holy-land";
  }

  @Override
  public void execute(CommandSender sender, List<String> args) throws CommandException {
    Player player = (Player) sender;
    String godName = believersGod(player);
    String name = HolyLandNames.validName(String.join(" ", args));
    if (name == null) {
      throw new CommandException(
          "command.landname.invalid", Messages.text("max", HolyLandNames.MAX_LENGTH));
    }
    String old =
        this.ctx
            .holyLandService()
            .renameLand(player.getLocation(), godName, name, player.getName());
    if (old == null) {
      throw new CommandException("command.landname.not-own-land", Messages.god(godName));
    }
    send(
        sender,
        "command.landname.done",
        Messages.god(godName),
        Messages.text("name", name),
        Messages.text("old", old));
  }

  /** The name of the land the priest stands in, to change it a little */
  @Override
  public List<String> tabComplete(CommandSender sender, List<String> args) {
    if (args.size() == 1
        && sender instanceof Player player
        && this.ctx.settings().holyLand().enabled()) {
      String current = this.ctx.holyLandService().getHolyLandName(player.getLocation());
      String godName = godOf(player);
      if (current != null
          && godName != null
          && godName.equals(
              this.ctx.holyLandService().getGodAtHolyLandLocation(player.getLocation()))) {
        return List.of(current);
      }
    }
    return List.of();
  }
}
