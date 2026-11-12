package com.dogonfire.gods.command;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.api.event.BelieverDevotionChangeEvent;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.message.Voice;
import com.dogonfire.gods.model.Altar;
import com.dogonfire.gods.model.DevotionLevel;
import com.dogonfire.gods.model.God;
import com.dogonfire.gods.model.GodGender;
import com.dogonfire.gods.model.GodRank;
import com.dogonfire.gods.model.GodStats;
import com.dogonfire.gods.model.GodType;
import com.dogonfire.gods.model.Quest;
import com.dogonfire.gods.service.AltarRegistry;
import com.dogonfire.gods.service.MainAltarRules;
import com.dogonfire.gods.service.QuestService;

/**
 * /g admin &lt;action&gt; ...: tools for server admins, each through the service method the game (and the API) uses,
 * so events, effects, words and saves are the same. Each action needs its own gods.admin.&lt;action&gt;
 * ({@link AdminArgs#ACTIONS}); having any of them is enough to use the command, and gods.admin gives them all. Without an action it lists the actions the sender may use.
 */
public final class AdminCommand extends SubCommand
{
	public AdminCommand(GodsContext ctx)
	{
		super(ctx, "admin", "gods.admin");
	}

	/** Any one action's gods.admin.&lt;action&gt; is enough to use /g admin (gods.admin gives them all) */
	@Override
	public boolean permitted(Predicate<String> hasNode)
	{
		return AdminArgs.ACTIONS.stream().anyMatch(action -> hasNode.test(AdminArgs.permission(action)));
	}

	/** Like the router: ops have every node, players through Gods' permission check, the console through Bukkit */
	private boolean may(CommandSender sender, String node)
	{
		if (sender.isOp())
		{
			return true;
		}
		if (sender instanceof Player player)
		{
			return this.ctx.permissions().hasPermission(player, node);
		}
		return sender.hasPermission(node);
	}

	@Override
	public void execute(CommandSender sender, List<String> args) throws CommandException, UsageException
	{
		if (args.isEmpty())
		{
			list(sender);
			return;
		}
		String action = AdminArgs.action(args.getFirst());
		if (action == null)
		{
			throw new CommandException("command.admin.unknown", Messages.text("action", args.getFirst()));
		}
		if (!may(sender, AdminArgs.permission(action)))
		{
			throw new CommandException("command.no-permission");
		}
		List<String> rest = args.subList(1, args.size());
		switch (action)
		{
			case "info" -> info(sender, rest);
			case "setrank" -> setRank(sender, rest);
			case "devotion" -> devotion(sender, rest);
			case "prayerpower" -> prayerPower(sender, rest);
			case "mood" -> mood(sender, rest);
			case "gender" -> gender(sender, rest);
			case "godskin" -> godSkin(sender, rest);
			case "quest" -> quest(sender, rest);
			case "setaltar" -> setAltar(sender, rest);
			default -> throw new CommandException("command.admin.unknown", Messages.text("action", action));
		}
	}

	/** The actions the sender may use, with their parameters and what they do */
	private void list(CommandSender sender)
	{
		send(sender, "command.admin.header");
		for (String action : AdminArgs.ACTIONS)
		{
			if (may(sender, AdminArgs.permission(action)))
			{
				send(sender, "command.admin.line", Messages.text("action", action), parameters(action), Messages.component("description",
						this.ctx.messages().component("command.admin." + action + ".description")));
			}
		}
	}

	private TagResolver parameters(String action)
	{
		return Messages.component("parameters", this.ctx.messages().component("command.admin." + action + ".parameters"));
	}

	/** Stops with the action's usage unless there are {@code min} to {@code max} arguments */
	private void need(String action, List<String> args, int min, int max) throws CommandException
	{
		if (args.size() < min || args.size() > max)
		{
			throw new CommandException("command.admin.usage", Messages.text("action", action), parameters(action));
		}
	}

	/** The god's name as Gods writes it */
	private String god(String name) throws CommandException
	{
		if (!this.ctx.godService().godExist(name))
		{
			throw new CommandException("command.admin.no-god", Messages.god(name));
		}
		return this.ctx.godService().formatGodName(name);
	}

