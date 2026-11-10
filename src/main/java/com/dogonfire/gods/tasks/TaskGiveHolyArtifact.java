package com.dogonfire.gods.tasks;

import java.util.Random;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.GodManager.GodType;
import com.dogonfire.gods.managers.HolyArtifactManager;
import com.dogonfire.gods.managers.LanguageManager;

public class TaskGiveHolyArtifact extends Task
{
	private Player	player	= null;
	private String	godName	= null;
	private GodType	godType	= null;
	private boolean	speak	= false;

	public TaskGiveHolyArtifact(String god, GodType godType, Player p, boolean godspeak)
	{
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

			HolyArtifactManager.instance().createHolyArtifact(this.player.getName(), this.godType, this.godName, spawnLocation);

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
				LanguageManager.instance().setPlayerName(this.player.getName());
				GodManager.instance().GodSay(this.godName, this.player, LanguageManager.LANGUAGESTRING.GodToBelieverHolyArtifactBlessing, 2 + random.nextInt(10));
			}
		}
	}
}
