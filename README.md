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

A player shows their faith in a god by building an altar for their god and using the altar to pray.
In this way, many gods may exist at a time, all with names created by the players.

The main purpose for believing in these Gods is to gain from the help the Gods provide (see below) and to provide some basic faction play based on ideology and culture (I.e. "My god is better than yours because apples are holy!").

Another purpose is simply to get people inspired to build awesome churches and use them for their Gods.

With Holy Lands, players can easily protect their lands from grief by simply building alters to their Gods.

Holy Land (`holy-land.enabled` in config.yml) is claimed per chunk, one holy land per altar: a praying altar claims the chunks of its altar block, its sign and the spot in front of the sign, and as its god rises in rank the land grows into a square of chunks around the altar whenever a believer prays there (3x3 chunks for a recognized god up to 9x9 for a greater one, `ranks.unlocks.holy-land-radius-chunks`). Land another religion holds and neutral land are never taken, a religion holds at most power / 10 + 3 holy lands, and a land goes when nobody prays in it for `holy-land.delete-abandoned-after-days` or when the last altar of its god in it is destroyed. `/g setsafe` makes the 3x3 chunks around an admin neutral land (or clears them). Holy and neutral land keep out other players' building, breaking, fighting, hostile mobs, explosions and ender pearls; `holy-land.protection` (`auto`, `on`, `off`) decides whether Gods does that: with `auto` it leaves it to GriefPrevention or WorldGuard when one is on the server, and the land is still claimed, shown on entering, counted for ranks, gives its aura and can be attacked.

With GriefPrevention installed, a priest can make their own claim the holy land of their god: standing in the claim, `/g holyclaim` (permission `gods.priest.holyclaim`) makes the whole claim holy land, at any height, when it is a whole claim (not a subclaim or an admin claim) that the priest owns and an altar of their god stands in it. It counts as one holy land whatever its size, toward the same power / 10 + 3, and doesn't grow with the rank: the claim is the land, and wins over any holy land of chunks it overlaps. Running `/g holyclaim` again in it releases it. Prayers at its altars keep it from being abandoned and claim no chunks inside it; it goes with the last altar of its god in it, when GriefPrevention deletes the claim, when the claim is given to someone who isn't a priest of that god, or when it is resized so that no altar of the god is left in it. Without GriefPrevention such lands are kept in holyland.yml but hold no land until it is back.


