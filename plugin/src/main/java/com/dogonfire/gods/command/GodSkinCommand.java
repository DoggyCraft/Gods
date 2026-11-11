package com.dogonfire.gods.command;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;

/**
 * /g godskin &lt;player&gt; | reset: the priest gives their god's NPC (at its main altar) a player's skin, or gives it
 * back its god type's. The skin is looked up in the background (an online player's at once, anyone else's through
 * Mojang) and stored on the god with the name it came from, so the god keeps it.
 */
public final class GodSkinCommand extends SubCommand
{
	public static final String RESET = "reset";

	public GodSkinCommand(GodsContext ctx)
	{
		super(ctx, "godskin", "gods.priest.godskin");
		priestOnly("command.godskin.not-priest");
		args(1, 1);
	}

	@Override
	public String disabledMessage()
	{
		return this.ctx.settings().npc().enabled() ? null : "command.disabled.npc";
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException
	{
		String godName = believersGod((Player) sender);
		String name = args.getFirst();
		if (name.equalsIgnoreCase(RESET))
		{
			this.ctx.godService().setNpcSkin(godName, null);
			send(sender, "command.godskin.reset", Messages.god(godName));
			return;
		}
		if (!name.matches("[A-Za-z0-9_]{1,16}"))
		{
			throw new CommandException("command.godskin.not-found", Messages.player(name));
		}

		send(sender, "command.godskin.looking", Messages.player(name));
		UUID priestId = ((Player) sender).getUniqueId();
		this.ctx.godNpcs().skins().resolve(name, skin -> {
			// The answer may come later: the priest may have logged off, the god may be gone
			Player priest = this.ctx.plugin().getServer().getPlayer(priestId);
			if (skin == null)
			{
				if (priest != null)
				{
					send(priest, "command.godskin.not-found", Messages.player(name));
				}
				return;
			}
			if (!this.ctx.godService().godExist(godName))
			{
				return;
			}
			this.ctx.godService().setNpcSkin(godName, skin);
			this.ctx.plugin().log((priest != null ? priest.getName() : priestId.toString()) + " gave " + godName + " the skin of " + skin.name());
			if (priest != null)
			{
				send(priest, "command.godskin.done", Messages.god(godName), Messages.player(skin.name() != null ? skin.name() : name));
			}
		});
	}

	@Override
	public List<String> tabComplete(CommandSender sender, List<String> args)
	{
		if (args.size() != 1)
		{
			return List.of();
		}
		List<String> names = new ArrayList<>(onlinePlayerNames(sender));
		names.add(RESET);
		return names;
	}
}
