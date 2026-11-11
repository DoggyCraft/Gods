package com.dogonfire.gods.task;

import java.util.Random;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.message.Messages;

public class GiveItemTask extends Task {
	private Player player = null;
	private Material itemType;
	private String godName = null;
	private boolean speak = false;

	public GiveItemTask(GodsContext ctx, String god, Player p, Material material, boolean godspeak) {
		super(ctx);
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
				ctx.godSpeech().say(this.godName, this.player, Voice.BELIEVER_ITEM_BLESSING, 2 + random.nextInt(10), Messages.player(this.player.getName()), Messages.type(this.itemType.name()));
			}
		}
	}
}
