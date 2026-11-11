package com.dogonfire.gods.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.api.event.GodGenderChangeEvent;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.model.BlockPos;
import com.dogonfire.gods.model.God;
import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.model.GodHome;
import com.dogonfire.gods.model.GodType;
import com.dogonfire.gods.storage.CleanupGuard;
import com.dogonfire.gods.storage.GodRepository;
import com.dogonfire.gods.util.LegacyDates;

/**
 * Gods themselves: creating and removing them, which gods exist and which have believers online, and what a god is
 * (type, gender, color, title, language file, seed and verbosity) and what its priests set (description, home,
 * access, PvP and mobs in its holy land). Its power comes from its believers ({@link BelieverService}). Loads gods.yml
 * ({@link GodRepository}), which the other god services (moods, relations, priests, blessings, commandments) share.
 * The pure rules are in {@link GodRules}. Main thread only.
 */
public class GodService
{
	private final Random			random		= new Random();
	private final List<String>		onlineGods	= new ArrayList<String>();

	private final GodsContext		ctx;
	private final GodRepository		repository;

	public GodService(GodsContext ctx, GodRepository repository)
	{
		this.ctx = ctx;
		this.repository = repository;
	}

	/**
	 * Reads gods.yml (an old one is upgraded first, see {@link com.dogonfire.gods.migration.GodsV1ToV2}). If the file
	 * can't be read, Storage keeps it untouched and nothing here changes it.
	 */
	public void load()
	{
		this.repository.load();
	}

	private God god(String godName)
	{
		return this.repository.find(godName);
	}

	// ---------------------------------------------------------------------
	// Which gods there are

	/** Every god in gods.yml, in file order (a copy) */
	public List<String> getAllGods()
	{
		return this.repository.names();
	}

	/** Whether the god was created at an altar (the name is matched as {@link #formatGodName} writes it) */
	public boolean godExist(String godName)
	{
		God god = god(formatGodName(godName));
		return god != null && god.created() != null;
	}

	public String formatGodName(String godName)
	{
		return GodRules.formatGodName(godName);
	}

	/** Creates a god at its first altar, with its gender, type and seed (its personality, see {@link #getSeedForGod}) */
	public void createGod(String godName, Location location, GodGender godGender, GodType godType, long seed)
	{
		setHomeForGod(godName, location);
		setGenderForGod(godName, godGender);
		setDivineForceForGod(godName, godType);
		setPrivateAccess(godName, ctx.settings().gods().privateByDefault());

		God god = this.repository.forChange(godName);
		if (god != null)
		{
			god.setCreated(LegacyDates.SECONDS.now());
			if (seed != 0L)
			{
				god.setSeed(seed);
			}
		}

		this.repository.markDirty();
	}

	/** Removes the god, its place in other gods' relations, its holy book and its holy lands */
	public void removeGod(String godName)
	{
		if (godName == null)
		{
			return;
		}

		ctx.relationService().removeRelationsWith(godName);

		this.repository.remove(godName);
		// Its altars stay registered: they still stand, and praying at one founds the god anew

		if (ctx.settings().bibles().enabled())
		{
			ctx.holyBookService().clearBible(godName);
		}

		if (ctx.settings().holyLand().enabled())
		{
			ctx.holyLandService().removeHolyLandsForGod(godName);
		}

		this.repository.save();
	}

	/**
	 * Removes the god if nobody believes in it and it has no power left; returns whether it did. Never while
	 * {@link com.dogonfire.gods.storage.Storage#cleanupVerdict()} says the believers can't be trusted for that (a file
	 * could not be read, no believers loaded, just after a (re)load): every god would look dead then.
	 */
	public boolean isDeadGod(String godName)
	{
		if (ctx.storage().cleanupVerdict() != CleanupGuard.Verdict.ALLOWED)
		{
			return false;
		}
		if (GodRules.isDead(ctx.believerService().getBelieversForGod(godName).size(), getGodPower(godName)))
		{
			removeGod(godName);

			return true;
		}
		return false;
	}

	/** The gods with a believer online (see {@link #updateOnlineGods}) */
	public List<String> getOnlineGods()
	{
		return this.onlineGods;
	}

	public List<String> getOfflineGods()
	{
		List<String> offlineGods = new ArrayList<String>();
		for (String godName : getAllGods())
		{
			if (!this.onlineGods.contains(godName))
			{
				offlineGods.add(godName);
			}
		}
		return offlineGods;
	}

	/** On join and quit: the gods with a believer online */
	public void updateOnlineGods()
	{
		this.onlineGods.clear();
		for (Player player : ctx.plugin().getServer().getOnlinePlayers())
		{
			String godName = ctx.believerService().getGodForBeliever(player.getUniqueId());
			if (godName != null)
			{
				if (!this.onlineGods.contains(godName))
				{
					this.onlineGods.add(godName);
				}
			}
		}
	}

	// ---------------------------------------------------------------------
	// What a god is

