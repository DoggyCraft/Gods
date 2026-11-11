package com.dogonfire.gods.message;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The situations a god speaks in: one key of the voice files ({@code lang/<language>/voices/<type>_<gender>.yml}),
 * each a list of lines the god picks one of. The key names say who is spoken to: {@code believer-} one believer,
 * {@code believers-} all of them, {@code priest-} the priest(s), {@code player-} anyone; the {@code *-bible-text}
 * keys are pages of a holy book. Every key here must be in all 26 bundled voice files ({@code MessageKeysTest}).
 */
public enum Voice
{
	DEFAULT_BIBLE_TEXT_1("default-bible-text-1"),
	DEFAULT_BIBLE_TEXT_2("default-bible-text-2"),
	DEFAULT_BIBLE_TEXT_3("default-bible-text-3"),
	DEFAULT_BIBLE_TEXT_4("default-bible-text-4"),
	DEFAULT_BIBLE_TEXT_5("default-bible-text-5"),
	DEFAULT_BIBLE_TEXT_6("default-bible-text-6"),
	DEFAULT_BIBLE_TEXT_7("default-bible-text-7"),
	BELIEVER_ACCEPTED_MARRIAGE_PROPOSAL("believer-accepted-marriage-proposal"),
	BELIEVER_ACCEPTED_YOUR_MARRIAGE_PROPOSAL("believer-accepted-your-marriage-proposal"),
	BELIEVER_ALTAR_BUILT("believer-altar-built"),
	/** A new god, created at an altar without a gender word on the sign, chose its gender */
	BELIEVER_GOD_CHOSE_GENDER("believer-god-chose-gender"),
	BELIEVER_BAD_SACRIFICE("believer-bad-sacrifice"),
	BELIEVER_CURSED_ANGRY("believer-cursed-angry"),
	BELIEVER_GOOD_SACRIFICE("believer-good-sacrifice"),
	BELIEVER_HEALTH_BLESSING("believer-health-blessing"),
	BELIEVER_HOLY_ARTIFACT_BLESSING("believer-holy-artifact-blessing"),
	BELIEVER_HOLY_FOOD_SACRIFICE("believer-holy-food-sacrifice"),
	BELIEVER_ITEM_BLESSING("believer-item-blessing"),
	BELIEVER_MARRIAGE_PARTNER_TOKEN_PICKUP("believer-marriage-partner-token-pickup"),
	BELIEVER_MARRIAGE_PROPOSAL("believer-marriage-proposal"),
	BELIEVER_MARRIAGE_TOKEN_PICKUP("believer-marriage-token-pickup"),
	BELIEVER_MARRIED("believer-married"),
	BELIEVER_MARRIED_COUPLE("believer-married-couple"),
	BELIEVER_MEH_SACRIFICE("believer-meh-sacrifice"),
	BELIEVER_NO_QUESTION("believer-no-question"),
	BELIEVER_OFFER_PRIEST("believer-offer-priest"),
	BELIEVER_PRAYER_RECENT_ITEM_BLESSING("believer-prayer-recent-item-blessing"),
	BELIEVER_PRAYER_TOO_SOON("believer-prayer-too-soon"),
	BELIEVER_PRAYER_WHEN_NO_ITEM_NEED("believer-prayer-when-no-item-need"),
	BELIEVER_PRAYING("believer-praying"),
	BELIEVER_PRIEST_REJECTED("believer-priest-rejected"),
	BELIEVER_RANDOM_ANGRY_SPEECH("believer-random-angry-speech"),
	BELIEVER_RANDOM_DISPLEASED_SPEECH("believer-random-displeased-speech"),
	BELIEVER_RANDOM_EXALTED_SPEECH("believer-random-exalted-speech"),
	BELIEVER_RANDOM_NEUTRAL_SPEECH("believer-random-neutral-speech"),
	BELIEVER_RANDOM_PLEASED_SPEECH("believer-random-pleased-speech"),
	BELIEVERS_ALLIANCE("believers-alliance"),
	BELIEVERS_ALLIANCE_CANCELLED("believers-alliance-cancelled"),
	BELIEVERS_ATTACK_HOLY_LAND_FAILED("believers-attack-holy-land-failed"),
	BELIEVERS_ATTACK_HOLY_LAND_SUCCESS("believers-attack-holy-land-success"),
	BELIEVERS_ATTACK_STARTED("believers-attack-started"),
	BELIEVERS_BLESSED_PLAYER_SET("believers-blessed-player-set"),
	BELIEVERS_BLESSED_PLAYER_UNSET("believers-blessed-player-unset"),
	BELIEVERS_BUILD_ALTARS_QUEST_COMPLETED("believers-build-altars-quest-completed"),
	BELIEVERS_BUILD_ALTARS_QUEST_FAILED("believers-build-altars-quest-failed"),
	BELIEVERS_BUILD_ALTARS_QUEST_PROGRESS("believers-build-altars-quest-progress"),
	BELIEVERS_BUILD_ALTARS_QUEST_STARTED("believers-build-altars-quest-started"),
	BELIEVERS_BUILD_ALTARS_QUEST_STATUS("believers-build-altars-quest-status"),
	BELIEVERS_BUILD_TOWER_QUEST_COMPLETED("believers-build-tower-quest-completed"),
	BELIEVERS_BUILD_TOWER_QUEST_FAILED("believers-build-tower-quest-failed"),
	BELIEVERS_BUILD_TOWER_QUEST_PROGRESS("believers-build-tower-quest-progress"),
	BELIEVERS_BUILD_TOWER_QUEST_STARTED("believers-build-tower-quest-started"),
	BELIEVERS_BUILD_TOWER_QUEST_STATUS("believers-build-tower-quest-status"),
	BELIEVERS_BURN_BIBLES_QUEST_COMPLETED("believers-burn-bibles-quest-completed"),
	BELIEVERS_BURN_BIBLES_QUEST_FAILED("believers-burn-bibles-quest-failed"),
	BELIEVERS_BURN_BIBLES_QUEST_PROGRESS("believers-burn-bibles-quest-progress"),
	BELIEVERS_BURN_BIBLES_QUEST_STARTED("believers-burn-bibles-quest-started"),
	BELIEVERS_BURN_BIBLES_QUEST_STATUS("believers-burn-bibles-quest-status"),
	BELIEVERS_CLAIMED_HOLY_LAND("believers-claimed-holy-land"),
	BELIEVERS_CLAIM_HOLY_LAND_QUEST_COMPLETED("believers-claim-holy-land-quest-completed"),
	BELIEVERS_CLAIM_HOLY_LAND_QUEST_FAILED("believers-claim-holy-land-quest-failed"),
	BELIEVERS_CLAIM_HOLY_LAND_QUEST_PROGRESS("believers-claim-holy-land-quest-progress"),
	BELIEVERS_CLAIM_HOLY_LAND_QUEST_STARTED("believers-claim-holy-land-quest-started"),
	BELIEVERS_CLAIM_HOLY_LAND_QUEST_STATUS("believers-claim-holy-land-quest-status"),
	BELIEVERS_CONVERT_QUEST_COMPLETED("believers-convert-quest-completed"),
	BELIEVERS_CONVERT_QUEST_FAILED("believers-convert-quest-failed"),
	BELIEVERS_CONVERT_QUEST_PROGRESS("believers-convert-quest-progress"),
	BELIEVERS_CONVERT_QUEST_STARTED("believers-convert-quest-started"),
	BELIEVERS_CONVERT_QUEST_STATUS("believers-convert-quest-status"),
	BELIEVERS_CRUSADE_QUEST_COMPLETED("believers-crusade-quest-completed"),
	BELIEVERS_CRUSADE_QUEST_FAILED("believers-crusade-quest-failed"),
	BELIEVERS_CRUSADE_QUEST_PROGRESS("believers-crusade-quest-progress"),
	BELIEVERS_CRUSADE_QUEST_STARTED("believers-crusade-quest-started"),
	BELIEVERS_CRUSADE_QUEST_STATUS("believers-crusade-quest-status"),
	BELIEVERS_CURSED_PLAYER_SET("believers-cursed-player-set"),
	BELIEVERS_CURSED_PLAYER_UNSET("believers-cursed-player-unset"),
	BELIEVERS_DEFEND_HOLY_LAND_FAILED("believers-defend-holy-land-failed"),
	BELIEVERS_DEFEND_HOLY_LAND_SUCCESS("believers-defend-holy-land-success"),
	BELIEVERS_EAT_FOOD_BLESSING("believers-eat-food-blessing"),
	/** A priest (or a plugin) changed the god's gender; {@code <gender>} is its new one ("God", "Goddess", "Deity") */
	BELIEVERS_GENDER_CHANGED("believers-gender-changed"),
	/** The god descended to its main altar and appears there as an NPC (config.yml npc.descend) */
	BELIEVERS_GOD_DESCENDED("believers-god-descended"),
	/** The god that had descended ascended again: no priest, or too few believers for too long */
	BELIEVERS_GOD_ASCENDED("believers-god-ascended"),
	BELIEVERS_GIVE_BIBLES_QUEST_PROGRESS("believers-give-bibles-quest-progress"),
	BELIEVERS_GIVE_ITEMS_QUEST_COMPLETED("believers-give-items-quest-completed"),
	BELIEVERS_GIVE_ITEMS_QUEST_FAILED("believers-give-items-quest-failed"),
	BELIEVERS_GIVE_ITEMS_QUEST_STARTED("believers-give-items-quest-started"),
	BELIEVERS_GIVE_ITEMS_QUEST_STATUS("believers-give-items-quest-status"),
	BELIEVERS_HOLY_FEAST_QUEST_COMPLETED("believers-holy-feast-quest-completed"),
	BELIEVERS_HOLY_FEAST_QUEST_FAILED("believers-holy-feast-quest-failed"),
	BELIEVERS_HOLY_FEAST_QUEST_PROGRESS("believers-holy-feast-quest-progress"),
	BELIEVERS_HOLY_FEAST_QUEST_STARTED("believers-holy-feast-quest-started"),
	BELIEVERS_HOLY_FEAST_QUEST_STATUS("believers-holy-feast-quest-status"),
	BELIEVERS_LOST_BELIEVER("believers-lost-believer"),
	BELIEVERS_NEW_PLAYER_ACCEPTED("believers-new-player-accepted"),
	BELIEVERS_NOT_EAT_FOOD_CURSING("believers-not-eat-food-cursing"),
	BELIEVERS_NOT_SLAY_MOB_CURSING("believers-not-slay-mob-cursing"),
	BELIEVERS_PILGRIMAGE_QUEST_COMPLETED("believers-pilgrimage-quest-completed"),
	BELIEVERS_PILGRIMAGE_QUEST_FAILED("believers-pilgrimage-quest-failed"),
	BELIEVERS_PILGRIMAGE_QUEST_PROGRESS("believers-pilgrimage-quest-progress"),
	BELIEVERS_PILGRIMAGE_QUEST_STARTED("believers-pilgrimage-quest-started"),
	BELIEVERS_PILGRIMAGE_QUEST_STATUS("believers-pilgrimage-quest-status"),
	BELIEVERS_PLAYER_BLESSED("believers-player-blessed"),
	BELIEVERS_PLAYER_CURSED("believers-player-cursed"),
	BELIEVERS_PLAYER_JOINED_RELIGION("believers-player-joined-religion"),
	BELIEVERS_PLAYER_LEFT_RELIGION("believers-player-left-religion"),
	BELIEVERS_PRIEST_ACCEPTED("believers-priest-accepted"),
	BELIEVERS_REMOVED_PRIEST("believers-removed-priest"),
	BELIEVERS_SACRIFICE_ITEM_TYPE("believers-sacrifice-item-type"),
	BELIEVERS_SACRIFICE_QUEST_COMPLETED("believers-sacrifice-quest-completed"),
	BELIEVERS_SACRIFICE_QUEST_FAILED("believers-sacrifice-quest-failed"),
	BELIEVERS_SACRIFICE_QUEST_PROGRESS("believers-sacrifice-quest-progress"),
	BELIEVERS_SACRIFICE_QUEST_STARTED("believers-sacrifice-quest-started"),
	BELIEVERS_SACRIFICE_QUEST_STATUS("believers-sacrifice-quest-status"),
	BELIEVERS_SET_HOME("believers-set-home"),
	BELIEVERS_SLAY_MOB_BLESSING("believers-slay-mob-blessing"),
	BELIEVERS_SLAY_QUEST_COMPLETED("believers-slay-quest-completed"),
	BELIEVERS_SLAY_QUEST_FAILED("believers-slay-quest-failed"),
	BELIEVERS_SLAY_QUEST_PROGRESS("believers-slay-quest-progress"),
	BELIEVERS_SLAY_QUEST_STARTED("believers-slay-quest-started"),
	BELIEVERS_SLAY_QUEST_STATUS("believers-slay-quest-status"),
	BELIEVERS_WAR("believers-war"),
	BELIEVERS_WAR_CANCELLED("believers-war-cancelled"),
	PLAYER_ACCEPTED_INVITATION("player-accepted-invitation"),
	PLAYER_BLESSED("player-blessed"),
	PLAYER_CURSED("player-cursed"),
	PLAYER_INVITE("player-invite"),
	PRIEST_BLESSED_PLAYER_SET("priest-blessed-player-set"),
	PRIEST_BLESSED_PLAYER_UNSET("priest-blessed-player-unset"),
	PRIEST_CURSED_PLAYER_SET("priest-cursed-player-set"),
	PRIEST_CURSED_PLAYER_UNSET("priest-cursed-player-unset"),
	PRIEST_EAT_FOOD_TYPE("priest-eat-food-type"),
	PRIEST_NOT_EAT_FOOD_TYPE("priest-not-eat-food-type"),
	PRIEST_NOT_SLAY_MOB_TYPE("priest-not-slay-mob-type"),
	PRIEST_PRIEST_ACCEPTED("priest-priest-accepted"),
	PRIEST_SLAY_MOB_TYPE("priest-slay-mob-type"),
	PRIEST_USE_BIBLE("priest-use-bible");

	private final String key;

	Voice(String key)
	{
		this.key = key;
	}

	/** The key in the voice files */
	public String key()
	{
		return this.key;
	}

	private static final Map<String, Voice> BY_KEY = new HashMap<>();
	static
	{
		for (Voice voice : values())
		{
			BY_KEY.put(voice.key, voice);
		}
	}

	/** The situation with that key in the voice files (e.g. {@code believer-praying}), if there is one */
	public static Optional<Voice> byKey(String key)
	{
		return Optional.ofNullable(key != null ? BY_KEY.get(key) : null);
	}
}
