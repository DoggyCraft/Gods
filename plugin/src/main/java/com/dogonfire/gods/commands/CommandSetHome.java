package com.dogonfire.gods.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.message.Messages;

public class CommandSetHome extends GodsCommand
{
	CommandSetHome(GodsContext ctx)
	{
		super(ctx, "sethome");
		this.permission = "gods.sethome";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!hasPermission(sender))
		{
			send(sender, NO_PERMISSION);
			return;
		}
		if (sender instanceof Player == false)
		{
			send(sender, PLAYER_ONLY);
			return;
		}
		Player player = (Player) sender;
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		if (godName == null)
		{
			send(sender, "command.not-believer");
			return;
		}
		if ((ctx.settings().priests().onlyPriestsCanSetHome()) && (!ctx.priestService().isPriest(player.getUniqueId())))
		{
			send(sender, "command.sethome.not-priest");
			return;
		}
		if (ctx.settings().holyLand().enabled())
		{
			if (ctx.holyLandService().isNeutralLandLocation(player.getLocation()))
			{
				send(sender, "command.sethome.not-holy-land");
				return;
			}
			String locationGod = ctx.holyLandService().getGodAtHolyLandLocation(player.getLocation());
			if ((locationGod == null) || (!locationGod.equals(godName)))
			{
				send(sender, "command.sethome.not-holy-land");
				return;
			}
		}
		ctx.godService().setHomeForGod(godName, player.getLocation());
		ctx.godSpeech().sayToBelievers(godName, Voice.BELIEVERS_SET_HOME, 2, Messages.player(player.getName()));
	}
}
