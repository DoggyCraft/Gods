package com.dogonfire.gods.command;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.model.GodType;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

/**
 * /g help [page|altar|blocks]: a page of the commands the sender can use ({@link
 * HelpPages#PER_PAGE} a page), how to build an altar, or the altar blocks of each god type
 */
public final class HelpCommand extends SubCommand {
  private final CommandRouter router;

  public HelpCommand(GodsContext ctx, CommandRouter router) {
    super(ctx, "help", "gods.help");
    this.router = router;
  }

  @Override
  public void execute(CommandSender sender, List<String> args) throws CommandException {
    String topic = args.isEmpty() ? "" : args.getFirst();
    if (topic.equalsIgnoreCase("altar")) {
      send(sender, "command.help.altar");
    } else if (topic.equalsIgnoreCase("blocks")) {
      helpBlocks(sender);
    } else {
      // A number is a page; anything else shows the first page, as /g help always did
      int page = Math.max(1, HelpPages.parsePage(topic));
      List<SubCommand> commands = this.router.commandsFor(sender);
      int pages = HelpPages.pageCount(commands.size(), HelpPages.PER_PAGE);
      if (page > pages) {
        throw new CommandException(
            "command.help.no-page", Messages.amount(page), Messages.text("pages", pages));
      }
      send(
          sender,
          "command.help.header",
          Messages.text("plugin", this.ctx.plugin().getDescription().getFullName()),
          Messages.text("page", page),
          Messages.text("pages", pages));
      for (SubCommand command : HelpPages.page(commands, page, HelpPages.PER_PAGE)) {
        send(
            sender,
            "command.help.line",
            command.usage(this.ctx.messages()),
            command.description(this.ctx.messages()));
      }
      if (page < pages) {
        send(
            sender,
            "command.help.next-page",
            Messages.text("next", page + 1),
            Messages.text("pages", pages));
      }
    }
  }

  private void helpBlocks(CommandSender sender) {
    send(sender, "command.help.blocks-header");
    for (GodType godType : GodType.values()) {
      List<Material> blocks = this.ctx.altarService().getAltarBlocks(godType);
      if (!blocks.isEmpty()) {
        List<Component> names =
            blocks.stream().map(block -> this.ctx.messages().itemName(block)).toList();
        send(
            sender,
            "command.help.block-type",
            Messages.component("blocks", Component.join(JoinConfiguration.commas(true), names)),
            Messages.color("type_color", this.ctx.godService().getColorForGodType(godType)),
            Messages.component(
                "god_type", this.ctx.messages().godTypeName(godType, GodGender.None)));
      }
    }
    send(sender, "command.help.blocks-footer");
  }

  @Override
  public List<String> tabComplete(CommandSender sender, List<String> args) {
    if (args.size() != 1) {
      return List.of();
    }
    List<String> options = new java.util.ArrayList<>(List.of("altar", "blocks"));
    options.addAll(
        HelpPages.pageNumbers(
            HelpPages.pageCount(this.router.commandsFor(sender).size(), HelpPages.PER_PAGE)));
    return options;
  }
}
