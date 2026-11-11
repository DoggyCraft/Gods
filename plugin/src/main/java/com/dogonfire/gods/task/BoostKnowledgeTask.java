package com.dogonfire.gods.task;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

public class BoostKnowledgeTask extends Task {
	private Player player;
	private long amount;

	public BoostKnowledgeTask(GodsContext ctx, Player player, long amount) {
		super(ctx);
		this.amount = amount;
		this.player = player;
	}

	@Override
	public void run() {
		if (!this.player.isOnline()) {
			return;
		}

		this.player.playSound(this.player.getLocation(), Sound.AMBIENT_CAVE, 1.0F, 0.1F);

		// setExp() takes the 0..1 progress of the current level, so give whole levels instead
		this.player.giveExpLevels((int) this.amount);
	}
}
