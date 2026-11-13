package com.dogonfire.gods.service;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.api.event.BelieverDevotionChangeEvent;
import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.model.Believer;
import com.dogonfire.gods.model.DevotionLevel;
import com.dogonfire.gods.model.GodType;
import com.dogonfire.gods.storage.BelieverRepository;
import com.dogonfire.gods.util.LegacyDates;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;

/**
 * Believers' devotion to their god (config.yml's {@code devotion}): the points they earn serving it
 * ({@link #earn}), their level ({@link DevotionLevel}), fading on their god's turn ({@link
 * #update}), what they keep when they leave ({@link #leftGod}) and what their level unlocks ({@link
 * #grants}, {@link #allows}). A level change fires {@link BelieverDevotionChangeEvent}; a rise is
 * congratulated by the god with an effect around the believer. Believers from before devotion are
 * placed once when believers.yml is loaded ({@link DevotionRules#seed}). Devotion lives in
 * believers.yml ({@link BelieverRepository}); the rules are {@link DevotionRules}. Main thread
 * only.
 */
public class DevotionService {
  /** What a believer did for their god */
  public enum Deed {
    PRAYER,
    SACRIFICE,
    QUEST,
    CONVERT,
    HOLY_FOOD,
    UNHOLY_MOB;

    /** The points it earns (config.yml's devotion.points) */
    int points(Settings.DevotionPoints points) {
      return switch (this) {
        case PRAYER -> points.prayer();
        case SACRIFICE -> points.sacrifice();
        case QUEST -> points.quest();
        case CONVERT -> points.convert();
        case HOLY_FOOD -> points.holyFood();
        case UNHOLY_MOB -> points.unholyMob();
      };
    }

    /** Whether it is keeping a commandment, which earns points at most once per cooldown */
    boolean isCommandment() {
      return this == HOLY_FOOD || this == UNHOLY_MOB;
    }
  }

  private final GodsContext ctx;
  private final BelieverRepository repository;
  private final Random random = new Random();
  // When each believer last earned points for a commandment (not saved: a restart only forgives the
  // cooldown)
  private final Map<UUID, Instant> commandments = new HashMap<>();

  public DevotionService(GodsContext ctx, BelieverRepository repository) {
    this.ctx = ctx;
    this.repository = repository;
  }

  private Settings.Devotion settings() {
    return ctx.settings().devotion();
  }

  private static Instant now() {
    return LegacyDates.SECONDS.now();
  }

  /** The believer's entry if they believe in a god */
  private Believer believer(UUID believerId) {
    Believer believer = believerId != null ? this.repository.find(believerId) : null;
    return believer != null && believer.godName() != null ? believer : null;
  }

  // ---------------------------------------------------------------------
  // Seeding

  /**
   * Places every believer from before devotion (no {@code Devotion} in believers.yml) once: their
   * prayers to their god become points ({@link DevotionRules#seed}) and their level what those
   * reach. Quiet: no event and no words. Called after believers.yml is loaded.
   */
  public void seedAll() {
    int seeded = 0;
    for (Believer believer : this.repository.all()) {
      if (believer.devotion() == null) {
        seed(believer);
        seeded++;
      }
    }
    if (seeded > 0) {
      this.repository.markDirty();
      ctx.plugin()
          .log("Gave " + seeded + " believers from before devotion their starting devotion.");
    }
  }

  private void seed(Believer believer) {
    int points = DevotionRules.seed(settings(), believer.godName() != null, believer.prayers());
    believer.setDevotion(points);
    DevotionLevel level = DevotionRules.levelFor(settings(), points);
    believer.setDevotionLevel(level);
    believer.setDevotionSince(level != DevotionLevel.FOLLOWER ? now() : null);
  }

  // ---------------------------------------------------------------------
  // Reading

  /** Whether believers earn devotion at all (config.yml's devotion.enabled) */
  public boolean enabled() {
    return settings().enabled();
  }

  /** The believer's devotion points; 0 without a god */
  public int getPoints(UUID believerId) {
    Believer believer = believer(believerId);
    return believer != null && believer.devotion() != null ? believer.devotion() : 0;
  }

