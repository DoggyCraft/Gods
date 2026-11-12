package com.dogonfire.gods.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.config.Theme;
import com.dogonfire.gods.integration.GriefPreventionIntegration;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.model.Altar;
import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.model.GodType;

/**
 * Altars: a block of an altar type (its god type's theme, or config.yml) with a light on top ({@link AltarLights}) and
 * a wall sign or wall hanging sign on its side ({@link AltarGeometry}). Recognizes altar blocks and signs, turns newly written signs into praying, cursing and blessing
 * altars (the text rules are in {@link AltarSigns}), and remembers who dropped which item, for sacrifices. Has no data
 * file. The events are handled by {@code listener/AltarListener}. Main thread only.
 */
public class AltarService
{
	private Random							random			= new Random();
	private final DroppedItems			droppedItems	= new DroppedItems();
	// The altar lights this server has; resolved when first needed
	private Set<Material>				altarLights;

	private final GodsContext ctx;

	// The tags of altar signs: gods:altar (praying, curses, blessings), gods:altar_god, gods:altar_player (UUID) and
	// gods:altar_player_name
	private final NamespacedKey		kindKey;
	private final NamespacedKey		godKey;
	private final NamespacedKey		playerKey;
	private final NamespacedKey		playerNameKey;

	public AltarService(GodsContext ctx)
	{
		this.ctx = ctx;
		this.kindKey = new NamespacedKey(ctx.plugin(), "altar");
		this.godKey = new NamespacedKey(ctx.plugin(), "altar_god");
		this.playerKey = new NamespacedKey(ctx.plugin(), "altar_player");
		this.playerNameKey = new NamespacedKey(ctx.plugin(), "altar_player_name");
	}

	/** Remembers that the player dropped this item, so it counts as their sacrifice if it burns */
	public void addDroppedItem(Item item, UUID playerId)
	{
		long now = System.currentTimeMillis();
		if (this.droppedItems.size() > 256)
		{
			pruneDroppedItems(now);
		}
		this.droppedItems.add(item.getUniqueId(), playerId, now);
	}

	/** Forgets dropped items that despawned, were picked up or burned */
	public void clearDroppedItems()
	{
		int removed = pruneDroppedItems(System.currentTimeMillis());
		ctx.plugin().logDebug("Forgot " + removed + " dropped items, " + this.droppedItems.size() + " left");
	}

	private int pruneDroppedItems(long now)
	{
		return this.droppedItems.prune(now, itemId -> {
			Entity entity = ctx.plugin().getServer().getEntity(itemId);
			return entity == null || !entity.isValid();
		});
	}

	/**
	 * Any wooden wall sign or wall hanging sign (oak, spruce, ..., bamboo, crimson, warped) can be an altar sign; standing
	 * and ceiling hanging signs can't ({@link AltarGeometry}).
	 */
	public static boolean isAltarSignType(Block block)
	{
		return mount(block) != null;
	}

	/** How the sign is mounted, or null if it is no wall sign or wall hanging sign */
	public static AltarGeometry.Mount mount(Block block)
	{
		if (block == null)
		{
			return null;
		}
		if (Tag.WALL_SIGNS.isTagged(block.getType()))
		{
			return AltarGeometry.Mount.WALL;
		}
		return Tag.WALL_HANGING_SIGNS.isTagged(block.getType()) ? AltarGeometry.Mount.WALL_HANGING : null;
	}

	/** The sides of a sign that show an altar's text: both for a hanging sign, the front for a wall sign */
	private static List<Side> textSides(Block block)
	{
		return mount(block) == AltarGeometry.Mount.WALL_HANGING ? List.of(Side.FRONT, Side.BACK) : List.of(Side.FRONT);
	}

	/**
	 * The sign item that a wall sign or wall hanging sign block drops, e.g. SPRUCE_SIGN for SPRUCE_WALL_SIGN and
	 * SPRUCE_HANGING_SIGN for SPRUCE_WALL_HANGING_SIGN.
	 */
	public static ItemStack getSignItem(Material wallSignType)
	{
		Material item = Material.getMaterial(wallSignType.name().replace("_WALL_", "_"));

		return new ItemStack((item != null && item.isItem()) ? item : Material.OAK_SIGN, 1);
	}

