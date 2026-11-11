package com.dogonfire.gods.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Gods' API for other plugins. Gods registers it in Bukkit's services manager while it is enabled:
 *
 * <pre>{@code
 * GodsApi gods = Bukkit.getServicesManager().load(GodsApi.class);
 * if (gods != null) {
 *     gods.getGod(player).ifPresent(god -> player.sendMessage("You believe in " + god.name()));
 * }
 * }</pre>
 *
 * Add Gods to {@code softdepend} (or {@code depend}) in your plugin.yml so it is enabled first, and look the service up
 * again after Gods is re-enabled. Only call the API from the server thread; other threads get an
 * {@link IllegalStateException}.
 * <p>
 * Gods are named by players. Methods that take a god's name ignore its case ({@code "zeus"} finds {@code Zeus});
 * {@link GodInfo#name()} is how Gods writes it.
 */
public interface GodsApi
{
	// ---------------------------------------------------------------------
	// Gods

	/** The names of every god, in the order they were created */
	List<String> getGodNames();

	/** A snapshot of every god, in the order they were created */
	List<GodInfo> getGods();

	/** A god by its name (in any case), if it exists */
	Optional<GodInfo> getGod(String godName);

	/** The god the player believes in, if any */
	Optional<GodInfo> getGod(Player player);

	// ---------------------------------------------------------------------
	// Believers

	/** The player as a believer, online or not; empty if they believe in no god */
	Optional<BelieverInfo> getBeliever(UUID playerId);

	// ---------------------------------------------------------------------
	// Holy land

	/**
	 * The god whose holy land the location is in (holy land is claimed per 128x128 block region), if any. Always empty
	 * while holy land is switched off in Gods' config.
	 */
	Optional<String> getHolyLandGod(Location location);

	/**
	 * Whether the location is in neutral land, where nobody can fight or claim holy land. Always false while holy land
	 * is switched off in Gods' config.
	 */
	boolean isNeutralLand(Location location);
}
