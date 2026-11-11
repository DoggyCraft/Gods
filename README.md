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
*	Run a [Paper](https://papermc.io/) or Spigot server on Minecraft 26.1 or newer, with Java 25.
*	Download `Gods-<version>.jar` from the [releases](https://github.com/DoggyCraft/Gods/releases) and drop it into the `plugins` folder.
*	Optional: [WorldGuard](https://enginehub.org/worldguard), [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/).
*	Start the server. Commands and permissions are listed in [plugin.yml](plugin/src/main/resources/plugin.yml).

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

- **Read**: gods (`getGods()`, `getGod(name)`, `getGod(player)`; `GodInfo` snapshots with type, gender, mood, power, believers, priests, allies, enemies, home, holy land and current quest), believers (`getBeliever(uuid)`), and whose holy land a location is.
- **Speak as a god**: `say(god, player, text)` with a [MiniMessage](https://docs.advntr.dev/minimessage/format.html) text, `sayToBelievers(god, text)`, and the god's own voice lines with `sayVoiceLine(god, player, situation)` / `getVoiceLine(god, situation)`. The situations are the keys of the voice files (`getVoiceSituations()`, e.g. `believer-random-pleased-speech`).
- **Act as a god**: bless, curse, heal, give an item or a holy artifact, smite, change its mood, start, complete or fail its quest, invite a player, offer, assign or remove priests, and declare alliances and wars. Each returns an `ActionResult`.
- **Events** in `com.dogonfire.gods.api.event`: `GodSpeakEvent`, `BelieverPrayEvent`, `BelieverJoinGodEvent`, `BelieverLeaveGodEvent`, `GodCreateEvent`, `GodMoodChangeEvent`, `PriestAssignEvent`, `PriestRemoveEvent`, `GodBlessPlayerEvent`, `GodCursePlayerEvent`, `QuestStartEvent`, `QuestCompleteEvent`, `QuestFailEvent`, `HolyLandClaimEvent`, `HolyLandAttackStartEvent` and `GodRelationChangeEvent`. Most can be cancelled.

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
