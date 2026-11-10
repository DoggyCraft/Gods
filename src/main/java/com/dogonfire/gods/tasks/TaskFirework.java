package com.dogonfire.gods.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

public class TaskFirework extends Task {
	private Player player;
	private Random random = new Random();
	private int numberOfRockets = 1;

	public TaskFirework(Player player, int numberOfRockets) {
		this.player = player;
		this.numberOfRockets = numberOfRockets;
	}

	@Override
	public void run() {
		int a = (int) this.player.getLocation().getX();
		int y = (int) this.player.getLocation().getY();
		int z = (int) this.player.getLocation().getZ();

		int power = (int) (Math.random() * 2.0D) + 1;
		List<Color> c = new ArrayList<Color>();

		for (int n = 0; n < 2; n++) {
			switch (random.nextInt(19)) {
				case 0:
					c.add(Color.PURPLE);
				case 1:
					c.add(Color.AQUA);
				case 2:
					c.add(Color.BLACK);
				case 3:
					c.add(Color.BLUE);
				case 4:
					c.add(Color.FUCHSIA);
				case 5:
					c.add(Color.GRAY);
				case 6:
					c.add(Color.GREEN);
				case 7:
					c.add(Color.LIME);
				case 8:
					c.add(Color.MAROON);
				case 9:
					c.add(Color.NAVY);
				case 10:
					c.add(Color.OLIVE);
				case 11:
					c.add(Color.ORANGE);
				case 12:
					c.add(Color.PURPLE);
				case 13:
					c.add(Color.RED);
				case 14:
					c.add(Color.SILVER);
				case 15:
					c.add(Color.TEAL);
				case 16:
					c.add(Color.WHITE);
				case 17:
					c.add(Color.YELLOW);
			}
		}

		int type = (int) (5.0D * Math.random()) + 1;

		FireworkEffect.Type effect = FireworkEffect.Type.BALL;

		switch (type) {
			case 1:
				effect = FireworkEffect.Type.BALL;
			case 2:
				effect = FireworkEffect.Type.BALL_LARGE;
			case 3:
				effect = FireworkEffect.Type.BURST;
			case 4:
				effect = FireworkEffect.Type.CREEPER;
			case 5:
				effect = FireworkEffect.Type.STAR;
		}

		for (int x = 0; x <= this.numberOfRockets; x++) {
			Firework fireworks = (Firework) this.player.getWorld().spawnEntity(new Location(this.player.getWorld(), a + x, y, z), EntityType.FIREWORK_ROCKET);
			FireworkMeta fireworkmeta = fireworks.getFireworkMeta();
			FireworkEffect e = FireworkEffect.builder().flicker(true).withColor(c).withFade(c).with(effect).trail(true).build();

			fireworkmeta.addEffect(e);
			fireworkmeta.setPower(power);
			fireworks.setFireworkMeta(fireworkmeta);
		}

		int n = (int) ((this.numberOfRockets) / 1.50f);

		if (n > 0) {
			getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new TaskFirework(this.player, n), 20 + this.random.nextInt(80));
		}
	}
}
