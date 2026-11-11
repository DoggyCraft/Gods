package com.dogonfire.gods.command;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

/** What {@code /g} alone shows: the plugin, how many gods and believers there are, and the player's own religion */
public final class Overview implements Consumer<CommandSender>
{
	private final GodsContext	ctx;

	public Overview(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	@Override
	public void accept(CommandSender sender)
	{
		Messages messages = this.ctx.messages();
		messages.send(sender, "command.gods.header", Messages.text("plugin", this.ctx.plugin().getDescription().getFullName()), Messages.amount(this.ctx.godService().getAllGods().size()), Messages.text("believers", this.ctx.believerService().getBelieverCount()));
		if (sender instanceof Player player)
		{
			UUID playerId = player.getUniqueId();
			String godName = this.ctx.believerService().getGodForBeliever(playerId);
			if (godName != null)
			{
				List<UUID> priests = this.ctx.priestService().getPriestsForGod(godName);
				boolean priest = priests != null && priests.contains(playerId);
				messages.send(sender, priest ? "command.gods.priest" : "command.gods.believer", Messages.god(godName));
				if (this.ctx.settings().prayers().enabled())
				{
					messages.send(sender, "command.gods.prayer-power", Messages.amount(this.ctx.believerService().getPrayerPower(playerId)));
				}
			}
			else
			{
				messages.send(sender, "command.gods.no-god");
			}
			if (this.ctx.settings().marriage().enabled())
			{
				String partnerName = this.ctx.marriageService().getPartnerName(playerId);
				if (partnerName != null)
				{
					messages.send(sender, "command.gods.married", Messages.player(partnerName));
				}
			}
			messages.send(sender, "command.gods.separator");
			this.ctx.godSpeech().sendInfo(playerId, "help.commands-hint", 80);
			this.ctx.godSpeech().sendInfo(playerId, "help.altar-hint", 160);
			if (this.ctx.settings().prayers().enabled())
			{
				this.ctx.godSpeech().sendInfo(playerId, "help.prayfor-hint", 240);
			}
		}
		messages.send(sender, "command.gods.footer");
		this.ctx.plugin().log(sender.getName() + " /gods");
	}
}
