package com.dogonfire.gods.task;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.managers.LanguageManager;

public class GodSpeakTask extends Task {
	private UUID playerId;
	private String godName;
	private LanguageManager.LANGUAGESTRING message;
	private int amount;
	private String playerNameString;
	private String typeString;

	public GodSpeakTask(GodsContext ctx, String gname, UUID playerId, String player, String type, int a, LanguageManager.LANGUAGESTRING m) {
		super(ctx);
		this.playerId = playerId;
		this.godName = gname;
		this.message = m;
		this.playerNameString = (player != null) ? player : "";
		this.typeString = (type != null) ? type : "";
		this.amount = a;
	}

	@Override
	public void run() {
		Player player = getPlugin().getServer().getPlayer(this.playerId);
		if (player == null || this.message == null) {
			return;
		}

		// LanguageManager is shared, so set every value this message may use before reading it
		ctx.languageManager().setAmount(this.amount);
		try {
			ctx.languageManager().setType(this.typeString);
		} catch (Exception ex) {
			getPlugin().logDebug(ex.getMessage());
		}
		ctx.languageManager().setPlayerName(this.playerNameString);

		player.sendMessage(ChatColor.GOLD + "<" + this.godName + ">: " + ChatColor.WHITE + ChatColor.BOLD + ctx.languageManager().getLanguageString(this.godName, this.message));
	}
}
