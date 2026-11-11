package com.dogonfire.gods.service;

import java.util.HashMap;
import java.util.Map;

import net.kyori.adventure.text.Component;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.model.Altar;
import com.dogonfire.gods.model.AltarKind;
import com.dogonfire.gods.model.BlockPos;
import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.model.GodType;

/**
 * The text floating above praying altars (config.yml's {@code altars.floating-text}): a {@link TextDisplay} with the
 * god's name and title in its god type's style ({@link AltarTexts}). Rules:
 * <ul>
 * <li>Never saved: {@code setPersistent(false)} before the display enters the world, and tagged {@code gods:floating}
 * in its PersistentDataContainer, so {@link #start()} can remove strays a crash or a hard reload left in loaded
 * chunks.</li>
 * <li>Shown for a registered praying altar of an existing god whose sign and altar block chunks are loaded, when its
 * chunk is scanned ({@link AltarRegistry}); hidden when either chunk unloads, the altar is removed, or the feature
 * stops ({@link #stop()}, from {@code Gods.stopFeatures}: reload and disable). At most {@link AltarTexts#MAX_DISPLAYS};
 * players see it from {@code altars.floating-text-view-distance-blocks} (the display's view range).</li>
 * <li>Changes lazily: {@link #refreshGod} on the god's turn ({@code GodUpdateTask}), a gender change, a main altar
 * change and the god's creation or removal; a text is only set when it differs.</li>
 * </ul>
 * Main thread only.
 */
public class AltarTextService
{
	private final GodsContext				ctx;
	private final NamespacedKey				floatingKey;
	// The displays shown, by the altar's sign
	private final Map<BlockPos, TextDisplay>	displays	= new HashMap<>();
	private boolean							running;

	public AltarTextService(GodsContext ctx)
	{
		this.ctx = ctx;
		this.floatingKey = new NamespacedKey(ctx.plugin(), "floating");
	}

	/**
	 * Removes stray floating texts in loaded chunks, then, if config.yml switches them on, shows the texts of the
	 * registered altars that are loaded (the queued chunk scans add the rest)
	 */
	public void start()
	{
		removeStrays();
		this.running = ctx.settings().altarTexts().enabled();
		if (!this.running)
		{
			return;
		}
		for (Altar altar : ctx.altarRegistry().all())
		{
			show(altar);
		}
	}

	/** Removes every floating text (on reload and disable, and when the feature is switched off) */
	public void stop()
	{
		this.running = false;
		for (TextDisplay display : this.displays.values())
		{
			if (display.isValid())
			{
				display.remove();
			}
		}
		this.displays.clear();
	}

	/** Gods' floating texts in loaded chunks that it doesn't know (left by a crash or another copy of Gods) */
	private void removeStrays()
	{
		int removed = 0;
		for (World world : ctx.plugin().getServer().getWorlds())
		{
			for (TextDisplay display : world.getEntitiesByClass(TextDisplay.class))
			{
				if (display.getPersistentDataContainer().has(this.floatingKey, PersistentDataType.BYTE) && !this.displays.containsValue(display))
				{
					display.remove();
					removed++;
				}
			}
		}
		if (removed > 0)
		{
			ctx.plugin().log("Removed " + removed + " floating altar texts left from before.");
		}
	}

	/** Shows, updates or hides the altar's floating text as things are now */
	public void show(Altar altar)
	{
		TextDisplay display = this.displays.get(altar.sign());
		if (display != null && !display.isValid())
		{
			this.displays.remove(altar.sign());
			display = null;
		}
		String text = this.running ? textFor(altar) : null;
		if (text == null)
		{
			hide(altar.sign());
			return;
		}
		if (display != null)
		{
			if (!text.equals(display.getText()))
			{
				display.setText(text);
			}
			return;
		}
		if (this.displays.size() >= AltarTexts.MAX_DISPLAYS)
		{
			ctx.plugin().logDebug("Too many floating altar texts; none for the " + altar);
			return;
		}
		World world = ctx.plugin().getServer().getWorld(altar.world());
		if (world == null)
		{
			return;
		}
		Location at = new Location(world, altar.block().x() + 0.5, altar.block().y() + AltarTexts.HEIGHT, altar.block().z() + 0.5);
		float viewRange = Math.max(0.1F, ctx.settings().altarTexts().viewDistanceBlocks() / 64F);
		TextDisplay spawned = world.spawn(at, TextDisplay.class, setup -> {
			setup.setPersistent(false);
			setup.getPersistentDataContainer().set(this.floatingKey, PersistentDataType.BYTE, (byte) 1);
			setup.setText(text);
			setup.setBillboard(Display.Billboard.CENTER);
			setup.setAlignment(TextDisplay.TextAlignment.CENTER);
			setup.setShadowed(true);
			setup.setSeeThrough(false);
			setup.setDefaultBackground(false);
			// A faint shade behind the text, so it reads against the sky and against bright blocks
			setup.setBackgroundColor(Color.fromARGB(48, 0, 0, 0));
			setup.setBrightness(new Display.Brightness(15, 15));
			setup.setViewRange(viewRange);
		});
		this.displays.put(altar.sign(), spawned);
	}

	/** Removes the altar's floating text, if it has one */
	public void hide(BlockPos sign)
	{
		TextDisplay display = this.displays.remove(sign);
		if (display != null && display.isValid())
		{
			display.remove();
		}
	}

	/** The god's altars show what the god is now (its gender, title, mood and believers may have changed) */
	public void refreshGod(String godName)
	{
		if (!this.running || godName == null)
		{
			return;
		}
		for (Altar altar : ctx.altarRegistry().forGod(godName))
		{
			show(altar);
		}
	}

	/**
	 * The altar's text now, or null if it shows none: only praying altars of a god that exists, only main altars with
	 * {@code floating-text: main}, only with both its chunks loaded
	 */
	private String textFor(Altar altar)
	{
		if (altar.kind() != AltarKind.PRAYING || altar.god() == null || !ctx.godService().godExist(altar.god()))
		{
			return null;
		}
		boolean main = ctx.altarRegistry().isMainAltar(altar);
		if (ctx.settings().altarTexts().floatingText() == Settings.FloatingText.MAIN && !main)
		{
			return null;
		}
		if (!ctx.altarRegistry().isLoaded(altar.sign().chunk()) || !ctx.altarRegistry().isLoaded(altar.block().chunk()))
		{
			return null;
		}
		String godName = altar.god();
		GodType type = ctx.godService().getDivineForceForGod(godName);
		GodGender gender = ctx.godService().getGenderForGod(godName);
		Component title = type != null && ctx.settings().gods().titles() ? ctx.messages().godTypeName(type, gender) : Component.empty();
		String style = ctx.themes().of(type).sign().messages();
		return AltarTexts.render(ctx.messages(), style, main, Messages.god(godName), Messages.component("title", title),
				Messages.component("gender", ctx.messages().genderName(gender)), Messages.component("mood", ctx.messages().moodName(ctx.moodService().getMoodForGod(godName))),
				Messages.text("believers", ctx.believerService().getBelieversForGod(godName).size()), Messages.text("rank", ctx.rankService().getRankName(godName)));
	}
}
