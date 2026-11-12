package com.dogonfire.gods.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.service.Permissions;

/** The router's checks, help listing and tab completion, with fake commands and senders (no server) */
class CommandRouterTest
{
	private static final String	MESSAGES	= """
			command:
			  usage: 'Usage: /gods <usage>'
			  no-permission: no permission
			  player-only: players only
			  priest-only: priests only
			  no-god: no god
			  invalid: invalid
			  disabled:
			    feature: feature off
			  pray:
			    parameters: '[god]'
			    description: prays
			  ally:
			    parameters: '\\<god>'
			    description: allies
			    not-priest: only priests ally
			  kick:
			    parameters: '\\<player>'
			    description: kicks
			  reload:
			    parameters: ''
			    description: reloads
			  feature:
			    parameters: ''
			    description: a feature
			  fail:
			    parameters: ''
			    description: fails
			    oops: 'oops <thing>'
			""";

	@TempDir
	Path						lang;

	private final Set<UUID>		priests		= new HashSet<>();
	private final Map<UUID, String>	gods	= new HashMap<>();
	private final List<CommandSender>	overviews	= new ArrayList<>();
	private CommandRouter		router;
	private Fake				pray;
	private Fake				ally;
	private Fake				kick;
	private Fake				reload;
	private Fake				feature;

	/** A command that records what it was run with */
	private static class Fake extends SubCommand
	{
		final List<List<String>>	runs		= new ArrayList<>();
		boolean						enabled		= true;
		List<String>				completions	= List.of();

		Fake(String name, String permission)
		{
			super(null, name, permission);
		}

		@Override
		public String disabledMessage()
		{
			return this.enabled ? null : "command.disabled.feature";
		}

		@Override
		public void execute(CommandSender sender, List<String> args) throws CommandException, UsageException
		{
			this.runs.add(args);
		}

		@Override
		public List<String> tabComplete(CommandSender sender, List<String> args)
		{
			return this.completions;
		}
	}

	/** A sender that keeps the messages it gets */
	private static final class Sender implements InvocationHandler
	{
		final List<String>	received	= new ArrayList<>();
		final Set<String>	permissions	= new HashSet<>();
		final UUID			id			= UUID.randomUUID();
		boolean				op;

		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
		{
			return switch (method.getName())
			{
				case "sendMessage" ->
				{
					if (args[0] instanceof String text)
					{
						this.received.add(text);
					}
					yield null;
				}
				case "hasPermission" -> args[0] instanceof String node && this.permissions.contains(node);
				case "isOp" -> this.op;
				case "getUniqueId" -> this.id;
				case "getName" -> "Bob";
				case "equals" -> proxy == args[0];
				case "hashCode" -> System.identityHashCode(proxy);
				case "toString" -> "Sender";
				default -> method.getReturnType() == boolean.class ? false : null;
			};
		}
	}

	private Sender		bob;
	private Player		player;
	private Sender		consoleHandler;
	private CommandSender	console;