	/**
	 * @return the god's type, or null for a null name or a god that doesn't exist. A god with a missing or unknown
	 *         type gets a random one.
	 */
	public GodType getDivineForceForGod(String godName)
	{
		God god = god(godName);
		if (god == null)
		{
			return null;
		}

		if (god.type() != null)
		{
			return god.type();
		}

		Object typeName = god.extras().get("DivineForce");
		ctx.plugin().log("Could not parse GodType " + typeName + " for the god '" + godName + "'. Assigning a random GodType.");
		GodType type;
		do
		{
			type = GodType.values()[this.random.nextInt(GodType.values().length)];
		}
		while (type == GodType.WEREWOLVES);
		setDivineForceForGod(godName, type);
		return type;
	}

	public void setDivineForceForGod(String godName, GodType divineForce)
	{
		God god = this.repository.forChange(godName);
		if (god != null)
		{
			god.setType(divineForce);
		}

		this.repository.save();
	}

	public GodGender getGenderForGod(String godName)
	{
		God god = god(godName);
		return god != null && god.gender() != null ? god.gender() : GodGender.None;
	}

	public void setGenderForGod(String godName, GodGender godGender)
	{
		God god = this.repository.forChange(godName);
		if (god != null)
		{
			god.setGender(godGender);
		}

		this.repository.markDirty();
	}

	/**
	 * Changes the god's gender, for /g gender and the API (the cooldown is the caller's to check): fires
	 * {@link GodGenderChangeEvent}, then sets the gender and when it changed, saves, and the god tells its believers in
	 * its new voice. Its title and the altar signs that show it follow (signs when next used).
	 *
	 * @return false if a listener cancelled it (nothing changed)
	 */
	public boolean changeGender(String godName, GodGender gender)
	{
		GodGender oldGender = getGenderForGod(godName);
		if (ctx.fire(new GodGenderChangeEvent(godName, ApiSnapshots.gender(oldGender), ApiSnapshots.gender(gender))).isCancelled())
		{
			return false;
		}

		God god = this.repository.forChange(godName);
		if (god != null)
		{
			god.setGender(gender);
			god.setGenderChangedTime(LegacyDates.SECONDS.now());
		}
		this.repository.save();

		ctx.plugin().log("The gender of " + godName + " changed from " + oldGender + " to " + gender);
		ctx.godSpeech().sayToBelievers(godName, Voice.BELIEVERS_GENDER_CHANGED, 2, genderTag(godName));
		return true;
	}

	/** When the god's gender was last changed (/g gender or the API), or null for never */
	public Instant getGenderChangedTime(String godName)
	{
		God god = god(godName);
		return god != null ? god.genderChangedTime() : null;
	}

	/** {@code <gender>}: the name of the god's gender ("God", "Goddess", "Deity"), for its words and messages */
	public TagResolver genderTag(String godName)
	{
		return Messages.component("gender", ctx.messages().genderName(getGenderForGod(godName)));
	}

	public ChatColor getColorForGod(String godName)
	{
		return getColorForGodType(getDivineForceForGod(godName));
	}

	public ChatColor getColorForGodType(GodType godType)
	{
		return GodRules.color(godType);
	}

	/** "God of Thunder" and the like, or "" when titles are off */
	public String getTitleForGod(String godName)
	{
		if (!ctx.settings().gods().titles())
		{
			return "";
		}
		GodType godType = getDivineForceForGod(godName);
		if (godType == null)
		{
			return "";
		}
		return Messages.legacy(ctx.messages().godTypeName(godType, getGenderForGod(godName)));
	}

	/**
	 * @return the voices the god speaks with, e.g. [sun_male], from its type and gender ({@link GodRules#voices}). A god
	 *         without a gender always gets the same one, picked from its seed; a non-binary god gets its type's male
	 *         and female voice, and each line it says comes from one of them.
	 */
	public List<String> getVoicesForGod(String godName)
	{
		GodType godType = getDivineForceForGod(godName);
		GodGender godGender = godType != null ? getGenderForGod(godName) : null;
		long seed = godGender == GodGender.None ? getSeedForGod(godName) : 0L;
		return GodRules.voices(godType, godGender, seed);
	}

	/** A new seed for a god about to be created (never 0, which means none) */
	public long newSeed()
	{
		long seed;
		do
		{
			seed = this.random.nextLong();
		}
		while (seed == 0L);
		return seed;
	}

	/** The god's personality: a random number, made once and kept */
	public long getSeedForGod(String godName)
	{
		God god = god(godName);
		long seed = god != null ? god.seed() : 0L;
		if (seed == 0L)
		{
			seed = this.random.nextLong();
			god = this.repository.forChange(godName);
			if (god != null)
			{
				god.setSeed(seed);
			}

			this.repository.markDirty();
		}
		return seed;
	}

	/** How talkative the god is now (1 or more); see {@link GodRules#verbosity} */
	public int getVerbosityForGod(String godName)
	{
		God god = god(godName);
		int verbosity = god != null ? god.verbosity() : 0;
		if (verbosity == 0)
		{
			verbosity = 1 + this.random.nextInt(50);

			god = this.repository.forChange(godName);
			if (god != null)
			{
				god.setVerbosity(verbosity);
			}

			this.repository.save();
		}

		return GodRules.verbosity(verbosity, getSeedForGod(godName), getGodPower(godName), ctx.settings().gods().verbosity(), System.currentTimeMillis());
	}