	/**
	 * Tags, styles and waxes the sign of a new altar one tick later (after the text from the sign editor has been
	 * applied), so it is known by its tags and players can't open the sign editor on it.
	 */
	public void finishAltarSignLater(Block signBlock, AltarSigns.AltarSign altar, UUID builder)
	{
		ctx.plugin().getServer().getScheduler().runTask(ctx.plugin(), () -> {
			if (!isAltarSignType(signBlock))
			{
				// Gone within the tick; a god founded at it needs another main altar
				ctx.altarRegistry().notBuilt(AltarRegistry.pos(signBlock), altar.god());
				return;
			}
			markAltarSign(signBlock, altar);
			ctx.altarRegistry().register(signBlock, altar, builder);
		});
	}

	/** The altar a sign is (by its tags, or by its text for altars from before tags), or null if it is none */
	public AltarSigns.AltarSign readAltarSign(Block block)
	{
		if (block == null || !(block.getState() instanceof Sign sign))
		{
			return null;
		}
		PersistentDataContainer tags = sign.getPersistentDataContainer();
		return AltarSigns.recognize(tags.get(this.kindKey, PersistentDataType.STRING), tags.get(this.godKey, PersistentDataType.STRING), tags.get(this.playerNameKey, PersistentDataType.STRING),
				tags.get(this.playerKey, PersistentDataType.STRING), sign.getSide(Side.FRONT).getLines());
	}

	/**
	 * The altar a sign is by its tags only (no text check, no block lookups: for scanning a chunk's tile entities), or
	 * null if it has no {@code gods:altar} tag
	 */
	public AltarSigns.AltarSign readTaggedAltar(Sign sign)
	{
		PersistentDataContainer tags = sign.getPersistentDataContainer();
		String kind = tags.get(this.kindKey, PersistentDataType.STRING);
		if (kind == null || AltarSigns.Kind.byKey(kind) == null)
		{
			return null;
		}
		return AltarSigns.recognize(kind, tags.get(this.godKey, PersistentDataType.STRING), tags.get(this.playerNameKey, PersistentDataType.STRING),
				tags.get(this.playerKey, PersistentDataType.STRING), null);
	}

	/**
	 * The block a wall sign or wall hanging sign hangs on (whatever it is: the block behind a wall sign; for a hanging
	 * sign the side {@link AltarGeometry#attached} picks), or null if the block is neither. Never loads a chunk: a
	 * neighbor in an unloaded chunk counts as no altar block that may hold the sign, so the registry keeps an altar block
	 * it knows there.
	 */
	public Block attachedBlock(Block sign)
	{
		BlockFace side = attachedSide(sign, false);
		return side != null ? sign.getRelative(side) : null;
	}

	/**
	 * The side of the sign its block is on ({@link AltarGeometry#attached}), or null if it is no altar sign type; with
	 * {@code mustBeLit}, null unless that block is a lit altar block
	 */
	private BlockFace attachedSide(Block sign, boolean mustBeLit)
	{
		AltarGeometry.Mount mount = mount(sign);
		if (mount == null || !(sign.getBlockData() instanceof Directional directional))
		{
			return null;
		}
		BlockFace facing = directional.getFacing();
		if (mount == AltarGeometry.Mount.WALL && !mustBeLit)
		{
			// The block behind it, without reading it
			return facing.getOppositeFace();
		}
		Altar known = ctx.altarRegistry().atSign(AltarRegistry.pos(sign));
		Predicate<BlockFace> isKnown = side -> known != null && known.block().equals(AltarRegistry.pos(sign.getRelative(side)));
		// A side in an unloaded chunk can't be read: it may hold the sign (so a known altar block there is kept)
		Predicate<BlockFace> holds = side -> !isLoaded(sign.getRelative(side)) || sign.getRelative(side).getType().isSolid();
		Predicate<BlockFace> lit = side -> isLoaded(sign.getRelative(side)) && isLitAltarBlock(sign.getRelative(side));
		return mustBeLit ? AltarGeometry.altarSide(mount, facing, isKnown, holds, lit) : AltarGeometry.attached(mount, facing, isKnown, holds, lit);
	}

