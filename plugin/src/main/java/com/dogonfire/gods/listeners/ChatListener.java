package com.dogonfire.gods.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.ChatManager;

public class ChatListener implements Listener
{
	/** How often the chat cache is refreshed for everyone online (ticks). */
	private static final long CACHE_REFRESH_TICKS = 100L;

	public ChatListener()
	{
		// Priest status and the like change in places that don't notify the
		// chat cache, so refresh it now and then on the main thread
		Gods.instance().getServer().getScheduler().runTaskTimer(Gods.instance(), () -> ChatManager.instance().refreshAll(), 1L, CACHE_REFRESH_TICKS);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		ChatManager.instance().refresh(event.getPlayer().getUniqueId());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		ChatManager.instance().remove(event.getPlayer().getUniqueId());
	}

	// Runs on the chat thread: only read the ChatManager cache here
	@EventHandler(ignoreCancelled = true)
	public void onPlayerChat(AsyncPlayerChatEvent event)
	{
		Player player = event.getPlayer();

		ChatManager.ChatInfo info = ChatManager.instance().getChatInfo(player.getUniqueId());
		String godName = info.godName;

		if (GodsConfiguration.instance().isChatFormattingEnabled())
		{
			event.setFormat(ChatManager.instance().formatChat(player, godName, event.getFormat()));
		}

		if (godName == null || !info.religionChat)
		{
			return;
		}

		event.setCancelled(true);

		String message;
		if (info.priest)
		{
			message = ChatColor.YELLOW + "[" + godName + "Chat] " + player.getName() + ": " + ChatColor.WHITE + event.getMessage();
		}
		else
		{
			message = ChatColor.YELLOW + "[" + godName + "Chat] " + ChatColor.RED + player.getName() + ChatColor.YELLOW + ": " + ChatColor.WHITE + event.getMessage();
		}

		for (Player otherPlayer : Gods.instance().getServer().getOnlinePlayers())
		{
			if (godName.equals(ChatManager.instance().getChatInfo(otherPlayer.getUniqueId()).godName))
			{
				otherPlayer.sendMessage(message);
			}
		}

		Gods.instance().log(player.getName() + "(GODCHAT): " + event.getMessage());
	}
}
