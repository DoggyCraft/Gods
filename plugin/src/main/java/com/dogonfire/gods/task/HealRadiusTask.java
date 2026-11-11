package com.dogonfire.gods.task;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.util.NearbyEntities;
import com.dogonfire.gods.managers.LanguageManager.LANGUAGESTRING;

/**
 * Heals up to maxPlayers other players within 20 blocks.
 */
public class HealRadiusTask extends Task {
	private Player player;
	private long maxPlayers;

	public HealRadiusTask(GodsContext ctx, Player player, long maxPlayers) {
		super(ctx);
		this.player = player;
		this.maxPlayers = maxPlayers;
	}

	@Override
	public void run() {
		if (!this.player.isOnline()) {
			return;
		}

		this.player.playSound(this.player.getLocation(), Sound.AMBIENT_CAVE, 1.0F, 0.1F);

		int n = 0;
		for (Entity entity : NearbyEntities.living(this.player.getLocation(), 20.0D)) {
			if (n >= this.maxPlayers) {
				break;
			}
			if (!(entity instanceof Player target) || target.equals(this.player) || target.isDead()) {
				continue;
			}
			AttributeInstance maxHealth = target.getAttribute(Attribute.MAX_HEALTH);
			if (maxHealth == null) {
				continue;
			}
			target.setHealth(maxHealth.getValue());
			target.getWorld().playEffect(target.getLocation(), Effect.ENDER_SIGNAL, 0);
			n++;
		}
		ctx.godSpeech().sendInfo(this.player.getUniqueId(), LANGUAGESTRING.YouHealedBeings, ChatColor.AQUA, n, "", 20);
	}
}
