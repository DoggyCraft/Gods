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

Above every praying altar the god's name and title float in the style of its type. Every god has a main altar, the home of its religion: the altar where it was founded (a god from before main altars gets its oldest altar as Gods finds it). The main altar gets a line of its own above it, and `/g info` shows its believers where it is. `/g home` (permission `gods.home`) takes believers there: in front of the altar sign, facing the altar, or onto the nearest safe spot within 3 blocks if that one is blocked. A priest can make another altar of their god the main altar: look at its sign, block or light (at most 5 blocks away) and use `/g setaltar`, or `/g sethome`, which is the same command (permission `gods.priest.setaltar`); the god tells its believers. If the main altar is destroyed, the god's oldest remaining altar becomes its main altar and the god tells its priests; with no altar left it has none until one is built. A god without a main altar that had a home from before (Gods 0.9.x and earlier, where `/g sethome` saved where you stood) keeps going there with `/g home`.

In config.yml, `altars.floating-text` is `all` (above every praying altar, the default), `main` (only above main altars) or `off`, and `altars.floating-text-view-distance-blocks` (32) sets how far away players see it. The texts are `altar.floating.*` in `lang/<language>/messages.yml`. They are never saved with the world: Gods puts them back when the altar's chunk loads.

Gods keeps track of its altars in `plugins/Gods/altars.yml`. Altars that already stand are found as their chunks load (altars from Gods 0.9.x the first time someone uses them), so nothing needs to be done after an update.

