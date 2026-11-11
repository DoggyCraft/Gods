package com.dogonfire.gods.task;

import java.util.Random;

import org.bukkit.Effect;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.managers.LanguageManager;

public class HealPlayerTask extends Task
{
	private Player							player	= null;
	private String							godName	= null;
	private LanguageManager.LANGUAGESTRING	languageString;

	public HealPlayerTask(GodsContext ctx, String god, Player p, LanguageManager.LANGUAGESTRING speak)
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
		Random random = new Random();
		if (healPlayer())
		{
			ctx.languageManager().setPlayerName(this.player.getName());
			ctx.godSpeech().say(this.godName, this.player, this.languageString, 2 + random.nextInt(10));
			getPlugin().log(this.godName + " healed " + this.player.getName());
		}
	}
}
