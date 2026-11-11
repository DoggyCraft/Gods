package com.dogonfire.gods.commands;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.model.GodType;

public class CommandHelp extends GodsCommand
{
	private final GodsCommandExecuter	executer;

	CommandHelp(GodsContext ctx, GodsCommandExecuter executer)
	{
		super(ctx, "help");
		this.executer = executer;
		this.permission = "gods.help";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!hasPermission(sender))
		{
			send(sender, NO_PERMISSION);
			return;
		}
		if (args.length < 2)
		{
			helpGeneral(sender);
			return;
		}
		if (args[1].equalsIgnoreCase("Altar"))
		{
			helpAltar(sender);
			return;
		}
		if (args[1].equalsIgnoreCase("blocks"))
		{
			helpBlocks(sender);
			return;
		}
		helpGeneral(sender);
	}

	private void helpGeneral(CommandSender sender)
	{
		boolean isPriest = (sender instanceof Player) && ctx.priestService().isPriest(((Player) sender).getUniqueId());

		send(sender, "command.help.header", Messages.text("plugin", ctx.plugin().getDescription().getFullName()));

		for (GodsCommand localCommand : this.executer.getCommands())
		{
			if (!localCommand.isAvailableTo(sender, isPriest))
			{
				continue;
			}

			send(sender, "command.help.line", localCommand.usage(), Messages.component("description", localCommand.description()));
		}
	}

	private void helpAltar(CommandSender sender)
	{
		send(sender, "command.help.altar");
	}

	private void helpBlocks(CommandSender sender)
	{
		send(sender, "command.help.blocks-header");
		
		for (GodType godType : GodType.values())
		{
			List<String> materials = ctx.altarService().getAltarBlockTypesFromGodType(godType);
			if (materials != null)
			{
				for (String blockMaterial : materials)
				{
					send(sender, "command.help.block", Messages.text("block", blockMaterial), Messages.color("type_color", ctx.godService().getColorForGodType(godType)), Messages.component("god_type", ctx.messages().godTypeName(godType, GodGender.None)));
				}
			}
		}
		
		send(sender, "command.help.blocks-footer");
	}

}