	/** A player with a god: online by exact name, else any known believer of any god */
	private UUID believer(String name) throws CommandException
	{
		UUID playerId = null;
		Player online = this.ctx.plugin().getServer().getPlayerExact(name);
		if (online != null)
		{
			playerId = online.getUniqueId();
		}
		else
		{
			for (String godName : godNames())
			{
				playerId = knownBeliever(godName, name);
				if (playerId != null)
				{
					break;
				}
			}
		}
		if (playerId == null)
		{
			throw new CommandException("command.admin.no-player", Messages.player(name));
		}
		if (this.ctx.believerService().getGodForBeliever(playerId) == null)
		{
			throw new CommandException("command.admin.not-believer", Messages.player(name));
		}
		return playerId;
	}

	private String playerName(UUID playerId, String typed)
	{
		String name = nameOf(playerId);
		return name != null ? name : typed;
	}

	// ---------------------------------------------------------------------
	// Actions

	private void info(CommandSender sender, List<String> args) throws CommandException
	{
		need("info", args, 1, 1);
		String godName = god(args.getFirst());
		God god = this.ctx.storage().gods().find(godName);
		GodType type = this.ctx.godService().getDivineForceForGod(godName);
		GodGender gender = this.ctx.godService().getGenderForGod(godName);
		GodRank rank = this.ctx.rankService().getRank(godName);
		GodStats stats = this.ctx.rankService().getStats(godName);

		Map<DevotionLevel, Integer> devotion = new EnumMap<>(DevotionLevel.class);
		for (DevotionLevel level : DevotionLevel.values())
		{
			devotion.put(level, 0);
		}
		Set<UUID> believers = this.ctx.believerService().getBelieversForGod(godName);
		for (UUID believerId : believers)
		{
			devotion.merge(this.ctx.devotionService().getLevel(believerId), 1, Integer::sum);
		}
		List<String> devotionCounts = new ArrayList<>();
		for (Map.Entry<DevotionLevel, Integer> entry : devotion.entrySet())
		{
			devotionCounts.add(entry.getKey().key() + " " + entry.getValue());
		}

		List<String> priests = new ArrayList<>();
		for (UUID priestId : god != null ? god.priests() : List.<UUID>of())
		{
			priests.add(playerName(priestId, priestId.toString()));
		}

		Altar mainAltar = this.ctx.altarRegistry().mainAltar(godName);
		Component none = this.ctx.messages().component("command.admin.info.none");
		Component altarText = mainAltar != null ? Component.text(mainAltar.world() + " " + mainAltar.block().x() + ", " + mainAltar.block().y() + ", " + mainAltar.block().z())
				: none;
		Quest quest = this.ctx.storage().quests().find(godName);
		Component questText = quest != null && quest.type() != null ? Component.text(quest.type().name().toLowerCase(Locale.ROOT)) : none;
		Instant descended = god != null ? god.descended() : null;

		send(sender, "command.admin.info.lines", Messages.god(godName),
				Messages.component("type", type != null ? this.ctx.messages().godTypeName(type, gender) : none),
				Messages.text("rank", this.ctx.rankService().getRankName(godName)), Messages.text("rank-key", rank.key()),
				Messages.text("rank-since", god != null && god.rankSince() != null ? god.rankSince().toString() : "-"),
				Messages.text("descended", descended != null ? descended.toString() : "-"),
				Messages.text("npc", this.ctx.godNpcs().isShown(godName) ? "yes" : "no"),
				Messages.text("power", String.format(Locale.ROOT, "%.1f", this.ctx.godService().getGodPower(godName))),
				Messages.text("mood", String.format(Locale.ROOT, "%.1f", this.ctx.moodService().getExactMoodForGod(godName))),
				Messages.component("mood-name", this.ctx.messages().moodName(this.ctx.moodService().getMoodForGod(godName))),
				Messages.amount(believers.size()), Messages.text("devotion", String.join(", ", devotionCounts)),
				Messages.text("priests", priests.isEmpty() ? "-" : String.join(", ", priests)),
				Messages.text("prayers", stats.prayers()), Messages.text("sacrifices", stats.sacrifices()), Messages.text("quests", stats.quests()),
				Messages.text("converts", stats.converts()), Messages.component("main-altar", altarText), Messages.component("quest", questText));
	}

