<h1 align="center">Gods</h1>
<div align="center">

[![Build](https://github.com/DoggyCraft/Gods/actions/workflows/build.yml/badge.svg)](https://github.com/DoggyCraft/Gods/actions/workflows/build.yml)
[![Bukkit](https://img.shields.io/curseforge/dt/39139?label=Bukkit)](https://dev.bukkit.org/projects/gods)
[![bStats Servers](https://img.shields.io/bstats/servers/35252?label=Servers)](https://bstats.org/plugin/bukkit/Gods/35252)
[![bStats Players](https://img.shields.io/bstats/players/35252?label=Players)](https://bstats.org/plugin/bukkit/Gods/35252)
</div>

![Gods](img/Gods.jpg)

The plugin for adding Gods to minecraft!

A God is an entity that exist because a player has chosen to believe in it.

A player shows his faith in a god by building an altar for his god and using the altar to pray. 
In this way, many gods may exist at a time, all with names created by the players.

The main purpose for believing in these Gods is to gain from the help the Gods provide (see below) and to provide some basic faction play based on ideology and culture (I.e. "My god is better than yours because apples are holy!").

Another purpose is simply to get people inspired to build awesome churches and use them for their Gods.

With Holy Lands, players can easily protect their lands from grief by simply building alters to their Gods.


Installation
---------
*	Run a [Paper](https://papermc.io/) or Spigot server on Minecraft 1.21.3 or newer (including 26.x), with Java 21 or newer.
*	Download `Gods-<version>.jar` from the [releases](https://github.com/DoggyCraft/Gods/releases) and drop it into the `plugins` folder.
*	Optional: [WorldGuard](https://enginehub.org/worldguard), [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/), [GriefPrevention](https://www.spigotmc.org/resources/griefprevention.1884/) (new altars only in claims, see below), [Citizens](https://www.spigotmc.org/resources/citizens.13811/) or [FancyNpcs](https://modrinth.com/plugin/fancynpcs) (the god at its altar on servers older than 1.21.9, see below).
*	Start the server. Commands and permissions are listed in [plugin.yml](plugin/src/main/resources/plugin.yml).

Building an altar
---------
1. Place an altar block. The block decides the type of god: snow, packed or blue ice for a god of frost, prismarine or sea lanterns for the sea, copper for thunder, amethyst or bookshelves for wisdom, moss or azalea for nature, and so on. `/g help blocks` lists them all.
2. Put a light on top: a torch (any kind), a lantern or a candle.
3. Place a wall sign on the side of the block and write the name of your god on it. To choose its gender, write `God`, `Goddess` or `Deity` (non-binary) on another line; otherwise the god chooses and tells you.
4. Right-click the sign to pray, or the altar block or its light. Sneak to use the block as usual (placing blocks against it); items that do something to the block (flint and steel on a candle, an axe, honeycomb, ...) and blocks with a use of their own (note blocks, jukeboxes, chiseled bookshelves, candle cakes) keep it.

Gods writes the sign in the style of the god's type and remembers it, so the text can't be changed. Signs with `Curses` or `Blessings` on the first line and a player's name on the third let priests curse or bless that player.

With [GriefPrevention](https://www.spigotmc.org/resources/griefprevention.1884/) installed, new altars (praying, curse and blessing signs) can only be built inside a claim where you have build trust (your own, or one you are trusted in); elsewhere the sign is refused. Altars that already stand keep working wherever they are. Set `altars.require-claim: false` in config.yml to allow altars anywhere. Without GriefPrevention the setting does nothing.

A god's priests can change its gender later with `/g gender <god|goddess|deity>`, once every 24 hours (`priests.gender-change-cooldown-hours` in config.yml); the god tells its believers. A Deity, a non-binary god, has no voice of its own: each thing it says comes from its type's male or female voice, picked at random. The gender words on signs and for `/g gender` are `altar.gender-words` in `lang/<language>/messages.yml`; the English ones always work.

Above every praying altar the god's name and title float in the style of its type. A priest can make one altar the god's main altar: look at its sign, block or light (at most 5 blocks away) and use `/g setaltar` (permission `gods.priest.setaltar`). The main altar gets a line of its own above it, and `/g info` shows its believers where it is. If it is destroyed, the god has no main altar until a priest chooses another.

In config.yml, `altars.floating-text` is `all` (above every praying altar, the default), `main` (only above main altars) or `off`, and `altars.floating-text-view-distance-blocks` (32) sets how far away players see it. The texts are `altar.floating.*` in `lang/<language>/messages.yml`. They are never saved with the world: Gods puts them back when the altar's chunk loads.

Gods keeps track of its altars in `plugins/Gods/altars.yml`. Altars that already stand are found as their chunks load (altars from Gods 0.9.x the first time someone uses them), so nothing needs to be done after an update.

The god at its altar
---------
Once a religion has grown, its god descends to its main altar and stands there as a figure in front of the altar sign, turning to look at players who come close. Right-clicking the figure prays to the god, just like praying at the altar. The god descends when it has a main altar, a priest for at least 24 hours (`npc.descend.requires-priest`, `npc.descend.hours-after-priest`), at least 3 believers (`npc.descend.min-believers`) and, if you set one, enough power (`npc.descend.min-power`); lightning strikes (only a flash, it does no harm), its effects rise and it tells its believers. It ascends again when it has no priest any more, or when its believers stay fewer than `min-believers` for 24 hours (`npc.descend.ascend-grace-hours`), and descends again when it qualifies once more.

Who makes the figure is `npc.provider` in config.yml:

*	`auto` (the default): [Citizens](https://www.spigotmc.org/resources/citizens.13811/) if it is installed, else [FancyNpcs](https://modrinth.com/plugin/fancynpcs) if it is installed, else the built-in figure, else none.
*	`builtin`: a vanilla mannequin. Minecraft 1.21.9 and newer have mannequins (Spigot and Paper); on older servers there is no built-in figure, so install Citizens or FancyNpcs there.
*	`citizens`, `fancynpcs`: that plugin, if it is installed. Gods' figures are never saved in its files.
*	`off`, or `npc.enabled: false`: no figures.

The figures are never saved with the world: Gods puts them back when the altar's chunks load. The god's name shows above the figure only when `altars.floating-text` is `off` (otherwise the floating text already shows it).

A god wears the skin of its god type: `npc.skin` in its theme, a player name or a signed texture (`value` and `signature`, e.g. from [mineskin.org](https://mineskin.org)). Gods' own themes give the werewolves the werewolf alpha; the other types have the default look. A priest can give the god a player's skin with `/g godskin <player>` (permission `gods.priest.godskin`) and give back its type's with `/g godskin reset`; Gods looks the skin up in the background and keeps it in gods.yml.

```yaml
sun:
  npc:
    skin: Notch                 # a player's skin
moon:
  npc:
    skin:
      value: ewogICJ0aW1l...    # a signed texture
      signature: Z8NgxOC7...
```

Themes
---------
Each god type has a theme: the mobs it holds holy or wants slain, the foods it wants eaten or forbids, what it wants sacrificed, what its slay quests ask for, its altar blocks and how its altar signs look. Gods' own themes are built into the jar ([themes.yml](plugin/src/main/resources/themes.yml)) and use mobs, foods and blocks up to the newest Minecraft; whatever your server's version doesn't have is skipped.

To change a theme, put only your changes in `plugins/Gods/themes.yml` (Gods writes a commented example there once and never touches it again). A list replaces Gods' list; `add:` and `remove:` change it:

```yaml
frost:
  holy-mobs: [polar_bear, fox]
  holy-foods:
    add: [pumpkin_pie]
    remove: [cooked_cod]
  sign:
    dye: blue
    glowing: false
werewolves:
  altar-blocks: {add: [dark_oak_wood]}   # dark oak wood makes werewolf altars too
```

Gods that already picked a holy or unholy mob or food keep it. `altars.block-types` in config.yml still works: a god type listed there uses exactly those blocks instead of its theme's. The sign texts are `altar.sign.*` in `lang/<language>/messages.yml`. Use `/g reload` after a change.

Effects
---------
Prayers, sacrifices, blessings and curses have short effects in the style of the god's type: frost gods answer with snowflakes and chimes of ice, love gods with hearts and purring, thunder gods with sparks, and so on.

*	Prayer: particles rise around the altar with the god's sound, higher and brighter when the god is pleased, low and dark when it is angry; the god's symbol (❄, ♥, ⚡, ...) floats up from the light unless it is angry.
*	Sacrifice: the burning item floats up from the fire, spins and shrinks away, with a pleased effect when the god wanted it and an angry one when it didn't.
*	Blessing and curse: a soft glow and a chime rise around a blessed player, a dark swirl and a low sound sink around a cursed one.

Only players nearby see and hear them. In config.yml, `effects.enabled` switches them all off, `effects.prayer`, `effects.sacrifice` and `effects.blessings` each kind, and `effects.view-distance-blocks` (32) sets how close players must be. Each god type's look is the `effects` section of its theme (a `symbol`, and `pleased`, `neutral` and `angry` lists of `particles`, `sounds` (ids like `block.amethyst_block.chime`) and `colors` (dye colors or `"#RRGGBB"`)), changed in `plugins/Gods/themes.yml` like the rest:

```yaml
frost:
  effects:
    symbol: "*"
    pleased:
      sounds: [block.bell.use]
      particles: {add: [cloud]}
```

Links
---------

*	[Bukkit Plugin Page](https://dev.bukkit.org/projects/gods)
*	[Statistics on bStats](https://bstats.org/plugin/bukkit/Gods/35252)
*	[Releases](https://github.com/DoggyCraft/Gods/releases)

API for other plugins
---------
Other plugins can read Gods' gods and believers, make gods speak and act, and listen to what happens. The API is published through [JitPack](https://jitpack.io/#DoggyCraft/Gods) from release tags. With Maven:

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.DoggyCraft.Gods</groupId>
    <artifactId>gods-api</artifactId>
    <version>v1.0.0</version> <!-- a release tag -->
    <scope>provided</scope>
</dependency>
```

With Gradle:

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.DoggyCraft.Gods:gods-api:v1.0.0") // a release tag
}
```

Gods registers a `GodsApi` in Bukkit's services manager. Add `softdepend: [Gods]` to your plugin.yml, then:

```java
GodsApi gods = Bukkit.getServicesManager().load(GodsApi.class);
if (gods != null) {
    gods.getGod(player).ifPresent(god -> player.sendMessage("You believe in " + god.name() + ", who is " + god.mood()));
}
```

- **Read**: gods (`getGods()`, `getGod(name)`, `getGod(player)`; `GodInfo` snapshots with type, gender, mood, power, believers, priests, allies, enemies, home, holy land and current quest, main altar and descent), the god's figure at its altar (`getNpc(god)`, `isGodNpc(entity)`), believers (`getBeliever(uuid)`), and whose holy land a location is.
- **Speak as a god**: `say(god, player, text)` with a [MiniMessage](https://docs.advntr.dev/minimessage/format.html) text, `sayToBelievers(god, text)`, and the god's own voice lines with `sayVoiceLine(god, player, situation)` / `getVoiceLine(god, situation)`. The situations are the keys of the voice files (`getVoiceSituations()`, e.g. `believer-random-pleased-speech`).
- **Act as a god**: bless, curse, heal, give an item or a holy artifact, smite, change its mood or its gender (`setGender`, which ignores the priests' cooldown), start, complete or fail its quest, invite a player, offer, assign or remove priests, and declare alliances and wars. Each returns an `ActionResult`.
- **Events** in `com.dogonfire.gods.api.event`: `GodSpeakEvent`, `BelieverPrayEvent`, `BelieverJoinGodEvent`, `BelieverLeaveGodEvent`, `GodCreateEvent`, `GodMoodChangeEvent`, `GodGenderChangeEvent`, `PriestAssignEvent`, `PriestRemoveEvent`, `GodBlessPlayerEvent`, `GodCursePlayerEvent`, `QuestStartEvent`, `QuestCompleteEvent`, `QuestFailEvent`, `HolyLandClaimEvent`, `HolyLandAttackStartEvent`, `GodRelationChangeEvent`, `GodDescendEvent` and `GodAscendEvent`. Most can be cancelled.

Everything a god says to a player fires a `GodSpeakEvent` first, so a plugin can, for example, let a god speak through an NPC instead of the chat:

```java
@EventHandler
public void onGodSpeak(GodSpeakEvent event) {
    GodNpc npc = npcs.near(event.getRecipient(), event.getGodName()); // your own NPC lookup
    if (npc != null) {
        event.setCancelled(true);                                         // no chat message
        npc.showDialogue(event.getRecipient(), event.getWords(), event.getDelayTicks());
    }
}

// The NPC greets a believer with one of its god's own lines, or says something of its own
gods.sayVoiceLine("Zeus", player, "believer-random-pleased-speech");
gods.say("Zeus", player, "<gray>Kneel, <player>, and be heard.");
```

Only the `com.dogonfire.gods.api` package is meant to stay stable between versions. Call the API from the server thread.

Building
---------
Gods is built with Maven and JDK 25:

```bash
git clone https://github.com/DoggyCraft/Gods.git
cd Gods
mvn clean package        # or: nix develop -c mvn clean package
```

The plugin jar ends up in `plugin/target/Gods-<version>.jar`, the API jar in `api/target/gods-api-<version>.jar`.