The god at its altar
---------
When a religion rises to the descended rank (see [Ranks](#ranks)), its god descends to its main altar and stands there as a figure in front of the altar sign, turning to look at players who come close. Right-clicking the figure prays to the god, just like praying at the altar. By default the god descends when it has a main altar, a priest for at least 24 hours, at least 3 believers and is at least neutral (`ranks.descended` in config.yml); lightning strikes (only a flash, it does no harm), its effects rise and it tells its believers. It ascends again when it falls below that rank: at once when it has no priest any more, or when its believers stay fewer than it needs for 24 hours (`ranks.drop-grace-hours`), and descends again when it rises once more.

Who makes the figure is `npc.provider` in config.yml:

*	`auto` (the default): [Citizens](https://www.spigotmc.org/resources/citizens.13811/) if it is installed, else [FancyNpcs](https://modrinth.com/plugin/fancynpcs) if it is installed, else the built-in figure, else none.
*	`builtin`: a vanilla mannequin. Minecraft 1.21.9 and newer have mannequins (Spigot and Paper); on older servers there is no built-in figure, so install Citizens or FancyNpcs there.
*	`citizens`, `fancynpcs`: that plugin, if it is installed. Gods' figures are never saved in its files.
*	`off`, or `npc.enabled: false`: no figures.

The figures are never saved with the world: Gods puts them back when the altar's chunks load. The god's name shows above the figure only when `altars.floating-text` is `off` (otherwise the floating text already shows it).

A god wears a skin for its gender from its theme (`npc.skin`, with `god`, `goddess` and `deity` entries):

*	a god (and a god from before genders): its type's `god` skin, else the `default` section's `god` skin;
*	a goddess: its type's `goddess` skin, else the `default` section's `goddess` skin;
*	a deity: its type's `deity` skin, its type's `god` skin, the `default` section's `deity` skin, else the `default` section's `god` skin.

Gods' own themes have a god, a goddess and a deity in the `default` section, so every god has a skin; every type has its own god, and all but party (Dionysus) their own goddess too (thunder's Zeus and a storm goddess, the sun's Amaterasu, wisdom's Athena, …); the moon has its own deity, other types' deities wear their type's god skin. When a priest changes the god's gender with `/g gender`, its figure changes skin too.

A priest can give the god a skin of its own, which comes before all of these: `/g godskin` gives it the skin the priest wears now, `/g godskin <player>` a player's skin (permission `gods.priest.godskin`), and `/g godskin reset` gives back its theme's. Gods looks the skin up in the background and keeps it in gods.yml.

To change the themes' skins, put them in `plugins/Gods/themes.yml`. A skin there must be a texture Mojang signed (its `value` and `signature`); player names don't work (Gods warns about them and ignores them). To get one, upload the skin's PNG to [mineskin.org](https://mineskin.org) and copy the value and signature it shows, or skip the file: wear the skin, run `/g godskin` as the god's priest, and change back. Each entry you give replaces Gods' skin for that gender; `''` removes it:

```yaml
moon:
  npc:
    skin:
      goddess:
        value: ewogICJ0aW1l...    # a signed texture
        signature: Z8NgxOC7...
werewolves:
  npc:
    skin:
      goddess: ''               # the default goddess instead of the werewolf alpha
```

Ranks
---------
A religion grows through five ranks, like the ranks of a forum. A god starts as a spirit and rises one rank at a time, on one of its turns, when it meets every condition of the next rank. Each god type has its own names for the ranks (`ranks` in its theme): the default ones are Spirit, Recognized, Descended, Exalted and Greater God; a god of the sea rises from Tide through Current, Breaker and Maelstrom to Leviathan, a god of frost from Snowflake to Eternal Winter. `/g rank [god]` shows a god's rank and, to its believers, what its next rank needs, condition by condition ("Sacrifices: 31/20", "Mood: Neutral (at least Pleased)"); `/g info`, `/g list`, the text above main altars and PlaceholderAPI's `%gods_rank%` show the rank too.

| Rank | Needs (config.yml's defaults) | Unlocks |
|---|---|---|
| Spirit | a new god | |
| Recognized | a priest, 3 believers | the holy book, quests, 1 more region of holy land |
| Descended | a priest for 24 hours, 3 believers, a neutral mood, a main altar | the god appears at its main altar; 2 more regions of holy land |
| Exalted | 10 believers, 50 power, a pleased mood, 72 hours as descended, 150 prayers, 20 accepted sacrifices, 2 completed quests, 2 altars, 2 converts | holy artifacts, alliances and wars; 4 more regions of holy land |
| Greater | 20 believers, 150 power, a pleased mood, 168 hours as exalted, 500 prayers, 75 sacrifices, 6 quests, 4 altars, 4 regions of holy land, 5 converts | a second priest, an aura (Regeneration I) for believers in its holy land, 8 more regions of holy land; the whole server hears of it |

Each condition is in `ranks.<rank>` in config.yml and each is optional (remove it and it doesn't count): `min-believers`, `min-power`, `requires-priest`, `min-mood` (`angry`, `displeased`, `neutral`, `pleased` or `exalted`: at least that mood), `requires-main-altar`, `hours-after-priest`, `hours-at-previous-rank`, and what the religion has done in its whole life: `min-prayers` (prayers at its altars), `min-sacrifices` (sacrifices it was pleased with), `min-quests` (completed quests), `min-altars` (praying altars standing), `min-holy-land-regions` and `min-converts` (players who joined through `/g invite`). Conditions of a feature that is off (sacrifices, quests, holy land) don't count. `requires-priest` and `min-believers` carry over to the ranks above.

A god never falls because of its mood or what its religion did: an angry god just stops rising. It falls at once below every rank that needs a priest when it has none left, and when it has fewer believers than its rank needs for `ranks.drop-grace-hours` (24) it falls to the highest rank it still has the believers for.

`ranks.unlocks` sets the lowest rank of each feature (`bibles`, `quests`, `holy-artifacts`, `relations`, `extra-priest`, `aura`) and the extra holy land regions per rank (`holy-land-extra-regions`, on top of power / 10 + 3); `ranks.aura` sets the aura's effect, level and seconds (or switches it off). A command of a feature a god hasn't unlocked says which rank it needs. The features' own switches (`quests.enabled`, `quests.required-believers`, `priests.max-per-god`, ...) still apply.

Rising to a rank shows an effect at the main altar (grander for each rank; the descent has its lightning) and the god tells its believers; a greater god is announced to the whole server. Altar effects grow with the rank: a spirit's prayers are a flicker, its symbol floats up from the descended rank on, and a greater god's are the grandest.

Gods from before ranks get a rank on their first turn: the highest their believers, power, priest, mood, altars, holy land and age allow, with their lifetime stats (which Gods didn't count before) set to what that rank asks for, so they keep it. A god that had descended stays descended.

Themes
---------
Each god type has a theme: the mobs it holds holy or wants slain, the foods it wants eaten or forbids, what it wants sacrificed, what its slay quests ask for, the mobs its curses send (`curse-mobs`: evil sends wither skeletons and vexes, frost strays, the sea drowned, ...), the peaceful mobs that may still spawn in its holy land when it doesn't allow mob spawning (`peaceful-mobs`: the sea's dolphins and fish, the sun's camels and armadillos, ...), its altar blocks and how its altar signs look. Gods' own themes are built into the jar ([themes.yml](plugin/src/main/resources/themes.yml)) and use mobs, foods and blocks up to the newest Minecraft; whatever your server's version doesn't have is skipped.

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

- **Read**: gods (`getGods()`, `getGod(name)`, `getGod(player)`; `GodInfo` snapshots with type, gender, mood, power, believers, priests, allies, enemies, home, holy land and current quest, main altar, descent and rank; `home()` is the spot in front of the main altar), a god's progress towards its next rank (`getRankProgress(god)`), the god's figure at its altar (`getNpc(god)`, `isGodNpc(entity)`), believers (`getBeliever(uuid)`), and whose holy land a location is.
- **Speak as a god**: `say(god, player, text)` with a [MiniMessage](https://docs.advntr.dev/minimessage/format.html) text, `sayToBelievers(god, text)`, and the god's own voice lines with `sayVoiceLine(god, player, situation)` / `getVoiceLine(god, situation)`. The situations are the keys of the voice files (`getVoiceSituations()`, e.g. `believer-random-pleased-speech`).
- **Act as a god**: bless, curse, heal, give an item or a holy artifact, smite, change its mood or its gender (`setGender`, which ignores the priests' cooldown), start, complete or fail its quest, invite a player, offer, assign or remove priests, and declare alliances and wars, and set its rank (`setRank`, bypassing the conditions). Each returns an `ActionResult`.
- **Events** in `com.dogonfire.gods.api.event`: `GodSpeakEvent`, `BelieverPrayEvent`, `BelieverJoinGodEvent`, `BelieverLeaveGodEvent`, `GodCreateEvent`, `GodMoodChangeEvent`, `GodGenderChangeEvent`, `PriestAssignEvent`, `PriestRemoveEvent`, `GodBlessPlayerEvent`, `GodCursePlayerEvent`, `QuestStartEvent`, `QuestCompleteEvent`, `QuestFailEvent`, `HolyLandClaimEvent`, `HolyLandAttackStartEvent`, `GodRelationChangeEvent`, `GodDescendEvent`, `GodAscendEvent` and `GodRankChangeEvent` (a rise can be cancelled, a fall can't). Most can be cancelled.

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
