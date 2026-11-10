package com.dogonfire.gods.tasks;

import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.dogonfire.gods.managers.HolyPowerManager;

public class TaskDrunk extends Task {
	private Player player;

	public TaskDrunk(Player player, long amount) {
		this.player = player;
	}

	@Override
	public void run() {
		this.player.playSound(this.player.getLocation(), Sound.AMBIENT_CAVE, 1.0F, 0.1F);

		Entity[] entities = HolyPowerManager.instance().getNearbyLivingEntities(this.player.getLocation(), 20.0D);
		for (Entity entity : entities) {
			if (this.player.getEntityId() != entity.getEntityId()) {
				LivingEntity targetEntity = (LivingEntity) entity;
				targetEntity.setHealth(targetEntity.getAttribute(Attribute.MAX_HEALTH).getValue());
				targetEntity.getWorld().playEffect(targetEntity.getLocation(), Effect.ENDER_SIGNAL, 0);
			}
		}
	}
}
