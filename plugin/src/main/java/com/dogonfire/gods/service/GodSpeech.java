package com.dogonfire.gods.service;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.managers.LanguageManager.LANGUAGESTRING;
import com.dogonfire.gods.tasks.TaskGodSpeak;
import com.dogonfire.gods.tasks.TaskInfo;

/**
 * Every message Gods sends to players about gods: a god speaking to a believer, its believers or its priests
 * ({@link TaskGodSpeak}, in the god's voice from its language file), and the plugin's info messages
 * ({@link TaskInfo}, from the general language file). Messages are sent a few ticks later, and only to players who
 * are online, in an enabled world and (for a god's words) have {@code gods.listen}.
 * <p>
 * A god's message uses the player name, type and amount set on the {@link LanguageManager} right before the call;
 * they are taken now, so the next message can set its own. This is the one place that turns language keys into
 * chat, so the move to MiniMessage only has to change this class and the two tasks.
 */
public class GodSpeech
{
	private final Random		random	= new Random();
	private final GodsContext	ctx;

	public GodSpeech(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	/** The god says {@code message} to the player after {@code delay} ticks */
	public void say(String godName, Player player, LANGUAGESTRING message, int delay)
	{
		String playerNameString = ctx.languageManager().getPlayerName();
		String typeNameString = ctx.languageManager().getType();
		int amount = ctx.languageManager().getAmount();

		if (player == null)
		{
			ctx.plugin().logDebug("GodSay(): Player is null!");
			return;
		}

		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return;
		}

		ctx.plugin().logDebug(godName + " to " + player.getName() + ": " + ctx.languageManager().getLanguageString(godName, message));

		if (!ctx.permissionsManager().hasPermission(player, "gods.listen"))
		{
			return;
		}

		ctx.plugin().getServer().getScheduler().runTaskLater(ctx.plugin(), new TaskGodSpeak(ctx, godName, player.getUniqueId(), playerNameString, typeNameString, amount, message), delay);
	}

	/** Like {@link #say}, followed by how to answer (/gods yes or /gods no) */
	public void sayWithQuestion(String godName, Player player, LANGUAGESTRING message, int delay)
	{
		String playerNameString = ctx.languageManager().getPlayerName();
		String typeNameString = ctx.languageManager().getType();
		int amount = ctx.languageManager().getAmount();

		if (player == null)
		{
			ctx.plugin().logDebug("GodSay(): Player is null!");
			return;
		}
		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return;
		}
		ctx.plugin().logDebug(godName + " to " + player.getName() + ": " + ctx.languageManager().getLanguageString(godName, message));
		if (!ctx.permissionsManager().hasPermission(player, "gods.listen"))
		{
			return;
		}

		ctx.plugin().getServer().getScheduler().runTaskLater(ctx.plugin(), new TaskGodSpeak(ctx, godName, player.getUniqueId(), playerNameString, typeNameString, amount, message), delay);

		sendInfo(player.getUniqueId(), LANGUAGESTRING.GodToBelieverQuestionHelp, ChatColor.AQUA, ChatColor.WHITE + "/gods yes or /gods no", ChatColor.WHITE + "/gods yes or /gods no", delay + 80);
	}

	/** To one believer, if online, after 2-11 ticks */
	public void sayToBeliever(String godName, UUID playerId, LANGUAGESTRING message)
	{
		sayToBeliever(godName, playerId, message, 2 + this.random.nextInt(10));
	}

	/** To one believer, if online */
	public void sayToBeliever(String godName, UUID playerId, LANGUAGESTRING message, int delay)
	{
		Player player = ctx.plugin().getServer().getPlayer(playerId);

		if (player == null)
		{
			ctx.plugin().logDebug("GodSayToBeliever player is null");
			return;
		}
		say(godName, player, message, delay);
	}

	/** To every online believer of the god */
	public void sayToBelievers(String godName, LANGUAGESTRING message, int delay)
	{
		for (UUID playerId : ctx.believerService().getBelieversForGod(godName))
		{
			Player player = ctx.plugin().getServer().getPlayer(playerId);
			if (player != null)
			{
				say(godName, player, message, delay);
			}
		}
	}

	/** To every online believer of the god but one, each after 2-21 ticks */
	public void sayToBelieversExcept(String godName, LANGUAGESTRING message, UUID exceptPlayer)
	{
		for (UUID playerId : ctx.believerService().getBelieversForGod(godName))
		{
			Player player = ctx.plugin().getServer().getPlayer(playerId);

			if (player != null && !player.getUniqueId().equals(exceptPlayer))
			{
				say(godName, player, message, 2 + this.random.nextInt(20));
			}
		}
	}

	/** To every online priest of the god, each after 2-31 ticks */
	public void sayToPriests(String godName, LANGUAGESTRING message)
	{
		List<UUID> priests = ctx.priestService().getPriestsForGod(godName);
		if (priests == null)
		{
			return;
		}

		for (UUID priest : priests)
		{
			Player player = ctx.plugin().getServer().getPlayer(priest);
			if (player != null)
			{
				say(godName, player, message, 2 + this.random.nextInt(30));
			}
		}
	}

	// ---------------------------------------------------------------------
	// Info messages

	/** $Amount is {@code amount}, $PlayerName is {@code name} */
	public void sendInfo(UUID playerId, LANGUAGESTRING message, ChatColor color, int amount, String name, int delay)
	{
		if (isOnline(playerId))
		{
			ctx.plugin().getServer().getScheduler().runTaskLater(ctx.plugin(), new TaskInfo(ctx, color, playerId, message, amount, name), delay);
		}
	}

	/** $PlayerName is {@code name}, $Type is {@code amount1} and $Amount is {@code amount2} */
	public void sendInfo(UUID playerId, LANGUAGESTRING message, ChatColor color, String name, int amount1, int amount2, int delay)
	{
		if (isOnline(playerId))
		{
			ctx.plugin().getServer().getScheduler().runTaskLater(ctx.plugin(), new TaskInfo(ctx, color, playerId, message, name, amount1, amount2), delay);
		}
	}

	/** $PlayerName is {@code name1}, $Type is {@code name2} */
	public void sendInfo(UUID playerId, LANGUAGESTRING message, ChatColor color, String name1, String name2, int delay)
	{
		if (isOnline(playerId))
		{
			ctx.plugin().getServer().getScheduler().runTaskLater(ctx.plugin(), new TaskInfo(ctx, color, playerId, message, name1, name2), delay);
		}
	}

	/** To every online believer of the god, after 10 ticks */
	public void sendInfoToBelievers(String godName, LANGUAGESTRING message, ChatColor color, int delay)
	{
		for (UUID playerId : ctx.believerService().getBelieversForGod(godName))
		{
			Player player = ctx.plugin().getServer().getPlayer(playerId);

			if (player != null)
			{
				sendInfo(playerId, message, color, 0, "", 10);
			}
		}
	}

	/** To every online believer of the god, after 10 ticks */
	public void sendInfoToBelievers(String godName, LANGUAGESTRING message, ChatColor color, String name, int amount1, int amount2, int delay)
	{
		for (UUID playerId : ctx.believerService().getBelieversForGod(godName))
		{
			Player player = ctx.plugin().getServer().getPlayer(playerId);
			if (player != null)
			{
				sendInfo(playerId, message, color, name, amount1, amount2, 10);
			}
		}
	}

	private boolean isOnline(UUID playerId)
	{
		Player player = ctx.plugin().getServer().getPlayer(playerId);
		if (player == null)
		{
			ctx.plugin().logDebug("sendInfo can not find online player with id " + playerId);
			return false;
		}
		return true;
	}
}
