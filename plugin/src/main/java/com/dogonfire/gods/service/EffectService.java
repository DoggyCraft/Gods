package com.dogonfire.gods.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.config.Settings;
import com.dogonfire.gods.config.Theme;
import com.dogonfire.gods.model.GodType;

/**
 * Short effects in the style of a god's type (the {@code effects} of its theme in themes.yml): at prayers, sacrifices,
 * blessings and curses. Particles and sounds go only to players within config.yml's view distance, and nothing at all
 * happens when nobody is that close. Floating items and symbols are display entities that are never saved
 * ({@code setPersistent(false)}), move by client-side interpolation, are removed when their effect ends and, through
 * {@link #stop()}, when Gods stops or reloads. An effect is a few delayed tasks, never a repeating one. Main thread
 * only. The numbers are in {@link EffectRules}.
 */
public class EffectService
{
	// Particles whose data Gods can't fill in (themes.yml leaves them out already)
	private static final Object		UNSUPPORTED	= new Object();

	private final GodsContext		ctx;
	private final Random			random		= new Random();
	// The displays of effects still running
	private final Set<Display>		displays	= new HashSet<>();

	public EffectService(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	/**
	 * A believer prayed at this altar block: themed particles rise around it with the god's sound, brighter and higher
	 * when the god is pleased, darker and short when it is angry; the theme's symbol floats up from the light unless
	 * the god is angry.
	 */
	public void prayer(String godName, Block altarBlock)
	{
		if (!effects().prayerOn() || altarBlock == null)
		{
			return;
		}
		Location center = altarBlock.getLocation().add(0.5, 0.2, 0.5);
		List<Player> viewers = viewers(center);
		if (viewers.isEmpty())
		{
			return;
		}
		Theme.Effects theme = themeOf(godName).effects();
		Theme.EffectMood mood = EffectRules.moodOf(ctx.moodService().getMoodForGod(godName));
		Theme.EffectStyle style = theme.of(mood);
		EffectRules.Waves waves = EffectRules.prayer(mood);

		sound(viewers, center, style, SoundCategory.BLOCKS, waves.pitch());
		waves(viewers, () -> center, style, waves);
		if (EffectRules.showsSymbol(mood, theme.symbol()))
		{
			symbol(center.clone().add(0, 1.4, 0), theme.symbol(), firstColor(style), mood == Theme.EffectMood.PLEASED ? 1.3 : 0.8);
		}
	}

	/**
	 * The god descends to its main altar (its NPC appears there): the theme's pleased particles and sound rise around
	 * the spot and its symbol floats up high. With effects switched off nothing is shown (the lightning that comes with
	 * it is the caller's).
	 */
	public void descent(String godName, Location at)
	{
		if (!effects().enabled() || at == null)
		{
			return;
		}
		List<Player> viewers = viewers(at);
		if (viewers.isEmpty())
		{
			return;
		}
		Theme.Effects theme = themeOf(godName).effects();
		Theme.EffectStyle style = theme.of(Theme.EffectMood.PLEASED);
		EffectRules.Waves waves = EffectRules.prayer(Theme.EffectMood.PLEASED);
		sound(viewers, at, style, SoundCategory.BLOCKS, waves.pitch());
		waves(viewers, () -> at, style, waves);
		if (EffectRules.showsSymbol(Theme.EffectMood.PLEASED, theme.symbol()))
		{
			symbol(at.clone().add(0, 2.2, 0), theme.symbol(), firstColor(style), 1.6);
		}
	}

	/**
	 * A sacrifice burns: the item floats up from the fire, spins and shrinks away, with the god's pleased (wanted),
	 * neutral or angry (unwanted) particles and sound.
	 */
	public void sacrifice(String godName, Item burning, Theme.EffectMood mood)
	{
		if (!effects().sacrificeOn() || burning == null)
		{
			return;
		}
		Location start = burning.getLocation().add(0, 0.4, 0);
		List<Player> viewers = viewers(start);
		if (viewers.isEmpty())
		{
			return;
		}
		Theme.EffectStyle style = themeOf(godName).effects().of(mood);
		EffectRules.Rise rise = EffectRules.sacrifice(mood);
		sound(viewers, start, style, SoundCategory.BLOCKS, rise.pitch());

		ItemStack shown = burning.getItemStack().clone();
		shown.setAmount(1);
		ItemDisplay display = spawn(start, ItemDisplay.class, item -> {
			item.setItemStack(shown);
			item.setBillboard(Display.Billboard.FIXED);
			item.setTransformation(transformation(0, 0, false, 0.5F));
		});
		boolean angry = mood == Theme.EffectMood.ANGRY;
		for (int step = 1; step <= rise.steps(); step++)
		{
			int at = step;
			// One tick after spawning, so the client has the start to interpolate from
			later(2 + (long) (step - 1) * rise.ticksPerStep(), () -> {
				float scale = at == rise.steps() ? 0F : 0.5F - 0.1F * at;
				float height = (float) (rise.rise() * at / rise.steps());
				// An angry god casts it about, a pleased one spins it
				float angle = angry ? rise.spinDegrees() * (at % 2 == 0 ? 1 : -1) : rise.spinDegrees() * at;
				move(display, transformation(height, angle, angry, scale), rise.ticksPerStep());
				Location now = start.clone().add(0, height, 0);
				particles(viewers, now, style, 3, 0.15, at);
			});
		}
		later(2 + (long) rise.ticks(), () -> {
			particles(viewers, start.clone().add(0, rise.rise(), 0), style, 10, 0.3, 0);
			remove(display);
		});
	}

	/** A god blessed this player: a soft glow in its colors rises around them, with a high chime */
	public void blessing(String godName, Player player)
	{
		aroundPlayer(godName, player, Theme.EffectMood.PLEASED, EffectRules.BLESSING, 0.1);
	}

	/** A god cursed this player: a dark swirl sinks around them, with a low sound */
	public void curse(String godName, Player player)
	{
		aroundPlayer(godName, player, Theme.EffectMood.ANGRY, EffectRules.CURSE, 2.0);
	}

	private void aroundPlayer(String godName, Player player, Theme.EffectMood mood, EffectRules.Waves waves, double startHeight)
	{
		if (!effects().blessingsOn() || player == null || !player.isOnline())
		{
			return;
		}
		List<Player> viewers = viewers(player.getLocation());
		if (viewers.isEmpty())
		{
			return;
		}
		Theme.EffectStyle style = themeOf(godName).effects().of(mood);
		sound(viewers, player.getLocation(), style, SoundCategory.PLAYERS, waves.pitch());
		// Follows the player while they move; stops if they leave or die
		waves(viewers, () -> player.isOnline() && !player.isDead() ? player.getLocation().add(0, startHeight, 0) : null, style, waves);
	}

	/** Removes the displays of every running effect (on plugin stop and reload) */
	public void stop()
	{
		for (Display display : this.displays)
		{
			if (display.isValid())
			{
				display.remove();
			}
		}
		this.displays.clear();
	}

	// ---------------------------------------------------------------------

	private Settings.Effects effects()
	{
		return ctx.settings().effects();
	}

	/** The theme of a god's type (the default theme for a god without one) */
	private Theme themeOf(String godName)
	{
		GodType type = godName != null && ctx.godService().godExist(godName) ? ctx.godService().getDivineForceForGod(godName) : null;
		return ctx.themes().of(type);
	}

	/** The players close enough to see and hear an effect here */
	private List<Player> viewers(Location at)
	{
		int distance = effects().viewDistanceBlocks();
		List<Player> viewers = new ArrayList<>();
		if (at.getWorld() == null)
		{
			return viewers;
		}
		for (Player player : at.getWorld().getPlayers())
		{
			if (EffectRules.inView(player.getLocation().distanceSquared(at), distance))
			{
				viewers.add(player);
			}
		}
		return viewers;
	}

	private void sound(List<Player> viewers, Location at, Theme.EffectStyle style, SoundCategory category, float pitch)
	{
		String sound = EffectRules.pick(style.sounds(), this.random);
		if (sound == null)
		{
			return;
		}
		for (Player viewer : viewers)
		{
			if (viewer.isOnline())
			{
				viewer.playSound(at, sound, category, 0.7F, pitch);
			}
		}
	}

	/** Rings of particles, one per wave, around wherever {@code center} says (null stops them) */
	private void waves(List<Player> viewers, Supplier<Location> center, Theme.EffectStyle style, EffectRules.Waves waves)
	{
		if (style.particles().isEmpty())
		{
			return;
		}
		for (int wave = 0; wave < waves.waves(); wave++)
		{
			int at = wave;
			later((long) wave * waves.ticksApart(), () -> {
				Location origin = center.get();
				if (origin == null)
				{
					return;
				}
				for (int index = 0; index < waves.particles(); index++)
				{
					double[] offset = EffectRules.ringOffset(waves, at, index);
					Particle particle = style.particles().get((at + index) % style.particles().size());
					Color color = colorAt(style, index);
					spawnParticle(viewers, particle, origin.clone().add(offset[0], offset[1], offset[2]), 1, 0.03, color, colorAt(style, index + 1));
				}
			});
		}
	}

	/** A few particles of the style around a spot */
	private void particles(List<Player> viewers, Location at, Theme.EffectStyle style, int count, double spread, int turn)
	{
		if (style.particles().isEmpty())
		{
			return;
		}
		Particle particle = style.particles().get(turn % style.particles().size());
		spawnParticle(viewers, particle, at, count, spread, colorAt(style, turn), colorAt(style, turn + 1));
	}

	private void spawnParticle(List<Player> viewers, Particle particle, Location at, int count, double spread, Color color, Color fade)
	{
		Object data = data(particle, color, fade);
		if (data == UNSUPPORTED)
		{
			return;
		}
		for (Player viewer : viewers)
		{
			if (!viewer.isOnline())
			{
				continue;
			}
			if (data == null)
			{
				viewer.spawnParticle(particle, at, count, spread, spread, spread, 0.01);
			}
			else
			{
				viewer.spawnParticle(particle, at, count, spread, spread, spread, 0.01, data);
			}
		}
	}

	/** The data a particle needs: none, a color, dust or a color change in the style's colors, or a number */
	private static Object data(Particle particle, Color color, Color fade)
	{
		Class<?> type = particle.getDataType();
		if (type == Void.class)
		{
			return null;
		}
		if (type == Particle.DustOptions.class)
		{
			return new Particle.DustOptions(color, 1.0F);
		}
		if (type == Particle.DustTransition.class)
		{
			return new Particle.DustTransition(color, fade, 1.0F);
		}
		if (type == Color.class)
		{
			return color;
		}
		if (type == Float.class)
		{
			return 0.0F;
		}
		if (type == Integer.class)
		{
			return 0;
		}
		return UNSUPPORTED;
	}

	private static Color colorAt(Theme.EffectStyle style, int index)
	{
		return style.colors().isEmpty() ? Color.WHITE : style.colors().get(Math.floorMod(index, style.colors().size()));
	}

	private static Color firstColor(Theme.EffectStyle style)
	{
		return colorAt(style, 0);
	}

	/** The theme's symbol floats up from here in the style's color, grows a little and shrinks away */
	private void symbol(Location at, String symbol, Color color, double rise)
	{
		TextDisplay display = spawn(at, TextDisplay.class, text -> {
			text.setText(EffectRules.legacyColor(color.asRGB()) + symbol);
			text.setBillboard(Display.Billboard.CENTER);
			text.setShadowed(true);
			text.setDefaultBackground(false);
			text.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
			text.setTransformation(transformation(0, 0, false, 0.6F));
		});
		later(2, () -> move(display, transformation((float) (rise * 0.6), 0, false, 1.4F), 14));
		later(16, () -> move(display, transformation((float) rise, 0, false, 0F), 12));
		later(29, () -> remove(display));
	}

	/**
	 * A display that is never saved, glows at full brightness and is seen from as far as the effects are; null when
	 * too many effect displays are alive already
	 */
	private <T extends Display> T spawn(Location at, Class<T> type, Consumer<T> setup)
	{
		if (this.displays.size() >= EffectRules.MAX_DISPLAYS || at.getWorld() == null)
		{
			return null;
		}
		float viewRange = Math.max(0.1F, effects().viewDistanceBlocks() / 64F);
		T display = at.getWorld().spawn(at, type, spawned -> {
			spawned.setPersistent(false);
			spawned.setBrightness(new Display.Brightness(15, 15));
			spawned.setViewRange(viewRange);
			setup.accept(spawned);
		});
		this.displays.add(display);
		return display;
	}

	/** Moves a display to its new transformation; the client interpolates over {@code ticks} */
	private static void move(Display display, Transformation transformation, int ticks)
	{
		if (display == null || !display.isValid())
		{
			return;
		}
		display.setInterpolationDelay(0);
		display.setInterpolationDuration(ticks);
		display.setTransformation(transformation);
	}

	private void remove(Display display)
	{
		if (display == null)
		{
			return;
		}
		this.displays.remove(display);
		if (display.isValid())
		{
			display.remove();
		}
	}

	/** Up by {@code height}, turned {@code degrees} around the vertical (or, tilted, around the line of sight) */
	private static Transformation transformation(float height, float degrees, boolean tilted, float scale)
	{
		float radians = (float) Math.toRadians(degrees);
		Quaternionf rotation = tilted ? new Quaternionf().rotateZ(radians) : new Quaternionf().rotateY(radians);
		return new Transformation(new Vector3f(0, height, 0), rotation, new Vector3f(scale, scale, scale), new Quaternionf());
	}

	private void later(long ticks, Runnable task)
	{
		if (ticks <= 0)
		{
			task.run();
			return;
		}
		ctx.plugin().getServer().getScheduler().runTaskLater(ctx.plugin(), task, ticks);
	}
}
