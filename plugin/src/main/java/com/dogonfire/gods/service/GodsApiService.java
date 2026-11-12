package com.dogonfire.gods.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.api.ActionResult;
import com.dogonfire.gods.api.BelieverInfo;
import com.dogonfire.gods.api.GodInfo;
import com.dogonfire.gods.api.GodRank;
import com.dogonfire.gods.api.GodsApi;
import com.dogonfire.gods.api.QuestInfo;
import com.dogonfire.gods.api.RankProgress;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.model.Believer;
import com.dogonfire.gods.model.God;
import com.dogonfire.gods.model.GodGender;

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
		int claims = ctx.settings().holyLand().enabled() ? ctx.holyLandService().getHolyLandsForGod(godName) : 0;
		return ApiSnapshots.god(god, ctx.godService().getGodPower(godName), ctx.believerService().getBelieversForGod(godName), ApiSnapshots.home(ctx.godService().getHome(godName), ctx.plugin().getServer()::getWorld),
				claims, quest(godName), ctx.altarRegistry().mainAltarLocation(godName));
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
		return Optional.ofNullable(ApiSnapshots.believer(believer, believer != null && isPriest(believer.godName(), playerId)));
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
	// The god at its altar

	@Override
	public Optional<Entity> getNpc(String godName)
	{
		checkThread();
		return existing(godName).map(name -> ctx.godNpcs().entity(name));
	}

	@Override
	public boolean isGodNpc(Entity entity)
	{
		checkThread();
		return entity != null && ctx.godNpcs().godOf(entity) != null;
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
	// ---------------------------------------------------------------------
	// Acting as a god

	private static ActionResult done(boolean happened)
	{
		return happened ? ActionResult.SUCCESS : ActionResult.CANCELLED;
	}

	@Override
	public ActionResult bless(String godName, Player player)
	{
		checkThread();
		Optional<String> god = existing(godName);
		if (god.isEmpty())
		{
			return ActionResult.NO_SUCH_GOD;
		}
		if (!ctx.settings().blessings().enabled())
		{
			return ActionResult.FEATURE_DISABLED;
		}
		if (!player.isOnline() || ctx.believerService().hasRecentBlessing(player.getUniqueId()))
		{
			return ActionResult.NOT_POSSIBLE;
		}
		return done(ctx.blessingService().blessPlayer(god.get(), player.getUniqueId(), ctx.godService().getGodPower(god.get())));
	}

	@Override
	public ActionResult curse(String godName, Player player)
	{
		checkThread();
		Optional<String> god = existing(godName);
		if (god.isEmpty())
		{
			return ActionResult.NO_SUCH_GOD;
		}
		if (!ctx.settings().curses().enabled())
		{
			return ActionResult.FEATURE_DISABLED;
		}
		if (!player.isOnline() || ctx.believerService().hasRecentCursing(player.getUniqueId()))
		{
			return ActionResult.NOT_POSSIBLE;
		}
		return done(ctx.blessingService().cursePlayer(god.get(), player.getUniqueId(), ctx.godService().getGodPower(god.get())));
	}

	@Override
	public ActionResult heal(String godName, Player player)
	{
		checkThread();
		Optional<String> god = existing(godName);
		return god.isEmpty() ? ActionResult.NO_SUCH_GOD : done(ctx.blessingService().healPlayer(god.get(), player));
	}

	@Override
	public ActionResult giveItem(String godName, Player player, Material material)
	{
		checkThread();
		if (material == null || !material.isItem() || material.isAir())
		{
			throw new IllegalArgumentException(material + " is not an item");
		}
		Optional<String> god = existing(godName);
		return god.isEmpty() ? ActionResult.NO_SUCH_GOD : done(ctx.blessingService().giveItem(god.get(), player, material, true));
	}

	@Override
	public ActionResult giveHolyArtifact(String godName, Player player)
	{
		checkThread();
		Optional<String> god = existing(godName);
		if (god.isEmpty())
		{
			return ActionResult.NO_SUCH_GOD;
		}
		if (!ctx.settings().holyArtifacts().enabled())
		{
			return ActionResult.FEATURE_DISABLED;
		}
		if (!player.isOnline() || !ctx.plugin().isEnabledInWorld(player.getWorld()) || ctx.holyArtifactService().hasHolyArtifact(player) || ctx.godService().getDivineForceForGod(god.get()) == null)
		{
			return ActionResult.NOT_POSSIBLE;
		}
		return done(ctx.blessingService().blessPlayerWithHolyArtifact(god.get(), player));
	}

	@Override
	public ActionResult smite(String godName, Player player, int damage)
	{
		checkThread();
		Optional<String> god = existing(godName);
		return god.isEmpty() ? ActionResult.NO_SUCH_GOD : done(ctx.blessingService().smite(god.get(), player, damage));
	}

	@Override
	public ActionResult addMood(String godName, float amount)
	{
		checkThread();
		Optional<String> god = existing(godName);
		return god.isEmpty() ? ActionResult.NO_SUCH_GOD : done(ctx.moodService().addMoodForGod(god.get(), amount));
	}

	@Override
	public ActionResult setGender(String godName, com.dogonfire.gods.api.GodGender gender)
	{
		checkThread();
		Objects.requireNonNull(gender, "gender");
		Optional<String> god = existing(godName);
		if (god.isEmpty())
		{
			return ActionResult.NO_SUCH_GOD;
		}
		if (gender == com.dogonfire.gods.api.GodGender.NONE)
		{
			return ActionResult.NOT_POSSIBLE;
		}
		GodGender newGender = ApiSnapshots.modelGender(gender);
		if (ctx.godService().getGenderForGod(god.get()) == newGender)
		{
			return ActionResult.SUCCESS;
		}
		return done(ctx.godService().changeGender(god.get(), newGender));
	}

	@Override
	public ActionResult addPrayerPower(UUID playerId, int amount)
	{
		checkThread();
		if (ctx.believerService().getGodForBeliever(playerId) == null)
		{
			return ActionResult.NOT_BELIEVER;
		}
		ctx.believerService().increasePrayerPower(playerId, amount);
		return ActionResult.SUCCESS;
	}

	@Override
	public ActionResult startQuest(String godName)
	{
		checkThread();
		Optional<String> god = existing(godName);
		if (god.isEmpty())
		{
			return ActionResult.NO_SUCH_GOD;
		}
		if (!ctx.settings().quests().enabled())
		{
			return ActionResult.FEATURE_DISABLED;
		}
		return switch (ctx.questService().startQuest(god.get()))
		{
			case STARTED -> ActionResult.SUCCESS;
			case CANCELLED -> ActionResult.CANCELLED;
			case NOT_POSSIBLE -> ActionResult.NOT_POSSIBLE;
		};
	}

	@Override
	public ActionResult completeQuest(String godName)
	{
		checkThread();
		Optional<String> god = existing(godName);
		if (god.isEmpty())
		{
			return ActionResult.NO_SUCH_GOD;
		}
		return ctx.questService().completeQuest(god.get()) ? ActionResult.SUCCESS : ActionResult.NOT_POSSIBLE;
	}

	@Override
	public ActionResult failQuest(String godName)
	{
		checkThread();
		Optional<String> god = existing(godName);
		if (god.isEmpty())
		{
			return ActionResult.NO_SUCH_GOD;
		}
		return ctx.questService().failQuest(god.get()) ? ActionResult.SUCCESS : ActionResult.NOT_POSSIBLE;
	}

	@Override
	public ActionResult invite(String godName, Player player)
	{
		checkThread();
		Optional<String> god = existing(godName);
		if (god.isEmpty())
		{
			return ActionResult.NO_SUCH_GOD;
		}
		if (god.get().equals(ctx.believerService().getGodForBeliever(player.getUniqueId())))
		{
			return ActionResult.NOT_POSSIBLE;
		}
		ctx.prayerService().invite(god.get(), player);
		return ActionResult.SUCCESS;
	}

	@Override
	public ActionResult offerPriesthood(String godName, Player player)
	{
		checkThread();
		Optional<String> god = existing(godName);
		if (god.isEmpty())
		{
			return ActionResult.NO_SUCH_GOD;
		}
		if (!god.get().equals(ctx.believerService().getGodForBeliever(player.getUniqueId())))
		{
			return ActionResult.NOT_BELIEVER;
		}
		if (isPriest(god.get(), player.getUniqueId()) || !ctx.priestService().offerPriesthood(god.get(), player))
		{
			return ActionResult.NOT_POSSIBLE;
		}
		return ActionResult.SUCCESS;
	}

	@Override
	public ActionResult assignPriest(String godName, UUID playerId)
	{
		checkThread();
		Optional<String> god = existing(godName);
		if (god.isEmpty())
		{
			return ActionResult.NO_SUCH_GOD;
		}
		if (!god.get().equals(ctx.believerService().getGodForBeliever(playerId)))
		{
			return ActionResult.NOT_BELIEVER;
		}
		return done(ctx.priestService().assignPriest(god.get(), playerId));
	}

	@Override
	public ActionResult removePriest(String godName, UUID playerId)
	{
		checkThread();
		Optional<String> god = existing(godName);
		if (god.isEmpty())
		{
			return ActionResult.NO_SUCH_GOD;
		}
		if (!isPriest(god.get(), playerId))
		{
			return ActionResult.NOT_POSSIBLE;
		}
		ctx.priestService().removePriest(god.get(), playerId);
		return ActionResult.SUCCESS;
	}

	@Override
	public ActionResult setAlliance(String godName, String otherGodName, boolean allied)
	{
		checkThread();
		Optional<String> god = existing(godName);
		Optional<String> other = existing(otherGodName);
		if (god.isEmpty() || other.isEmpty())
		{
			return ActionResult.NO_SUCH_GOD;
		}
		if (god.get().equals(other.get()))
		{
			return ActionResult.NOT_POSSIBLE;
		}
		return done(ctx.relationService().setAlliance(god.get(), other.get(), allied));
	}

	@Override
	public ActionResult setWar(String godName, String otherGodName, boolean atWar)
	{
		checkThread();
		Optional<String> god = existing(godName);
		Optional<String> other = existing(otherGodName);
		if (god.isEmpty() || other.isEmpty())
		{
			return ActionResult.NO_SUCH_GOD;
		}
		if (god.get().equals(other.get()))
		{
			return ActionResult.NOT_POSSIBLE;
		}
		return done(ctx.relationService().setWar(god.get(), other.get(), atWar));
	}

	@Override
	public Optional<RankProgress> getRankProgress(String godName)
	{
		checkThread();
		return existing(godName).map(god -> {
			com.dogonfire.gods.model.GodRank rank = ctx.rankService().getRank(god);
			com.dogonfire.gods.model.GodRank next = rank.next();
			return ApiSnapshots.rankProgress(rank, next, ctx.rankService().progress(god, next != null ? next : rank));
		});
	}

	@Override
	public ActionResult setRank(String godName, GodRank rank)
	{
		checkThread();
		Optional<String> god = existing(godName);
		if (god.isEmpty())
		{
			return ActionResult.NO_SUCH_GOD;
		}
		if (rank == null)
		{
			return ActionResult.NOT_POSSIBLE;
		}
		return done(ctx.rankService().setRank(god.get(), ApiSnapshots.modelRank(rank)));
	}

	// ---------------------------------------------------------------------
	// Devotion

	@Override
	public ActionResult addDevotion(UUID playerId, int points)
	{
		checkThread();
		if (!ctx.devotionService().enabled())
		{
			return ActionResult.FEATURE_DISABLED;
		}
		if (playerId == null || ctx.believerService().getGodForBeliever(playerId) == null)
		{
			return ActionResult.NOT_BELIEVER;
		}
		ctx.devotionService().addPoints(playerId, points, com.dogonfire.gods.api.event.BelieverDevotionChangeEvent.Cause.API);
		return ActionResult.SUCCESS;
	}

	@Override
	public ActionResult setDevotionLevel(UUID playerId, com.dogonfire.gods.api.DevotionLevel level)
	{
		checkThread();
		if (!ctx.devotionService().enabled())
		{
			return ActionResult.FEATURE_DISABLED;
		}
		if (playerId == null || ctx.believerService().getGodForBeliever(playerId) == null)
		{
			return ActionResult.NOT_BELIEVER;
		}
		if (level == null)
		{
			return ActionResult.NOT_POSSIBLE;
		}
		return done(ctx.devotionService().setLevel(playerId, ApiSnapshots.modelDevotion(level)));
	}

	/** Whether the player is in the god's priest list as stored (see {@link #getBeliever}) */
	private boolean isPriest(String godName, UUID playerId)
	{
		God god = ctx.storage().gods().find(godName);
		return god != null && god.priests().contains(playerId);
	}
}
