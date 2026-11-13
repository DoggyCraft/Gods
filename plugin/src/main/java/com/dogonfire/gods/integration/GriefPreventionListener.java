package com.dogonfire.gods.integration;

import com.dogonfire.gods.GodsContext;
import java.util.Objects;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.events.ClaimChangeEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimDeletedEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimTransferEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Claim lands (/g holyclaim) follow their GriefPrevention claims: a deleted claim, a claim given to
 * someone who is not a priest of the land's god, and a claim resized so that no altar of the god is
 * left in it stop being holy land ({@code HolyLandService.checkClaimLand}, {@code
 * claimTransferred}). Registered while holy land is enabled and GriefPrevention is; only created
 * then, since it references GriefPrevention's classes.
 */
public final class GriefPreventionListener implements Listener {
  private final GodsContext ctx;

  public GriefPreventionListener(GodsContext ctx) {
    this.ctx = ctx;
  }

  /**
   * Deleted claims (abandoned, expired, deleted by an admin): checked a tick later, when
   * GriefPrevention is done
   */
  @EventHandler(priority = EventPriority.MONITOR)
  public void onClaimDeleted(ClaimDeletedEvent event) {
    checkLater(event.getClaim(), false);
  }

  /**
   * Resized claims (ClaimResizeEvent and ClaimModifiedEvent are ClaimChangeEvents): an altar must
   * still stand in it
   */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onClaimChanged(ClaimChangeEvent event) {
    checkLater(event.getFrom(), true);
    // A resize keeps the claim's id; check the new one too should it differ
    if (event.getTo() != null
        && event.getFrom() != null
        && !Objects.equals(event.getTo().getID(), event.getFrom().getID())) {
      checkLater(event.getTo(), true);
    }
  }

  /** A claim given to another player (or made an admin claim, a null owner) */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onClaimTransferred(ClaimTransferEvent event) {
    Claim claim = event.getClaim();
    if (claim == null
        || claim.getID() == null
        || !this.ctx.holyLandService().isClaimLand(claim.getID())) {
      return;
    }
    this.ctx.holyLandService().claimTransferred(claim.getID(), event.getNewOwner());
  }

  private void checkLater(Claim claim, boolean altarRule) {
    if (claim == null
        || claim.getID() == null
        || !this.ctx.holyLandService().isClaimLand(claim.getID())) {
      return;
    }
    long id = claim.getID();
    this.ctx
        .plugin()
        .getServer()
        .getScheduler()
        .runTask(this.ctx.plugin(), () -> this.ctx.holyLandService().checkClaimLand(id, altarRule));
  }
}
