package com.dogonfire.gods.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.api.event.GodAscendEvent;
import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.api.event.GodDescendEvent;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.model.Altar;
import com.dogonfire.gods.model.AltarKind;
import com.dogonfire.gods.model.BlockPos;
import com.dogonfire.gods.model.God;
import com.dogonfire.gods.model.GodRank;
import com.dogonfire.gods.model.GodStats;
import com.dogonfire.gods.model.GodType;
import com.dogonfire.gods.storage.CleanupGuard;
import com.dogonfire.gods.storage.GodRepository;
import com.dogonfire.gods.util.LegacyDates;

/**
 * Gods' ranks (config.yml's {@code ranks}): what rank a god has, its religion's lifetime stats, and the check on its
 * turn ({@link #evaluate}) that makes it rise or fall ({@link RankRules}). Crossing into {@link GodRank#DESCENDED} is
 * its descent: it appears at its main altar ({@link GodNpcService}), fires {@link GodDescendEvent} and keeps
 * {@code DescendedTime}; falling below it is its ascent ({@link GodAscendEvent}). A god from before ranks is placed
 * once ({@link RankRules#seed}). Ranks and stats live in gods.yml ({@link GodRepository}). Main thread only.
 */
public class RankService
{
	private final GodsContext		ctx;
	private final GodRepository		repository;
	// The aura's effect that was warned about as unknown
	private String					unknownEffect;

	public RankService(GodsContext ctx, GodRepository repository)
	{
		this.ctx = ctx;
		this.repository = repository;
	}

	private God god(String godName)
	{
		return godName != null ? this.repository.find(godName) : null;
	}

	// ---------------------------------------------------------------------
	// Reading

	/** The god's rank; {@link GodRank#SPIRIT} for one that was never ranked or doesn't exist */
	public GodRank getRank(String godName)
	{
		God god = god(godName);
		return god != null && god.rank() != null ? god.rank() : GodRank.SPIRIT;
	}

	/** What the god's rank is called (its god type's name for it, themes.yml's {@code ranks}) */
	public String getRankName(String godName)
	{
		return rankName(godName, getRank(godName));
	}

	/** What a rank is called for this god (its god type's name for it; the default theme's for a god without one) */
	public String rankName(String godName, GodRank rank)
	{
		GodType type = godName != null && ctx.godService().godExist(godName) ? ctx.godService().getDivineForceForGod(godName) : null;
		return ctx.themes().of(type).rankNames().of(rank);
	}

	/** When the god reached its rank, or null */
	public Instant getRankSince(String godName)
	{
		God god = god(godName);
		return god != null ? god.rankSince() : null;
	}

	/** What the god's religion has done in its whole life */
	public GodStats getStats(String godName)
	{
		God god = god(godName);
		return god != null ? god.stats() : GodStats.ZERO;
	}

	/** Whether the god's rank is {@code needed} or higher (see config.yml's {@code ranks.unlocks}) */
	public boolean hasUnlocked(String godName, GodRank needed)
	{
		return RankRules.unlocked(getRank(godName), needed);
	}

	/** Which features are on, for the conditions that depend on them */
	private RankRules.Features features()
	{
		return new RankRules.Features(ctx.settings().sacrifices().enabled(), ctx.settings().quests().enabled(), ctx.settings().holyLand().enabled());
	}

	private RankRules.State state(God god)
	{
		return new RankRules.State(god.rank() != null ? god.rank() : GodRank.SPIRIT, god.rankSince(), god.priestSince(), god.ascendingSince());
	}

	/** How the god is doing now, for its conditions */
	private RankRules.Facts facts(String godName, God god)
	{
		BlockPos mainSign = god.mainAltar();
		Altar altar = mainSign != null ? ctx.altarRegistry().atSign(mainSign) : null;
		boolean hasMainAltar = altar != null && altar.kind() == AltarKind.PRAYING && godName.equals(altar.god());
		int altars = (int) ctx.altarRegistry().forGod(godName).stream().filter(standing -> standing.kind() == AltarKind.PRAYING).count();
		int holyLand = ctx.settings().holyLand().enabled() ? ctx.holyLandService().getClaimedChunksForGod(godName) : 0;
		return new RankRules.Facts(!ctx.priestService().getPriestsForGod(godName).isEmpty(), ctx.believerService().getBelieversForGod(godName).size(),
				ctx.godService().getGodPower(godName), ctx.moodService().getMoodForGod(godName), hasMainAltar, altars, holyLand, god.stats(), LegacyDates.SECONDS.now());
	}