	@BeforeEach
	void setUp()
	{
		Messages messages = new Messages(this.lang, path -> path.equals("lang/english/messages.yml") ? (InputStream) new ByteArrayInputStream(MESSAGES.getBytes(StandardCharsets.UTF_8)) : null, Logger.getLogger("test"));
		messages.load("english", "Test");
		CommandChecks checks = new CommandChecks()
		{
			@Override
			public String godOf(UUID playerId)
			{
				return CommandRouterTest.this.gods.get(playerId);
			}

			@Override
			public boolean isPriest(UUID playerId)
			{
				return CommandRouterTest.this.priests.contains(playerId);
			}
		};
		this.router = new CommandRouter(messages, new Permissions(), checks, this.overviews::add);

		this.pray = new Fake("pray", "gods.pray");
		this.pray.believerOnly();
		this.pray.args(0, 1);
		this.pray.aliases("p");
		this.ally = new Fake("ally", "gods.priest.alliance");
		this.ally.priestOnly("command.ally.not-priest");
		this.ally.args(1, 1);
		this.kick = new Fake("kick", "gods.priest.kick");
		this.kick.priestOnly();
		this.reload = new Fake("reload", "gods.reload");
		this.feature = new Fake("feature", "gods.feature");
		this.feature.playerOnly();
		for (Fake fake : List.of(this.reload, this.pray, this.kick, this.ally, this.feature))
		{
			this.router.register(fake);
		}

		this.bob = new Sender();
		this.player = (Player) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Player.class }, this.bob);
		this.consoleHandler = new Sender();
		this.consoleHandler.op = true;
		this.console = (CommandSender) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { CommandSender.class }, this.consoleHandler);
	}

	private void run(CommandSender sender, String... args)
	{
		this.router.onCommand(sender, null, "g", args);
	}

	private List<String> complete(CommandSender sender, String... args)
	{
		return this.router.onTabComplete(sender, null, "g", args);
	}

	@Test
	void runsTheCommandNamedByTheFirstArgumentIgnoringCase()
	{
		this.bob.permissions.add("gods.pray");
		this.gods.put(this.bob.id, "Thor");
		run(this.player, "PRAY", "Odin");
		run(this.player, "p");
		assertEquals(List.of(List.of("Odin"), List.of()), this.pray.runs);
		assertTrue(this.bob.received.isEmpty());
	}

	@Test
	void noArgumentsShowsTheOverview()
	{
		run(this.player);
		assertEquals(List.of(this.player), this.overviews);
	}

	@Test
	void anUnknownCommandIsInvalid()
	{
		run(this.player, "dance");
		assertEquals(List.of("invalid"), this.bob.received);
	}

	@Test
	void permissionIsCheckedAndOpsHaveEveryPermission()
	{
		run(this.player, "reload");
		assertEquals(List.of("no permission"), this.bob.received);
		assertTrue(this.reload.runs.isEmpty());

		this.bob.op = true;
		run(this.player, "reload");
		assertEquals(1, this.reload.runs.size());

		// The console is op, as on a server
		run(this.console, "reload");
		assertEquals(2, this.reload.runs.size());
	}

	@Test
	void theConsoleCantUsePlayerCommands()
	{
		run(this.console, "feature");
		assertEquals(List.of("players only"), this.consoleHandler.received);
		assertTrue(this.feature.runs.isEmpty());
	}

	@Test
	void priestCommandsNeedAPriestWithTheirOwnOrTheCommonMessage()
	{
		this.bob.permissions.addAll(List.of("gods.priest.alliance", "gods.priest.kick"));
		this.gods.put(this.bob.id, "Thor");
		run(this.player, "ally", "Odin");
		run(this.player, "kick", "Alice");
		assertEquals(List.of("only priests ally", "priests only"), this.bob.received);

		this.priests.add(this.bob.id);
		run(this.player, "ally", "Odin");
		run(this.player, "kick", "Alice");
		assertEquals(List.of(List.of("Odin")), this.ally.runs);
		assertEquals(List.of(List.of("Alice")), this.kick.runs);
	}

	@Test
	void believerCommandsNeedAGod()
	{
		this.bob.permissions.add("gods.pray");
		run(this.player, "pray");
		assertEquals(List.of("no god"), this.bob.received);
		assertTrue(this.pray.runs.isEmpty());
	}

	@Test
	void theWrongNumberOfArgumentsShowsTheUsage()
	{
		this.bob.permissions.add("gods.priest.alliance");
		this.priests.add(this.bob.id);
		this.gods.put(this.bob.id, "Thor");
		run(this.player, "ally");
		run(this.player, "ally", "Odin", "Loki");
		assertEquals(List.of("Usage: /gods ally <god>", "Usage: /gods ally <god>"), this.bob.received);
		assertTrue(this.ally.runs.isEmpty());
	}

	@Test
	void aDisabledCommandSaysSoAndIsNotOffered()
	{
		this.feature.enabled = false;
		this.bob.op = true;
		run(this.player, "feature");
		assertEquals(List.of("feature off"), this.bob.received);
		assertTrue(this.feature.runs.isEmpty());
		assertEquals(List.of(), complete(this.player, "fea"));
	}

	@Test
	void exceptionsBecomeMessages()
	{
		SubCommand fail = new SubCommand(null, "fail", "gods.fail")
		{
			@Override
			public void execute(CommandSender sender, List<String> args) throws CommandException, UsageException
			{
				if (args.isEmpty())
				{
					throw new UsageException();
				}
				throw new CommandException("command.fail.oops", Messages.text("thing", args.getFirst()));
			}
		};
		this.router.register(fail);
		run(this.console, "fail");
		run(this.console, "fail", "<red>x");
		assertEquals(List.of("Usage: /gods fail", "oops <red>x"), this.consoleHandler.received);
	}

	@Test
	void helpListsWhatTheSenderCanUseByName()
	{
		this.bob.permissions.addAll(List.of("gods.pray", "gods.priest.alliance", "gods.feature"));
		assertEquals(List.of("feature", "pray"), names(this.router.commandsFor(this.player)));
		this.priests.add(this.bob.id);
		assertEquals(List.of("ally", "feature", "pray"), names(this.router.commandsFor(this.player)));
		assertEquals(List.of("reload"), names(this.router.commandsFor(this.console)), "the others are for players");
	}

	@Test
	void tabCompletesCommandsTheSenderCanUseThenTheirArguments()
	{
		this.bob.permissions.addAll(List.of("gods.pray", "gods.priest.alliance"));
		assertEquals(List.of("p", "pray"), complete(this.player, "p"));
		assertEquals(List.of("p", "pray"), complete(this.player, ""));

		this.pray.completions = List.of("Thor", "Odin", "Tyr");
		assertEquals(List.of("Thor", "Tyr"), complete(this.player, "pray", "t"));
		this.ally.completions = List.of("Thor");
		assertEquals(List.of(), complete(this.player, "ally", ""), "not a priest");
		assertEquals(List.of(), complete(this.player, "dance", ""));
	}

	@Test
	void aNameCanOnlyBeRegisteredOnce()
	{
		assertThrows(IllegalArgumentException.class, () -> this.router.register(new Fake("pray", "gods.pray")));
	}

	private static List<String> names(List<SubCommand> commands)
	{
		return commands.stream().map(SubCommand::name).toList();
	}
	/** A command can decide itself which nodes let a sender use it (like /g admin: any action's node) */
	@Test
	void aCommandMayAcceptAnyOfSeveralPermissions()
	{
		Fake tools = new Fake("tools", "gods.tools")
		{
			@Override
			public boolean permitted(java.util.function.Predicate<String> hasNode)
			{
				return hasNode.test("gods.tools.a") || hasNode.test("gods.tools.b");
			}
		};
		this.router.register(tools);

		run(this.player, "tools");
		assertEquals(List.of("no permission"), this.bob.received);
		assertTrue(tools.runs.isEmpty());
		assertTrue(complete(this.player, "to").isEmpty());

		this.bob.permissions.add("gods.tools.b");
		run(this.player, "tools");
		assertEquals(1, tools.runs.size());
		assertEquals(List.of("tools"), complete(this.player, "to"));
	}

}
