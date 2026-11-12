package com.dogonfire.gods.command;

import java.util.List;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.model.Altar;
import com.dogonfire.gods.model.GodMood;
import com.dogonfire.gods.model.GodRank;

/** /g info [god]: everything about a god (the player's own by default) */
public final class InfoCommand extends SubCommand
{
	public InfoCommand(GodsContext ctx)
	{
		super(ctx, "info", "gods.info");
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		String godName = args.size() == 1 ? this.ctx.godService().formatGodName(args.getFirst()) : null;
		if (godName == null && sender instanceof Player player)
		{
			godName = godOf(player);
		}
		if (godName == null)
		{
			throw new CommandException("command.info.no-god");
		}
		if (!this.ctx.godService().godExist(godName))
		{
			throw new CommandException("command.info.unknown-god");
		}

		send(sender, "command.info.header", Messages.god(godName), Messages.color("type_color", this.ctx.godService().getColorForGod(godName)), Messages.legacyText("title", this.ctx.godService().getTitleForGod(godName)));
		send(sender, "command.info.god-description", Messages.text("description", String.valueOf(this.ctx.godService().getGodDescription(godName))));

		GodMood godMood = this.ctx.moodService().getMoodForGod(godName);
		send(sender, "command.info.mood." + godMood.name(), Messages.god(godName), Messages.component("mood", this.ctx.messages().moodName(godMood)));

		Material neededItem = this.ctx.commandmentService().getSacrificeItemTypeForGod(godName);
		if (neededItem != null)
		{
			send(sender, "command.info.sacrifice", Messages.god(godName), Messages.type(this.ctx.messages().itemName(neededItem)));
		}

		List<UUID> priests = this.ctx.priestService().getPriestsForGod(godName);
		if (priests == null || priests.isEmpty())
		{
			send(sender, "command.info.no-priest");
		}
		else if (priests.size() == 1)
		{
			send(sender, "command.info.priest", Messages.player(nameOf(priests.getFirst())));
		}
		else
		{
			send(sender, "command.info.priests");
			for (UUID priest : priests)
			{
				send(sender, "command.info.priests-line", Messages.player(nameOf(priest)));
			}
		}

		rank(sender, godName);
		if (sender instanceof Player player && godName.equals(godOf(player)) && this.ctx.devotionService().enabled())
		{
			// The believer's own devotion (/g devotion shows more)
			send(sender, "command.info.devotion", Messages.text("level", this.ctx.devotionService().getLevelName(player.getUniqueId())),
					Messages.amount(this.ctx.devotionService().getPoints(player.getUniqueId())));
		}

		send(sender, "command.info.believers",Messages.amount(this.ctx.believerService().getBelieversForGod(godName).size()));
		send(sender, "command.info.power", Messages.text("power", String.format(java.util.Locale.ROOT, "%.2f", this.ctx.godService().getGodPower(godName))));
		if (this.ctx.settings().commandments().enabled())
		{
			send(sender, "command.info.holy-food", Messages.type(this.ctx.messages().itemName(this.ctx.commandmentService().getHolyFoodTypeForGod(godName))));
			send(sender, "command.info.unholy-food", Messages.type(this.ctx.messages().itemName(this.ctx.commandmentService().getUnholyFoodTypeForGod(godName))));
			send(sender, "command.info.holy-creature", Messages.type(this.ctx.messages().mobName(this.ctx.commandmentService().getHolyMobTypeForGod(godName))));
			send(sender, "command.info.unholy-creature", Messages.type(this.ctx.messages().mobName(this.ctx.commandmentService().getUnholyMobTypeForGod(godName))));
		}

		List<String> allies = this.ctx.relationService().getAllianceRelations(godName);
		List<String> enemies = this.ctx.relationService().getWarRelations(godName);
		if (!allies.isEmpty() || !enemies.isEmpty())
		{
			send(sender, "command.info.relations");
			for (String ally : allies)
			{
				send(sender, "command.info.alliance", Messages.god(ally));
			}
			for (String enemy : enemies)
			{
				send(sender, "command.info.war", Messages.god(enemy));
			}
		}

		mainAltar(sender, godName);

		if (this.ctx.settings().holyLand().enabled())
		{
			send(sender, "command.info.holy-land", Messages.text("claimed", this.ctx.holyLandService().getHolyLandsForGod(godName)), Messages.text("max", this.ctx.holyLandService().getMaximumHolyLandsForGod(godName)));
		}
	}

	/**
	 * The god's rank; for its believers (and the console) also its next rank and how far it is with each condition, on
	 * one line (/g rank shows them one by one)
	 */
	private void rank(CommandSender sender, String godName)
	{
		send(sender, "command.info.rank", Messages.text("rank", this.ctx.rankService().getRankName(godName)));
		boolean believer = !(sender instanceof Player player) || godName.equals(godOf(player));
		GodRank next = this.ctx.rankService().getRank(godName).next();
		if (!believer || next == null)
		{
			return;
		}
		String conditions = String.join(this.ctx.messages().legacy("command.info.next-rank-separator"),
				RankLines.lines(this.ctx, this.ctx.rankService().progress(godName, next), "command.info.next-rank-met", "command.info.next-rank-unmet"));
		send(sender, "command.info.next-rank", Messages.text("rank", this.ctx.rankService().rankName(godName, next)), Messages.legacyText("conditions", conditions));
	}

	/**
	 * Where the god's main altar is: only for its believers (and the console), so other religions can't look it up;
	 * its priests are told when it has none
	 */
	private void mainAltar(CommandSender sender, String godName)
	{
		boolean believer = !(sender instanceof Player player) || godName.equals(godOf(player));
		if (!believer)
		{
			return;
		}
		Altar altar = this.ctx.altarRegistry().mainAltar(godName);
		if (altar != null)
		{
			send(sender, "command.info.main-altar", Messages.text("world", altar.world()), Messages.text("x", altar.block().x()), Messages.text("y", altar.block().y()),
					Messages.text("z", altar.block().z()));
		}
		else if (sender instanceof Player player && this.ctx.priestService().isPriest(player.getUniqueId()))
		{
			send(sender, "command.info.no-main-altar");
		}
	}

	@Override
	public List<String> tabComplete(CommandSender sender, List<String> args)
	{
		return args.size() == 1 ? godNames() : List.of();
	}
}
