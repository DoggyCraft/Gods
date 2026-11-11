package com.dogonfire.gods.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.task.GodSpeakTask;
import com.dogonfire.gods.task.InfoTask;

/**
 * Gods speaking to players, and the info messages Gods sends a little later. A god speaks in its voice
 * ({@link com.dogonfire.gods.message.GodVoices}: a random line for the situation from the voice file of its type and
 * gender), formatted by {@code god.speech} in messages.yml, and only to players who are online, in an enabled world and
 * have {@code gods.listen}. Messages are sent after a delay in ticks ({@link GodSpeakTask}, {@link InfoTask}).
 * <p>
 * Every call passes its own tags. A god's line also knows {@code <god>} (the god's name) and, unless the call says
 * otherwise, {@code <player>} (the name of the player spoken to). Every god utterance goes through
 * {@link #deliver}, so that is where an event that lets other plugins change or cancel what a god says belongs.
 */
public class GodSpeech
{
	private final Random		random	= new Random();
	private final GodsContext	ctx;

	public GodSpeech(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	private Messages messages()
	{
		return ctx.messages();
	}

	// ---------------------------------------------------------------------
	// What a god says

	/**
	 * One of the god's lines for the situation (MiniMessage, tags not filled in), picked at random from its voice; null
	 * if its voice has nothing to say there
	 */
	public String voiceLine(String godName, Voice situation)
	{
		return messages().voices().randomLine(ctx.godService().getVoiceForGod(godName), situation);
	}

	/**
	 * One of the god's lines for the situation as plain text without colors or highlights (for book pages), or null.
	 * {@code <god>} is the god's name.
	 */
	public String plainVoiceLine(String godName, Voice situation, TagResolver... tags)
	{
		String line = voiceLine(godName, situation);
		if (line == null)
		{
			return null;
		}
		return Messages.legacy(messages().parsePlain(line, withGod(godName, tags)));
	}

	/** The god says one of its lines for the situation to the player after {@code delay} ticks */
	public void say(String godName, Player player, Voice situation, int delay, TagResolver... tags)
	{
		if (!canHear(godName, player))
		{
			return;
		}
		String line = voiceLine(godName, situation);
		if (line == null)
		{
			return;
		}
		deliver(godName, player, line, delay, tags);
	}

	/**
	 * The god says {@code text} (MiniMessage) to the player after {@code delay} ticks, looking like everything else it
	 * says. For words that are not in its voice file.
	 */
	public void sayText(String godName, Player player, String text, int delay, TagResolver... tags)
	{
		if (canHear(godName, player))
		{
			deliver(godName, player, text, delay, tags);
		}
	}

	/** Like {@link #say}, followed by how to answer (/gods yes or /gods no) */
	public void sayWithQuestion(String godName, Player player, Voice situation, int delay, TagResolver... tags)
	{
		if (!canHear(godName, player))
		{
			return;
		}
		String line = voiceLine(godName, situation);
		if (line != null)
		{
			deliver(godName, player, line, delay, tags);
		}
		sendInfo(player.getUniqueId(), "god.question-hint", delay + 80);
	}

	/** To one believer, if online, after 2-11 ticks */
	public void sayToBeliever(String godName, UUID playerId, Voice situation, TagResolver... tags)
	{
		sayToBeliever(godName, playerId, situation, 2 + this.random.nextInt(10), tags);
	}

	/** To one believer, if online */
	public void sayToBeliever(String godName, UUID playerId, Voice situation, int delay, TagResolver... tags)
	{
		Player player = ctx.plugin().getServer().getPlayer(playerId);
		if (player == null)
		{
			ctx.plugin().logDebug("GodSayToBeliever player is null");
			return;
		}
		say(godName, player, situation, delay, tags);
	}

	/** To every online believer of the god, each with a line of its own */
	public void sayToBelievers(String godName, Voice situation, int delay, TagResolver... tags)
	{
		for (UUID playerId : ctx.believerService().getBelieversForGod(godName))
		{
			Player player = ctx.plugin().getServer().getPlayer(playerId);
			if (player != null)
			{
				say(godName, player, situation, delay, tags);
			}
		}
	}

	/** To every online believer of the god but one, each after 2-21 ticks */
	public void sayToBelieversExcept(String godName, Voice situation, UUID exceptPlayer, TagResolver... tags)
	{
		for (UUID playerId : ctx.believerService().getBelieversForGod(godName))
		{
			Player player = ctx.plugin().getServer().getPlayer(playerId);
			if (player != null && !player.getUniqueId().equals(exceptPlayer))
			{
				say(godName, player, situation, 2 + this.random.nextInt(20), tags);
			}
		}
	}

	/** To every online priest of the god, each after 2-31 ticks */
	public void sayToPriests(String godName, Voice situation, TagResolver... tags)
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
				say(godName, player, situation, 2 + this.random.nextInt(30), tags);
			}
		}
	}

	private boolean canHear(String godName, Player player)
	{
		if (player == null)
		{
			ctx.plugin().logDebug("GodSay(): Player is null!");
			return false;
		}
		if (!ctx.plugin().isEnabledInWorld(player.getWorld()))
		{
			return false;
		}
		return ctx.permissions().hasPermission(player, "gods.listen");
	}

	/**
	 * Every god's words to a player pass here: the line (MiniMessage) is formatted with {@code god.speech} now and sent
	 * after {@code delay} ticks. A cancellable event for other plugins (to change or silence what a god says) belongs
	 * here.
	 */
	private void deliver(String godName, Player player, String line, int delay, TagResolver... tags)
	{
		TagResolver lineTags = withGod(godName, tags);
		if (!lineTags.has("player"))
		{
			lineTags = TagResolver.resolver(lineTags, Messages.player(player.getName()));
		}

		Component words = messages().component("god.speech", lineTags, Placeholder.parsed("message", line));
		List<String> text = List.of(Messages.legacy(words));

		ctx.plugin().logDebug(godName + " to " + player.getName() + ": " + Messages.plain(words));

		ctx.plugin().getServer().getScheduler().runTaskLater(ctx.plugin(), new GodSpeakTask(ctx, player.getUniqueId(), text), delay);
	}

	private static TagResolver withGod(String godName, TagResolver... tags)
	{
		TagResolver given = TagResolver.resolver(tags);
		return given.has("god") ? given : TagResolver.resolver(given, Messages.god(godName));
	}

	// ---------------------------------------------------------------------
	// Info messages

	/** Sends a message from messages.yml to the player after {@code delay} ticks, if the player is online */
	public void sendInfo(UUID playerId, String key, int delay, TagResolver... tags)
	{
		Player player = ctx.plugin().getServer().getPlayer(playerId);
		if (player == null)
		{
			ctx.plugin().logDebug("sendInfo can not find online player with id " + playerId);
			return;
		}

		List<String> lines = new ArrayList<>(messages().legacyLines(key, tags));
		ctx.plugin().getServer().getScheduler().runTaskLater(ctx.plugin(), new InfoTask(ctx, playerId, lines), delay);
	}

	/** To every online believer of the god, after 10 ticks */
	public void sendInfoToBelievers(String godName, String key, TagResolver... tags)
	{
		for (UUID playerId : ctx.believerService().getBelieversForGod(godName))
		{
			if (ctx.plugin().getServer().getPlayer(playerId) != null)
			{
				sendInfo(playerId, key, 10, tags);
			}
		}
	}
}