	/**
	 * How far the god is with each condition of its next rank, or of its own rank when it has the highest; empty for a
	 * god that doesn't exist
	 */
	public List<RankRules.Progress> progress(String godName, GodRank rank)
	{
		God god = god(godName);
		if (god == null || !ctx.godService().godExist(godName))
		{
			return List.of();
		}
		return RankRules.progress(ctx.settings().ranks(), rank, state(god), facts(godName, god), features());
	}

	// ---------------------------------------------------------------------
	// Counting what the religion does

	/** A new god starts as a spirit, with its stats counted from now (it is never seeded) */
	public void newGod(String godName)
	{
		God god = god(godName);
		if (god == null)
		{
			return;
		}
		god.setRank(GodRank.SPIRIT);
		god.setRankSince(LegacyDates.SECONDS.now());
		god.setStats(GodStats.ZERO);
		god.setStatsSeeded(true);
		this.repository.markDirty();
	}

	/** A believer prayed at one of the god's altars */
	public void countPrayer(String godName)
	{
		count(godName, stats -> stats.withPrayers(stats.prayers() + 1));
	}

	/** The god was pleased with a sacrifice */
	public void countSacrifice(String godName)
	{
		count(godName, stats -> stats.withSacrifices(stats.sacrifices() + 1));
	}

	/** The god's believers completed a quest */
	public void countQuest(String godName)
	{
		count(godName, stats -> stats.withQuests(stats.quests() + 1));
	}

	/** A player joined the god by accepting an invitation */
	public void countConvert(String godName)
	{
		count(godName, stats -> stats.withConverts(stats.converts() + 1));
	}

	private void count(String godName, java.util.function.UnaryOperator<GodStats> change)
	{
		God god = god(godName);
		if (god == null || god.created() == null)
		{
			return;
		}
		god.setStats(change.apply(god.stats()));
		this.repository.markDirty();
	}

	// ---------------------------------------------------------------------
	// The aura

	/**
	 * On the god's turn: its believers in its own holy land feel its aura (config.yml's {@code ranks.aura}), a gentle
	 * effect that lasts until about its next turn, from the rank {@code ranks.unlocks.aura} names. Only while holy land
	 * is on.
	 */
	public void aura(String godName)
	{
		Settings.Aura aura = ctx.settings().ranks().aura();
		if (!aura.enabled() || !ctx.settings().holyLand().enabled() || !hasUnlocked(godName, ctx.settings().ranks().unlocks().aura()))
		{
			return;
		}
		PotionEffectType type = effectType(aura.effect());
		if (type == null)
		{
			return;
		}
		for (UUID believerId : ctx.believerService().getOnlineBelieversForGod(godName))
		{
			Player believer = ctx.plugin().getServer().getPlayer(believerId);
			if (believer == null || believer.isDead() || !ctx.plugin().isEnabledInWorld(believer.getWorld()))
			{
				continue;
			}
			if (godName.equals(ctx.holyLandService().getGodAtHolyLandLocation(believer.getLocation())))
			{
				// Ambient, without particles: a gentle effect, not a potion
				believer.addPotionEffect(new PotionEffect(type, aura.seconds() * 20, aura.amplifier(), true, false, true));
			}
		}
	}

	/** The effect with this id, or null (warned about once) */
	private PotionEffectType effectType(String id)
	{
		NamespacedKey key = NamespacedKey.fromString(id);
		PotionEffectType type = key != null ? Registry.EFFECT.get(key) : null;
		if (type == null && !id.equals(this.unknownEffect))
		{
			this.unknownEffect = id;
			ctx.plugin().getLogger().warning("Unknown effect '" + id + "' for ranks.aura.effect in config.yml; gods have no aura.");
		}
		return type;
	}

	// ---------------------------------------------------------------------
	// The god's turn

	/**
	 * On the god's turn: places a god from before ranks ({@link RankRules#seed}), else lets it rise or fall
	 * ({@link RankRules#evaluate}). Never while {@link com.dogonfire.gods.storage.Storage#cleanupVerdict()} says the
	 * believers can't be trusted: every god would look abandoned then.
	 */
	public void evaluate(String godName)
	{
		God god = god(godName);
		if (god == null || !ctx.godService().godExist(godName))
		{
			return;
		}
		if (ctx.storage().cleanupVerdict() != CleanupGuard.Verdict.ALLOWED)
		{
			return;
		}
		if (!god.statsSeeded())
		{
			seed(godName, god);
			return;
		}
		RankRules.State before = state(god);
		RankRules.Facts facts = facts(godName, god);
		RankRules.Outcome outcome = RankRules.evaluate(ctx.settings().ranks(), before, facts, features());
		switch (outcome.change())
		{
			case NONE -> store(god, outcome.state(), false);
			case UP -> rise(godName, god, before, outcome);
			case DOWN -> fall(godName, god, before, outcome, facts);
		}
	}

