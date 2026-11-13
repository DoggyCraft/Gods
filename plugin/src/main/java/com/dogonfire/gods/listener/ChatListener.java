package com.dogonfire.gods.listener;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.service.ChatService;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ChatListener implements Listener {
  private final GodsContext ctx;

  public ChatListener(GodsContext ctx) {
    this.ctx = ctx;
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlayerJoin(PlayerJoinEvent event) {
    ctx.chatService().refresh(event.getPlayer().getUniqueId());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerQuit(PlayerQuitEvent event) {
    ctx.chatService().remove(event.getPlayer().getUniqueId());
  }

  // Runs on the chat thread: only read the ChatService cache here
  @EventHandler(ignoreCancelled = true)
  public void onPlayerChat(AsyncPlayerChatEvent event) {
    Player player = event.getPlayer();

    ChatService.ChatInfo info = ctx.chatService().getChatInfo(player.getUniqueId());
    String godName = info.godName;

    if (ctx.settings().chat().formatting()) {
      event.setFormat(ctx.chatService().formatChat(player, godName, event.getFormat()));
    }

    if (godName == null || !info.religionChat) {
      return;
    }

    event.setCancelled(true);

    // Rendered once, here on the chat thread (Messages is thread-safe)
    // A believer with a devotion title shows it before their name (config.yml devotion.chat-title)
    TagResolver name =
        ctx.settings().devotion().chatTitle() && !info.devotionTitle.isEmpty()
            ? Messages.component(
                "player",
                ctx.messages()
                    .component(
                        "chat.titled-player",
                        Messages.player(player.getName()),
                        Messages.text("title", info.devotionTitle)))
            : Messages.player(player.getName());
    String message =
        ctx.messages()
            .legacy(
                info.priest ? "chat.priest" : "chat.believer",
                Messages.god(godName),
                name,
                Messages.text("message", event.getMessage()));

    for (Player otherPlayer : ctx.plugin().getServer().getOnlinePlayers()) {
      if (godName.equals(ctx.chatService().getChatInfo(otherPlayer.getUniqueId()).godName)) {
        otherPlayer.sendMessage(message);
      }
    }

    ctx.plugin().log(player.getName() + "(GODCHAT): " + event.getMessage());
  }
}
