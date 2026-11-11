package com.dogonfire.gods.api;

import java.time.Instant;
import java.util.UUID;

/**
 * A snapshot of a player who believes in a god, as it was when it was fetched.
 *
 * @param playerId    the player
 * @param god         the god they believe in
 * @param prayers     how many times they prayed to it; their prayers make up the god's power
 * @param prayerPower what they can spend on {@code /g prayfor} (blessings, items, health, quests)
 * @param priest      whether they are a priest of the god
 * @param joined      when they joined the god, or null if that isn't known
 * @param lastPrayer  when they last prayed, or null if never
 */
public record BelieverInfo(UUID playerId, String god, int prayers, int prayerPower, boolean priest, Instant joined,
		Instant lastPrayer)
{
}