  /** The believer's devotion level; {@link DevotionLevel#FOLLOWER} without a god */
  public DevotionLevel getLevel(UUID believerId) {
    Believer believer = believer(believerId);
    return believer != null && believer.devotionLevel() != null
        ? believer.devotionLevel()
        : DevotionLevel.FOLLOWER;
  }

  /** When the believer reached their level, or null */
  public Instant getLevelSince(UUID believerId) {
    Believer believer = believer(believerId);
    return believer != null ? believer.devotionSince() : null;
  }

  /**
   * What a level is called by the believers of this god (its god type's name for it, themes.yml's
   * devotion)
   */
  public String levelName(String godName, DevotionLevel level) {
    GodType type =
        godName != null && ctx.godService().godExist(godName)
            ? ctx.godService().getDivineForceForGod(godName)
            : null;
    return ctx.themes().of(type).devotionNames().of(level);
  }

  /** What the believer's level is called; "" without a god */
  public String getLevelName(UUID believerId) {
    Believer believer = believer(believerId);
    return believer != null ? levelName(believer.godName(), getLevel(believerId)) : "";
  }

  /**
   * The believer's title: their level's name once their level has {@code devotion.unlocks.title},
   * else ""
   */
  public String getTitle(UUID believerId) {
    return grants(believerId, settings().unlocks().title()) ? getLevelName(believerId) : "";
  }

  // ---------------------------------------------------------------------
  // Unlocks

  /**
   * Whether the believer's level gives what needs {@code needed} (a bonus: a discount, more
   * blessings, praise, a title, priesthood). Never while devotion is off, nor without a god.
   */
  public boolean grants(UUID believerId, DevotionLevel needed) {
    return enabled()
        && believer(believerId) != null
        && DevotionRules.unlocked(getLevel(believerId), needed);
  }

  /**
   * Whether the believer may have what needs {@code needed} (a restriction, like holy artifacts).
   * Always while devotion is off.
   */
  public boolean allows(UUID believerId, DevotionLevel needed) {
    return !enabled() || DevotionRules.unlocked(getLevel(believerId), needed);
  }

  /** What /g prayfor costs the believer: less from devotion.unlocks.prayfor-discount on */
  public int prayForCost(UUID believerId, int cost) {
    return grants(believerId, settings().unlocks().prayForDiscount())
        ? DevotionRules.discountedCost(cost, settings().prayForDiscountPercent())
        : cost;
  }

  /**
   * The bound of a blessing's {@code random.nextInt(bound) == 0} roll for this believer: smaller
   * (likelier) from devotion.unlocks.more-blessings on
   */
  public int blessingBound(UUID believerId, int bound) {
    return grants(believerId, settings().unlocks().moreBlessings())
        ? DevotionRules.blessingBound(bound, settings().blessingChanceMultiplier())
        : bound;
  }

  // ---------------------------------------------------------------------
  // Earning

  /**
   * The believer did something for {@code godName}: earns the deed's points if it is their god
   * (commandments at most once per devotion.commandment-cooldown-minutes), and they rise if the
   * points reach a higher level. Nothing while devotion is off.
   */
  public void earn(UUID believerId, String godName, Deed deed) {
    Believer believer = believer(believerId);
    if (!enabled() || believer == null || godName == null || !godName.equals(believer.godName())) {
      return;
    }
    int points = deed.points(settings().points());
    if (points <= 0) {
      return;
    }
    if (deed.isCommandment()) {
      Instant now = now();
      if (!DevotionRules.commandmentReady(
          this.commandments.get(believerId), now, settings().commandmentCooldownMinutes())) {
        return;
      }
      this.commandments.put(believerId, now);
    }
    add(believer, points, BelieverDevotionChangeEvent.Cause.EARNED);
  }

