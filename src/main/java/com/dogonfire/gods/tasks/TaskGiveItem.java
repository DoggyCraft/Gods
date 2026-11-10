package com.dogonfire.gods.tasks;

import java.util.Random;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.LanguageManager;

public class TaskGiveItem extends Task {
	private Player player = null;
	private Material itemType;
	private String godName = null;
	private boolean speak = false;

	public TaskGiveItem(String god, Player p, Material material, boolean godspeak) {
		this.player = p;
		this.godName = new String(god);
		this.itemType = material;
		this.speak = godspeak;
	}

	private boolean giveItem() {
		Vector dir = this.player.getLocation().getDirection();

		dir.setY(0);

		Location spawnLocation = this.player.getLocation().toVector().add(dir.multiply(4)).toLocation(this.player.getWorld());

		spawnLocation.setY(spawnLocation.getY() + 2.0D);
		if (spawnLocation.getBlock().getType() != Material.AIR) {
			return false;
		}
		try {
			spawnLocation.getWorld().dropItem(spawnLocation, new ItemStack(this.itemType, 1));

			spawnLocation.getWorld().playEffect(spawnLocation, Effect.MOBSPAWNER_FLAMES, 25);
		} catch (Exception ex) {
			return false;
		}
		return true;
	}

	@Override
	public void run() {
		if (giveItem()) {
			Random random = new Random();
			if (this.speak) {
				LanguageManager.instance().setPlayerName(this.player.getName());
				try {
					LanguageManager.instance().setType(this.itemType.name());
				} catch (Exception ex) {
					getPlugin().logDebug(ex.getStackTrace().toString());
				}
				GodManager.instance().GodSay(this.godName, this.player, LanguageManager.LANGUAGESTRING.GodToBelieverItemBlessing, 2 + random.nextInt(10));
			}
		}
	}
}
