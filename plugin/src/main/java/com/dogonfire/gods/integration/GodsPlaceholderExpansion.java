package com.dogonfire.gods.integration;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.service.ChatService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * The %gods_...% placeholders of PlaceholderAPI. Registered by {@link com.dogonfire.gods.Gods} only
 * when PlaceholderAPI is installed (this class references its classes), and unregistered on
 * disable.
 */
public class GodsPlaceholderExpansion extends PlaceholderExpansion {
  private final GodsContext ctx;

  public GodsPlaceholderExpansion(GodsContext ctx) {
    this.ctx = ctx;
  }

  /** Registered by Gods itself, so it stays when PlaceholderAPI reloads */
  @Override
  public boolean persist() {
    return true;
  }

  @Override
  public boolean canRegister() {
    return true;
  }

  @Override
  public String getAuthor() {
    return String.join(", ", this.ctx.plugin().getDescription().getAuthors());
  }

  /** %gods_<value>% */
  @Override
  public String getIdentifier() {
    return "gods";
  }

  @Override
  public String getVersion() {
    return this.ctx.plugin().getDescription().getVersion();
  }

  @Override
  public String onPlaceholderRequest(Player player, String identifier) {
    if (player == null) {
      return "";
    }

    // Chat plugins resolve placeholders on the async chat thread, where the YAML data must not be
    // read; ChatService keeps a snapshot for that (refreshed on every change and every few seconds)
    ChatService.ChatInfo info =
        this.ctx.chatService().info(player.getUniqueId(), Bukkit.isPrimaryThread());
    String god = info.godName;

    return switch (identifier) {
      // %gods_name_space%
      case "name_space" -> (god == null) ? "" : god + " ";
      // %gods_name%
      case "name" -> (god == null) ? "" : god;
      // %gods_prayerpower%
      case "prayerpower" -> String.valueOf(info.prayerPower);
      // %gods_color%
      case "color" -> "&" + ((info.godColor != null) ? info.godColor : ChatColor.WHITE).getChar();
      // %gods_rank%: the god's rank, as its god type calls it
      case "rank" -> info.rankName;
      // %gods_devotion%: the player's devotion level, as their god's type calls it
      case "devotion" -> info.devotionName;
      // %gods_devotion_points%
      case "devotion_points" -> String.valueOf(info.devotionPoints);
      // %gods_devotion_title%: their level's name once it is a title (devotion.unlocks.title), else
      // empty
      case "devotion_title" -> info.devotionTitle;
      // %gods_tag%: their god as a ready-made tag (placeholder.tag in messages.yml), empty without
      // a god
      case "tag" -> info.tag;
      // An invalid placeholder (f.e. %gods_cakenomnom%)
      default -> null;
    };
  }
}
