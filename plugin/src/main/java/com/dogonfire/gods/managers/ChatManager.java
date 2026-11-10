package com.dogonfire.gods.managers;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.dogonfire.gods.Gods;

/**
 * Chat formatting, and a thread-safe snapshot of the religion data that the
 * (asynchronous) chat handler and PlaceholderAPI need. The managers keep their data in YAML
 * configurations that must only be touched on the main thread, so the chat
 * thread reads this cache instead. It is filled on the main thread: on join,
 * when a believer's god or chat mode changes in BelieverManager, and
 * periodically for everyone online (see ChatListener).
 */
public class ChatManager
{
	private static ChatManager instance;

	public static ChatManager instance()
	{
		if (instance == null)
			instance = new ChatManager();
		return instance;
	}

	/** What the chat handler knows about one online player. */
	public static final class ChatInfo
	{
		public final String		godName;
		public final boolean	religionChat;
		public final boolean	priest;
		public final ChatColor	godColor;
		public final int		prayerPower;

		ChatInfo(String godName, boolean religionChat, boolean priest, ChatColor godColor, int prayerPower)
		{
			this.godName = godName;
			this.religionChat = religionChat;
			this.priest = priest;
			this.godColor = godColor;
			this.prayerPower = prayerPower;
		}
	}

	private static final ChatInfo		NO_GOD		= new ChatInfo(null, false, false, ChatColor.WHITE, 0);

	private final String				chatTag		= "[GOD]";
	private final Map<UUID, ChatInfo>	chatInfos	= new ConcurrentHashMap<UUID, ChatInfo>();

	private ChatManager()
	{
	}

	/**
	 * Replaces the [GOD] tag in a chat format with the god's name. The format is
	 * a String.format pattern, so a '%' in the god name is escaped.
	 */
	public String formatChat(Player player, String godName, String format)
	{
		if (godName != null)
		{
			return format.replace(this.chatTag, godName.replace("%", "%%") + " ");
		}

		return format.replace(this.chatTag, "");
	}

	/** Safe to call from any thread. Never returns null. */
	public ChatInfo getChatInfo(UUID playerId)
	{
		ChatInfo info = this.chatInfos.get(playerId);

		return info != null ? info : NO_GOD;
	}

	/** Main thread only: reads the believer and god data for an online player. */
	public void refresh(UUID playerId)
	{
		if (Gods.instance() == null || Gods.instance().getServer().getPlayer(playerId) == null)
		{
			this.chatInfos.remove(playerId);
			return;
		}

		String godName = BelieverManager.instance().getGodForBeliever(playerId);

		if (godName == null)
		{
			this.chatInfos.put(playerId, new ChatInfo(null, false, false, ChatColor.WHITE, BelieverManager.instance().getPrayerPower(playerId)));
			return;
		}

		boolean religionChat = BelieverManager.instance().getReligionChat(playerId);
		List<UUID> priests = GodManager.instance().getPriestsForGod(godName);
		boolean priest = priests != null && priests.contains(playerId);

		this.chatInfos.put(playerId, new ChatInfo(godName, religionChat, priest, GodManager.instance().getColorForGod(godName), BelieverManager.instance().getPrayerPower(playerId)));
	}

	/** Main thread only: refreshes everyone online. */
	public void refreshAll()
	{
		for (Player player : Gods.instance().getServer().getOnlinePlayers())
		{
			refresh(player.getUniqueId());
		}

		this.chatInfos.keySet().removeIf(playerId -> Gods.instance().getServer().getPlayer(playerId) == null);
	}

	public void remove(UUID playerId)
	{
		this.chatInfos.remove(playerId);
	}
}
