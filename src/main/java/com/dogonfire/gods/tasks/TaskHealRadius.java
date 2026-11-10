package com.dogonfire.gods.tasks;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.dogonfire.gods.managers.HolyPowerManager;
import com.dogonfire.gods.managers.LanguageManager.LANGUAGESTRING;

public class TaskHealRadius extends Task {
	private Player player;

	public TaskHealRadius(Player player, long amount) {
		this.player = player;
	}

	@Override
	public void run() {
		this.player.playSound(this.player.getLocation(), Sound.AMBIENT_CAVE, 1.0F, 0.1F);

		Entity[] entities = HolyPowerManager.instance().getNearbyLivingEntities(this.player.getLocation(), 20.0D);
		int n = 0;
		for (Entity entity : entities) {
			if (this.player.getEntityId() != entity.getEntityId()) {
				LivingEntity targetEntity = (LivingEntity) entity;
				targetEntity.setHealth(targetEntity.getAttribute(Attribute.MAX_HEALTH).getValue());
				targetEntity.getWorld().playEffect(targetEntity.getLocation(), Effect.ENDER_SIGNAL, 0);
				n++;
			}
		}
		getPlugin().sendInfo(this.player.getUniqueId(), LANGUAGESTRING.YouHealedBeings, ChatColor.AQUA, n, "", 20);
	}
}