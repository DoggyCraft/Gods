package com.dogonfire.gods.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.message.Voice;

/** /g sethome: sets the home of the player's religion where they stand (inside its holy land, if that is on) */
public final class SetHomeCommand extends SubCommand
{
	public SetHomeCommand(GodsContext ctx)
	{
		super(ctx, "sethome", "gods.sethome");
		believerOnly();
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		Player player = (Player) sender;
		String godName = believersGod(player);
		if (this.ctx.settings().priests().onlyPriestsCanSetHome() && !this.ctx.priestService().isPriest(player.getUniqueId()))
		{
			throw new CommandException("command.sethome.not-priest");
		}
		if (this.ctx.settings().holyLand().enabled())
		{
			if (this.ctx.holyLandService().isNeutralLandLocation(player.getLocation()))
			{
				throw new CommandException("command.sethome.not-holy-land");
			}
			String locationGod = this.ctx.holyLandService().getGodAtHolyLandLocation(player.getLocation());
			if (locationGod == null || !locationGod.equals(godName))
			{
				throw new CommandException("command.sethome.not-holy-land");
			}
		}
		this.ctx.godService().setHomeForGod(godName, player.getLocation());
		this.ctx.godSpeech().sayToBelievers(godName, Voice.BELIEVERS_SET_HOME, 2, Messages.player(player.getName()));
	}
}
