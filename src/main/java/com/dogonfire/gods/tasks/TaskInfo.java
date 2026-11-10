package com.dogonfire.gods.tasks;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.dogonfire.gods.managers.LanguageManager;

public class TaskInfo extends Task {
	private UUID playerId;
	private String name1;
	private String name2;
	private LanguageManager.LANGUAGESTRING message;
	private int amount;
	private ChatColor color;

	public TaskInfo(ChatColor color, UUID playerId, LanguageManager.LANGUAGESTRING m, int amount, String name1) {
		this.playerId = playerId;
		this.message = m;
		this.name1 = name1;
		this.amount = amount;
		this.color = color;
	}

	/**
	 * $PlayerName is name, $Type is amount1 and $Amount is amount2.
	 */
	public TaskInfo(ChatColor color, UUID playerId, LanguageManager.LANGUAGESTRING m, String name, int amount1, int amount2) {
		this.playerId = playerId;
		this.name1 = name;
		this.name2 = String.valueOf(amount1);
		this.message = m;
		this.amount = amount2;
		this.color = color;
	}

	public TaskInfo(ChatColor color, UUID playerId, LanguageManager.LANGUAGESTRING m, String name1, String name2) {
		this.playerId = playerId;
		this.name1 = name1;
		this.name2 = name2;
		this.message = m;
		this.color = color;
	}

	@Override
	public void run() {
		Player player = getPlugin().getServer().getPlayer(this.playerId);

		if (player == null) {
			return;
		}

		LanguageManager.instance().setPlayerName((this.name1 != null) ? this.name1 : "");

		if (this.name2 != null) {
			try {
				LanguageManager.instance().setType(this.name2);
			} catch (Exception ex) {
				getPlugin().logDebug(ex.getMessage());
			}
		}

		LanguageManager.instance().setAmount(this.amount);

		player.sendMessage(this.color + LanguageManager.instance().getInfoString(this.message, this.color));
	}
}