	/** Whether the block's chunk is loaded (reading a block in an unloaded chunk would load it) */
	private static boolean isLoaded(Block block)
	{
		return block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4);
	}

	/** The online player a curse or blessing sign is about, or null */
	public Player getAltarSignPlayer(AltarSigns.AltarSign altar)
	{
		if (altar.playerId() != null)
		{
			return ctx.plugin().getServer().getPlayer(altar.playerId());
		}
		// Exact name: getPlayer(String) would also match name prefixes
		return altar.playerName() != null ? ctx.plugin().getServer().getPlayerExact(altar.playerName()) : null;
	}

	/**
	 * Brings an altar sign up to date when it is used: an altar from before tags is tagged (with the god of whoever uses
	 * a curse or blessing sign, for its style, and the player it names if they are online), and the sign is written in
	 * its god type's style if it isn't (the god's type or the themes may have changed). Returns the altar as tagged.
	 */
	public AltarSigns.AltarSign refreshAltarSign(Block block, AltarSigns.AltarSign altar, Player user)
	{
		AltarSigns.AltarSign tagged = altar;
		if (altar.kind() == AltarSigns.Kind.PRAYING && (altar.god() == null || altar.god().length() <= 1))
		{
			// Not a proper god name; left as it is
			return altar;
		}
		if (!altar.tagged())
		{
			if (altar.kind() == AltarSigns.Kind.PRAYING)
			{
				tagged = altar.asTagged(ctx.godService().formatGodName(altar.god()), null);
			}
			else
			{
				Player named = getAltarSignPlayer(altar);
				tagged = altar.asTagged(ctx.believerService().getGodForBeliever(user.getUniqueId()), named != null ? named.getUniqueId() : null);
			}
			ctx.plugin().logDebug("Tagging the " + altar.kind().key() + " altar sign at " + block.getLocation());
		}

		if (!altar.tagged() || !isStyled(block, tagged))
		{
			markAltarSign(block, tagged);
		}
		return tagged;
	}

	/** True if the sign shows the altar's text in its style */
	private boolean isStyled(Block block, AltarSigns.AltarSign altar)
	{
		if (!(block.getState() instanceof Sign sign))
		{
			return true;
		}
		Theme.SignStyle style = signStyle(altar);
		String[] text = signText(altar);
		for (Side side : textSides(block))
		{
			SignSide signSide = sign.getSide(side);
			// The text without its colors: the server may write the same colors with other codes
			String[] lines = signSide.getLines();
			for (int line = 0; line < text.length; line++)
			{
				if (!AltarSigns.plain(line < lines.length ? lines[line] : "").equals(AltarSigns.plain(text[line])))
				{
					return false;
				}
			}
			if (signSide.getColor() != style.dye() || signSide.isGlowingText() != style.glowing())
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Tags the sign as this altar (its kind, god and player) and writes it in the god type's style (a wall sign's front,
	 * both sides of a hanging sign): the text of {@code altar.sign.<style>.<kind>} in messages.yml, the theme's dye color
	 * and glowing text. Waxes it, so players can't edit it.
	 */
	public void markAltarSign(Block block, AltarSigns.AltarSign altar)
	{
		if (!(block.getState() instanceof Sign sign))
		{
			return;
		}
		PersistentDataContainer tags = sign.getPersistentDataContainer();
		tags.set(this.kindKey, PersistentDataType.STRING, altar.kind().key());
		setOrRemove(tags, this.godKey, altar.god());
		setOrRemove(tags, this.playerNameKey, altar.playerName());
		setOrRemove(tags, this.playerKey, altar.playerId() != null ? altar.playerId().toString() : null);

		String[] text = signText(altar);
		Theme.SignStyle style = signStyle(altar);
		for (Side side : textSides(block))
		{
			SignSide signSide = sign.getSide(side);
			for (int line = 0; line < text.length; line++)
			{
				signSide.setLine(line, text[line]);
			}
			signSide.setColor(style.dye());
			signSide.setGlowingText(style.glowing());
		}
		sign.setWaxed(true);
		sign.update();
	}

	private static void setOrRemove(PersistentDataContainer tags, NamespacedKey key, String value)
	{
		if (value != null)
		{
			tags.set(key, PersistentDataType.STRING, value);
		}
		else
		{
			tags.remove(key);
		}
	}

	/**
	 * The front text of an altar sign in its style (four legacy text lines): {@code altar.sign.<style>.<kind>} in
	 * messages.yml, or {@code altar.sign.default.<kind>} when the style has none
	 */
	public String[] signText(AltarSigns.AltarSign altar)
	{
		String key = "altar.sign." + signStyle(altar).messages() + "." + altar.kind().key();
		if (!ctx.messages().has(key))
		{
			key = "altar.sign.default." + altar.kind().key();
		}
		if (altar.kind() != AltarSigns.Kind.PRAYING)
		{
			return AltarSigns.fourLines(ctx.messages().legacyLines(key, Messages.player(signPlayerName(altar))));
		}
		// The god's gender and title, so a sign showing them is written anew when a priest changes the gender
		GodGender gender = ctx.godService().getGenderForGod(altar.god());
		return AltarSigns.fourLines(ctx.messages().legacyLines(key, Messages.god(altar.god()), Messages.component("gender", ctx.messages().genderName(gender)),
				Messages.component("title", ctx.messages().godTypeName(ctx.godService().getDivineForceForGod(altar.god()), gender))));
	}

	/** The name a curse or blessing sign shows: the player's name now if they are online */
	private String signPlayerName(AltarSigns.AltarSign altar)
	{
		Player player = altar.playerId() != null ? ctx.plugin().getServer().getPlayer(altar.playerId()) : null;
		return player != null ? player.getName() : (altar.playerName() != null ? altar.playerName() : "");
	}

	/** The style of an altar sign: its god's type's (the default theme's for no god, or a god without a type) */
	private Theme.SignStyle signStyle(AltarSigns.AltarSign altar)
	{
		GodType type = altar.god() != null && ctx.godService().godExist(altar.god()) ? ctx.godService().getDivineForceForGod(altar.god()) : null;
		return ctx.themes().of(type).sign();
	}

	/**
	 * The altar block a wall sign or wall hanging sign hangs on ({@link AltarGeometry}): a block of an altar type with a
	 * light on top, or null if it hangs on none
	 */
	public Block getAltarBlockFromSign(Block block)
	{
		BlockFace side = attachedSide(block, true);
		return side != null ? block.getRelative(side) : null;
	}

	/** True if the block is a light that marks an altar when it stands on top of the altar block ({@link AltarLights}) */
	public boolean isAltarLight(Material type)
	{
		if (this.altarLights == null)
		{
			List<Material> candles = new ArrayList<Material>(Tag.CANDLES.getValues());
			candles.addAll(Tag.CANDLE_CAKES.getValues());
			this.altarLights = AltarLights.resolve(Material::getMaterial, candles, name -> ctx.plugin().logDebug("This server has no " + name + "; it can't light altars here"));
		}
		return this.altarLights.contains(type);
	}

	/** The blocks altars to this god type are built on (config.yml's list for it, or its theme's) */
	public List<Material> getAltarBlocks(GodType godType)
	{
		return ctx.themes().altars().blocksFor(godType);
	}

	public Player getBlessedPlayerFromAltarSign(Block block, String[] lines)
	{
		return getPlayerFromAltarSign(block, lines, "blessings");
	}

	public Player getCursedPlayerFromAltar(Block block, String[] lines)
	{
		return getPlayerFromAltarSign(block, lines, "curses");
	}

	/** The online player named on line 3 of a sign whose first line is the given keyword. */
	private Player getPlayerFromAltarSign(Block block, String[] lines, String keyword)
	{
		if (!isAltarSignType(block))
		{
			return null;
		}

		String playerName = AltarSigns.namedPlayer(lines, keyword);

		// Exact name: getPlayer(String) would also match name prefixes
		return playerName != null ? ctx.plugin().getServer().getPlayerExact(playerName) : null;
	}

	/**
	 * The player who dropped this item, if it is to count as their sacrifice now. An item counts once: fire and lava set
	 * a burning item on fire again every tick, and only the first time counts.
	 */
	public UUID takeDroppedItemPlayer(Item item)
	{
		return this.droppedItems.take(item.getUniqueId(), System.currentTimeMillis());
	}

	/** What a sign written as a new praying altar says: the god's name and maybe a gender word ({@link AltarSigns}) */
	public AltarSigns.NewAltar readNewPrayingAltar(String[] lines)
	{
		return AltarSigns.newPrayingAltar(lines, genderWords());
	}

	/**
	 * The words of this language that name a god's gender, on an altar sign and in /g gender
	 * ({@code altar.gender-words.*} in messages.yml), and the English ones
	 */
	public AltarSigns.GenderWords genderWords()
	{
		return new AltarSigns.GenderWords(genderWords("altar.gender-words.male", "God"), genderWords("altar.gender-words.female", "Goddess"),
				genderWords("altar.gender-words.non-binary", "Deity"));
	}

	/** The words of this language that choose a gender on an altar sign (messages.yml), and the English one */
	private List<String> genderWords(String key, String english)
	{
		List<String> words = new ArrayList<String>();
		for (Component word : ctx.messages().lines(key))
		{
			words.add(Messages.plain(word));
		}
		words.add(english);
		return words;
	}

	public GodType getGodTypeForAltarBlockType(Material altarBlockType)
	{
		List<GodType> godTypes = ctx.themes().altars().godTypesFor(altarBlockType);
		if (godTypes.isEmpty())
		{
			ctx.plugin().logDebug("No god types available for block type " + altarBlockType.name() + "!");
			return null;
		}
		return godTypes.get(this.random.nextInt(godTypes.size()));
	}

	/** A new blessing altar ("Blessings" and a player's name): the altar, or null if the player may not build one */
	public AltarSigns.AltarSign handleNewBlessingAltar(SignChangeEvent event)
	{
		Player player = event.getPlayer();

		if (!player.isOp() && !ctx.permissions().hasPermission(player, "gods.altar.build"))
		{
			return null;
		}
		AltarSigns.AltarSign altar = newPlayerAltar(event, AltarSigns.Kind.BLESSINGS, getBlessedPlayerFromAltarSign(event.getBlock(), event.getLines()));

		ctx.godSpeech().sendInfo(player.getUniqueId(), "altar.blessing-hint", 10);

		return altar;
	}

	/** A new curse altar ("Curses" and a player's name): the altar, or null if the player may not build one */
	public AltarSigns.AltarSign handleNewCursingAltar(SignChangeEvent event)
	{
		Player player = event.getPlayer();
		if ((!player.isOp()) && (!ctx.permissions().hasPermission(player, "gods.altar.build")))
		{
			ctx.godSpeech().sendInfo(player.getUniqueId(), "altar.build-not-allowed", 10);
			return null;
		}

		AltarSigns.AltarSign altar = newPlayerAltar(event, AltarSigns.Kind.CURSES, getCursedPlayerFromAltar(event.getBlock(), event.getLines()));

		ctx.godSpeech().sendInfo(player.getUniqueId(), "altar.curse-hint", 20);

		return altar;
	}

	/** A curse or blessing altar about {@code target}, in the style of its builder's god, written on the sign */
	private AltarSigns.AltarSign newPlayerAltar(SignChangeEvent event, AltarSigns.Kind kind, Player target)
	{
		String builderGod = ctx.believerService().getGodForBeliever(event.getPlayer().getUniqueId());
		AltarSigns.AltarSign altar = new AltarSigns.AltarSign(kind, builderGod, target.getName(), target.getUniqueId(), true);
		writeSign(event, altar);
		return altar;
	}

	/** The text of the altar in its style, as the sign's new text */
	private void writeSign(SignChangeEvent event, AltarSigns.AltarSign altar)
	{
		String[] text = signText(altar);
		for (int line = 0; line < text.length; line++)
		{
			event.setLine(line, text[line]);
		}
	}

	/** A new praying altar (the god's name, maybe a gender word): the altar, or null if it can't be built */
	public AltarSigns.AltarSign handleNewPrayingAltar(SignChangeEvent event)
	{
		Player player = event.getPlayer();
		if ((!player.isOp()) && (!ctx.permissions().hasPermission(player, "gods.altar.build")))
		{
			ctx.godSpeech().sendInfo(player.getUniqueId(), "altar.build-not-allowed", 10);
			return null;
		}

		// The god's name, and maybe "God" or "Goddess" on another line
		AltarSigns.NewAltar nameLine = readNewPrayingAltar(event.getLines());
		if (nameLine.problem() == AltarSigns.Problem.INVALID_SIGN)
		{
			ctx.godSpeech().sendInfo(player.getUniqueId(), "altar.invalid-sign", 10);
			return null;
		}
		if (nameLine.problem() == AltarSigns.Problem.INVALID_GOD_NAME)
		{
			ctx.godSpeech().sendInfo(player.getUniqueId(), "altar.invalid-god-name", 20);
			return null;
		}

		String godName = nameLine.godName().trim();
		godName = ctx.godService().formatGodName(godName);

		if ((godName.length() <= 1) || (godName.contains(" ")))
		{
			ctx.godSpeech().sendInfo(player.getUniqueId(), "altar.invalid-god-name", 20);
			return null;
		}

		if (!ctx.godListService().isAllowedGod(godName))
		{
			ctx.godSpeech().sendInfo(player.getUniqueId(), "altar.god-not-allowed", 1);
			return null;
		}

		if (!ctx.godService().hasGodAccess(player.getUniqueId(), godName))
		{
			ctx.godSpeech().sendInfo(player.getUniqueId(), "altar.no-access", 1);
			return null;
		}

		String currentGodName = ctx.believerService().getGodForBeliever(player.getUniqueId());
		if (currentGodName != null && !currentGodName.equals(godName))
		{
			ctx.godSpeech().sendInfo(player.getUniqueId(), "altar.other-god", 1, Messages.god(godName));
			return null;
		}

		Block altarBlock = getAltarBlockFromSign(player.getWorld().getBlockAt(event.getBlock().getLocation()));

		if (altarBlock == null)
		{
			return null;
		}

		AltarSigns.AltarSign altar;
		
		if (ctx.prayerService().addAltar(event.getPlayer(), godName, event.getBlock().getLocation(), nameLine.gender()))
		{
			if ((ctx.settings().holyLand().enabled()) && (ctx.permissions().hasPermission(player, "gods.holyland")))
			{
				ctx.holyLandService().addAltar(player, godName, altarBlock.getLocation(), event.getBlock().getLocation());
			}
			
			ctx.questService().handleBuiltPrayingAltar(godName);

			altar = new AltarSigns.AltarSign(AltarSigns.Kind.PRAYING, godName, null, null, true);
			writeSign(event, altar);

			ctx.godSpeech().sendInfo(event.getPlayer().getUniqueId(), "altar.pray-hint", 80, Messages.god(godName));
		}
		else
		{
			event.setLine(0, "");
			event.setLine(1, "");
			event.setLine(2, "");
			event.setLine(3, "");

			return null;
		}

		return altar;
	}

	public boolean isAltarBlock(Block block)
	{
		if ((block == null) || (getGodTypeForAltarBlockType(block.getType()) == null))
		{
			return false;
		}
		if (!isAltarLight(block.getRelative(BlockFace.UP).getType()))
		{
			return false;
		}

		for (BlockFace face : AltarClicks.SIDES)
		{
			if (isAltarSignType(block.getRelative(face)))
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * What part of an altar the block is: an altar block (a block of an altar type with a light on top), the light on
	 * top of one, or neither ({@link AltarClicks}).
	 */
	public AltarClicks.Part altarPart(Block block)
	{
		if (block == null)
		{
			return AltarClicks.Part.NONE;
		}
		if (isAltarLight(block.getType()))
		{
			return isLitAltarBlock(block.getRelative(BlockFace.DOWN)) ? AltarClicks.Part.LIGHT : AltarClicks.Part.NONE;
		}
		return isLitAltarBlock(block) ? AltarClicks.Part.BLOCK : AltarClicks.Part.NONE;
	}

	/** A block of an altar type with a light on top (without looking for signs) */
	private boolean isLitAltarBlock(Block block)
	{
		return !ctx.themes().altars().godTypesFor(block.getType()).isEmpty() && isAltarLight(block.getRelative(BlockFace.UP).getType());
	}

	/**
	 * The praying sign of the altar whose block or light this is ({@link AltarClicks#chooseSign}: a tagged praying
	 * sign first), or null if it has none. Only signs hanging on the altar block count: wall signs facing away from it,
	 * and wall hanging signs whose altar block it is ({@link AltarGeometry}).
	 */
	public Block findPrayingSign(Block clicked)
	{
		AltarClicks.Part part = altarPart(clicked);
		if (part == AltarClicks.Part.NONE)
		{
			return null;
		}
		Block altarBlock = part == AltarClicks.Part.LIGHT ? clicked.getRelative(BlockFace.DOWN) : clicked;
		List<AltarClicks.Candidate<Block>> candidates = new ArrayList<>();
		for (BlockFace side : AltarClicks.SIDES)
		{
			Block sign = altarBlock.getRelative(side);
			AltarGeometry.Mount mount = mount(sign);
			if (mount == null || !(sign.getBlockData() instanceof Directional directional) || !AltarGeometry.hangsOn(mount, side, directional.getFacing())
					|| (mount == AltarGeometry.Mount.WALL_HANGING && !altarBlock.equals(getAltarBlockFromSign(sign))))
			{
				continue;
			}
			AltarSigns.AltarSign altar = readAltarSign(sign);
			if (altar != null)
			{
				candidates.add(new AltarClicks.Candidate<>(sign, altar.kind(), altar.tagged()));
			}
		}
		return AltarClicks.chooseSign(candidates);
	}

	/**
	 * True if a new altar may be built with this sign as far as land claims go: with config.yml's
	 * {@code altars.require-claim} on and GriefPrevention installed, the sign and its altar block must be in a claim
	 * where the player may build ({@link AltarClaims}). Altars that already stand are never checked.
	 */
	public boolean mayBuildAltarHere(Player player, Block sign)
	{
		GriefPreventionIntegration griefPrevention = ctx.griefPrevention();
		if (!AltarClaims.applies(ctx.settings().altars().requireClaim(), griefPrevention != null))
		{
			return true;
		}
		Block altarBlock = getAltarBlockFromSign(sign);
		AltarClaims.Claim signSpot = griefPrevention.claimAt(player, sign.getLocation());
		return altarBlock == null ? AltarClaims.mayBuild(signSpot) : AltarClaims.mayBuild(signSpot, griefPrevention.claimAt(player, altarBlock.getLocation()));
	}

	/** A wall sign or wall hanging sign on an altar block (a block with an altar type and a light on top). */
	public boolean isAltarSign(Block block)
	{
		return getAltarBlockFromSign(block) != null;
	}

	/** True if this sign already is a finished altar (tagged, or with the text altars had before tags) */
	public boolean isFormattedAltarSign(Block block)
	{
		return readAltarSign(block) != null;
	}

	public boolean isBlessingAltar(Block block, String[] lines)
	{
		if (!isAltarSign(block))
		{
			return false;
		}
		return getBlessedPlayerFromAltarSign(block, lines) != null;
	}

	public boolean isCursingAltar(Block block, String[] lines)
	{
		if (!isAltarSign(block))
		{
			return false;
		}
		return getCursedPlayerFromAltar(block, lines) != null;
	}
}