  /**
   * Adds points (or takes them, never below 0) and lets the level follow ({@link
   * DevotionRules#evaluate}). Returns false if the believer has no god or devotion is off.
   */
  public boolean addPoints(UUID believerId, int points, BelieverDevotionChangeEvent.Cause cause) {
    Believer believer = believer(believerId);
    if (!enabled() || believer == null) {
      return false;
    }
    add(believer, points, cause);
    return true;
  }

  private void add(Believer believer, int points, BelieverDevotionChangeEvent.Cause cause) {
    fade(believer);
    believer.setDevotion(
        (int) Math.max(0, Math.min(Integer.MAX_VALUE, (long) current(believer) + points)));
    this.repository.markDirty();
    DevotionLevel from = level(believer);
    DevotionLevel to = DevotionRules.evaluate(settings(), from, believer.devotion());
    if (to != from) {
      change(
          believer,
          believer.godName(),
          from,
          to,
          to.compareTo(from) > 0
              ? cause
              : (cause == BelieverDevotionChangeEvent.Cause.API
                  ? cause
                  : BelieverDevotionChangeEvent.Cause.FADED));
    } else {
      ctx.chatService().refresh(believer.id());
    }
  }

  /**
   * Gives the believer a level (for the API), with the points it needs: at least its threshold for
   * a rise, exactly its threshold for a fall. Returns false if a plugin cancelled it, the believer
   * has no god or devotion is off.
   */
  public boolean setLevel(UUID believerId, DevotionLevel level) {
    Believer believer = believer(believerId);
    if (!enabled() || believer == null) {
      return false;
    }
    fade(believer);
    DevotionLevel from = level(believer);
    if (from == level) {
      return true;
    }
    int threshold = settings().threshold(level);
    int points = level.compareTo(from) > 0 ? Math.max(current(believer), threshold) : threshold;
    if (!change(believer, believer.godName(), from, level, BelieverDevotionChangeEvent.Cause.API)) {
      return false;
    }
    believer.setDevotion(points);
    this.repository.markDirty();
    return true;
  }

  private static int current(Believer believer) {
    return believer.devotion() != null ? believer.devotion() : 0;
  }

  private static DevotionLevel level(Believer believer) {
    return believer.devotionLevel() != null ? believer.devotionLevel() : DevotionLevel.FOLLOWER;
  }

  /**
   * Fades the believer's points for the days they didn't pray (quietly; the level is the caller's)
   */
  private void fade(Believer believer) {
    if (believer.devotion() == null) {
      seed(believer);
    }
    DevotionRules.Faded faded =
        DevotionRules.fade(
            settings(), current(believer), believer.lastPrayer(), believer.devotionFaded(), now());
    if (faded.points() != current(believer)
        || !java.util.Objects.equals(faded.fadedUntil(), believer.devotionFaded())) {
      believer.setDevotion(faded.points());
      believer.setDevotionFaded(faded.fadedUntil());
      this.repository.markDirty();
    }
  }

  // ---------------------------------------------------------------------
  // The god's turn

  /**
   * On the god's turn: the devotion of each of its believers (online or not) fades for the days
   * they didn't pray, and those whose points fell below what their level needs to be kept fall.
   * Nothing while devotion is off.
   */
  public void update(String godName) {
    if (!enabled()) {
      return;
    }
    List<Believer> believers = new ArrayList<>();
    for (Believer believer : this.repository.all()) {
      if (godName.equals(believer.godName())) {
        believers.add(believer);
      }
    }
    for (Believer believer : believers) {
      fade(believer);
      DevotionLevel from = level(believer);
      DevotionLevel to = DevotionRules.evaluate(settings(), from, current(believer));
      if (to.compareTo(from) < 0) {
        change(believer, godName, from, to, BelieverDevotionChangeEvent.Cause.FADED);
      }
    }
  }

