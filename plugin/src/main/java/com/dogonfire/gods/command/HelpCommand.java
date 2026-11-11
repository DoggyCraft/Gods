package com.dogonfire.gods.command;

import java.util.List;

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
			List<String> materials = this.ctx.altarService().getAltarBlockTypesFromGodType(godType);
			if (materials != null)
			{
				for (String blockMaterial : materials)
				{
					send(sender, "command.help.block", Messages.text("block", blockMaterial), Messages.color("type_color", this.ctx.godService().getColorForGodType(godType)), Messages.component("god_type", this.ctx.messages().godTypeName(godType, GodGender.None)));
				}
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
