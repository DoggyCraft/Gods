package com.dogonfire.gods.api;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;

/**
 * A snapshot of a god, as it was when it was fetched. It doesn't change when the god does; fetch it again for fresh
 * data, and use {@link GodsApi} to make changes.
 *
 * @param name           the god's name, as Gods writes it (first letter upper case)
 * @param type           the god's divine force
 * @param gender         the god's gender
 * @param mood           how the god feels
 * @param moodValue      the god's exact mood, from -100 (furious) to 100 (delighted); see {@link GodMood}
 * @param power          the god's power, from its believers' prayers
 * @param believers      every player who believes in the god, online or not
 * @param priests        the god's priests, in the order they became priests
 * @param allies         the gods this god is allied with
 * @param enemies        the gods this god is at war with
 * @param description    what its priests wrote about it, or null if they wrote nothing
 * @param home           where the god lives (its first altar, or where a priest set it), or null if that world isn't
 *                       loaded
 * @param holyLandClaims how many regions of holy land the god has claimed (0 when holy land is switched off)
 * @param quest          the god's current quest, or null if it has none
 * @param created        when the god was created, or null if that isn't known
 */
public record GodInfo(String name, GodType type, GodGender gender, GodMood mood, float moodValue, float power,
		Set<UUID> believers, List<UUID> priests, List<String> allies, List<String> enemies, String description,
		Location home, int holyLandClaims, QuestInfo quest, Instant created)
{
	public GodInfo
	{
		believers = Set.copyOf(believers);
		priests = List.copyOf(priests);
		allies = List.copyOf(allies);
		enemies = List.copyOf(enemies);
		home = home != null ? home.clone() : null;
	}

	/** A copy, so changing it doesn't change the snapshot */
	@Override
	public Location home()
	{
		return this.home != null ? this.home.clone() : null;
	}

	/** Whether the player believes in the god */
	public boolean isBeliever(UUID playerId)
	{
		return this.believers.contains(playerId);
	}

	/** Whether the player is one of the god's priests */
	public boolean isPriest(UUID playerId)
	{
		return this.priests.contains(playerId);
	}
}
