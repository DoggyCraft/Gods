package com.dogonfire.gods.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
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
	 * god has a list of lines for each, in the voice of its type and gender, and says one at random (a non-binary god
	 * speaks each line with its type's male or female voice, picked at random).
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

	// ---------------------------------------------------------------------
	// Acting as a god
	//
	// Each action goes through the same code as the game itself: the same effects, messages and events. An action that
	// fires a cancellable event returns CANCELLED when a listener cancels it. The god says nothing unless noted: make it
	// speak with say or sayVoiceLine.

	/**
	 * The god blesses the player with a good potion effect for a while (longer the more powerful the god is). Fires a
	 * {@link com.dogonfire.gods.api.event.GodBlessPlayerEvent}.
	 *
	 * @return {@link ActionResult#SUCCESS}, {@link ActionResult#NO_SUCH_GOD}, {@link ActionResult#FEATURE_DISABLED}
	 *         (blessings are off), {@link ActionResult#NOT_POSSIBLE} (the player is offline or was blessed too recently) or
	 *         {@link ActionResult#CANCELLED}
	 */
	ActionResult bless(String godName, Player player);

	/**
	 * The god curses the player: a bad potion effect, or lightning or hostile mobs when the config allows them. Fires a
	 * {@link com.dogonfire.gods.api.event.GodCursePlayerEvent}.
	 *
	 * @return {@link ActionResult#SUCCESS}, {@link ActionResult#NO_SUCH_GOD}, {@link ActionResult#FEATURE_DISABLED}
	 *         (curses are off), {@link ActionResult#NOT_POSSIBLE} (the player is offline or was cursed too recently) or
	 *         {@link ActionResult#CANCELLED}
	 */
	ActionResult curse(String godName, Player player);

	/**
	 * The god heals the player and says so (its {@code believer-health-blessing} line). Fires a
	 * {@link com.dogonfire.gods.api.event.GodBlessPlayerEvent}.
	 *
	 * @return {@link ActionResult#SUCCESS}, {@link ActionResult#NO_SUCH_GOD} or {@link ActionResult#CANCELLED}
	 */
	ActionResult heal(String godName, Player player);

	/**
	 * The god drops one item of the material in front of the player and says so (its {@code believer-item-blessing}
	 * line). Fires a {@link com.dogonfire.gods.api.event.GodBlessPlayerEvent}. Nothing drops if a block is in the way.
	 *
	 * @param material an item material
	 * @return {@link ActionResult#SUCCESS}, {@link ActionResult#NO_SUCH_GOD} or {@link ActionResult#CANCELLED}
	 * @throws IllegalArgumentException if the material is not an item
	 */
	ActionResult giveItem(String godName, Player player, Material material);

	/**
	 * The god drops a holy artifact of its type in front of the player and says so. Fires a
	 * {@link com.dogonfire.gods.api.event.GodBlessPlayerEvent}.
	 *
	 * @return {@link ActionResult#SUCCESS}, {@link ActionResult#NO_SUCH_GOD}, {@link ActionResult#FEATURE_DISABLED}
	 *         (holy artifacts are off), {@link ActionResult#NOT_POSSIBLE} (the player is offline or carries one
	 *         already, Gods is not enabled in their world, or the god has no type yet) or {@link ActionResult#CANCELLED}
	 */
	ActionResult giveHolyArtifact(String godName, Player player);

	/**
	 * The god strikes the player with lightning. Fires a {@link com.dogonfire.gods.api.event.GodCursePlayerEvent}. The
	 * lightning sets no fire and changes no creature; holy land and neutral land protect against it as against lightning.
	 *
	 * @param damage 1 hurts as much as a bolt of lightning, each point more half a heart more; 0 or less only looks like
	 *            lightning
	 * @return {@link ActionResult#SUCCESS}, {@link ActionResult#NO_SUCH_GOD} or {@link ActionResult#CANCELLED}
	 */
	ActionResult smite(String godName, Player player, int damage);

	/**
	 * Pleases (a positive amount) or angers (a negative amount) the god; its mood stays within -100 to 100. Believers
	 * pleasing it adds 5 to 14 (depending on the god), angering it takes 1. Fires a
	 * {@link com.dogonfire.gods.api.event.GodMoodChangeEvent}.
	 *
	 * @return {@link ActionResult#SUCCESS}, {@link ActionResult#NO_SUCH_GOD} or {@link ActionResult#CANCELLED}
	 */
	ActionResult addMood(String godName, float amount);

	/**
	 * Changes the god's gender, as a priest's {@code /g gender} does: the god tells its believers in its new voice and
	 * its title changes ("Deity of the Sea"). Unlike the command, this ignores the cooldown between changes in Gods'
	 * config, but it does start a new one for the god's priests. Fires a
	 * {@link com.dogonfire.gods.api.event.GodGenderChangeEvent} if it changes anything.
	 *
	 * @param gender {@link GodGender#MALE}, {@link GodGender#FEMALE} or {@link GodGender#NON_BINARY}
	 * @return {@link ActionResult#SUCCESS} (also if the god has that gender already), {@link ActionResult#NO_SUCH_GOD},
	 *         {@link ActionResult#NOT_POSSIBLE} (the gender is {@link GodGender#NONE}, which only gods from before
	 *         genders could be chosen have) or {@link ActionResult#CANCELLED}
	 * @throws NullPointerException if {@code gender} is null
	 */
	ActionResult setGender(String godName, GodGender gender);

	/**
	 * Gives the believer prayer power (or, with a negative amount, takes it; it never goes below 0), which they spend
	 * on {@code /g prayfor}. They are told how much they have now. A god's own power comes from its believers' prayers
	 * and can't be set.
	 *
	 * @return {@link ActionResult#SUCCESS} or {@link ActionResult#NOT_BELIEVER} (the player believes in no god)
	 */
	ActionResult addPrayerPower(UUID playerId, int amount);

	/**
	 * The god gives its believers a new quest of a kind the config allows, and tells them. Fires a
	 * {@link com.dogonfire.gods.api.event.QuestStartEvent}.
	 *
	 * @return {@link ActionResult#SUCCESS}, {@link ActionResult#NO_SUCH_GOD}, {@link ActionResult#FEATURE_DISABLED}
	 *         (quests are off), {@link ActionResult#NOT_POSSIBLE} (it has a quest, or no kind of quest could be made)
	 *         or {@link ActionResult#CANCELLED}
	 */
	ActionResult startQuest(String godName);

	/**
	 * The god's quest is completed, as if its believers had done it: the god says so, is pleased and rewards the
	 * believers who are online. Fires a {@link com.dogonfire.gods.api.event.QuestCompleteEvent}.
	 *
	 * @return {@link ActionResult#SUCCESS}, {@link ActionResult#NO_SUCH_GOD} or {@link ActionResult#NOT_POSSIBLE} (it
	 *         has no quest)
	 */
	ActionResult completeQuest(String godName);

	/**
	 * The god's quest fails, as if its time had run out: the god is angered and says so. Fires a
	 * {@link com.dogonfire.gods.api.event.QuestFailEvent}.
	 *
	 * @return {@link ActionResult#SUCCESS}, {@link ActionResult#NO_SUCH_GOD} or {@link ActionResult#NOT_POSSIBLE} (it
	 *         has no quest)
	 */
	ActionResult failQuest(String godName);

	/**
	 * The god invites the player to become its believer, as a priest's {@code /g invite} does; the player answers with
	 * {@code /g yes} (which fires a {@link com.dogonfire.gods.api.event.BelieverJoinGodEvent}) or {@code /g no}.
	 *
	 * @return {@link ActionResult#SUCCESS}, {@link ActionResult#NO_SUCH_GOD} or {@link ActionResult#NOT_POSSIBLE} (the
	 *         player believes in the god already)
	 */
	ActionResult invite(String godName, Player player);

	/**
	 * The god offers the believer to become its priest; they answer with {@code /g yes} (which fires a
	 * {@link com.dogonfire.gods.api.event.PriestAssignEvent}) or {@code /g no}. A god makes at most one offer every 3
	 * minutes.
	 *
	 * @return {@link ActionResult#SUCCESS}, {@link ActionResult#NO_SUCH_GOD}, {@link ActionResult#NOT_BELIEVER} or
	 *         {@link ActionResult#NOT_POSSIBLE} (they are a priest already, or the god made an offer too recently)
	 */
	ActionResult offerPriesthood(String godName, Player player);

	/**
	 * Makes the believer one of the god's priests right away (running the priest command in Gods' config). Fires a
	 * {@link com.dogonfire.gods.api.event.PriestAssignEvent}.
	 *
	 * @return {@link ActionResult#SUCCESS} (also if they are a priest already), {@link ActionResult#NO_SUCH_GOD},
	 *         {@link ActionResult#NOT_BELIEVER} or {@link ActionResult#CANCELLED}
	 */
	ActionResult assignPriest(String godName, UUID playerId);

	/**
	 * The priest is no longer one of the god's priests (running the priest removal command in Gods' config). Fires a
	 * {@link com.dogonfire.gods.api.event.PriestRemoveEvent}.
	 *
	 * @return {@link ActionResult#SUCCESS}, {@link ActionResult#NO_SUCH_GOD} or {@link ActionResult#NOT_POSSIBLE} (they
	 *         are not its priest)
	 */
	ActionResult removePriest(String godName, UUID playerId);

	/**
	 * Makes the two gods allies (ending a war between them) or ends their alliance, and tells both gods' believers, as
	 * a priest's {@code /g ally} does. Fires a {@link com.dogonfire.gods.api.event.GodRelationChangeEvent} if it
	 * changes anything.
	 *
	 * @return {@link ActionResult#SUCCESS} (also if they already were or weren't allies), {@link ActionResult#NO_SUCH_GOD}
	 *         (either god), {@link ActionResult#NOT_POSSIBLE} (it is the same god) or {@link ActionResult#CANCELLED}
	 */
	ActionResult setAlliance(String godName, String otherGodName, boolean allied);

	/**
	 * Puts the two gods at war (ending an alliance between them) or ends their war, and tells both gods' believers, as
	 * a priest's {@code /g war} does. Fires a {@link com.dogonfire.gods.api.event.GodRelationChangeEvent} if it changes
	 * anything.
	 *
	 * @return {@link ActionResult#SUCCESS} (also if they already were or weren't at war), {@link ActionResult#NO_SUCH_GOD}
	 *         (either god), {@link ActionResult#NOT_POSSIBLE} (it is the same god) or {@link ActionResult#CANCELLED}
	 */
	ActionResult setWar(String godName, String otherGodName, boolean atWar);
}
