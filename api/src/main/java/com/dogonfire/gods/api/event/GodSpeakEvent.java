package com.dogonfire.gods.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * A god is about to say something to a player: one of its voice lines (a believer prayed, a quest started, a random
 * remark, ...) or a text Gods or another plugin made it say. Every word a god says to a player comes through here,
 * once per player it is said to, before it is sent.
 * <p>
 * The text is already rendered, as a string with {@code §} color codes. A god's words are sent
 * {@link #getDelayTicks()} ticks later, so cancelling the event and showing {@link #getWords()} some other way (for
 * example as an NPC's dialogue) replaces Gods' chat message. Changing {@link #setMessage(String) the message} changes
 * the chat line the player gets.
 */
public class GodSpeakEvent extends GodEvent implements Cancellable
{
	private static final HandlerList HANDLERS = new HandlerList();

	private final Player recipient;
	private final String situation;
	private final String words;
	private final int delayTicks;
	private String message;
	private boolean cancelled;

	/**
	 * @param godName    the god who speaks
	 * @param recipient  the player spoken to
	 * @param situation  the voice line's situation, or null for a text that isn't a voice line
	 * @param words      what the god says, rendered, without the chat format
	 * @param message    the chat line the player gets, rendered
	 * @param delayTicks how many ticks from now the message is sent
	 */
	public GodSpeakEvent(String godName, Player recipient, String situation, String words, String message, int delayTicks)
	{
		super(godName);
		this.recipient = recipient;
		this.situation = situation;
		this.words = words;
		this.message = message;
		this.delayTicks = delayTicks;
	}

	/** The player the god speaks to */
	public Player getRecipient()
	{
		return this.recipient;
	}

	/** Whether the god says one of its voice lines (rather than a text it was given) */
	public boolean isVoiceLine()
	{
		return this.situation != null;
	}

	/**
	 * The situation of the voice line, a key of Gods' voice files such as {@code believer-praying} (see
	 * {@link com.dogonfire.gods.api.GodsApi#getVoiceSituations()}); null when the god says a text it was given
	 */
	public String getSituation()
	{
		return this.situation;
	}

	/** What the god says, with {@code §} colors, without the chat format around it (no god name in front) */
	public String getWords()
	{
		return this.words;
	}

	/**
	 * The chat line the player gets: the words in Gods' format for gods' speech, with {@code §} colors. Words to one player
	 * and words to the whole religion (its believers or priests) each have their own format in Gods' messages.
	 */
	public String getMessage()
	{
		return this.message;
	}

	/** Changes the chat line the player gets ({@code §} colors); an empty one sends nothing */
	public void setMessage(String message)
	{
		this.message = message != null ? message : "";
	}

	/** How many ticks from now the chat line is sent */
	public int getDelayTicks()
	{
		return this.delayTicks;
	}

	@Override
	public boolean isCancelled()
	{
		return this.cancelled;
	}

	/** A cancelled god says nothing to this player (the chat line is not sent) */
	@Override
	public void setCancelled(boolean cancelled)
	{
		this.cancelled = cancelled;
	}

	@Override
	public HandlerList getHandlers()
	{
		return HANDLERS;
	}

	public static HandlerList getHandlerList()
	{
		return HANDLERS;
	}
}
