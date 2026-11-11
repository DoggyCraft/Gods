package com.dogonfire.gods.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
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
import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.model.GodType;

/**
 * Altars: a block of an altar type (its god type's theme, or config.yml) with a light on top ({@link AltarLights}) and
 * a wall sign on it. Recognizes altar blocks and signs, turns newly written signs into praying, cursing and blessing
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

	/** Any wooden wall sign (oak, spruce, ..., bamboo, crimson, warped) can be an altar sign. */
	public static boolean isWallSign(Block block)
	{
		return block != null && Tag.WALL_SIGNS.isTagged(block.getType());
	}

	/** The sign item that a wall sign block drops, e.g. SPRUCE_SIGN for SPRUCE_WALL_SIGN. */
	public static ItemStack getSignItem(Material wallSignType)
	{
		Material item = Material.getMaterial(wallSignType.name().replace("_WALL_SIGN", "_SIGN"));

		return new ItemStack((item != null && item.isItem()) ? item : Material.OAK_SIGN, 1);
	}

	/**
	 * Tags, styles and waxes the sign of a new altar one tick later (after the text from the sign editor has been
	 * applied), so it is known by its tags and players can't open the sign editor on it.
	 */
	public void finishAltarSignLater(Block signBlock, AltarSigns.AltarSign altar, UUID builder)
	{
		ctx.plugin().getServer().getScheduler().runTask(ctx.plugin(), () -> {
			if (!isWallSign(signBlock))
			{
				// Gone within the tick
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

	/** The block a wall sign hangs on (whatever it is), or null if the block is no wall sign */
	public static Block blockBehindSign(Block sign)
	{
		if (!isWallSign(sign) || !(sign.getBlockData() instanceof Directional directional))
		{
			return null;
		}
		return sign.getRelative(directional.getFacing().getOppositeFace());
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
		SignSide front = sign.getSide(Side.FRONT);
		Theme.SignStyle style = signStyle(altar);
		// The text without its colors: the server may write the same colors with other codes
		String[] lines = front.getLines();
		String[] text = signText(altar);
		for (int line = 0; line < text.length; line++)
		{
			if (!AltarSigns.plain(line < lines.length ? lines[line] : "").equals(AltarSigns.plain(text[line])))
			{
				return false;
			}
		}
		return front.getColor() == style.dye() && front.isGlowingText() == style.glowing();
	}

	/**
	 * Tags the sign as this altar (its kind, god and player) and writes its front in the god type's style: the text of
	 * {@code altar.sign.<style>.<kind>} in messages.yml, the theme's dye color and glowing text. Waxes it, so players
	 * can't edit it.
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

		SignSide front = sign.getSide(Side.FRONT);
		String[] text = signText(altar);
		for (int line = 0; line < text.length; line++)
		{
			front.setLine(line, text[line]);
		}
		Theme.SignStyle style = signStyle(altar);
		front.setColor(style.dye());
		front.setGlowingText(style.glowing());
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

	public Block getAltarBlockFromSign(Block block)
	{
		if (!isWallSign(block))
		{
			return null;
		}
		BlockData bd = block.getBlockData();
		if (!(bd instanceof Directional)) {
			return null;
		}
		Directional directional = (Directional) bd;

		Block altarBlock = block.getRelative(directional.getFacing().getOppositeFace());

		ctx.plugin().logDebug("getAltarBlockFromSign(): AltarBlock block is " + altarBlock.getType().name());
		if (getGodTypeForAltarBlockType(altarBlock.getType()) == null)
		{
			return null;
		}
		if (!isAltarLight(altarBlock.getRelative(BlockFace.UP).getType()))
		{
			return null;
		}
		return altarBlock;
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
		if (!isWallSign(block))
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
				ctx.holyLandService().addAltar(player, godName, altarBlock.getLocation());
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

		for (BlockFace face : new BlockFace[] { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST })
		{
			if (isWallSign(block.getRelative(face)))
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
	 * sign first), or null if it has none. Only wall signs hanging on the altar block count.
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
			if (!isWallSign(sign) || !(sign.getBlockData() instanceof Directional directional) || !AltarClicks.hangsOn(side, directional.getFacing()))
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

	/** A wall sign on an altar block (a block with an altar type and a light on top). */
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
