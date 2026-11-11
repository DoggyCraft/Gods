package com.dogonfire.gods.task;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;

/** /g love: hearts around both partners, three times, and now and then fireworks */
public class LoveTask extends Task
{
	private Player	player1;
	private Player	player2;
	private int		cycle	= 3;

	public LoveTask(GodsContext ctx, Player player1, Player player2)
	{
		super(ctx);
		this.player1 = player1;
		this.player2 = player2;
	}

	public LoveTask(GodsContext ctx, Player player1, Player player2, int cycle)
	{
		super(ctx);
		this.player1 = player1;
		this.player2 = player2;
		this.cycle = cycle;
	}

	@Override
	public void run()
	{
		if (!this.player1.isOnline() || !this.player2.isOnline())
		{
			return;
		}

		Random random = new Random();
		boolean firework = (random.nextInt(100) == 0);

		{
			hearts(this.player1);

			if (firework)
			{
				ctx.holyPowerService().shootFirework(player1, 16);
			}
		}

		{
			hearts(this.player2);

			if (firework)
			{
				ctx.holyPowerService().shootFirework(player2, 16);
			}
		}

		int newcycle = this.cycle - 1;
		if (newcycle > 0)
		{
			getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new LoveTask(ctx, this.player1, this.player2, newcycle), 40L);
		}
	}

	// Hearts around the player, like a wolf being tamed (a wolf spawned and removed in the same tick showed nothing)
	private static void hearts(Player player)
	{
		player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0.0D, 1.0D, 0.0D), 7, 0.5D, 0.5D, 0.5D);
	}
}
