package com.dogonfire.gods.integration.npc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import com.dogonfire.gods.model.NpcSkin;

/**
 * Finds the skin of a player by name, for the gods' NPCs: an online player's at once, anyone else's through the server
 * ({@link PlayerProfile#update()}, which asks Mojang off the main thread). Answers come on the main thread and are
 * kept for the life of the plugin; a name that couldn't be resolved isn't asked again for
 * {@link #RETRY_MILLIS}. The main thread never waits for it.
 * <p>
 * On Paper the answer is the signed texture (the profile's {@code textures} property); Spigot's profile API only
 * gives the skin's URL and model, so there it is an unsigned texture made from them ({@link SkinTextures#encode}),
 * which the mannequin takes but player NPCs (Citizens) don't: they get the name.
 */
public final class SkinResolver
{
	/** How long a name that couldn't be resolved is left alone */
	public static final long						RETRY_MILLIS	= 10 * 60 * 1000L;

	private final Plugin							plugin;
	// By lower case name
	private final Map<String, NpcSkin>				resolved		= new HashMap<>();
	private final Map<String, Long>					failed			= new HashMap<>();
	private final Map<String, List<Consumer<NpcSkin>>>	pending		= new HashMap<>();

	public SkinResolver(Plugin plugin)
	{
		this.plugin = plugin;
	}

	/** The skin of the name if it was resolved already, else null */
	public NpcSkin known(String name)
	{
		return this.resolved.get(name.toLowerCase(Locale.ROOT));
	}

	/**
	 * Finds the skin of the player with this name and gives it (with a texture, and the name as the profile spells it)
	 * to {@code then} on the main thread, or null if there is none (no such player, Mojang unreachable). Main thread
	 * only.
	 */
	public void resolve(String name, Consumer<NpcSkin> then)
	{
		String key = name.toLowerCase(Locale.ROOT);
		NpcSkin known = this.resolved.get(key);
		if (known != null)
		{
			then.accept(known);
			return;
		}
		Player online = this.plugin.getServer().getPlayerExact(name);
		if (online != null)
		{
			PlayerProfile profile = profileOf(online);
			NpcSkin skin = profile != null ? fromProfile(online.getName(), profile) : null;
			if (skin != null)
			{
				this.resolved.put(key, skin);
				this.failed.remove(key);
				then.accept(skin);
				return;
			}
		}
		Long failedAt = this.failed.get(key);
		if (failedAt != null && System.currentTimeMillis() - failedAt < RETRY_MILLIS)
		{
			then.accept(null);
			return;
		}
		List<Consumer<NpcSkin>> waiting = this.pending.get(key);
		if (waiting != null)
		{
			waiting.add(then);
			return;
		}
		waiting = new ArrayList<>();
		waiting.add(then);
		this.pending.put(key, waiting);

		CompletableFuture<? extends PlayerProfile> lookup;
		try
		{
			lookup = this.plugin.getServer().createPlayerProfile(name).update();
		}
		catch (RuntimeException e)
		{
			// Not a valid player name
			finish(key, name, null);
			return;
		}
		lookup.whenComplete((profile, error) -> {
			try
			{
				this.plugin.getServer().getScheduler().runTask(this.plugin, () -> finish(key, name, error == null ? profile : null));
			}
			catch (RuntimeException e)
			{
				// The plugin was disabled meanwhile: nobody waits for it any more
			}
		});
	}

	/**
	 * An online player's profile, with their skin. Called by reflection: Paper's getPlayerProfile() returns its own
	 * profile type, which Spigot 1.21.3 doesn't have, so a direct call wouldn't link there.
	 */
	private static PlayerProfile profileOf(Player player)
	{
		try
		{
			return (PlayerProfile) org.bukkit.OfflinePlayer.class.getMethod("getPlayerProfile").invoke(player);
		}
		catch (ReflectiveOperationException | RuntimeException e)
		{
			return null;
		}
	}

	private void finish(String key, String name, PlayerProfile profile)
	{
		NpcSkin skin = profile != null ? fromProfile(profile.getName() != null ? profile.getName() : name, profile) : null;
		if (skin != null)
		{
			this.resolved.put(key, skin);
			this.failed.remove(key);
		}
		else
		{
			this.failed.put(key, System.currentTimeMillis());
			this.plugin.getLogger().info("Could not find the skin of the player " + name + "; the god NPCs that wear it look like the default for now.");
		}
		List<Consumer<NpcSkin>> waiting = this.pending.remove(key);
		if (waiting != null)
		{
			for (Consumer<NpcSkin> then : waiting)
			{
				then.accept(skin);
			}
		}
	}

	/**
	 * The skin in a player profile: Paper's signed {@code textures} property where there is one, else the skin URL and
	 * model as an unsigned texture; null if the profile has no skin
	 */
	public static NpcSkin fromProfile(String name, PlayerProfile profile)
	{
		NpcSkin signed = paperTextures(name, profile);
		if (signed != null)
		{
			return signed;
		}
		PlayerTextures textures = profile.getTextures();
		if (textures == null || textures.getSkin() == null)
		{
			return null;
		}
		boolean slim = textures.getSkinModel() == PlayerTextures.SkinModel.SLIM;
		return new NpcSkin(name, SkinTextures.encode(new SkinTextures.Texture(textures.getSkin().toString(), slim)), null);
	}

	/** Paper's profile has its properties; Spigot's doesn't */
	private static NpcSkin paperTextures(String name, PlayerProfile profile)
	{
		try
		{
			Class<?> paperProfile = Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
			if (!paperProfile.isInstance(profile))
			{
				return null;
			}
			Class<?> property = Class.forName("com.destroystokyo.paper.profile.ProfileProperty");
			Method getName = property.getMethod("getName");
			Method getValue = property.getMethod("getValue");
			Method getSignature = property.getMethod("getSignature");
			for (Object each : (Collection<?>) paperProfile.getMethod("getProperties").invoke(profile))
			{
				if ("textures".equals(getName.invoke(each)))
				{
					NpcSkin skin = new NpcSkin(name, (String) getValue.invoke(each), (String) getSignature.invoke(each));
					return skin.hasTexture() ? skin : null;
				}
			}
			return null;
		}
		catch (ReflectiveOperationException | RuntimeException | LinkageError e)
		{
			return null;
		}
	}
}
