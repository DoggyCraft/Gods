package com.dogonfire.gods.command;

import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.model.GodType;

/** /g help [altar/blocks]: the commands the sender can use, how to build an altar, or the altar blocks of each god type */
public final class HelpCommand extends SubCommand
{
	private final CommandRouter	router;

	public HelpCommand(GodsContext ctx, CommandRouter router)
	{
		super(ctx, "help", "gods.help");
		this.router = router;
	}

	@Override
	public void execute(CommandSender sender, List<String> args)
	{
		String topic = args.isEmpty() ? "" : args.getFirst();
		if (topic.equalsIgnoreCase("altar"))
		{
			send(sender, "command.help.altar");
		}
		else if (topic.equalsIgnoreCase("blocks"))
		{
			helpBlocks(sender);
		}
		else
		{
			send(sender, "command.help.header", Messages.text("plugin", this.ctx.plugin().getDescription().getFullName()));
			for (SubCommand command : this.router.commandsFor(sender))
			{
				send(sender, "command.help.line", command.usage(this.ctx.messages()), command.description(this.ctx.messages()));
			}
		}
	}

	private void helpBlocks(CommandSender sender)
	{
		send(sender, "command.help.blocks-header");
		for (GodType godType : GodType.values())
		{
			List<Material> blocks = this.ctx.altarService().getAltarBlocks(godType);
			if (!blocks.isEmpty())
			{
				List<Component> names = blocks.stream().map(block -> this.ctx.messages().itemName(block)).toList();
				send(sender, "command.help.block-type", Messages.component("blocks", Component.join(JoinConfiguration.commas(true), names)), Messages.color("type_color", this.ctx.godService().getColorForGodType(godType)), Messages.component("god_type", this.ctx.messages().godTypeName(godType, GodGender.None)));
			}
		}
		send(sender, "command.help.blocks-footer");
	}

	@Override
	public List<String> tabComplete(CommandSender sender, List<String> args)
	{
		return args.size() == 1 ? List.of("altar", "blocks") : List.of();
	}
}
