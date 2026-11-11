package com.dogonfire.gods.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.message.Voice;

/** /g invite &lt;player&gt;: the priest's god invites an online player to the religion */
public final class InviteCommand extends SubCommand
{
	public InviteCommand(GodsContext ctx)
	{
		super(ctx, "invite", "gods.priest.invite");
		priestOnly("command.invite.not-priest");
		believerOnly("command.invite.no-god");
		args(1, ANY);
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		String godName = believersGod((Player) sender);
		String playerName = args.getFirst();
		Player invited = onlinePlayer(playerName, "command.invite.no-player", Messages.player(playerName));
		if (godName.equals(godOf(invited)))
		{
			throw new CommandException("command.invite.already", Messages.player(playerName), Messages.god(godName));
		}
		this.ctx.believerService().setInvitation(invited.getUniqueId(), godName);
		this.ctx.plugin().log(godName + " invited to " + invited.getName() + " to join the religion");
		this.ctx.godSpeech().say(godName, invited, Voice.PLAYER_INVITE, 10, Messages.player(invited.getName()));
		this.ctx.godSpeech().sendInfo(invited.getUniqueId(), "god.question-hint", 40);
		send(sender, "command.invite.done", Messages.player(playerName), Messages.god(godName));
	}

	@Override
	public List<String> tabComplete(CommandSender sender, List<String> args)
	{
		return args.size() == 1 ? onlinePlayerNames(sender) : List.of();
	}
}
