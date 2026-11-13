package com.dogonfire.gods.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One player's entry in marriages.yml: where they are on the way from a proposal to a marriage. A
 * proposal (sent with /g marry, accepted by praying) makes both players engaged ("getting
 * married"); they marry when both have picked up a wedding token. Each step is kept on both
 * partners' entries. A plain data object: {@code MarriageService} changes it.
 */
public final class Marriage {
  /** A proposal, valid for a short time ({@code MarriageProposal.Partner/Time}) */
  public record Proposal(UUID partner, Instant time) {}

  /** Engaged ({@code GettingMarried.Partner/Time/HasPickupWeddingToken}) */
  public record Engagement(UUID partner, Instant since, boolean tokenPickedUp) {}

  /** Married ({@code Married.Partner/Time/LastLove}) */
  public record Wedlock(UUID partner, Instant since, Instant lastLove) {}

  // The sections in marriages.yml. One whose partner is not a UUID (very old files) is kept in the
  // extras as it is,
  // until it is replaced
  public static final String PROPOSAL = "MarriageProposal";
  public static final String ENGAGEMENT = "GettingMarried";
  public static final String WEDLOCK = "Married";

  private final UUID playerId;
  private Proposal proposal;
  private Engagement engagement;
  private Wedlock wedlock;

  /** Keys of the entry this version doesn't know, written back unchanged */
  private Map<String, Object> extras = new LinkedHashMap<String, Object>();

  public Marriage(UUID playerId) {
    this.playerId = playerId;
  }

  public UUID playerId() {
    return this.playerId;
  }

  public Proposal proposal() {
    return this.proposal;
  }

  public void setProposal(Proposal proposal) {
    this.extras.remove(PROPOSAL);
    this.proposal = proposal;
  }

  public Engagement engagement() {
    return this.engagement;
  }

  public void setEngagement(Engagement engagement) {
    this.extras.remove(ENGAGEMENT);
    this.engagement = engagement;
  }

  public Wedlock wedlock() {
    return this.wedlock;
  }

  public void setWedlock(Wedlock wedlock) {
    this.extras.remove(WEDLOCK);
    this.wedlock = wedlock;
  }

  public Map<String, Object> extras() {
    return this.extras;
  }

  public void setExtras(Map<String, Object> extras) {
    this.extras = extras;
  }
}
