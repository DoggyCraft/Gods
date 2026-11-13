package com.dogonfire.gods.service;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.api.event.GodRelationChangeEvent;
import com.dogonfire.gods.api.event.HolyLandAttackStartEvent;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.model.ContestedLand;
import com.dogonfire.gods.model.God;
import com.dogonfire.gods.storage.GodRepository;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;

/**
 * Relations between gods: alliances and wars (priests toggle them with /g ally and /g war), and the
 * holy land a god attacks. {@link HolyLandService} owns the attack itself (on the land: who
 * attacks, kills, when it ends); this keeps the attacker's side in gods.yml and drops it once the
 * land no longer has this god as attacker. The rules are in {@link RelationRules}. Main thread
 * only.
 */
public class RelationService {
  private final GodsContext ctx;
  private final GodRepository repository;

  public RelationService(GodsContext ctx, GodRepository repository) {
    this.ctx = ctx;
    this.repository = repository;
  }

  private God god(String godName) {
    return this.repository.find(godName);
  }

  // ---------------------------------------------------------------------
  // Alliances and wars

  /** The god's allies (a copy) */
  public List<String> getAllianceRelations(String godName) {
    God god = god(godName);
    return god != null ? new ArrayList<String>(god.allies()) : new ArrayList<String>();
  }

  /** The gods it is at war with (a copy) */
  public List<String> getWarRelations(String godName) {
    God god = god(godName);
    return god != null ? new ArrayList<String>(god.enemies()) : new ArrayList<String>();
  }

  public boolean hasAllianceRelation(String godName, String otherGodName) {
    return getAllianceRelations(godName).contains(otherGodName);
  }

  public boolean hasWarRelation(String godName, String otherGodName) {
    return getWarRelations(godName).contains(otherGodName);
  }

  /**
   * Makes the two gods allies (ending any war between them), or ends their alliance, and tells both
   * gods' believers. Fires a {@link GodRelationChangeEvent}; does nothing if they are already (not)
   * allies.
   *
   * @return false if a listener cancelled it
   */
  public boolean setAlliance(String godName, String allyGodName, boolean allied) {
    if (hasAllianceRelation(godName, allyGodName) == allied) {
      return true;
    }
    if (ctx.fire(
            new GodRelationChangeEvent(
                godName, allyGodName, GodRelationChangeEvent.Relation.ALLIANCE, allied))
        .isCancelled()) {
      return false;
    }

    if (allied) {
      RelationRules.makeAllies(
          this.repository.forChange(godName), this.repository.forChange(allyGodName));
      this.repository.markDirty();
    } else {
      RelationRules.end(god(godName), godName, god(allyGodName), allyGodName, true);
      this.repository.save();
    }

    Voice voice = allied ? Voice.BELIEVERS_ALLIANCE : Voice.BELIEVERS_ALLIANCE_CANCELLED;
    ctx.godSpeech().sayToBelievers(allyGodName, voice, 10, Messages.player(godName));
    ctx.godSpeech().sayToBelievers(godName, voice, 10, Messages.player(allyGodName));
    return true;
  }

  /**
   * Puts the two gods at war (ending any alliance between them), or ends their war, and tells both
   * gods' believers. Fires a {@link GodRelationChangeEvent}; does nothing if they are already (not)
   * at war.
   *
   * @return false if a listener cancelled it
   */
  public boolean setWar(String godName, String enemyGodName, boolean atWar) {
    if (hasWarRelation(godName, enemyGodName) == atWar) {
      return true;
    }
    if (ctx.fire(
            new GodRelationChangeEvent(
                godName, enemyGodName, GodRelationChangeEvent.Relation.WAR, atWar))
        .isCancelled()) {
      return false;
    }

    if (atWar) {
      RelationRules.makeEnemies(
          this.repository.forChange(godName), this.repository.forChange(enemyGodName));
      this.repository.markDirty();
    } else {
      RelationRules.end(god(godName), godName, god(enemyGodName), enemyGodName, false);
      this.repository.save();
    }

    Voice voice = atWar ? Voice.BELIEVERS_WAR : Voice.BELIEVERS_WAR_CANCELLED;
    ctx.godSpeech().sayToBelievers(enemyGodName, voice, 10, Messages.player(godName));
    ctx.godSpeech().sayToBelievers(godName, voice, 10, Messages.player(enemyGodName));
    return true;
  }

  /** A god is removed: no other god is its ally or enemy any more */
  public void removeRelationsWith(String godName) {
    for (God other : this.repository.all()) {
      RelationRules.forget(other, godName);
    }
  }

  // ---------------------------------------------------------------------
  // The holy land a god attacks

  /**
   * A priest started an attack on the holy land at {@code contestedLand}. Fires a {@link
   * HolyLandAttackStartEvent}.
   *
   * @return false if a listener cancelled it
   */
  public boolean setContestedHolyLandForGod(String godName, Location contestedLand) {
    if (ctx.fire(
            new HolyLandAttackStartEvent(
                godName,
                ctx.holyLandService().getGodAtHolyLandLocation(contestedLand),
                contestedLand))
        .isCancelled()) {
      return false;
    }

    God god = this.repository.forChange(godName);
    if (god != null) {
      god.setContestedLand(
          new ContestedLand(
              contestedLand.getWorld().getName(),
              contestedLand.getBlockX(),
              contestedLand.getBlockY(),
              contestedLand.getBlockZ(),
              null));
    }

    ctx.holyLandService().setContestedLand(contestedLand, godName);

    this.repository.markDirty();
    return true;
  }

  public void clearContestedHolyLandForGod(String godName) {
    God god = god(godName);
    if (god != null) {
      GodRepository.clearContestedLand(god);
    }

    this.repository.markDirty();
  }

  /** Where the attack was started (the world is null if it isn't loaded, or there is no attack) */
  public Location getContestedHolyLandAttackLocationForGod(String godName) {
    God god = god(godName);
    ContestedLand land = god != null ? god.contestedLand() : null;
    if (land == null) {
      return new Location(null, 0, 0, 0);
    }

    return new Location(
        ctx.plugin().getServer().getWorld(land.world()), land.x(), land.y(), land.z());
  }

  /**
   * Whether godName is attacking a holy land right now. An attack whose land no longer has godName
   * as attacker (it was settled, abandoned or taken over, or comes from an old data file) is
   * cleared.
   */
  public boolean isAttackingHolyLand(String godName) {
    God god = god(godName);
    if (god == null || god.contestedLand() == null) {
      return false;
    }
    Location location = getContestedHolyLandAttackLocationForGod(godName);
    if (!ctx.holyLandService().isAttackingHolyLand(godName, location)) {
      clearContestedHolyLandForGod(godName);
      return false;
    }
    return true;
  }
}