  /**
   * Now and then on the god's turn: the god praises one of its believers online whose level has
   * devotion.unlocks.praise, by name, to its other believers online
   */
  public void praise(String godName) {
    if (!enabled()
        || this.random.nextInt(1 + 3000 / Math.max(1, ctx.godService().getVerbosityForGod(godName)))
            != 0) {
      return;
    }
    List<Player> praised = new ArrayList<>();
    for (UUID believerId : ctx.believerService().getOnlineBelieversForGod(godName)) {
      Player player = ctx.plugin().getServer().getPlayer(believerId);
      if (player != null && grants(believerId, settings().unlocks().praise())) {
        praised.add(player);
      }
    }
    if (praised.isEmpty()) {
      return;
    }
    Player saint = praised.get(this.random.nextInt(praised.size()));
    ctx.godSpeech()
        .sayToBelieversExcept(
            godName,
            Voice.BELIEVERS_DEVOTED_PRAISED,
            saint.getUniqueId(),
            Messages.player(saint.getName()),
            Messages.text("level", getLevelName(saint.getUniqueId())));
  }

  // ---------------------------------------------------------------------
  // Leaving

  /**
   * The believer left {@code oldGod} or changed to another god: they keep
   * devotion.kept-on-changing-god-percent of their points and the level those reach (a fall fires
   * {@link BelieverDevotionChangeEvent} with {@link BelieverDevotionChangeEvent.Cause#CHANGED_GOD},
   * about the old god). Their devotion counts for their new god from now on.
   */
  public void leftGod(UUID believerId, String oldGod) {
    Believer believer = believerId != null ? this.repository.find(believerId) : null;
    if (believer == null || oldGod == null) {
      return;
    }
    if (believer.devotion() == null) {
      seed(believer);
    }
    DevotionLevel from = level(believer);
    int kept = DevotionRules.afterLeaving(settings(), current(believer));
    believer.setDevotion(kept);
    believer.setDevotionFaded(null);
    DevotionLevel to = DevotionRules.levelFor(settings(), kept);
    this.repository.markDirty();
    if (to != from) {
      change(believer, oldGod, from, to, BelieverDevotionChangeEvent.Cause.CHANGED_GOD);
    }
  }

  // ---------------------------------------------------------------------

  /**
   * Changes the level: fires {@link BelieverDevotionChangeEvent} (a rise and an API change may be
   * cancelled), stores it, refreshes the chat snapshot and, for a believer online, has the god
   * congratulate them on a rise (with an effect around them) or tell them they fell. Returns false
   * if a plugin cancelled it.
   */
  private boolean change(
      Believer believer,
      String godName,
      DevotionLevel from,
      DevotionLevel to,
      BelieverDevotionChangeEvent.Cause cause) {
    if (ctx.fire(
            new BelieverDevotionChangeEvent(
                godName,
                believer.id(),
                ApiSnapshots.devotion(from),
                ApiSnapshots.devotion(to),
                cause))
        .isCancelled()) {
      return false;
    }
    believer.setDevotionLevel(to);
    believer.setDevotionSince(to != DevotionLevel.FOLLOWER ? now() : null);
    this.repository.markDirty();
    ctx.chatService().refresh(believer.id());

    Player player = ctx.plugin().getServer().getPlayer(believer.id());
    ctx.plugin()
        .log(
            (player != null ? player.getName() : believer.id().toString())
                + (to.compareTo(from) > 0 ? " rose" : " fell")
                + " from devotion level "
                + from.key()
                + " to "
                + to.key()
                + " ("
                + cause.name().toLowerCase(java.util.Locale.ROOT)
                + ").");
    if (player == null || cause == BelieverDevotionChangeEvent.Cause.CHANGED_GOD) {
      return true;
    }
    TagResolver level = Messages.text("level", levelName(godName, to));
    if (to.compareTo(from) > 0) {
      ctx.godSpeech()
          .say(
              godName,
              player,
              Voice.BELIEVER_DEVOTION_LEVEL_UP,
              2,
              Messages.player(player.getName()),
              level);
      if (ctx.settings().effects().enabled()) {
        ctx.effectService().devotionUp(godName, player, to);
      }
    } else {
      ctx.godSpeech()
          .say(
              godName,
              player,
              Voice.BELIEVER_DEVOTION_LEVEL_DOWN,
              2,
              Messages.player(player.getName()),
              level);
    }
    return true;
  }
}
