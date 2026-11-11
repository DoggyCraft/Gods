package com.dogonfire.gods.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.model.GodMood;

public class CommandInfo extends GodsCommand
{
	CommandInfo(GodsContext ctx)
	{
		super(ctx, "info");
		this.permission = "gods.info";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!hasPermission(sender))
		{
			send(sender, NO_PERMISSION);
			return;
		}
		
		String godName = null;

		if (args.length == 2)
		{
			godName = ctx.godService().formatGodName(args[1]);
		}

		if (godName == null)
		{
			if (sender instanceof Player)
				godName = ctx.believerService().getGodForBeliever(((Player) sender).getUniqueId());
			if (godName == null)
			{
				send(sender, "command.info.no-god");
				return;
			}
		}

		if (!ctx.godService().godExist(godName))
		{
			send(sender, "command.info.unknown-god");
			return;
		}

		List<UUID> priests = ctx.priestService().getPriestsForGod(godName);

		if (priests == null)
		{
			priests = new ArrayList<UUID>();
		}

		send(sender, "command.info.header", Messages.god(godName), Messages.color("type_color", ctx.godService().getColorForGod(godName)), Messages.legacyText("title", ctx.godService().getTitleForGod(godName)));

		send(sender, "command.info.god-description", Messages.text("description", String.valueOf(ctx.godService().getGodDescription(godName))));

		GodMood godMood = ctx.moodService().getMoodForGod(godName);
		send(sender, "command.info.mood." + godMood.name(), Messages.god(godName), Messages.component("mood", ctx.messages().moodName(godMood)));
	
		Material neededItem = ctx.commandmentService().getSacrificeItemTypeForGod(godName);
		if (neededItem != null)
		{
			send(sender, "command.info.sacrifice", Messages.god(godName), Messages.type(ctx.messages().itemName(neededItem)));
		}

		if (priests.size() == 0)
		{
			send(sender, "command.info.no-priest");
		}
		else if (priests.size() == 1)
		{
			send(sender, "command.info.priest", Messages.player(ctx.plugin().getServer().getOfflinePlayer(priests.get(0)).getName()));
		}
		else
		{
			send(sender, "command.info.priests");
			for (UUID priest : priests)
			{
				send(sender, "command.info.priests-line", Messages.player(ctx.plugin().getServer().getOfflinePlayer(priest).getName()));
			}
		}

		send(sender, "command.info.believers", Messages.amount(ctx.believerService().getBelieversForGod(godName).size()));
		send(sender, "command.info.power", Messages.text("power", ctx.godService().getGodPower(godName)));
		if (ctx.settings().commandments().enabled())
		{
			send(sender, "command.info.holy-food", Messages.type(ctx.messages().itemName(ctx.commandmentService().getHolyFoodTypeForGod(godName))));
			send(sender, "command.info.unholy-food", Messages.type(ctx.messages().itemName(ctx.commandmentService().getUnholyFoodTypeForGod(godName))));

			send(sender, "command.info.holy-creature", Messages.type(ctx.messages().mobName(ctx.commandmentService().getHolyMobTypeForGod(godName))));
			send(sender, "command.info.unholy-creature", Messages.type(ctx.messages().mobName(ctx.commandmentService().getUnholyMobTypeForGod(godName))));
		}

		List<String> allyRelations = ctx.relationService().getAllianceRelations(godName);
		Object warRelations = ctx.relationService().getWarRelations(godName);

		if ((((List<?>) warRelations).size() > 0) || (allyRelations.size() > 0))
		{
			send(sender, "command.info.relations");
			for (String ally : ctx.relationService().getAllianceRelations(godName))
			{
				send(sender, "command.info.alliance", Messages.god(ally));
			}
			List<String> enemies = ctx.relationService().getWarRelations(godName);
			for (String enemy : enemies)
			{
				send(sender, "command.info.war", Messages.god(enemy));
			}
		}
		
		if(ctx.settings().holyLand().enabled())
		{
			int claimed = ctx.holyLandService().getClaimedChunksForGod(godName);
			int maxclaim = ctx.holyLandService().getMaximumChunksForGod(godName);
			send(sender, "command.info.holy-land", Messages.text("claimed", claimed), Messages.text("max", maxclaim));					
		}
	}
}
