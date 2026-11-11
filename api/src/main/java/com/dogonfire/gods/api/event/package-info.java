/**
 * Gods' events. Gods fires them from the code that makes the change, so they come whether a player, the gods' own
 * turns or another plugin (through {@link com.dogonfire.gods.api.GodsApi}) caused it. Most can be cancelled; a
 * cancelled event changes nothing, and a command that caused it answers with Gods' {@code command.cancelled} message.
 * They are all fired on the server thread.
 * <p>
 * The events about a god carry its name ({@link com.dogonfire.gods.api.event.GodEvent#getGodName()}); get a snapshot
 * of it with {@link com.dogonfire.gods.api.GodsApi#getGod(String)} if you need more.
 * <p>
 * Listen to the concrete events. {@link com.dogonfire.gods.api.event.GodEvent} and
 * {@link com.dogonfire.gods.api.event.QuestEvent} only share what their events carry: they have no handler list, so
 * Bukkit refuses a listener for them.
 */
package com.dogonfire.gods.api.event;
