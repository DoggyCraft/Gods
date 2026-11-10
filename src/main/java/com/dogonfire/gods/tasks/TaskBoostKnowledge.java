package com.dogonfire.gods.tasks;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class TaskBoostKnowledge extends Task {
	private Player player;
	private long amount;

	public TaskBoostKnowledge(Player player, long amount) {
		this.amount = amount;
		this.player = player;
	}

	@Override
	public void run() {
		this.player.playSound(this.player.getLocation(), Sound.AMBIENT_CAVE, 1.0F, 0.1F);

		this.player.setExp(this.player.getExp() + this.amount);
	}
}