Installation
---------
*	Run a [Paper](https://papermc.io/) or Spigot server on Minecraft 1.21.3 or newer (including 26.x), with Java 21 or newer.
*	Download `Gods-<version>.jar` from the [releases](https://github.com/DoggyCraft/Gods/releases) and drop it into the `plugins` folder.
*	Optional: [WorldGuard](https://enginehub.org/worldguard), [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/), [GriefPrevention](https://www.spigotmc.org/resources/griefprevention.1884/) (new altars only in claims, see below), [Citizens](https://www.spigotmc.org/resources/citizens.13811/) or [FancyNpcs](https://modrinth.com/plugin/fancynpcs) (the god at its altar on servers older than 1.21.9, see below).
*	Start the server. Commands and permissions are listed in [plugin.yml](plugin/src/main/resources/plugin.yml); in game, `/g help` lists the commands you can use, 8 to a page (`/g help 2` for the next).

Building an altar
---------
1. Place an altar block. The block decides the type of god: snow, packed or blue ice for a god of frost, prismarine or sea lanterns for the sea, copper for thunder, amethyst or bookshelves for wisdom, moss or azalea for nature, and so on. `/g help blocks` lists them all.
2. Put a light on top: a torch (any kind), a lantern, a candle or fire (fire on netherrack, or soul fire on soul sand or soil, burns forever).
3. Place a wall sign or a hanging sign on the side of the block (a hanging sign's bracket on the block) and write the name of your god on it (on either side of a hanging sign; the finished altar shows it on both). To choose its gender, write `God`, `Goddess` or `Deity` (non-binary) on another line; otherwise the god chooses and tells you.
4. Right-click the sign to pray, or the altar block or its light. Sneak to use the block as usual (placing blocks against it); items that do something to the block (flint and steel on a candle, an axe, honeycomb, ...) and blocks with a use of their own (note blocks, jukeboxes, chiseled bookshelves, candle cakes) keep it.

Gods writes the sign in the style of the god's type and remembers it, so the text can't be changed. Signs with `Curses` or `Blessings` on the first line and a player's name on the third let priests curse or bless that player.

With [GriefPrevention](https://www.spigotmc.org/resources/griefprevention.1884/) installed, new altars (praying, curse and blessing signs) can only be built inside a claim where you have build trust (your own, or one you are trusted in); elsewhere the sign is refused. Altars that already stand keep working wherever they are. Set `altars.require-claim: false` in config.yml to allow altars anywhere. Without GriefPrevention the setting does nothing. A priest can also make their claim the holy land of their god with `/g holyclaim` (see Holy Land above).

A god's priests can change its gender later with `/g gender <god|goddess|deity>`, once every 24 hours (`priests.gender-change-cooldown-hours` in config.yml); the god tells its believers. A Deity, a non-binary god, has no voice of its own: each thing it says comes from its type's male or female voice, picked at random. What a god says about itself fits any gender: where a line names the god's title it uses `<gender>` (God, Goddess or Deity), which every voice line knows. The gender words on signs and for `/g gender` are `altar.gender-words` in `lang/<language>/messages.yml`; the English ones always work.

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
| Recognized | a priest, 3 believers | the holy book, quests, holy land of 3x3 chunks around each altar |
| Descended | a priest for 24 hours, 3 believers, a neutral mood, a main altar | the god appears at its main altar; holy land of 5x5 chunks |
| Exalted | 10 believers, 50 power, a pleased mood, 72 hours as descended, 150 prayers, 20 accepted sacrifices, 2 completed quests, 2 altars, 2 converts | holy artifacts, alliances and wars; holy land of 7x7 chunks |
| Greater | 20 believers, 150 power, a pleased mood, 168 hours as exalted, 500 prayers, 75 sacrifices, 6 quests, 4 altars, 4 holy lands, 5 converts | a second priest, an aura (Regeneration I) for believers in its holy land, holy land of 9x9 chunks; the whole server hears of it |

Each condition is in `ranks.<rank>` in config.yml and each is optional (remove it and it doesn't count): `min-believers`, `min-power`, `requires-priest`, `min-mood` (`angry`, `displeased`, `neutral`, `pleased` or `exalted`: at least that mood), `requires-main-altar`, `hours-after-priest`, `hours-at-previous-rank`, and what the religion has done in its whole life: `min-prayers` (prayers at its altars), `min-sacrifices` (sacrifices it was pleased with), `min-quests` (completed quests), `min-altars` (praying altars standing), `min-holy-land-regions` (holy lands, one per altar that claimed land) and `min-converts` (players who joined through `/g invite`). Conditions of a feature that is off (sacrifices, quests, holy land) don't count. `requires-priest` and `min-believers` carry over to the ranks above.

A god never falls because of its mood or what its religion did: an angry god just stops rising. It falls at once below every rank that needs a priest when it has none left, and when it has fewer believers than its rank needs for `ranks.drop-grace-hours` (24) it falls to the highest rank it still has the believers for.

`ranks.unlocks` sets the lowest rank of each feature (`bibles`, `quests`, `holy-artifacts`, `relations`, `extra-priest`, `aura`) and how far each altar's holy land reaches per rank (`holy-land-radius-chunks`: 0 is the altar's own chunks, 1 a square of 3x3 chunks, up to 8); `ranks.aura` sets the aura's effect, level and seconds (or switches it off). A command of a feature a god hasn't unlocked says which rank it needs. The features' own switches (`quests.enabled`, `quests.required-believers`, `priests.max-per-god`, ...) still apply.

Rising to a rank shows an effect at the main altar (grander for each rank; the descent has its lightning) and the god tells its believers; a greater god is announced to the whole server. Altar effects grow with the rank: a spirit's prayers are a flicker, its symbol floats up from the descended rank on, and a greater god's are the grandest. So do the holy artifacts a god gives: a descended god's call the sun for 2 minutes, heal 2 players or tame within 4 blocks, an exalted god's 3, a greater god's 4 (a spirit's and a recognized god's 1, as before); an artifact keeps the strength it was made with.

Gods from before ranks get a rank on their first turn: the highest their believers, power, priest, mood, altars, holy land and age allow, with their lifetime stats (which Gods didn't count before) set to what that rank asks for, so they keep it. A god that had descended stays descended.

Devotion
---------
Believers have ranks too: every believer is a follower of their god and grows in devotion by serving it, to devotee, zealot and saint. Each god type names the levels its own way (`devotion` in its theme): a sea god's believers rise from Deckhand through Tidecaller and Stormrider to Saint of the Deep, a party god's from Guest to Legend of the Feast. `/g devotion [player]` (permission `gods.devotion`) shows a believer's level and points and, for your own, the next level, what earns points and what each level brings; `/g info` and `/g followers` show it too, and PlaceholderAPI's `%gods_devotion%`.

Believers earn devotion points (config.yml's `devotion.points`, the defaults):

| Deed | Points |
|---|---|
| A prayer at an altar (at most one every `prayers.min-minutes-between-prayers`, 30) | 10 |
| A sacrifice the god wanted | 15 |
| Taking part in a completed quest (the slayers, sacrificers or pilgrim who counted for it; for a quest that doesn't know who did it, the believers online who share its rewards) | 40 |
| Inviting a player (`/g invite`) who joins | 30 |
| Eating the god's holy food, slaying its unholy mob (once per `commandment-cooldown-minutes`, 10) | 5 each |

| Level | Points (`devotion.levels`) | Brings (`devotion.unlocks`) |
|---|---|---|
| Follower | every believer | |
| Devotee | 150 (about 15 prayers: a few days of regular prayer) | `/g prayfor` costs 25 % less |
| Zealot | 600 (about two weeks) | the god may give them a holy artifact (only zealots and saints get one); its gifts (items, healing, holy artifacts) come twice as often |
| Saint | 2000 (weeks of prayers, sacrifices and quests) | the god now and then praises them by name to its other believers; their level is their title (`%gods_devotion_title%`, and before their name in religion chat); when the god needs a priest it asks them on its next turn (a god always asks its most devoted believer first, and of equals whoever joined first) |

A believer rises as soon as they have the points, and the god congratulates them with an effect around them. Devotion fades when a believer stops praying: after `fade-after-days` (3) without a prayer they lose `fade-percent-per-day` (5 %) of their points each day. A level is only lost when the points fall below `keep-level-percent` (75 %) of what it needs, so a few days off never cost one; a saint who stops praying falls about nine days after their last prayer. Leaving the god or changing to another keeps `kept-on-changing-god-percent` (0: they start over). Believers from before devotion start with 10 points per prayer to their god, at most a zealot's 600, so every saint is earned. `devotion.enabled: false` switches it all off: nobody earns devotion, nothing needs a level (holy artifacts are for every believer again) and no level gives more. Holy artifacts start at zealot on purpose: they are the god's rarest gift, for believers who have served it for weeks.

Prayer power and leaving a religion
---------
Believers gain prayer power by praying (1 a prayer), by sacrifices their god wanted, quests and slaying enemies of their religion, and spend it on `/g prayfor`. A wanted sacrifice counts as a prayer below too. A believer who stops praying loses it, and leaves their religion when it is gone (config.yml's `believers`, the defaults):

*	After a quiet period without a prayer, `decay-after-days` (3) times their devotion level's `decay-after-multiplier`: a follower 3 days, a devotee 4.5, a zealot 6 and a saint 9 (with `devotion.enabled: false` everyone waits as long as a follower), their prayer power falls once a day: first by `decay-percent-per-day` (10 %, rounded up), then by `decay-per-day` (2).
*	When that decay has brought it to 0, they leave: their god tells its believers it lost them, a priest stops being one, and their devotion is gone. A believer with 20 prayer power leaves 9 days after their last prayer as a follower, 15 as a saint; one who had none leaves on the first day of decay.
*	A prayer stops the decay and starts the quiet period over. Having no prayer power never makes a believer who keeps praying leave (spending it all on `/g prayfor` is fine).
*	Devotion fades on its own (above), so a saint who stops praying may fall to zealot after about nine days and then has a zealot's quiet period, which is already over.
*	With both `decay-per-day` and `decay-percent-per-day` at 0, believers never leave for not praying. At most 5 believers of a god leave on one turn of the god.

Config files from before (`believers.max-hours-without-prayer`, 154 hours) are upgraded to `decay-after-days`: half the old time in days (154 hours: 3 days), so with the decay a server keeps roughly its patience. Priests still stop being priests after `priests.max-hours-without-prayer` (72) without a prayer.

Admin commands
---------
`/g admin <action> ...` (permission `gods.admin`, ops by default) changes a god or a believer the way the game does, with the same events, effects, words and saves. Each action has its own node, `gods.admin.<action>` (e.g. `gods.admin.info`): give someone only the actions they need, and they can use `/g admin` for those; `gods.admin` gives every action. `/g admin` alone lists the actions you may use.

| Action | Does | Permission |
|---|---|---|
| `info <god>` | the god's rank, its time at it, descent and figure, power, mood, believers per devotion level, priests, lifetime stats, main altar and quest | `gods.admin.info` |
| `setrank <god> <rank>` | gives the god a rank whatever its conditions (`spirit` to `greater`, or its type's name for it), with the rank's moment | `gods.admin.setrank` |
| `devotion <player> <level/+n/-n/n>` | gives a believer a devotion level, or adds, takes or sets their points | `gods.admin.devotion` |
| `prayerpower <player> <+n/-n/n>` | adds, takes or sets a believer's prayer power | `gods.admin.prayerpower` |
| `mood <god> <+n/-n/n/=-n>` | changes the god's mood (-100 to 100): `+10` and `-10` add and take, `50` and `=-50` set it | `gods.admin.mood` |
| `gender <god> <god/goddess/deity>` | changes the god's gender without the priests' cooldown | `gods.admin.gender` |
| `godskin <god> <player/reset>` | gives any god's figure a player's skin, or its theme's again | `gods.admin.godskin` |
| `quest <god> <start/complete/fail>` | starts a quest for the god, or completes (with its rewards) or fails its quest | `gods.admin.quest` |
| `setaltar <god>` | makes the praying altar of that god you look at its main altar | `gods.admin.setaltar` |

Players are online players or any believer Gods knows. `/g setpriest`, `/g setsafe` and `/g reload` stay as they are.

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

Messages and placeholders
---------
Everything players see is in `plugins/Gods/lang/<language>/`, in [MiniMessage](https://docs.advntr.dev/minimessage/format.html): `messages.yml` for the messages, `voices/<type>_<gender>.yml` for what each god says. A message you remove falls back to Gods' own.

A god's words have two formats in `messages.yml`: `god.speech` for words to one player (a prayer answered, a blessing, a question; like a private message) and `god.speech-believers` for words to the whole religion, its believers or priests (a new believer, a quest, a new rank; like a channel). Both start out the same; style them apart to tell them apart. Each knows `<god>`, `<message>` (the line from the voice file), `<player>` (who hears it) and `<gender>`:

```yaml
god:
  speech: '<gold>\<<god>>: <white><bold><message>'
  speech-believers: '<gold>[<god>] <yellow><message>'
```

With [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) installed, chat, tab list and name tag plugins can show a player's religion:

| Placeholder | Shows |
|---|---|
| `%gods_name%` | the player's god (empty without one) |
| `%gods_name_space%` | the god with a space after it (empty without one) |
| `%gods_color%` | the god type's color code, like `&b` |
| `%gods_prayerpower%` | the player's prayer power |
| `%gods_rank%` | the god's rank, as its god type calls it |
| `%gods_devotion%` | the player's devotion level, as their god's type calls it |
| `%gods_devotion_points%` | the player's devotion points |
| `%gods_devotion_title%` | their devotion level once it is a title (a saint's, `devotion.unlocks.title`), else empty |
| `%gods_tag%` | the god as a ready-made tag, like `[Zeus] ` in the god's color, for name tags, chat or the tab list; empty without a god |

`%gods_tag%` is `placeholder.tag` in `messages.yml` (`placeholder.tag-priest` for priests), with `<god>`, `<god-color>…</god-color>`, `<rank>`, `<devotion>` and `<title>`. With the [TAB](https://github.com/NEZNAMY/TAB) plugin, for example, in its `groups.yml`:

```yaml
_DEFAULT_:
  tagprefix: "%gods_tag%"
  tabprefix: "%gods_tag%"
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

- **Read**: gods (`getGods()`, `getGod(name)`, `getGod(player)`; `GodInfo` snapshots with type, gender, mood, power, believers, priests, allies, enemies, home, holy land and current quest, main altar, descent and rank; `home()` is the spot in front of the main altar), a god's progress towards its next rank (`getRankProgress(god)`), the god's figure at its altar (`getNpc(god)`, `isGodNpc(entity)`), believers (`getBeliever(uuid)`, with their devotion level and points), and whose holy land a location is.
- **Speak as a god**: `say(god, player, text)` with a [MiniMessage](https://docs.advntr.dev/minimessage/format.html) text, `sayToBelievers(god, text)`, and the god's own voice lines with `sayVoiceLine(god, player, situation)` / `getVoiceLine(god, situation)`. The situations are the keys of the voice files (`getVoiceSituations()`, e.g. `believer-random-pleased-speech`).
- **Act as a god**: bless, curse, heal, give an item or a holy artifact, smite, change its mood or its gender (`setGender`, which ignores the priests' cooldown), start, complete or fail its quest, invite a player, offer, assign or remove priests, and declare alliances and wars, and set its rank (`setRank`, bypassing the conditions); give a believer devotion (`addDevotion`) or a devotion level (`setDevotionLevel`). Each returns an `ActionResult`.
- **Events** in `com.dogonfire.gods.api.event`: `GodSpeakEvent`, `BelieverPrayEvent`, `BelieverJoinGodEvent`, `BelieverLeaveGodEvent`, `GodCreateEvent`, `GodMoodChangeEvent`, `GodGenderChangeEvent`, `PriestAssignEvent`, `PriestRemoveEvent`, `GodBlessPlayerEvent`, `GodCursePlayerEvent`, `QuestStartEvent`, `QuestCompleteEvent`, `QuestFailEvent`, `HolyLandClaimEvent`, `HolyLandAttackStartEvent`, `GodRelationChangeEvent`, `GodDescendEvent`, `GodAscendEvent`, `GodRankChangeEvent` (a rise can be cancelled, a fall can't) and `BelieverDevotionChangeEvent` (likewise). Most can be cancelled.

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
