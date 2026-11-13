package com.dogonfire.gods.task;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Effect;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.message.Messages;

public class HealPlayerTask extends Task
{
	private Player							player	= null;
	private String							godName	= null;
	private Voice	languageString;

	public HealPlayerTask(GodsContext ctx, String god, Player p, Voice speak)
	{
		super(ctx);
		this.player = p;
		this.godName = new String(god);
		this.languageString = speak;
	}

	private boolean healPlayer()
	{
		this.player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 10, 1));

		this.player.getLocation().getWorld().playEffect(this.player.getLocation(), Effect.MOBSPAWNER_FLAMES, 4);
		return true;
	}

	@Override
	public void run()
	{
		ThreadLocalRandom random = ThreadLocalRandom.current();
		if (healPlayer())
		{
			ctx.godSpeech().say(this.godName, this.player, this.languageString, 2 + random.nextInt(10), Messages.player(this.player.getName()));
			getPlugin().log(this.godName + " healed " + this.player.getName());
		}
	}
}
