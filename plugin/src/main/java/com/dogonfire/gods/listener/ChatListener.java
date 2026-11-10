package com.dogonfire.gods.listener;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.service.ChatService;

public class ChatListener implements Listener
{
	private final GodsContext ctx;

	public ChatListener(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		ctx.chatService().refresh(event.getPlayer().getUniqueId());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		ctx.chatService().remove(event.getPlayer().getUniqueId());
	}

	// Runs on the chat thread: only read the ChatService cache here
	@EventHandler(ignoreCancelled = true)
	public void onPlayerChat(AsyncPlayerChatEvent event)
	{
		Player player = event.getPlayer();

		ChatService.ChatInfo info = ctx.chatService().getChatInfo(player.getUniqueId());
		String godName = info.godName;

		if (ctx.settings().chat().formatting())
		{
			event.setFormat(ctx.chatService().formatChat(player, godName, event.getFormat()));
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

		for (Player otherPlayer : ctx.plugin().getServer().getOnlinePlayers())
		{
			if (godName.equals(ctx.chatService().getChatInfo(otherPlayer.getUniqueId()).godName))
			{
				otherPlayer.sendMessage(message);
			}
		}

		ctx.plugin().log(player.getName() + "(GODCHAT): " + event.getMessage());
	}
}
