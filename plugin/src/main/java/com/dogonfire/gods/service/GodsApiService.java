package com.dogonfire.gods.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.api.BelieverInfo;
import com.dogonfire.gods.api.GodInfo;
import com.dogonfire.gods.api.GodsApi;
import com.dogonfire.gods.api.QuestInfo;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.model.Believer;
import com.dogonfire.gods.model.God;

/**
 * {@link GodsApi} on top of the services, so what other plugins do follows the same rules, sends the same messages
 * and fires the same events as what players do. Registered in Bukkit's services manager by {@code Gods.onEnable}.
 * Snapshots are built by {@link ApiSnapshots}.
 */
public final class GodsApiService implements GodsApi
{
	private final GodsContext ctx;

	public GodsApiService(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	/** The API is main thread only, like the services under it */
	private void checkThread()
	{
		if (!ctx.plugin().getServer().isPrimaryThread())
		{
			throw new IllegalStateException("The Gods API may only be used from the server thread");
		}
	}

	/** The god's name as Gods writes it, if the god exists */
	private Optional<String> existing(String godName)
	{
		if (godName == null || godName.isBlank() || !ctx.godService().godExist(godName))
		{
			return Optional.empty();
		}
		return Optional.of(ctx.godService().formatGodName(godName));
	}

	/** A snapshot of an existing god, or null */
	GodInfo info(String godName)
	{
		God god = ctx.storage().gods().find(godName);
		if (god == null)
		{
			return null;
		}
		int claims = ctx.settings().holyLand().enabled() ? ctx.holyLandService().getClaimedChunksForGod(godName) : 0;
		return ApiSnapshots.god(god, ctx.godService().getGodPower(godName), ctx.believerService().getBelieversForGod(godName), ctx.godService().getHomeForGod(godName),
				claims, quest(godName));
	}

	/** A snapshot of the god's quest, or null */
	QuestInfo quest(String godName)
	{
		return ApiSnapshots.quest(ctx.storage().quests().find(godName), ctx.questService().getQuestLocation(godName));
	}

	// ---------------------------------------------------------------------
	// Gods

	@Override
	public List<String> getGodNames()
	{
		checkThread();
		List<String> names = new ArrayList<>();
		for (String godName : ctx.godService().getAllGods())
		{
			if (ctx.godService().godExist(godName))
			{
				names.add(godName);
			}
		}
		return List.copyOf(names);
	}

	@Override
	public List<GodInfo> getGods()
	{
		checkThread();
		List<GodInfo> gods = new ArrayList<>();
		for (String godName : getGodNames())
		{
			GodInfo info = info(godName);
			if (info != null)
			{
				gods.add(info);
			}
		}
		return List.copyOf(gods);
	}

	@Override
	public Optional<GodInfo> getGod(String godName)
	{
		checkThread();
		return existing(godName).map(this::info);
	}

	@Override
	public Optional<GodInfo> getGod(Player player)
	{
		checkThread();
		return existing(ctx.believerService().getGodForBeliever(player.getUniqueId())).map(this::info);
	}

	// ---------------------------------------------------------------------
	// Believers

	@Override
	public Optional<BelieverInfo> getBeliever(UUID playerId)
	{
		checkThread();
		Believer believer = ctx.storage().believers().find(playerId);
		// The god's priest list as it is (PriestService drops inactive priests when asked, with a message to believers)
		God god = believer != null && believer.godName() != null ? ctx.storage().gods().find(believer.godName()) : null;
		return Optional.ofNullable(ApiSnapshots.believer(believer, god != null && god.priests().contains(playerId)));
	}

	// ---------------------------------------------------------------------
	// Holy land

	@Override
	public Optional<String> getHolyLandGod(Location location)
	{
		checkThread();
		if (!ctx.settings().holyLand().enabled() || location.getWorld() == null)
		{
			return Optional.empty();
		}
		return Optional.ofNullable(ctx.holyLandService().getGodAtHolyLandLocation(location));
	}

	@Override
	public boolean isNeutralLand(Location location)
	{
		checkThread();
		return ctx.settings().holyLand().enabled() && location.getWorld() != null && ctx.holyLandService().isNeutralLandLocation(location);
	}
	// ---------------------------------------------------------------------
	// Speaking as a god

	// Ticks before the words of a god the API makes speak are sent (the next tick)
	private static final int	SPEECH_DELAY	= 1;
	private static final Pattern	TAG_NAME	= Pattern.compile("[a-z0-9_-]+");

	/** The voice situation with that key; an unknown one is the caller's mistake */
	static Voice situation(String key)
	{
		return Voice.byKey(key).orElseThrow(() -> new IllegalArgumentException("Gods has no voice situation '" + key + "'"));
	}

	/** The placeholders as tags, inserted as plain text */
	static TagResolver[] placeholders(Map<String, String> placeholders)
	{
		List<TagResolver> tags = new ArrayList<>();
		for (Map.Entry<String, String> placeholder : placeholders.entrySet())
		{
			if (placeholder.getKey() == null || !TAG_NAME.matcher(placeholder.getKey()).matches())
			{
				throw new IllegalArgumentException("'" + placeholder.getKey() + "' is not a valid placeholder name (a-z, 0-9, _ and -)");
			}
			tags.add(Messages.text(placeholder.getKey(), String.valueOf(placeholder.getValue())));
		}
		return tags.toArray(new TagResolver[0]);
	}

	@Override
	public Set<String> getVoiceSituations()
	{
		Set<String> keys = new LinkedHashSet<>();
		for (Voice voice : Voice.values())
		{
			keys.add(voice.key());
		}
		return Collections.unmodifiableSet(keys);
	}

	@Override
	public boolean say(String godName, Player player, String text)
	{
		checkThread();
		Optional<String> god = existing(godName);
		return god.isPresent() && ctx.godSpeech().sayText(god.get(), player, text, SPEECH_DELAY);
	}

	@Override
	public int sayToBelievers(String godName, String text)
	{
		checkThread();
		return existing(godName).map(god -> ctx.godSpeech().sayTextToBelievers(god, text, SPEECH_DELAY)).orElse(0);
	}

	@Override
	public boolean sayVoiceLine(String godName, Player player, String situation, Map<String, String> placeholders)
	{
		checkThread();
		Voice voice = situation(situation);
		TagResolver[] tags = placeholders(placeholders);
		Optional<String> god = existing(godName);
		return god.isPresent() && ctx.godSpeech().say(god.get(), player, voice, SPEECH_DELAY, tags);
	}

	@Override
	public Optional<String> getVoiceLine(String godName, String situation, Map<String, String> placeholders)
	{
		checkThread();
		Voice voice = situation(situation);
		TagResolver[] tags = placeholders(placeholders);
		return existing(godName).map(god -> ctx.godSpeech().renderedVoiceLine(god, voice, tags));
	}
}
