package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

public class CommandGiveHolyArtifact extends GodsCommand
{
	CommandGiveHolyArtifact(GodsContext ctx)
	{
		super(ctx, "holyartifact");
		this.permission = "gods.holyartifact";
		this.description = "Let your God give you a Holy Artifact";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!ctx.settings().holyArtifacts().enabled())
		{
			sender.sendMessage(ChatColor.RED + "Holy Artifacts are not enabled on this server");
			return;
		}
		if (!hasPermission(sender))
		{
			sender.sendMessage(stringNoPermission);
			return;
		}
		if (sender instanceof Player == false)
		{
			sender.sendMessage(stringPlayerOnly);
			return;
		}
		Player player = (Player) sender;
		String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		if (godName == null)
		{
			sender.sendMessage(stringNoGod);
			return;
		}
		if (ctx.holyArtifactManager().hasHolyArtifact(player))
		{
			sender.sendMessage(ChatColor.RED + "You already carry a Holy Artifact");
			return;
		}
		ctx.blessingService().blessPlayerWithHolyArtifact(godName, player);
		sender.sendMessage(ChatColor.GOLD + godName + ChatColor.AQUA + " gave " + player.getName() + ChatColor.AQUA + " a Holy artifact!");
	}
}
