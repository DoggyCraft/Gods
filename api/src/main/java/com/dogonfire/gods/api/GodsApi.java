package com.dogonfire.gods.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

	// ---------------------------------------------------------------------
	// Speaking as a god

	/**
	 * The situations gods have voice lines for: the keys of Gods' voice files ({@code lang/<language>/voices/}), such as
	 * {@code believer-praying}, {@code believer-random-pleased-speech} or {@code believers-slay-quest-started}. Each
	 * god has a list of lines for each, in the voice of its type and gender, and says one at random.
	 */
	Set<String> getVoiceSituations();

	/**
	 * The god says a text to the player, formatted like everything else it says. Like all of a god's words it fires a
	 * {@link com.dogonfire.gods.api.event.GodSpeakEvent} and is sent a tick later, only to a player who has the
	 * {@code gods.listen} permission and is in a world Gods is enabled in.
	 *
	 * @param text the words, in MiniMessage format (e.g. {@code "Kneel, <player>!"}); {@code <god>} is the god's name and
	 *             {@code <player>} the player's. Gods parses it, so don't put players' input in it unescaped.
	 * @return false if there is no such god, the player can't hear gods, or a listener cancelled the event
	 */
	boolean say(String godName, Player player, String text);

	/**
	 * The god says a text to each of its believers who is online, like {@link #say(String, Player, String)}.
	 *
	 * @return how many believers it was said to; 0 if there is no such god
	 */
	int sayToBelievers(String godName, String text);

	/**
	 * The god says one of its voice lines for the situation to the player, like {@link #say(String, Player, String)}.
	 *
	 * @param situation one of {@link #getVoiceSituations()}
	 * @return false if there is no such god, it has no line for the situation, the player can't hear gods, or a
	 *         listener cancelled the event
	 * @throws IllegalArgumentException if the situation is not one of {@link #getVoiceSituations()}
	 */
	default boolean sayVoiceLine(String godName, Player player, String situation)
	{
		return sayVoiceLine(godName, player, situation, Map.of());
	}

	/**
	 * Like {@link #sayVoiceLine(String, Player, String)}, filling in the line's placeholders.
	 *
	 * @param placeholders values for the line's tags, by tag name (lower case, e.g. {@code amount} or {@code type});
	 *                     the values are inserted as plain text. {@code god} is the god's name and {@code player} the
	 *                     player's unless given here. Lines whose tags get no value show them as they are written.
	 * @throws IllegalArgumentException if the situation is not one of {@link #getVoiceSituations()}, or a placeholder
	 *                                  name is not a valid tag name ({@code a-z}, {@code 0-9}, {@code _} and {@code -})
	 */
	boolean sayVoiceLine(String godName, Player player, String situation, Map<String, String> placeholders);

	/**
	 * One of the god's voice lines for the situation, picked at random and rendered (with {@code §} colors, without the
	 * chat format), for showing it some other way, e.g. as an NPC's dialogue. {@code <god>} is the god's name; other
	 * tags are shown as they are written.
	 *
	 * @return empty if there is no such god or it has no line for the situation
	 * @throws IllegalArgumentException if the situation is not one of {@link #getVoiceSituations()}
	 */
	default Optional<String> getVoiceLine(String godName, String situation)
	{
		return getVoiceLine(godName, situation, Map.of());
	}

	/**
	 * Like {@link #getVoiceLine(String, String)}, filling in the line's placeholders as
	 * {@link #sayVoiceLine(String, Player, String, Map)} does.
	 *
	 * @throws IllegalArgumentException if the situation is not one of {@link #getVoiceSituations()}, or a placeholder
	 *                                  name is not a valid tag name
	 */
	Optional<String> getVoiceLine(String godName, String situation, Map<String, String> placeholders);
}
