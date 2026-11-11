package com.dogonfire.gods.task;

import java.util.Random;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.message.Voice;

/** A god's item blessing: the item appears in front of the player ({@link Task#giftSpot}) */
public class GiveItemTask extends Task
{
	private Player		player	= null;
	private Material	itemType;
	private String		godName	= null;
	private boolean		speak	= false;

	public GiveItemTask(GodsContext ctx, String god, Player p, Material material, boolean godspeak)
	{
		super(ctx);
		this.player = p;
		this.godName = god;
		this.itemType = material;
		this.speak = godspeak;
	}

	@Override
	public void run()
	{
		if (!this.player.isOnline())
		{
			return;
		}
		Location spawnLocation = giftSpot(this.player);
		spawnLocation.getWorld().dropItem(spawnLocation, new ItemStack(this.itemType, 1));
		spawnLocation.getWorld().playEffect(spawnLocation, Effect.MOBSPAWNER_FLAMES, 25);

		if (this.speak)
		{
			ctx.godSpeech().say(this.godName, this.player, Voice.BELIEVER_ITEM_BLESSING, 2 + new Random().nextInt(10), Messages.player(this.player.getName()), Messages.type(this.itemType.name()));
		}
	}
}
