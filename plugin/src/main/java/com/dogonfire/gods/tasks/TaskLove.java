package com.dogonfire.gods.tasks;

import java.util.Random;

import org.bukkit.EntityEffect;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

import com.dogonfire.gods.GodsContext;

public class TaskLove extends Task
{
	private Player	player1;
	private Player	player2;
	private int		cycle	= 3;

	public TaskLove(GodsContext ctx, Player player1, Player player2)
	{
		super(ctx);
		this.player1 = player1;
		this.player2 = player2;
	}

	public TaskLove(GodsContext ctx, Player player1, Player player2, int cycle)
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
			Wolf wolf = player1.getWorld().spawn(this.player1.getLocation(), Wolf.class);
			wolf.playEffect(EntityEffect.WOLF_HEARTS);		
			wolf.remove();

			if (firework)
			{
				ctx.holyPowerManager().shootFirework(player1, 16);
			}
		}

		{
			Wolf wolf = player2.getWorld().spawn(this.player2.getLocation(), Wolf.class);
			wolf.playEffect(EntityEffect.WOLF_HEARTS);		
			wolf.remove();

			if (firework)
			{
				ctx.holyPowerManager().shootFirework(player2, 16);
			}
		}

		int newcycle = this.cycle - 1;
		if (newcycle > 0)
		{
			getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new TaskLove(ctx, this.player1, this.player2, newcycle), 40L);
		}
	}
}
