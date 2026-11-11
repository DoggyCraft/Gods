package com.dogonfire.gods.task;

import java.util.Random;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.model.GodType;

public class GiveHolyArtifactTask extends Task
{
	private Player	player	= null;
	private String	godName	= null;
	private GodType	godType	= null;
	private boolean	speak	= false;

	public GiveHolyArtifactTask(GodsContext ctx, String god, GodType godType, Player p, boolean godspeak)
	{
		super(ctx);
		this.player = p;
		this.godName = new String(god);
		this.godType = godType;
		this.speak = godspeak;
	}

	private boolean giveItem()
	{
		Vector dir = this.player.getLocation().getDirection();
		dir.setY(0);
		Location spawnLocation = this.player.getLocation().toVector().add(dir.multiply(4)).toLocation(this.player.getWorld());

		spawnLocation.setY(spawnLocation.getY() + 2.0D);
		if (spawnLocation.getBlock().getType() != Material.AIR)
		{
			getPlugin().logDebug("Could not giveItem(): Not air infront of " + this.player.getName());
			return false;
		}
		try
		{
			getPlugin().logDebug("Creating holy artifact for " + this.player.getName());

			ctx.holyArtifactManager().createHolyArtifact(this.godType, this.godName, spawnLocation);

			spawnLocation.getWorld().playEffect(spawnLocation, Effect.MOBSPAWNER_FLAMES, 25);
		}
		catch (Exception ex)
		{
			getPlugin().log("Could not giveItem(): " + ex.getMessage());
			return false;
		}
		return true;
	}

	@Override
	public void run()
	{
		if (giveItem())
		{
			Random random = new Random();
			if (this.speak)
			{
				ctx.languageManager().setPlayerName(this.player.getName());
				ctx.godSpeech().say(this.godName, this.player, LanguageManager.LANGUAGESTRING.GodToBelieverHolyArtifactBlessing, 2 + random.nextInt(10));
			}
		}
	}
}
