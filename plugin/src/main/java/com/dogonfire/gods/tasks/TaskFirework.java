package com.dogonfire.gods.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import com.dogonfire.gods.GodsContext;

public class TaskFirework extends Task {
	private static final Color[] COLORS = { Color.AQUA, Color.BLACK, Color.BLUE, Color.FUCHSIA, Color.GRAY, Color.GREEN, Color.LIME, Color.MAROON, Color.NAVY, Color.OLIVE, Color.ORANGE, Color.PURPLE, Color.RED, Color.SILVER, Color.TEAL, Color.WHITE, Color.YELLOW };

	private Player player;
	private Random random = new Random();
	private int numberOfRockets = 1;

	public TaskFirework(GodsContext ctx, Player player, int numberOfRockets) {
		super(ctx);
		this.player = player;
		this.numberOfRockets = numberOfRockets;
	}

	@Override
	public void run() {
		if (!this.player.isOnline()) {
			return;
		}

		Location origin = this.player.getLocation();
		int power = 1 + this.random.nextInt(2);

		List<Color> colors = new ArrayList<Color>();
		for (int n = 0; n < 2; n++) {
			colors.add(COLORS[this.random.nextInt(COLORS.length)]);
		}

		FireworkEffect.Type[] types = FireworkEffect.Type.values();
		FireworkEffect effect = FireworkEffect.builder().flicker(true).withColor(colors).withFade(colors).with(types[this.random.nextInt(types.length)]).trail(true).build();

		for (int x = 0; x < this.numberOfRockets; x++) {
			Firework firework = origin.getWorld().spawn(origin.clone().add(x, 0.0D, 0.0D), Firework.class);
			FireworkMeta fireworkMeta = firework.getFireworkMeta();
			fireworkMeta.addEffect(effect);
			fireworkMeta.setPower(power);
			firework.setFireworkMeta(fireworkMeta);
		}

		int n = (int) (this.numberOfRockets / 1.50f);

		if (n > 0) {
			getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new TaskFirework(ctx, this.player, n), 20 + this.random.nextInt(80));
		}
	}
}
