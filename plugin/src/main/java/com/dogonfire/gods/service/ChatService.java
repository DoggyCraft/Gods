package com.dogonfire.gods.service;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Chat formatting, and a thread-safe snapshot of the religion data that the (asynchronous) chat
 * handler and PlaceholderAPI need. The services keep their data in models that must only be touched
 * on the main thread, so the chat thread reads this cache instead. It is filled on the main thread:
 * on join, when a believer's god, chat mode or devotion or a god's priests or rank change, and
 * periodically for everyone online (see Gods#onEnable).
 */
public class ChatService {
  /** What the chat handler and the placeholders know about one online player. */
  public static final class ChatInfo {
    public final String godName;
    public final boolean religionChat;
    public final boolean priest;
    public final ChatColor godColor;
    public final int prayerPower;

    /** The god's rank, as its god type calls it ("" without a god) */
    public final String rankName;

    /**
     * Their devotion level, as their god's type calls it ("" without a god or while devotion is
     * off)
     */
    public final String devotionName;

    /** Their devotion points (0 without a god or while devotion is off) */
    public final int devotionPoints;

    /** Their devotion title: their level's name once it has devotion.unlocks.title, else "" */
    public final String devotionTitle;

    /**
     * %gods_tag%: their god as a tag (placeholder.tag or placeholder.tag-priest, legacy text; ""
     * without a god)
     */
    public final String tag;

    ChatInfo(
        String godName,
        boolean religionChat,
        boolean priest,
        ChatColor godColor,
        int prayerPower,
        String rankName,
        String devotionName,
        int devotionPoints,
        String devotionTitle,
        String tag) {
      this.godName = godName;
      this.religionChat = religionChat;
      this.priest = priest;
      this.godColor = godColor;
      this.prayerPower = prayerPower;
      this.rankName = rankName;
      this.devotionName = devotionName;
      this.devotionPoints = devotionPoints;
      this.devotionTitle = devotionTitle;
      this.tag = tag;
    }
  }

  /** How often the cache is refreshed for everyone online, in ticks */
  public static final long REFRESH_TICKS = 100L;

  private static final ChatInfo NO_GOD =
      new ChatInfo(null, false, false, ChatColor.WHITE, 0, "", "", 0, "", "");

  private final String chatTag = "[GOD]";
  private final Map<UUID, ChatInfo> chatInfos = new ConcurrentHashMap<UUID, ChatInfo>();

  private final GodsContext ctx;

  public ChatService(GodsContext ctx) {
    this.ctx = ctx;
  }

  /**
   * Replaces the [GOD] tag in a chat format with the god's name. The format is a String.format
   * pattern, so a '%' in the god name is escaped.
   */
  public String formatChat(Player player, String godName, String format) {
    if (godName != null) {
      return format.replace(this.chatTag, godName.replace("%", "%%") + " ");
    }

    return format.replace(this.chatTag, "");
  }

  /** Safe to call from any thread. Never returns null. */
  public ChatInfo getChatInfo(UUID playerId) {
    ChatInfo info = this.chatInfos.get(playerId);

    return info != null ? info : NO_GOD;
  }

  /**
   * The player's snapshot: the cached one; on the main thread ({@code mainThread}) a fresh one when
   * none is cached yet
   */
  public ChatInfo info(UUID playerId, boolean mainThread) {
    ChatInfo info = this.chatInfos.get(playerId);
    if (info != null) {
      return info;
    }
    return mainThread ? snapshot(playerId) : NO_GOD;
  }

  /**
   * Main thread only: what the player's religion is now, read from the services (the cache is not
   * changed)
   */
  public ChatInfo snapshot(UUID playerId) {
    String godName = ctx.believerService().getGodForBeliever(playerId);

    if (godName == null) {
      return new ChatInfo(
          null,
          false,
          false,
          ChatColor.WHITE,
          ctx.believerService().getPrayerPower(playerId),
          "",
          "",
          0,
          "",
          "");
    }

    boolean religionChat = ctx.believerService().getReligionChat(playerId);
    java.util.List<UUID> priests = ctx.priestService().getPriestsForGod(godName);
    boolean priest = priests != null && priests.contains(playerId);
    ChatColor godColor = ctx.godService().getColorForGod(godName);
    String rankName = ctx.rankService().getRankName(godName);
    boolean devotion = ctx.devotionService().enabled();
    String devotionName = devotion ? ctx.devotionService().getLevelName(playerId) : "";
    String devotionTitle = ctx.devotionService().getTitle(playerId);
    String tag =
        ctx.messages()
            .legacy(
                priest ? "placeholder.tag-priest" : "placeholder.tag",
                Messages.god(godName),
                Messages.color("god-color", godColor),
                Messages.text("rank", rankName),
                Messages.text("devotion", devotionName),
                Messages.text("title", devotionTitle));

    return new ChatInfo(
        godName,
        religionChat,
        priest,
        godColor,
        ctx.believerService().getPrayerPower(playerId),
        rankName,
        devotionName,
        devotion ? ctx.devotionService().getPoints(playerId) : 0,
        devotionTitle,
        tag);
  }

  /** Main thread only: reads the believer and god data for an online player. */
  public void refresh(UUID playerId) {
    if (ctx.plugin().getServer().getPlayer(playerId) == null) {
      this.chatInfos.remove(playerId);
      return;
    }

    this.chatInfos.put(playerId, snapshot(playerId));
  }

  /** Main thread only: refreshes everyone online. */
  public void refreshAll() {
    for (Player player : ctx.plugin().getServer().getOnlinePlayers()) {
      refresh(player.getUniqueId());
    }

    this.chatInfos
        .keySet()
        .removeIf(playerId -> ctx.plugin().getServer().getPlayer(playerId) == null);
  }

  public void remove(UUID playerId) {
    this.chatInfos.remove(playerId);
  }
}
