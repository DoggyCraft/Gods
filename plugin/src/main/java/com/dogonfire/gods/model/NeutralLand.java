package com.dogonfire.gods.model;

import java.time.Instant;

/**
 * A region set as neutral land with /g setsafe: no building, no PvP, no hostile mobs. In holyland.yml it is
 * {@code NeutralLands.<world key>.<hash>: <time set>}.
 *
 * @param since when it was set, or null if the file has no readable time (it is still neutral)
 */
public record NeutralLand(RegionKey region, Instant since)
{
}