	/** A god from before ranks gets the rank it qualifies for and the stats that rank asks for */
	private void seed(String godName, God god)
	{
		GodRank floor = god.rank() != null ? god.rank() : GodRank.SPIRIT;
		if (god.descended() != null && floor.compareTo(GodRank.DESCENDED) < 0)
		{
			floor = GodRank.DESCENDED;
		}
		RankRules.Facts facts = facts(godName, god);
		RankRules.Seed seed = RankRules.seed(ctx.settings().ranks(), facts, features(), god.created(), floor);
		GodRank rank = seed.rank();
		boolean descends = rank.atLeast(GodRank.DESCENDED) && god.descended() == null;
		if (descends && ctx.fire(new GodDescendEvent(godName, ctx.altarRegistry().mainAltarLocation(godName))).isCancelled())
		{
			rank = GodRank.RECOGNIZED;
			descends = false;
		}
		god.setStats(seed.stats());
		god.setStatsSeeded(true);
		Instant now = facts.now();
		god.setPriestSince(facts.hasPriest() ? (god.priestSince() != null ? god.priestSince() : now) : null);
		god.setAscendingSince(null);
		setRank(god, rank, now);
		this.repository.save();
		ctx.plugin().log(godName + " is a god from before ranks: it starts at rank " + rank.key() + " (" + seed.stats() + ").");
		if (descends)
		{
			descended(godName);
		}
	}

	private void rise(String godName, God god, RankRules.State before, RankRules.Outcome outcome)
	{
		GodRank to = outcome.state().rank();
		boolean descends = to == GodRank.DESCENDED;
		if (descends && ctx.fire(new GodDescendEvent(godName, ctx.altarRegistry().mainAltarLocation(godName))).isCancelled())
		{
			// It tries again on a later turn
			store(god, new RankRules.State(before.rank(), before.rankSince(), outcome.state().priestSince(), null), false);
			return;
		}
		store(god, outcome.state(), true);
		ctx.plugin().log(godName + " rose to rank " + to.key() + ".");
		if (descends)
		{
			descended(godName);
		}
		ctx.altarTexts().refreshGod(godName);
	}

	private void fall(String godName, God god, RankRules.State before, RankRules.Outcome outcome, RankRules.Facts facts)
	{
		GodRank to = outcome.state().rank();
		store(god, outcome.state(), true);
		ctx.plugin().log(godName + " fell from rank " + before.rank().key() + " to " + to.key() + " (" + (outcome.reason() == RankRules.Reason.NO_PRIEST ? "no priest"
				: "too few believers") + ").");
		if (before.rank().atLeast(GodRank.DESCENDED) && !to.atLeast(GodRank.DESCENDED))
		{
			ctx.godNpcs().refreshGod(godName);
			ctx.fire(new GodAscendEvent(godName));
			ctx.godSpeech().sayToBelievers(godName, Voice.BELIEVERS_GOD_ASCENDED, 2);
		}
		ctx.altarTexts().refreshGod(godName);
	}

	/** The god descended to its main altar: its NPC appears with lightning and its effects, and it tells its believers */
	private void descended(String godName)
	{
		ctx.plugin().log(godName + " descended to its main altar.");
		ctx.godNpcs().descended(godName);
		ctx.godSpeech().sayToBelievers(godName, Voice.BELIEVERS_GOD_DESCENDED, 2);
	}

	/**
	 * Stores the state; a new rank is saved at once, anything else with the next autosave (and only when it changed).
	 * The god's {@code DescendedTime} follows its rank.
	 */
	private void store(God god, RankRules.State state, boolean rankChanged)
	{
		boolean same = state.rank() == god.rank() && java.util.Objects.equals(state.rankSince(), god.rankSince())
				&& java.util.Objects.equals(state.priestSince(), god.priestSince()) && java.util.Objects.equals(state.fallingSince(), god.ascendingSince());
		if (same)
		{
			return;
		}
		god.setPriestSince(state.priestSince());
		god.setAscendingSince(state.fallingSince());
		if (rankChanged || state.rank() != god.rank())
		{
			setRank(god, state.rank(), state.rankSince() != null ? state.rankSince() : LegacyDates.SECONDS.now());
			this.repository.save();
		}
		else
		{
			god.setRank(state.rank());
			god.setRankSince(state.rankSince());
			this.repository.markDirty();
		}
	}

	/** Sets the rank and keeps DescendedTime in line with it */
	private static void setRank(God god, GodRank rank, Instant since)
	{
		god.setRank(rank);
		god.setRankSince(since);
		if (rank.atLeast(GodRank.DESCENDED))
		{
			if (god.descended() == null)
			{
				god.setDescended(since);
			}
		}
		else
		{
			god.setDescended(null);
		}
	}
}
