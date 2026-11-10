package com.dogonfire.gods;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired before Gods shows a title to a player. Listeners can change the title
 * and subtitle, or cancel the event so no title is shown.
 */
public class TitleSendEvent extends Event implements Cancellable
{
	private static final HandlerList	handlers	= new HandlerList();
	private final Player				player;
	private String						title;
	private String						subtitle;
	private boolean						cancelled	= false;

	public TitleSendEvent(Player player, String title, String subtitle)
	{
		this.player = player;
		this.title = title;
		this.subtitle = subtitle;
	}

	@Override
	public HandlerList getHandlers()
	{
		return handlers;
	}

	public static HandlerList getHandlerList()
	{
		return handlers;
	}

	public Player getPlayer()
	{
		return player;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getSubtitle()
	{
		return subtitle;
	}

	public void setSubtitle(String subtitle)
	{
		this.subtitle = subtitle;
	}

	@Override
	public boolean isCancelled()
	{
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled)
	{
		this.cancelled = cancelled;
	}

}