	private void setRank(CommandSender sender, List<String> args) throws CommandException
	{
		need("setrank", args, 2, ANY);
		String godName = god(args.getFirst());
		String typed = String.join(" ", args.subList(1, args.size()));
		GodRank rank = AdminArgs.named(typed, GodRank.values(), r -> Messages.plain(this.ctx.messages().parse(this.ctx.rankService().rankName(godName, r))));
		if (rank == null)
		{
			throw new CommandException("command.admin.setrank.unknown", Messages.text("rank", typed));
		}
		if (rank == this.ctx.rankService().getRank(godName))
		{
			throw new CommandException("command.admin.setrank.already", Messages.god(godName), Messages.text("rank", this.ctx.rankService().rankName(godName, rank)));
		}
		if (!this.ctx.rankService().setRank(godName, rank))
		{
			throw new CommandException("command.cancelled");
		}
		this.ctx.plugin().log(sender.getName() + " set the rank of " + godName + " to " + rank.key());
		send(sender, "command.admin.setrank.done", Messages.god(godName), Messages.text("rank", this.ctx.rankService().rankName(godName, rank)),
				Messages.text("rank-key", rank.key()));
	}

	private void devotion(CommandSender sender, List<String> args) throws CommandException
	{
		need("devotion", args, 2, ANY);
		if (!this.ctx.devotionService().enabled())
		{
			throw new CommandException("command.disabled.devotion");
		}
		UUID playerId = believer(args.getFirst());
		String name = playerName(playerId, args.getFirst());
		String godName = this.ctx.believerService().getGodForBeliever(playerId);
		String typed = String.join(" ", args.subList(1, args.size()));
		AdminArgs.Amount amount = AdminArgs.amount(typed);
		boolean done;
		if (amount != null)
		{
			int current = this.ctx.devotionService().getPoints(playerId);
			long change = Math.max(0L, amount.applyTo(current)) - current;
			done = this.ctx.devotionService().addPoints(playerId, (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, change)),
					BelieverDevotionChangeEvent.Cause.API);
		}
		else
		{
			DevotionLevel level = AdminArgs.named(typed, DevotionLevel.values(), l -> Messages.plain(this.ctx.messages().parse(this.ctx.devotionService().levelName(godName, l))));
			if (level == null)
			{
				throw new CommandException("command.admin.devotion.unknown", Messages.text("value", typed));
			}
			done = this.ctx.devotionService().setLevel(playerId, level);
		}
		if (!done)
		{
			throw new CommandException("command.cancelled");
		}
		this.ctx.plugin().log(sender.getName() + " changed the devotion of " + name + " (" + typed + ")");
		send(sender, "command.admin.devotion.done", Messages.player(name), Messages.text("level", this.ctx.devotionService().getLevelName(playerId)),
				Messages.amount(this.ctx.devotionService().getPoints(playerId)));
	}

	private void prayerPower(CommandSender sender, List<String> args) throws CommandException
	{
		need("prayerpower", args, 2, 2);
		UUID playerId = believer(args.getFirst());
		String name = playerName(playerId, args.getFirst());
		AdminArgs.Amount amount = AdminArgs.amount(args.get(1));
		if (amount == null)
		{
			throw new CommandException("command.admin.not-a-number", Messages.text("value", args.get(1)));
		}
		int current = this.ctx.believerService().getPrayerPower(playerId);
		long target = Math.max(0L, Math.min(Integer.MAX_VALUE, amount.applyTo(current)));
		this.ctx.believerService().increasePrayerPower(playerId, (int) (target - current));
		this.ctx.plugin().log(sender.getName() + " set the prayer power of " + name + " to " + target);
		send(sender, "command.admin.prayerpower.done", Messages.player(name), Messages.amount(this.ctx.believerService().getPrayerPower(playerId)));
	}

	private void mood(CommandSender sender, List<String> args) throws CommandException
	{
		need("mood", args, 2, 2);
		String godName = god(args.getFirst());
		AdminArgs.Decimal value = AdminArgs.decimal(args.get(1));
		if (value == null)
		{
			throw new CommandException("command.admin.not-a-number", Messages.text("value", args.get(1)));
		}
		float current = this.ctx.moodService().getExactMoodForGod(godName);
		// Kept within -100 to 100 by the mood service, as every change of mood
		float change = (float) (value.applyTo(current) - current);
		if (!this.ctx.moodService().addMoodForGod(godName, change))
		{
			throw new CommandException("command.cancelled");
		}
		this.ctx.plugin().log(sender.getName() + " set the mood of " + godName + " to " + this.ctx.moodService().getExactMoodForGod(godName));
		send(sender, "command.admin.mood.done", Messages.god(godName),
				Messages.text("mood", String.format(Locale.ROOT, "%.1f", this.ctx.moodService().getExactMoodForGod(godName))),
				Messages.component("mood-name", this.ctx.messages().moodName(this.ctx.moodService().getMoodForGod(godName))));
	}

	private void gender(CommandSender sender, List<String> args) throws CommandException
	{
		need("gender", args, 2, 2);
		String godName = god(args.getFirst());
		GodGender gender = this.ctx.altarService().genderWords().gender(args.get(1));
		if (gender == null)
		{
			throw new CommandException("command.gender.unknown", Messages.text("word", args.get(1)));
		}
		if (gender == this.ctx.godService().getGenderForGod(godName))
		{
			throw new CommandException("command.admin.gender.already", Messages.god(godName), this.ctx.godService().genderTag(godName));
		}
		// No cooldown for admins, but the change counts for the priests' cooldown like any other
		if (!this.ctx.godService().changeGender(godName, gender))
		{
			throw new CommandException("command.cancelled");
		}
		this.ctx.plugin().log(sender.getName() + " made " + godName + " " + gender.name());
		send(sender, "command.admin.gender.done", Messages.god(godName), this.ctx.godService().genderTag(godName));
	}

	private void godSkin(CommandSender sender, List<String> args) throws CommandException
	{
		need("godskin", args, 2, 2);
		if (!this.ctx.settings().npc().enabled())
		{
			throw new CommandException("command.disabled.npc");
		}
		String godName = god(args.getFirst());
		String name = args.get(1);
		if (name.equalsIgnoreCase(GodSkinCommand.RESET))
		{
			this.ctx.godService().setNpcSkin(godName, null);
			send(sender, "command.godskin.reset", Messages.god(godName));
			return;
		}
		if (!name.matches("[A-Za-z0-9_]{1,16}"))
		{
			throw new CommandException("command.godskin.not-found", Messages.player(name));
		}
		send(sender, "command.godskin.looking", Messages.player(name));
		UUID adminId = sender instanceof Player player ? player.getUniqueId() : null;
		this.ctx.godNpcs().skins().resolve(name, skin -> {
			// The answer may come later: the admin may have logged off, the god may be gone
			CommandSender to = adminId != null ? this.ctx.plugin().getServer().getPlayer(adminId) : sender;
			if (skin == null)
			{
				if (to != null)
				{
					send(to, "command.godskin.not-found", Messages.player(name));
				}
				return;
			}
			if (!this.ctx.godService().godExist(godName))
			{
				return;
			}
			this.ctx.godService().setNpcSkin(godName, skin);
			this.ctx.plugin().log(sender.getName() + " gave " + godName + " the skin of " + skin.name());
			if (to != null)
			{
				send(to, "command.godskin.done", Messages.god(godName), Messages.player(skin.name() != null ? skin.name() : name));
			}
		});
	}

	private void quest(CommandSender sender, List<String> args) throws CommandException
	{
		need("quest", args, 2, 2);
		if (!this.ctx.settings().quests().enabled())
		{
			throw new CommandException("command.disabled.quests");
		}
		String godName = god(args.getFirst());
		String what = args.get(1).toLowerCase(Locale.ROOT);
		switch (what)
		{
			case "start" ->
			{
				QuestService.QuestStart start = this.ctx.questService().startQuest(godName);
				switch (start)
				{
					case STARTED ->
					{
					}
					case CANCELLED -> throw new CommandException("command.cancelled");
					case NOT_POSSIBLE -> throw new CommandException("command.admin.quest.not-started", Messages.god(godName));
				}
			}
			case "complete" ->
			{
				if (!this.ctx.questService().completeQuest(godName))
				{
					throw new CommandException("command.admin.quest.no-quest", Messages.god(godName));
				}
			}
			case "fail" ->
			{
				if (!this.ctx.questService().failQuest(godName))
				{
					throw new CommandException("command.admin.quest.no-quest", Messages.god(godName));
				}
			}
			default -> throw new CommandException("command.admin.usage", Messages.text("action", "quest"), parameters("quest"));
		}
		this.ctx.plugin().log(sender.getName() + " made the quest of " + godName + ": " + what);
		send(sender, "command.admin.quest." + what, Messages.god(godName));
	}

	private void setAltar(CommandSender sender, List<String> args) throws CommandException
	{
		need("setaltar", args, 1, 1);
		if (!(sender instanceof Player player))
		{
			throw new CommandException("command.player-only");
		}
		String godName = god(args.getFirst());
		Block looked = player.getTargetBlockExact(MainAltarRules.REACH_BLOCKS);
		if (looked == null)
		{
			throw new CommandException("command.admin.setaltar.no-altar");
		}
		AltarRegistry registry = this.ctx.altarRegistry();
		MainAltarRules.Target target = MainAltarRules.choose(MainAltarRules.candidates(AltarRegistry.pos(looked), registry::atSign, registry::onBlock), godName);
		switch (target.verdict())
		{
			case NO_ALTAR -> throw new CommandException("command.admin.setaltar.no-altar");
			case NOT_PRAYING -> throw new CommandException("command.setaltar.not-praying");
			case OTHER_GOD -> throw new CommandException("command.setaltar.other-god", Messages.god(godName));
			case OK ->
			{
			}
		}
		Altar altar = target.altar();
		if (registry.isMainAltar(altar))
		{
			throw new CommandException("command.setaltar.already", Messages.god(godName));
		}
		registry.setMainAltar(altar);
		TagResolver[] where = { Messages.text("world", altar.world()), Messages.text("x", altar.block().x()), Messages.text("y", altar.block().y()),
				Messages.text("z", altar.block().z()) };
		this.ctx.plugin().log(sender.getName() + " made the altar at " + altar.world() + " " + altar.block().x() + ", " + altar.block().y() + ", " + altar.block().z()
				+ " the main altar of " + godName);
		send(sender, "command.setaltar.done", Messages.god(godName), where[0], where[1], where[2], where[3]);
		this.ctx.godSpeech().sayToBelievers(godName, Voice.BELIEVERS_SET_HOME, 2, Messages.player(player.getName()), where[0], where[1], where[2],
				where[3]);
	}

	// ---------------------------------------------------------------------
	// Tab completion

	@Override
	public List<String> tabComplete(CommandSender sender, List<String> args)
	{
		if (args.size() == 1)
		{
			return AdminArgs.ACTIONS.stream().filter(action -> may(sender, AdminArgs.permission(action))).toList();
		}
		String action = AdminArgs.action(args.getFirst());
		if (action == null || !may(sender, AdminArgs.permission(action)))
		{
			return List.of();
		}
		int position = args.size() - 1;
		return switch (action)
		{
			case "info", "setaltar" -> position == 1 ? godNames() : List.of();
			case "setrank" -> position == 1 ? godNames() : position == 2 && this.ctx.godService().godExist(args.get(1))
					? AdminArgs.completions(GodRank.values(), r -> Messages.plain(this.ctx.messages().parse(this.ctx.rankService().rankName(args.get(1), r))))
					: List.of();
			case "devotion" -> position == 1 ? allBelieverNames() : position == 2 ? devotionCompletions(args.get(1)) : List.of();
			case "prayerpower" -> position == 1 ? allBelieverNames() : position == 2 ? List.of("+10", "-10", "0") : List.of();
			case "mood" -> position == 1 ? godNames() : position == 2 ? List.of("+10", "-10", "0", "=-50", "50") : List.of();
			case "gender" -> position == 1 ? godNames() : position == 2 ? List.of("god", "goddess", "deity") : List.of();
			case "godskin" -> position == 1 ? godNames() : position == 2 ? withReset(onlinePlayerNames(sender)) : List.of();
			case "quest" -> position == 1 ? godNames() : position == 2 ? AdminArgs.QUEST_ACTIONS : List.of();
			default -> List.of();
		};
	}

	private List<String> allBelieverNames()
	{
		List<String> names = new ArrayList<>();
		for (String godName : godNames())
		{
			names.addAll(believerNames(godName));
		}
		return names;
	}

	private List<String> devotionCompletions(String playerName)
	{
		Player online = this.ctx.plugin().getServer().getPlayerExact(playerName);
		String godName = online != null ? this.ctx.believerService().getGodForBeliever(online.getUniqueId()) : null;
		List<String> completions = new ArrayList<>(AdminArgs.completions(DevotionLevel.values(),
				l -> godName != null ? Messages.plain(this.ctx.messages().parse(this.ctx.devotionService().levelName(godName, l))) : null));
		completions.add("+100");
		completions.add("-100");
		return completions;
	}

	private static List<String> withReset(List<String> names)
	{
		List<String> all = new ArrayList<>(names);
		all.add(GodSkinCommand.RESET);
		return all;
	}
}
