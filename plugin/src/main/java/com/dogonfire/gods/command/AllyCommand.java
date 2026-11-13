package com.dogonfire.gods.command;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /g ally &lt;god&gt;: starts or ends an alliance of the priest's god with another god */
public final class AllyCommand extends SubCommand {
  public AllyCommand(GodsContext ctx) {
    super(ctx, "ally", "gods.priest.alliance");
    priestOnly("command.ally.not-priest");
    args(1, 1);
  }

  @Override
  public void execute(CommandSender sender, List<String> args) throws CommandException {
    String godName = believersGod((Player) sender);
    String typed = args.getFirst();
    if (!this.ctx.godService().godExist(typed)) {
      throw new CommandException("command.no-such-god", Messages.god(typed));
    }
    String allyGodName = this.ctx.godService().formatGodName(typed);
    if (allyGodName.equals(godName)) {
      throw new CommandException("command.ally.own-god");
    }
    if (this.ctx.relationService().getWarRelations(godName).contains(allyGodName)) {
      throw new CommandException("command.ally.at-war", Messages.god(typed));
    }
    boolean allied = !this.ctx.relationService().hasAllianceRelation(godName, allyGodName);
    if (allied) {
      // Ending an alliance is always possible
      requireRank(godName, this.ctx.settings().ranks().unlocks().relations());
    }
    if (!this.ctx.relationService().setAlliance(godName, allyGodName, allied)) {
      throw new CommandException("command.cancelled");
    }
  }

  @Override
  public List<String> tabComplete(CommandSender sender, List<String> args) {
    return args.size() == 1 ? godNames() : List.of();
  }
}
