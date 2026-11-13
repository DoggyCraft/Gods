package com.dogonfire.gods.model;

import java.util.Date;
import java.util.UUID;

/**
 * Two married players, as listed by /g marriages and talked about by gods.
 *
 * @param godName the god of {@code player1Id}
 * @param lastLove when they last used /g love
 */
public record MarriedCouple(UUID player1Id, UUID player2Id, String godName, Date lastLove) {}
