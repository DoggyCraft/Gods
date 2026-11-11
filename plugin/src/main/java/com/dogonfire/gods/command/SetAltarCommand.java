package com.dogonfire.gods.command;

import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.model.Altar;
import com.dogonfire.gods.service.AltarRegistry;
import com.dogonfire.gods.service.MainAltarRules;

/**
 * /g setaltar: the priest makes the praying altar they look at (its sign, altar block or light, within
 * {@link MainAltarRules#REACH_BLOCKS}) their god's main altar
 */
public final class SetAltarCommand extends SubCommand
{
	public SetAltarCommand(GodsContext ctx)
	{
		super(ctx, "setaltar", "gods.priest.setaltar");
		priestOnly("command.setaltar.not-priest");
		args(0, 0);
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		Player player = (Player) sender;
		String godName = believersGod(player);
		Block looked = player.getTargetBlockExact(MainAltarRules.REACH_BLOCKS);
		if (looked == null)
		{
			throw new CommandException("command.setaltar.no-altar");
		}

		AltarRegistry registry = this.ctx.altarRegistry();
		MainAltarRules.Target target = MainAltarRules.choose(MainAltarRules.candidates(AltarRegistry.pos(looked), registry::atSign, registry::onBlock), godName);
		switch (target.verdict())
		{
			case NO_ALTAR -> throw new CommandException("command.setaltar.no-altar");
			case NOT_PRAYING -> throw new CommandException("command.setaltar.not-praying");
			case OTHER_GOD -> throw new CommandException("command.setaltar.other-god", Messages.god(godName));
			case OK ->
			{
			}
		}

		Altar altar = target.altar();
		if (registry.isMainAltar(altar))
		{
			throw new CommandException("command.setaltar.already", Messages.god(godName));
		}
		registry.setMainAltar(altar);
		send(sender, "command.setaltar.done", Messages.god(godName), Messages.text("world", altar.world()), Messages.text("x", altar.block().x()),
				Messages.text("y", altar.block().y()), Messages.text("z", altar.block().z()));
	}
}
