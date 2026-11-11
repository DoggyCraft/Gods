package com.dogonfire.gods.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.message.Voice;

/** /g marry &lt;player&gt;: proposes to an online believer of the same god */
public final class MarryCommand extends SubCommand
{
	public MarryCommand(GodsContext ctx)
	{
		super(ctx, "marry", "gods.marry");
		believerOnly();
		args(1, ANY);
	}

	@Override
	public String disabledMessage()
	{
		return this.ctx.settings().marriage().enabled() ? null : "command.disabled.marriage";
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		Player player = (Player) sender;
		String godName = believersGod(player);
		String otherPlayerName = args.getFirst();
		Player otherPlayer = onlinePlayer(otherPlayerName, "command.marry.no-player", Messages.player(otherPlayerName));
		if (otherPlayer.getUniqueId().equals(player.getUniqueId()))
		{
			throw new CommandException("command.marry.yourself");
		}
		String otherGodName = godOf(otherPlayer);
		if (otherGodName == null)
		{
			throw new CommandException("command.marry.no-god", Messages.player(otherPlayerName));
		}
		if (!godName.equals(otherGodName))
		{
			throw new CommandException("command.marry.other-god", Messages.player(otherPlayerName), Messages.god(godName));
		}
		String partnerName = this.ctx.marriageService().getPartnerName(otherPlayer.getUniqueId());
		if (partnerName != null)
		{
			throw new CommandException("command.marry.other-married", Messages.player(otherPlayerName), Messages.text("partner", partnerName));
		}
		partnerName = this.ctx.marriageService().getPartnerName(player.getUniqueId());
		if (partnerName != null)
		{
			throw new CommandException("command.marry.married", Messages.text("partner", partnerName));
		}

		this.ctx.marriageService().proposeMarriage(player.getUniqueId(), otherPlayer.getUniqueId());
		this.ctx.godSpeech().sayWithQuestion(godName, otherPlayer, Voice.BELIEVER_MARRIAGE_PROPOSAL, 1, Messages.player(player.getName()));
		send(player, "command.marry.proposed", Messages.player(otherPlayerName), Messages.god(godName));
	}

	@Override
	public List<String> tabComplete(CommandSender sender, List<String> args)
	{
		return args.size() == 1 ? onlinePlayerNames(sender) : List.of();
	}
}