	/** The power of all its believers (at least the whitelist's minimum); 0 for a god that doesn't exist */
	public float getGodPower(String godName)
	{
		if (god(godName) == null)
		{
			return 0.0F;
		}

		int minGodPower = 0;
		if (ctx.settings().gods().whitelist())
		{
			minGodPower = (int) ctx.godListService().getMinGodPower(godName);
		}

		Set<UUID> believers = ctx.believerService().getBelieversForGod(godName);
		List<Float> powers = new ArrayList<Float>();
		for (UUID believerId : believers)
		{
			powers.add(ctx.believerService().getBelieverPower(believerId));
		}
		return GodRules.godPower(powers, minGodPower);
	}

	// ---------------------------------------------------------------------
	// What its priests set

	public String getGodDescription(String godName)
	{
		God god = god(godName);
		String description = god != null ? god.description() : null;
		if (description == null)
		{
			description = "No description :/";
		}
		return description;
	}

	public void setGodDescription(String godName, String description)
	{
		God god = description == null ? god(godName) : this.repository.forChange(godName);
		if (god != null)
		{
			god.setDescription(description);
		}

		this.repository.markDirty();
	}

	/** Where the god lives (its first altar, or /g sethome), or null */
	public Location getHomeForGod(String godName)
	{
		God god = god(godName);
		GodHome home = god != null ? god.home() : null;
		if (home == null)
		{
			return null;
		}

		return new Location(ctx.plugin().getServer().getWorld(home.world()), home.x(), home.y(), home.z());
	}

	public void setHomeForGod(String godName, Location location)
	{
		God god = this.repository.forChange(godName);
		if (god != null)
		{
			god.setHome(new GodHome(location.getWorld().getName(), location.getX(), location.getY(), location.getZ()));
		}

		this.repository.markDirty();
	}

	/** The sign of the god's main altar (/g setaltar), or null; see {@link AltarRegistry#mainAltar} */
	public BlockPos getMainAltar(String godName)
	{
		God god = god(godName);
		return god != null ? god.mainAltar() : null;
	}

	/** Makes the altar with this sign the god's main altar (null: none); saved now */
	public void setMainAltar(String godName, BlockPos sign)
	{
		God god = god(godName);
		if (god == null || Objects.equals(god.mainAltar(), sign))
		{
			return;
		}
		god.setMainAltar(sign);
		this.repository.save();
	}

	/** Whether only the god's believers may use its altars */
	public boolean isPrivateAccess(String godName)
	{
		God god = god(godName);
		return god != null && god.privateAccess();
	}

	public void setPrivateAccess(String godName, boolean privateAccess)
	{
		God god = this.repository.forChange(godName);
		if (god != null)
		{
			god.setPrivateAccess(privateAccess);
		}

		this.repository.markDirty();
	}

	/** Whether the player may use the god's altars: it is public, or they believe in it */
	public boolean hasGodAccess(UUID believerId, String godName)
	{
		if (!isPrivateAccess(godName))
		{
			return true;
		}

		String currentGodName = ctx.believerService().getGodForBeliever(believerId);

		return currentGodName != null && currentGodName.equals(godName);
	}

	/** PvP in the god's holy land (config.yml's default unless a priest set it) */
	public boolean getGodPvP(String godName)
	{
		God god = god(godName);
		return god != null && god.pvp() != null ? god.pvp() : ctx.settings().holyLand().defaultPvp();
	}

	public void setGodPvP(String godName, boolean pvp)
	{
		God god = this.repository.forChange(godName);
		if (god != null)
		{
			god.setPvp(pvp);
		}

		this.repository.markDirty();
	}

	/** Mob damage in the god's holy land (config.yml's default unless set) */
	public boolean getGodMobDamage(String godName)
	{
		God god = god(godName);
		return god != null && god.mobDamage() != null ? god.mobDamage() : ctx.settings().holyLand().defaultMobDamage();
	}

	/** Mob spawning in the god's holy land (off unless set) */
	public boolean getGodMobSpawning(String godName)
	{
		God god = god(godName);
		return god != null && god.mobSpawning() != null && god.mobSpawning();
	}

	// ---------------------------------------------------------------------
	// Quests

	/**
	 * @return minutes since the god's last quest started, or Long.MAX_VALUE if it never had one
	 */
	public long getMinutesSinceLastQuest(String godName)
	{
		God god = god(godName);
		if (god == null || god.lastQuestTime() == null)
		{
			return Long.MAX_VALUE;
		}

		return (System.currentTimeMillis() - god.lastQuestTime().toEpochMilli()) / 60000L;
	}

	/** A quest started now */
	public void setTimeSinceLastQuest(String godName)
	{
		God god = this.repository.forChange(godName);
		if (god != null)
		{
			god.setLastQuestTime(LegacyDates.SECONDS.now());
		}

		this.repository.markDirty();
	}
}
