package com.dogonfire.gods.task;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.GodsContext;

/** A short task run once (or rescheduling itself until it is done) through the Bukkit scheduler */
public abstract class Task implements Runnable
{
	protected final GodsContext ctx;

	protected Task(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	protected Gods getPlugin()
	{
		return this.ctx.plugin();
	}

	@Override
	public abstract void run();
}
